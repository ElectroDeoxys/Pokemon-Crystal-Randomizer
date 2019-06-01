package engine;

import java.util.ArrayList;
import static java.lang.Math.*;
import java.util.Arrays;

import static data.Constants.*;
import data.Pokemon;

class PokemonSorter<T extends Pokemon>
{

    private T[] mons;

    private ArrayList<T> starters;
    private ArrayList<ArrayList<T>> byType = new ArrayList<>();

    private PokemonTierList tierList;
    private PokemonTierList[] tierListType = new PokemonTierList[N_TYPES]; // [type][tier][position]

    private ArrayList<ArrayList<T>> byEvoLines = new ArrayList<>(); // lists all the evolutionary lines [number of evo line][list of species in order of evolution]
    private ArrayList<ArrayList<T>> starterCand = new ArrayList<>(); // candidates for starters (below a BST threshold and able to evolve)
    private ArrayList<ArrayList<T>> starterCand3Stages = new ArrayList<>(); // candidates for starters (3-stage evolutions)

    PokemonSorter(T[] mons)
    {
        this.mons = mons;
        fillTypeList();
        
        tierList = new PokemonTierList<>(mons, BOT_BST, TOP_BST, N_TIERS);
        
        for (int i = 0; i < N_TYPES; i++)
            tierListType[i] = new PokemonTierList<>(byType.get(i), BOT_BST, TOP_BST, N_TYPE_TIERS);
        
        setTiers();
        
        sortPokemon();
    }

    PokemonSorter(T[] mons, T[] startersArray)
    {
        this(mons);
        this.starters = new ArrayList<>(Arrays.asList(startersArray));
    }

    PokemonSorter(T[] mons, T[] starters, Names names)
    {
        this(mons, starters);

        for (int i = 0; i < byEvoLines.size(); i++)
        {
            System.out.println("Evo " + i);
            for (int j = 0; j < byEvoLines.get(i).size(); j++)
            {
                System.out.print(names.pokemon(byEvoLines.get(i).get(j).getIntIndex()) + " ");
            }
            System.out.println();
        }
    }
    
    private void setTiers()
    {
        for (int tier = 0; tier < N_TIERS; tier++)
        {
            ArrayList<T> curTierList = tierList.getTier(tier);
            for (T mon : curTierList)
            {
                mon.setTier(tier);
                if (mon.getOldTier() < 0)
                {
                    mon.setOldTier(tier); // only change if it hasn't been set
                }
            }
        }
        
        for (PokemonTierList curTierListType : tierListType)
        {
            for (int tier = 0; tier < N_TYPE_TIERS; tier++)
            {
                ArrayList<T> curTierList = curTierListType.getTier(tier);
                for (T mon : curTierList)
                {
                    mon.setTypeTier(tier);
                    if (mon.getOldTypeTier() < 0)
                    {
                        mon.setOldTypeTier(tier); // only change if it hasn't been set
                    }
                }
            }
        }
    }

