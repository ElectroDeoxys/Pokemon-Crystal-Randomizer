package engine;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Pokemon;

class PokemonSorter
{
	private Pokemon[] mons;
	
	private byte[] starters;
	
	private byte[][] byStats; // [tier number][position]
	private byte[][] byType; // [type][position]
	private byte[][][] byTypeStats; // [type][tier][position]
	
	private byte[][] byEvoLines; // lists all the evolutionary lines [number of evo line][list of species in order of evolution]
	private byte[][] starterCand; // candidates for starters (below a BST threshold and able to evolve)
	private byte[][] starterCand3Stages; // candidates for starters (3-stage evolutions)
	
	PokemonSorter(Pokemon[] mons, byte[] starters) 
	{
		this.mons = mons;
		this.starters = starters;
		sortPokemon();
	}
	
	void sortPokemon()
	{	
		// list from lowest BST to highest in discrete tiers
		int span = (TOP_BST - BOT_BST) / (N_TIERS - 1);
		float typeTierMult = (float) N_TYPE_TIERS/N_TIERS; // multiplier between total tiers and type tiers
		
		ArrayList<ArrayList<Byte>> tierHolder = new ArrayList<ArrayList<Byte>>();
		ArrayList<ArrayList<Byte>> typeHolder = new ArrayList<ArrayList<Byte>>();
		ArrayList<ArrayList<ArrayList<Byte>>> typeTierHolder = new ArrayList<ArrayList<ArrayList<Byte>>>();
		
		for (int i = 0; i < N_TIERS; i++) // construct the 2-dimensional array tier list
			tierHolder.add(new ArrayList<Byte>());

		for (int i = 0; i < N_TYPES; i++) // construct the 2-dimensional type list
		{
			typeHolder.add(new ArrayList<Byte>());
			typeTierHolder.add(new ArrayList<ArrayList<Byte>>());
			for (int j = 0; j < N_TYPE_TIERS; j++) // construct the 2-dimensional array type tier list
				typeTierHolder.get(i).add(new ArrayList<Byte>());
		}
		
		ArrayList<ArrayList<Byte>> evoHolder = new ArrayList<ArrayList<Byte>>(); // for the evolutions
		ArrayList<ArrayList<Byte>> strHolder = new ArrayList<ArrayList<Byte>>(); // for the starters
		ArrayList<ArrayList<Byte>> strHolder3Stages = new ArrayList<ArrayList<Byte>>(); // for the 3-stage evos
		ArrayList<Byte> evoChecked = new ArrayList<Byte>(); // checked Pokemon for evolutions
		
		
		for (int i = 0; i < mons.length; i++)
		{
			// concerning the tiers
			
			int bst = mons[i].getBST();
			int indexTier = (int) min(floor(max((bst - (BOT_BST + 1)), 0) / span), N_TIERS - 1);
			int indexTypeTier = (int) floor((float) typeTierMult * indexTier);
			
			// min (max) are for BST under (above) the specified bounds
			
			tierHolder.get(indexTier).add(mons[i].getIndex());
			mons[i].setTier(indexTier);
			if (mons[i].getOldTier() < 0) mons[i].setOldTier(indexTier); // only change if it hasn't been set
			
			// concerning type sorting
			
			int[] typeOfMon = mons[i].getIndexTypes();
			
			typeHolder.get(typeOfMon[0]).add(mons[i].getIndex());
			
			if (!(typeOfMon[0] == 0 && typeOfMon[1] == 2)) // don't count Normal/Flying as Normal-type
				typeTierHolder.get(typeOfMon[0]).get(indexTypeTier).add(mons[i].getIndex());
			
			if (typeOfMon[0] != typeOfMon[1]) // if dual-type
			{
				typeHolder.get(typeOfMon[1]).add(mons[i].getIndex());
				
				if (!(typeOfMon[0] == 6 && typeOfMon[1] == 2)) // don't count Bug/Flying as Flying-type
					typeTierHolder.get(typeOfMon[1]).get(indexTypeTier).add(mons[i].getIndex());
			}
			
			mons[i].setTypeTier(indexTypeTier);
			if (mons[i].getOldTypeTier() < 0) mons[i].setOldTypeTier(indexTypeTier); // only change if it hasn't been set
			
			// concerning evo sorting
			
			if (!evoChecked.contains(mons[i].getIndex())) // if this Pokemon hasn't been processed yet
			{
				// start a new array for this line				
				ArrayList<Byte> thisEvoLine = new ArrayList<Byte>();
				int startIndex = i; // assume that this is the start in the evo line
				boolean is3Stage = false; // assume it's not 3-stage evolution
				
				// may have up to two pre-evolutions, so get to the bottom of the chain
				if (mons[i].hasPre()) // if this Pokemon has a pre-evolution
				{
					byte[] preEvo = mons[i].getPreEvo();
					int preEvoIndex = byteToValue(preEvo[0]) - 1;
					
					if (mons[preEvoIndex].hasPre()) //if this pre-evo still has pre-evo, start there
					{
						byte[] prePreEvo = mons[preEvoIndex].getPreEvo();
						startIndex = byteToValue(prePreEvo[0]) - 1;
					}
					else // the pre-evo is already the bottom, so update the starting index
						startIndex = preEvoIndex;
				}
				
				thisEvoLine.add(mons[startIndex].getIndex()); // add the starting Pokemon
				evoChecked.add(mons[startIndex].getIndex());
				
				if (mons[startIndex].hasEvos()) // explore all the evolution branches
				{
					byte[][] evoBytes = mons[startIndex].getEvos();
					for (int j = 0; j < evoBytes.length; j++) // explore all evolutions
					{
						thisEvoLine.add(evoBytes[j][evoBytes[j].length-1]); // add this evolution Pokemon
						evoChecked.add(evoBytes[j][evoBytes[j].length-1]);
						
						int thisEvo = byteToValue(evoBytes[j][evoBytes[j].length-1]) - 1;
						
						if (mons[thisEvo].hasEvos()) // check evolutions of the evo
						{
							byte[][] evoEvoBytes = mons[thisEvo].getEvos();
							for (int k = 0; k < evoEvoBytes.length; k++) // explore all evolutions
							{
								thisEvoLine.add(evoEvoBytes[k][evoEvoBytes[k].length-1]); // add this evolution Pokemon
								evoChecked.add(evoEvoBytes[k][evoEvoBytes[k].length-1]);
							}
							
							is3Stage = true; // set the 3-stage evolution line check to true
						}
					}
				}
				
				evoHolder.add(thisEvoLine); // add the evolution line to the array
				if ((mons[startIndex].hasEvos()) && (mons[startIndex].getBST() <= STARTER_BST)) 
					strHolder.add(thisEvoLine); // add this in only if it's 3-stage
				if ((is3Stage)  && (mons[startIndex].getBST() <= STARTER_BST))
					strHolder3Stages.add(thisEvoLine); // add this in only if it's 3-stage
			}
		}
		
		// convert the ArrayLists into byte arrays
		
		byte[][] tierByte = new byte[N_TIERS][];
		byte[][] typeByte = new byte[N_TYPES][];
		byte[][][] typeTierByte= new byte[N_TYPES][N_TYPE_TIERS][];
		
		for (int i = 0; i < N_TIERS; i ++)
			tierByte[i] = convertByteArray(tierHolder.get(i).toArray(new Byte[0]));

		for (int i = 0; i < N_TYPES; i ++)
		{
			typeByte[i] = convertByteArray(typeHolder.get(i).toArray(new Byte[0]));
			
			for (int j = 0; j < N_TYPE_TIERS; j ++)
				typeTierByte[i][j] = convertByteArray(typeTierHolder.get(i).get(j).toArray(new Byte[0]));
		}
		
		byte[][] evoByte = new byte[evoHolder.size()][];
		for (int i = 0; i < evoByte.length; i ++)
			evoByte[i] = convertByteArray(evoHolder.get(i).toArray(new Byte[0]));
		
		byte[][] strByte = new byte[strHolder.size()][];
		for (int i = 0; i < strByte.length; i ++)
			strByte[i] = convertByteArray(strHolder.get(i).toArray(new Byte[0]));
		
		byte[][] strByte3Stages = new byte[strHolder3Stages.size()][];
		for (int i = 0; i < strByte3Stages.length; i ++)
			strByte3Stages[i] = convertByteArray(strHolder3Stages.get(i).toArray(new Byte[0]));
		
		this.byStats = tierByte;
		this.byType = typeByte;
		this.byTypeStats = typeTierByte;
		this.byEvoLines = evoByte;
		this.starterCand = strByte;
		this.starterCand3Stages = strByte3Stages;
	}
	
