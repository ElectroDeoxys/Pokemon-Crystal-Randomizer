package engine;

import static java.lang.Math.*;
import java.util.ArrayList;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

import static data.Constants.*;
import data.Route;
import data.Trainer;
import data.PokemonGame;
import data.Move;
import data.Sprite;

class RomReader
{
	private FileChannel ch = null;
	
	RomReader(FileChannel ch)
	{
		this.ch = ch;
	}
	
	static byte[] readFromRom(FileChannel ch, int pos, int length) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(length);
		ch.read(buffer, pos);
		return buffer.array();
	}
	
	static byte readByteFromRom(FileChannel ch, int pos) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(1);
		ch.read(buffer, pos);
		byte[] byteHolder = buffer.array();
		return byteHolder[0];
	}

	static byte[] readTextFromRom(FileChannel ch, int pos) throws IOException
	{
		byte charRead;
		int n = 0; // keep track number of loop
		ArrayList<Byte> textList = new ArrayList<Byte>();
		
		while (true)
		{
			charRead = readByteFromRom(ch, pos+n);
			
			if (charRead != (byte) 0x50) // if not a terminating byte
				textList.add(charRead);
			else
				break; 

			n++;
		}
		
		Byte[] textOutObj = textList.toArray(new Byte[0]);
		byte[] textOut = new byte[textOutObj.length]; // convert object into primitive
		for (int i = 0; i < textOut.length; i++)
			textOut[i] = textOutObj[i].byteValue();
		
		return textOut;		
	}

	static int lengthUntilByte(FileChannel ch, int pos, byte term) throws IOException
	{
		byte byteRead;
		int n = 0; // keep track number of loop
		
		while (true)
		{
			byteRead = readByteFromRom(ch, pos+n);
			
			if (byteRead == term) // if a terminating byte
				break; 
			
			n++;
		}
		
		return n;		
	}
	
	Route[] readRomRoutes() throws IOException
	{
		int nRoutesTotal = 0;
		
		for (int i : N_WILD_ROUTES)
			nRoutesTotal += i; // add all routes
		
		Route[] routes = new Route[nRoutesTotal];
		
		for (int i = 0; i < nRoutesTotal; i++) // cycle all routes
		{
			int lIndex = Route.getLandIndex(i); // index if land route (0) or water route (1)
			
			int pos = Route.convertIndexToOffset(i);
			byte[] dataRead = readFromRom(ch, pos, N_WILD_SLOTS[lIndex] * 2);
			
			byte[] poke = new byte[N_WILD_SLOTS[lIndex]];
			byte[] lvl = new byte[N_WILD_SLOTS[lIndex]];
			
			for(int j = 0; j < N_WILD_SLOTS[lIndex]; j++) // cycle slots to split Pokemon and Lvls
			{
				poke[j] = dataRead[2*j+1];
				lvl[j] = dataRead[2*j];
			}
		
			routes[i] = new Route(i, poke, lvl);
		}
		
		return routes;
	}
	
	Trainer[] readRomTrainers() throws IOException
	{
		Trainer[] trainers = new Trainer[N_TRAINERS];
		int pos = OFFSET_TRAINERS; // use for tracking the position after processing each trainer
		
		for (int i = 0; i < N_TRAINERS; i++) // cycle all trainers
		{
			int initPos = pos; // initial offset position
			
			byte[] name = readTextFromRom(ch, pos);
			pos += name.length + 1; // adjust the position
			
			byte trnKind = readByteFromRom(ch, pos);
			pos++;
			
			byte statExp = readByteFromRom(ch, pos);
			pos++;
			
			int sizeParty = lengthUntilByte(ch, pos, (byte) 0xFF);
			byte[] dataRead = readFromRom(ch, pos, sizeParty);
			byte[][] party;
			
			if (trnKind == 0) // no items, no moves
			{
				byte[][] partyData = new byte[sizeParty/2][2];
				
				for (int j = 0; j < sizeParty/2; j++) // cycle party Pokemon
				{
					partyData[j][0] = dataRead[2*j]; // level
					partyData[j][1] = dataRead[2*j+1]; // species
				}
				
				party = partyData;
			}
			else if (trnKind == 1) // no items, with moves
			{
				byte[][] partyData = new byte[sizeParty/6][6];
				
				for (int j = 0; j < sizeParty/6; j++) // cycle party Pokemon
				{
					partyData[j][0] = dataRead[6*j]; // level
					partyData[j][1] = dataRead[6*j+1]; // species
					
					for (int k = 0; k < 4; k++)
						partyData[j][k+2] = dataRead[6*j+k+2]; // moves
				}
				
				party = partyData;
				
			}
			else if (trnKind == 2) // with items, no moves
			{
				byte[][] partyData = new byte[sizeParty/3][3];
				
				for (int j = 0; j < sizeParty/3; j++) // cycle party Pokemon
				{
					partyData[j][0] = dataRead[3*j]; // level
					partyData[j][1] = dataRead[3*j+1]; // species
					partyData[j][2] = dataRead[3*j+2]; // item
				}
				
				party = partyData;
			}
			else // with items, with moves
			{
				byte[][] partyData = new byte[sizeParty/7][7];
				
				for (int j = 0; j < sizeParty/7; j++) // cycle party Pokemon
				{
					partyData[j][0] = dataRead[7*j]; // level
					partyData[j][1] = dataRead[7*j+1]; // species
					partyData[j][2] = dataRead[7*j+2]; // item
					
					for (int k = 0; k < 4; k++)
						partyData[j][k+3] = dataRead[7*j+k+3]; // moves
				}
				
				party = partyData;
			}
			
			pos += sizeParty + 1; // adjust position for next trainer
			trainers[i] = new Trainer(i, initPos, name, trnKind, statExp, party);
		}
		
		return trainers;
	}
	
	PokemonGame[] readRomPokemon() throws IOException
	{
		PokemonGame[] mons = new PokemonGame[N_POKEMON];
		
		int thisOffset2 = OFFSET_POKEMON_2; // for keeping track of the offsets in the second set
		
		ArrayList<Byte> futureEvos = new ArrayList<Byte>(); // keep track of Pokemon after who have pre-evos
		ArrayList<Byte> futureEvosIndex = new ArrayList<Byte>(); // store the Pokémon index that evolve into the evos
		
		for (int i = 0; i < N_POKEMON; i++)
		{
			/////////////////////////////////////
			// first set of data at OFFSET_POKEMON_1
			// covering base stats, types, and other misc data
			/////////////////////////////////////
			
			byte[] preEvoIndex = new byte[1]; // store preEvoIndex, if any
			boolean hasPre = false;
			
			if (futureEvos.contains(valueToByte(i+1))) // check if the pre-evolution has been processed yet
			{
				preEvoIndex[0] = futureEvosIndex.get(futureEvos.indexOf(valueToByte(i+1))).byteValue();
				hasPre = true;
			}
			
			int pos = PokemonGame.convertIndexToOffset(i);
			pos++;
			
			byte[] base = readFromRom(ch, pos, 6); // read base stats
			pos += 6;
			
			byte[] type = readFromRom(ch, pos, 2); // read types
			pos += 2;
			
			byte[] misc = new byte[9];
			
			byte[] misc1 = readFromRom(ch, pos, 5);
			pos += 5 + 1; // 1 padding byte
			misc[5] = readByteFromRom(ch, pos); // step cycles
			pos += 1 + 1; // 1 padding byte
			byte gfx = readByteFromRom(ch, pos); // gfx
			pos += 1 + 4; // 4 padding bytes
			byte[] misc2 = readFromRom(ch, pos, 2); // growth rate and egg group
			pos += 2;
			byte[] tmhm = readFromRom(ch, pos, 8);
			
			for (int j = 0; j < misc1.length; j++)
				misc[j] = misc1[j];
			for (int j = 0; j < misc2.length; j++)
				misc[j+6] = misc2[j];
			
			/////////////////////////////////////
			// second set of data at OFFSET_POKEMON_2
			// covering evolution and level-up move data
			/////////////////////////////////////
			
			pos = thisOffset2; // set position to second set
			
			ArrayList<Byte[]> evoList = new ArrayList<Byte[]>();
			
			int readLength = lengthUntilByte(ch, pos, (byte) 0x00);
			
			if (readLength == 0) // no evolutions
			{
				Byte[] evoNull = new Byte[0];
				evoList.add(evoNull);
			}
			else while (readByteFromRom(ch, pos) != (byte) 0x00) // read evolutions
			{
				Byte[] evo; // declare new evolution array
				
				if (readByteFromRom(ch, pos) == 0x05) // Tyrogue evolution takes in 4 bytes
				{
					evo = convertByteArray(readFromRom(ch, pos, 4));
					pos += 4;
				}
				else
				{
					evo = convertByteArray(readFromRom(ch, pos, 3)); // all the rest takes in 3 bytes
					pos += 3;
				}
				
				evoList.add(evo); // append that evolution to the list
				
				if (byteToValue(evo[evo.length-1].byteValue()) < i+1) // if evolution has already been processed
				{
					byte[] preEvoByte = new byte[1];
					preEvoByte[0] = valueToByte(i+1);
					mons[byteToValue(evo[evo.length-1].byteValue()) - 1].setPreEvo(preEvoByte);
				}
				else // check afterwards to determine it's an evolution form and give it the index
				{
					futureEvos.add(evo[evo.length-1].byteValue());
					futureEvosIndex.add(valueToByte(i+1));
				}
			}
			
			byte[][] evoArray = new byte[evoList.size()][];
			
			for (int j = 0; j < evoArray.length; j++)
				evoArray[j] = convertByteArray(evoList.get(j));
		
			pos++; // position for moves	
			
			ArrayList<Byte[]> moveList = new ArrayList<Byte[]>();
			
			while (readByteFromRom(ch, pos) != (byte) 0x00) // read moves
			{
				Byte[] move = convertByteArray(readFromRom(ch, pos, 2)); // moves take 2 bytes
				moveList.add(move); // append that move to the list
				pos += 2; // adjust position for next move
			}
			
			byte[][] moveArray = new byte[moveList.size()][];
			
			for (int j = 0; j < moveArray.length; j++)
				moveArray[j] = convertByteArray(moveList.get(j));
			
			thisOffset2 = pos + 1; // setting the next offset
			
			mons[i] = new PokemonGame(i, i+1, base, type, misc, gfx, tmhm, evoArray, moveArray);
			
			if (hasPre)
				mons[i].setPreEvo(preEvoIndex);
		}
		
		for (int i = 0; i < N_POKEMON; i++) // loop again
		{
			/////////////////////////////////////
			// third set of data at OFFSET_POKEMON_3
			// covering egg move data
			/////////////////////////////////////
			
			int ptrPos = OFFSET_POINTERS_2 + i * 2; // position of this Pokemon's egg moves pointer
			byte[] ptr = readFromRom(ch, ptrPos, 2);
			int pos = getOffset(ptr, OFFSET_POINTERS_2); // position for this egg moves
			
			if (readByteFromRom(ch, pos) == (byte) 0xFF)
				continue; // don't process Pokemon with no egg moves
			
			ArrayList<Byte> eggMoveList = new ArrayList<Byte>();
			
			while (readByteFromRom(ch, pos) != (byte) 0xFF) // read moves
			{
				Byte move = readByteFromRom(ch, pos); // moves take 1 byte
				eggMoveList.add(move); // append that move to the list
				pos ++; // adjust position for next move
			}
			
			byte[] eggMoveArray = new byte[eggMoveList.size()];
			
			for (int j = 0; j < eggMoveArray.length; j++)
				eggMoveArray[j] = eggMoveList.get(j).byteValue();
			
			mons[i].setEggMoves(eggMoveArray);
			mons[i].setEggMovesCarry(eggMoveArray);
			
			if (mons[i].hasEvos()) // check the evolutions of this Pokemon to pass on the egg moves
			{
				byte[][] evo = mons[i].getEvos();
				
				for (int j = 0; j < evo.length; j++) // cycle all evolutions
				{
					PokemonGame evoMon = mons[byteToValue(evo[j][evo[j].length - 1]) - 1];
					evoMon.setEggMovesCarry(eggMoveArray);
					
					if (evoMon.hasEvos()) // if that Pokemon has evos too, set their egg moves as well
					{
						byte[][] evo2 = evoMon.getEvos();
				
						for (int k = 0; k < evo2.length; k++) // cycle all evolutions
						{
							PokemonGame evoMon2 = mons[byteToValue(evo2[k][evo2[k].length - 1]) - 1];
							evoMon2.setEggMovesCarry(eggMoveArray);
						}
					}
				}
			}
		}
		
		return mons;
	}
	
	Move[] readRomMoves() throws IOException
	{
		Move[] moves = new Move[N_MOVES];
		int posNames = OFFSET_MOVE_NAMES;

		for (int i = 0; i < N_MOVES; i++) // cycle all moves
		{
			int pos = Move.convertIndexToOffset(i);
			byte[] dataRead = readFromRom(ch, pos, 7);
			byte[] effect = {dataRead[1], dataRead[6]};
		
			moves[i] = new Move(i, dataRead[0], effect, dataRead[2], dataRead[3], dataRead[4], dataRead[5]);

			byte[] name = readTextFromRom(ch, posNames);
			posNames += name.length + 1; // adjust the position
			moves[i].setName(name);
		}
		
		return moves;
	}
	
	byte[] readRomTMs() throws IOException // has TM, HM and move tutor moves
	{
		byte[] moveBytes = new byte[N_TM+N_HM+N_MOVE_TUTOR];
		
		for (int i = 0; i < moveBytes.length; i++) // cycle all moves
		{
			int pos = OFFSET_TM_MOVES + i;
			moveBytes[i] = readByteFromRom(ch, pos);
		}
		
		return moveBytes;
	}
	
	byte[] readRomCritAnimations() throws IOException
	{
		byte[] critBytes;
		int length = lengthUntilByte(ch, OFFSET_CRIT_MOVES, (byte) 0xFF);
		critBytes = readFromRom(ch, OFFSET_CRIT_MOVES, length);
		
		return critBytes;
	}
	
	byte[] readRomStarters() throws IOException
	{
		byte[] starterBytes = new byte[3];

		starterBytes[0] = readByteFromRom(ch, OFFSET_STARTERS[0][0]);
		starterBytes[1] = readByteFromRom(ch, OFFSET_STARTERS[1][0]);
		starterBytes[2] = readByteFromRom(ch, OFFSET_STARTERS[2][0]);
		
		return starterBytes;
	}
	
	Sprite[] readRomSprites(boolean unownSprites) throws IOException
	{
		// if (unownSprites) get the sprites for Unown
		
		Sprite[] sprites = (unownSprites) ? new Sprite[N_UNOWN] : new Sprite[N_POKEMON];
		int ptrOffset = (unownSprites) ? OFFSET_SPRITE_POINTERS_U : OFFSET_SPRITE_POINTERS;
		
		for (int i = 0; i < sprites.length; i++)
		{
			byte[] ptr; // pointer to sprite
			int[] pos = new int[2];
			int length;
			
			ptr = readFromRom(ch, ptrOffset + 6 * i, 3);
			pos[0] = Sprite.pointerToOffset(ptr);
			length = (!unownSprites && i == INDEX_UNOWN - 1) ? 0 : getPicSize(pos[0]);
			
			byte[] front = readFromRom(ch, pos[0], length);
			
			ptr = readFromRom(ch, ptrOffset + 6 * i + 3, 3);
			pos[1] = Sprite.pointerToOffset(ptr);
			length = (!unownSprites && i == INDEX_UNOWN - 1) ? 0 : getPicSize(pos[1]);
			
			byte[] back = readFromRom(ch, pos[1], length);
			
			byte dim;
			if (unownSprites)
				dim = readByteFromRom(ch, OFFSET_POKEMON_1 + 0x11 + (0x20 * (INDEX_UNOWN - 1))); // read only the relevant byte
			else
				dim = readByteFromRom(ch, OFFSET_POKEMON_1 + 0x11 + (0x20 * i)); // read only the relevant byte
			
			sprites[i] = new Sprite(front, back, dim, pos);
		}
		
		return sprites;
	}
	
	byte[] readRomSpriteDim() throws IOException
	{
		byte[] dim = new byte[N_POKEMON];
		
		for (int i = 0; i < dim.length; i++)
			dim[i] = readByteFromRom(ch, OFFSET_POKEMON_1 + 0x11 + (0x20 * i)); // read only the relevant byte
		
		return dim;
	}
	
	Sprite[] readRomTrainerSprites() throws IOException
	{
		Sprite[] sprites = new Sprite[N_TRAINER_SPRITES];
		int ptrOffset = OFFSET_TRAINER_SPRITE_POINTERS;
		
		for (int i = 0; i < sprites.length; i++)
		{
			byte[] ptr; // pointer to sprite
			int[] pos = new int[2];
			int length;
			
			ptr = readFromRom(ch, ptrOffset + 3 * i, 3);
			pos[0] = Sprite.pointerToOffset(ptr);
			length = getPicSize(pos[0]);
			byte[] front = readFromRom(ch, pos[0], length);
			
			pos[1] = 0;
			
			sprites[i] = new Sprite(front, pos);
		}
		
		return sprites;
	}	
	
	Sprite readRomEggSprite() throws IOException
	{
		Sprite sprite;
		int ptrOffset = OFFSET_SPRITE_POINTER_EGG;
		
		byte[] ptr; // pointer to sprite
		int[] pos = new int[2];
		int length;
		
		ptr = readFromRom(ch, ptrOffset, 3);
		pos[0] = Sprite.pointerToOffset(ptr);
		length = getPicSize(pos[0]);
		byte[] front = readFromRom(ch, pos[0], length);
		
		pos[1] = 0;
		
		sprite = new Sprite(front, pos);
		
		return sprite;
	}
	
	byte[][][] readRomPalettes() throws IOException
	{
		byte[][][] pal = new byte[N_POKEMON][2][4];
		
		for (int i = 0; i < pal.length; i++)
		{
			pal[i][0] = readFromRom(ch, OFFSET_PAL + i*8, 4); // regular
			pal[i][1] = readFromRom(ch, OFFSET_PAL + i*8 + 4, 4); // shiny
		}
		
		return pal;
	}
	
	byte[] readPokemonName(int n) throws IOException
	{
		// returns the name of the nth Pokemon
		int pos = OFFSET_POKEMON_NAMES + NAME_LEN * n;
		int len = min(lengthUntilByte(ch, pos, (byte) 0x50), 10);
		
		return readFromRom(ch, pos, len);
	}
	
	int getPicSize(int pos) throws IOException
	{
		// gets size of lz compressed pics that starts at position pos in the ROM
		
		int out = pos;
		int jump = 0;
		byte ptrPar; // pointer parameter
		
		byte b = readByteFromRom(ch, out);
		
		while (b != (byte) 0xFF)
		{
			switch (b & 0b111_00000)
			{
				case (0b000_00000): // command with n parameters
					byte bMask = (byte) 0b0001_1111;
					jump = (byteToValue((byte) (b & bMask)) + 1) + 1;
					break;
				case (0b001_00000): // command with one parameter
					jump = 1 + 1;
					break;
				case (0b010_00000): // command with two parameters
					jump = 2 + 1;
					break;
				case (0b011_00000): // command with no parameters
					jump = 0 + 1;
					break;
				case (0b100_00000): // commands with pointer parameters
				case (0b101_00000):
				case (0b110_00000):
					ptrPar = readByteFromRom(ch, out+1);
					if ((ptrPar & 0b1000_0000) == 0b1000_0000) // 7-bit negative offset
						jump = 1 + 1;
					else // 15-bit positive offset
						jump = 2 + 1;
					break;
				case (0b111_00000): // lz long
					byte[] c = readFromRom(ch, out, 2);
					switch (c[0] & 0b000_111_00)
					{
						case (0b000_000_00): // command with n parameters
							byte[] b2Mask = new byte[2];
							b2Mask[0] = (byte) 0b0000_0011;
							b2Mask[1] = (byte) 0b1111_1111;
							jump = (byteToValue((byte) (c[0] & b2Mask[0])) * 0x100 + byteToValue((byte) (c[1] & b2Mask[1])) + 1) + 2;
							break;
						case (0b000_001_00): // command with one parameter
							jump = 1 + 2;
							break;
						case (0b000_010_00): // command with two parameters
							jump = 2 + 2;
							break;
						case (0b000_011_00): // command with no parameters
							jump = 0 + 2;
							break;
						case (0b000_100_00): // commands with pointer parameters
						case (0b000_101_00):
						case (0b000_110_00):
							ptrPar = readByteFromRom(ch, out+2);
							if ((ptrPar & 0b1000_0000) == 0b1000_0000) // 7-bit negative offset
								jump = 1 + 2;
							else // 15-bit positive offset
								jump = 2 + 2;
							break;
						default: break;
					}
					break;
				default: break;
			}
			
			out += jump;
			b = readByteFromRom(ch, out);
		}
		return (out - pos + 1);
	}
}