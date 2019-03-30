package engine;

import java.util.ArrayList;
import java.io.IOException;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Move;
import data.Trainer;
import data.PokemonGame;

class TrainerEditor
{
	private Trainer[] trainers;
	private PokemonGame[] mons;
	private PokemonSorter monSorter;
	
	TrainerEditor(RomReader romReader, PokemonGame[] mons) throws IOException
	{
		this.trainers = romReader.readRomTrainers();
		this.mons = mons;
		this.monSorter = monSorter;
	}
	
	void buffKanto(PokemonSorter monSorter, MoveSorter moveSorter)
	{
		// matches Kanto Trainers with Champion level
		// adds up to 3 new Pokemon in each roster
		// new Pokemon are same tier as the other established ones (randomly picked)
		
		for (int i : INDEX_KANTO_TRAINERS)
		{
			byte kind = trainers[i].getKind();
			
			int size = trainers[i].getPartySize();
			int[] lvl = new int[size];
			byte[] party = new byte[size];
			byte[] items = new byte[size];
			byte[][] moves = new byte[size][4];
			int[] partyTiers = new int[size];
			
			int newSize = (int) min(size + floor(random() * 3), 6);
			int[] newLvl = new int[newSize];
			int[] newParty = new int[newSize];
			byte[] newItems = new byte[newSize];
			byte[][] newMoves = new byte[newSize][4];
			
			for (int j = 0; j < size; j++)
			{
				byte[] b = trainers[i].getPokeBytes(j);
				
				lvl[j] = min((byteToValue(b[0]) + 15), 100);
				party[j] = b[1];
				partyTiers[j] = monSorter.getPokemonOldTier(byteToValue(party[j]), false);
				
				newLvl[j] = lvl[j];
				newParty[j] = byteToValue(party[j]);
				
				int a = 0; // keep track of indexes to look into
				
				if (((kind >> 1) & 0x01) == (byte) 0x1) // has items
				{
					items[j] = b[2];
					newItems[j] = items[j];
					a++;
				}
				
				if ((kind & 0x01) == (byte) 0x1) // has moves
					for (int k = 0; k < 4; k++)
					{
						moves[j][k] = b[2 + a + k];
						newMoves[j][k] = moves[j][k];
					}
			}
			
			int lastLvl = lvl[size - 1]; // get the last Pokemon level
			
			for (int j = size; j < newSize; j++) // add the new Pokemon to party
			{	
				lastLvl += (int) floor(random() * 2); // randomly add levels
				lastLvl = min(lastLvl, 100); // constrain level
				newLvl[j] = lastLvl;
				
				int randTier = partyTiers[(int) floor(random() * size)]; // get a random tier from the original party
				newParty[j] = monSorter.getSameTier(randTier, -1, Type.NO_TYPE, true, false, false);

				if (((kind >> 1) & 0x01) == (byte) 0x1) // has items
					newItems[j] = (byte) 0x00; // no item
				
				if ((kind & 0x01) == (byte) 0x1) // has moves
					newMoves[j] = getMoveset(moveSorter, newParty[j], valueToByte(newLvl[j]), false);
			}
			
			byte[][] finalParty = new byte[newSize][];
			
			for (int j = 0; j < newSize; j++) // set the trainer bytes
			{
				ArrayList<Byte> slotList = new ArrayList<Byte>();
				slotList.add(valueToByte(newLvl[j]));
				slotList.add(valueToByte(newParty[j]));
				
				if (((kind >> 1) & 0x01) == (byte) 0x1) // has items
					slotList.add(newItems[j]);
				
				if ((kind & 0x01) == (byte) 0x1) // has moves
					for (byte curMove : newMoves[j])
						slotList.add(curMove);
					
				finalParty[j] = convertByteArray(slotList.toArray(new Byte[0]));
			}
			
			trainers[i].setParty(finalParty);
		}
	}
	
	void kantoForceEvolved(PokemonSorter monSorter)
	{
		// forces evolved Pokemon, if possible, on Kanto trainers
		
		for (int i : INDEX_KANTO_TRAINERS)
		{	
			int size = trainers[i].getPartySize();
			byte[] party = new byte[size];
			int[] lvls = new int[size];
			
			for (int j = 0; j < size; j++)
			{
				party[j] = trainers[i].getPokeByte(j);
				lvls[j] = byteToValue(trainers[i].getLvl(j));
			}
			
			byte[] newParty = valueToByte(monSorter.evolveTeam(byteToValue(party), lvls));
			
			for (int j = 0; j < size; j++) // set the trainer bytes
				trainers[i].setPoke(j, newParty[j]);
		}
	}
	
