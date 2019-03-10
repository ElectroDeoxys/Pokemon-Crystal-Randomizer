package data;

import static data.Constants.*;

public class Trainer
{
	private int index; // index of trainer
	private int offset; // offset in ROM
	private byte[] name; // name of trainer
	private byte trnKind; // whether the trainer has custom moves/items
	private byte statExp; // stat exp byte
	private byte[][] party = null; // [Party slot][Level, Species, Item, Moves]
	
	public Trainer(int index, int offset, byte[] name, byte trnKind, byte statExp, byte[][] party)
	{
		this.index = index;
		this.offset = offset;
		this.name = name;
		this.trnKind = trnKind;
		this.statExp = statExp;
		this.party = party;
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
	
	public byte getKind()
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
	
	private int bytesPerPoke()
	{
		int res = 0;
		
		switch (this.trnKind)
		{
			case 0: res = 2; break;
			case 1: res = 6; break;
			case 2: res = 3; break;
			case 3: res = 7; break;
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
		if (trnKind == 1)
			for (int i = 0; i < newMoves.length; i++)
				this.party[partyPos][2+i] = newMoves[i];
		else if (trnKind == 3)
			for (int i = 0; i < newMoves.length; i++)
				this.party[partyPos][3+i] = newMoves[i];
	}
	
	public void setParty(byte[][] party)
	{
		this.party = party;
	}
}