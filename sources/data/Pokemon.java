package data;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;

public abstract class Pokemon
{
	protected int trueIndex; // national index of Pokemon

	protected byte[] base = new byte[6]; // base stats (hp, atk, def, spd, sat, sdf)
	protected byte[] typeByte = new byte[2];
	
	// misc data (catch rate, base exp, item1, item2, gender ratio, 
	// egg steps, growth rate, egg group)
	protected byte[] misc = new byte[8];
	
	protected byte gfx;
	protected byte[] tmhmByte; // TM/HM compatibility
	
	protected byte[][] evo; // [evo number][method, method parameter(s), species index]
	protected byte[][] move; // [move number][level, move index]
	protected byte[] eggMove;
	protected byte[] eggMoveCarry; // egg moves that it can carry from itself or pre-evolutions
	
	protected int tier = 0; // tier list to compare strengths
	protected int typeTier = 0; // tier list to compare strengths for same type
	
	protected byte[] preEvo = new byte[0]; // lists pre-evolution
	protected byte[] name;
	protected byte icon;

	protected Type[] types = new Type[2];

	protected boolean[] tmhmComp = new boolean[N_TM + N_HM + N_MOVE_TUTOR];
	
	protected int oldTier = -1; // tier list to compare strengths from Pokemon it replaced in the old Pokedex
	protected int oldTypeTier = -1; // tier list to compare strengths for same type from Pokemon it replaced in the old Pokedex
	
	/////////////////////////////////////////////
	// Return value methods
	/////////////////////////////////////////////
	
	public abstract int getIntIndex();

	public int getTrueIndex()
	{
		return this.trueIndex;
	}
	
	public byte[] getBaseBytes()
	{
		return this.base;
	}

	public int[] getBase()
	{
		return byteToValue(this.base);
	}
	
	public Type[] getTypes()
	{
		return this.types;
	}

	public byte[] getTypesByte()
	{
		return typeByte;
	}
	
	public byte[] getMisc()
	{
		return this.misc;
	}
	
	public byte getGfx()
	{
		return this.gfx;
	}
	
	public boolean hasEvos()
	{
		boolean out;
		
		if (evo[0].length == 0)
			out = false;
		else
			out = true;
		
		return out;
	}
	
	public byte[][] getEvos()
	{
		return this.evo;
	}

	abstract public int[] getEvoInt();
	
	public byte[][] getMoves()
	{
		return this.move;
	}
	
	public boolean hasPre()
	{
		boolean out = false;
		
		if (preEvo.length > 0)
			out = true;
		
		return out;
	}
	
	public byte[] getPreEvo()
	{
		return preEvo;
	}

	abstract public int[] getPreEvoInt();
	
	public byte[] getEggMoves()
	{
		return this.eggMove;
	}
	
	public byte[] getEggMovesCarry()
	{
		return this.eggMoveCarry;
	}
	
	public int getNBytes() // get number of bytes from all moves and evolutions 
	{
		int res = 0;
		
		if (this.hasEvos()) // if has evolutions
			for (int i = 0; i < evo.length; i++) // cycle evos
				res += evo[i].length;
				
		for (int i = 0; i < move.length; i++) // cycle moves
			res += 2; // two bytes per move
		
		return res;
	}
	
	public int getBST()
	{
		int BST = 0;
		for (byte b : base)
			BST += byteToValue(b);
		return BST;
	}
	
	public int getTier()
	{
		return this.tier;
	}

	public int getTypeTier()
	{
		return this.typeTier;
	}
	
	public boolean isLegendary()
	{
		boolean out = false; // assume it isn't legendary
		
		if ((this.getBST() >= LEGENDARY_BST) && !(this.hasPre()))
			out = true;
		
		return out;
	}
	
	public byte[] getName()
	{
		return this.name;
	}
	
	public byte getIcon()
	{
		return this.icon;
	}

	public boolean[] getCompatibilities()
	{
		return this.tmhmComp;
	}
	
	public byte[] getCompatibilitiesByte()
	{
		int nBytes = 8;
		byte[] out = new byte[nBytes];
		
		for (int i = 0; i < out.length; i++) // cycle bytes
		{
			out[i] = 0x00; // initialize to zero
			
			for (int j = 0; j < 8; j++) // cycle bits
			{
				if (8*i+j >= tmhmComp.length)
					break; // avoid array out of bounds
				if (tmhmComp[8*i+j]) // if this TM is compatible
				{
					byte addTerm = 0x01;
					out[i] += addTerm << j; // shift bits to right
				}
			}
		}

		return out;
	}

