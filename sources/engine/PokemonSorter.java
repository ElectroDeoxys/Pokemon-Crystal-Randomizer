package engine;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Pokemon;

class PokemonSorter
{

    private Pokemon[] mons;

    private int[] starters;

    private int[][] byStats; // [tier number][position]
    private int[][] byType; // [type][position]
    private int[][][] byTypeStats; // [type][tier][position]

    private int[][] byEvoLines; // lists all the evolutionary lines [number of evo line][list of species in order of evolution]
    private int[][] starterCand; // candidates for starters (below a BST threshold and able to evolve)
    private int[][] starterCand3Stages; // candidates for starters (3-stage evolutions)

    PokemonSorter(Pokemon[] mons)
    {
        this.mons = mons;
        this.starters = new int[0];
        sortPokemon();

    }

    PokemonSorter(Pokemon[] mons, byte[] starters)
    {
        this.mons = mons;
        this.starters = byteToValue(starters);
        sortPokemon();
    }

    PokemonSorter(Pokemon[] mons, byte[] starters, Names names)
    {
        this.mons = mons;
        this.starters = byteToValue(starters);
        sortPokemon();

        for (int i = 0; i < byEvoLines.length; i++)
        {
            System.out.println("Evo " + i);
            for (int j = 0; j < byEvoLines[i].length; j++)
            {
                System.out.print(names.pokemon(byEvoLines[i][j] - 1) + " ");
            }
            System.out.println();
        }
    }

