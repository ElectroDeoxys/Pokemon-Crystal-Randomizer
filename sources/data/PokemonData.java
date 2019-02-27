package data;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;

public class PokemonData
{
	private int trueIndex; // national index of Pokemon
	private byte[] base = new byte[6]; // base stats (hp, atk, def, spd, sat, sdf)
	private byte[] typeByte = new byte[2];
	
	// misc data (catch rate, base exp, item1, item2, gender ratio, 
	// egg steps, growth rate, egg group)
	private byte[] misc = new byte[8];
	
	private byte gfx;
	private byte[] tmhmByte; // TM/HM compatibility
	
	private byte[][] evo; // [evo number][method, method parameter(s)] (no species index yet)
	private int[] evoIndex; // array of evolution indexes
	private byte[][] move; // [move number][level, move index]
	private byte[] eggMove;
	private byte[] eggMoveCarry; // egg moves that it can carry from itself or pre-evolutions
	
	private int[] preEvo = new int[0]; // lists pre-evolution
	
	private byte[] name;
	private byte icon;
	
	private int tier = 0;
	private int typeTier = 0;

	private Type[] types = new Type[2];	
	
	public PokemonData(int trueIndex, byte[] base, byte[] typeByte, byte[] misc, byte gfx, byte[] tmhmByte, byte[][] evo, int[] evoIndex, byte[][] move)
	{
		this.trueIndex = trueIndex;
		
		this.base = base;
		this.typeByte = typeByte;
		this.misc = misc;
		this.gfx = gfx;
		this.tmhmByte = tmhmByte;
		
		this.evo = evo;
		this.evoIndex = evoIndex;
		this.move = move;
		
		this.eggMove = new byte[0]; // initialize egg moves
		this.eggMoveCarry = new byte[0];

		resolveType();
	}
	
	public Pokemon convertPokemon(int newIndex, byte[][] newEvo, byte[] newPreEvo)
	{	
		Pokemon mon = new Pokemon(newIndex, trueIndex, base, typeByte, misc, gfx, tmhmByte, newEvo, move);
		mon.setEggMoves(eggMove);
		mon.setEggMovesCarry(eggMoveCarry);
		mon.setPreEvo(newPreEvo);
		mon.setName(name);
		mon.setIcon(icon);
		return mon;
	}
	
	/////////////////////////////////////////////
	// Return value methods
	/////////////////////////////////////////////
	
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
		return this.types;
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
	
	public int[] getEvoIndexes()
	{
		return this.evoIndex;
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
	
	public int[] getPreEvo()
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
	
	public int getNBytes() // get number of bytes from all moves and evolutions 
	{
		int res = 0;
		
		if (this.hasEvos()) // if has evolutions
			for (int i = 0; i < evo.length; i++) // cycle evos
				res += evo[i].length + 1;
				
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
	
	public boolean isLegendary()
	{
		boolean out = false; // assume it isn't legendary
		
		if ((this.getBST() >= LEGENDARY_BST) && !(this.hasPre()))
			out = true;
		
		return out;
	}
	
	public int getTier()
	{
		return this.tier;
	}
	
	public int getTypeTier()
	{
		return this.typeTier;
	}	
	
	/////////////////////////////////////////////
	// Set value methods
	/////////////////////////////////////////////	
	
	public void setPreEvo(int[] preEvoIndex)
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
	
	public void setName(byte[] name)
	{
		this.name = name;
	}
	
	public void setTier(int tier)
	{
		this.tier = tier;
	}
	
	public void setTypeTier(int typeTier)
	{
		this.typeTier = typeTier;
	}
	
	public void setIcon(byte icon)
	{
		this.icon = icon;
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
}