	byte getSameTier(Pokemon mon, int typeIndex, boolean noLeg, boolean onlyEvolved, boolean forcedMix, byte ... prevMon)
	{
		// typeIndex = -1 for type-independent
		boolean bType = (typeIndex >= 0); // true if it's type exclusive
		
		int tierN = (bType) ? mon.getOldTypeTier() : mon.getOldTier();
		int trueIndex = mon.getTrueIndex();
		byte newMon;
		
		if (prevMon.length > 0) // if this argument exists
			newMon = getSameTier(tierN, trueIndex, typeIndex, noLeg, onlyEvolved, forcedMix, prevMon);
		else
			newMon = getSameTier(tierN, trueIndex, typeIndex, noLeg, onlyEvolved, forcedMix);
		
		return newMon;
	}
	
	byte getSameTier(int tierN, int trueIndex, int typeIndex, boolean noLeg, boolean onlyEvolved, boolean forcedMix, byte ... prevMon)
	{
		// typeIndex = -1 for type-independent
		boolean bType = (typeIndex >= 0); // true if it's type exclusive
		
		byte[] thisTier = (bType) ? byTypeStats[typeIndex][tierN] : byStats[tierN];
		int n = 1; // number of loop
		int lastIteration = tierN;
		boolean randDir = (random() * 100 < 50); // 50% chance of starting up/down

		byte[] validTier = getValidTier(thisTier, trueIndex, noLeg, onlyEvolved, forcedMix, prevMon);
		
		while (validTier.length == 0) // find a tier with Pokemon in it
		{
			lastIteration = tierN;
			
			if (randDir)
				tierN += pow(-1,n) * n;
			else
				tierN -= pow(-1,n) * n;
			
			tierN = (bType) ? min(max(tierN, 0), N_TYPE_TIERS - 1) : min(max(tierN, 0), N_TIERS - 1); // constrain index search
			thisTier = (bType) ? byTypeStats[typeIndex][tierN] : byStats[tierN];
			validTier = getValidTier(thisTier, trueIndex, noLeg, onlyEvolved, forcedMix, prevMon);
			
			if (((bType) && (tierN == 0 || tierN == N_TYPE_TIERS - 1))
				|| ((!bType) && (tierN == 0 || tierN == N_TIERS - 1)))
				break; // couldn't find it through this method
		}
		
		if (validTier.length == 0) // continue search if interrupted
		{
			if (tierN == 0) // it means the search hit the bottom without looking tiers above
			{
				tierN = lastIteration; // fall back on the previous iteration
				
				while (validTier.length == 0)
				{
					tierN++;
					thisTier = (bType) ? byTypeStats[typeIndex][tierN] : byStats[tierN];
					validTier = getValidTier(thisTier, trueIndex, noLeg, onlyEvolved, forcedMix, prevMon);
				}
			}
			else // it means the search hit the bottom without looking tiers below
			{				
				tierN = lastIteration; // fall back on the previous iteration
				
				while (validTier.length == 0)
				{
					tierN--;
					thisTier = (bType) ? byTypeStats[typeIndex][tierN] : byStats[tierN];
					validTier = getValidTier(thisTier, trueIndex, noLeg, onlyEvolved, forcedMix, prevMon);
				}
			}
		}
		
		int randPos; // get a random position in the tier list for another Pokemon
		int thisTrueIndex;
		boolean thisLeg;
			
		randPos = (int) floor(random() * validTier.length);
		
		return validTier[randPos];
	}
	
