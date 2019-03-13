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

	public enum MoveEffect 
	{
	    NO_EFFECT 	 ((byte) 0x00),
	    CAUSE_SLEEP	 ((byte) 0x01),
	    DREAM_EATER  ((byte) 0x08),
	    ATKUP1       ((byte) 0x0A),
	    DEFUP1       ((byte) 0x0B),
	    SATKUP1      ((byte) 0x0C),
	    EVAUP1       ((byte) 0x10),
	    NEVER_MISS   ((byte) 0x11),
	    ATKDOWN1     ((byte) 0x12),
	    DEFDOWN1     ((byte) 0x13),
	    SPDDOWN1     ((byte) 0x14),
	    SATKDOWN1    ((byte) 0x15),
	    SDEFDOWN1    ((byte) 0x16),
	    ACCDOWN1     ((byte) 0x17),
	    EVADOWN1     ((byte) 0x18),
	    BIDE         ((byte) 0x1A),
	    MULTI_HIT    ((byte) 0x1D),
	    RECOVER_REST ((byte) 0x20),
	    CAUSE_TOXIC  ((byte) 0x21),
	    LIGHT_SCREEN ((byte) 0x23),
	    OHKO         ((byte) 0x26),
	    RAZOR_WIND   ((byte) 0x27),
	    HALVE_HP     ((byte) 0x28),
	    FIXED_DAMAGE ((byte) 0x29),
	    TWO_HITS     ((byte) 0x2C),
	    CAUSE_CNF    ((byte) 0x31),
	    ATKUP2       ((byte) 0x32),
	    DEFUP2       ((byte) 0x33),
	    SPDUP2       ((byte) 0x34),
	    SATKUP2      ((byte) 0x35),
	    SDEFUP2      ((byte) 0x36),
	    REFLECT      ((byte) 0x41),
	    CAUSE_PSN    ((byte) 0x42),
	    CAUSE_PRZ    ((byte) 0x43),
	    SKY_ATTACK   ((byte) 0x4B),
	    TWO_HITS_PSN ((byte) 0x4D),
	    RECHARGE     ((byte) 0x50),
	    MIMIC        ((byte) 0x52),
	    LVL_DAMAGE   ((byte) 0x57),
	    RND_DAMAGE   ((byte) 0x58),
	    COUNTER      ((byte) 0x59),
	    LOCK_ON      ((byte) 0x5E),
	    SLEEP_TALK   ((byte) 0x61),
	    REVERSAL     ((byte) 0x63),
	    DISABLE      ((byte) 0x64),
	    PRIORITY     ((byte) 0x67),
	    TRIPLE_HIT   ((byte) 0x68),
	    THIEF        ((byte) 0x69),
	    NIGHTMARE    ((byte) 0x6B),
	    CURSE        ((byte) 0x6D),
	    PROTECT      ((byte) 0x6F),
	    PERISH_SONG  ((byte) 0x72),
	    SANDSTORM    ((byte) 0x73),
	    ENDURE       ((byte) 0x74),
	    RETURN       ((byte) 0x79),
	    PRESENT      ((byte) 0x7A),
	    FRUSTRATION  ((byte) 0x7B),
	    MAGNITUDE    ((byte) 0x7E),
	    MORNING_SUN  ((byte) 0x84),
	    SYNTHESIS    ((byte) 0x85),
	    MOONLIGHT    ((byte) 0x86),
	    HIDDEN_PWR   ((byte) 0x87),
	    RAIN_DANCE   ((byte) 0x88),
	    SUNNY_DAY    ((byte) 0x89),
	    PSYCH_UP     ((byte) 0x8F),
	    MIRROR_COAT  ((byte) 0x90),
	    FUTURE_SIGHT ((byte) 0x94),
		SOLARBEAM    ((byte) 0x97),
        TELEPORT     ((byte) 0x99);

	    private final byte index;

	    MoveEffect(byte index) 
	    {
	        this.index = index;
	    }

	    public byte index() 
	    { 
	    	return index; 
	    }
	}

	MoveEffect getEffect(byte effByte)
	{
		switch (effByte)
		{
			case (byte) 0x01: return MoveEffect.CAUSE_SLEEP; 
			case (byte) 0x08: return MoveEffect.DREAM_EATER; 
			case (byte) 0x0A: return MoveEffect.ATKUP1; 
			case (byte) 0x0B: return MoveEffect.DEFUP1; 
			case (byte) 0x0C: return MoveEffect.SATKUP1; 
			case (byte) 0x10: return MoveEffect.EVAUP1; 
			case (byte) 0x11: return MoveEffect.NEVER_MISS; 
			case (byte) 0x12: return MoveEffect.ATKDOWN1; 
			case (byte) 0x13: return MoveEffect.DEFDOWN1; 
			case (byte) 0x14: return MoveEffect.SPDDOWN1; 
			case (byte) 0x15: return MoveEffect.SATKDOWN1; 
			case (byte) 0x16: return MoveEffect.SDEFDOWN1; 
			case (byte) 0x17: return MoveEffect.ACCDOWN1; 
			case (byte) 0x18: return MoveEffect.EVADOWN1; 
			case (byte) 0x1A: return MoveEffect.BIDE; 
			case (byte) 0x1D: return MoveEffect.MULTI_HIT; 
			case (byte) 0x20: return MoveEffect.RECOVER_REST; 
			case (byte) 0x21: return MoveEffect.CAUSE_TOXIC; 
			case (byte) 0x23: return MoveEffect.LIGHT_SCREEN; 
			case (byte) 0x26: return MoveEffect.OHKO; 
			case (byte) 0x27: return MoveEffect.RAZOR_WIND; 
			case (byte) 0x28: return MoveEffect.HALVE_HP; 
			case (byte) 0x29: return MoveEffect.FIXED_DAMAGE; 
			case (byte) 0x2C: return MoveEffect.TWO_HITS; 
			case (byte) 0x31: return MoveEffect.CAUSE_CNF; 
			case (byte) 0x32: return MoveEffect.ATKUP2; 
			case (byte) 0x33: return MoveEffect.DEFUP2; 
			case (byte) 0x34: return MoveEffect.SPDUP2; 
			case (byte) 0x35: return MoveEffect.SATKUP2; 
			case (byte) 0x36: return MoveEffect.SDEFUP2; 
			case (byte) 0x41: return MoveEffect.REFLECT; 
			case (byte) 0x42: return MoveEffect.CAUSE_PSN; 
			case (byte) 0x43: return MoveEffect.CAUSE_PRZ; 
			case (byte) 0x4B: return MoveEffect.SKY_ATTACK; 
			case (byte) 0x4D: return MoveEffect.TWO_HITS_PSN; 
			case (byte) 0x50: return MoveEffect.RECHARGE; 
			case (byte) 0x52: return MoveEffect.MIMIC; 
			case (byte) 0x57: return MoveEffect.LVL_DAMAGE; 
			case (byte) 0x58: return MoveEffect.RND_DAMAGE; 
			case (byte) 0x59: return MoveEffect.COUNTER; 
			case (byte) 0x5E: return MoveEffect.LOCK_ON; 
			case (byte) 0x61: return MoveEffect.SLEEP_TALK; 
			case (byte) 0x63: return MoveEffect.REVERSAL; 
			case (byte) 0x64: return MoveEffect.DISABLE; 
			case (byte) 0x67: return MoveEffect.PRIORITY; 
			case (byte) 0x68: return MoveEffect.TRIPLE_HIT; 
			case (byte) 0x69: return MoveEffect.THIEF; 
			case (byte) 0x6B: return MoveEffect.NIGHTMARE; 
			case (byte) 0x6D: return MoveEffect.CURSE; 
			case (byte) 0x6F: return MoveEffect.PROTECT; 
			case (byte) 0x72: return MoveEffect.PERISH_SONG; 
			case (byte) 0x73: return MoveEffect.SANDSTORM; 
			case (byte) 0x74: return MoveEffect.ENDURE; 
			case (byte) 0x79: return MoveEffect.RETURN; 
			case (byte) 0x7A: return MoveEffect.PRESENT; 
			case (byte) 0x7B: return MoveEffect.FRUSTRATION; 
			case (byte) 0x7E: return MoveEffect.MAGNITUDE; 
			case (byte) 0x84: return MoveEffect.MORNING_SUN; 
			case (byte) 0x85: return MoveEffect.SYNTHESIS; 
			case (byte) 0x86: return MoveEffect.MOONLIGHT; 
			case (byte) 0x87: return MoveEffect.HIDDEN_PWR; 
			case (byte) 0x88: return MoveEffect.RAIN_DANCE; 
			case (byte) 0x89: return MoveEffect.SUNNY_DAY; 
			case (byte) 0x8F: return MoveEffect.PSYCH_UP; 
			case (byte) 0x90: return MoveEffect.MIRROR_COAT; 
			case (byte) 0x94: return MoveEffect.FUTURE_SIGHT; 
			case (byte) 0x97: return MoveEffect.SOLARBEAM; 
			case (byte) 0x99: return MoveEffect.TELEPORT; 
			default: 		  return MoveEffect.NO_EFFECT;
		}
	}
	
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
			byte[] effByte = moves[i].getEffect();
			MoveEffect eff = getEffect(effByte[0]);

			int basePower = byteToValue(moves[i].getBasePower());
			int indexTier = 0;
			float accFloat = byteToValue(moves[i].getAcc());
			float acc = (accFloat / 255);
			
			if (basePower > 1) // damaging move with constant base power
			{
				int calcPower = basePower;

				if (eff != MoveEffect.NEVER_MISS) // taking accuracy into account
					calcPower = (int) round((float) (calcPower * acc));
					
				if (hasCritAnim(moves[i])) // taking into account crit
					calcPower = (int) round((1 + 2 * (1/4)) * calcPower);
				else if (!isFixed(moves[i])) // other moves that can crit
					calcPower = (int) round((1 + 2 * (1/16)) * calcPower);
				
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
					indexTier = (int) min(indexTier + 1, N_MOVE_TIERS - 1);
				
				moves[i].setCalcPower(calcPower);
			}
			else if (basePower == 1) // counter damage & other attacks
			{
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
					int calcPower = (int) round(71 * acc);
					indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
					moves[i].setCalcPower(calcPower);
				}
				else if (eff == MoveEffect.HIDDEN_PWR) // hidden power has average power of 40
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
				if (eff == MoveEffect.ATKUP2
				 || eff == MoveEffect.SPDUP2
				 || eff == MoveEffect.SATKUP2
				 || eff == MoveEffect.MORNING_SUN
				 || eff == MoveEffect.SYNTHESIS
				 || eff == MoveEffect.MOONLIGHT
				 || (eff == MoveEffect.RECOVER_REST && moves[i].getIndex() != (byte) 0x9C)) // recovery (not rest)
					indexTier = MOVE_TOP_TIER;
				else if (eff == MoveEffect.LIGHT_SCREEN 
					  || eff == MoveEffect.REFLECT)
					indexTier = MOVE_2ND_TIER;
				else if (eff == MoveEffect.CAUSE_SLEEP
					  || eff == MoveEffect.CAUSE_TOXIC 
					  || eff == MoveEffect.CAUSE_CNF 
					  || eff == MoveEffect.CAUSE_PSN 
					  || eff == MoveEffect.CAUSE_PRZ)
				{
					if (acc >= 0.75 ) // accurate status inducing moves
						indexTier = MOVE_TOP_TIER;
					else // inaccurate
						indexTier = MOVE_MID_TIER;
				}
				else if (eff == MoveEffect.MIMIC
					  || eff == MoveEffect.BIDE
					  || eff == MoveEffect.PERISH_SONG
					  || eff == MoveEffect.LOCK_ON
					  || eff == MoveEffect.PSYCH_UP
					  || eff == MoveEffect.OHKO
					  || eff == MoveEffect.NIGHTMARE
					  || eff == MoveEffect.DISABLE
					  || eff == MoveEffect.TELEPORT
					  || eff == MoveEffect.ATKDOWN1
					  || eff == MoveEffect.DEFDOWN1 
					  || eff == MoveEffect.SPDDOWN1 
					  || eff == MoveEffect.SATKDOWN1 
					  || eff == MoveEffect.SDEFDOWN1
					  || eff == MoveEffect.ACCDOWN1
					  || eff == MoveEffect.EVADOWN1
					  || eff == MoveEffect.SANDSTORM // treat weather effects separately 
					  || eff == MoveEffect.RAIN_DANCE 
					  || eff == MoveEffect.SUNNY_DAY 
					  || eff == MoveEffect.SLEEP_TALK)
					indexTier = MOVE_BOT_TIER;
				else if (eff == MoveEffect.ATKUP1 // 1 stage raising
					  || eff == MoveEffect.DEFUP1 
					  || eff == MoveEffect.SATKUP1
					  || eff == MoveEffect.EVAUP1
					  || (eff == MoveEffect.RECOVER_REST && moves[i].getIndex() == (byte) 0x9C) // rest
					  || eff == MoveEffect.CURSE
					  || eff == MoveEffect.PROTECT 
					  || eff == MoveEffect.ENDURE)
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
			maxTier = (int) N_MOVE_TIERS - 1;
		}
		
		// get all the level up moves first
		for (int i = 0; i < lvlMoves.length; i++) // run through all the level moves
		{
			if (listMoves.contains(moves[byteToValue(lvlMoves[i]) - 1])) // avoid repeats
				continue;
			
			listMoves.add(moves[byteToValue(lvlMoves[i]) - 1]);
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
				
			byte[] effByte = movesTM[i].getEffect();
			MoveEffect eff = getEffect(effByte[0]);
			
			/*
			if (eff == (byte) 0x21)
				if ((byteToValue(baseStats[1]) >= 70 || byteToValue(baseStats[4]) >= 70) // it has good attacking stats
					 || (byteToValue(baseStats[1]) + byteToValue(baseStats[4]) > byteToValue(baseStats[2]) + byteToValue(baseStats[5]))) // or is more offensive
					continue; // skip Toxic
			*/
					
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
			newMoves.add(listMoves.get(i));
		
		for (int i = 4; i < listMoves.size(); i ++) // rest of moves are selected
		{
			compareMoves(listMoves.get(i), newMoves, monTypes, baseStats);
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
		
		if (move.getCalcPower() <= 1) 
			out = move.getCalcPower(); // do nothing with non-damaging moves
		else
		{
			out = (moveType == types[0] || moveType == types[1] && (!isFixed(move))) ? (int) (move.getCalcPower() * 1.5) : move.getCalcPower(); // apply STAB except to Future Sight
			
			if (moveCat == 0b01000000) // physical/special move takes into account atk/satk
				out = out * byteToValue(atk);
			else
				out = out * byteToValue(satk);
		}
			
		return out;
	}
	
	private int getPower(Move move, int dam, byte[] types, byte atk, byte satk)
	{
		// used to compare similar moves with appropriate boosts applied to dam
		int out;
		byte moveType = move.getType();
		byte moveCat = move.getCat();
		
		if (dam <= 1) 
			out = dam; // do nothing with non-damaging moves
		else
		{
			out = (moveType == types[0] || moveType == types[1] && (!isFixed(move))) ? (int) (dam * 1.5) : dam; // apply STAB except to fixed power
			
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
					
					if (replaced)
						break;
					
					if (thisSlotMove.getCalcPower() <= 1)
						continue;
					
					int slotMovePower;
					slotMovePower = getPower(thisSlotMove, monTypes, baseStats[1], baseStats[4]);
					
					if (movePower > slotMovePower // if stack is stronger
						|| ((isFixed(thisSlotMove)) && movePower * 2 > slotMovePower && move.getType() != (byte) 0x00)) // or if compared to Future Sight it can hit super-effective
					{
						if (!(repeatedType(move, moveSet, i)) // if there isn't another move of this type
							&& !(move.getType() == (byte) 0x00 && movePower < 2 * slotMovePower && (!isFixed(thisSlotMove)))) // if move isn't normal-type replacing coverage moves
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

	private boolean isFixed(Move move)
	{
		byte[] effByte = move.getEffect();
		MoveEffect eff = getEffect(effByte[0]);

		// return true if it is a fixed power move
		return (eff == MoveEffect.FIXED_DAMAGE || eff == MoveEffect.FUTURE_SIGHT);
	}
}