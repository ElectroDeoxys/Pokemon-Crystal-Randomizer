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

    private ArrayList<PokemonData> dexList = new ArrayList<>();
    private PokemonGame[] mons;
    private Sprite[][] monSpr;
    private byte[][][] pal;

    private int[] indexLookup;
    private PokemonSorter<PokemonData> monSorter;

    PokedexRandomizer(PokemonData[] monData, Sprite[][] spr, byte[][][] palData)
    {
        this.monData = monData;
        this.spr = spr;
        this.palData = palData;
        monSorter = new PokemonSorter<>(monData);

        indexLookup = new int[N_POKEMON_DATA + 1];
        for (int i = 0; i < N_POKEMON_DATA + 1; i++)
        {
            indexLookup[i] = -1; // initialize the index lookup array (starts at 1)
        }
        
        randomize();
        this.mons = getPokemonArray();
        this.monSpr = getSpriteArray();
        this.pal = getPaletteArray();
    }

    private void randomize()
    {
        System.out.println("Filling in Pokedex...");

        ArrayList<Integer> evoList = new ArrayList<>();

        ArrayList<ArrayList<PokemonData>> evoLines = monSorter.getEvoLines();
        int nEvos = evoLines.size();
        
        int nDexEvos = 0; // number of evolution lines in the Pokedex
        int nDexMons = 0;
        
        ArrayList<Integer> availableLines = new ArrayList<>();
        
        for (int i = 0; i < evoLines.size(); i++)
        {
            availableLines.add(i);
        }

        // first ensure there are evo-lines with each type
        for (Type t : Type.values())
        {
            if (t == Type.NO_TYPE) continue;

            int randEvoLine = getRandomEvoLineType(t, availableLines, false);
            evoList.add(randEvoLine);
            availableLines.remove(new Integer(randEvoLine));
            nDexMons += evoLines.get(randEvoLine).size();

            nDexEvos++;
        }

        // fill the rest of the dex
        while (nDexMons < N_POKEMON)
        {
            int randEvoLine;

            do // only get the ones that don't exceed the number of Pokedex slots
            {
                randEvoLine = getRandomEvoLine(availableLines);
            }
            while (evoLines.get(randEvoLine).size() > N_POKEMON - nDexMons);

            evoList.add(randEvoLine);
            availableLines.remove(new Integer(randEvoLine));
            nDexMons += evoLines.get(randEvoLine).size();

            nDexEvos++;
        }

        int dexCount = 1; // indexLookup starts at 1

        // add all the Pokemon to the Pokedex
        for (int i = 0; i < evoList.size(); i++)
        {
            for (int j = 0; j < evoLines.get(evoList.get(i)).size(); j++)
            {
                dexList.add(evoLines.get(evoList.get(i)).get(j));
                indexLookup[evoLines.get(evoList.get(i)).get(j).getIntIndex()] = dexCount; // update indexLookup
                dexCount++;
            }
        }
    }

    private int getRandomEvoLine(ArrayList<Integer> availableLines)
    {
        // returns a random evolution line with no repeats
        return availableLines.get((int) floor(random() * availableLines.size()));
    }

    private int getRandomEvoLineType(Type type, ArrayList<Integer> availableLines, boolean includeLegendaries)
    {
        // returns a random evolution line that has given type with no repeats
        ArrayList<PokemonData> typeArray = monSorter.getPokemonOfType(type);
        int randEvoLine;
        PokemonData randMon;

        do
        {
            randMon = typeArray.get((int) floor(random() * typeArray.size()));
            randEvoLine = monSorter.findEvoLineContaining(randMon);
        }
        while (!availableLines.contains(randEvoLine)
                || (randMon.isLegendary() && !includeLegendaries)
                || (type == Type.NORMAL && randMon.isNormalFlying())
                || (type == Type.FLYING && randMon.isBugFlying()));

        return randEvoLine;
    }

    private PokemonGame[] getPokemonArray()
    {
        PokemonGame[] mons = new PokemonGame[dexList.size()];

        for (int i = 0; i < dexList.size(); i++)
        {
            PokemonData mon = dexList.get(i); // get the index of this Pokemon

            byte[][] newEvo;

            if (mon.hasEvos())
            {
                byte[][] evo = mon.getEvos();
                int[] evoIndex = mon.getEvoIndex();

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
                newEvo = mon.getEvos();
            }

            byte[] newPreEvo;

            if (mon.hasPre())
            {
                int[] preEvo = mon.getPreEvoIndex();
                newPreEvo = new byte[1];
                newPreEvo[0] = valueToByte(indexLookup[preEvo[0]]);
            }
            else
            {
                newPreEvo = new byte[0];
            }

            mons[i] = mon.convertPokemon(i, newEvo, newPreEvo);

            // set old tiers to have a coherent comparison in strength in the new Dex
            mons[i].setOldTier(monData[i].getTier());
            mons[i].setOldTypeTier(monData[i].getTypeTier());
        }

        return mons;
    }

    private Sprite[][] getSpriteArray()
    {
        Sprite[][] monSpr = new Sprite[2][];
        monSpr[0] = new Sprite[dexList.size()]; // for Pokemon
        monSpr[1] = new Sprite[1]; // for Unown slot

        for (int i = 0; i < dexList.size(); i++)
        {
            if (i != INDEX_UNOWN - 1) // if the replacement is Unown, replace with random forme
            {
                monSpr[0][i] = (dexList.get(i).getIntIndex() == INDEX_UNOWN) 
                        ? spr[1][(int) floor(random() * spr[1].length)] 
                        : spr[0][dexList.get(i).getIntIndex() - 1];
            }
            else
            {
                monSpr[0][i] = spr[0][INDEX_UNOWN - 1]; // replace the normal slot with a null sprite
                monSpr[1][0] = spr[0][dexList.get(i).getIntIndex() - 1]; // and replace the first Unown array index with its sprite
            }
        }

        return monSpr;
    }

    private byte[][][] getPaletteArray()
    {
        byte[][][] pal = new byte[dexList.size()][2][4];

        for (int i = 0; i < dexList.size(); i++)
        {
            pal[i] = palData[dexList.get(i).getIntIndex() - 1];
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