    private void sortPokemon()
    {
        ArrayList<Integer> evoChecked = new ArrayList<>(); // checked T for evolutions

        for (int i = 0; i < mons.length; i++)
        {
            // concerning evo sorting

            if (!evoChecked.contains(mons[i].getIntIndex())) // if this T hasn't been processed yet
            {
                // start a new array for this line				
                ArrayList<T> thisEvoLine = new ArrayList<>();
                int startIndex = i; // assume that this is the start in the evo line
                boolean is3Stage = false; // assume it's not 3-stage evolution

                // may have up to two pre-evolutions, so get to the bottom of the chain
                if (mons[i].hasPre()) // if this T has a pre-evolution
                {
                    //System.out.println(mons[i].getTrueIndex());
                    int[] preEvo = mons[i].getPreEvoInt();
                    int preEvoIndex = preEvo[0] - 1;

                    if (mons[preEvoIndex].hasPre()) //if this pre-evo still has pre-evo, start there
                    {
                        int[] prePreEvo = mons[preEvoIndex].getPreEvoInt();
                        startIndex = prePreEvo[0] - 1;
                    }
                    else // the pre-evo is already the bottom, so update the starting index
                    {
                        startIndex = preEvoIndex;
                    }
                }

                thisEvoLine.add(mons[startIndex]); // add the starting T
                evoChecked.add(mons[startIndex].getIntIndex());

                if (mons[startIndex].hasEvos()) // explore all the evolution branches
                {
                    int[] evoInts = mons[startIndex].getEvoInt();
                    for (int j = 0; j < evoInts.length; j++) // explore all evolutions
                    {
                        if (evoInts[j] == 255)
                            System.out.println(true);
                        
                        thisEvoLine.add(mons[evoInts[j]-1]); // add this evolution T
                        evoChecked.add(evoInts[j]);

                        int thisEvo = evoInts[j] - 1;

                        if (mons[thisEvo].hasEvos()) // check evolutions of the evo
                        {
                            int[] evoEvoInts = mons[thisEvo].getEvoInt();
                            for (int k = 0; k < evoEvoInts.length; k++) // explore all evolutions
                            {
                                thisEvoLine.add(mons[evoEvoInts[k]-1]); // add this evolution T
                                evoChecked.add(evoEvoInts[k]);
                            }

                            is3Stage = true; // set the 3-stage evolution line check to true
                        }
                    }
                }

                byEvoLines.add(thisEvoLine); // add the evolution line to the array
                if ((mons[startIndex].hasEvos()) && (mons[startIndex].getBST() <= STARTER_BST))
                {
                    starterCand.add(thisEvoLine); // add this in only if it's 3-stage
                }
                if ((is3Stage) && (mons[startIndex].getBST() <= STARTER_BST))
                {
                    starterCand3Stages.add(thisEvoLine); // add this in only if it's 3-stage
                }
            }
        }
    }

    ArrayList<T> getPokemonOfType(Type type) // for one single type
    {
        return this.byType.get(type.intIndex());
    }

    ArrayList<T> getPokemonOfType(Type[] type) // for multiple types
    {
        ArrayList<T> list = new ArrayList<>();

        for (Type t : type) // cycle types
            for (int i = 0; i < byType.get(t.intIndex()).size(); i++)
                list.add(byType.get(t.intIndex()).get(i));

        return list;
    }

    int getPokemonOldTier(int pokeInt, boolean bType)
    {
        T mon = mons[pokeInt - 1];
        int out = (bType) ? mon.getOldTypeTier() : mon.getOldTier();
        return out;
    }

    ArrayList<T> getRandomStarters(int starterKind)
    {
        // gives three random T as starters
        // if starterKind == 0 : completely random
        // if starterKind == 1 : starters with at least one evolution and below STARTER_BST
        // if starterKind == 2 : only give out the starting T of a 3-stage evolution
        // always returns the lowest T of the evo-line
        // always returns different T with different types

        switch (starterKind)
        {
            case 0:
                starters = generateRandomStarters(byEvoLines);
                break;
            case 1:
                starters = generateRandomStarters(starterCand);
                break;
            case 2:
                starters = generateRandomStarters(starterCand3Stages);
                break;
            default:
                break;
        }

        return starters;
    }

    ArrayList<T> generateRandomStarters(ArrayList<ArrayList<T>> evoLineList)
    {
        ArrayList<T> out = new ArrayList<>();

        while (out.size() < 3)
        {
            out.add(randomElement(evoLineList).get(0));
            
            if (containsRepeats(out) || isTypeRepeated(out))
                out.remove(out.size() - 1);
        }

        return out;
    }
    
    private boolean containsRepeats(ArrayList<T> list)
    {
        boolean test = false;
        
        for (int i = 0; i < list.size() - 1; i++)
            for (int j = i + 1; j < list.size(); j++)
            {
                test = (list.get(i) == list.get(j));
                if (test) break;
            }
        
        return test;
    }
    
