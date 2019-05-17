package engine;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;
import data.PokemonGame;
import data.Move;

class PokemonClassifier extends MoveAnalyser
{
    MoveSorter moveSorter;

    PokemonClassifier(Move[] moves, MoveSorter moveSorter)
    {
        this.moves = moves;
        this.moveSorter = moveSorter;
    }

    ArrayList<ArrayList<Role>> classify(PokemonGame[] team, int[] lvls, PokemonGame[] mons)
    {
        // assign roles
        ArrayList<ArrayList<Role>> roles = new ArrayList<>();

        for (int i = 0; i < team.length; i++)
        {
            PokemonGame mon = team[i];
            int lvl = lvls[i];

            ArrayList<Move> movepool = getMovepool(mon, lvl, mons, moveSorter.getAllLearnable());
            ArrayList<Move> movesPhy = getPhysical(movepool);
            ArrayList<Move> movesSpe = getSpecial(movepool);
            ArrayList<Move> movesSta = getStatus(movepool);

            movesPhy.sort(MovePowerCmp);
            movesSpe.sort(MovePowerCmp);

            ArrayList<Role> curRoles = new ArrayList<>();

            if (movepool.size() < 5) // no choice to be had for movesets anyway
            {
                curRoles.add(Role.DEFAULT);
            }
            else
            {
                if (isPhysicalOffense(mon, movesPhy, levelTier(lvl)))
                {
                    curRoles.add(Role.PHYOFF);
                }
                if (isSpecialOffense(mon, movesSpe, levelTier(lvl)))
                {
                    curRoles.add(Role.SPEOFF);
                }
                if (isMixed(mon, curRoles))
                {
                    curRoles.add(Role.MIXED);
                }
                if (isPhysicalSweeper(mon, movesPhy, levelTier(lvl)))
                {
                    curRoles.add(Role.PHYSWEEPER);
                }
                if (isSpecialSweeper(mon, movesSpe, levelTier(lvl)))
                {
                    curRoles.add(Role.SPESWEEPER);
                }
                if (isPhysicalTank(mon, movesPhy, levelTier(lvl)))
                {
                    curRoles.add(Role.PHYTANK);
                }
                if (isSpecialTank(mon, movesSpe, levelTier(lvl)))
                {
                    curRoles.add(Role.SPETANK);
                }
                if (isSupport(mon, movesSta))
                {
                    curRoles.add(Role.SUPPORT);
                }
                if (isStaller(mon, movesSta))
                {
                    curRoles.add(Role.STALLER);
                }
                if (isSleepTalker(mon, movesSta, curRoles))
                {
                    curRoles.add(Role.SLEEP_TALKER);
                }
                if (isDreamEater(mon, movepool, curRoles))
                {
                    curRoles.add(Role.DREAM_EATER);
                }
                if (isCurser(mon, movesSta, curRoles))
                {
                    curRoles.add(Role.CURSER);
                }
                if (isFlailer(movepool, curRoles))
                {
                    curRoles.add(Role.FLAILER);
                }
                if (isBellyDrummer(movepool, curRoles))
                {
                    curRoles.add(Role.BELLY_DRUMMER);
                }
                if (isPerishTrapper(movepool))
                {
                    curRoles.add(Role.PERISH_TRAPPER);
                }

                if (curRoles.isEmpty()) // if no roles applicable
                {
                    curRoles.add(Role.DEFAULT);
                }
            }

            for (int j = curRoles.size() - 1; j >= 0; j--)
            {
                switch (curRoles.get(j))
                {
                    case PHYOFF:
                        if (curRoles.contains(Role.PHYSWEEPER)
                                || curRoles.contains(Role.PHYTANK)
                                || curRoles.contains(Role.SLEEP_TALKER)
                                || curRoles.contains(Role.FLAILER))
                        {
                            curRoles.remove(j);
                        }
                        break;
                    case SPEOFF:
                        if (curRoles.contains(Role.SPESWEEPER)
                                || curRoles.contains(Role.SPETANK)
                                || curRoles.contains(Role.SLEEP_TALKER)
                                || curRoles.contains(Role.DREAM_EATER))
                        {
                            curRoles.remove(j);
                        }
                        break;
                }
            }

            roles.add(curRoles);
        }

        return roles;
    }

    private boolean isPhysicalOffense(PokemonGame mon, ArrayList<Move> movesPhy, int maxTier)
    {
        Move mainMovePhy = getMainMove(mon, movesPhy);

        return ((hasNonSituational(movesPhy))
                && // has physical moves
                (mon.getAtk() >= 0.15 * mon.getBST())
                && // atk stat is good
                (mainMovePhy.getTier() >= maxTier - 1) // good main physical move
                );
    }

