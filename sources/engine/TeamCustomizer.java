package engine;

import java.util.ArrayList;
import static java.lang.Math.*;

import static data.Constants.*;
import data.PokemonGame;
import data.Move;

class TeamCustomizer extends MoveAnalyser
{
    MoveSorter moveSorter;
    PokemonClassifier monClassifier;
    Names names;

    TeamCustomizer(Move[] moves, MoveSorter moveSorter, Names names)
    {
        this.moves = moves;
        this.moveSorter = moveSorter;
        monClassifier = new PokemonClassifier(moves, moveSorter);
        this.names = names;
    }

    ArrayList<ArrayList<Move>> customize(PokemonGame[] team, int[] lvls, PokemonGame[] mons)
    {
        // assign roles
        ArrayList<ArrayList<Move>> movesets = new ArrayList<>();
        ArrayList<ArrayList<Role>> roles = monClassifier.classify(team, lvls, mons);

        for (int i = 0; i < team.length; i++)
        {
            PokemonGame mon = team[i];
            int lvl = lvls[i];
            Role chosenRole = chooseRole(mon, roles.get(i));

            ArrayList<Move> movepool = getMovepool(mon, lvl, mons, moveSorter.getAllLearnable());
            ArrayList<Move> lvlUpMoves = getLevelUpMoves(mon, lvl, mons);

            ArrayList<Move> curMoveset = generateMoveset(mon, lvl, chosenRole, movepool, lvlUpMoves);

            if (true)
            {
                ArrayList<Double> prob = calculateProbabilities(mon, roles.get(i));
                System.out.println(names.pokemon(team[i].getIndex()) + " lvl" + lvls[i]);
                System.out.printf("Roles: ");

                for (int j = 0; j < roles.get(i).size(); j++)
                {
                    System.out.print(roles.get(i).get(j) + " " + (int) (prob.get(j) * 100) + "% | ");
                }

                System.out.printf("\n");
                System.out.printf("Chosen:" + chosenRole + "\n\n");

                curMoveset.forEach((m) ->
                {
                    System.out.println("\t" + names.move(m.getIndex()) + ", tier " + m.getTier());
                });

                System.out.printf("\n");
            }

            movesets.add(curMoveset);
        }

        return movesets;
    }

    private Role chooseRole(PokemonGame mon, ArrayList<Role> roles)
    {
        // chooses a random role
        ArrayList<Double> prob = calculateProbabilities(mon, roles);
        Double cumulative = prob.get(0);
        Double r = random();
        int i = 0;
        Role chosenRole = null;

        for (Double p : prob)
        {
            if (r < cumulative)
            {
                chosenRole = roles.get(i);
                break;
            }

            i++;
            cumulative += p;
        }

        if (chosenRole == null) // failsafe
        {
            chosenRole = roles.get(roles.size() - 1);
        }

        return chosenRole;
    }

    private ArrayList<Double> calculateProbabilities(PokemonGame mon, ArrayList<Role> roles)
    {
        // calculate the weighted probabilities of getting each role according to Pokemon's attributes
        ArrayList<Double> prob = new ArrayList<>();
        ArrayList<Double> points = new ArrayList<>();
        double totalPoints = 0;

        int hp = mon.getHP();
        int atk = mon.getAtk();
        int satk = mon.getSAtk();
        int def = mon.getDef();
        int sdef = mon.getSDef();
        int spd = mon.getSpd();

        int statAvrg = mon.getBST() / 6;

        for (Role r : roles)
        {
            double calc = 0;

            switch (r)
            {
                case PHYOFF:
                    calc += (atk - statAvrg) / (double) 1;
                    break;
                case SPEOFF:
                    calc += (satk - statAvrg) / (double) 1;
                    break;
                case MIXED:
                    calc += (atk - statAvrg) / (double) 2;
                    calc += (satk - statAvrg) / (double) 2;
                    break;
                case PHYSWEEPER:
                case FLAILER:
                    calc += (atk - statAvrg) / (double) 2;
                    calc += (spd - statAvrg) / (double) 2;
                    break;
                case SPESWEEPER:
                case DREAM_EATER:
                    calc += (satk - statAvrg) / (double) 2;
                    calc += (spd - statAvrg) / (double) 2;
                    break;
                case PHYTANK:
                case CURSER:
                    calc += (atk - statAvrg) / (double) 3;
                    calc += (def - statAvrg) / (double) 3;
                    calc += (sdef - statAvrg) / (double) 3;
                    break;
                case SPETANK:
                    calc += (satk - statAvrg) / (double) 3;
                    calc += (def - statAvrg) / (double) 3;
                    calc += (sdef - statAvrg) / (double) 3;
                    break;
                case SUPPORT:
                case STALLER:
                    calc += (hp - statAvrg) / (double) 3;
                    calc += (def - statAvrg) / (double) 3;
                    calc += (sdef - statAvrg) / (double) 3;
                    break;
                case SLEEP_TALKER:
                    calc += (hp - statAvrg) / (double) 2;
                    calc += max((atk - statAvrg), (satk - statAvrg)) / (double) 2;
                    break;
                default:
                    calc = 1;
            }

            calc = max(1, calc);

            points.add(calc);
            totalPoints += calc;
        }

        for (int i = 0; i < roles.size(); i++)
        {
            prob.add(points.get(i) / totalPoints);
        }

        return prob;
    }

