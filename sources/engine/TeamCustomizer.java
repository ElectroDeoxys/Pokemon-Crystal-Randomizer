package engine;

import java.util.ArrayList;
import java.util.Comparator;
import static java.lang.Math.*;

import static data.Constants.*;
import data.PokemonGame;
import data.Move;

class TeamCustomizer extends MoveAnalyser
{
	private enum Role
	{
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

	MoveSorter moveSorter;

	TeamCustomizer(Move[] moves, MoveSorter moveSorter)
	{
		this.moves = moves;
		this.moveSorter = moveSorter;
	}

	ArrayList<Move> customize(PokemonGame[] team, int[] lvls, PokemonGame[] mons, Names names)
	{
		// assign roles
		ArrayList<ArrayList<Role>> roles = new ArrayList<ArrayList<Role>>();

		for (int i = 0; i < team.length; i++)
		{
			PokemonGame mon = team[i];
			int lvl = lvls[i];

			ArrayList<Move> movepool = getMovepool(mon, lvl, mons, moves, moveSorter.getAllLearnable());
			ArrayList<Move> movesPhy = getPhysical(movepool);
			ArrayList<Move> movesSpe = getSpecial(movepool);
			ArrayList<Move> movesSta = getStatus(movepool);

			movesPhy.sort(MovePowerCmp);
			movesSpe.sort(MovePowerCmp);

			ArrayList<Role> curRoles = new ArrayList<Role>();

			int maxatk = max(mon.getAtk(), mon.getSAtk());
			int minatk = min(mon.getAtk(), mon.getSAtk());
	 		int maxdef = max(mon.getDef(), mon.getSDef());
			int mindef = min(mon.getDef(), mon.getSDef());

			if (isPhysicalOffense(mon, movesPhy, levelTier(lvl)))
				curRoles.add(Role.PHYOFF);
			if (isSpecialOffense(mon, movesSpe, levelTier(lvl)))
				curRoles.add(Role.SPEOFF);
			if (isMixed(mon, curRoles))
				curRoles.add(Role.MIXED);
			if (isPhysicalSweeper(mon, movesPhy, levelTier(lvl)))
				curRoles.add(Role.PHYSWEEPER);
			if (isSpecialSweeper(mon, movesSpe, levelTier(lvl)))
				curRoles.add(Role.SPESWEEPER);
			if (isPhysicalTank(mon, movesPhy, levelTier(lvl)))
				curRoles.add(Role.PHYTANK);
			if (isSpecialTank(mon, movesSpe, levelTier(lvl)))
				curRoles.add(Role.SPETANK);
			if (isSupport(mon, movesSta))
				curRoles.add(Role.SUPPORT);
			if (isStaller(mon, movesSta))
				curRoles.add(Role.STALLER);
			if (isSleepTalker(mon, movesSta, curRoles))
				curRoles.add(Role.SLEEP_TALKER);
			if (isDreamEater(mon, movepool, curRoles))
				curRoles.add(Role.DREAM_EATER);
			if (isCurser(mon, movesSta, curRoles))
				curRoles.add(Role.CURSER);
			if (isFlailer(mon, movepool, curRoles))
				curRoles.add(Role.FLAILER);

			roles.add(curRoles);

			if (true)
			{
				System.out.println(names.pokemon(mon.getIndex()) + " lvl" + lvl );
				System.out.printf("Roles: ");
				for (Role r : curRoles)
					System.out.printf(r + " ");
				System.out.printf("\n\n");
			}
		}

		return null;
	}

	ArrayList<Move> getMovepool(PokemonGame mon, int lvl, PokemonGame[] mons, Move[] moves, Move[] movesTM)
	{
		byte[][] lvlMoves = mon.getMovesUpToLevel(mons, valueToByte(lvl));
		ArrayList<Move> movepool = new ArrayList<Move>();

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
			Move thisMove = moves[index];

			if (thisMove.getTier() > maxTier)
				continue;

			if (!movepool.contains(thisMove))
				movepool.add(thisMove);
		}

