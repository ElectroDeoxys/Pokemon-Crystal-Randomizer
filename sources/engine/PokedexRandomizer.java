package engine;

import static java.lang.Math.*;
import java.util.ArrayList;

import static data.Constants.*;
import data.PokemonData;
import data.PokemonGame;
import data.Sprite;

class PokedexRandomizer
{

    private PokemonData[] monData;
    private Sprite[][] spr;
    private byte[][][] palData;

    private int[] dexArray;
    private PokemonGame[] mons;
    private Sprite[][] monSpr;
    private byte[][][] pal;

    private int[] indexLookup;

    PokedexRandomizer(PokemonData[] monData, Sprite[][] spr, byte[][][] palData)
    {
        this.monData = monData;
        this.spr = spr;
        this.palData = palData;

        indexLookup = new int[N_POKEMON_DATA + 1];
        for (int i = 0; i < N_POKEMON_DATA + 1; i++)
        {
            indexLookup[i] = -1; // initialize the index lookup array (starts at 1)
        }
        this.dexArray = randomize();
        this.mons = getPokemonArray();
        this.monSpr = getSpriteArray();
        this.pal = getPaletteArray();
    }

    private int[] randomize()
    {
        System.out.println("Filling in Pokedex...");

        ArrayList<Integer> dexList = new ArrayList<>();
        ArrayList<Integer> evoList = new ArrayList<>();

        PokemonSorter monSorter = new PokemonSorter(monData);
        int[][] evoLines = monSorter.getEvoLines();
        int nEvos = evoLines.length;
        int nDexEvos = 0; // number of evolution lines in the Pokedex
        int nDexMons = 0;

        // first ensure there are evo-lines with each type
        for (Type t : Type.values())
        {
            if (t == Type.NO_TYPE)
            {
                continue;
            }

            evoList.add(getRandomEvoLineType(monSorter, t, evoList, false));

            for (int j = 0; j < evoLines[evoList.get(t.intIndex())].length; j++)
            {
                nDexMons++;
            }

            nDexEvos++;
        }

        // fill the rest of the dex
        while (nDexMons < N_POKEMON)
        {
            int randEvoLine = getRandomEvoLine(evoList, nEvos);

            do // only get the ones that don't exceed the number of Pokedex slots
            {
                randEvoLine = getRandomEvoLine(evoList, nEvos);
            }
            while (evoLines[randEvoLine].length > N_POKEMON - nDexMons);

            evoList.add(randEvoLine);

            for (int i = 0; i < evoLines[evoList.get(nDexEvos)].length; i++)
            {
                nDexMons++;
            }

            nDexEvos++;
        }

        int dexCount = 1; // indexLookup starts at 1

        // add all the Pokemon to the Pokedex
        for (int i = 0; i < evoList.size(); i++)
        {
            for (int j = 0; j < evoLines[evoList.get(i)].length; j++)
            {
                dexList.add(evoLines[evoList.get(i)][j]);
                indexLookup[evoLines[evoList.get(i)][j]] = dexCount; // update indexLookup
                dexCount++;
            }
        }

        return convertIntArray(dexList.toArray(new Integer[0]));
    }

    private int getRandomEvoLine(ArrayList<Integer> curEvoList, int nEvos)
    {
        // returns a random evolution line with no repeats
        int randEvoLine;

        do
        {
            randEvoLine = (int) floor(random() * nEvos);
        }
        while (curEvoList.contains(randEvoLine));

        return randEvoLine;
    }

    private int getRandomEvoLineType(PokemonSorter monSorter, Type type, ArrayList<Integer> curEvoList, boolean includeLegendaries)
    {
        // returns a random evolution line that has given type with no repeats
        int[] typeArray = monSorter.getPokemonOfType(type);
        int randEvoLine;
        int randMon;

        do
        {
            randMon = typeArray[(int) floor(random() * typeArray.length)];
            randEvoLine = monSorter.findEvoLineContaining(randMon);
        }
        while (curEvoList.contains(randEvoLine)
                || (monData[randMon - 1].isLegendary() && !includeLegendaries)
                || (type == Type.NORMAL && monData[randMon - 1].isNormalFlying())
                || (type == Type.FLYING && monData[randMon - 1].isBugFlying()));

        return randEvoLine;
    }

    private PokemonGame[] getPokemonArray()
    {
        PokemonGame[] mons = new PokemonGame[dexArray.length];

        for (int i = 0; i < dexArray.length; i++)
        {
            int curIndex = dexArray[i]; // get the index of this Pokemon

            byte[][] newEvo;

            if (monData[curIndex - 1].hasEvos())
            {
                byte[][] evo = monData[curIndex - 1].getEvos();
                int[] evoIndex = monData[curIndex - 1].getEvoIndex();

                newEvo = new byte[evo.length][];

                for (int j = 0; j < newEvo.length; j++) // fill in evolution bytes accordingly
                {

                    newEvo[j] = new byte[evo[j].length + 1];
                    System.arraycopy(evo[j], 0, newEvo[j], 0, evo[j].length);

                    // check how to change indexes
                    newEvo[j][evo[j].length] = valueToByte(indexLookup[evoIndex[j]]);
                }
            }
            else
            {
                newEvo = monData[curIndex - 1].getEvos();
            }

            byte[] newPreEvo;

            if (monData[curIndex - 1].hasPre())
            {
                int[] preEvo = monData[curIndex - 1].getPreEvoIndex();
                newPreEvo = new byte[1];
                newPreEvo[0] = valueToByte(indexLookup[preEvo[0]]);
            }
            else
            {
                newPreEvo = new byte[0];
            }

            mons[i] = monData[curIndex - 1].convertPokemon(i, newEvo, newPreEvo);

            // set old tiers to have a coherent comparison in strength in the new Dex
            mons[i].setOldTier(monData[i].getTier());
            mons[i].setOldTypeTier(monData[i].getTypeTier());
        }

        return mons;
    }

    private Sprite[][] getSpriteArray()
    {
        Sprite[][] monSpr = new Sprite[2][];
        monSpr[0] = new Sprite[dexArray.length]; // for Pokemon
        monSpr[1] = new Sprite[1]; // for Unown slot

        for (int i = 0; i < dexArray.length; i++)
        {
            if (i != INDEX_UNOWN - 1) // if the replacement is Unown, replace with random forme
            {
                monSpr[0][i] = (dexArray[i] == INDEX_UNOWN) ? spr[1][(int) floor(random() * spr[1].length)] : spr[0][dexArray[i] - 1];
            }
            else
            {
                monSpr[0][i] = spr[0][INDEX_UNOWN - 1]; // replace the normal slot with a null sprite
                monSpr[1][0] = spr[0][dexArray[i] - 1]; // and replace the first Unown array index with its sprite
            }
        }

        return monSpr;
    }

    private byte[][][] getPaletteArray()
    {
        byte[][][] pal = new byte[dexArray.length][2][4];

        for (int i = 0; i < dexArray.length; i++)
        {
            pal[i] = palData[dexArray[i] - 1];
        }

        return pal;
    }

    PokemonGame[] getAllPokemon()
    {
        return mons;
    }

    Sprite[][] getAllSprites()
    {
        return monSpr;
    }

    byte[][][] getAllPalettes()
    {
        return pal;
    }

    void printPokedex(Names names)
    {
        for (int i = 0; i < N_POKEMON; i++)
        {
            System.out.println(names.pokemon(i));
        }
    }
}