    private ArrayList<Move> generateMoveset(PokemonGame mon, int lvl, Role role, ArrayList<Move> movepool, ArrayList<Move> lvlUpMoves)
    {
        int maxTier = levelTier(lvl);
        trimMovepool(movepool, lvlUpMoves, maxTier);

        movepool.sort(MoveTierCmp); // sort by tier from strongest to weakest
        ArrayList<Move> movesPhy = getPhysical(movepool);
        ArrayList<Move> movesSpe = getSpecial(movepool);
        ArrayList<Move> movesSta = getStatus(movepool);

        ArrayList<Move> movesMix = new ArrayList<>();
        movesMix.addAll(movesPhy);
        movesMix.addAll(movesSpe);
        movesMix.sort(MoveTierCmp); // sort by tier from strongest to weakest

        ArrayList<Move> moveset;

        switch (role)
        {
            case PHYOFF:
                moveset = basicOffenseMoveset(mon, maxTier, movesPhy, movesSta);
                break;
            case SPEOFF:
                moveset = basicOffenseMoveset(mon, maxTier, movesSpe, movesSta);
                break;
            case MIXED:
                moveset = basicOffenseMoveset(mon, maxTier, movesMix, movesSta);
                break;
            case PHYSWEEPER:
                moveset = sweeperMoveset(mon, maxTier, movesPhy, movesSta);
                break;
            case SPESWEEPER:
                moveset = sweeperMoveset(mon, maxTier, movesSpe, movesSta);
                break;
            case PHYTANK:
                moveset = tankMoveset(mon, maxTier, movesPhy, movesSta);
                break;
            case SPETANK:
                moveset = tankMoveset(mon, maxTier, movesSpe, movesSta);
                break;
            case SUPPORT:
                moveset = supportMoveset(mon, movesMix, movesSta);
                break;
            case STALLER:
                moveset = stallerMoveset(mon, movesMix, movesSta);
                break;
            case SLEEP_TALKER:
                moveset = sleepTalkerMoveset(mon, movesMix, movesSta);
                break;
            case DREAM_EATER:
                moveset = dreamEaterMoveset(mon, movesMix, movesSta);
                break;
            case CURSER:
                moveset = curserMoveset(mon, maxTier, movesPhy, movesSta);
                break;
            case FLAILER:
                moveset = flailerMoveset(mon, maxTier, movesPhy, movesSta);
                break;
            default:
                moveset = defaultMoveset(movepool);
        }

        padMovepool(moveset, lvlUpMoves);
        return moveset;
    }

    private void trimMovepool(ArrayList<Move> movepool, ArrayList<Move> lvlUpMoves, int maxTier)
    {
        // removes moves that are over maxTier, except those that can already be attained by level up
        for (int i = movepool.size() - 1; i >= 0; i--)
        {
            Move curMove = movepool.get(i);
            if (curMove.getTier() > maxTier && !lvlUpMoves.contains(curMove))
            {
                movepool.remove(i);
            }
        }
    }

    private void padMovepool(ArrayList<Move> moveset, ArrayList<Move> lvlUpMoves)
    {
        // adds level up moves until at 4 moves
        for (Move m : lvlUpMoves)
        {
            if (moveset.size() >= 4)
            {
                break;
            }
            if (moveset.contains(m))
            {
                continue;
            }
            if (getEffect(m) != MoveEffect.NO_EFFECT && hasMoveEffect(moveset, getEffect(m)))
            {
                continue;
            }
            moveset.add(m);
        }
    }

