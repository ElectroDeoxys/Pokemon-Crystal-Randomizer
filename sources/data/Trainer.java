package data;

import java.util.ArrayList;
import static data.Constants.*;

public class Trainer
{
    private int index; // index of trainer
    private int offset; // offset in ROM
    private byte[] name; // name of trainer
    private Kind trnKind; // whether the trainer has custom moves/items
    private byte statExp; // stat exp byte
    private byte[][] party = null; // [Party slot][Level, Species, Item, Moves]

    public Trainer(int index, int offset, byte[] name, byte kindByte, byte statExp, byte[][] party)
    {
        this.index = index;
        this.offset = offset;
        this.name = name;
        this.trnKind = kindFromByte(kindByte);
        this.statExp = statExp;
        this.party = party;
    }

    public enum Kind
    {
        NONE(0),
        WMOVES(1),
        WITEMS(2),
        WMOVESITEMS(3);

        private final int index;

        private Kind(int index)
        {
            this.index = index;
        }

        public byte getByte()
        {
            return valueToByte(index);
        }
    }

    private Kind kindFromByte(byte kindByte)
    {
        switch (kindByte)
        {
            case (byte) 1:
                return Kind.WMOVES;
            case (byte) 2:
                return Kind.WITEMS;
            case (byte) 3:
                return Kind.WMOVESITEMS;
            case (byte) 0:
            default:
                return Kind.NONE;
        }
    }

    public int getTotalSize()
    {
        int size = 0;
        size += name.length + 3; // bytes for the name, statExp, and trnKind
        size += party.length * bytesPerPoke();

        return size;
    }

    public int getIndex()
    {
        return this.index;
    }

    public int getOffset()
    {
        return this.offset;
    }

    public byte[] getName()
    {
        return this.name;
    }

    public byte getStatExp()
    {
        return this.statExp;
    }

    public Kind getKind()
    {
        return this.trnKind;
    }

    public int getPartySize()
    {
        return this.party.length;
    }

    public byte getPokeByte(int partyPos)
    {
        return this.party[partyPos][1];
    }

    public byte[] getPokeBytes(int partyPos)
    {
        return this.party[partyPos];
    }

    public byte getLvl(int partyPos)
    {
        return this.party[partyPos][0];
    }
    
    public int getMaxLvl()
    {
        int maxLvl = 0;
        
        for (byte[] partySlot : party)
        {
            int curLvl = byteToValue(partySlot[0]);
            maxLvl = (maxLvl < curLvl) ? curLvl : maxLvl;
        }
        
        return maxLvl;
    }
    
    public boolean hasMoves()
    {
        return (this.trnKind == Kind.WMOVES || this.trnKind == Kind.WMOVESITEMS);
    }
    
    public boolean hasItems()
    {
        return (this.trnKind == Kind.WITEMS || this.trnKind == Kind.WMOVESITEMS);
    }

    private int bytesPerPoke()
    {
        int res = 0;

        switch (this.trnKind)
        {
            case NONE:
                res = 2;
                break;
            case WMOVES:
                res = 6;
                break;
            case WITEMS:
                res = 3;
                break;
            case WMOVESITEMS:
                res = 7;
                break;
        }

        return res;
    }

    public void setStatExp(byte statExp)
    {
        this.statExp = statExp;
    }

    public void setPoke(int partyPos, int pokeIndex)
    {
        this.party[partyPos][1] = valueToByte(pokeIndex);
    }

    public void setLvl(int partyPos, byte lvl)
    {
        this.party[partyPos][0] = lvl;
    }

    public void setMoves(int partyPos, byte[] newMoves)
    {
        if (trnKind == Kind.WMOVES)
        {
            System.arraycopy(newMoves, 0, this.party[partyPos], 2, newMoves.length);
        }
        else if (trnKind == Kind.WMOVESITEMS)
        {
            System.arraycopy(newMoves, 0, this.party[partyPos], 3, newMoves.length);
        }
    }

    public void setMoves(int partyPos, ArrayList<Move> newMoves)
    {
        if (trnKind == Kind.WMOVES)
        {
            for (int i = 0; i < newMoves.size(); i++)
            {
                this.party[partyPos][2 + i] = newMoves.get(i).getIndex();
            }
        }
        else if (trnKind == Kind.WMOVESITEMS)
        {
            for (int i = 0; i < newMoves.size(); i++)
            {
                this.party[partyPos][3 + i] = newMoves.get(i).getIndex();
            }
        }
    }

    public void setParty(byte[][] party)
    {
        this.party = party;
    }

    public void removeCustMoves()
    {
        if (trnKind == Kind.WMOVES)
        {
            byte[][] newParty = new byte[party.length][2];
            for (int i = 0; i < party.length; i++)
            {
                System.arraycopy(party[i], 0, newParty[i], 0, 2);
            }
            setParty(newParty);
            trnKind = Kind.NONE;
        }
        else if (trnKind == Kind.WMOVESITEMS)
        {
            byte[][] newParty = new byte[party.length][3];
            for (int i = 0; i < party.length; i++)
            {
                System.arraycopy(party[i], 0, newParty[i], 0, 2);
                newParty[i][2] = party[i][3];
            }
            setParty(newParty);
            trnKind = Kind.WITEMS;
        }
    }
    
    public void addCustMoves()
    {
        if (trnKind == Kind.NONE)
        {
            byte[][] newParty = new byte[party.length][6];
            for (int i = 0; i < party.length; i++)
            {
                System.arraycopy(party[i], 0, newParty[i], 0, 2);
                for (int j = 0; j < 4; j++)
                    newParty[i][2+j] = valueToByte(0); // placeholder move
            }
            setParty(newParty);
            trnKind = Kind.WMOVES;
        }
        else if (trnKind == Kind.WITEMS)
        {
            byte[][] newParty = new byte[party.length][7];
            for (int i = 0; i < party.length; i++)
            {
                System.arraycopy(party[i], 0, newParty[i], 0, 3);
                for (int j = 0; j < 4; j++)
                    newParty[i][3+j] = valueToByte(0); // placeholder move
            }
            setParty(newParty);
            trnKind = Kind.WMOVESITEMS;
        }
    }
}
