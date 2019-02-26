package engine;

import static java.lang.Math.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

import static data.Constants.*;
import static engine.RomReader.*;
import static engine.RomWriter.*;

class RomPatcher
{
	private FileChannel ch = null;
	private FileChannel chSrc = null;
	
	RomPatcher(FileChannel ch, FileChannel chSrc)
	{
		this.ch = ch;
		this.chSrc = chSrc;
	}
	
	void updateTypeEnhanceItems() throws IOException
	{
		// edited bank: 0x01
		// original game has type-enhancing items that increase attack power by 10%
		// this patch increases it to 20%
		
		for (int i = 0; i < OFFSET_TYPE_ENHANCING_ITEMS.length; i++)
			writeToRom(ch, (byte) 0x14, OFFSET_TYPE_ENHANCING_ITEMS[i]); // 20% boost
	}	
	
	void updateHeldItemRates() throws IOException
	{
		// edited bank: 0x0F
		// original game has 23% and 2% for the wild held item rates
		// this patch increases the likelihood to 50% and 5% respectively
		
		// 55% percent that it holds an item
		writeToRom(ch, (byte) 0x8D, OFFSET_WILD_ITEM_RATE[0]);
		// 9% percent that it holds the rarer item (0.55*0.09 ~= 0.05)
		writeToRom(ch, (byte) 0x16, OFFSET_WILD_ITEM_RATE[1]);
	}
}