    private void basicOffenseMoveset(ArrayList<Move> moveset, PokemonGame mon, int maxTier, ArrayList<Move> movesOff)
    {
        // tries to find maxTier moves, with tolerance of one tier below
        // may add nothing

        ArrayList<ArrayList<Move>> movesSTAB = getSTAB(mon, movesOff);
        ArrayList<Move> movesCvrg = getCoverage(mon, movesOff);
        ArrayList<Move> movesNorm = getNormalType(mon, movesOff);

        // step 1: decide main STAB move(s)
        for (ArrayList<Move> curMovesSTAB : movesSTAB)
        {
            if (!hasNonSituational(curMovesSTAB))
            {
                break;
            }

            int minTier = max(0, maxTier - 1);
            Move randMove = pickRandomMove(curMovesSTAB, minTier, N_MOVE_TIERS, false);
            if (randMove != null && !moveset.contains(randMove)) // if found
            {
                moveset.add(randMove);
            }
            else
            {
                break;
            }
        }

        // step 2: decide coverage move(s)
        int numCvrgMoves = (maxTier <= 2) ? 1 : 4 - moveset.size();

        for (int i = 0; i < numCvrgMoves; i++)
        {
            if (!hasNonSituational(movesCvrg))
            {
                break;
            }

            int minTier = max(0, maxTier - 1);
            Move randMove = pickRandomMove(movesCvrg, minTier, N_MOVE_TIERS, false);
            if (randMove != null && !moveset.contains(randMove)) // if found
            {
                moveset.add(randMove);
                discardRedundant(movesCvrg, randMove);
            }
            else
            {
                break;
            }

        }

        // step 3: decide normal move(s)
        int numNormMoves = (moveset.size() == 4) ? 0 : 1;

        for (int i = 0; i < numNormMoves; i++)
        {
            if (!hasNonSituational(movesNorm))
            {
                break;
            }

            Move randMove = pickRandomMove(movesNorm, maxTier, N_MOVE_TIERS, false);
            if (randMove != null && !moveset.contains(randMove)) // if found
            {
                moveset.add(randMove);
            }
            else
            {
                break;
            }
        }
    }

    private void basicOffenseMoveset(ArrayList<Move> moveset, PokemonGame mon, int maxTier, ArrayList<Move> movesOff, ArrayList<Move> movesSta)
    {
        basicOffenseMoveset(moveset, mon, maxTier, movesOff);

        // step 4: decide status move(s)
        int numStaMoves = 4 - moveset.size();

        for (int i = 0; i < numStaMoves; i++)
        {
            if (!hasNonSituational(movesSta))
            {
                break;
            }

            Move randMove = pickRandomMove(movesSta, false);
            if (randMove != null && !moveset.contains(randMove)) // if found
            {
                moveset.add(randMove);
            }
            else
            {
                break;
            }
        }

        // step 5: fill in possible empty slots
        while (hasNonSituational(movesOff) && moveset.size() < 4)
        {
            int minTier = 0;
            Move randMove = pickRandomMove(movesOff, minTier, N_MOVE_TIERS, false);
            if (randMove != null && !moveset.contains(randMove)) // if found
            {
                moveset.add(randMove);
            }
            else
            {
                break;
            }

            System.out.println(movesOff.size());
        }
    }

    private ArrayList<Move> basicOffenseMoveset(PokemonGame mon, int maxTier, ArrayList<Move> movesOff, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();
        basicOffenseMoveset(moveset, mon, maxTier, movesOff, movesSta);
        return moveset;
    }

    private ArrayList<Move> sweeperMoveset(PokemonGame mon, int maxTier, ArrayList<Move> movesOff, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();
        int curTier = maxTier;

        do
        {
            basicOffenseMoveset(moveset, mon, curTier, movesOff);
            curTier--;
        }
        while (moveset.size() < 2 && curTier >= 0);

        if (moveset.size() >= 2)
        {
            boolean isSpecial = (moveset.get(0).getCat() == MOVE_SPECIAL_CATEGORY);
            ArrayList<Move> movesToAdd = new ArrayList<>();

            // first, +2 setup moves
            Move setupMove = pickOffSetupMoveUp2(mon, movesSta, isSpecial);
            if (setupMove != null)
            {
                movesToAdd.add(setupMove);
            }

            // second, healing
            Move healMove = pickHealingMove(movesSta);
            if (healMove != null)
            {
                movesToAdd.add(healMove);
            }

            // third, status inflicting
            Move statusMove = null;
            if (movesToAdd.isEmpty() || moveset.size() + movesToAdd.size() < 3)
            {
                statusMove = pickStatusMove(movesSta);
            }
            if (statusMove != null)
            {
                movesToAdd.add(statusMove);
            }

            // fourth, +1 setup moves
            if (movesToAdd.isEmpty()) // only add if none of the above were added
            {
                setupMove = pickOffSetupMoveUp1(mon, movesSta, isSpecial);
                if (setupMove != null)
                {
                    movesToAdd.add(setupMove);
                }
            }

            fitMovesIn(movesToAdd, moveset);
        }

        return moveset;
    }

