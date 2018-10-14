package engine;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Move;

class MoveSorter
{
	private byte[][] byCalcPower; // power taking into account accuracy and multiple hits
	private Move[] moves;
	private Move[] movesTM;
	private byte[] critAnims; // moves with animations linked to higher crit rates
	
	MoveSorter(Move[] moves, Move[] movesTM, byte[] critAnims) 
	{
		this.moves = moves;
		this.movesTM = movesTM;
		this.critAnims = critAnims;
		
		sortMoves();
	}
	
	void sortMoves()
	{	
		// list damaging from 0 BP to 100 in discrete tiers
		int bot = 0;
		int up = 100;
		int span = (up - bot)/N_MOVE_TIERS;
		
		// list status from 10 PP to 35 PP in discrete tiers
		int botPP = 10;
		int upPP = 35;
		int spanPP = (upPP - botPP)/N_MOVE_TIERS;
		
		ArrayList<ArrayList<Byte>> tierHolder = new ArrayList<ArrayList<Byte>>();
		
		for (int i = 0; i < N_MOVE_TIERS; i++) // construct the 2-dimensional array tier list
			tierHolder.add(new ArrayList<Byte>());
		
		for (int i = 0; i < moves.length; i++)
		{
			byte[] eff = moves[i].getEffect();
			int basePower = byteToValue(moves[i].getBasePower());
			int indexTier = 0;
			float accFloat = byteToValue(moves[i].getAcc());
			float acc = (accFloat / 255);
			
			if (basePower > 1) // damaging move with constant base power
			{
				int calcPower = basePower;

				if (eff[0] != (byte) 0x11) // taking accuracy into account
					calcPower = (int) round((float) (calcPower * acc));
					
				if (hasCritAnim(moves[i])) // taking into account crit
					calcPower = (int) round((1 + 2 * (1/4)) * calcPower);
				else if (eff[0] != (byte) 0x29 && eff[0] != (byte) 0x94) // other moves that can crit
					calcPower = (int) round((1 + 2 * (1/16)) * calcPower);
				
				if (eff[0] == (byte) 0x1D) // 2-5 multiple hits hits on average 3 times
					calcPower = 3 * calcPower;
				else if (eff[0] == (byte) 0x2C || eff[0] == (byte) 0x4D) // always hits twice
					calcPower = 2 * calcPower;
				else if (eff[0] == (byte) 0x68) // hits three times with higher BP
					calcPower = 6 * calcPower;
				else if (eff[0] == (byte) 0x27 || eff[0] == (byte) 0x4B || eff[0] == (byte) 0x50 || eff[0] == (byte) 0x97) // two-turn attacks
					calcPower = calcPower/2;
				else if (eff[0] == (byte) 0x08) // dream eater
					calcPower = 1;
				
				indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
				
				if (eff[0] == (byte) 0x67) // increased priority moves get bumped up a tier
					indexTier = (int) min(indexTier + 1, N_MOVE_TIERS - 1);
				
				moves[i].setCalcPower(calcPower);
			}
			else if (basePower == 1) // counter damage & other attacks
			{
				if (eff[0] == (byte) 0x79 // return
				 || eff[0] == (byte) 0x7A // present
				 || eff[0] == (byte) 0x7B)// frustration 
					indexTier = MOVE_BOT_TIER;
				else if (eff[0] == (byte) 0x59 || eff[0] == (byte) 0x57 || eff[0] == (byte) 0x58 || eff[0] == (byte) 0x90) // countering/level-based
					indexTier = MOVE_MID_TIER;
				else if (eff[0] == (byte) 0x28 || eff[0] == (byte) 0x68) // halving/reversal
					indexTier = MOVE_2ND_TIER;
				else if (eff[0] == (byte) 0x7E) // magnitude has average power of 71
				{
					int calcPower = (int) round(71 * acc);
					indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
					moves[i].setCalcPower(calcPower);
				}
				else if (eff[0] == (byte) 0x87) // hidden power has average power of 40
				{
					int calcPower = (int) round(40 * acc);
					indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
					moves[i].setCalcPower(calcPower);
				}			
			}
			else // status moves
			{
				int pP = byteToValue(moves[i].getPP()); // generally moves with lower PP are better
				indexTier = (N_MOVE_TIERS - 1) - (int) min(floor(max(pP - botPP, 0) / spanPP), N_MOVE_TIERS - 1);
				
				// list some exceptions
				if (eff[0] == (byte) 0x32 // sharply raise ATK
				 || eff[0] == (byte) 0x34 // sharply raise SATK
				 || eff[0] == (byte) 0x35 // sharply raise SPD
				 || eff[0] == (byte) 0x84 //
				 || eff[0] == (byte) 0x85 //
				 || eff[0] == (byte) 0x86 //
				 || (eff[0] == (byte) 0x20 && moves[i].getIndex() != (byte) 0x9C)) // recovery (not rest)
					indexTier = MOVE_TOP_TIER;
				else if (eff[0] == (byte) 0x23 
					  || eff[0] == (byte) 0x41)
					indexTier = MOVE_2ND_TIER;
				else if (eff[0] == (byte) 0x01 // cause sleep
					  || eff[0] == (byte) 0x21 
					  || eff[0] == (byte) 0x31 
					  || eff[0] == (byte) 0x42 
					  || eff[0] == (byte) 0x43)
				{
					if (acc >= 0.75 ) // accurate status inducing moves
						indexTier = MOVE_TOP_TIER;
					else // inaccurate
						indexTier = MOVE_MID_TIER;
				}
				else if (eff[0] == (byte) 0x52 // mimic
					  || eff[0] == (byte) 0x1A // bide
					  || eff[0] == (byte) 0x72 // perish song
					  || eff[0] == (byte) 0x5E // lock-on
					  || eff[0] == (byte) 0x8F // psych up
					  || eff[0] == (byte) 0x26 // OHKO
					  || eff[0] == (byte) 0x6B // nightmare
					  || eff[0] == (byte) 0x64 // teleport
					  || eff[0] == (byte) 0x12 // 1 stage lowering 
					  || eff[0] == (byte) 0x13 
					  || eff[0] == (byte) 0x14 
					  || eff[0] == (byte) 0x17 
					  || eff[0] == (byte) 0x18
					  || eff[0] == (byte) 0x73 // treat weather effects separately 
					  || eff[0] == (byte) 0x88 
					  || eff[0] == (byte) 0x89 
					  || eff[0] == (byte) 0x61)
					indexTier = MOVE_BOT_TIER;
				else if (eff[0] == (byte) 0x0A // 1 stage raising
					  || eff[0] == (byte) 0x0B 
					  || eff[0] == (byte) 0x0D
					  || (eff[0] == (byte) 0x20 && moves[i].getIndex() == (byte) 0x9C) // rest
					  || eff[0] == (byte) 0x10 // protect
					  || eff[0] == (byte) 0x6D // curse
					  || eff[0] == (byte) 0x6F 
					  || eff[0] == (byte) 0x74) // endure
					indexTier = MOVE_MID_TIER;
			}
			
			// min (max) are for under (above) the specified bounds
			
			tierHolder.get(indexTier).add(moves[i].getIndex());
			moves[i].setTier(indexTier);
		}
		
		byte[][] tierByte = new byte[N_MOVE_TIERS][];
		
		for (int i = 0; i < N_MOVE_TIERS; i ++)
			tierByte[i] = convertByteArray(tierHolder.get(i).toArray(new Byte[0]));

		this.byCalcPower = tierByte;
	}
	
