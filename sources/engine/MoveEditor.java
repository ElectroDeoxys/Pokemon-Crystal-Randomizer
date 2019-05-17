package engine;

import java.util.ArrayList;
import java.io.IOException;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Move;
import java.util.Arrays;

class MoveEditor
{
    private ArrayList<Move> moves;
    private ArrayList<Move> movesTM = new ArrayList<>(N_TM + N_HM + N_MOVE_TUTOR); // actual Move classes of TM, HM and move tutors
    private byte[] critAnims; // moves with animations linked to higher crit rates

    MoveEditor(RomReader romReader) throws IOException
    {
        this.moves = new ArrayList<>(Arrays.asList(romReader.readRomMoves()));
        this.critAnims = romReader.readRomCritAnimations();
        
        byte[] moveTMBytes = romReader.readRomTMs();

        for (byte moveTMByte : moveTMBytes)
        {
            movesTM.add(moves.get(byteToValue(moveTMByte) - 1));
        }
    }

    void randomizeBasePowers()
    {
        for (Move move : moves)
        {
            if (byteToValue(move.getBasePower()) > 0)
            {
                move.setBasePower((byte) (random() * 0xFF));
            }
        }
    }

    void randomizeTMs(boolean moveWSimilar)
    {
        ArrayList<Integer> tMList = new ArrayList<>();

        for (int i = 0; i < N_TM; i++)
        {

        }
    }

    void updateMoves()
    {
        moves.get(13).setPP((byte) 0x14);
        moves.get(18).setBasePower((byte) 0x5A);
        moves.get(19).setAcc((byte) 0xD9);
        moves.get(21).setBasePower((byte) 0x2D);
        moves.get(21).setPP((byte) 0x19);
        moves.get(25).setBasePower((byte) 0x64);
        moves.get(25).setPP((byte) 0x0A);
        moves.get(32).setBasePower((byte) 0x32);
        moves.get(32).setAcc((byte) 0xFF);
        moves.get(34).setAcc((byte) 0xE6);
        moves.get(36).setBasePower((byte) 0x78);
        moves.get(36).setPP((byte) 0x0A);
        moves.get(41).setBasePower((byte) 0x19);
        moves.get(41).setAcc((byte) 0xF2);
        moves.get(49).setAcc((byte) 0xFF);
        moves.get(50).setEffect((byte) 0x48); // acid lower Sp. Def
        moves.get(52).setBasePower((byte) 0x5A);
        moves.get(55).setBasePower((byte) 0x6E);
        moves.get(56).setBasePower((byte) 0x5A);
        moves.get(57).setBasePower((byte) 0x5A);
        moves.get(58).setBasePower((byte) 0x6E);
        moves.get(65).setPP((byte) 0x14);
        moves.get(66).setAcc((byte) 0xFF);
        moves.get(70).setPP((byte) 0x19);
        moves.get(71).setPP((byte) 0x0F);
        moves.get(73).setPP((byte) 0x14);
        moves.get(79).setBasePower((byte) 0x78);
        moves.get(79).setPP((byte) 0x0A);
        moves.get(82).setBasePower((byte) 0x23);
        moves.get(82).setAcc((byte) 0xD8);
        moves.get(84).setBasePower((byte) 0x5A);
        moves.get(85).setAcc((byte) 0xE5);
        moves.get(86).setBasePower((byte) 0x6E);
        moves.get(90).setBasePower((byte) 0x50);
        moves.get(91).setAcc((byte) 0xE5);
        moves.get(104).setPP((byte) 0x0A);
        moves.get(106).setPP((byte) 0x0A);
        moves.get(111).setPP((byte) 0x14);
        moves.get(121).setBasePower((byte) 0x1E);
        moves.get(122).setBasePower((byte) 0x1E);
        moves.get(125).setBasePower((byte) 0x6E);
        moves.get(126).setEffect((byte) 0x1F);
        moves.get(126).setEffectChance((byte) 0x33);
        moves.get(127).setAcc((byte) 0xD8);
        moves.get(127).setPP((byte) 0x0F);
        moves.get(129).setBasePower((byte) 0x82);
        moves.get(129).setPP((byte) 0x1A);
        moves.get(135).setBasePower((byte) 0x82);
        moves.get(135).setPP((byte) 0x0A);
        moves.get(136).setAcc((byte) 0xFF);
        moves.get(138).setAcc((byte) 0xE5);
        moves.get(140).setBasePower((byte) 0x50);
        moves.get(140).setPP((byte) 0x0A);
        moves.get(144).setBasePower((byte) 0x28);
        moves.get(147).setAcc((byte) 0xFF);
        moves.get(148).setAcc((byte) 0xFF);
        moves.get(150).setPP((byte) 0x14);
        moves.get(151).setBasePower((byte) 0x64);
        moves.get(151).setAcc((byte) 0xE5);
        moves.get(167).setBasePower((byte) 0x3C);
        moves.get(167).setPP((byte) 0x19);
        moves.get(172).setBasePower((byte) 0x32);
        moves.get(173).setTypeCat((byte) (0x08 | 0b11000000));
        moves.get(177).setAcc((byte) 0xFF);
        moves.get(183).setAcc((byte) 0xFF);
        moves.get(191).setBasePower((byte) 0x78);
        moves.get(197).setAcc((byte) 0xE5);
        moves.get(199).setBasePower((byte) 0x78);
        moves.get(199).setPP((byte) 0x0A);
        moves.get(201).setBasePower((byte) 0x4B);
        moves.get(201).setPP((byte) 0x0A);
        moves.get(206).setAcc((byte) 0xD8);
        moves.get(209).setBasePower((byte) 0x28);
        moves.get(241).setEffect((byte) 0x45); // crunch lower def
        moves.get(247).setBasePower((byte) 0x78);
        moves.get(247).setAcc((byte) 0xFF);
        moves.get(247).setPP((byte) 0x0A);
        moves.get(248).setBasePower((byte) 0x28);
        moves.get(249).setBasePower((byte) 0x23);
        moves.get(249).setAcc((byte) 0xD8);
    }

    Move[] getMoves()
    {
        return moves.toArray(new Move[0]);
    }

    Move[] getAllLearnable()
    {
        return movesTM.toArray(new Move[0]);
    }

    byte[] getAllLearnableBytes()
    {
        byte[] moveTMBytes = new byte[movesTM.size()];
        int i = 0;
        movesTM.forEach((moveTM) -> {moveTMBytes[i] = moveTM.getIndex();});
        return moveTMBytes;
    }

    byte[] getCritAnims()
    {
        return critAnims;
    }
}