    private ArrayList<Move> tankMoveset(PokemonGame mon, int maxTier, ArrayList<Move> movesOff, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();
        int curTier = maxTier;

        do
        {
            basicOffenseMoveset(moveset, mon, curTier, movesOff);
            curTier--;
        }
        while (moveset.size() < 1 && curTier >= 0);

        ArrayList<Move> movesToAdd = new ArrayList<>();

        // first, healing
        Move healMove = pickHealingMove(movesSta);
        if (healMove != null)
        {
            movesToAdd.add(healMove);
        }

        // explosion
        Move moveExp = pickMoveEffect(movesOff, MoveEffect.EXPLODE);
        if (moveExp != null && healMove == null)
        {
            movesToAdd.add(moveExp);
        }

        // second, status inflicting
        Move statusMove = pickStatusMove(movesSta);
        if (statusMove != null)
        {
            movesToAdd.add(statusMove);
        }

        // third, +2 setup moves
        if (movesToAdd.isEmpty()) // only add if none of the above were added
        {
            Move setupMove = pickDefSetupMoveUp2(movesSta);
            if (setupMove != null)
            {
                movesToAdd.add(setupMove);
            }
        }

        fitMovesIn(movesToAdd, moveset);

        return moveset;
    }

    private ArrayList<Move> supportMoveset(PokemonGame mon, ArrayList<Move> movesOff, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();
        ArrayList<Move> movesToAdd = new ArrayList<>();

        // first, healing
        Move healMove = pickHealingMove(movesSta);
        if (healMove != null)
        {
            movesToAdd.add(healMove);
        }

        // second, support moves
        addSupportMoves(movesToAdd, movesSta);

        // explosion
        Move moveExp = pickMoveEffect(movesOff, MoveEffect.EXPLODE);
        if (moveExp != null && healMove == null)
        {
            movesToAdd.add(moveExp);
        }

        fitMovesIn(movesToAdd, moveset);

        if (moveset.size() < 4)
        {
            Move moveOff = getStrongestMove(mon, movesOff, false);
            if (moveOff != null)
            {
                moveset.add(moveOff);
            }
        }

        return moveset;
    }

    private ArrayList<Move> stallerMoveset(PokemonGame mon, ArrayList<Move> movesOff, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();
        ArrayList<Move> movesToAdd = new ArrayList<>();
        boolean hasOffensive = false;

        // first, toxic
        movesToAdd.add(pickMoveEffect(movesSta, MoveEffect.CAUSE_TOXIC));

        // second, healing
        Move healMove = pickHealingMove(movesSta);
        if (healMove != null)
        {
            movesToAdd.add(healMove);
        }

        // third, semi-invulnerable moves
        Move semiInvulnMove = pickMoveEffect(movesOff, MoveEffect.FLY_DIG);
        if (semiInvulnMove != null && mon.getAtk() > 0.1 * mon.getBST()
                && mon.getAtk() >= mon.getSAtk())
        {
            movesToAdd.add(semiInvulnMove);
            hasOffensive = true;
        }

        // or a reliable damaging move
        Move moveOff = getStrongestMove(mon, movesOff, false);
        if (moveOff != null && !hasOffensive
                && ((moveOff.getCat() == MOVE_PHYSICAL_CATEGORY && mon.getAtk() > 0.1 * mon.getBST())
                || (moveOff.getCat() == MOVE_SPECIAL_CATEGORY && mon.getSAtk() > 0.1 * mon.getBST())))
        {
            movesToAdd.add(moveOff);
            hasOffensive = true;
        }

        // explosion
        Move moveExp = pickMoveEffect(movesOff, MoveEffect.EXPLODE);
        if (moveExp != null && hasOffensive)
        {
            movesToAdd.add(moveExp);
        }

        // fourth, cause confusion
        Move staMove = pickMoveEffect(movesSta, MoveEffect.CAUSE_CNF);
        if (staMove != null && moveset.size() < 4)
        {
            movesToAdd.add(staMove);
        }

        // fifth, protect
        staMove = pickMoveEffect(movesSta, MoveEffect.PROTECT);
        if (staMove != null && moveset.size() < 4)
        {
            movesToAdd.add(staMove);
        }

        // rest
        if (healMove == null)
        {
            healMove = pickRest(movesSta);
            if (healMove != null)
            {
                movesToAdd.add(healMove);
            }
        }

        fitMovesIn(movesToAdd, moveset);

        return moveset;
    }

    private ArrayList<Move> sleepTalkerMoveset(PokemonGame mon, ArrayList<Move> movesOff, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();

        for (int i = 0; i < 2; i++)
        {
            Move moveOff = getStrongestMove(mon, movesOff, false);
            if (moveOff != null)
            {
                moveset.add(moveOff);
                discardRedundant(movesOff, moveOff);
            }
        }

        if (moveset.size() >= 2)
        {
            ArrayList<Move> movesToAdd = new ArrayList<>();

            // rest and sleep talk
            movesToAdd.add(pickRest(movesSta));
            movesToAdd.add(pickMoveEffect(movesSta, MoveEffect.SLEEP_TALK));

            fitMovesIn(movesToAdd, moveset);
        }

        return moveset;
    }