    private boolean isSpecialOffense(PokemonGame mon, ArrayList<Move> movesSpe, int maxTier)
    {
        Move mainMoveSpe = getMainMove(mon, movesSpe);

        return ((hasNonSituational(movesSpe))
                && // has special moves
                (mon.getSAtk() >= 0.15 * mon.getBST())
                && // satk stat is good
                (mainMoveSpe.getTier() >= maxTier - 1) // good main special move
                );
    }

    private boolean isMixed(PokemonGame mon, ArrayList<Role> roles)
    {
        return ((roles.contains(Role.PHYOFF))
                && (roles.contains(Role.SPEOFF))
                && (abs(mon.getAtk() - mon.getSAtk()) < 0.05 * mon.getBST()) // no appreciable difference between stats
                );
    }

    private boolean isPhysicalSweeper(PokemonGame mon, ArrayList<Move> movesPhy, int maxTier)
    {
        Move mainMovePhy = getMainMove(mon, movesPhy);

        return ((hasNonSituational(movesPhy))
                && // has physical moves
                (mon.getAtk() >= 0.18 * mon.getBST())
                && // atk stat is good
                (mon.getSpd() >= 0.13 * mon.getBST())
                && // spd stat is good
                (mainMovePhy.getTier() >= maxTier - 1) // good main physical move
                );
    }

    private boolean isSpecialSweeper(PokemonGame mon, ArrayList<Move> movesSpe, int maxTier)
    {
        Move mainMoveSpe = getMainMove(mon, movesSpe);

        return ((hasNonSituational(movesSpe))
                && // has special moves
                (mon.getSAtk() >= 0.18 * mon.getBST())
                && // satk stat is good
                (mon.getSpd() >= 0.13 * mon.getBST())
                && // satk stat is good
                (mainMoveSpe.getTier() >= maxTier - 1) // good main special move
                );
    }

    private boolean isPhysicalTank(PokemonGame mon, ArrayList<Move> movesPhy, int maxTier)
    {
        Move mainMovePhy = getMainMove(mon, movesPhy);

        return ((hasNonSituational(movesPhy))
                && // has physical moves
                ((mon.getDef() + mon.getHP() > 0.3 * mon.getBST())
                || (mon.getSDef() + mon.getHP() > 0.3 * mon.getBST()))
                && // good (sp) defense
                (mon.getAtk() >= 0.1 * mon.getBST())
                && // atk stat is useable
                (mainMovePhy.getTier() >= maxTier - 1) // top main physical move
                );
    }

    private boolean isSpecialTank(PokemonGame mon, ArrayList<Move> movesSpe, int maxTier)
    {
        Move mainMoveSpe = getMainMove(mon, movesSpe);

        return ((hasNonSituational(movesSpe))
                && // has special moves
                ((mon.getDef() + mon.getHP() > 0.3 * mon.getBST())
                || (mon.getSDef() + mon.getHP() > 0.3 * mon.getBST()))
                && // good (sp) defense
                (mon.getSAtk() >= 0.1 * mon.getBST())
                && // satk stat is useable
                (mainMoveSpe.getTier() >= maxTier - 1) // top main special move
                );
    }

    private boolean isSupport(PokemonGame mon, ArrayList<Move> movesSta)
    {
        return ((numSupportMoves(movesSta) >= 2)
                && // has support moves
                ((mon.getDef() + mon.getHP() > 0.3 * mon.getBST())
                || (mon.getSDef() + mon.getHP() > 0.3 * mon.getBST())) // good (sp) defense
                );
    }

    private boolean isStaller(PokemonGame mon, ArrayList<Move> movesSta)
    {
        return ((hasMoveEffect(movesSta, MoveEffect.CAUSE_TOXIC))
                && (numStallMoves(movesSta) >= 2)
                && // has stall moves
                ((mon.getDef() + mon.getHP() > 0.35 * mon.getBST())
                || (mon.getSDef() + mon.getHP() > 0.35 * mon.getBST())) // good (sp) defense
                );
    }

    private boolean isSleepTalker(PokemonGame mon, ArrayList<Move> movesSta, ArrayList<Role> roles)
    {
        return ((hasRest(movesSta))
                && (!hasHealing(movesSta))
                && (hasMoveEffect(movesSta, MoveEffect.SLEEP_TALK))
                && (roles.contains(Role.PHYSWEEPER) || roles.contains(Role.SPESWEEPER)
                || (roles.contains(Role.PHYTANK) || roles.contains(Role.SPETANK)))
                && ((mon.getDef() + mon.getHP() > 0.3 * mon.getBST())
                || (mon.getSDef() + mon.getHP() > 0.3 * mon.getBST())) // good (sp) defense
                );
    }

