package engine;

import static java.lang.Math.*;
import java.util.ArrayList;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

import static data.Constants.*;
import static engine.RomReader.*;
import static engine.RomWriter.*;

import data.Pokemon;

class SavePatcher
{
	private FileChannel ch = null;
	
	private static final int[] OFFSET_SAVE_TEAM_SPECIES = {0x1A66, 0x2866};
	private static final int[] OFFSET_SAVE_TEAM = {0x1A6D, 0x286D};
	private static final int LENGTH_POKEMON_DATA = 0x30;
	
	SavePatcher(FileChannel ch)
	{
		this.ch = ch;
	}
	
	private class SaveMon
	{
		byte index;
		byte item;
		byte[] moves;
		int totalExp;
		int lvl;
		
		SaveMon(byte index, byte item, byte[] moves, int totalExp, int lvl)
		{
			this.index = index;
			this.item = item;
			this.moves = moves;
			this.totalExp = totalExp;
			this.lvl = lvl;
		}
	}
	
	void generateTeam(PokemonSorter monSorter, MoveSorter moveSorter, Pokemon[] mons, int nMons, int lvl) throws IOException
	{
		// generates a nMons-party team with specified level
		Pokemon[] monTeam = new Pokemon[nMons];
		byte[] monTeamByte = new byte[nMons];
		
		// create dummy Pokemon team with random tiers
		for (int i = 0; i < nMons; i++)
		{
			byte[] tmhmByte = new byte[N_TM + N_HM + N_MOVE_TUTOR];
			for (int j = 0; j < tmhmByte.length; j++)
				tmhmByte[j] = (byte) 0x00; // placeholder tmhm bytes
			
			monTeam[i] = new Pokemon(0, (byte) 0xFF, new byte[0], new byte[0], new byte[0], (byte) 0x00, tmhmByte, new byte[0][0], new byte[0][0]);
			monTeam[i].setOldTier((int) floor(random() * (N_TIERS - 1)));
		}
		
		ArrayList<Byte> prevMonList = new ArrayList<Byte>(); // keep track of previous Pokemon in team
					
		for (int i = 0; i < nMons; i++) // cycle party
		{
			byte[] prevMonArray = convertByteArray(prevMonList.toArray(new Byte[0]));
			monTeamByte[i] = monSorter.getSameTier(monTeam[i], -1, false, false, true, prevMonArray);
			prevMonList.add(monTeamByte[i]);
		}
		
		int[] lvlL = new int[nMons];
		for (int i = 0; i < lvlL.length; i++)
			lvlL[i] = lvl;
		monTeamByte = monSorter.evolveTeam(monTeamByte, lvlL);
		
		// apply moveset
		byte[][] moves = new byte[nMons][4];
		for (int i = 0; i < nMons; i++)
			moves[i] = monSorter.getMoveset(moveSorter, monTeamByte[i], valueToByte(lvl), true);
		
		// create team for saving		
		SaveMon[] savMon = new SaveMon[nMons];
		
		for (int i = 0; i < nMons; i++)
			savMon[i] = new SaveMon(monTeamByte[i], (byte) 0x00, moves[i], mons[byteToValue(monTeamByte[i]) - 1].getTotalExp(lvl), lvl);
		
		writeTeam(savMon);
	}
	
	void writeTeam(SaveMon[] savMon) throws IOException
	{
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < savMon.length; j++)
			{
				ByteBuffer buffer = ByteBuffer.allocate(1);
				byte[] b = new byte[1];
				
				// update species list
				int posSpecies = OFFSET_SAVE_TEAM_SPECIES[i] + j;

				b[0] = savMon[j].index;
				buffer.put(b); // index
				writeToRom(ch, buffer, posSpecies);
				
				// update party
				int pos = OFFSET_SAVE_TEAM[i] + LENGTH_POKEMON_DATA * j;
				
				b[0] = savMon[j].index;
				buffer.put(b); // index
				writeToRom(ch, buffer, pos);
				
				ByteBuffer bufferItem = ByteBuffer.allocate(1);
				b[0] = savMon[j].item;
				bufferItem.put(b); // item
				writeToRom(ch, bufferItem, pos + 0x01);
				
				ByteBuffer bufferMoves = ByteBuffer.allocate(4);
				bufferMoves.put(savMon[j].moves); // moves
				writeToRom(ch, bufferMoves, pos + 0x02);
				
				byte[] expByte = new byte[3];
				expByte[0] = (byte) ((savMon[j].totalExp & 0xFF0000) >> 16);
				expByte[1] = (byte) ((savMon[j].totalExp & 0x00FF00) >> 8);
				expByte[2] = (byte) ((savMon[j].totalExp & 0x0000FF));
				
				ByteBuffer bufferExp = ByteBuffer.allocate(3);
				bufferExp.put(expByte); // Exp
				writeToRom(ch, bufferExp, pos + 0x08);
				
				int statExp = 25600;
				byte[] statByte = new byte[2];
				statByte[0] = (byte) ((statExp & 0xFF00) >> 8);
				statByte[1] = (byte) ((statExp & 0x00FF));
				
				ByteBuffer bufferStat = ByteBuffer.allocate(2);
				bufferStat.put(statByte); // HP Stat Exp
				writeToRom(ch, bufferStat, pos + 0x0B);
				bufferStat.put(statByte); // Attack Stat Exp
				writeToRom(ch, bufferStat, pos + 0x0B + 1);
				bufferStat.put(statByte); // Defense Stat Exp
				writeToRom(ch, bufferStat, pos + 0x0B + 2);
				bufferStat.put(statByte); // Speed Stat Exp
				writeToRom(ch, bufferStat, pos + 0x0B + 3);
				bufferStat.put(statByte); // Special Stat Exp
				writeToRom(ch, bufferStat, pos + 0x0B + 4);
				
				b[0] = valueToByte(savMon[j].lvl);
				buffer.put(b); // Level
				writeToRom(ch, buffer, pos + 0x1F);
			}
	}
	
	void updateChecksums() throws IOException
	{
		int[] initPos = {0x2009, 0x1209};
		int[] endPos = {0x2B82, 0x1D82};
		int[] chsumPos = {0x2D0D, 0x1F0D};
		
		int pos;
		
		int[] sum = {0, 0};
		
		for (int i = 0; i < 2; i++)
		{
			pos = initPos[i];
			while (pos <= endPos[i])
			{
				sum[i] += byteToValue(readByteFromRom(ch, pos));
				pos++;
			}
		
			ByteBuffer buffer = ByteBuffer.allocate(2);
			byte[] b = new byte[2];
			
			b[0] = (byte) ((sum[i] & 0x00FF));
			b[1] = (byte) ((sum[i] & 0xFF00) >> 8);
			buffer.put(b);
			writeToRom(ch, buffer, chsumPos[i]);
		}
	}
}