	byte[] getTierMoveset(byte[] lvlMoves, byte[] eggMoves, boolean[] moveComp, byte[] monTypes, byte[] baseStats, byte lvl)
	{
		ArrayList<Move> newMoves = new ArrayList<Move>();
		int maxPower; // limit damage output in TMs and egg moves
		int maxTier; // limit status tiers 
		
		if (byteToValue(lvl) <= 20)
		{
			maxPower = 60;
			maxTier = (int) floor((N_MOVE_TIERS - 1)/3);
		}
		else if (byteToValue(lvl) <= 25)
		{
			maxPower = 80;
			maxTier = (int) floor((N_MOVE_TIERS - 1)/2);
		}
		else
		{
			maxPower = 0xFF; // no max power
			maxTier = (int) N_MOVE_TIERS - 1;
		}
		
		// get all the best level up moves first
		if (lvlMoves.length <= 4) // smaller movepool with no selection options
		{
			for (int i = 0; i < 4; i++)
				if (lvlMoves.length - 1 - i >= 0)
				{
					if (newMoves.contains(moves[byteToValue(lvlMoves[lvlMoves.length - 1 - i]) - 1])) // avoid repeats
						continue;
						
					newMoves.add(moves[byteToValue(lvlMoves[lvlMoves.length - 1 - i]) - 1]);
				}
		}
		else 
		{
			for (int i = 0; i < lvlMoves.length; i++) // run through all the level moves
			{
				if (newMoves.contains(moves[byteToValue(lvlMoves[i]) - 1])) // avoid repeats
					continue;
				
				if (newMoves.size() < 4) // first 4 moves are free
					newMoves.add(moves[byteToValue(lvlMoves[i]) - 1]);
				else // do some selection
				{
					Move stackMove = moves[byteToValue(lvlMoves[i]) - 1];
					compareMoves(stackMove, newMoves, monTypes, baseStats);
				}
			}
		}
		
		// run through the TM/HM/Move tutors 
		for (int i = 0; i < movesTM.length; i++) // run through all the TM moves
		{
			if (!moveComp[i]) // if not compatible, skip
				continue;
			if (movesTM[i].getCalcPower() > 1 && movesTM[i].getCalcPower() > maxPower) // skip powerful moves
				continue;
			if (movesTM[i].getCalcPower() <= 1 && movesTM[i].getTier() > maxTier) // skip powerful moves
				continue;
			if (newMoves.contains(movesTM[i])) // avoid repeats
				continue;
				
			byte[] eff = movesTM[i].getEffect();
			
			if (eff[0] == (byte) 0x21)
				if ((byteToValue(baseStats[1]) >= 70 || byteToValue(baseStats[4]) >= 70) // it has good attacking stats
					 || (byteToValue(baseStats[1]) + byteToValue(baseStats[4]) > byteToValue(baseStats[2]) + byteToValue(baseStats[5]))) // or is more offensive
					continue; // skip Toxic
					
			if (eff[0] == (byte) 0x69)
				continue; // skip Thief
			
			if (eff[0] == (byte) 0x87)
				continue; // skip Hidden Power
			
			if (newMoves.size() < 4) // first 4 moves are free
				newMoves.add(movesTM[i]);
			else // do some selection
			{
				Move stackMove = movesTM[i];
				compareMoves(stackMove, newMoves, monTypes, baseStats);
			}
		}
		
		// run through the egg moves 
		for (int i = 0; i < eggMoves.length; i++) // run through all the egg moves
		{
			if (moves[byteToValue(eggMoves[i]) - 1].getCalcPower() > 1
				&& moves[byteToValue(eggMoves[i]) - 1].getCalcPower() > maxPower) // skip powerful moves
				continue;
			if (moves[byteToValue(eggMoves[i]) - 1].getCalcPower() <= 1
				&& moves[byteToValue(eggMoves[i]) - 1].getTier() > maxTier) // skip powerful moves
				continue;
				
			if (newMoves.contains(moves[byteToValue(eggMoves[i]) - 1])) // avoid repeats
				continue;
			
			if (newMoves.size() < 4) // first 4 moves are free
				newMoves.add(moves[byteToValue(eggMoves[i]) - 1]);
			else // do some selection
			{
				Move stackMove = moves[byteToValue(eggMoves[i]) - 1];
				compareMoves(stackMove, newMoves, monTypes, baseStats);
			}
		}
		
		byte[] newMovesByte = new byte[4];
		
		for (int i = 0; i < 4; i++)
		{
			if (i < newMoves.size())
				newMovesByte[i] = newMoves.get(i).getIndex();
			else
				newMovesByte[i] = (byte) 0x00;
		}
		
		return newMovesByte;
	}
	