	byte getSameTier(Pokemon mon, int[] typeIndex, boolean noLeg) // for multiple types
	{
		// get the type array and return Pokemon of random type
		// all types are equally likely to get chosen
		
		byte out = (byte) 0x00;
		int len = typeIndex.length;

		for (int i = 0; i < len; i++)
		{
			float r = (float) random();
			float p = (float) 1/(len - i);

			if (r < p) // probability is dependent on how many types are left
			{
				out = getSameTier(mon, typeIndex[i], noLeg, false, false);
				break; // end the search
			}
		}

		return out;
	}
	
	byte getSameTier(int tierN, int trueIndex, int[] typeIndex, boolean noLeg) // for multiple types
	{
		// get the type array and return Pokemon of random type
		// all types are equally likely to get chosen
		
		byte out = (byte) 0x00;
		int len = typeIndex.length;

		for (int i = 0; i < len; i++)
		{
			float r = (float) random();
			float p = (float) 1/(len - i);

			if (r < p) // probability is dependent on how many types are left
			{
				out = getSameTier(tierN, trueIndex, typeIndex[i], noLeg, false, false);
				break; // end the search
			}
		}

		return out;
	}
	
	private byte[] getValidTier(byte[] tier, int trueIndex, boolean noLeg, boolean onlyEvolved, boolean forcedMix, byte ... prevMon)
	{
		// gets a valid tier to get a random Pokemon from, given the conditions
		ArrayList<Byte> validTierHolder = new ArrayList<Byte>();
		
		for (int i = 0; i < tier.length; i++)
		{
			if ((mons[byteToValue(tier[i]) - 1].isLegendary()) && (noLeg)) // if it's legendary and no legendaries are allowed
				continue;
			
			if (mons[byteToValue(tier[i]) - 1].getTrueIndex() == trueIndex) // if it's the same Pokemon
				continue;
				
			if ((onlyEvolved) && !(mons[byteToValue(tier[i]) - 1].hasPre())) // if it isn't evolved
				continue;
				
			if (forcedMix)
			{	
				byte[] testParty = new byte[prevMon.length + 1]; // get a new list with this Pokemon to test
				for (int j = 0; j < prevMon.length; j++)
					testParty[j] = prevMon[j];
				testParty[prevMon.length] = tier[i]; // append this new byte to the last slot of the list
				
				if (isTypeRedundant(false, testParty))
					continue;
			}
			
			validTierHolder.add(tier[i]);
		}
		
		byte[] out = convertByteArray(validTierHolder.toArray(new Byte[0]));
		return out;
	}
	