    private boolean isDreamEater(PokemonGame mon, ArrayList<Move> movepool, ArrayList<Role> roles)
    {
        return ((hasMoveEffect(movepool, MoveEffect.DREAM_EATER))
                && (hasMoveEffect(movepool, MoveEffect.CAUSE_SLP))
                && (roles.contains(Role.SPEOFF) || roles.contains(Role.SPETANK)));
    }

    private boolean isCurser(PokemonGame mon, ArrayList<Move> movesSta, ArrayList<Role> roles)
    {
        return ((hasMoveEffect(movesSta, MoveEffect.CURSE))
                && (!isGhost(mon))
                && (roles.contains(Role.PHYOFF) || roles.contains(Role.PHYTANK))
                && (mon.getSpd() < 0.1 * mon.getBST()));
    }

    private boolean isFlailer(ArrayList<Move> movepool, ArrayList<Role> roles)
    {
        return ((hasMoveEffect(movepool, MoveEffect.REVERSAL))
                && (hasMoveEffect(movepool, MoveEffect.ENDURE))
                && (roles.contains(Role.PHYOFF)));
    }
    
    private boolean isBellyDrummer(ArrayList<Move> movepool, ArrayList<Role> roles)
    {
        return ((hasMoveEffect(movepool, MoveEffect.BELLY_DRUM))
                && (roles.contains(Role.PHYOFF)));
    }
    
    private boolean isPerishTrapper(ArrayList<Move> movepool)
    {
        return ((hasMoveEffect(movepool, MoveEffect.PERISH_SONG))
                && (hasMoveEffect(movepool, MoveEffect.MEAN_LOOK)));
    }

    private int numSupportMoves(ArrayList<Move> movepool)
    {
        // count number of support moves
        int count = 0;

        for (Move m : movepool)
        {
            if (m.getCat() != MOVE_OTHER_CATEGORY) continue;

            if (isSupportMove(m))
            {
                count++;
            }
        }

        return count;
    }

    private int numStallMoves(ArrayList<Move> movepool)
    {
        // count number of stall moves
        int count = 0;

        for (Move m : movepool)
        {
            MoveEffect eff = getEffect(m);

            if ((eff == MoveEffect.CAUSE_CNF)
                    || (eff == MoveEffect.RECOVER_REST && m.getIndex() != (byte) 0x9C)
                    || (eff == MoveEffect.PROTECT)
                    || (eff == MoveEffect.MORNING_SUN)
                    || (eff == MoveEffect.SYNTHESIS)
                    || (eff == MoveEffect.MOONLIGHT)
                    || (eff == MoveEffect.ENCORE)
                    || (eff == MoveEffect.FLY_DIG))
            {
                count++;
            }
        }

        return count;
    }

    Move getMainMove(PokemonGame mon, ArrayList<Move> movepool)
    {
        // returns the main damaging move
        Move mainMove = null;
        int mainMovePower = 0;

        for (Move m : movepool)
        {
            if (m.getCalcPower() == 0)
            {
                continue;
            }

            MoveEffect eff = getEffect(m);

            if (eff == MoveEffect.EXPLODE)
            {
                continue;
            }
            if (eff == MoveEffect.RECHARGE)
            {
                continue;
            }

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

    boolean hasHealing(ArrayList<Move> movepool)
    {
        boolean test = false;

        if (hasRecover(movepool))
        {
            test = true;
        }
        else if (hasMoveEffect(movepool, MoveEffect.MORNING_SUN))
        {
            test = true;
        }
        else if (hasMoveEffect(movepool, MoveEffect.SYNTHESIS))
        {
            test = true;
        }
        else if (hasMoveEffect(movepool, MoveEffect.MOONLIGHT))
        {
            test = true;
        }

        return test;
    }

    boolean hasRest(ArrayList<Move> movepool)
    {
        boolean test = false;

        for (Move m : movepool)
        {
            if (m.getCat() != MOVE_OTHER_CATEGORY)
            {
                continue;
            }

            MoveEffect curEff = getEffect(m);

            if (curEff == MoveEffect.RECOVER_REST && m.getIndex() == (byte) 0x9C)
            {
                test = true;
                break;
            }
        }

        return test;
    }

    boolean hasRecover(ArrayList<Move> movepool)
    {
        boolean test = false;

        for (Move m : movepool)
        {
            if (m.getCat() != MOVE_OTHER_CATEGORY)
            {
                continue;
            }

            MoveEffect curEff = getEffect(m);

            if (curEff == MoveEffect.RECOVER_REST && m.getIndex() != (byte) 0x9C)
            {
                test = true;
                break;
            }
        }

        return test;
    }

    boolean isGhost(PokemonGame mon)
    {
        Type[] types = mon.getTypes();
        return (types[0] == Type.GHOST || types[1] == Type.GHOST);
    }
}
