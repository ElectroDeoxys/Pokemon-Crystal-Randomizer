package data;

import static data.Constants.*;

public class Move
{
	private int index; // index of move
	private byte animIndex; // index of animation
	private byte[] effect; // [effect index, chance]
	private byte basePower;
	private byte typeCat;
	private byte acc;
	private byte pP;
	private int tier = 0;
	
	private int calcPower = 0; // calculated power
	
	public Move(int index, byte animIndex, byte[] effect, byte basePower, byte typeCat, byte acc, byte pP)
	{
		this.index = index;
		
		this.animIndex = animIndex;
		this.effect = effect; // [effect index, chance]
		this.basePower = basePower;
		this.typeCat = typeCat;
		this.acc = acc;
		this.pP = pP;
	}
	
	/////////////////////////////////////////////
	// Return value methods
	/////////////////////////////////////////////
	
	public byte getIndex()
	{
		byte out = (byte) (this.index + 0x1);
		return out;
	}
	
	public byte getAnimIndex()
	{
		return animIndex;
	}
	
	public byte[] getEffect()
	{
		return effect;
	}

	public byte getBasePower()
	{
		return basePower;
	}
	
	public byte getType()
	{
		return (byte) (typeCat & 0b00111111);
	}
	
	public byte getCat()
	{
		return (byte) (typeCat & 0b11000000);
	}
	
	public byte getTypeCat()
	{
		return typeCat;
	}
	
	public byte getAcc()
	{
		return acc;
	}

	public byte getPP()
	{
		return pP;
	}
	
	public int getOffset()
	{
		return convertIndexToOffset(this.index);
	}
	
	public int getCalcPower()
	{
		return this.calcPower;
	}
	
	public int getTier()
	{
		return this.tier;
	}
	
	public boolean isFixed()
	{
		// return true if it is a fixed power move
		return (effect[0] == (byte) 0x29 || effect[0] == (byte) 0x94);
	}
	
	/////////////////////////////////////////////
	// Set value methods
	/////////////////////////////////////////////	
	
	public void setBasePower(byte basePower)
	{
		this.basePower = basePower;
	}
	
	public void setPP(byte pP)
	{
		this.pP = pP;
	}
	
	public void setAcc(byte acc)
	{
		this.acc = acc;
	}
	
	public void setEffect(byte eff)
	{
		this.effect[0] = eff;
	}
	
	public void setEffectChance(byte effC)
	{
		this.effect[1] = effC;
	}
	
	public void setTypeCat(byte typeCat)
	{
		this.typeCat = typeCat;
	}
	
	public void setCalcPower(int calcPower)
	{
		this.calcPower = calcPower;
	}
	
	public void setTier(int tier)
	{
		this.tier = tier;
	}	
	
	public static int convertIndexToOffset(int n)
	{
		return OFFSET_MOVES + 0x07 * n;
	}
}