	byte[] getPokemonOfType(int n) // for one single type
	{
		return this.byType[n];
	}
	
	int getPokemonOldTier(byte pokeByte, boolean bType)
	{
		Pokemon mon = mons[byteToValue(pokeByte) - 1];
		int out = (bType) ? mon.getOldTypeTier() : mon.getOldTier();
		return out;
	}
	
	byte[] getPokemonOfType(int[] n) // for multiple types
	{
		int len = 0; // length of all type arrays
		for (int i : n) // cycle types
			len += byType[i].length;
		
		byte[] list = new byte[len];
		int c = 0; // keep track of number of entries
		
		for (int i = 0; i < n.length; i++) // cycle types
			for (int j = 0; j < byType[n[i]].length; j++)
			{
				list[c] = byType[n[i]][j];
				c++;
			}
		
		return list;
	}
	
	void printTiers(Pokemon[] mons)
	{
		for (int i = 0; i < byStats.length; i++)
		{
			System.out.println("Tier " + i + ":");
			
			for (int j = 0; j < byStats[i].length; j++)
				System.out.println(mons[byStats[i][j] - 1].getBST());
		}
	}
	
	byte[] getRandomStarters(int starterKind)
	{
		// gives three random Pokemon as starters
		// if starterKind == 0 : completely random
		// if starterKind == 1 : starters with at least one evolution and below STARTER_BST
		// if starterKind == 2 : only give out the starting Pokemon of a 3-stage evolution
		// always returns the lowest Pokemon of the evo-line
		// always returns different Pokemon with different types
		
		if (starterKind == 0)
			starters = generateRandomStarters(byEvoLines);
		else if (starterKind == 1)
			starters = generateRandomStarters(starterCand);
		else if (starterKind == 2)
			starters = generateRandomStarters(starterCand3Stages);
		
		return starters;
	}
	
