package engine;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Move;

class MoveSorter extends MoveAnalyser
{
	private Move[] movesTM;
	private byte[] critAnims; // moves with animations linked to higher crit rates

	private static final int BOT_POWER = 0;
	private static final int UPR_POWER = 100;
	private static final int BOT_PP = 10;
	private static final int UPR_PP = 35;
	
	MoveSorter(Move[] moves, Move[] movesTM, byte[] critAnims) 
	{
		this.moves = moves;
		this.movesTM = movesTM;
		this.critAnims = critAnims;
		
		sortMoves();
	}

	Move[] getAllLearnable()
	{
		return this.movesTM;
	}
	
	void sortMoves()
	{	
		for (int i = 0; i < moves.length; i++)
		{
			MoveEffect eff = getEffect(moves[i]);

			int basePower = byteToValue(moves[i].getBasePower());
			float accFloat = byteToValue(moves[i].getAcc());
			float acc = (accFloat / 255);

			int indexTier = 0;
			
			if (basePower > 1) // damaging move with constant base power
			{
				indexTier = calculateDamagingMoveTier(moves[i], eff, basePower, acc);
			}
			else if (basePower == 1) // counter damage & other attacks
			{
				indexTier = calculateVariableMoveTier(moves[i], eff, acc);		
			}
			else // status moves
			{
				indexTier = calculateStatusMoveTier(moves[i], eff, acc);
			}
			
			// min (max) are for under (above) the specified bounds

			moves[i].setTier(indexTier);
		}
	}

	private int calculateDamagingMoveTier(Move move, MoveEffect eff, int basePower, float acc)
	{
		int indexTier = 0;

		// list damaging from 0 BP to 100 in discrete tiers
		int span = (UPR_POWER - BOT_POWER)/N_MOVE_TIERS;

		int calcPower = basePower;

		if (eff != MoveEffect.NEVER_MISS) // taking accuracy into account
			calcPower = round(calcPower * acc);
			
		if (hasCritAnim(move)) // taking into account crit
			calcPower = round((1 + 2 * (1/4)) * calcPower);
		else if (!isFixed(move)) // other moves that can crit
			calcPower = round((1 + 2 * (1/16)) * calcPower);
		
		if (eff == MoveEffect.MULTI_HIT) // 2-5 multiple hits hits on average 3 times
			calcPower = 3 * calcPower;
		else if (eff == MoveEffect.TWO_HITS || eff == MoveEffect.TWO_HITS_PSN) // always hits twice
			calcPower = 2 * calcPower;
		else if (eff == MoveEffect.TRIPLE_HIT) // hits three times with higher BP
			calcPower = 6 * calcPower;
		else if (eff == MoveEffect.RAZOR_WIND || eff == MoveEffect.SKY_ATTACK || eff == MoveEffect.RECHARGE || eff == MoveEffect.SOLARBEAM) // two-turn attacks
			calcPower = calcPower/2;
		else if (eff == MoveEffect.DREAM_EATER) // dream eater
			calcPower = 1;
		
		indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
		
		if (eff == MoveEffect.PRIORITY) // increased priority moves get bumped up a tier
			indexTier = min(indexTier + 1, N_MOVE_TIERS - 1);
		
		move.setCalcPower(calcPower);

		return indexTier;
	}

	private int calculateVariableMoveTier(Move move, MoveEffect eff, float acc)
	{
		int indexTier = 0;

		// list damaging from 0 BP to 100 in discrete tiers
		int span = (UPR_POWER - BOT_POWER)/N_MOVE_TIERS;

		if (eff == MoveEffect.RETURN
		 || eff == MoveEffect.PRESENT
		 || eff == MoveEffect.FRUSTRATION)
			indexTier = MOVE_BOT_TIER;
		else if (eff == MoveEffect.COUNTER || eff == MoveEffect.LVL_DAMAGE || eff == MoveEffect.RND_DAMAGE || eff == MoveEffect.MIRROR_COAT) // countering/level-based
			indexTier = MOVE_MID_TIER;
		else if (eff == MoveEffect.HALVE_HP || eff == MoveEffect.REVERSAL) // halving/reversal
			indexTier = MOVE_2ND_TIER;
		else if (eff == MoveEffect.MAGNITUDE) // magnitude has average power of 71
		{
			int calcPower = round(71 * acc);
			indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
			move.setCalcPower(calcPower);
		}
		else if (eff == MoveEffect.HIDDEN_PWR) // hidden power has average power of 40
		{
			int calcPower = round(40 * acc);
			indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
			move.setCalcPower(calcPower);
		}

		return indexTier;
	}

	private int calculateStatusMoveTier(Move move, MoveEffect eff, float acc)
	{
		int indexTier = 0;

		// list status from 10 PP to 35 PP in discrete tiers
		int span = (UPR_PP - BOT_PP)/N_MOVE_TIERS;
		int pP = byteToValue(move.getPP()); // generally moves with lower PP are better
		indexTier = (N_MOVE_TIERS - 1) - (int) min(floor(max(pP - BOT_PP, 0) / span), N_MOVE_TIERS - 1);

		return indexTier;
	}
	