	private int getPower(Move move, byte[] types, byte atk, byte satk)
	{
		int out;
		byte moveType = move.getType();
		byte moveCat = move.getCat();
		byte[] eff = move.getEffect();
		
		if (move.getCalcPower() <= 1) 
			out = move.getCalcPower(); // do nothing with non-damaging moves
		else
		{
			out = (moveType == types[0] || moveType == types[1] && (eff[0] != (byte) 0x29 && eff[0] != (byte) 0x94)) ? (int) (move.getCalcPower() * 1.5) : move.getCalcPower(); // apply STAB except to Future Sight
			
			if (moveCat == 0b01000000) // physical/special move takes into account atk/satk
				out = out * byteToValue(atk);
			else
				out = out * byteToValue(satk);
		}
			
		return out;
	}
	
	private void compareMoves(Move move, ArrayList<Move> moveSet, byte[] monTypes, byte[] baseStats)
	{
		int movePower; // calculate power taking into account STAB
		boolean replaced = false;
		movePower = getPower(move, monTypes, baseStats[1], baseStats[4]);
		
		// look first to replace non-damaging
		for (int i = 0; i < 4; i++) // look into each move slot
		{
			if (moveSet.get(i).getCalcPower() > 1)
				continue;
				
			if (move.getTier() > moveSet.get(i).getTier())
			{
				moveSet.set(i, move);
				replaced = true;
				break; // no need looking more
			}
			else if (movePower <= 1 && move.getTier() == moveSet.get(i).getTier() && random() * 100 < 50) // 50% chance to replace same tier
			{
				moveSet.set(i, move);
				replaced = true;
				break; // no need looking more
			}
		}
		
		int nDam = numberDamaging(moveSet);
		
		if (!replaced) // if already replaced, skip
			if (!(nDam <= 1 && movePower <= 1))
			{
				// now look into the damaging moves
				for (int i = 0; i < 4; i++) // look into each move slot
				{
					Move thisSlotMove = moveSet.get(i);
					byte[] eff = thisSlotMove.getEffect();
					
					if (replaced)
						break;
					
					if (thisSlotMove.getCalcPower() <= 1)
						continue;
					
					int slotMovePower;
					slotMovePower = getPower(thisSlotMove, monTypes, baseStats[1], baseStats[4]);
					
					if (movePower > slotMovePower // if stack is stronger
						|| (eff[0] == (byte) 0x94 && movePower * 2 > slotMovePower && move.getType() != (byte) 0x00)) // or if compared to Future Sight it can hit super-effective
					{
						if (!(repeatedType(move, moveSet, i)) // if there isn't another move of this type
							&& !(move.getType() == (byte) 0x00 && movePower < 2 * slotMovePower && eff[0] != (byte) 0x94)) // if move isn't normal-type replacing coverage moves
						{
							moveSet.set(i, move);
							break; // no need looking more
						}
					}
					else if ((movePower < slotMovePower) && (nDam > 1 && move.getType() != thisSlotMove.getType()) // check if there is type redundancy with another damaging move
							  && (movePower * 2 > slotMovePower)) // if it isn't a really weak coverage move to justify overwriting
					{
						for (int j = i+1; j < 4; j++) // look into moves slots ahead
						{
							Move nextSlotMove = moveSet.get(j);
							
							if (nextSlotMove.getCalcPower() <= 1)
								continue; // skip non-damaging moves
							if (nextSlotMove.getType() != thisSlotMove.getType())
								continue; // skip moves of other types
							
							if (nextSlotMove.getCalcPower() < thisSlotMove.getCalcPower()) // if that one is weaker, replace that one
								moveSet.set(j, move);
							else // if that one is the stronger one, replace this one
								moveSet.set(i, move);
								
							replaced = true;
							break;
						}
					}
				}
			}
	}
	
	private boolean hasCritAnim(Move move)
	{
		// returns whether it has higher critical hit ratio
		boolean out = false; // assume it isn't
		for (int i = 0; i < critAnims.length; i++)
			if (critAnims[i] == move.getAnimIndex())
				out = true;
		return out;
	}
	
	private int numberDamaging(ArrayList<Move> moveSet) // returns number of damaging moves in a moveset
	{
		int count = 0;
		
		for (int j = 0; j < 4; j++) // look into each move slot
			if (moveSet.get(j).getCalcPower() > 1)
				count ++;
			
		return count;
	}
	
	private boolean repeatedType(Move move, ArrayList<Move> moveSet, int i)
	{
		boolean repType = false;
						
		for (int j = 0; j < 4; j++) // look into other moves to avoid repeating type
		{
			if (i==j || moveSet.get(j).getCalcPower() <= 1)
				continue; // if it's the same move, or the move is not damaging, skip
			
			if (move.getType() == moveSet.get(j).getType())
			{
				repType = true;
				break;
			}
		}
		
		return repType;
	}
}