package engine;

import java.util.ArrayList;
import java.io.IOException;
import static java.lang.Math.*;

import static data.Constants.*;
import data.PokemonGame;

class PokemonEditor
{
    private PokemonGame[] mons;

    PokemonEditor(RomReader romReader) throws IOException
    {
        this.mons = romReader.readRomPokemon();

        for (int i = 0; i < mons.length; i++)
        {
            mons[i].setName(romReader.readPokemonName(i));
        }
    }

    PokemonEditor(PokedexRandomizer dexRand)
    {
        this.mons = dexRand.getAllPokemon();
    }

    void randomizeMovesets()
    {
        for (int i = 0; i < N_POKEMON; i++)
        {
            byte[][] move = mons[i].getMoves();

            for (int j = 0; j < move.length; j++)
            {
                byte[] moveRand =
                {
                    move[j][0], (byte) (random() * N_MOVES)
                };
                mons[i].setMove(j, moveRand);
            }
        }
    }

    void randomizeCompatibilities()
    {
        for (int i = 0; i < N_POKEMON; i++)
        {
            for (int j = 0; j < N_TM + N_HM + N_MOVE_TUTOR; j++)
            {
                boolean randComp = (random() * 100 < 50);
                mons[i].setCompatibility(j, randComp);
            }
        }
    }

    void fitEggMoves(byte[] learnMoves)
    {
        // egg moves may extend over its bank
        // so we have to shorten the data
        // firstly we remove TM/HM moves since they are redundant
        for (PokemonGame mon : mons)
        {
            byte[] eggMoves = mon.getEggMoves();
            if (eggMoves.length == 0)
            {
                continue; // skip Pokemon with no egg moves
            }
            ArrayList<Byte> eggMovesList = new ArrayList<>(eggMoves.length);
            for (byte eggMove : eggMoves)
            {
                eggMovesList.add(eggMove);
            }
            ArrayList<Byte> learnMovesList = new ArrayList<>(learnMoves.length);
            for (byte learnMove : learnMoves)
            {
                learnMovesList.add(learnMove);
            }
            eggMovesList.removeAll(learnMovesList); // remove from egg moves the learnable moves
            eggMoves = convertByteArray(eggMovesList.toArray(new Byte[0]));
            mon.setEggMoves(eggMoves);
        }

        int size = getEggMoveSize();

        // next we remove possible level-up moves
        int count = 0;
        while ((size > SIZE_EGG_MOVES_MEM) && (count < mons.length))
        {
            byte[] eggMoves = mons[count].getEggMoves();

            if (eggMoves.length == 0)
            {
                count++;
                continue; // skip Pokemon with no egg moves
            }

            ArrayList<Byte> eggMovesList = new ArrayList<>(eggMoves.length);

            for (byte eggMove : eggMoves)
            {
                eggMovesList.add(eggMove);
            }

            byte[][] levelUpMoves = mons[count].getMoves();
            byte[] levelMoves = new byte[levelUpMoves.length];

            for (int i = 0; i < levelUpMoves.length; i++)
            {
                levelMoves[i] = levelUpMoves[i][1];
            }

            ArrayList<Byte> levelMovesList = new ArrayList<>(levelMoves.length);

            for (byte levelMove : levelMoves)
            {
                levelMovesList.add(levelMove);
            }

            eggMovesList.removeAll(levelMovesList); // remove from egg moves the level moves

            eggMoves = convertByteArray(eggMovesList.toArray(new Byte[0]));

            mons[count].setEggMoves(eggMoves);

            size = getEggMoveSize();
            count++;
        }

        // to be sure it doesn't leak to the next bank, delete random moves
        while ((size > SIZE_EGG_MOVES_MEM))
        {
            int randIndex = (int) floor(random() * mons.length);
            byte[] eggMoves = mons[randIndex].getEggMoves();

            if (eggMoves.length == 0)
            {
                continue; // skip Pokemon with no egg moves
            }
            ArrayList<Byte> eggMovesList = new ArrayList<>(eggMoves.length);

            for (byte eggMove : eggMoves)
            {
                eggMovesList.add(eggMove);
            }

            int randEntry = (int) floor(random() * eggMovesList.size());
            eggMovesList.remove(randEntry); // remove from egg moves the level moves

            eggMoves = convertByteArray(eggMovesList.toArray(new Byte[0]));

            mons[count].setEggMoves(eggMoves);

            size = getEggMoveSize();
        }

        System.out.println("Egg moves memory: " + size + "/" + SIZE_EGG_MOVES_MEM);
    }

    private int getEggMoveSize()
    {
        int size = 0;

        // returns the number of bytes that egg moves occupies
        for (PokemonGame mon : mons)
        {
            byte[] bArray = mon.getEggMoves();
            size += bArray.length;
            size += (bArray.length > 0) ? 1 : 0; // terminating byte
        }

        size++; // overall terminating byte
        return size;
    }

    static PokemonGame getPokemonFromByte(byte x, PokemonGame[] mons)
    {
        return mons[byteToValue(x) - 1];
    }

    PokemonGame[] getAllPokemon()
    {
        return mons;
    }
}
