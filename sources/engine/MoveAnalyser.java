package engine;

import java.util.ArrayList;
import java.util.Comparator;
import static java.lang.Math.*;
import static data.Constants.*;

import data.Move;
import data.PokemonGame;

class MoveAnalyser
{
	protected Move[] moves;

	protected enum Role
	{
		DEFAULT,
		PHYOFF,
		SPEOFF,
		MIXED,
		PHYSWEEPER,
		SPESWEEPER,
		PHYTANK,
		SPETANK,
		SUPPORT,
		STALLER,
		SLEEP_TALKER,
		DREAM_EATER,
		CURSER,
		FLAILER;
	}

	protected enum MoveEffect 
	{
	    NO_EFFECT 	 ((byte) 0x00, false),
	    CAUSE_SLP	 ((byte) 0x01, false),
	    EXPLODE      ((byte) 0x07, true),
	    DREAM_EATER  ((byte) 0x08, true),
	    ATKUP1       ((byte) 0x0A, true),
	    DEFUP1       ((byte) 0x0B, false),
	    SPDUP1       ((byte) 0x0C, false),
	    SATKUP1      ((byte) 0x0D, true),
	    EVAUP1       ((byte) 0x10, true),
	    NEVER_MISS   ((byte) 0x11, false),
	    ATKDOWN1     ((byte) 0x12, false),
	    DEFDOWN1     ((byte) 0x13, false),
	    SPDDOWN1     ((byte) 0x14, false),
	    SATKDOWN1    ((byte) 0x15, false),
	    SDEFDOWN1    ((byte) 0x16, false),
	    ACCDOWN1     ((byte) 0x17, false),
	    EVADOWN1     ((byte) 0x18, false),
	    BIDE         ((byte) 0x1A, false),
	    MULTI_HIT    ((byte) 0x1D, false),
	    RECOVER_REST ((byte) 0x20, true),
	    CAUSE_TOXIC  ((byte) 0x21, true),
	    LIGHT_SCREEN ((byte) 0x23, false),
	    OHKO         ((byte) 0x26, false),
	    RAZOR_WIND   ((byte) 0x27, false),
	    HALVE_HP     ((byte) 0x28, false),
	    FIXED_DAMAGE ((byte) 0x29, false),
	    TWO_HITS     ((byte) 0x2C, false),
	    CAUSE_CNF    ((byte) 0x31, false),
	    ATKUP2       ((byte) 0x32, true),
	    DEFUP2       ((byte) 0x33, false),
	    SPDUP2       ((byte) 0x34, true),
	    SATKUP2      ((byte) 0x35, true),
	    SDEFUP2      ((byte) 0x36, false),
	    REFLECT      ((byte) 0x41, false),
	    CAUSE_PSN    ((byte) 0x42, false),
	    CAUSE_PRZ    ((byte) 0x43, false),
	    SKY_ATTACK   ((byte) 0x4B, false),
	    TWO_HITS_PSN ((byte) 0x4D, false),
	    RECHARGE     ((byte) 0x50, false),
	    MIMIC        ((byte) 0x52, false),
	    LEECH_SEED   ((byte) 0x54, false),
	    LVL_DAMAGE   ((byte) 0x57, false),
	    RND_DAMAGE   ((byte) 0x58, false),
	    COUNTER      ((byte) 0x59, false),
	    ENCORE       ((byte) 0x5A, false),
	    SNORE        ((byte) 0x5C, true),
	    LOCK_ON      ((byte) 0x5E, true),
	    SLEEP_TALK   ((byte) 0x61, true),
	    REVERSAL     ((byte) 0x63, true),
	    DISABLE      ((byte) 0x64, false),
	    HEAL_BELL    ((byte) 0x66, false),
	    PRIORITY     ((byte) 0x67, false),
	    TRIPLE_HIT   ((byte) 0x68, false),
	    THIEF        ((byte) 0x69, true),
	    NIGHTMARE    ((byte) 0x6B, true),
	    CURSE        ((byte) 0x6D, true),
	    PROTECT      ((byte) 0x6F, true),
	    SPIKES	     ((byte) 0x70, true),
	    PERISH_SONG  ((byte) 0x72, true),
	    SANDSTORM    ((byte) 0x73, true),
	    ENDURE       ((byte) 0x74, true),
	    SWAGGER      ((byte) 0x74, true),
	    ATTRACT      ((byte) 0x78, true),
	    RETURN       ((byte) 0x79, true),
	    PRESENT      ((byte) 0x7A, false),
	    FRUSTRATION  ((byte) 0x7B, true),
	    MAGNITUDE    ((byte) 0x7E, false),
	    MORNING_SUN  ((byte) 0x84, false),
	    SYNTHESIS    ((byte) 0x85, false),
	    MOONLIGHT    ((byte) 0x86, false),
	    HIDDEN_PWR   ((byte) 0x87, true),
	    RAIN_DANCE   ((byte) 0x88, true),
	    SUNNY_DAY    ((byte) 0x89, true),
	    BELLY_DRUM   ((byte) 0x8E, true),
	    PSYCH_UP     ((byte) 0x8F, true),
	    MIRROR_COAT  ((byte) 0x90, false),
	    FUTURE_SIGHT ((byte) 0x94, true),
		SOLARBEAM    ((byte) 0x97, true),
        TELEPORT     ((byte) 0x99, true),
        FLY_DIG      ((byte) 0x9B, true);

