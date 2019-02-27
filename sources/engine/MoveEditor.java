package engine;

import java.util.ArrayList;
import java.io.IOException;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Move;

class MoveEditor
{
	private Move[] moves;
	private byte[] moveTMBytes; // has bytes of TM, HM and move tutors
	private Move[] movesTM = new Move[N_TM + N_HM + N_MOVE_TUTOR]; // actual Move classes of TM, HM and move tutors
	private byte[] critAnims; // moves with animations linked to higher crit rates
	
	MoveEditor(RomReader romReader) throws IOException
	{
		this.moves = romReader.readRomMoves();
		this.moveTMBytes = romReader.readRomTMs();
		this.critAnims = romReader.readRomCritAnimations();
		
		for (int i = 0; i < movesTM.length; i++)
			movesTM[i] = moves[byteToValue(moveTMBytes[i]) - 1];
	}
	
	void randomizeBasePowers()
	{
		for (int i = 0; i < moves.length; i++)
			if (byteToValue(moves[i].getBasePower()) > 0)
				moves[i].setBasePower((byte) (random() * 0xFF));
	}
	
	void randomizeTMs()
	{
		ArrayList<Integer> tMList = new ArrayList<Integer>();
		
		for (int i = 0; i < N_TM; i++)
		{
			int randMove;
			
			do
			{
				randMove = (int) floor(random() * N_MOVES);
			} while (tMList.contains(randMove)); // avoid repeats
			
			tMList.add(randMove);
			movesTM[i] = moves[randMove];
		}
	}
	
	void updateMoves()
	{
		moves[13].setPP((byte) 0x14);
		moves[18].setBasePower((byte) 0x5A);
		moves[19].setAcc((byte) 0xD9);
		moves[21].setBasePower((byte) 0x2D);
		moves[21].setPP((byte) 0x19);
		moves[25].setBasePower((byte) 0x64);
		moves[25].setPP((byte) 0x0A);
		moves[32].setBasePower((byte) 0x32);
		moves[32].setAcc((byte) 0xFF);
		moves[34].setAcc((byte) 0xE6);
		moves[36].setBasePower((byte) 0x78);
		moves[36].setPP((byte) 0x0A);
		moves[41].setBasePower((byte) 0x19);
		moves[41].setAcc((byte) 0xF2);
		moves[49].setAcc((byte) 0xFF);
		moves[50].setEffect((byte) 0x48); // acid lower Sp. Def
		moves[52].setBasePower((byte) 0x5A);
		moves[55].setBasePower((byte) 0x6E);
		moves[56].setBasePower((byte) 0x5A);
		moves[57].setBasePower((byte) 0x5A);
		moves[58].setBasePower((byte) 0x6E);
		moves[65].setPP((byte) 0x14);
		moves[66].setAcc((byte) 0xFF);
		moves[70].setPP((byte) 0x19);
		moves[71].setPP((byte) 0x0F);
		moves[73].setPP((byte) 0x14);
		moves[79].setBasePower((byte) 0x78);
		moves[79].setPP((byte) 0x0A);
		moves[82].setBasePower((byte) 0x23);
		moves[82].setAcc((byte) 0xD8);
		moves[84].setBasePower((byte) 0x5A);
		moves[85].setAcc((byte) 0xE5);
		moves[86].setBasePower((byte) 0x6E);
		moves[90].setBasePower((byte) 0x50);
		moves[91].setAcc((byte) 0xE5);
		moves[104].setPP((byte) 0x0A);
		moves[106].setPP((byte) 0x0A);
		moves[111].setPP((byte) 0x14);
		moves[121].setBasePower((byte) 0x1E);
		moves[122].setBasePower((byte) 0x1E);
		moves[125].setBasePower((byte) 0x6E);
		moves[126].setEffect((byte) 0x1F);
		moves[126].setEffectChance((byte) 0x33);
		moves[127].setAcc((byte) 0xD8);
		moves[127].setPP((byte) 0x0F);
		moves[129].setBasePower((byte) 0x82);
		moves[129].setPP((byte) 0x1A);
		moves[135].setBasePower((byte) 0x82);
		moves[135].setPP((byte) 0x0A);
		moves[136].setAcc((byte) 0xFF);
		moves[138].setAcc((byte) 0xE5);
		moves[140].setBasePower((byte) 0x50);
		moves[140].setPP((byte) 0x0A);
		moves[144].setBasePower((byte) 0x28);
		moves[147].setAcc((byte) 0xFF);
		moves[148].setAcc((byte) 0xFF);
		moves[150].setPP((byte) 0x14);
		moves[151].setBasePower((byte) 0x64);
		moves[151].setAcc((byte) 0xE5);
		moves[167].setBasePower((byte) 0x3C);
		moves[167].setPP((byte) 0x19);
		moves[172].setBasePower((byte) 0x32);
		moves[173].setTypeCat((byte) (0x08 | 0b11000000));
		moves[177].setAcc((byte) 0xFF);
		moves[183].setAcc((byte) 0xFF);
		moves[191].setBasePower((byte) 0x78);
		moves[197].setAcc((byte) 0xE5);
		moves[199].setBasePower((byte) 0x78);
		moves[199].setPP((byte) 0x0A);
		moves[201].setBasePower((byte) 0x4B);
		moves[201].setPP((byte) 0x0A);
		moves[206].setAcc((byte) 0xD8);
		moves[209].setBasePower((byte) 0x28);
		moves[241].setEffect((byte) 0x45); // crunch lower def
		moves[247].setBasePower((byte) 0x78);
		moves[247].setAcc((byte) 0xFF);
		moves[247].setPP((byte) 0x0A);
		moves[248].setBasePower((byte) 0x28);
		moves[249].setBasePower((byte) 0x23);
		moves[249].setAcc((byte) 0xD8);
	}
	
	Move[] getMoves()
	{
		return moves;
	}
	
	Move[] getAllLearnable()
	{
		return movesTM;
	}
	
	byte[] getAllLearnableBytes()
	{
		return moveTMBytes;
	}
	
	byte[] getCritAnims()
	{
		return critAnims;
	}
}