    private void sortPokemon()
    {
        // list from lowest BST to highest in discrete tiers
        int span = (TOP_BST - BOT_BST) / (N_TIERS - 1);
        float typeTierMult = (float) N_TYPE_TIERS / N_TIERS; // multiplier between total tiers and type tiers

        ArrayList<ArrayList<Integer>> tierHolder = new ArrayList<>();
        ArrayList<ArrayList<Integer>> typeHolder = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> typeTierHolder = new ArrayList<>();

        for (int i = 0; i < N_TIERS; i++) // construct the 2-dimensional array tier list
        {
            tierHolder.add(new ArrayList<>());
        }

        for (int i = 0; i < N_TYPES; i++) // construct the 2-dimensional type list
        {
            typeHolder.add(new ArrayList<>());
            typeTierHolder.add(new ArrayList<>());
            for (int j = 0; j < N_TYPE_TIERS; j++) // construct the 2-dimensional array type tier list
            {
                typeTierHolder.get(i).add(new ArrayList<>());
            }
        }

        ArrayList<ArrayList<Integer>> evoHolder = new ArrayList<>(); // for the evolutions
        ArrayList<ArrayList<Integer>> strHolder = new ArrayList<>(); // for the starters
        ArrayList<ArrayList<Integer>> strHolder3Stages = new ArrayList<>(); // for the 3-stage evos
        ArrayList<Integer> evoChecked = new ArrayList<>(); // checked Pokemon for evolutions

        for (int i = 0; i < mons.length; i++)
        {
            // concerning the tiers

            int bst = mons[i].getBST();
            int indexTier = (int) min(floor(max((bst - (BOT_BST + 1)), 0) / span), N_TIERS - 1);
            int indexTypeTier = (int) floor(typeTierMult * indexTier);

            // min (max) are for BST under (above) the specified bounds
            tierHolder.get(indexTier).add(mons[i].getIntIndex());
            mons[i].setTier(indexTier);
            if (mons[i].getOldTier() < 0)
            {
                mons[i].setOldTier(indexTier); // only change if it hasn't been set
            }
            // concerning type sorting

            Type[] typeOfMon = mons[i].getTypes();

            typeHolder.get(typeOfMon[0].intIndex()).add(mons[i].getIntIndex());

            if (!(mons[i].isNormalFlying())) // don't count Normal/Flying as Normal-type
            {
                typeTierHolder.get(typeOfMon[0].intIndex()).get(indexTypeTier).add(mons[i].getIntIndex());
            }

            if (typeOfMon[0] != typeOfMon[1]) // if dual-type
            {
                typeHolder.get(typeOfMon[1].intIndex()).add(mons[i].getIntIndex());

                if (!(mons[i].isBugFlying())) // don't count Bug/Flying as Flying-type
                {
                    typeTierHolder.get(typeOfMon[1].intIndex()).get(indexTypeTier).add(mons[i].getIntIndex());
                }
            }

            mons[i].setTypeTier(indexTypeTier);
            if (mons[i].getOldTypeTier() < 0)
            {
                mons[i].setOldTypeTier(indexTypeTier); // only change if it hasn't been set
            }
            // concerning evo sorting

            if (!evoChecked.contains(mons[i].getIntIndex())) // if this Pokemon hasn't been processed yet
            {
                // start a new array for this line				
                ArrayList<Integer> thisEvoLine = new ArrayList<>();
                int startIndex = i; // assume that this is the start in the evo line
                boolean is3Stage = false; // assume it's not 3-stage evolution

                // may have up to two pre-evolutions, so get to the bottom of the chain
                if (mons[i].hasPre()) // if this Pokemon has a pre-evolution
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

                thisEvoLine.add(mons[startIndex].getIntIndex()); // add the starting Pokemon
                evoChecked.add(mons[startIndex].getIntIndex());

                if (mons[startIndex].hasEvos()) // explore all the evolution branches
                {
                    int[] evoInts = mons[startIndex].getEvoInt();
                    for (int j = 0; j < evoInts.length; j++) // explore all evolutions
                    {
                        thisEvoLine.add(evoInts[j]); // add this evolution Pokemon
                        evoChecked.add(evoInts[j]);

                        int thisEvo = evoInts[j] - 1;

                        if (mons[thisEvo].hasEvos()) // check evolutions of the evo
                        {
                            int[] evoEvoInts = mons[thisEvo].getEvoInt();
                            for (int k = 0; k < evoEvoInts.length; k++) // explore all evolutions
                            {
                                thisEvoLine.add(evoEvoInts[k]); // add this evolution Pokemon
                                evoChecked.add(evoEvoInts[k]);
                            }

                            is3Stage = true; // set the 3-stage evolution line check to true
                        }
                    }
                }

                evoHolder.add(thisEvoLine); // add the evolution line to the array
                if ((mons[startIndex].hasEvos()) && (mons[startIndex].getBST() <= STARTER_BST))
                {
                    strHolder.add(thisEvoLine); // add this in only if it's 3-stage
                }
                if ((is3Stage) && (mons[startIndex].getBST() <= STARTER_BST))
                {
                    strHolder3Stages.add(thisEvoLine); // add this in only if it's 3-stage
                }
            }
        }

        // convert the ArrayLists into int arrays
        int[][] tierInt = new int[N_TIERS][];
        int[][] typeInt = new int[N_TYPES][];
        int[][][] typeTierInt = new int[N_TYPES][N_TYPE_TIERS][];

        for (int i = 0; i < N_TIERS; i++)
        {
            tierInt[i] = convertIntArray(tierHolder.get(i).toArray(new Integer[0]));
        }

        for (int i = 0; i < N_TYPES; i++)
        {
            typeInt[i] = convertIntArray(typeHolder.get(i).toArray(new Integer[0]));

            for (int j = 0; j < N_TYPE_TIERS; j++)
            {
                typeTierInt[i][j] = convertIntArray(typeTierHolder.get(i).get(j).toArray(new Integer[0]));
            }
        }

        int[][] evoInt = new int[evoHolder.size()][];
        for (int i = 0; i < evoInt.length; i++)
        {
            evoInt[i] = convertIntArray(evoHolder.get(i).toArray(new Integer[0]));
        }

        int[][] strInt = new int[strHolder.size()][];
        for (int i = 0; i < strInt.length; i++)
        {
            strInt[i] = convertIntArray(strHolder.get(i).toArray(new Integer[0]));
        }

        int[][] strInt3Stages = new int[strHolder3Stages.size()][];
        for (int i = 0; i < strInt3Stages.length; i++)
        {
            strInt3Stages[i] = convertIntArray(strHolder3Stages.get(i).toArray(new Integer[0]));
        }

        this.byStats = tierInt;
        this.byType = typeInt;
        this.byTypeStats = typeTierInt;
        this.byEvoLines = evoInt;
        this.starterCand = strInt;
        this.starterCand3Stages = strInt3Stages;
    }