	    private final byte index;
	    private final boolean situational;

	    MoveEffect(byte index, boolean situational) 
	    {
	        this.index = index;
	        this.situational = situational;
	    }

	    public byte index() 
	    { 
	    	return index; 
	    }

	    public boolean situational() 
	    { 
	    	return situational; 
	    }
	}

	MoveEffect getEffect(Move move)
	{
		byte[] effBytes = move.getEffect();
		return getEffect(effBytes[0]);
	}

	MoveEffect getEffect(byte effByte)
	{
		switch (effByte)
		{
			case (byte) 0x01: return MoveEffect.CAUSE_SLP; 
			case (byte) 0x07: return MoveEffect.EXPLODE; 
			case (byte) 0x08: return MoveEffect.DREAM_EATER; 
			case (byte) 0x0A: return MoveEffect.ATKUP1; 
			case (byte) 0x0B: return MoveEffect.DEFUP1; 
			case (byte) 0x0C: return MoveEffect.SPDUP1; 
			case (byte) 0x0D: return MoveEffect.SATKUP1; 
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
			case (byte) 0x54: return MoveEffect.LEECH_SEED; 
			case (byte) 0x57: return MoveEffect.LVL_DAMAGE; 
			case (byte) 0x58: return MoveEffect.RND_DAMAGE; 
			case (byte) 0x59: return MoveEffect.COUNTER; 
			case (byte) 0x5A: return MoveEffect.ENCORE; 
			case (byte) 0x5C: return MoveEffect.SNORE; 
			case (byte) 0x5E: return MoveEffect.LOCK_ON; 
			case (byte) 0x61: return MoveEffect.SLEEP_TALK; 
			case (byte) 0x63: return MoveEffect.REVERSAL; 
			case (byte) 0x64: return MoveEffect.DISABLE; 
			case (byte) 0x66: return MoveEffect.HEAL_BELL; 
			case (byte) 0x67: return MoveEffect.PRIORITY; 
			case (byte) 0x68: return MoveEffect.TRIPLE_HIT; 
			case (byte) 0x69: return MoveEffect.THIEF; 
			case (byte) 0x6B: return MoveEffect.NIGHTMARE; 
			case (byte) 0x6D: return MoveEffect.CURSE; 
			case (byte) 0x6F: return MoveEffect.PROTECT; 
			case (byte) 0x70: return MoveEffect.SPIKES; 
			case (byte) 0x72: return MoveEffect.PERISH_SONG; 
			case (byte) 0x73: return MoveEffect.SANDSTORM; 
			case (byte) 0x74: return MoveEffect.ENDURE; 
			case (byte) 0x76: return MoveEffect.SWAGGER; 
			case (byte) 0x78: return MoveEffect.ATTRACT; 
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
			case (byte) 0x8E: return MoveEffect.BELLY_DRUM; 
			case (byte) 0x8F: return MoveEffect.PSYCH_UP; 
			case (byte) 0x90: return MoveEffect.MIRROR_COAT; 
			case (byte) 0x94: return MoveEffect.FUTURE_SIGHT; 
			case (byte) 0x97: return MoveEffect.SOLARBEAM; 
			case (byte) 0x99: return MoveEffect.TELEPORT; 
			case (byte) 0x9B: return MoveEffect.FLY_DIG; 
			default: 		  return MoveEffect.NO_EFFECT;
		}
	}

	public static Comparator<Move> MovePowerCmp = new Comparator<Move>() 
	{
		public int compare(Move m1, Move m2) 
		{

		int movePower1 = m1.getCalcPower();
		int movePower2 = m2.getCalcPower();

		/*For descending order*/
		return movePower2-movePower1;
		}
	};

	public static Comparator<Move> MoveTierCmp = new Comparator<Move>() 
	{
		public int compare(Move m1, Move m2) 
		{

		int moveTier1 = m1.getTier();
		int moveTier2 = m2.getTier();

		/*For descending order*/
		return moveTier2-moveTier1;
		}
	};