		return movepool;
	}

	private ArrayList<Move> getPhysical(ArrayList<Move> movepool)
	{
		ArrayList<Move> movesPhy = new ArrayList<Move>();

		for (Move m : movepool)
			if (m.getCat() == MOVE_PHYSICAL_CATEGORY && m.getCalcPower() > 0)
				movesPhy.add(m);

		return movesPhy;
	}

	private ArrayList<Move> getSpecial(ArrayList<Move> movepool)
	{
		ArrayList<Move> movesSpe = new ArrayList<Move>();

		for (Move m : movepool)
			if (m.getCat() == MOVE_SPECIAL_CATEGORY && m.getCalcPower() > 0)
				movesSpe.add(m);
		
		return movesSpe;
	}

	ArrayList<Move> getStatus(ArrayList<Move> movepool)
	{
		ArrayList<Move> movesSta = new ArrayList<Move>();

		for (Move m : movepool)
			if (m.getCat() == MOVE_OTHER_CATEGORY || m.getCalcPower() == 0)
				movesSta.add(m);
		
		return movesSta;
	}

	private boolean isPhysicalOffense(PokemonGame mon, ArrayList<Move> movesPhy, int maxTier)
	{
		Move mainMovePhy = getMainMove(mon, movesPhy);

		return 
		(
			(!movesPhy.isEmpty()) &&						// has physical moves
			(mon.getAtk() >= 0.15 * mon.getBST()) &&		// atk stat is good
			(mainMovePhy.getTier() >= maxTier - 1) 			// good main physical move
		);
	}

	private boolean isSpecialOffense(PokemonGame mon, ArrayList<Move> movesSpe, int maxTier)
	{
		Move mainMoveSpe = getMainMove(mon, movesSpe);

		return
		(
			(!movesSpe.isEmpty()) &&						// has special moves
			(mon.getSAtk() >= 0.15 * mon.getBST()) &&		// satk stat is good
			(mainMoveSpe.getTier() >= maxTier - 1) 			// good main special move
		);
	}

	private boolean isMixed(PokemonGame mon, ArrayList<Role> roles)
	{
		return
		(
			(roles.contains(Role.PHYOFF)) &&
			(roles.contains(Role.SPEOFF)) &&
			(abs(mon.getAtk() - mon.getSAtk()) < 0.05 * mon.getBST()) // no appreciable difference between stats
		);
	}

	private boolean isPhysicalSweeper(PokemonGame mon, ArrayList<Move> movesPhy, int maxTier)
	{
		Move mainMovePhy = getMainMove(mon, movesPhy);

		return 
		(
			(!movesPhy.isEmpty()) &&						// has physical moves
			(mon.getAtk() >= 0.2 * mon.getBST()) &&			// atk stat is good
			(mon.getSpd() >= 0.15 * mon.getBST()) &&		// spd stat is good
			(mainMovePhy.getTier() >= maxTier - 1) 			// good main physical move
		);
	}

	private boolean isSpecialSweeper(PokemonGame mon, ArrayList<Move> movesSpe, int maxTier)
	{
		Move mainMoveSpe = getMainMove(mon, movesSpe);

		return
		(
			(!movesSpe.isEmpty()) &&						// has special moves
			(mon.getSAtk() >= 0.2 * mon.getBST()) &&		// satk stat is good
			(mon.getSpd() >= 0.15 * mon.getBST()) &&		// satk stat is good
			(mainMoveSpe.getTier() >= maxTier - 1) 			// good main special move
		);
	}

	private boolean isPhysicalTank(PokemonGame mon, ArrayList<Move> movesPhy, int maxTier)
	{
		Move mainMovePhy = getMainMove(mon, movesPhy);

		return 
		(
			(!movesPhy.isEmpty()) &&					// has physical moves
			((mon.getDef() >= 0.2 * mon.getBST()) ||
			 (mon.getSDef() >= 0.2 * mon.getBST())) &&	// good (sp) defense
			(mon.getAtk() >= 0.1 * mon.getBST()) &&		// atk stat is useable
			(mainMovePhy.getTier() >= maxTier - 1)		// top main physical move
		);
	}