	void randomizePokemon(PokemonSorter monSorter, boolean withSimilar, int typeExpert, boolean persRival, boolean noLeg)
	{
		// typeExpert: 
		// 0 = no type specialists;
		// 1 = preserve type specialists;
		// 2 = randomize type specialists.
		
		for (int i = 0; i <  N_TRAINERS; i++)
		{
			if (isSpecialTrainer(i, typeExpert, true, persRival))
				continue; // skip trainers handled afterwards
			
			for (int j = 0; j < trainers[i].getPartySize(); j++)
			{
				if (withSimilar)
				{
					PokemonGame initialMon = mons[byteToValue(trainers[i].getPokeByte(j)) - 1];
					trainers[i].setPoke(j, monSorter.getSameTier(initialMon, Type.NO_TYPE, noLeg, false, false));
				}
				else
				{
					trainers[i].setPoke(j, valueToByte((int) floor(N_POKEMON * random()) + 1));
				}
			}
		}
		
		if (typeExpert != 0) // patch over type specialists
		{
			Type[][] typeClassList = TRAINER_CLASS_TYPES; // get type list for trainer class
			
			for (int i = 0; i < INDEX_TRAINER_CLASSES.length; i++) // cycle trainer classes
			{
				int[] monsOfType = monSorter.getPokemonOfType(typeClassList[i]);
				
				for (int j = INDEX_TRAINER_CLASSES[i][0]; j <= INDEX_TRAINER_CLASSES[i][1]; j++) // cycle trainers
				{
					if (isSpecialTrainer(j, typeExpert, false, persRival))
						continue; // skip trainers handled afterwards
					
					for (int k = 0; k < trainers[j].getPartySize(); k++) // cycle party
					{
						int randMon;
						
						if (withSimilar)
						{
							PokemonGame initialMon = mons[byteToValue(trainers[j].getPokeByte(k)) - 1];
							randMon = monSorter.getSameTier(initialMon, typeClassList[i], noLeg);
						}
						else
						{
							randMon = monsOfType[(int) floor(random() * monsOfType.length)];
						}
						
						trainers[j].setPoke(k, randMon);
					}
				}
			}
			
			Type[] typeList = (typeExpert == 1) ? GYM_TYPES : randomizeTypeList(GYM_TYPES); // get type list for gyms
			
			for (int i = 0; i < INDEX_GYM_TRAINERS.length; i++) // cycle gyms
			{
				int[] monsOfType = monSorter.getPokemonOfType(typeList[i]);
				
				for (int j = 0; j < INDEX_GYM_TRAINERS[i].length; j++) // cycle trainers
					for (int k = 0; k < trainers[INDEX_GYM_TRAINERS[i][j]].getPartySize(); k++) // cycle party
					{
						int randMon;
						
						if (withSimilar)
						{
							PokemonGame initialMon = mons[byteToValue(trainers[INDEX_GYM_TRAINERS[i][j]].getPokeByte(k)) - 1];
							randMon = monSorter.getSameTier(initialMon, typeList[i], noLeg, false, false);
						}
						else
						{
							randMon = monsOfType[(int) floor(random() * monsOfType.length)];
						}
						
						trainers[INDEX_GYM_TRAINERS[i][j]].setPoke(k, randMon);
					}
			}
			
			typeList = (typeExpert == 1) ? ELITE_TYPES : randomizeTypeList(ELITE_TYPES); // get type list for Elite Four
			
			for (int i = 0; i < INDEX_ELITE_FOUR.length; i++) // cycle Elite Four
			{
				int[] monsOfType = monSorter.getPokemonOfType(typeList[i]);
				
				for (int j = 0; j < trainers[INDEX_ELITE_FOUR[i]].getPartySize(); j++) // cycle party
				{
					int randMon;
					
					if (withSimilar)
					{
						PokemonGame initialMon = mons[byteToValue(trainers[INDEX_ELITE_FOUR[i]].getPokeByte(j)) - 1];
						randMon = monSorter.getSameTier(initialMon, typeList[i], noLeg, false, false);
					}
					else
					{
						randMon = monsOfType[(int) floor(random() * monsOfType.length)];
					}
					
					trainers[INDEX_ELITE_FOUR[i]].setPoke(j, randMon);
				}
			}
			
			if (withSimilar) // only applies to this option, yet
			{
				// handle forced mixed teams
				
				for (int i = 0; i < INDEX_MIXED_TRAINERS.length; i++) // cycle mixed Trainers
				{
					ArrayList<Integer> prevMonList = new ArrayList<Integer>(); // keep track of previous Pokemon in team
					
					for (int j = 0; j < trainers[INDEX_MIXED_TRAINERS[i]].getPartySize(); j++) // cycle party
					{
						int randMon;
						int[] prevMonArray = convertIntArray(prevMonList.toArray(new Integer[0]));
						
						PokemonGame initialMon = mons[byteToValue(trainers[INDEX_MIXED_TRAINERS[i]].getPokeByte(j)) - 1];
						randMon = monSorter.getSameTier(initialMon, Type.NO_TYPE, noLeg, false, true, prevMonArray);
						
						trainers[INDEX_MIXED_TRAINERS[i]].setPoke(j, randMon);
						prevMonList.add(randMon);
					}
				}
				
				if (persRival) // handle persistent Rival team
				{
					int[][][] levels = new int[INDEX_RIVAL.length][RIVAL_PARTY_SIZES.length][];
					int[][] finalRivalTeam = new int[INDEX_RIVAL.length][RIVAL_PARTY_SIZES[RIVAL_PARTY_SIZES.length - 1]];
					
					// collect Rival team data
					for (int i = 0; i < INDEX_RIVAL.length; i++) // cycle starters
						for (int j = 0; j < RIVAL_PARTY_SIZES.length; j++) // cycle battles
						{
							levels[i][j] = new int[RIVAL_PARTY_SIZES[j]];
							
							for (int k = 0; k < RIVAL_PARTY_SIZES[j]; k++) // cycle parties
							{
								levels[i][j][k] = byteToValue(trainers[INDEX_RIVAL[i][j]].getLvl(k));
								
								if (j == RIVAL_PARTY_SIZES.length - 1) // if it's the final battle
									finalRivalTeam[i][k] = byteToValue(trainers[INDEX_RIVAL[i][j]].getPokeByte(k));
							}
						}
								
					int[][][] rivalTeams = monSorter.generateRivalTeams(finalRivalTeam, levels, noLeg); //[Starter index][Battle number][Pokemon party]

					for (int i = 0; i < rivalTeams.length; i++) // cycle starters
						for (int j = 0; j < rivalTeams[i].length; j++) // cycle battles
							for (int k = 0; k < rivalTeams[i][j].length; k++) // cycle Pokemon party
								trainers[INDEX_RIVAL[i][j]].setPoke(k, rivalTeams[i][j][k]);
				}
			}
		}
	}
	