    private boolean isTypeRepeated(ArrayList<T> monList)
    {
        // decides if there is type redundancy in a list of T
        // will return true if any types are repeated

        boolean out = false; // assume it's not repeated

        Type[][] types = new Type[monList.size()][2]; // two types per T

        for (int i = 0; i < monList.size(); i++) // cycle T list
        {
            types[i] = monList.get(i).getTypes();
        }

        for (int i = 0; i < monList.size(); i++) // cycle T list
        {
            for (int j = i + 1; j < monList.size(); j++) // cycle all T ahead of list
            {                    
                out =    ((types[i][0] == types[j][0]) || (types[i][0] == types[j][1]))
                      || ((types[i][1] == types[j][0]) || (types[i][1] == types[j][1]));

                if (out) break; // exit loop if repetition found
            }

            if (out) break;
        }

        return out;
    }

    private boolean areSameType(T mon1, T mon2)
    {
        // gets two T and decides if they share a type
        Type[] types1 = mon1.getTypes();
        Type[] types2 = mon2.getTypes();
        boolean out = (types1[0] == types2[0] || types1[0] == types2[1] || types1[1] == types2[0] || types1[1] == types2[1]);
        return out;
    }

    int[][][] generateRivalTeams(int[][] finalRivalTeam, int[][][] levels, boolean noLeg)
    {
        // generates Rival teams that have persistent T throughout the battles
        // depending on starter chosen by the player, and which is forced to be mixed
        // T evolve according to level and all have evolutionary lines except possibly for the final lead
        // Rival's starter is chosen from the in-game starter

        int[][][] rivalTeams = new int[INDEX_RIVAL.length][RIVAL_PARTY_SIZES.length][];
        int[] finalTeam = new int[RIVAL_PARTY_SIZES[RIVAL_PARTY_SIZES.length - 1]];

        for (int i = 0; i < INDEX_RIVAL.length; i++) // cycle starters (each starter will generate a different team)
        {
            // decide the evolutionary line of the starter first
            ArrayList<T> starterSlot = getEvoLine(starters.get(i), -1);

            // generate the final team first
            ArrayList<T> prevMonList = new ArrayList<>(); // keep track of previous T in team
            prevMonList.add(starterSlot.get(starterSlot.size() - 1)); // add starter evo to take into account its types

            // initialize the other slots 
            ArrayList<ArrayList<T>> monSlots = new ArrayList<>();

            for (int j = 0; j < finalTeam.length; j++) // cycle through the final battle party
            {
                if (j < finalTeam.length - 1) // not the starter slot
                {
                    boolean isLead = (j == 0); // forced evolved unless it's the lead
                    T thisMon = mons[finalRivalTeam[i][j] - 1];
                    T chosenMon = getSameTier(thisMon, noLeg, (!isLead), true, prevMonList);

                    finalTeam[j] = chosenMon.getIntIndex();
                    monSlots.add(getEvoLine(chosenMon, -1));

                    // may result in an unevolved T, so change it to be the final form in the last battle
                    //finalTeam[j] = monSlots[j][monSlots[j].length - 1];
                    prevMonList.add(chosenMon);

                }
                else
                {
                    finalTeam[j] = starterSlot.get(starterSlot.size() - 1).getIntIndex(); // add in the evolved starter
                }
            }

            // final team is formed, now to generate the earlier teams based on the result
            // initialize the byte arrays for each battle
            for (int j = 0; j < rivalTeams[i].length; j++)
            {
                rivalTeams[i][j] = new int[RIVAL_PARTY_SIZES[j]];
            }

            // battle 0
            rivalTeams[i][0][0] = decideEvo(starterSlot, levels[i][0][0]).getIntIndex();
            // battle 1
            rivalTeams[i][1][0] = decideEvo(monSlots.get(3), levels[i][1][0]).getIntIndex();
            rivalTeams[i][1][1] = decideEvo(monSlots.get(1), levels[i][1][1]).getIntIndex();
            rivalTeams[i][1][2] = decideEvo(starterSlot, levels[i][1][2]).getIntIndex();
            // battle 2
            rivalTeams[i][2][0] = decideEvo(monSlots.get(3), levels[i][2][0]).getIntIndex();
            rivalTeams[i][2][1] = decideEvo(monSlots.get(2), levels[i][2][1]).getIntIndex();
            rivalTeams[i][2][2] = decideEvo(monSlots.get(1), levels[i][2][2]).getIntIndex();
            rivalTeams[i][2][3] = decideEvo(starterSlot, levels[i][2][3]).getIntIndex();
            // battle 3
            rivalTeams[i][3][0] = decideEvo(monSlots.get(1), levels[i][3][0]).getIntIndex();
            rivalTeams[i][3][1] = decideEvo(monSlots.get(2), levels[i][3][1]).getIntIndex();
            rivalTeams[i][3][2] = decideEvo(monSlots.get(3), levels[i][3][2]).getIntIndex();
            rivalTeams[i][3][3] = decideEvo(monSlots.get(0), levels[i][3][3]).getIntIndex();
            rivalTeams[i][3][4] = decideEvo(starterSlot, levels[i][3][4]).getIntIndex();
            // battle from 4 upwards
            for (int j = 4; j < RIVAL_PARTY_SIZES.length; j++)
            {
                rivalTeams[i][j][0] = decideEvo(monSlots.get(0), levels[i][j][0]).getIntIndex();
                rivalTeams[i][j][1] = decideEvo(monSlots.get(1), levels[i][j][1]).getIntIndex();
                rivalTeams[i][j][2] = decideEvo(monSlots.get(2), levels[i][j][2]).getIntIndex();
                rivalTeams[i][j][3] = decideEvo(monSlots.get(3), levels[i][j][3]).getIntIndex();
                rivalTeams[i][j][4] = decideEvo(monSlots.get(4), levels[i][j][4]).getIntIndex();
                rivalTeams[i][j][5] = decideEvo(starterSlot, levels[i][j][5]).getIntIndex();
            }
        }

        return rivalTeams;
    }