	byte[] generateRandomStarters(byte[][] evoLineList)
	{
		byte[] out = new byte[3];
		int randLine;

		// pick the first one
		randLine = (int) floor(random()*(evoLineList.length)); // random evolution line
		out[0] = evoLineList[randLine][0];
		
		// pick the second one different from the first and of different type
		do
		{
			randLine = (int) floor(random()*(evoLineList.length)); // random evolution line
			out[1] = evoLineList[randLine][0];
		} while ((out[0] == out[1]) || (isTypeRedundant(false, out[0], out[1])));
		
		// pick the third one different from the first two and of different type
		do
		{
			do
			{
				randLine = (int) floor(random()*(evoLineList.length)); // random evolution line
				out[2] = evoLineList[randLine][0];
			} while ((out[1] == out[2]) || (areSameType(mons[byteToValue(out[1]) - 1], mons[byteToValue(out[2]) - 1])));
		} while ((out[0] == out[2]) || (isTypeRedundant(false, out[0], out[1], out[2])));
		
		return out;
	}
	
	private boolean areSameType(Pokemon mon1, Pokemon mon2)
	{
		// gets two Pokemon and decides if they share a type
		byte[] types1 = mon1.getTypes();
		byte[] types2 = mon2.getTypes();
		boolean out = (types1[0] == types2[0] || types1[0] == types2[1] || types1[1] == types2[0] || types1[1] == types2[1]);
		return out;
	}
	
	private boolean isTypeRedundant(boolean total, byte ... monBytes)
	{
		// decides if there is type redundancy in a list of Pokemon
		// if (total), then will return true if any types are repeated
		// if (!total), then will return true only if both Pokemon's types are repeated
		
		Pokemon[] monList = new Pokemon[monBytes.length];
		for (int i = 0; i < monBytes.length; i++) // translate bytes to actual Pokemon classes
			monList[i] = mons[byteToValue(monBytes[i]) - 1];
		
		boolean out = false; // assume it's not repeated
		
		byte[][] types = new byte[monList.length][2]; // two types per Pokemon
		
		for (int i = 0; i < monList.length; i++) // cycle Pokemon list
			types[i] = monList[i].getTypes();
		
		if (total) // all it takes is one repeated type
		{
			for (int i = 0; i < monList.length; i++) // cycle Pokemon list
			{
				for (int j = i + 1; j < monList.length; j++) // cycle all Pokemon ahead of list
				{
					if ((types[i][0] == types[j][0]) || (types[i][0] == types[j][1])) // compare first type
						out = true;
					if ((types[i][1] == types[j][0]) || (types[i][1] == types[j][1])) // compare second type
						out = true;
						
					if (out) break; // exit loop if repetition found
				}
				
				if (out) break;
			}
		}
		else // only true if both a Pokemon's types are redundant
		{
			for (int i = 0; i < monList.length; i++) // cycle Pokemon list
			{
				boolean test1 = false;
				boolean test2 = false;
				
				for (int j = 0; j < monList.length; j++) // first type
				{
					if (i == j) continue; // skip comparing with self
					
					if ((types[i][0] == types[j][0]) || (types[i][0] == types[j][1])) // compare first type
						test1 = true;
						
					if (test1) break; // exit loop if repetition found
				}
				
				for (int j = 0; j < monList.length; j++) // second type
				{
					if (i == j) continue; // skip comparing with self
					
					if ((types[i][1] == types[j][0]) || (types[i][1] == types[j][1])) // compare second type
						test2 = true;
						
					if (test2) break; // exit loop if repetition found
				}
				
				if ((test1) && (test2)) out = true; // only get a true result if both tests pass
				
				if (out) break;
			}
		}
			
		return out;
	}
	
