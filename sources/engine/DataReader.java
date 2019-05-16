package engine;

import java.io.File;
import java.util.ArrayList;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

import static data.Constants.*;
import data.PokemonData;
import data.Sprite;

class DataReader
{
    PokemonData[] monData;
    Sprite[][] monSpr = new Sprite[2][]; // sprites [Pokemon, Unown Formes][Index]
    byte[][][] pal; // [Pokemon Index][Regular or Shiny][Palette bytes]

    DataReader() throws IOException
    {
        // stats & moves data
        File fileStats = new File("pokemon_data\\data_stats"); // stats data
        File fileEvoMoves = new File("pokemon_data\\data_evomoves"); // evo and moves data
        File fileEggMoves = new File("pokemon_data\\data_eggmoves"); // egg moves data
        File fileNames = new File("pokemon_data\\names.txt");

        // misc data (icons)
        File fileMisc = new File("pokemon_data\\data_misc");

        try (
                RandomAccessFile strStats = new RandomAccessFile(fileStats, "r");
                FileChannel chStats = strStats.getChannel();
                RandomAccessFile strEvoMoves = new RandomAccessFile(fileEvoMoves, "r");
                FileChannel chEvoMoves = strEvoMoves.getChannel();
                RandomAccessFile strEggMoves = new RandomAccessFile(fileEggMoves, "r");
                FileChannel chEggMoves = strEggMoves.getChannel();
                RandomAccessFile strNames = new RandomAccessFile(fileNames, "r");
                FileChannel chNames = strNames.getChannel();
                RandomAccessFile strMisc = new RandomAccessFile(fileMisc, "r");
                FileChannel chMisc = strMisc.getChannel();)
        {
            System.out.println("Reading Pokemon data...");
            monData = readPokemon(chStats, chEvoMoves, chEggMoves, chNames, chMisc);
        }

        File fileSprF = new File("pokemon_data\\sprites_front"); // file for LZ compressed sprites (front)
        File fileSprB = new File("pokemon_data\\sprites_back"); // file for LZ compressed sprites (back)
        File fileSprPtr = new File("pokemon_data\\offsets_sprites"); // file with all the offset values to sprites (front/back)

        File fileSprFU = new File("pokemon_data\\sprites_front_unown"); // file for Unown LZ compressed sprites (front)
        File fileSprBU = new File("pokemon_data\\sprites_back_unown"); // file for Unown LZ compressed sprites (back)
        File fileSprPtrU = new File("pokemon_data\\offsets_sprites_unown"); // file with all the offset values to sprites (front/back)

        File fileDim = new File("pokemon_data\\dimensions"); // dimension data
        File filePal = new File("pokemon_data\\palettes"); // palette data

        try (
                RandomAccessFile strSprF = new RandomAccessFile(fileSprF, "r");
                FileChannel chF = strSprF.getChannel();
                RandomAccessFile strSprB = new RandomAccessFile(fileSprB, "r");
                FileChannel chB = strSprB.getChannel();
                RandomAccessFile strSprPtr = new RandomAccessFile(fileSprPtr, "r");
                FileChannel chPtr = strSprPtr.getChannel();
                RandomAccessFile strSprFU = new RandomAccessFile(fileSprFU, "r");
                FileChannel chFU = strSprFU.getChannel();
                RandomAccessFile strSprBU = new RandomAccessFile(fileSprBU, "r");
                FileChannel chBU = strSprBU.getChannel();
                RandomAccessFile strSprPtrU = new RandomAccessFile(fileSprPtrU, "r");
                FileChannel chPtrU = strSprPtrU.getChannel();
                RandomAccessFile strDim = new RandomAccessFile(fileDim, "r");
                FileChannel chDim = strDim.getChannel();
                RandomAccessFile strPal = new RandomAccessFile(filePal, "r");
                FileChannel chPal = strPal.getChannel();)
        {
            System.out.println("Reading sprite data...");
            monSpr = readDataSprites(chF, chB, chPtr, chFU, chBU, chPtrU, chDim);
            pal = readDataPalettes(chPal);
        }
    }

