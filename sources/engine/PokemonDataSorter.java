package engine;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;
import data.PokemonData;

class PokemonDataSorter
{
	private PokemonData[] monData;
	
	private int[][] byType; // [type][position]
	private int[][] byEvoLines; // lists all the evolutionary lines [number of evo line][list of species in order of evolution]
	
	PokemonDataSorter(PokemonData[] monData) 
	{
		this.monData = monData;
		sortPokemon();
	}
	
	void sortPokemon()
	{
		// list from lowest BST to highest in discrete tiers
		int span = (TOP_BST - BOT_BST) / (N_TIERS - 1);
		float typeTierMult = (float) N_TYPE_TIERS/N_TIERS; // multiplier between total tiers and type tiers
		
		ArrayList<ArrayList<Integer>> typeHolder = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> evoHolder = new ArrayList<ArrayList<Integer>>(); // for the evolutions
		ArrayList<Integer> evoChecked = new ArrayList<Integer>(); // checked Pokemon for evolutions
		
		for (int i = 0; i < N_TYPES; i++) // construct the 2-dimensional type list
			typeHolder.add(new ArrayList<Integer>());
		
		for (int i = 0; i < monData.length; i++)
		{
			// concerning type sorting
			
			Type[] typeOfMon = monData[i].getIndexTypes();
			
			typeHolder.get(typeOfMon[0].intIndex()).add(monData[i].getTrueIndex());
			
			if (typeOfMon[0] != typeOfMon[1]) // if dual-type
				typeHolder.get(typeOfMon[1].intIndex()).add(monData[i].getTrueIndex());
			
			// concerning evo sorting
			
			if (!evoChecked.contains(monData[i].getTrueIndex())) // if this Pokemon hasn't been processed yet
			{
				// start a new array for this line				
				ArrayList<Integer> thisEvoLine = new ArrayList<Integer>();
				int startIndex = i + 1; // assume that this is the start in the evo line
				
				// may have up to two pre-evolutions, so get to the bottom of the chain
				if (monData[i].hasPre()) // if this Pokemon has a pre-evolution
				{
					int[] preEvo = monData[i].getPreEvo();
					int preEvoIndex = preEvo[0];
					
					if (monData[preEvoIndex - 1].hasPre()) //if this pre-evo still has pre-evo, start there
					{
						int[] prePreEvo = monData[preEvoIndex - 1].getPreEvo();
						startIndex = prePreEvo[0];
					}
					else // the pre-evo is already the bottom, so update the starting index
						startIndex = preEvoIndex;
				}
				
				thisEvoLine.add(monData[startIndex - 1].getTrueIndex()); // add the starting Pokemon
				evoChecked.add(monData[startIndex - 1].getTrueIndex());
				
				if (monData[startIndex - 1].hasEvos()) // explore all the evolution branches
				{
					int[] evoArray = monData[startIndex - 1].getEvoIndexes();
					
					for (int j = 0; j < evoArray.length; j++) // explore all evolutions
					{
						thisEvoLine.add(evoArray[j]); // add this evolution Pokemon
						evoChecked.add(evoArray[j]);
						
						if (monData[evoArray[j] - 1].hasEvos()) // check evolutions of the evo
						{
							int[] evoevoArray = monData[evoArray[j] - 1].getEvoIndexes();
							for (int k = 0; k < evoevoArray.length; k++) // explore all evolutions
							{
								thisEvoLine.add(evoevoArray[k]); // add this evolution Pokemon
								evoChecked.add(evoevoArray[k]);
							}
						}
					}
				}
				
				evoHolder.add(thisEvoLine); // add the evolution line to the array
			}
			
			// concerning tiers
			int bst = monData[i].getBST();
			int indexTier = (int) min(floor(max((bst - (BOT_BST + 1)), 0) / span), N_TIERS - 1);
			int indexTypeTier = (int) floor((float) typeTierMult * indexTier);
			
			monData[i].setTier(indexTier);
			monData[i].setTypeTier(indexTypeTier);
		}
		
		// convert the ArrayLists into int arrays
		
		int[][] typeInt = new int[N_TYPES][];

		for (int i = 0; i < N_TYPES; i ++)
			typeInt[i] = convertIntArray(typeHolder.get(i).toArray(new Integer[0]));
		
		int[][] evoInt = new int[evoHolder.size()][];
		
		for (int i = 0; i < evoInt.length; i ++)
			evoInt[i] = convertIntArray(evoHolder.get(i).toArray(new Integer[0]));
		
		this.byType = typeInt;
		this.byEvoLines = evoInt;
	}
	
	int findEvoLineContaining(int n)
	{
		// finds the evolutionary line containing Pokemon with index n
		int out = -1;
		
		for (int i = 0; i < byEvoLines.length; i++)
			for (int j = 0; j < byEvoLines[i].length; j++)
				if (byEvoLines[i][j] == n)
				{
					out = i;
					break;
				}
		
		return out;
	}
	
	int[][] getEvoLines()
	{
		return this.byEvoLines;
	}
	
	int[] getPokemonOfType(Type type) // for one single type
	{
		return this.byType[type.intIndex()];
	}
}