	byte[][][] generateRivalTeams(byte[][] finalRivalTeam, int[][][] levels, boolean noLeg)
	{
		// generates Rival teams that have persistent Pokemon throughout the battles
		// depending on starter chosen by the player, and which is forced to be mixed
		// Pokemon evolve according to level and all have evolutionary lines except possibly for the final lead
		// Rival's starter is chosen from the in-game starter
		
		byte[][][] rivalTeams = new byte[INDEX_RIVAL.length][RIVAL_PARTY_SIZES.length][];
		byte[] finalTeam = new byte[RIVAL_PARTY_SIZES[RIVAL_PARTY_SIZES.length - 1]];
		
		for (int i = 0; i < INDEX_RIVAL.length; i++) // cycle starters (each starter will generate a different team)
		{
			// decide the evolutionary line of the starter first
			byte[] starterSlot = getEvoLine(starters[i], -1);
			
			// generate the final team first
			ArrayList<Byte> prevMonList = new ArrayList<Byte>(); // keep track of previous Pokemon in team
			prevMonList.add(starterSlot[starterSlot.length - 1]); // add starter evo to take into account its types
			
			// initialize the other slots 
			byte[][] monSlots = new byte[finalTeam.length - 1][]; 
			
			for (int j = 0; j < finalTeam.length; j++) // cycle through the final battle party
			{
				if (j < finalTeam.length - 1) // not the starter slot
				{
					boolean isLead = (j == 0); // forced evolved unless it's the lead
					byte[] prevMonArray = convertByteArray(prevMonList.toArray(new Byte[0]));
					Pokemon thisMon = mons[byteToValue(finalRivalTeam[i][j]) - 1];
					
					finalTeam[j] = getSameTier(thisMon, -1, noLeg, (!isLead), true, prevMonArray);
					monSlots[j] = getEvoLine(finalTeam[j], -1);
					
					// may result in an unevolved Pokemon, so change it to be the final form in the last battle
					//finalTeam[j] = monSlots[j][monSlots[j].length - 1];
					prevMonList.add(finalTeam[j]);
					
				}
				else
					finalTeam[j] = starterSlot[starterSlot.length - 1]; // add in the evolved starter
			}
			
			// final team is formed, now to generate the earlier teams based on the result
			
			// initialize the byte arrays for each battle
			for (int j = 0; j < rivalTeams[i].length; j++)
				rivalTeams[i][j] = new byte[RIVAL_PARTY_SIZES[j]];
			
			// battle 0
			rivalTeams[i][0][0] = decideEvo(starterSlot, levels[i][0][0]);
			// battle 1
			rivalTeams[i][1][0] = decideEvo(monSlots[3], levels[i][1][0]);
			rivalTeams[i][1][1] = decideEvo(monSlots[1], levels[i][1][1]);
			rivalTeams[i][1][2] = decideEvo(starterSlot, levels[i][1][2]);
			// battle 2
			rivalTeams[i][2][0] = decideEvo(monSlots[3], levels[i][2][0]);
			rivalTeams[i][2][1] = decideEvo(monSlots[2], levels[i][2][1]);
			rivalTeams[i][2][2] = decideEvo(monSlots[1], levels[i][2][2]);
			rivalTeams[i][2][3] = decideEvo(starterSlot, levels[i][2][3]);
			// battle 3
			rivalTeams[i][3][0] = decideEvo(monSlots[1], levels[i][3][0]);
			rivalTeams[i][3][1] = decideEvo(monSlots[2], levels[i][3][1]);
			rivalTeams[i][3][2] = decideEvo(monSlots[3], levels[i][3][2]);
			rivalTeams[i][3][3] = decideEvo(monSlots[0], levels[i][3][3]);
			rivalTeams[i][3][4] = decideEvo(starterSlot, levels[i][3][4]);
			// battle from 4 upwards
			for (int j = 4; j < RIVAL_PARTY_SIZES.length; j++)
			{
				rivalTeams[i][j][0] = decideEvo(monSlots[0], levels[i][j][0]);
				rivalTeams[i][j][1] = decideEvo(monSlots[1], levels[i][j][1]);
				rivalTeams[i][j][2] = decideEvo(monSlots[2], levels[i][j][2]);
				rivalTeams[i][j][3] = decideEvo(monSlots[3], levels[i][j][3]);
				rivalTeams[i][j][4] = decideEvo(monSlots[4], levels[i][j][4]);
				rivalTeams[i][j][5] = decideEvo(starterSlot, levels[i][j][5]);
			}
		}
		
		return rivalTeams;
	}
	
	byte[] evolveTeam(byte[] monTeam, int[] lvls)
	{
		// makes Pokemon team evolve given their levels
		byte[] evoTeam = new byte[monTeam.length];
		
		for (int i = 0; i < monTeam.length; i++) // cycle team
			evoTeam[i] = decideEvo(getEvoLine(monTeam[i], -1), lvls[i]);
			
		return evoTeam;
	}
	