    int[] evolveTeam(int[] monTeam, int[] lvls)
    {
        // makes T team evolve given their levels
        int[] evoTeam = new int[monTeam.length];

        for (int i = 0; i < monTeam.length; i++) // cycle team
        {
            evoTeam[i] = decideEvo(getEvoLine(mons[monTeam[i] - 1], -1), lvls[i]).getIntIndex();
        }

        return evoTeam;
    }

    private ArrayList<T> getEvoLine(T mon, int branch)
    {
        // gets the evolutionary line of a T in order
        // if there's a branch ahead, branch dictates what should happen
        // branch = 0, 1, ... : the appropriate branch is chosen
        // branch = -1 : a random branch is chosen

        int evoIndex = -1; // find the evolutionary line index
        int evoPos = -1; // find the position in that evo line

        for (int i = 0; i < byEvoLines.size(); i++) // cycle evo-lines
        {
            for (int j = 0; j < byEvoLines.get(i).size(); j++)
            {
                if (mon.equals(byEvoLines.get(i).get(j)))
                {
                    evoIndex = i;
                    evoPos = j;
                    break;
                }
            }
        }

        ArrayList<T> outList = new ArrayList<>();

        for (int i = 0; i < byEvoLines.get(evoIndex).size();) // cycle line members
        {
            outList.add(byEvoLines.get(evoIndex).get(i));
            T thisMon = byEvoLines.get(evoIndex).get(i);

            if (!thisMon.hasEvos())
            {
                break; // no more evos to look into
            }
            // figure out whether to continue normally or skip to a particular branch
            int[] thisEvos = thisMon.getEvoInt();

            if (thisEvos.length == 1) // only one possible evolution
            {
                i++;
            }
            else // reached a branch
            {
                if (i < evoPos) // the line has already been determined by input T
                {
                    i = evoPos;
                }
                else // need to choose a branch now
                {
                    int nJumps = (branch >= 0) ? branch : (int) floor(random() * thisEvos.length);

                    i++;
                    for (int j = 0; j < nJumps; j++) // jump accordingly to the proper branch
                    {
                        if (byEvoLines.get(evoIndex).get(i).hasEvos())
                        {
                            i += 2;
                        }
                        else
                        {
                            i++;
                        }
                    }
                }
            }
        }

        return outList;
    }