	private boolean isSpecialTank(PokemonGame mon, ArrayList<Move> movesSpe, int maxTier)
	{
		Move mainMoveSpe = getMainMove(mon, movesSpe);

		return 
		(
			(!movesSpe.isEmpty()) &&					// has specia moves
			((mon.getDef() >= 0.2 * mon.getBST()) ||
			 (mon.getSDef() >= 0.2 * mon.getBST())) &&	// good (sp) defense
			(mon.getSAtk() >= 0.1 * mon.getBST()) &&	// satk stat is useable
			(mainMoveSpe.getTier() >= maxTier - 1) 		// top main special move
		);
	}

	private boolean isSupport(PokemonGame mon, ArrayList<Move> movesSta)
	{
		return 
		(
			(numSupportMoves(movesSta) >= 2) &&			// has support moves
			((mon.getDef() > 0.15 * mon.getBST()) ||
			 (mon.getSDef() > 0.15 * mon.getBST())) &&	// good (sp) defense
			(mon.getHP() > 0.15 * mon.getBST())			// hp stat is good
		);
	}

	private boolean isStaller(PokemonGame mon, ArrayList<Move> movesSta)
	{
		return 
		(
			(hasMoveEffect(movesSta, MoveEffect.CAUSE_TOXIC)) &&
			(numStallMoves(movesSta) >= 2) &&			// has stall moves
			((mon.getDef() >= 0.175 * mon.getBST()) ||
			 (mon.getSDef() >= 0.175 * mon.getBST())) &&	// good (sp) defense
			(mon.getHP() >= 0.15 * mon.getBST())		// hp stat is good
		);
	}

	private boolean isSleepTalker(PokemonGame mon, ArrayList<Move> movesSta, ArrayList<Role> roles)
	{
		return 
		(
			(hasRest(movesSta)) &&
			(!hasHealing(movesSta)) &&
			(hasMoveEffect(movesSta, MoveEffect.SLEEP_TALK)) &&
			(roles.contains(Role.PHYSWEEPER) || roles.contains(Role.SPESWEEPER) ||
			 (roles.contains(Role.PHYTANK) || roles.contains(Role.SPETANK))) &&
			((mon.getDef() >= 0.15 * mon.getBST()) ||
			 (mon.getSDef() >= 0.15 * mon.getBST())) &&	// good (sp) defense
			(mon.getHP() >= 0.15 * mon.getBST())		// hp stat is good
		);
	}

	private boolean isDreamEater(PokemonGame mon, ArrayList<Move> movepool, ArrayList<Role> roles)
	{
		return 
		(
			(hasMoveEffect(movepool, MoveEffect.DREAM_EATER)) &&
			(hasMoveEffect(movepool, MoveEffect.CAUSE_SLP)) &&
			(roles.contains(Role.SPEOFF) || roles.contains(Role.SPETANK))
		);
	}

	private boolean isCurser(PokemonGame mon, ArrayList<Move> movesSta, ArrayList<Role> roles)
	{
		return 
		(
			(hasMoveEffect(movesSta, MoveEffect.CURSE)) &&
			(!isGhost(mon)) &&
			(roles.contains(Role.PHYOFF) || roles.contains(Role.PHYTANK)) &&
			(mon.getSpd() < 0.1 * mon.getBST())
		);
	}

	private boolean isFlailer(PokemonGame mon, ArrayList<Move> movepool, ArrayList<Role> roles)
	{
		return 
		(
			(hasMoveEffect(movepool, MoveEffect.REVERSAL)) &&
			(hasMoveEffect(movepool, MoveEffect.ENDURE)) &&
			(roles.contains(Role.PHYOFF))
		);
	}

	boolean checkSTAB(PokemonGame mon, Move move)
	{
		Type[] monTypes = mon.getTypes();
		Type moveType = move.getType();

		return  (monTypes[0] == moveType) || (monTypes[1] == moveType);
	}