	private byte[] getEvoLine(byte monByte, int branch)
	{
		// gets the evolutionary line of a Pokemon in order
		// if there's a branch ahead, branch dictates what should happen
		// branch = 0, 1, ... : the appropriate branch is chosen
		// branch = -1 : a random branch is chosen
		
		int evoIndex = -1; // find the evolutionary line index
		int evoPos = -1; // find the position in that evo line
		
		for (int i = 0; i < byEvoLines.length; i++) // cycle evo-lines
			for (int j = 0; j < byEvoLines[i].length; j++)
				if (monByte == byEvoLines[i][j])
				{
					evoIndex = i;
					evoPos = j;
					break;
				}
		
		ArrayList<Byte> outList = new ArrayList<Byte>();
		
		for (int i = 0; i < byEvoLines[evoIndex].length;) // cycle line members
		{
			outList.add(byEvoLines[evoIndex][i]);
			Pokemon thisMon = mons[byteToValue(byEvoLines[evoIndex][i]) - 1];
			
			if (!thisMon.hasEvos())
				break; // no more evos to look into
			
			// figure out whether to continue normally or skip to a particular branch
			byte[][] thisEvos = thisMon.getEvos();
			
			if (thisEvos.length == 1) // only one possible evolution
				i++;
			else // reached a branch
			{
				if (i < evoPos) // the line has already been determined by input Pokemon
					i = evoPos;
				else // need to choose a branch now
				{
					int nJumps = (branch >= 0) ? branch : (int) floor(random() * thisEvos.length);
					
					i++;
					for (int j = 0; j < nJumps; j++) // jump accordingly to the proper branch
					{
						if (mons[byteToValue(byEvoLines[evoIndex][i]) - 1].hasEvos())
							i += 2;
						else
							i++;
					}
				}
			}
		}
		
		byte[] out = convertByteArray(outList.toArray(new Byte[0]));
		return out;
	}
	
	private byte decideEvo(byte[] evoLine, int lvl)
	{
		// decides what evolutionary form is appropriate for level lvl
		// evolutionary methods other than leveling up are taken to be OTHER_METHODS_LEVEL
		
		int c = 0;
		// start from the bottom and build up
		for (int i = 0; i < evoLine.length - 1; i++)
		{
			Pokemon thisMon = mons[byteToValue(evoLine[i]) - 1];
			byte[][] evos = thisMon.getEvos();
			boolean evolve = false; // assume it shouldn't evolve
			int branchIndex = getBranchIndex(evos, evoLine[i+1]);
			
			if ((evos[branchIndex][0] == (byte) 0x01) || (evos[branchIndex][0] == (byte) 0x05)) // level evolution
				evolve = (lvl >= byteToValue(evos[branchIndex][1]));
			else // other methods
			{
				// check if the its evolution also evolves
				if (mons[byteToValue(evoLine[i+1]) - 1].hasEvos())
					evolve = (lvl >= OTHER_METHODS_LEVEL_LOWER);
				else
					evolve = (lvl >= OTHER_METHODS_LEVEL);
			}
				
			if (!evolve) break;
			c++;
		}
		
		byte out = evoLine[c];
		return out;
	}
	
	private int getBranchIndex(byte[][] evos, byte monByte)
	{
		// find the branch index to get monByte
		int out = 0;
		
		for (int i = 0; i < evos.length; i++)
			if (monByte == evos[i][evos[i].length - 1])
			{
				out = i;
				break;
			}
			
		return out;
	}
	
	byte[] getMoveset(MoveSorter moveSorter, byte monByte, byte lvl, boolean extraCust)
	{
		int thisMon = byteToValue(monByte) - 1;
		byte[] lvlMoves = mons[thisMon].getMovesUpToLevel(lvl);
		byte[] newMoves = new byte[4];

		if (extraCust) // get customized moveset
		{
			byte[] eggMoves = mons[thisMon].getEggMovesCarry(); // egg moves it can carry
			boolean[] moveComp = mons[thisMon].getCompatibilities();
			
			newMoves = moveSorter.getTierMoveset(lvlMoves, eggMoves, moveComp, mons[thisMon].getTypes(), mons[thisMon].getBase(), lvl);
		}
		else // apply only level-up moves in reverse order
			for (int k = 0; k < 4; k++)
			{
				if (k < lvlMoves.length)
					newMoves[k] = lvlMoves[k];
				else
					newMoves[k] = (byte) 0x00;
			}
			
		return newMoves;
	}
}