	ArrayList<Move> getLevelUpMoves(PokemonGame mon, int lvl, PokemonGame[] mons)
	{
		ArrayList<Move> movepool = new ArrayList<Move>();
		byte[][] lvlMoves = mon.getMovesUpToLevel(mons, valueToByte(lvl));

		for (int i = 0; i < lvlMoves.length; i++)
		{			
			for (byte m : lvlMoves[i])
			{
				int index = byteToValue(m);
				Move thisMove = moves[index- 1];

				if (!movepool.contains(thisMove))
					movepool.add(thisMove);
			}
		}

		return movepool;
	}

	ArrayList<Move> getMovepool(PokemonGame mon, int lvl, PokemonGame[] mons, Move[] movesTM)
	{
		ArrayList<Move> movepool = getLevelUpMoves(mon, lvl, mons);

		int maxTier = levelTier(lvl);

		boolean[] moveComp = mon.getCompatibilities();

		for (int i = 0; i < movesTM.length; i++) // run through all the TM moves
		{
			if (!moveComp[i]) // if not compatible, skip
				continue;

			if (movesTM[i].getTier() > maxTier)
				continue;
				
			if (!movepool.contains(movesTM[i]))
				movepool.add(movesTM[i]);
		}


		byte[] eggMoves = mon.getEggMovesCarry(); // egg moves it can carry

		for (byte m : eggMoves)
		{			
			int index = byteToValue(m);
			Move thisMove = moves[index-1];

			if (thisMove.getTier() > maxTier)
				continue;

			if (!movepool.contains(thisMove))
				movepool.add(thisMove);
		}


		return movepool;
	}

	protected ArrayList<Move> getPhysical(ArrayList<Move> movepool)
	{
		ArrayList<Move> movesPhy = new ArrayList<Move>();

		for (Move m : movepool)
			if (m.getCat() == MOVE_PHYSICAL_CATEGORY && m.getCalcPower() > 0)
				movesPhy.add(m);

		return movesPhy;
	}

	protected ArrayList<Move> getSpecial(ArrayList<Move> movepool)
	{
		ArrayList<Move> movesSpe = new ArrayList<Move>();

		for (Move m : movepool)
			if (m.getCat() == MOVE_SPECIAL_CATEGORY && m.getCalcPower() > 0)
				movesSpe.add(m);
		
		return movesSpe;
	}

	protected ArrayList<Move> getStatus(ArrayList<Move> movepool)
	{
		ArrayList<Move> movesSta = new ArrayList<Move>();

		for (Move m : movepool)
			if (m.getCat() == MOVE_OTHER_CATEGORY || m.getCalcPower() == 0)
				movesSta.add(m);
		
		return movesSta;
	}

	protected boolean hasMoveEffect(ArrayList<Move> movepool, MoveEffect eff)
	{
		boolean test = false;

		for (Move m : movepool)
		{
			MoveEffect curEff = getEffect(m);

			if (curEff == eff)
			{
				test = true;
				break;
			}
		}

		return test;
	}

	boolean checkSTAB(PokemonGame mon, Move move)
	{
		Type[] monTypes = mon.getTypes();
		Type moveType = move.getType();

		return checkSTAB(monTypes, moveType);
	}

	boolean checkSTAB(Type[] monTypes, Type moveType)
	{
		return (monTypes[0] == moveType) || (monTypes[1] == moveType);
	}

	boolean checkSTAB(Type monType, Move move)
	{
		return (monType == move.getType());
	}

	boolean hasNonSituational(ArrayList<Move> movepool)
	{
		// check if a set of moves have at least one non situational move
		boolean test = false;
		for (Move m : movepool)
			if (!getEffect(m).situational())
			{
				test = true;
				break;
			}
		return test;
	}

	static int levelTier(int lvl)
	{
		int maxTier = 0;

		if (lvl <= 20)
		{
			maxTier = (int) floor((N_MOVE_TIERS - 1)/3);
		}
		else if (lvl <= 30)
		{
			maxTier = (int) floor((N_MOVE_TIERS - 1)/2);
		}
		else
		{
			maxTier = N_MOVE_TIERS - 1;
		}

		return maxTier;
	}

	protected boolean isSupportMove(Move move)
	{
		MoveEffect eff = getEffect(move);

		return
		(
			(eff == MoveEffect.CAUSE_SLP)    ||
			(eff == MoveEffect.LIGHT_SCREEN) ||
			(eff == MoveEffect.CAUSE_CNF)    ||
			(eff == MoveEffect.REFLECT)      ||
			(eff == MoveEffect.CAUSE_PRZ)    ||
			(eff == MoveEffect.LEECH_SEED)   ||
			(eff == MoveEffect.ENCORE)		 ||
			(eff == MoveEffect.HEAL_BELL)	 ||
			(eff == MoveEffect.SPIKES)
		);
	}

	protected boolean isFixed(Move move)
	{
		MoveEffect eff = getEffect(move);

		// return true if it is a fixed power move
		return (eff == MoveEffect.FIXED_DAMAGE || eff == MoveEffect.FUTURE_SIGHT);
	}
}