    private ArrayList<Move> dreamEaterMoveset(PokemonGame mon, ArrayList<Move> movesOff, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();
        ArrayList<Move> movesToAdd = new ArrayList<>();

        // sleeping move and dream eater
        movesToAdd.add(pickMoveEffect(movesSta, MoveEffect.CAUSE_SLP));
        movesToAdd.add(pickMoveEffect(movesOff, MoveEffect.DREAM_EATER));

        addSupportMoves(movesToAdd, movesSta);
        fitMovesIn(movesToAdd, moveset);

        return moveset;
    }

    private ArrayList<Move> curserMoveset(PokemonGame mon, int maxTier, ArrayList<Move> movesPhy, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();
        int curTier = maxTier;

        do
        {
            basicOffenseMoveset(moveset, mon, curTier, movesPhy);
            curTier--;
        }
        while (moveset.size() < 2 && curTier >= 0);

        if (moveset.size() >= 2)
        {
            ArrayList<Move> movesToAdd = new ArrayList<>();

            movesToAdd.add(pickMoveEffect(movesSta, MoveEffect.CURSE));

            // first, healing
            Move healMove = pickHealingMove(movesSta);
            if (healMove != null)
            {
                movesToAdd.add(healMove);
            }

            // explosion
            Move moveExp = pickMoveEffect(movesPhy, MoveEffect.EXPLODE);
            if (moveExp != null && healMove == null)
            {
                movesToAdd.add(moveExp);
            }

            // second, status inflicting
            Move statusMove = null;
            if (movesToAdd.isEmpty() || moveset.size() + movesToAdd.size() < 3)
            {
                statusMove = pickStatusMove(movesSta);
            }
            if (statusMove != null)
            {
                movesToAdd.add(statusMove);
            }

            fitMovesIn(movesToAdd, moveset);
        }

        return moveset;
    }

    private ArrayList<Move> flailerMoveset(PokemonGame mon, int maxTier, ArrayList<Move> movesPhy, ArrayList<Move> movesSta)
    {
        ArrayList<Move> moveset = new ArrayList<>();
        int curTier = maxTier;

        do
        {
            basicOffenseMoveset(moveset, mon, curTier, movesPhy);
            curTier--;
        }
        while (moveset.size() < 2 && curTier >= 0);

        boolean isSpecial = false;
        ArrayList<Move> movesToAdd = new ArrayList<>();

        Move moveReversal = pickMoveEffect(movesSta, MoveEffect.REVERSAL);
        movesSta.remove(movesSta.indexOf(moveReversal));

        Move moveReversalOther = pickMoveEffect(movesSta, MoveEffect.REVERSAL);

        if (moveReversalOther != null)
        {
            if (checkSTAB(mon, moveReversalOther)
                    || (moveReversal.getType() == Type.NORMAL && !checkSTAB(mon, moveReversal)))
            {
                moveReversal = moveReversalOther;
            }
        }

        movesToAdd.add(moveReversal);
        movesToAdd.add(pickMoveEffect(movesSta, MoveEffect.ENDURE));

        // first, +2 setup moves
        Move setupMove = pickOffSetupMoveUp2(mon, movesSta, isSpecial);
        if (setupMove != null)
        {
            movesToAdd.add(setupMove);
        }

        // second, status inflicting
        Move statusMove = null;
        if (movesToAdd.isEmpty() || moveset.size() + movesToAdd.size() < 3)
        {
            statusMove = pickStatusMove(movesSta);
        }
        if (statusMove != null)
        {
            movesToAdd.add(statusMove);
        }

        // third, +1 setup moves
        if (movesToAdd.isEmpty()) // only add if none of the above were added
        {
            setupMove = pickOffSetupMoveUp1(mon, movesSta, isSpecial);
            if (setupMove != null)
            {
                movesToAdd.add(setupMove);
            }
        }

        fitMovesIn(movesToAdd, moveset);

        return moveset;
    }

    private ArrayList<Move> defaultMoveset(ArrayList<Move> movepool)
    {
        ArrayList<Move> moveset = new ArrayList<>();

        int count = 0;

        for (Move m : movepool)
        {
            moveset.add(m);
            count++;
            if (count == 4)
            {
                break;
            }
        }

        return moveset;
    }