    private T decideEvo(ArrayList<T> evoLine, int lvl)
    {
        // decides what evolutionary form is appropriate for level lvl
        // evolutionary methods other than leveling up are taken to be OTHER_METHODS_LEVEL

        int c = 0;
        // start from the bottom and build up
        for (int i = 0; i < evoLine.size() - 1; i++)
        {
            T thisMon = evoLine.get(i);
            int[] evos = thisMon.getEvoInt();
            byte[][] evoBytes = thisMon.getEvos();
            boolean evolve; // assume it shouldn't evolve
            int branchIndex = getBranchIndex(evos, evoLine.get(i + 1));

            if ((evoBytes[branchIndex][0] == (byte) 0x01) || (evoBytes[branchIndex][0] == (byte) 0x05)) // level evolution
            {
                evolve = (lvl >= byteToValue(evoBytes[branchIndex][1]));
            }
            else // other methods
            {
                // check if the its evolution also evolves
                if (evoLine.get(i + 1).hasEvos())
                {
                    evolve = (lvl >= OTHER_METHODS_LEVEL_LOWER);
                }
                else
                {
                    evolve = (lvl >= OTHER_METHODS_LEVEL);
                }
            }

            if (!evolve)
            {
                break;
            }
            c++;
        }

        return evoLine.get(c);
    }

    private int getBranchIndex(int[] evos, T mon)
    {
        // find the branch index to get monInt
        int out = 0;

        for (int i = 0; i < evos.length; i++)
        {
            if (mon.getIntIndex() == evos[i])
            {
                out = i;
                break;
            }
        }

        return out;
    }

    int findEvoLineContaining(T mon)
    {
        // finds the evolutionary line containing T with index n
        int out = -1;

        for (int i = 0; i < byEvoLines.size(); i++)
        {
            if (byEvoLines.get(i).contains(mon))
            {
                out = i;
                break;
            }
        }

        return out;
    }

    ArrayList<ArrayList<T>> getEvoLines()
    {
        return this.byEvoLines;
    }

    private void fillTypeList()
    {
        for (Type type : Type.values()) 
            byType.add(new ArrayList<>());
        
        for (T mon : mons)
        {
            Type[] typeOfMon = mon.getTypes();
            byType.get(typeOfMon[0].intIndex()).add(mon);
            
            if (typeOfMon[0] != typeOfMon[1])
                byType.get(typeOfMon[1].intIndex()).add(mon);
        }
    }
    
    void print(Names names)
    {
        ArrayList<ArrayList<T>> list = tierList.getTierList();
        for (int tier = 0; tier < list.size(); tier++)
        {
            System.out.println("Tier " + tier + ":");

            for (T mon : list.get(tier))
            {
                System.out.println(names.pokemon(mon.getIntIndex()));
            }
        }
    }

    T getSameTier(T mon, boolean noLeg, boolean onlyEvolved, boolean forcedMixed, ArrayList<T> prevMonArray)
    {
        return (T) tierList.getSameTier(mon.getOldTier(), noLeg, onlyEvolved, forcedMixed, prevMonArray);
    }

    T getSameTier(T mon, Type type, boolean noLeg, boolean onlyEvolved)
    {
        PokemonTierList curTierList = (type == Type.NO_TYPE) ? tierList : tierListType[type.intIndex()];
        int tier = (type == Type.NO_TYPE) ? mon.getOldTier() : mon.getOldTypeTier();
        return (T) curTierList.getSameTier(tier, noLeg, onlyEvolved);
    }
    
    T getSameTier(T mon, Type[] types, boolean noLeg, boolean onlyEvolved)
    {
        Type type = randomElement(types);
        PokemonTierList curTierList = (type == Type.NO_TYPE) ? tierList : tierListType[type.intIndex()];
        int tier = (type == Type.NO_TYPE) ? mon.getOldTier() : mon.getOldTypeTier();
        return (T) curTierList.getSameTier(tier, noLeg, onlyEvolved);
    }
    
    T getSameTier(int tier, Type type, boolean noLeg, boolean onlyEvolved)
    {
        PokemonTierList curTierList = (type == Type.NO_TYPE) ? tierList : tierListType[type.intIndex()];
        return (T) curTierList.getSameTier(tier, noLeg, onlyEvolved);
    }
}
