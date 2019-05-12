package engine;

import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

import static data.Constants.*;
import data.Route;
import data.Trainer;
import data.PokemonGame;
import data.Move;
import data.Sprite;

class RomWriter
{
    private FileChannel ch = null;

    RomWriter(FileChannel ch)
    {
        this.ch = ch;
    }

    static void writeToRom(FileChannel ch, byte b, int pos) throws IOException
    {
        byte[] bA = new byte[1];
        bA[0] = b;
        writeToRom(ch, bA, pos);
    }

    static void writeToRom(FileChannel ch, byte[] b, int pos) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(b.length);
        buffer.put(b);
        buffer.flip();
        ch.write(buffer, pos);
    }

    static void writeToRom(FileChannel ch, ByteBuffer buffer, int pos) throws IOException
    {
        buffer.flip();
        ch.write(buffer, pos);
        buffer.rewind();
        buffer.clear();
    }

    void randomizeStarters(PokemonSorter monSorter, int starterKind) throws IOException
    {
        byte[] str = valueToByte(monSorter.getRandomStarters(starterKind)); // array with the new starters

        for (int i = 0; i < 3; i++)
        {
            for (int pos : OFFSET_STARTERS[i])
            {
                writeToRom(ch, str[i], pos);
            }
        }
    }

    void replaceRoutePokemon(Route route) throws IOException
    {
        byte[] b = new byte[route.getTotalSlots() * 2];

        for (int i = 0; i < route.getTotalSlots(); i++)
        {
            b[2 * i] = route.getLvl(i);
            b[2 * i + 1] = route.getPokeByte(i);
        }

        writeToRom(ch, b, route.getOffset());
    }

    void replaceAllRoutePokemon(Route[] routes) throws IOException
    {
        for (int i = 0; i < Route.indexBreaks[Route.indexBreaks.length - 1]; i++)
        {
            replaceRoutePokemon(routes[i]);
        }
    }

    void replaceTrainer(Trainer trainer, int size, int pos) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(size);

        buffer.put(trainer.getName());
        buffer.put((byte) 0x50); // name terminator
        buffer.put(trainer.getKind());
        buffer.put(trainer.getStatExp());

        for (int i = 0; i < trainer.getPartySize(); i++)
        {
            buffer.put(trainer.getPokeBytes(i));
        }

        writeToRom(ch, buffer, pos);
        //System.out.println(new String(convertBytesToText(trainer.getName())) + ": " + byteToValue(trainer.getStatExp()));
    }

    void replaceAllTrainers(Trainer[] trainers) throws IOException
    {
        int pos = OFFSET_TRAINERS;
        int ptrPos = OFFSET_TRAINERS_POINTERS;

        for (int i = 0; i < TRAINER_GROUPS.length; i++) // cycle trainer groups
        {
            // update pointer table
            writeToRom(ch, getPointer(pos), ptrPos);

            ptrPos += 2;

            if (i == 9)
            {
                continue; // unused trainer group
            }
            int next = (i == TRAINER_GROUPS.length - 1) ? N_TRAINERS : TRAINER_GROUPS[i + 1]; // protect against undefined array

            for (int j = TRAINER_GROUPS[i]; j < next; j++) // cycle indices of this trainer group
            {
                int size = trainers[j].getTotalSize();
                replaceTrainer(trainers[j], size, pos);
                pos += size;

                writeToRom(ch, (byte) 0xFF, pos);

                pos++;
            }
        }
    }

    void replaceAllPokemon(PokemonGame[] mons) throws IOException
    {
        int pos1 = OFFSET_POKEMON_2; // keep track of positions evo/moves
        int pos2 = OFFSET_POKEMON_3; // keep track of position egg moves

        for (int i = 0; i < N_POKEMON; i++)
        {
            /////////////////////////////////////
            // first set of data at OFFSET_POKEMON_1
            // covering base stats, types, and other misc data
            /////////////////////////////////////

            ByteBuffer buffer1 = ByteBuffer.allocate(0x20);

            byte[] misc = mons[i].getMisc();
            byte[] padding =
            {
                (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0
            };

            buffer1.put(mons[i].getIndex());
            buffer1.put(mons[i].getBaseBytes());
            buffer1.put(mons[i].getTypesByte());
            buffer1.put(misc[0]);
            buffer1.put(misc[1]);
            buffer1.put(misc[2]);
            buffer1.put(misc[3]);
            buffer1.put(misc[4]);
            buffer1.put((byte) 0x64); // padding byte
            buffer1.put(misc[5]);
            buffer1.put((byte) 0x05); // padding byte
            buffer1.put(mons[i].getGfx());
            buffer1.put(padding); // padding
            buffer1.put(misc[6]);
            buffer1.put(misc[7]);
            buffer1.put(mons[i].getCompatibilitiesByte());

            writeToRom(ch, buffer1, mons[i].getOffset1());

            /////////////////////////////////////
            // second set of data at OFFSET_POKEMON_2
            // covering evolution and level-up move data
            /////////////////////////////////////
            ByteBuffer bufferEvo = ByteBuffer.allocate(mons[i].getNBytes() + 2); // plus 2 terminator bytes

            byte[][] evo = mons[i].getEvos();
            byte[][] move = mons[i].getMoves();

            if (mons[i].hasEvos())
            {
                for (byte[] e : evo)
                {
                    bufferEvo.put(e);
                }
            }

            bufferEvo.put((byte) 0x00); // evolution terminator
            
            for (byte[] m : move)
            {
                bufferEvo.put(m);
            }

            bufferEvo.put((byte) 0x00); // move terminator

            writeToRom(ch, bufferEvo, pos1);

            // update pointer table
            writeToRom(ch, getPointer(pos1), OFFSET_POINTERS_1 + 2 * i);

            pos1 += mons[i].getNBytes() + 2; // set position for the next entry

            /////////////////////////////////////
            // third set of data at OFFSET_POKEMON_3
            // covering egg move data
            /////////////////////////////////////
            byte[] eggMoves = mons[i].getEggMoves();

            if (eggMoves.length > 0) // if it has egg moves
            {
                ByteBuffer bufferEgg = ByteBuffer.allocate(eggMoves.length + 1); // plus a terminator byte

                bufferEgg.put(eggMoves);
                bufferEgg.put((byte) 0xFF); // terminator

                writeToRom(ch, bufferEgg, pos2);

                // update pointer table		
                writeToRom(ch, getPointer(pos2), OFFSET_POINTERS_2 + 2 * i);

                pos2 += eggMoves.length + 1; // set position for the next entry
            }

            /////////////////////////////////////
            // name
            /////////////////////////////////////
            byte[] nameWrite = new byte[NAME_LEN];
            byte[] name = mons[i].getName();

            System.arraycopy(name, 0, nameWrite, 0, name.length); // fill with the name

            for (int j = name.length; j < nameWrite.length; j++) // fill with terminating bytes
            {
                nameWrite[j] = (byte) 0x50;
            }

            writeToRom(ch, nameWrite, OFFSET_POKEMON_NAMES + 10 * i);

            /////////////////////////////////////
            // icon
            /////////////////////////////////////
            writeToRom(ch, mons[i].getIcon(), OFFSET_POKEMON_ICONS + i);
        }

        // Pokemon with no egg moves need an 0xFF byte terminator at the end of the egg move list to point to
        writeToRom(ch, (byte) 0xFF, pos2); // pos2 is already the ending offset

        for (int i = 0; i < N_POKEMON; i++)
        {
            byte[] eggMoves = mons[i].getEggMoves();

            if (eggMoves.length > 0) // if it has egg moves
            {
                continue; // skip
            }
            writeToRom(ch, getPointer(pos2), OFFSET_POINTERS_2 + 2 * i);
        }
    }

    void replaceMove(Move move) throws IOException
    {
        byte[] b = new byte[7];
        byte[] effect = move.getEffect();

        b[0] = move.getIndex();
        b[1] = effect[0];
        b[2] = move.getBasePower();
        b[3] = move.getTypeCat();
        b[4] = move.getAcc();
        b[5] = move.getPP();
        b[6] = effect[1];

        writeToRom(ch, b, move.getOffset());
    }

    void replaceLearnableMove(Move move, int n) throws IOException
    {
        writeToRom(ch, move.getIndex(), OFFSET_TM_MOVES + n);
    }

    void replaceAllMoves(Move[] moves, Move[] movesTM) throws IOException
    {
        for (int i = 0; i < N_MOVES; i++)
        {
            replaceMove(moves[i]);
        }

        for (int i = 0; i < N_TM + N_HM + N_MOVE_TUTOR; i++)
        {
            replaceLearnableMove(movesTM[i], i);
        }
    }

    void replaceSprite(Sprite sprite, int n, boolean isUnown) throws IOException
    {
        int[] pos = sprite.getOffset();

        writeToRom(ch, sprite.getFront(), pos[0]);
        writeToRom(ch, sprite.getBack(), pos[1]);

        // update the sprite dimensions
        if (isUnown)
        {
            writeToRom(ch, sprite.getDim(), OFFSET_POKEMON_1 + 0x11 + (0x20 * (INDEX_UNOWN - 1)));
        }
        else
        {
            writeToRom(ch, sprite.getDim(), OFFSET_POKEMON_1 + 0x11 + (0x20 * n));
        }

        // update pointer
        if (!(n == (INDEX_UNOWN - 1) && !isUnown))
        {
            byte[][] ptrs = sprite.getSpritePointer();

            if (isUnown) // if it's the Unown slot
            {
                writeToRom(ch, ptrs[0], OFFSET_SPRITE_POINTERS_U + 6 * n);
                writeToRom(ch, ptrs[1], OFFSET_SPRITE_POINTERS_U + 6 * n + 3);
            }
            else
            {
                writeToRom(ch, ptrs[0], OFFSET_SPRITE_POINTERS + 6 * n);
                writeToRom(ch, ptrs[1], OFFSET_SPRITE_POINTERS + 6 * n + 3);
            }
        }

        if (n == (INDEX_UNOWN - 1) && !isUnown) // replace Unown's pointer with 0xFF
        {
            byte[] ptr =
            {
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
            };
            writeToRom(ch, ptr, OFFSET_SPRITE_POINTERS + 6 * n);
        }
    }

    void replaceTrainerSprite(Sprite sprite, int n) throws IOException
    {
        int[] pos = sprite.getOffset();
        writeToRom(ch, sprite.getFront(), pos[0]);

        byte[][] ptrs = sprite.getSpritePointer();
        writeToRom(ch, ptrs[0], OFFSET_TRAINER_SPRITE_POINTERS + 3 * n);
    }

    void replaceEggSprite(Sprite sprite) throws IOException
    {
        int[] pos = sprite.getOffset();
        writeToRom(ch, sprite.getFront(), pos[0]);

        byte[][] ptrs = sprite.getSpritePointer();
        writeToRom(ch, ptrs[0], OFFSET_SPRITE_POINTER_EGG);
    }

    void replaceAllSprites(Sprite[][] sprites, Sprite[] spritesTrn, Sprite spriteEgg, byte[][][] pal) throws IOException
    {
        // clear memory banks and fill it with 0x00
        for (int i = 0; i < N_SPRITE_BANKS; i++)
        {
            ByteBuffer bufferEmpty = ByteBuffer.allocate(0x4000);
            for (int j = 0; j < 0x4000; j++)
            {
                bufferEmpty.put((byte) 0x00);
            }

            writeToRom(ch, bufferEmpty, OFFSET_SPRITE_POINTERS + 0x4000 * i);
        }

        for (int i = 0; i < sprites.length; i++)
        {
            int len = (i == 0) ? sprites[i].length : N_UNOWN; // check how many times to cycle

            for (int j = 0; j < len; j++)
            {
                int k = j % sprites[i].length; // ensure the cycle happens when reaches end of array
                replaceSprite(sprites[i][k], j, (i == 1));
            }
        }

        for (int i = 0; i < spritesTrn.length; i++)
        {
            replaceTrainerSprite(spritesTrn[i], i);
        }

        // fill gap after pointer table
        byte[] fill =
        {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };
        writeToRom(ch, fill, OFFSET_SPRITE_POINTERS + 6 * N_POKEMON);
        writeToRom(ch, fill, OFFSET_SPRITE_POINTERS + 6 * N_POKEMON + 6);

        replaceEggSprite(spriteEgg);

        // overwrite palettes
        for (int i = 0; i < pal.length; i++)
        {
            writeToRom(ch, pal[i][0], OFFSET_PAL + i * 8);
            writeToRom(ch, pal[i][1], OFFSET_PAL + i * 8 + 4);
        }
    }
}