    private Move getStrongestMove(PokemonGame mon, ArrayList<Move> movepool, boolean includeSituational)
    {
        // returns the move with highest power output
        Move maxMove = null;
        int maxPower = 0;

        for (Move move : movepool)
        {
            if (move.getCalcPower() == 0)
            {
                continue;
            }

            if (getEffect(move).situational() && !includeSituational)
            {
                continue;
            }
            if (getEffect(move) == MoveEffect.RECHARGE && !includeSituational)
            {
                continue;
            }

            int power = calculateRelativePower(mon, move);

            if ((maxMove == null)
                    || (move.getType() != Type.NORMAL && power > maxPower)
                    || (move.getType() == Type.NORMAL
                    && (maxMove.getType() != Type.NORMAL && power > 2 * maxPower)
                    || (maxMove.getType() == Type.NORMAL && power > maxPower)))
            {
                maxMove = move;
                maxPower = power;
            }
        }

        return maxMove;
    }

    private int calculateRelativePower(PokemonGame mon, Move move)
    {
        int power = move.getCalcPower();

        int atk = mon.getAtk();
        int satk = mon.getSAtk();

        if (!isFixed(move))
        {
            power = (checkSTAB(mon, move)) ? (int) (power * 1.5)
                    : power;

            power = (move.getCat() == MOVE_PHYSICAL_CATEGORY) ? power * atk
                    : power * satk;
        }

        return power;
    }

    private void addSupportMoves(ArrayList<Move> moveset, ArrayList<Move> movepool)
    {
        for (Move m : movepool)
        {
            if (isSupportMove(m))
            {
                if (!hasMoveEffect(moveset, getEffect(m))
                        || (m.getTier() > pickMoveEffect(moveset, getEffect(m)).getTier()))
                {
                    moveset.add(m);
                }
            }
        }

        if (hasMoveEffect(moveset, MoveEffect.CAUSE_SLP)
                && hasMoveEffect(moveset, MoveEffect.CAUSE_PRZ))
        {
            for (Move m : movepool)
            {
                if (getEffect(m) == MoveEffect.CAUSE_PRZ)
                {
                    moveset.remove(moveset.indexOf(m));
                    break;
                }
            }
        }

    }

    private void defaultMoveset(ArrayList<Move> moveset, ArrayList<Move> movepool)
    {
        for (Move m : movepool)
        {
            if (moveset.size() == 4)
            {
                break;
            }
            moveset.add(m);
        }
    }

    private Move pickOffSetupMoveUp2(PokemonGame mon, ArrayList<Move> movesSta, boolean isSpecial)
    {
        Move setupMove = null;
        Move setupMoveOff = (isSpecial) ? pickMoveEffect(movesSta, MoveEffect.SATKUP2)
                : pickMoveEffect(movesSta, MoveEffect.ATKUP2);
        Move setupMoveSpd = pickMoveEffect(movesSta, MoveEffect.SPDUP2);

        if (setupMoveOff != null && (setupMoveSpd != null && mon.getSpd() < mon.getBST() * 0.17)) // if has both, pick randomly
        {
            setupMove = (random() < 0.5) ? setupMoveOff : setupMoveSpd;
        }
        else if (setupMoveOff != null) // only has offense setup
        {
            setupMove = setupMoveOff;
        }
        else if (setupMoveSpd != null
                && // only has speed setup
                mon.getSpd() < mon.getBST() * 0.17) // and speed stat isn't best stat
        {
            setupMove = setupMoveSpd;
        }

        return setupMove;
    }

    private Move pickOffSetupMoveUp1(PokemonGame mon, ArrayList<Move> movesSta, boolean isSpecial)
    {
        Move setupMove = null;
        Move setupMoveOff = (isSpecial) ? pickMoveEffect(movesSta, MoveEffect.SATKUP1)
                : pickMoveEffect(movesSta, MoveEffect.ATKUP1);
        Move setupMoveSpd = pickMoveEffect(movesSta, MoveEffect.SPDUP1);

        if (setupMoveOff != null && (setupMoveSpd != null && mon.getSpd() < mon.getBST() * 0.17)) // if has both, pick randomly
        {
            setupMove = (random() < 0.5) ? setupMoveOff : setupMoveSpd;
        }
        else if (setupMoveOff != null) // only has offense setup
        {
            setupMove = setupMoveOff;
        }
        else if (setupMoveSpd != null
                && // only has speed setup
                mon.getSpd() < mon.getBST() * 0.17) // and speed stat isn't best stat
        {
            setupMove = setupMoveSpd;
        }

        return setupMove;
    }