	private Move getMainMove(PokemonGame mon, ArrayList<Move> movepool)
	{
		// returns the main damaging move
		Move mainMove = null;
		int mainMovePower = 0;

		for (Move m : movepool)
		{
			if (m.getCalcPower() == 0)
				continue;

			MoveEffect eff = getEffect(m);

			if (eff == MoveEffect.EXPLODE)
				continue;
			if (eff == MoveEffect.RECHARGE)
				continue;

			if (mainMove == null)
			{
				mainMove = m;
				mainMovePower = moveSorter.getPower(mainMove, mon.getTypes(), mon.getAtk(), mon.getSAtk());
			}
			else
			{
				int curMovePower = moveSorter.getPower(m, mon.getTypes(), mon.getAtk(), mon.getSAtk());
				mainMove = (curMovePower > mainMovePower) ? m : mainMove;
				mainMovePower = (curMovePower > mainMovePower) ? curMovePower : mainMovePower;
			}
		}

		return mainMove;
	}

	private int numSupportMoves(ArrayList<Move> movepool)
	{
		// count number of support moves
		int count = 0;

		for (Move m : movepool)
		{
			if (m.getCat() != MOVE_OTHER_CATEGORY)
				continue;

			MoveEffect eff = getEffect(m);

			if 
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
			)
				count++;
		}

		return count;
	}

	private int numStallMoves(ArrayList<Move> movepool)
	{
		// count number of stall moves
		int count = 0;

		for (Move m : movepool)
		{
			if (m.getCat() != MOVE_OTHER_CATEGORY)
				continue;

			MoveEffect eff = getEffect(m);

			if 
			(
				(eff == MoveEffect.CAUSE_CNF)    ||
				(eff == MoveEffect.RECOVER_REST && m.getIndex() != (byte) 0x9C) ||
				(eff == MoveEffect.PROTECT)		 ||
				(eff == MoveEffect.MORNING_SUN)	 ||
				(eff == MoveEffect.SYNTHESIS)	 ||
				(eff == MoveEffect.MOONLIGHT)	 ||
				(eff == MoveEffect.FLY_DIG)
			)
				count++;
		}

		return count;
	}

	private boolean hasHealing(ArrayList<Move> movepool)
	{
		boolean test = false;

		if (hasRecover(movepool))
			test = true;
		else if (hasMoveEffect(movepool, MoveEffect.MORNING_SUN))
			test = true;
		else if (hasMoveEffect(movepool, MoveEffect.SYNTHESIS))
			test = true;
		else if (hasMoveEffect(movepool, MoveEffect.MOONLIGHT))
			test = true;

		return test;
	}

	private boolean hasRest(ArrayList<Move> movepool)
	{
		boolean test = false;

		for (Move m : movepool)
		{
			if (m.getCat() != MOVE_OTHER_CATEGORY)
				continue;

			MoveEffect curEff = getEffect(m);

			if (curEff == MoveEffect.RECOVER_REST && m.getIndex() == (byte) 0x9C)
			{
				test = true;
				break;
			}
		}

		return test;
	}

	private boolean hasRecover(ArrayList<Move> movepool)
	{
		boolean test = false;

		for (Move m : movepool)
		{
			if (m.getCat() != MOVE_OTHER_CATEGORY)
				continue;

			MoveEffect curEff = getEffect(m);

			if (curEff == MoveEffect.RECOVER_REST && m.getIndex() != (byte) 0x9C)
			{
				test = true;
				break;
			}
		}

		return test;
	}

	private boolean hasMoveEffect(ArrayList<Move> movepool, MoveEffect eff)
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

	private boolean isGhost(PokemonGame mon)
	{
		Type[] types = mon.getTypes();
		return (types[0] == Type.GHOST || types[1] == Type.GHOST);
	}

	private int levelTier(int lvl)
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
			maxTier = (int) N_MOVE_TIERS - 1;
		}

		return maxTier;
	}
}