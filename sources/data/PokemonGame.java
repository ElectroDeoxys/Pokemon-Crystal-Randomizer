package data;

import static java.lang.Math.*;

import static data.Constants.*;

public class PokemonGame extends Pokemon
{
    private int index; // in-game index of Pokemon

    public PokemonGame(int index, int trueIndex, byte[] base, byte[] typeByte, byte[] misc, byte gfx, byte[] tmhmByte, byte[][] evo, byte[][] move)
    {
        this.index = index;
        this.trueIndex = trueIndex;

        this.base = base;
        this.typeByte = typeByte;
        this.misc = misc;
        this.gfx = gfx;
        this.tmhmByte = tmhmByte;

        this.evo = evo;
        this.move = move;

        this.eggMove = new byte[0]; // initialize egg moves
        this.eggMoveCarry = new byte[0];

        convertCompatibilities();
        resolveType();
        setValue();
    }

    /////////////////////////////////////////////
    // Return value methods
    /////////////////////////////////////////////
    public byte getIndex()
    {
        byte out = (byte) (this.index + 0x1);
        return out;
    }

    @Override
    public int getIntIndex()
    {
        return this.index + 1;
    }

    @Override
    public int[] getEvoInt()
    {
        int[] index = new int[evo.length];
        for (int i = 0; i < evo.length; i++)
        {
            index[i] = byteToValue(evo[i][evo[i].length - 1]);
        }
        return index;
    }

    public int getOffset1() // offset to first set of data (base stats)
    {
        return convertIndexToOffset(this.index);
    }

    public static int convertIndexToOffset(int n)
    {
        return OFFSET_POKEMON_1 + 0x20 * n; // offset to first set of data (base stats)
    }

    @Override
    public byte[] getPreEvo()
    {
        return preEvo;
    }

    @Override
    public int[] getPreEvoInt()
    {
        return byteToValue(preEvo);
    }

    /////////////////////////////////////////////
    // Set value methods
    /////////////////////////////////////////////	
    private void convertCompatibilities()
    {
        for (int i = 0; i < tmhmComp.length; i++)
        {
            byte thisBit = getBit(tmhmByte[(int) floor(i / 8)], i % 8);
            tmhmComp[i] = (thisBit != 0);
        }
    }

    public void setCompatibility(int n, boolean isCompatible)
    {
        this.tmhmComp[n] = isCompatible;
    }
}