    private Move pickDefSetupMoveUp2(ArrayList<Move> movesSta)
    {
        Move setupMove = null;
        Move setupMoveDef = pickMoveEffect(movesSta, MoveEffect.DEFUP2);
        Move setupMoveSDef = pickMoveEffect(movesSta, MoveEffect.SDEFUP2);

        if (setupMoveDef != null && setupMoveSDef != null) // if has both, pick randomly
        {
            setupMove = (random() < 0.5) ? setupMoveDef : setupMoveSDef;
        }
        else if (setupMoveDef != null) // only has phy setup
        {
            setupMove = setupMoveDef;
        }
        else if (setupMoveSDef != null) // only has spe setup
        {
            setupMove = setupMoveSDef;
        }

        return setupMove;
    }

    private Move pickHealingMove(ArrayList<Move> movesSta)
    {
        Move healMove = pickRecover(movesSta);
        if (healMove == null)
        {
            healMove = pickMoveEffect(movesSta, MoveEffect.MORNING_SUN);
        }
        if (healMove == null)
        {
            healMove = pickMoveEffect(movesSta, MoveEffect.SYNTHESIS);
        }
        if (healMove == null)
        {
            healMove = pickMoveEffect(movesSta, MoveEffect.MOONLIGHT);
        }
        return healMove;
    }

    private Move pickStatusMove(ArrayList<Move> movesSta)
    {
        Move statusMove = null;
        if (statusMove == null)
        {
            statusMove = pickMoveEffect(movesSta, MoveEffect.CAUSE_SLP);
        }
        if (statusMove == null)
        {
            statusMove = pickMoveEffect(movesSta, MoveEffect.CAUSE_PRZ);
        }
        if (statusMove == null)
        {
            statusMove = pickMoveEffect(movesSta, MoveEffect.CAUSE_CNF);
        }
        return statusMove;
    }

    private ArrayList<ArrayList<Move>> getSTAB(PokemonGame mon, ArrayList<Move> movepool)
    {
        ArrayList<ArrayList<Move>> movesSTAB = new ArrayList<>();
        Type[] monTypes = mon.getTypes();

        for (int i = 0; i < monTypes.length; i++)
        {
            if (i > 0 && monTypes[i] == monTypes[i - 1])
            {
                break;
            }
            ArrayList<Move> curMovesSTAB = new ArrayList<>();

            for (Move move : movepool)
            {
                if (move.getCalcPower() == 0)
                {
                    continue;
                }
                if (!checkSTAB(monTypes[i], move))
                {
                    continue;
                }
                curMovesSTAB.add(move);
            }

            movesSTAB.add(curMovesSTAB);
        }

        return movesSTAB;
    }

    private ArrayList<Move> getCoverage(PokemonGame mon, ArrayList<Move> movepool)
    {
        ArrayList<Move> movesCvrg = new ArrayList<>();

        for (Move move : movepool)
        {
            if (move.getCalcPower() == 0) continue;
            if (move.getType() == Type.NORMAL) continue;
            if (checkSTAB(mon, move)) continue;

            movesCvrg.add(move);
        }

        return movesCvrg;
    }

    private ArrayList<Move> getNormalType(PokemonGame mon, ArrayList<Move> movepool)
    {
        ArrayList<Move> movesNorm = new ArrayList<>();
        ArrayList<Move> movesNormNoEffect = new ArrayList<>();

        for (Move move : movepool)
        {
            if (move.getCalcPower() == 0)
            {
                continue;
            }
            if (move.getType() != Type.NORMAL)
            {
                continue;
            }
            if (getEffect(move) == MoveEffect.NO_EFFECT)
            {
                movesNormNoEffect.add(move);
                continue;
            }
            if (checkSTAB(mon, move))
            {
                break; // don't add for Normal-type
            }
            movesNorm.add(move);
        }

        if (movesNorm.isEmpty()) // only add effect-less normal moves if no other available
        {
            movesNorm.addAll(movesNormNoEffect);
        }

        return movesNorm;
    }

    private Move pickRandomMove(ArrayList<Move> movepool, boolean includeSituational)
    {
        Move chosenMove = null;

        // choose a move given the number of options
        for (Move m : movepool)
        {
            if (chosenMove == null)
            {
                chosenMove = m;
            }
            else // compare moves
            {
                if (random() < 1 / (double) movepool.size())
                {
                    chosenMove = m;
                }
            }
        }

        if (chosenMove != null)
        {
            movepool.remove(movepool.indexOf(chosenMove));
        }

        return chosenMove;
    }

    private Move pickRandomMove(ArrayList<Move> movepool, int minTier, int maxTier, boolean includeSituational)
    {
        // assumes ordered movepool by tier
        // go from maxTier to minTier looking at each individually and choose a random one
        // if a tier is empty, moves on to the lower one
        Move chosenMove = null;

        for (int tier = maxTier; tier >= minTier && chosenMove == null; tier--)
        {
            chosenMove = pickRandomMove(movepool, tier, includeSituational);
        }

        return chosenMove;
    }