    private byte[] readFromData(FileChannel ch, int pos, int length) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        ch.read(buffer, pos);
        return buffer.array();
    }

    private byte readByteFromData(FileChannel ch, int pos) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        ch.read(buffer, pos);
        byte[] byteHolder = buffer.array();
        return byteHolder[0];
    }

    private int lengthUntilByte(FileChannel ch, int pos, byte term) throws IOException
    {
        byte byteRead;
        int n = 0; // keep track number of loop

        while (true)
        {
            byteRead = readByteFromData(ch, pos + n);

            if (byteRead == term) // if a terminating byte
            {
                break;
            }

            n++;
        }

        return n;
    }

    private PokemonData[] readPokemon(FileChannel chStats, FileChannel chEvoMoves, FileChannel chEggMoves, FileChannel chNames, FileChannel chMisc) throws IOException
    {
        PokemonData[] monData = new PokemonData[N_POKEMON_DATA];

        ArrayList<Integer> futureEvos = new ArrayList<>(); // keep track of Pokemon after who have pre-evos
        ArrayList<Integer> futureEvosIndex = new ArrayList<>(); // store the Pokémon index that evolve into the evos

        int posEvoMoves = 0; // keep track of evomoves position
        int posEggMoves = 0; // keep track of eggmoves position
        int posName = 0; // keep track of name offset

        for (int i = 0; i < N_POKEMON_DATA; i++)
        {
            int curIndex = i + 1; // true index of the Pokemon being processed

            int[] preEvoIndex = new int[1]; // store preEvoIndex, if any
            boolean hasPre = false;

            if (futureEvos.contains(curIndex)) // check if the pre-evolution has been processed yet
            {
                preEvoIndex[0] = futureEvosIndex.get(futureEvos.indexOf(curIndex));
                hasPre = true;
            }

            /////////////////////////////////////
            // stats data
            /////////////////////////////////////
            int statsLen = 33; // number of bytes of each entry

            int pos = statsLen * i;
            pos += 2; // index

            byte[] base = readFromData(chStats, pos, 6); // read base stats
            pos += 6;

            byte[] type = readFromData(chStats, pos, 2); // read types
            pos += 2;

            byte[] misc = new byte[9];

            byte[] misc1 = readFromData(chStats, pos, 5);
            pos += 5 + 1; // 1 padding byte
            misc[5] = readByteFromData(chStats, pos); // step cycles
            pos += 1 + 1; // 1 padding byte
            byte gfx = (byte) 0x00; // gfx
            pos += 1 + 4; // 4 padding bytes
            byte[] misc2 = readFromData(chStats, pos, 2); // growth rate and egg group
            pos += 2;
            byte[] tmhm = readFromData(chStats, pos, 8);

            System.arraycopy(misc1, 0, misc, 0, misc1.length);
            System.arraycopy(misc2, 0, misc, 6, misc2.length);

            /////////////////////////////////////
            // evolution and level-up move data
            /////////////////////////////////////
            pos = posEvoMoves; // set position to second set

            ArrayList<Byte[]> evoList = new ArrayList<>();
            ArrayList<Integer> evoIndexList = new ArrayList<>();

            if (readByteFromData(chEvoMoves, pos) == (byte) 0x00) // no evolutions
            {
                Byte[] evoNull = new Byte[0];
                evoList.add(evoNull);
            }
            else
            {
                while (readByteFromData(chEvoMoves, pos) != (byte) 0x00)
                {
                    Byte[] evo;
                    Integer evoIndex; // declare new evolution's true index

                    switch (readByteFromData(chEvoMoves, pos))
                    {
                        case (byte) 0x01: // by level up
                        case (byte) 0x02: // by item
                        case (byte) 0x03: // by trading
                        case (byte) 0x04: // by happiness
                            evo = convertByteArray(readFromData(chEvoMoves, pos, 2));
                            evoIndex = byteArrayToInt(readFromData(chEvoMoves, pos + 2, 2), true);
                            pos += 2 + 2;
                            break;

                        case (byte) 0x05: // by stats
                            evo = convertByteArray(readFromData(chEvoMoves, pos, 3));
                            evoIndex = byteArrayToInt(readFromData(chEvoMoves, pos + 3, 2), true);
                            pos += 3 + 2;
                            break;
                        default:
                            evo = new Byte[0];
                            evoIndex = 0xFF;
                            break;
                    }

                    evoList.add(evo); // append that evolution to the list
                    evoIndexList.add(evoIndex);

                    if (evoIndex < curIndex) // if evolution has already been processed
                    {
                        int[] preEvoByte = new int[1];
                        preEvoIndex[0] = curIndex;
                        monData[evoIndex - 1].setPreEvoIndex(preEvoIndex);
                    }
                    else // check afterwards to determine it's an evolution form and give it the index
                    {
                        futureEvos.add(evoIndex);
                        futureEvosIndex.add(curIndex);
                    }
                }
            }

            byte[][] evoArray = new byte[evoList.size()][];
            int[] evoIndexArray = new int[evoIndexList.size()];

            for (int j = 0; j < evoArray.length; j++)
            {
                evoArray[j] = convertByteArray(evoList.get(j));
                evoIndexArray = convertIntArray(evoIndexList.toArray(new Integer[0]));
            }

            pos++; // position for moves	

            ArrayList<Byte[]> moveList = new ArrayList<>();

            while (readByteFromData(chEvoMoves, pos) != (byte) 0x00) // read moves
            {
                Byte[] move = convertByteArray(readFromData(chEvoMoves, pos, 2)); // moves take 2 bytes
                moveList.add(move); // append that move to the list
                pos += 2; // adjust position for next move
            }

            byte[][] moveArray = new byte[moveList.size()][];

            for (int j = 0; j < moveArray.length; j++)
            {
                moveArray[j] = convertByteArray(moveList.get(j));
            }

            posEvoMoves = pos + 1; // setting the next offset

            monData[i] = new PokemonData(curIndex, base, type, misc, gfx, tmhm, evoArray, evoIndexArray, moveArray);

            if (hasPre)
            {
                monData[i].setPreEvoIndex(preEvoIndex);
            }

            /////////////////////////////////////
            // names
            /////////////////////////////////////
            int lenName = lengthUntilByte(chNames, posName, (byte) 0x0D);
            monData[i].setName(Names.parseTextBytes(readFromData(chNames, posName, lenName)));

            posName += lenName + 2; // two separating bytes

            /////////////////////////////////////
            // misc
            /////////////////////////////////////
            monData[i].setIcon(readByteFromData(chMisc, (0 * N_POKEMON_DATA) + i));
        }

        for (int i = 0; i < N_POKEMON_DATA; i++) // loop again
        {
            /////////////////////////////////////
            // egg move data
            /////////////////////////////////////

            int pos = posEggMoves; // position of this Pokemon's egg moves pointer

            if (readByteFromData(chEggMoves, pos) == (byte) 0xFF)
            {
                posEggMoves++;
                continue; // don't process Pokemon with no egg moves
            }

            ArrayList<Byte> eggMoveList = new ArrayList<>();

            while (readByteFromData(chEggMoves, pos) != (byte) 0xFF) // read moves
            {
                Byte move = readByteFromData(chEggMoves, pos); // moves take 1 byte
                eggMoveList.add(move); // append that move to the list
                pos++; // adjust position for next move
            }

            byte[] eggMoveArray = new byte[eggMoveList.size()];

            for (int j = 0; j < eggMoveArray.length; j++)
            {
                eggMoveArray[j] = eggMoveList.get(j);
            }

            monData[i].setEggMoves(eggMoveArray);
            monData[i].setEggMovesCarry(eggMoveArray);

            if (monData[i].hasEvos()) // check the evolutions of this Pokemon to pass on the egg moves
            {
                int[] evoIndex = monData[i].getEvoIndex();

                for (int j = 0; j < evoIndex.length; j++) // cycle all evolutions
                {
                    PokemonData evoMon = monData[evoIndex[j] - 1];
                    evoMon.setEggMovesCarry(eggMoveArray);

                    if (evoMon.hasEvos()) // if that Pokemon has evos too, set their egg moves as well
                    {
                        int[] evoIndex2 = monData[evoIndex[j] - 1].getEvoIndex();

                        for (int k = 0; k < evoIndex2.length; k++) // cycle all evolutions
                        {
                            PokemonData evoMon2 = monData[evoIndex2[k] - 1];
                            evoMon2.setEggMovesCarry(eggMoveArray);
                        }
                    }
                }
            }

            posEggMoves = pos + 1; // setting the next offset
        }

        return monData;
    }

    private Sprite[][] readDataSprites(FileChannel chF, FileChannel chB, FileChannel chPtr,
            FileChannel chFU, FileChannel chBU, FileChannel chPtrU,
            FileChannel chDim) throws IOException
    {
        Sprite[][] sprites = new Sprite[2][];
        sprites[0] = new Sprite[N_POKEMON_DATA];
        sprites[1] = new Sprite[N_UNOWN];

        int ptrOffset = 0;
        int ptrOffsetU = 0;

        for (int i = 0; i < N_POKEMON_DATA; i++)
        {
            byte[] ptr; // pointer to sprite
            int[] pos = new int[2];
            int length;

            if (i == INDEX_UNOWN - 1)
            {
                byte dim = readByteFromData(chDim, i);
                byte[] bNull = new byte[0];
                int[] posNull =
                {
                    0, 0
                }; // placeholder sprite offset
                sprites[0][i] = new Sprite(bNull, bNull, dim, posNull);

                for (int j = 0; j < N_UNOWN; j++)
                {
                    ptr = readFromData(chPtrU, 6 * j, 3);
                    pos[0] = byteArrayToInt(ptr, false);
                    length = getPicSize(chFU, pos[0]);

                    byte[] front = readFromData(chFU, pos[0], length);

                    ptr = readFromData(chPtrU, 6 * j + 3, 3);
                    pos[1] = byteArrayToInt(ptr, false);
                    length = getPicSize(chBU, pos[1]);

                    byte[] back = readFromData(chBU, pos[1], length);
                    int[] posNull1 =
                    {
                        0, 0
                    }; // placeholder sprite offset

                    sprites[1][j] = new Sprite(front, back, dim, posNull1);
                }
            }
            else
            {
                ptr = readFromData(chPtr, 6 * i, 3);
                pos[0] = byteArrayToInt(ptr, false);
                length = getPicSize(chF, pos[0]);

                byte[] front = readFromData(chF, pos[0], length);

                ptr = readFromData(chPtr, 6 * i + 3, 3);
                pos[1] = byteArrayToInt(ptr, false);
                length = getPicSize(chB, pos[1]);

                byte[] back = readFromData(chB, pos[1], length);

                byte dim = readByteFromData(chDim, i);
                int[] posNull =
                {
                    0, 0
                }; // placeholder sprite offset

                sprites[0][i] = new Sprite(front, back, dim, posNull);
            }
        }

        return sprites;
    }

    private byte[][][] readDataPalettes(FileChannel ch) throws IOException
    {
        byte[][][] pal = new byte[N_POKEMON_DATA][2][4];

        for (int i = 0; i < pal.length; i++)
        {
            pal[i][0] = readFromData(ch, i * 8, 4); // regular
            pal[i][1] = readFromData(ch, i * 8 + 4, 4); // shiny
        }

        return pal;
    }

    int getPicSize(FileChannel ch, int pos) throws IOException
    {
        // gets size of lz compressed pics that starts at position pos in the ROM

        int out = pos;
        int jump = 0;
        byte ptrPar; // pointer parameter

        byte b = readByteFromData(ch, out);

        while (b != (byte) 0xFF)
        {
            switch (b & 0b111_00000)
            {
                case (0b000_00000): // command with n parameters
                    byte bMask = (byte) 0b0001_1111;
                    jump = (byteToValue((byte) (b & bMask)) + 1) + 1;
                    break;
                case (0b001_00000): // command with one parameter
                    jump = 1 + 1;
                    break;
                case (0b010_00000): // command with two parameters
                    jump = 2 + 1;
                    break;
                case (0b011_00000): // command with no parameters
                    jump = 0 + 1;
                    break;
                case (0b100_00000): // commands with pointer parameters
                case (0b101_00000):
                case (0b110_00000):
                    ptrPar = readByteFromData(ch, out + 1);
                    if ((ptrPar & 0b1000_0000) == 0b1000_0000) // 7-bit negative offset
                    {
                        jump = 1 + 1;
                    }
                    else // 15-bit positive offset
                    {
                        jump = 2 + 1;
                    }
                    break;
                case (0b111_00000): // lz long
                    byte[] c = readFromData(ch, out, 2);
                    switch (c[0] & 0b000_111_00)
                    {
                        case (0b000_000_00): // command with n parameters
                            byte[] b2Mask = new byte[2];
                            b2Mask[0] = (byte) 0b0000_0011;
                            b2Mask[1] = (byte) 0b1111_1111;
                            jump = (byteToValue((byte) (c[0] & b2Mask[0])) * 0x100 + byteToValue((byte) (c[1] & b2Mask[1])) + 1) + 2;
                            break;
                        case (0b000_001_00): // command with one parameter
                            jump = 1 + 2;
                            break;
                        case (0b000_010_00): // command with two parameters
                            jump = 2 + 2;
                            break;
                        case (0b000_011_00): // command with no parameters
                            jump = 0 + 2;
                            break;
                        case (0b000_100_00): // commands with pointer parameters
                        case (0b000_101_00):
                        case (0b000_110_00):
                            ptrPar = readByteFromData(ch, out + 2);
                            if ((ptrPar & 0b1000_0000) == 0b1000_0000) // 7-bit negative offset
                            {
                                jump = 1 + 2;
                            }
                            else // 15-bit positive offset
                            {
                                jump = 2 + 2;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }

            out += jump;
            b = readByteFromData(ch, out);
        }
        return (out - pos + 1);
    }

    PokemonData[] getPokemonData()
    {
        return this.monData;
    }

    Sprite[][] getSprites()
    {
        return this.monSpr;
    }

    byte[][][] getPalettes()
    {
        return this.pal;
    }
}