	byte[] getTierMoveset(byte[][] lvlMoves, byte[] eggMoves, boolean[] moveComp, Type[] monTypes, int[] baseStats, byte lvl)
	{
		ArrayList<Move> listMoves = new ArrayList<Move>();
		int maxPower; // limit damage output in TMs and egg moves
		int maxTier; // limit status tiers 
		
		if (byteToValue(lvl) <= 20)
		{
			maxPower = 60;
			maxTier = (int) floor((N_MOVE_TIERS - 1)/3);
		}
		else if (byteToValue(lvl) <= 30)
		{
			maxPower = 80;
			maxTier = (int) floor((N_MOVE_TIERS - 1)/2);
		}
		else
		{
			maxPower = 0xFF; // no max power
			maxTier = N_MOVE_TIERS - 1;
		}
		
		// get all the level up moves first
		for (int i = 0; i < lvlMoves.length; i++) // run through all the level moves
		{
			for (int j = 0; j < lvlMoves[i].length; j++) // run through all the level moves
				{
					if (listMoves.contains(moves[byteToValue(lvlMoves[i][j]) - 1])) // avoid repeats
						continue;
					
					listMoves.add(moves[byteToValue(lvlMoves[i][j]) - 1]);
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
			if (listMoves.contains(movesTM[i])) // avoid repeats
				continue;
				
			MoveEffect eff = getEffect(movesTM[i]);
			
			
			// if (eff == (byte) 0x21)
			// 	if ((byteToValue(baseStats[1]) >= 70 || byteToValue(baseStats[4]) >= 70) // it has good attacking stats
			// 		 || (byteToValue(baseStats[1]) + byteToValue(baseStats[4]) > byteToValue(baseStats[2]) + byteToValue(baseStats[5]))) // or is more offensive
			// 		continue; // skip Toxic
			
					
			if (eff == MoveEffect.THIEF)
				continue; // skip Thief
			
			if (eff == MoveEffect.HIDDEN_PWR)
				continue; // skip Hidden Power
			
			
			listMoves.add(movesTM[i]);
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
				
			if (listMoves.contains(moves[byteToValue(eggMoves[i]) - 1])) // avoid repeats
				continue;
			
			listMoves.add(moves[byteToValue(eggMoves[i]) - 1]);
		}
		
		ArrayList<Move> newMoves = new ArrayList<Move>();
		
		for (int i = 0; i < min(4, listMoves.size()); i ++) // first 4 are free
		{
			MoveEffect eff = getEffect(listMoves.get(i));

			if (listMoves.size() > 4) // if there's possibility to choose
			{	
				if (!eff.situational()) // filter out situational
					newMoves.add(listMoves.get(i));
			}
			else
				newMoves.add(listMoves.get(i));
		}
		
		for (int i = 4; i < listMoves.size(); i ++) // rest of moves are selected
		{
			MoveEffect eff = getEffect(listMoves.get(i));

			if (!eff.situational()) // filter out situational moves
			{
				if (newMoves.size() < 4)
					newMoves.add(listMoves.get(i));
				else
					compareMoves(listMoves.get(i), newMoves, monTypes, baseStats);
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
	
	int getPower(Move move, Type[] types, int atk, int satk)
	{
		int out;
		Type moveType = move.getType();
		byte moveCat = move.getCat();
		
		if (move.getCalcPower() <= 1) 
			out = move.getCalcPower(); // do nothing with non-damaging moves
		else
		{
			out = (moveType == types[0] || moveType == types[1] && (!isFixed(move))) ? (int) (move.getCalcPower() * 1.5) : move.getCalcPower(); // apply STAB except to Future Sight
			
			if (moveCat == 0b01000000) // physical/special move takes into account atk/satk
				out = out * atk;
			else
				out = out * satk;
		}
			
		return out;
	}
	
	int getPower(Move move, int dam, Type[] types, int atk, int satk)
	{
		// used to compare similar moves with appropriate boosts applied to dam
		int out;
		Type moveType = move.getType();
		byte moveCat = move.getCat();
		
		if (dam <= 1) 
			out = dam; // do nothing with non-damaging moves
		else
		{
			out = (moveType == types[0] || moveType == types[1] && (!isFixed(move))) ? (int) (dam * 1.5) : dam; // apply STAB except to fixed power
			
			if (moveCat == 0b01000000) // physical/special move takes into account atk/satk
				out = out * atk;
			else
				out = out * satk;
		}
			
		return out;
	}
	
	private void compareMoves(Move move, ArrayList<Move> moveSet, Type[] monTypes, int[] baseStats)
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
					
					if (replaced)
						break;
					
					if (thisSlotMove.getCalcPower() <= 1)
						continue;
					
					int slotMovePower;
					slotMovePower = getPower(thisSlotMove, monTypes, baseStats[1], baseStats[4]);
					
					if (movePower > slotMovePower // if stack is stronger
						|| ((isFixed(thisSlotMove)) && movePower * 2 > slotMovePower && move.getType() != Type.NORMAL)) // or if compared to Future Sight it can hit super-effective
					{
						if (!(repeatedType(move, moveSet, i)) // if there isn't another move of this type
							&& !(move.getType() == Type.NORMAL && movePower < 2 * slotMovePower && (!isFixed(thisSlotMove)))) // if move isn't normal-type replacing coverage moves
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
					else if ((movePower >= slotMovePower - getPower(move, MOVE_DAM_MARGIN, monTypes, baseStats[1], baseStats[4]))  // check if it's a similar move in power
						&& (nDam > 1 && move.getType() == thisSlotMove.getType()) // if it's of the same type
						&& (!isFixed(move)) // not future sight
						&& (!(repeatedType(move, moveSet, i))) // if there isn't another move of this type
						&& (movePower > 1)) // and is damaging
					{
						if (random() < 0.5) // 50% of replacing
						{
							//System.out.println(byteToValue(thisSlotMove.getIndex()) + " -> " + byteToValue(move.getIndex()));
							moveSet.set(i, move);
							break; // no need looking more
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

	public void printMoveTiers(Names names)
	{
		for (Move m : moves)
			System.out.printf("%-12s: tier = %1d, calcPower = %-3d\n", names.move(m.getIndex()), m.getTier(), m.getCalcPower());
	}
}