    private Move pickRandomMove(ArrayList<Move> movepool, int tier, boolean includeSituational)
    {
        // assumes ordered movepool by tier
        Move chosenMove = null;

        ArrayList<Move> availableMoves = new ArrayList<>();

        for (Move m : movepool)
        {
            if (m.getTier() < tier)
            {
                break;
            }
            if (!includeSituational && getEffect(m).situational())
            {
                continue;
            }
            availableMoves.add(m);
        }

        // choose a move given the number of options
        for (Move m : availableMoves)
        {
            if (chosenMove == null)
            {
                chosenMove = m;
            }
            else // compare moves
            {
                if (random() < 1 / (double) availableMoves.size())
                {
                    chosenMove = m;
                }
            }
        }

        if (chosenMove != null)
        {
            movepool.remove(movepool.indexOf(chosenMove));
        }

        return chosenMove;
    }

    private Move pickMoveEffect(ArrayList<Move> movepool, MoveEffect eff)
    {
        Move move = null;

        for (Move m : movepool)
        {
            if (getEffect(m) == eff)
            {
                move = m;
                break;
            }
        }

        return move;
    }

    private Move pickRecover(ArrayList<Move> movepool)
    {
        Move move = null;

        for (Move m : movepool)
        {
            if (getEffect(m) == MoveEffect.RECOVER_REST
                    && m.getIndex() != (byte) 0x9C)
            {
                move = m;
                break;
            }
        }

        return move;
    }

    private Move pickRest(ArrayList<Move> movepool)
    {
        Move move = null;

        for (Move m : movepool)
        {
            if (getEffect(m) == MoveEffect.RECOVER_REST
                    && m.getIndex() == (byte) 0x9C)
            {
                move = m;
                break;
            }
        }

        return move;
    }

    private void discardRedundant(ArrayList<Move> movepool, Move move)
    {
        // eliminates all move candidates that are of same type in movepool
        for (int i = movepool.size() - 1; i >= 0; i--) // reverse iteration
        {
            if (move.getType() == movepool.get(i).getType())
            {
                movepool.remove(i);
            }
        }
    }

    private void fitMoveIn(Move move, ArrayList<Move> moveset)
    {
        // fits move into moveset by either filling an empty slot or
        // picking a random weak move to replace out

        if (moveset.size() < 4)
        {
            moveset.add(move);
        }
        else
        {
            ArrayList<Move> sameTierMoves = new ArrayList<>();
            int minTier = N_MOVE_TIERS;

            for (Move m : moveset)
            {
                if (m.getTier() < minTier)
                {
                    sameTierMoves.clear();
                    sameTierMoves.add(m);
                }
                else if (m.getTier() == minTier)
                {
                    sameTierMoves.add(m);
                }
            }

            int randIndex = (int) floor(random() * sameTierMoves.size());
            moveset.remove(moveset.indexOf(sameTierMoves.get(randIndex)));
            moveset.add(move);

            System.out.println(names.move(sameTierMoves.get(randIndex).getIndex()) + " replaced by " + names.move(move.getIndex()));
        }
    }

    private void fitMovesIn(ArrayList<Move> moves, ArrayList<Move> moveset)
    {
        // fits moves into moveset by either filling an empty slot or
        // picking a random weak move to replace out

        while (moves.size() > 4) // trim
        {
            moves.remove(moves.size() - 1);
        }

        for (Move mIn : moves)
        {
            if (moveset.contains(mIn))
            {
                continue;
            }

            if (moveset.size() < 4)
            {
                moveset.add(mIn);
            }
            else
            {
                ArrayList<Move> sameTierMoves = new ArrayList<>();
                int minTier = N_MOVE_TIERS;

                for (Move mS : moveset)
                {
                    if (moves.contains(mS))
                    {
                        continue; // skip moves added by method
                    }
                    if (mS.getType() == Type.NORMAL
                            && getEffect(mS) == MoveEffect.NO_EFFECT) // automatically replace effectless normal
                    {
                        sameTierMoves.clear();
                        sameTierMoves.add(mS);
                        break;
                    }

                    if (mS.getTier() < minTier)
                    {
                        minTier = mS.getTier();
                        sameTierMoves.clear();
                        sameTierMoves.add(mS);
                    }
                    else if (mS.getTier() == minTier)
                    {
                        sameTierMoves.add(mS);
                    }
                }

                int randIndex = (int) floor(random() * (float) sameTierMoves.size());
                moveset.remove(moveset.indexOf(sameTierMoves.get(randIndex)));
                moveset.add(mIn);

                //System.out.println(names.move(sameTierMoves.get(randIndex).getIndex()) + " replaced by " + names.move(mIn.getIndex()));
            }
        }
    }
}
