package data;

import java.util.ArrayList;

import static data.Constants.*;

public class Route
{
	private int index; // index of the route/area
	
	private byte[] poke; // index of pokemon 
	private byte[] lvl; // index of levels
	
	private Byte[] pokeList; // lists each Pokémon once
	private Byte[][] pokeSlot; // lists the position of each Pokemon listed
	
	public static int[] indexBreaks = getIndexBreaks(); // store the indices where routes change
	
	public Route(int index, byte[] poke, byte[] lvl)
	{
		this.index = index;
		this.poke = poke;
		this.lvl = lvl;
		setAllSlots(poke);
	}
	
	private static int[] getIndexBreaks()
	{
		int[] indexBreaks = new int[N_WILD_ROUTES.length];
		
		for (int i = 0; i < N_WILD_ROUTES.length; i++)
		{
			indexBreaks[i] = 0;
			
			for (int j = 0; j <= i; j++)
				indexBreaks[i] += N_WILD_ROUTES[j];
		}
		
		return indexBreaks;
	}
	
	public int getIndex()
	{
		return this.index;
	}
	
	public int getOffset()
	{
		return convertIndexToOffset(this.index);
	}
	
	public static int convertIndexToOffset(int n)
	{
		int offset;
		
		if (n < indexBreaks[0]) 	 // Johto land route
			offset = OFFSET_WILD[0] + N_BYTES_WILD[0] * n;
		else if (n < indexBreaks[1]) // Johto water route
			offset = OFFSET_WILD[1] + N_BYTES_WILD[1] * (n - indexBreaks[0]);
		else if (n < indexBreaks[2]) // Kanto land route
			offset = OFFSET_WILD[2] + N_BYTES_WILD[0] * (n - indexBreaks[1]);
		else						 // Kanto water route
			offset = OFFSET_WILD[3] + N_BYTES_WILD[1] * (n - indexBreaks[2]);

		return offset;
	}	
	
	public byte getPokeByte(int n)
	{
		return this.poke[n];
	}
	
	public byte getLvl(int n)
	{
		return this.lvl[n];
	}
	
	public void setAllSlots(byte[] poke)
	{
		ArrayList<Byte> pokeChecked = new ArrayList<Byte>(); // list to keep track of checked Pokemon
		
		for (byte i : poke)
			if (!pokeChecked.contains(i)) // hasn't checked yet
				pokeChecked.add(i);
		
		Byte[] holderArray = pokeChecked.toArray(new Byte[0]);
		this.pokeList = holderArray;
		
		Byte[][] pokeSlot = new Byte[pokeList.length][];
		
		for (byte i = 0; i < this.pokeList.length; i++)
		{
			ArrayList<Byte> slotPos = new ArrayList<Byte>(); // position of each species
			
			for (byte j = i; j < poke.length; j++)
				if (this.pokeList[i] == poke[j])
					slotPos.add(j); // add the position of the Pokemon
			
			holderArray = slotPos.toArray(new Byte[0]);
			pokeSlot[i] = holderArray;
		}
		
		this.pokeSlot = pokeSlot;
	}
	
	public int getTotalSlots() // returns the number of all the individual Pokemon slots
	{
		return this.poke.length;
	}

	public int getNumberSpecies() // returns the number of different species found
	{
		return this.pokeList.length;
	}
	
	public byte getPokeSpeciesByte(int n)
	{
		return this.pokeList[n];
	}
	
	// set Pokemon data individually
	public void setPoke(int n, int pokeIndex)
	{
		this.poke[n] = valueToByte(pokeIndex);
	}
	
	public void setLvl(int n, byte lvl)
	{
		this.lvl[n] = lvl;
	}
	
	// set Pokemon data in bulk
	public void setPokes(byte[] poke)
	{
		this.poke = poke;
	}
	
	public void setLvls(byte[] lvl)
	{
		this.lvl = lvl;
	}
	
	// set Pokemon in slots
	public void setSlot(int pokeSlotPos, int pokeIndex)
	{
		this.pokeList[pokeSlotPos] = valueToByte(pokeIndex); // substitute it in the species list
		
		for (int i = 0; i < this.pokeSlot[pokeSlotPos].length; i++) // loop through the slots of this Pokemon in the list
			this.poke[pokeSlot[pokeSlotPos][i]] = valueToByte(pokeIndex); // substitute each position listed for this species
	}
	
	public int getLandIndex() // decide if this route is land or water
	{
		return getLandIndex(index);
	}
	
	public static int getLandIndex(int n) // decide if a route index is land or water
	{
		// 0 is land, 1 is water
		
		int lIndex = 0;
		
		if ((n < indexBreaks[0]) || (n >= indexBreaks[1] && n < indexBreaks[2]))
			lIndex = 0;
		else 
			lIndex = 1;
		
		return lIndex;
	}
}