	void scaleLevel(float mult)
	{
		for (int i = 0; i <  N_TRAINERS; i++)
			for (int j = 0; j < trainers[i].getPartySize(); j++)
			{	
				int lvlMult = (int) max(min((trainers[i].getLvl(j) * mult), 100), 2);
				trainers[i].setLvl(j, (byte) lvlMult);
			}
	}
	
	Trainer[] getTrainers()
	{
		return trainers;
	}
	
	void applyMovesets(MoveSorter moveSorter, boolean extraCust)
	{
		// scaling level or changing Trainer Pokemon messes with custom moves
		// so this function must be called after applying one of those changes
		// for coherent movesets
		
		for (int i = 0; i <  N_TRAINERS; i++)
		{
			byte kind = trainers[i].getKind();
			
			if (kind != 1 && kind != 3)
				continue; // no custom moves
			
			for (int j = 0; j < trainers[i].getPartySize(); j++)
			{
				byte lvl = trainers[i].getLvl(j);
				byte[] newMoves = getMoveset(moveSorter, byteToValue(trainers[i].getPokeByte(j)), lvl, extraCust);
				trainers[i].setMoves(j, newMoves);
			}
		}
	}

	void applyMovesets(PokemonGame[] mons, TeamCustomizer teamCust)
	{	
		for (Trainer t : trainers)
		{
			byte kind = t.getKind();
			
			if (kind != 1 && kind != 3)
				continue; // no custom moves
			
			PokemonGame[] team = new PokemonGame[t.getPartySize()];
			int[] lvls = new int[team.length];

			for (int i = 0; i < team.length; i++)
			{
				int pokeIndex = byteToValue(t.getPokeByte(i));
				team[i] = mons[pokeIndex - 1];

				int lvl = byteToValue(t.getLvl(i));
				lvls[i] = lvl;
			}

			ArrayList<ArrayList<Move>> movesets = teamCust.customize(team, lvls, mons);

			for (int j = 0; j < t.getPartySize(); j++)
				t.setMoves(j, movesets.get(j));
		}
	}
	
	byte[] getMoveset(MoveSorter moveSorter, int monInt, byte lvl, boolean extraCust)
	{
		int thisMon = monInt - 1;
		byte[][] lvlMoves = mons[thisMon].getMovesUpToLevel(mons, lvl);

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
				if (k < lvlMoves[0].length)
					newMoves[k] = lvlMoves[0][k];
				else
					newMoves[k] = (byte) 0x00;
			}
			
