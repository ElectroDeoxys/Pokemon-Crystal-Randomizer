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

    private void sortMoves()
    {
        for (Move move : moves)
        {
            MoveEffect eff = getEffect(move);
            int basePower = byteToValue(move.getBasePower());
            float accFloat = byteToValue(move.getAcc());
            float acc = (accFloat / 255);
            int indexTier;
            if (basePower > 1)
            {
                indexTier = calculateDamagingMoveTier(move, eff, basePower, acc);
            }
            else if (basePower == 1)
            {
                indexTier = calculateVariableMoveTier(move, eff, acc);
            }
            else // status moves
            {
                indexTier = calculateStatusMoveTier(move, eff, acc);
            }
            // min (max) are for under (above) the specified bounds
            move.setTier(indexTier);
        }
    }

    private int calculateDamagingMoveTier(Move move, MoveEffect eff, int basePower, float acc)
    {
        int indexTier;

        // list damaging from 0 BP to 100 in discrete tiers
        int span = (UPR_POWER - BOT_POWER) / N_MOVE_TIERS;

        int calcPower = basePower;

        if (eff != MoveEffect.NEVER_MISS) // taking accuracy into account
        {
            calcPower = round(calcPower * acc);
        }

        if (hasCritAnim(move)) // taking into account crit
        {
            calcPower = round((1 + 2 * (1 / 4)) * calcPower);
        }
        else if (!isFixed(move)) // other moves that can crit
        {
            calcPower = round((1 + 2 * (1 / 16)) * calcPower);
        }

        if (null != eff)
        {
            switch (eff)
            {
                // 2-5 multiple hits hits on average 3 times
                case MULTI_HIT:
                    calcPower = 3 * calcPower;
                    break;
                // always hits twice
                case TWO_HITS:
                case TWO_HITS_PSN:
                    calcPower = 2 * calcPower;
                    break;
                // hits three times with higher BP
                case TRIPLE_HIT:
                    calcPower = 6 * calcPower;
                    break;
                // two-turn attacks
                case RAZOR_WIND:
                case SKY_ATTACK:
                case RECHARGE:
                case SOLARBEAM:
                    calcPower = calcPower / 2;
                    break;
                // dream eater
                case DREAM_EATER:
                    calcPower = 1;
                    break;
                default:
                    break;
            }
        }

        indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);

        if (eff == MoveEffect.PRIORITY) // increased priority moves get bumped up a tier
        {
            indexTier = min(indexTier + 1, N_MOVE_TIERS - 1);
        }

        move.setCalcPower(calcPower);

        return indexTier;
    }

    private int calculateVariableMoveTier(Move move, MoveEffect eff, float acc)
    {
        int indexTier = 0;

        // list damaging from 0 BP to 100 in discrete tiers
        int span = (UPR_POWER - BOT_POWER) / N_MOVE_TIERS;

        if (null != eff)
        {
            switch (eff)
            {
                case RETURN:
                case PRESENT:
                case FRUSTRATION:
                    indexTier = MOVE_BOT_TIER;
                    break;
            // countering/level-based
                case COUNTER:
                case LVL_DAMAGE:
                case RND_DAMAGE:
                case MIRROR_COAT:
                    indexTier = MOVE_MID_TIER;
                    break;
            // halving/reversal
                case HALVE_HP:
                case REVERSAL:
                    indexTier = MOVE_2ND_TIER;
                    break;
            // magnitude has average power of 71
                case MAGNITUDE:{
                    int calcPower = round(71 * acc);
                    indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
                    move.setCalcPower(calcPower);
                        break;
                    }
            // hidden power has average power of 40
                case HIDDEN_PWR:{
                    int calcPower = round(40 * acc);
                    indexTier = (int) min(floor(max(calcPower, 0) / span), N_MOVE_TIERS - 1);
                    move.setCalcPower(calcPower);
                        break;
                    }
                default:
                    break;
            }
        }

        return indexTier;
    }

    private int calculateStatusMoveTier(Move move, MoveEffect eff, float acc)
    {
        int indexTier;

        // list status from 10 PP to 35 PP in discrete tiers
        int span = (UPR_PP - BOT_PP) / N_MOVE_TIERS;
        int pP = byteToValue(move.getPP()); // generally moves with lower PP are better
        indexTier = (N_MOVE_TIERS - 1) - (int) min(floor(max(pP - BOT_PP, 0) / span), N_MOVE_TIERS - 1);

        return indexTier;
    }

    int getPower(Move move, Type[] types, int atk, int satk)
    {
        int out;
        Type moveType = move.getType();
        byte moveCat = move.getCat();

        if (move.getCalcPower() <= 1)
        {
            out = move.getCalcPower(); // do nothing with non-damaging moves
        }
        else
        {
            out = (moveType == types[0] || moveType == types[1] && (!isFixed(move))) ? (int) (move.getCalcPower() * 1.5) : move.getCalcPower(); // apply STAB except to Future Sight

            if (moveCat == 0b01000000) // physical/special move takes into account atk/satk
            {
                out = out * atk;
            }
            else
            {
                out = out * satk;
            }
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
        {
            out = dam; // do nothing with non-damaging moves
        }
        else
        {
            out = (moveType == types[0] || moveType == types[1] && (!isFixed(move))) ? (int) (dam * 1.5) : dam; // apply STAB except to fixed power

            if (moveCat == 0b01000000) // physical/special move takes into account atk/satk
            {
                out = out * atk;
            }
            else
            {
                out = out * satk;
            }
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
            {
                continue;
            }

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
        {
            if (!(nDam <= 1 && movePower <= 1))
            {
                // now look into the damaging moves
                for (int i = 0; i < 4; i++) // look into each move slot
                {
                    Move thisSlotMove = moveSet.get(i);

                    if (replaced)
                    {
                        break;
                    }

                    if (thisSlotMove.getCalcPower() <= 1)
                    {
                        continue;
                    }

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
                        for (int j = i + 1; j < 4; j++) // look into moves slots ahead
                        {
                            Move nextSlotMove = moveSet.get(j);

                            if (nextSlotMove.getCalcPower() <= 1)
                            {
                                continue; // skip non-damaging moves
                            }
                            if (nextSlotMove.getType() != thisSlotMove.getType())
                            {
                                continue; // skip moves of other types
                            }
                            if (nextSlotMove.getCalcPower() < thisSlotMove.getCalcPower()) // if that one is weaker, replace that one
                            {
                                moveSet.set(j, move);
                            }
                            else // if that one is the stronger one, replace this one
                            {
                                moveSet.set(i, move);
                            }

                            replaced = true;
                            break;
                        }
                    }
                    else if ((movePower >= slotMovePower - getPower(move, MOVE_DAM_MARGIN, monTypes, baseStats[1], baseStats[4])) // check if it's a similar move in power
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
    }

    private boolean hasCritAnim(Move move)
    {
        // returns whether it has higher critical hit ratio
        boolean out = false; // assume it isn't
        for (int i = 0; i < critAnims.length; i++)
        {
            if (critAnims[i] == move.getAnimIndex())
            {
                out = true;
            }
        }
        return out;
    }

    private int numberDamaging(ArrayList<Move> moveSet) // returns number of damaging moves in a moveset
    {
        int count = 0;

        for (int j = 0; j < 4; j++) // look into each move slot
        {
            if (moveSet.get(j).getCalcPower() > 1)
            {
                count++;
            }
        }

        return count;
    }

    private boolean repeatedType(Move move, ArrayList<Move> moveSet, int i)
    {
        boolean repType = false;

        for (int j = 0; j < 4; j++) // look into other moves to avoid repeating type
        {
            if (i == j || moveSet.get(j).getCalcPower() <= 1)
            {
                continue; // if it's the same move, or the move is not damaging, skip
            }
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
        {
            System.out.printf("%-12s: tier = %1d, calcPower = %-3d\n", names.move(m.getIndex()), m.getTier(), m.getCalcPower());
        }
    }
}
