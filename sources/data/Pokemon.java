package data;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;

public class Pokemon
{
	private int index; // in-game index of Pokemon
	private int trueIndex; // national index of Pokemon
	private byte[] base = new byte[6]; // base stats (hp, atk, def, spd, sat, sdf)
	private byte[] typeByte = new byte[2];
	
	// misc data (catch rate, base exp, item1, item2, gender ratio, 
	// egg steps, growth rate, egg group)
	private byte[] misc = new byte[8];
	
	private byte gfx;
	private byte[] tmhmByte; // TM/HM compatibility
	
	private byte[][] evo; // [evo number][method, method parameter(s), species index]
	private byte[][] move; // [move number][level, move index]
	private byte[] eggMove;
	private byte[] eggMoveCarry; // egg moves that it can carry from itself or pre-evolutions
	
	private int tier = 0; // tier list to compare strengths
	private int typeTier = 0; // tier list to compare strengths for same type
	private boolean[] tmhmComp = new boolean[N_TM + N_HM + N_MOVE_TUTOR];
	
	private byte[] preEvo = new byte[0]; // lists pre-evolution
	private byte[] name;
	private byte icon;
	
	private int oldTier = -1; // tier list to compare strengths from Pokemon it replaced in the old Pokedex
	private int oldTypeTier = -1; // tier list to compare strengths for same type from Pokemon it replaced in the old Pokedex

	private Type[] types = new Type[2];	
	
	public Pokemon(int index, int trueIndex, byte[] base, byte[] typeByte, byte[] misc, byte gfx, byte[] tmhmByte, byte[][] evo, byte[][] move)
	{
		this.index = index;
		this.trueIndex = trueIndex;
		
		this.base = base;
		this.typeByte = typeByte;
		this.misc = misc;
		this.gfx = gfx;
		this.tmhmByte = tmhmByte;
		
		this.evo = evo;
		this.move = move;
		
		this.eggMove = new byte[0]; // initialize egg moves
		this.eggMoveCarry = new byte[0];
		
		convertCompatibilities();
		resolveType();
	}
	
	/////////////////////////////////////////////
	// Return value methods
	/////////////////////////////////////////////
	
	public byte getIndex()
	{
		byte out = (byte) (this.index + 0x1);
		return out;
	}
	
	public int getIntIndex()
	{
		return this.index + 1;
	}
	
	public int getTrueIndex()
	{
		return this.trueIndex;
	}
	
	public byte[] getBase()
	{
		return this.base;
	}
	
	public byte[] getTypes()
	{
		return this.typeByte;
	}

	public Type[] getIndexTypes()
	{
		return types;
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
	
	public byte[] getEggMoves()
	{
		return this.eggMove;
	}
	
	public byte[] getEggMovesCarry()
	{
		return this.eggMoveCarry;
	}
	
	public int getOffset1() // offset to first set of data (base stats)
	{
		return convertIndexToOffset(this.index);
	}	
	
	public static int convertIndexToOffset(int n)
	{
		return OFFSET_POKEMON_1 + 0x20 * n; // offset to first set of data (base stats)
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

	public byte[] getMovesUpToLevel(byte lvl) // gets all moves learned at and before lvl
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
	
	public int getOldTier()
	{
		return this.oldTier;
	}
	
	public int getOldTypeTier()
	{
		return this.oldTypeTier;
	}
	
	public byte getIcon()
	{
		return this.icon;
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
	
	private void convertCompatibilities()
	{
		for (int i = 0; i < tmhmComp.length; i++)
		{
			byte thisBit = getBit(tmhmByte[(int) floor(i/8)], i % 8);

			if (thisBit == 0)
				tmhmComp[i] = false;
			else
				tmhmComp[i] = true;
		}
	}

	private void resolveType()
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
	
	public void setCompatibility(int n, boolean isCompatible)
	{
		this.tmhmComp[n] = isCompatible;
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
	
	public void setOldTier(int oldTier)
	{
		this.oldTier = oldTier;
	}
	
	public void setOldTypeTier(int oldTypeTier)
	{
		this.oldTypeTier = oldTypeTier;
	}
	
	public void setName(byte[] name)
	{
		this.name = name;
	}
	
	public void setIcon(byte icon)
	{
		this.icon = icon;
	}
}