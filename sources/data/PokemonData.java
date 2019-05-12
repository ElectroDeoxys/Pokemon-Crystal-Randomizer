package data;

import static data.Constants.*;

public class PokemonData extends Pokemon
{
	private int[] evoIndex; // array of evolution indexes
	private int[] preEvoIndex = new int[0]; // lists pre-evolution
	
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
	
	public PokemonGame convertPokemon(int newIndex, byte[][] newEvo, byte[] newPreEvo)
	{	
		PokemonGame mon = new PokemonGame(newIndex, trueIndex, base, typeByte, misc, gfx, tmhmByte, newEvo, move);
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
	
        @Override
	public int getIntIndex()
	{
		return getTrueIndex();
	}
	
	public int[] getEvoIndex()
	{
		return this.evoIndex;
	}

        @Override
	public int[] getEvoInt()
	{
		return getEvoIndex();
	}
	
	public int[] getPreEvoIndex()
	{
		return this.preEvoIndex;
	}

        @Override
	public int[] getPreEvoInt()
	{
		return getPreEvoIndex();
	}

	/////////////////////////////////////////////
	// Set value methods
	/////////////////////////////////////////////	

	public void setPreEvoIndex(int[] preEvoIndex)
	{
		this.preEvoIndex = preEvoIndex;
		this.preEvo = valueToByte(preEvoIndex);
	}
}