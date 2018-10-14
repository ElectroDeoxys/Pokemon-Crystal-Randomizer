package engine;

import java.util.ArrayList;
import java.io.IOException;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Trainer;
import data.Pokemon;

class TrainerEditor
{
	private Trainer[] trainers;
	private Pokemon[] mons;
	private PokemonSorter monSorter;
	
	TrainerEditor(RomReader romReader, Pokemon[] mons) throws IOException
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
			
			int newSize = (int) min(size + 1 + floor(random() * 3), 6);
			int[] newLvl = new int[newSize];
			byte[] newParty = new byte[newSize];
			byte[] newItems = new byte[newSize];
			byte[][] newMoves = new byte[newSize][4];
			
			for (int j = 0; j < size; j++)
			{
				byte[] b = trainers[i].getPokeBytes(j);
				
				lvl[j] = (int) min((byteToValue(b[0]) + 15), 100);
				party[j] = b[1];
				partyTiers[j] = monSorter.getPokemonOldTier(party[j], false);
				
				newLvl[j] = lvl[j];
				newParty[j] = party[j];
				
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
				lastLvl = (int) min(lastLvl, 100); // constrain level
				newLvl[j] = lastLvl;
				
				int randTier = partyTiers[(int) floor(random() * size)]; // get a random tier from the original party
				newParty[j] = monSorter.getSameTier(randTier, -1, -1, true, false, false);

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
				slotList.add(newParty[j]);
				
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
			
			byte[] newParty = monSorter.evolveTeam(party, lvls);
			
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
					Pokemon initialMon = mons[byteToValue(trainers[i].getPokeByte(j)) - 1];
					trainers[i].setPoke(j, monSorter.getSameTier(initialMon, -1, noLeg, false, false));
				}
				else
				{
					trainers[i].setPoke(j, (byte) valueToByte((int) floor(N_POKEMON * random()) + 1));
				}
			}
		}
		
		if (typeExpert != 0) // patch over type specialists
		{
			int[][] typeClassList = TRAINER_CLASS_TYPES; // get type list for trainer class
			
			for (int i = 0; i < INDEX_TRAINER_CLASSES.length; i++) // cycle trainer classes
			{
				byte[] monsOfType = monSorter.getPokemonOfType(typeClassList[i]);
				
				for (int j = INDEX_TRAINER_CLASSES[i][0]; j <= INDEX_TRAINER_CLASSES[i][1]; j++) // cycle trainers
				{
					if (isSpecialTrainer(j, typeExpert, false, persRival))
						continue; // skip trainers handled afterwards
					
					for (int k = 0; k < trainers[j].getPartySize(); k++) // cycle party
					{
						byte randMon;
						
						if (withSimilar)
						{
							Pokemon initialMon = mons[byteToValue(trainers[j].getPokeByte(k)) - 1];
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
			
			int[] typeList = (typeExpert == 1) ? getGymTypeList(false) : getGymTypeList(true); // get type list for gyms
			
			for (int i = 0; i < INDEX_GYM_TRAINERS.length; i++) // cycle gyms
			{
				byte[] monsOfType = monSorter.getPokemonOfType(typeList[i]);
				
				for (int j = 0; j < INDEX_GYM_TRAINERS[i].length; j++) // cycle trainers
					for (int k = 0; k < trainers[INDEX_GYM_TRAINERS[i][j]].getPartySize(); k++) // cycle party
					{
						byte randMon;
						
						if (withSimilar)
						{
							Pokemon initialMon = mons[byteToValue(trainers[INDEX_GYM_TRAINERS[i][j]].getPokeByte(k)) - 1];
							randMon = monSorter.getSameTier(initialMon, typeList[i], noLeg, false, false);
						}
						else
						{
							randMon = monsOfType[(int) floor(random() * monsOfType.length)];
						}
						
						trainers[INDEX_GYM_TRAINERS[i][j]].setPoke(k, randMon);
					}
			}
			
			typeList = (typeExpert == 1) ? getEliteTypeList(false) : getEliteTypeList(true); // get type list for Elite Four
			
			for (int i = 0; i < INDEX_ELITE_FOUR.length; i++) // cycle Elite Four
			{
				byte[] monsOfType = monSorter.getPokemonOfType(typeList[i]);
				
				for (int j = 0; j < trainers[INDEX_ELITE_FOUR[i]].getPartySize(); j++) // cycle party
				{
					byte randMon;
					
					if (withSimilar)
					{
						Pokemon initialMon = mons[byteToValue(trainers[INDEX_ELITE_FOUR[i]].getPokeByte(j)) - 1];
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
					ArrayList<Byte> prevMonList = new ArrayList<Byte>(); // keep track of previous Pokemon in team
					
					for (int j = 0; j < trainers[INDEX_MIXED_TRAINERS[i]].getPartySize(); j++) // cycle party
					{
						byte randMon;
						byte[] prevMonArray = convertByteArray(prevMonList.toArray(new Byte[0]));
						
						Pokemon initialMon = mons[byteToValue(trainers[INDEX_MIXED_TRAINERS[i]].getPokeByte(j)) - 1];
						randMon = monSorter.getSameTier(initialMon, -1, noLeg, false, true, prevMonArray);
						
						trainers[INDEX_MIXED_TRAINERS[i]].setPoke(j, randMon);
						prevMonList.add(randMon);
					}
				}
				
				if (persRival) // handle persistent Rival team
				{
					int[][][] levels = new int[INDEX_RIVAL.length][RIVAL_PARTY_SIZES.length][];
					byte[][] finalRivalTeam = new byte[INDEX_RIVAL.length][RIVAL_PARTY_SIZES[RIVAL_PARTY_SIZES.length - 1]];
					
					// collect Rival team data
					for (int i = 0; i < INDEX_RIVAL.length; i++) // cycle starters
						for (int j = 0; j < RIVAL_PARTY_SIZES.length; j++) // cycle battles
						{
							levels[i][j] = new int[RIVAL_PARTY_SIZES[j]];
							
							for (int k = 0; k < RIVAL_PARTY_SIZES[j]; k++) // cycle parties
							{
								levels[i][j][k] = byteToValue(trainers[INDEX_RIVAL[i][j]].getLvl(k));
								
								if (j == RIVAL_PARTY_SIZES.length - 1) // if it's the final battle
									finalRivalTeam[i][k] = trainers[INDEX_RIVAL[i][j]].getPokeByte(k);
							}
						}
								
					byte[][][] rivalTeams = monSorter.generateRivalTeams(finalRivalTeam, levels, noLeg); //[Starter index][Battle number][Pokemon party]

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
				byte[] newMoves = getMoveset(moveSorter, trainers[i].getPokeByte(j), lvl, extraCust);
				trainers[i].setMoves(j, newMoves);
			}
		}
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
	
	private int[] getGymTypeList(boolean isRandom)
	{
		int[] out = new int[GYM_TYPES.length]; // number of specialist gyms
		
		if (!isRandom)
		{
			out = GYM_TYPES;
		}
		else
		{
			int[] allTypes = new int[N_TYPES];
			for (int i = 0; i < N_TYPES; i++)
				allTypes[i] = i;
			
			allTypes = shuffleArray(allTypes); // shuffle types
			int nEqualities = 0;
			ArrayList<Integer> equalIndex = new ArrayList<Integer>();
			
			for (int i = 0; i < GYM_TYPES.length; i++)
				if (GYM_TYPES[i] == allTypes[i])
				{
					equalIndex.add(i);
					nEqualities++;
				}
			
			if (nEqualities > 0) // if we have gyms with unchanged types
			{
				for (int i = 0; i < floor(nEqualities/2); i++) // cycle pairs
				{
					int typeHolder = allTypes[equalIndex.get(2*i)];
					allTypes[equalIndex.get(i)] = allTypes[equalIndex.get(2*i+1)];
					allTypes[equalIndex.get(2*i+1)] = typeHolder;
				}
				
				if (nEqualities % 2 == 1) // if there's one without pair, switch it with an unused type
					allTypes[equalIndex.get(equalIndex.size()-1)] = allTypes[allTypes.length-1];
			}
			
			for (int i = 0; i < out.length; i++)
				out[i] = allTypes[i];
		}
		
		return out;
	}
	
	private int[] getEliteTypeList(boolean isRandom)
	{
		int[] out = new int[ELITE_TYPES.length]; // number of Elite
		
		if (!isRandom)
		{
			out = ELITE_TYPES;
		}
		else
		{
			int[] allTypes = new int[N_TYPES];
			for (int i = 0; i < N_TYPES; i++)
				allTypes[i] = i;
			
			allTypes = shuffleArray(allTypes); // shuffle types
			int nEqualities = 0;
			ArrayList<Integer> equalIndex = new ArrayList<Integer>();
			
			for (int i = 0; i < ELITE_TYPES.length; i++)
				if (ELITE_TYPES[i] == allTypes[i])
				{
					equalIndex.add(i);
					nEqualities++;
				}
			
			if (nEqualities > 0) // if we have Elite with unchanged types
			{
				for (int i = 0; i < floor(nEqualities/2); i++) // cycle pairs
				{
					int typeHolder = allTypes[equalIndex.get(2*i)];
					allTypes[equalIndex.get(i)] = allTypes[equalIndex.get(2*i+1)];
					allTypes[equalIndex.get(2*i+1)] = typeHolder;
				}
				
				if (nEqualities % 2 == 1) // if there's one without pair, switch it with an unused type
					allTypes[equalIndex.get(equalIndex.size()-1)] = allTypes[allTypes.length-1];
			}
			
			for (int i = 0; i < out.length; i++)
				out[i] = allTypes[i];
		}
		
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
}