	public byte[] getMovesUpToLevel(byte lvl) // gets moves learned at and before lvl only for this stage
	{
		ArrayList<Byte> movesOut = new ArrayList<Byte>();
		int n = this.move.length - 1; // index to start decreasing moveset

		// find index of the highest level move before lvl
		while (move[n][0] > lvl && n > 0)
			n--;

		// get moves before that level
		while (n >= 0)
		{
			movesOut.add(move[n][1]);
			n--;
		}
		
		return convertByteArray(movesOut.toArray(new Byte[0]));
	}

	public byte[][] getMovesUpToLevel(Pokemon[] mons, byte lvl) // gets all moves learned at and before lvl including pre-evolutions
	{
		int[] preEvoIndex = {-1, -1};

		int nPre = 0;

		if (hasPre())
		{
			nPre++;

			byte[] preEvoByte = getPreEvo();
			preEvoIndex[0] = byteToValue(preEvoByte[0]) - 1;

			if (mons[preEvoIndex[0]].hasPre())
			{
				nPre++;
				preEvoByte = mons[preEvoIndex[0]].getPreEvo();
				preEvoIndex[1] = byteToValue(preEvoByte[0]) - 1;
			}
		}

		byte[][] lvlMoves = new byte[nPre + 1][];
		lvlMoves[0] = getMovesUpToLevel(lvl);

		for (int i = 0; i < nPre; i++)// cycle pre-evolutions
			lvlMoves[i+1] = mons[preEvoIndex[i]].getMovesUpToLevel(lvl);

		return lvlMoves;
	}

	public int getOldTier()
	{
		return this.oldTier;
	}
	
	public int getOldTypeTier()
	{
		return this.oldTypeTier;
	}
	
	public int getTotalExp(int lvl)
	{
		// calculates this Pokemon's total Exp. Points at level lvl
		int exp = 0;
		int n = lvl;
		
		switch (misc[6])
		{
			case (byte) 0x04: // fast
				exp = (int) round(((float) 4/5) * pow(n, 3));
				break;
			case (byte) 0x00: // medium fast
				exp = (int) pow(n, 3);
				break;
			case (byte) 0x03: // medium slow
				exp = (int) round(((float) 6/5) * pow(n, 3) - 15 * pow(n, 2) + 100 * n - 140);
				break;
			case (byte) 0x05: // slow
				exp = (int) round(((float) 5/4) * pow(n, 3));
				break;
			default: break;
		}
	
		return exp;
	}

	public int getHP()
	{
		int[] baseStats = byteToValue(base);
		return baseStats[0];
	}

	public int getAtk()
	{
		int[] baseStats = byteToValue(base);
		return baseStats[1];
	}
	
	public int getDef()
	{
		int[] baseStats = byteToValue(base);
		return baseStats[2];
	}
	
	public int getSpd()
	{
		int[] baseStats = byteToValue(base);
		return baseStats[3];
	}
	
	public int getSAtk()
	{
		int[] baseStats = byteToValue(base);
		return baseStats[4];
	}
	
	public int getSDef()
	{
		int[] baseStats = byteToValue(base);
		return baseStats[5];
	}
	
	/////////////////////////////////////////////
	// Set value methods
	/////////////////////////////////////////////	
	
	public void setBase(byte[] base)
	{
		this.base = base;
	}
	
	public void setType(byte[] typeByte)
	{
		this.typeByte = typeByte;
	}
	
	public void setMisc(int n, byte value)
	{
		this.misc[n] = value;
	}
	
	public void setMove(int n, byte[] move)
	{
		this.move[n] = move;
	}
	
	public void setMoves(byte[][] moves)
	{
		this.move = moves;
	}

	protected void resolveType()
	{
		for (int i = 0; i < typeByte.length; i++)
		{
			for (Type t : Type.values())
	        	if (t.byteIndex() == typeByte[i])
	        	{
	        		types[i] = t;
	        		break;
	        	}
		}
	}
	
	public void setPreEvo(byte[] preEvoIndex)
	{
		this.preEvo = preEvoIndex;
	}
	
	public void setEggMoves(byte[] eggMoves)
	{
		this.eggMove = eggMoves;
	}
	
	public void setEggMovesCarry(byte[] eggMoves)
	{
		this.eggMoveCarry = eggMoves;
	}
	
	public void setTier(int tier)
	{
		this.tier = tier;
	}
	
	public void setTypeTier(int typeTier)
	{
		this.typeTier = typeTier;
	}
	
	public void setName(byte[] name)
	{
		this.name = name;
	}
	
	public void setIcon(byte icon)
	{
		this.icon = icon;
	}

	public void setOldTier(int oldTier)
	{
		this.oldTier = oldTier;
	}
	
	public void setOldTypeTier(int oldTypeTier)
	{
		this.oldTypeTier = oldTypeTier;
	}
}