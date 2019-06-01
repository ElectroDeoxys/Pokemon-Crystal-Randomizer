package engine;

import static data.Constants.*;
import data.Pokemon;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.Arrays;

public class PokemonTierList<T extends Pokemon>
{
    private ArrayList<ArrayList<T>> tierList;
    private int bot;
    private int top;
    private int nTiers;
    
    public PokemonTierList(T[] itemList, int bot, int top, int nTiers)
    {
        this(new ArrayList<T>(Arrays.asList(itemList)), bot, top, nTiers);
    }
    
    public PokemonTierList(ArrayList<T> itemList, int bot, int top, int nTiers)
    {
        this.bot = bot;
        this.top = top;
        this.nTiers = nTiers;
        sort(itemList);
    }
     
    private void sort(ArrayList<T> itemList)
    {
        tierList = new ArrayList<>();
        
        for (int i = 0; i < nTiers; i++)
            tierList.add(new ArrayList<>());
        
        for (T item : itemList) insert(item);
    }

    private void insert(T item)
    {
        int val = item.getValue();
        int tier = (int) floor((val - bot) / (float) (top - bot) * nTiers);
        tier = max(min(tier, nTiers - 1), 0);
        tierList.get(tier).add(item);
    }
    
    public ArrayList<T> getTier(int n)
    {
        return tierList.get(n);
    }
    
    public ArrayList<ArrayList<T>> getTierList()
    {
        return tierList;
    }
    
    T getSameTier(int tier, boolean noLeg, boolean onlyEvolved)
    {
        ArrayList<T> curList = fillList(tier, noLeg, onlyEvolved);
        int expansion = 1;
        
        while (curList.isEmpty() && expansion < nTiers)
        {
            expand(curList, expansion, tier, noLeg, onlyEvolved);
            expansion++;
        }
        
        return randomElement(curList);
    }

    T getSameTier(int tier, boolean noLeg, boolean onlyEvolved, boolean forcedMix, ArrayList<T> prevMon)
    {
        ArrayList<T> curList = fillList(tier, noLeg, onlyEvolved, forcedMix, prevMon);
        int expansion = 1;
        
        while (curList.isEmpty() && expansion < nTiers)
        {
            expand(curList, expansion, tier, noLeg, onlyEvolved, forcedMix, prevMon);
            expansion++;
        }
        
        return randomElement(curList);
    }
    
    private ArrayList<T> fillList(int tier, boolean noLeg, boolean onlyEvolved)
    {
        ArrayList<T> list = new ArrayList<>();
        
        if (tier >= nTiers)
            nTiers = nTiers;
        
        for (T mon : tierList.get(tier))
        {
            if (   (noLeg && mon.isLegendary())
                || (onlyEvolved && !mon.hasPre()))
                continue;
            
            list.add(mon);
        }
        
        return list;
    }    
    
    private ArrayList<T> fillList(int tier, boolean noLeg, boolean onlyEvolved, boolean forcedMix, ArrayList<T> prevMon)
    {
        ArrayList<T> list = new ArrayList<>();
        
        for (T mon : tierList.get(tier))
        {
            if (   (noLeg && mon.isLegendary())
                || (onlyEvolved && !mon.hasPre())
                || (forcedMix && isTypeRedundant(mon, prevMon)))
                continue;
            
            list.add(mon);
        }
        
        return list;
    }
    
    private boolean isTypeRedundant(T mon, ArrayList<T> monList)
    {
        // decides if there is type redundancy in a list of T
        // will return true only if both T's types are repeated
        
        ArrayList<T> monListAll = new ArrayList(monList);
        monListAll.add(mon);

        boolean out = false; // assume it's not repeated

        Type[][] types = new Type[monListAll.size()][2]; // two types per T

        for (int i = 0; i < monListAll.size(); i++) // cycle T list
            types[i] = monListAll.get(i).getTypes();

        for (int i = 0; i < monListAll.size(); i++) // cycle T list
        {
            boolean test1 = false;
            boolean test2 = false;

            for (int j = 0; j < monListAll.size(); j++) // first type
            {
                if (i == j) continue; // skip comparing with self
                test1 = (types[i][0] == types[j][0]) || (types[i][0] == types[j][1]); // compare first type
                if (test1) break; // exit loop if repetition found
            }

            for (int j = 0; j < monListAll.size(); j++) // second type
            {
                if (i == j) continue; // skip comparing with self
                test2 = (types[i][1] == types[j][0]) || (types[i][1] == types[j][1]); // compare second type
                if (test2) break; // exit loop if repetition found
            }

            out = (test1) && (test2);// only get a true result if both tests pass
            if (out) break;
        }

        return out;
    }
    
    private void expand(ArrayList<T> curList, int expansion, int tier, boolean noLeg, boolean onlyEvolved)
    {
        if (tier + expansion < nTiers-1)
            curList.addAll(fillList(tier + expansion, noLeg, onlyEvolved));
        if (tier - expansion >= 0)
            curList.addAll(fillList(tier - expansion, noLeg, onlyEvolved));
    }

    private void expand(ArrayList<T> curList, int expansion, int tier, boolean noLeg, boolean onlyEvolved, boolean forcedMix, ArrayList<T> prevMon)
    {
        if (tier + expansion < nTiers-1)
            curList.addAll(fillList(tier + expansion, noLeg, onlyEvolved, forcedMix, prevMon));
        if (tier - expansion >= 0)
            curList.addAll(fillList(tier - expansion, noLeg, onlyEvolved, forcedMix, prevMon));
    }
}
