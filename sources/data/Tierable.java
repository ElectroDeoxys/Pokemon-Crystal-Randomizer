package data;

public abstract class Tierable
{
    protected int tier = 0; // tier list to compare strengths
    protected int value;
    protected int oldTier = -1; // tier list to compare strengths replaced
    protected int oldTypeTier = -1; // tier list to compare strengths for same type from replaced
    
    public abstract void setValue();

    public int getTier()
    {
        return tier;
    }

    public void setTier(int tier)
    {
        this.tier = tier;
    }

    public int getValue()
    {
        return value;
    }
    
    public int getOldTier()
    {
        return this.oldTier;
    }

    public int getOldTypeTier()
    {
        return this.oldTypeTier;
    }
    
    abstract public int getTrueIndex();
}