    int getSameTier(Pokemon mon, Type type, boolean noLeg, boolean onlyEvolved, boolean forcedMix, int... prevMon)
    {
        // type = NO_TYPE for type-independent
        boolean bType = (type != Type.NO_TYPE); // true if it's type exclusive

        int tierN = (bType) ? mon.getOldTypeTier() : mon.getOldTier();
        int trueIndex = mon.getTrueIndex();
        int newMon;

        if (prevMon.length > 0) // if this argument exists
        {
            newMon = getSameTier(tierN, trueIndex, type, noLeg, onlyEvolved, forcedMix, prevMon);
        }
        else
        {
            newMon = getSameTier(tierN, trueIndex, type, noLeg, onlyEvolved, forcedMix);
        }

        return newMon;
    }

    int getSameTier(int tierN, int trueIndex, Type type, boolean noLeg, boolean onlyEvolved, boolean forcedMix, int... prevMon)
    {
        // type = NO_TYPE for type-independent
        boolean bType = (type != Type.NO_TYPE); // true if it's type exclusive

        int[] thisTier = (bType) ? byTypeStats[type.intIndex()][tierN] : byStats[tierN];
        int n = 1; // number of loop
        int lastIteration = tierN;
        boolean randDir = (random() * 100 < 50); // 50% chance of starting up/down

        int[] validTier = getValidTier(thisTier, trueIndex, noLeg, onlyEvolved, forcedMix, prevMon);

        while (validTier.length == 0) // find a tier with Pokemon in it
        {
            lastIteration = tierN;

            if (randDir)
            {
                tierN += pow(-1, n) * n;
            }
            else
            {
                tierN -= pow(-1, n) * n;
            }

            tierN = (bType) ? min(max(tierN, 0), N_TYPE_TIERS - 1) : min(max(tierN, 0), N_TIERS - 1); // constrain index search
            thisTier = (bType) ? byTypeStats[type.intIndex()][tierN] : byStats[tierN];
            validTier = getValidTier(thisTier, trueIndex, noLeg, onlyEvolved, forcedMix, prevMon);

            if (((bType) && (tierN == 0 || tierN == N_TYPE_TIERS - 1))
                    || ((!bType) && (tierN == 0 || tierN == N_TIERS - 1)))
            {
                break; // couldn't find it through this method
            }
        }

        if (validTier.length == 0) // continue search if interrupted
        {
            if (tierN == 0) // it means the search hit the bottom without looking tiers above
            {
                tierN = lastIteration; // fall back on the previous iteration

                while (validTier.length == 0)
                {
                    tierN++;
                    thisTier = (bType) ? byTypeStats[type.intIndex()][tierN] : byStats[tierN];
                    validTier = getValidTier(thisTier, trueIndex, noLeg, onlyEvolved, forcedMix, prevMon);
                }
            }
            else // it means the search hit the bottom without looking tiers below
            {
                tierN = lastIteration; // fall back on the previous iteration

                while (validTier.length == 0)
                {
                    tierN--;
                    thisTier = (bType) ? byTypeStats[type.intIndex()][tierN] : byStats[tierN];
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

    int getSameTier(Pokemon mon, Type[] type, boolean noLeg) // for multiple types
    {
        // get the type array and return Pokemon of random type
        // all types are equally likely to get chosen

        int out = 0x00;
        int len = type.length;

        for (int i = 0; i < len; i++)
        {
            float r = (float) random();
            float p = (float) 1 / (len - i);

            if (r < p) // probability is dependent on how many types are left
            {
                out = getSameTier(mon, type[i], noLeg, false, false);
                break; // end the search
            }
        }

        return out;
    }

    int getSameTier(int tierN, int trueIndex, Type[] type, boolean noLeg) // for multiple types
    {
        // get the type array and return Pokemon of random type
        // all types are equally likely to get chosen

        int out = 0x00;
        int len = type.length;

        for (int i = 0; i < len; i++)
        {
            float r = (float) random();
            float p = (float) 1 / (len - i);

            if (r < p) // probability is dependent on how many types are left
            {
                out = getSameTier(tierN, trueIndex, type[i], noLeg, false, false);
                break; // end the search
            }
        }

        return out;
    }

    private int[] getValidTier(int[] tier, int trueIndex, boolean noLeg, boolean onlyEvolved, boolean forcedMix, int... prevMon)
    {
        // gets a valid tier to get a random Pokemon from, given the conditions
        ArrayList<Integer> validTierHolder = new ArrayList<>();

        for (int i = 0; i < tier.length; i++)
        {
            if ((mons[tier[i] - 1].isLegendary()) && (noLeg)) // if it's legendary and no legendaries are allowed
            {
                continue;
            }

            if (mons[tier[i] - 1].getTrueIndex() == trueIndex) // if it's the same Pokemon
            {
                continue;
            }

            if ((onlyEvolved) && !(mons[tier[i] - 1].hasPre())) // if it isn't evolved
            {
                continue;
            }

            if (forcedMix)
            {
                int[] testParty = new int[prevMon.length + 1]; // get a new list with this Pokemon to test
                System.arraycopy(prevMon, 0, testParty, 0, prevMon.length);
                testParty[prevMon.length] = tier[i]; // append this new byte to the last slot of the list

                if (isTypeRedundant(false, testParty))
                {
                    continue;
                }
            }

            validTierHolder.add(tier[i]);
        }

        int[] out = convertIntArray(validTierHolder.toArray(new Integer[0]));
        return out;
    }

    int[] getPokemonOfType(Type type) // for one single type
    {
        return this.byType[type.intIndex()];
    }

    int[] getPokemonOfType(Type[] type) // for multiple types
    {
        int len = 0; // length of all type arrays
        for (Type t : type) // cycle types
        {
            if (t == Type.NO_TYPE)
            {
                continue;
            }
            len += byType[t.intIndex()].length;
        }

        int[] list = new int[len];
        int c = 0; // keep track of number of entries

        for (Type t : type) // cycle types
        {
            for (int i = 0; i < byType[t.intIndex()].length; i++)
            {
                list[c] = byType[t.intIndex()][i];
                c++;
            }
        }

        return list;
    }

    int getPokemonOldTier(int pokeInt, boolean bType)
    {
        Pokemon mon = mons[pokeInt - 1];
        int out = (bType) ? mon.getOldTypeTier() : mon.getOldTier();
        return out;
    }

    void printTiers(Pokemon[] mons)
    {
        for (int i = 0; i < byStats.length; i++)
        {
            System.out.println("Tier " + i + ":");

            for (int j = 0; j < byStats[i].length; j++)
            {
                System.out.println(mons[byStats[i][j] - 1].getBST());
            }
        }
    }

    int[] getRandomStarters(int starterKind)
    {
        // gives three random Pokemon as starters
        // if starterKind == 0 : completely random
        // if starterKind == 1 : starters with at least one evolution and below STARTER_BST
        // if starterKind == 2 : only give out the starting Pokemon of a 3-stage evolution
        // always returns the lowest Pokemon of the evo-line
        // always returns different Pokemon with different types

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

    int[] generateRandomStarters(int[][] evoLineList)
    {
        int[] out = new int[3];
        int randLine;

        // pick the first one
        randLine = (int) floor(random() * (evoLineList.length)); // random evolution line
        out[0] = evoLineList[randLine][0];

        // pick the second one different from the first and of different type
        do
        {
            randLine = (int) floor(random() * (evoLineList.length)); // random evolution line
            out[1] = evoLineList[randLine][0];
        }
        while ((out[0] == out[1]) || (isTypeRedundant(false, out[0], out[1])));

        // pick the third one different from the first two and of different type
        do
        {
            do
            {
                randLine = (int) floor(random() * (evoLineList.length)); // random evolution line
                out[2] = evoLineList[randLine][0];
            }
            while ((out[1] == out[2]) || (areSameType(mons[out[1] - 1], mons[out[2] - 1])));
        }
        while ((out[0] == out[2]) || (isTypeRedundant(false, out[0], out[1], out[2])));

        return out;
    }

    private boolean areSameType(Pokemon mon1, Pokemon mon2)
    {
        // gets two Pokemon and decides if they share a type
        Type[] types1 = mon1.getTypes();
        Type[] types2 = mon2.getTypes();
        boolean out = (types1[0] == types2[0] || types1[0] == types2[1] || types1[1] == types2[0] || types1[1] == types2[1]);
        return out;
    }

    private boolean isTypeRedundant(boolean total, int... monInts)
    {
        // decides if there is type redundancy in a list of Pokemon
        // if (total), then will return true if any types are repeated
        // if (!total), then will return true only if both Pokemon's types are repeated

        Pokemon[] monList = new Pokemon[monInts.length];
        for (int i = 0; i < monInts.length; i++) // translate bytes to actual Pokemon classes
        {
            monList[i] = mons[monInts[i] - 1];
        }

        boolean out = false; // assume it's not repeated

        Type[][] types = new Type[monList.length][2]; // two types per Pokemon

        for (int i = 0; i < monList.length; i++) // cycle Pokemon list
        {
            types[i] = monList[i].getTypes();
        }

        if (total) // all it takes is one repeated type
        {
            for (int i = 0; i < monList.length; i++) // cycle Pokemon list
            {
                for (int j = i + 1; j < monList.length; j++) // cycle all Pokemon ahead of list
                {
                    if ((types[i][0] == types[j][0]) || (types[i][0] == types[j][1])) // compare first type
                    {
                        out = true;
                    }
                    if ((types[i][1] == types[j][0]) || (types[i][1] == types[j][1])) // compare second type
                    {
                        out = true;
                    }

                    if (out)
                    {
                        break; // exit loop if repetition found
                    }
                }

                if (out)
                {
                    break;
                }
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
                    if (i == j)
                    {
                        continue; // skip comparing with self
                    }
                    if ((types[i][0] == types[j][0]) || (types[i][0] == types[j][1])) // compare first type
                    {
                        test1 = true;
                    }

                    if (test1)
                    {
                        break; // exit loop if repetition found
                    }
                }

                for (int j = 0; j < monList.length; j++) // second type
                {
                    if (i == j)
                    {
                        continue; // skip comparing with self
                    }
                    if ((types[i][1] == types[j][0]) || (types[i][1] == types[j][1])) // compare second type
                    {
                        test2 = true;
                    }

                    if (test2)
                    {
                        break; // exit loop if repetition found
                    }
                }

                if ((test1) && (test2))
                {
                    out = true; // only get a true result if both tests pass
                }
                if (out)
                {
                    break;
                }
            }
        }

        return out;
    }

    int[][][] generateRivalTeams(int[][] finalRivalTeam, int[][][] levels, boolean noLeg)
    {
        // generates Rival teams that have persistent Pokemon throughout the battles
        // depending on starter chosen by the player, and which is forced to be mixed
        // Pokemon evolve according to level and all have evolutionary lines except possibly for the final lead
        // Rival's starter is chosen from the in-game starter

        int[][][] rivalTeams = new int[INDEX_RIVAL.length][RIVAL_PARTY_SIZES.length][];
        int[] finalTeam = new int[RIVAL_PARTY_SIZES[RIVAL_PARTY_SIZES.length - 1]];

        for (int i = 0; i < INDEX_RIVAL.length; i++) // cycle starters (each starter will generate a different team)
        {
            // decide the evolutionary line of the starter first
            int[] starterSlot = getEvoLine(starters[i], -1);

            // generate the final team first
            ArrayList<Integer> prevMonList = new ArrayList<>(); // keep track of previous Pokemon in team
            prevMonList.add(starterSlot[starterSlot.length - 1]); // add starter evo to take into account its types

            // initialize the other slots 
            int[][] monSlots = new int[finalTeam.length - 1][];

            for (int j = 0; j < finalTeam.length; j++) // cycle through the final battle party
            {
                if (j < finalTeam.length - 1) // not the starter slot
                {
                    boolean isLead = (j == 0); // forced evolved unless it's the lead
                    int[] prevMonArray = convertIntArray(prevMonList.toArray(new Integer[0]));
                    Pokemon thisMon = mons[finalRivalTeam[i][j] - 1];

                    finalTeam[j] = getSameTier(thisMon, Type.NO_TYPE, noLeg, (!isLead), true, prevMonArray);
                    monSlots[j] = getEvoLine(finalTeam[j], -1);

                    // may result in an unevolved Pokemon, so change it to be the final form in the last battle
                    //finalTeam[j] = monSlots[j][monSlots[j].length - 1];
                    prevMonList.add(finalTeam[j]);

                }
                else
                {
                    finalTeam[j] = starterSlot[starterSlot.length - 1]; // add in the evolved starter
                }
            }

            // final team is formed, now to generate the earlier teams based on the result
            // initialize the byte arrays for each battle
            for (int j = 0; j < rivalTeams[i].length; j++)
            {
                rivalTeams[i][j] = new int[RIVAL_PARTY_SIZES[j]];
            }

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

    int[] evolveTeam(int[] monTeam, int[] lvls)
    {
        // makes Pokemon team evolve given their levels
        int[] evoTeam = new int[monTeam.length];

        for (int i = 0; i < monTeam.length; i++) // cycle team
        {
            evoTeam[i] = decideEvo(getEvoLine(monTeam[i], -1), lvls[i]);
        }

        return evoTeam;
    }

    private int[] getEvoLine(int monInt, int branch)
    {
        // gets the evolutionary line of a Pokemon in order
        // if there's a branch ahead, branch dictates what should happen
        // branch = 0, 1, ... : the appropriate branch is chosen
        // branch = -1 : a random branch is chosen

        int evoIndex = -1; // find the evolutionary line index
        int evoPos = -1; // find the position in that evo line

        for (int i = 0; i < byEvoLines.length; i++) // cycle evo-lines
        {
            for (int j = 0; j < byEvoLines[i].length; j++)
            {
                if (monInt == byEvoLines[i][j])
                {
                    evoIndex = i;
                    evoPos = j;
                    break;
                }
            }
        }

        ArrayList<Integer> outList = new ArrayList<>();

        for (int i = 0; i < byEvoLines[evoIndex].length;) // cycle line members
        {
            outList.add(byEvoLines[evoIndex][i]);
            Pokemon thisMon = mons[byEvoLines[evoIndex][i] - 1];

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
                if (i < evoPos) // the line has already been determined by input Pokemon
                {
                    i = evoPos;
                }
                else // need to choose a branch now
                {
                    int nJumps = (branch >= 0) ? branch : (int) floor(random() * thisEvos.length);

                    i++;
                    for (int j = 0; j < nJumps; j++) // jump accordingly to the proper branch
                    {
                        if (mons[byEvoLines[evoIndex][i] - 1].hasEvos())
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

        int[] out = convertIntArray(outList.toArray(new Integer[0]));
        return out;
    }

    private int decideEvo(int[] evoLine, int lvl)
    {
        // decides what evolutionary form is appropriate for level lvl
        // evolutionary methods other than leveling up are taken to be OTHER_METHODS_LEVEL

        int c = 0;
        // start from the bottom and build up
        for (int i = 0; i < evoLine.length - 1; i++)
        {
            Pokemon thisMon = mons[evoLine[i] - 1];
            int[] evos = thisMon.getEvoInt();
            byte[][] evoBytes = thisMon.getEvos();
            boolean evolve; // assume it shouldn't evolve
            int branchIndex = getBranchIndex(evos, evoLine[i + 1]);

            if ((evoBytes[branchIndex][0] == (byte) 0x01) || (evoBytes[branchIndex][0] == (byte) 0x05)) // level evolution
            {
                evolve = (lvl >= byteToValue(evoBytes[branchIndex][1]));
            }
            else // other methods
            {
                // check if the its evolution also evolves
                if (mons[evoLine[i + 1] - 1].hasEvos())
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

        int out = evoLine[c];
        return out;
    }

    private int getBranchIndex(int[] evos, int monInt)
    {
        // find the branch index to get monInt
        int out = 0;

        for (int i = 0; i < evos.length; i++)
        {
            if (monInt == evos[i])
            {
                out = i;
                break;
            }
        }

        return out;
    }

    int findEvoLineContaining(int n)
    {
        // finds the evolutionary line containing Pokemon with index n
        int out = -1;

        for (int i = 0; i < byEvoLines.length; i++)
        {
            for (int j = 0; j < byEvoLines[i].length; j++)
            {
                if (byEvoLines[i][j] == n)
                {
                    out = i;
                    break;
                }
            }
        }

        return out;
    }

    int[][] getEvoLines()
    {
        return this.byEvoLines;
    }
}