		return newMoves;
	}
	
	private Type[] randomizeTypeList(Type[] typeList)
	{
		Type[] out = new Type[typeList.length]; // number of specialist gyms
		
		Type[] allTypes = new Type[N_TYPES];
		int n = 0;

		for (Type t : Type.values())
		{
			if (t == Type.NO_TYPE) continue;
			allTypes[n] = t;
			n++;
		}
		
		allTypes = shuffleArray(allTypes); // shuffle types
		int nEqualities = 0;
		ArrayList<Integer> equalIndex = new ArrayList<Integer>();
		
		for (int i = 0; i < typeList.length; i++)
			if (typeList[i] == allTypes[i])
			{
				equalIndex.add(i);
				nEqualities++;
			}
		
		if (nEqualities > 0) // if we have gyms with unchanged types
		{
			for (int i = 0; i < floor(nEqualities/2); i++) // cycle pairs
			{
				Type typeHolder = allTypes[equalIndex.get(2*i)];
				allTypes[equalIndex.get(i)] = allTypes[equalIndex.get(2*i+1)];
				allTypes[equalIndex.get(2*i+1)] = typeHolder;
			}
			
			if (nEqualities % 2 == 1) // if there's one without pair, switch it with an unused type
				allTypes[equalIndex.get(equalIndex.size()-1)] = allTypes[allTypes.length-1];
		}
		
		for (int i = 0; i < out.length; i++)
			out[i] = allTypes[i];
		
		return out;
	}
	
	private boolean isSpecialTrainer(int index, int typeExpert, boolean includeTrainerClass, boolean persRival)
	{
		// checks if trainer of index is a special trainer to be handled afterwards
		boolean out = false; // assume it isn't
		
		if (typeExpert != 0)
		{
			if (includeTrainerClass)
			{
				for (int i = 0; i < INDEX_TRAINER_CLASSES.length; i++) // cycle trainer classes
					for (int j = INDEX_TRAINER_CLASSES[i][0]; j <= INDEX_TRAINER_CLASSES[i][1]; j++) // cycle trainers
						if (j == index)
						{
							out = true;
							break;
						}
			}
					
			for (int i = 0; i < INDEX_GYM_TRAINERS.length; i++) // cycle gym
				for (int j : INDEX_GYM_TRAINERS[i]) // cycle trainers
					if (j == index)
					{
						out = true;
						break;
					}
					
			for (int i : INDEX_ELITE_FOUR) // cycle elite four
				if (i == index)
				{
					out = true;
					break;
				}
		}
		
		if (!out)
		{
			for (int i : INDEX_MIXED_TRAINERS) // cycle trainers
				if (i == index)
				{
					out = true;
					break;
				}
		}
		
		if (!out && persRival)
		{
			for (int i = 0; i < INDEX_RIVAL.length; i++) // cycle starters
				for (int j : INDEX_RIVAL[i]) // cycle rival fights
					if (j == index)
					{
						out = true;
						break;
					}
		}
		
		return out;
	}
	
	void giveStatExp()
	{
		for (Trainer t : trainers)
		{
			// get lowest level (usually the first party slot)
			int lvl = byteToValue(t.getLvl(0));
			t.setStatExp(lvlToStatExp(lvl));
		}
	}
	
	private byte lvlToStatExp(int lvl)
	{
		// calculates stat exp to trainer Pokemon of level lvl
		float poly = (float) (0.0265 * lvl * lvl - 0.2111 * lvl + 1.2497);
		return valueToByte((int) floor(poly));
	}

	void printTeam(Names names, int n)
	{
		Trainer t = trainers[n];

		System.out.println("~~~ " + names.trainer(n) + " ~~~");
		for (int i = 0; i < t.getPartySize(); i++)
		{
			byte[] b = t.getPokeBytes(i);

			System.out.println(names.pokemon(b[1]) + " lvl " + byteToValue(b[0]));

			if (t.getKind() == 1)
				for (int j = 0; j < 4; j++)
					System.out.println(" - " + names.move(b[2+j]));
			else if (t.getKind() == 3)
				for (int j = 0; j < 4; j++)
					System.out.println(" - " + names.move(b[3+j]));

			System.out.println();
		}
	}

	void printTeams(Names names)
	{
		for (int i = 0; i < trainers.length; i++)
			printTeam(names, i);
	}

	void printCustTeams(Names names)
	{
		for (int i = 0; i < trainers.length; i++)
		{
			if (trainers[i].getKind() != 1 && trainers[i].getKind() != 3) continue;
			printTeam(names, i);
		}
	}
}