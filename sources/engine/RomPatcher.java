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
		
		ByteBuffer buff = ByteBuffer.allocate(1);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x69D1);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x69D8);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x69F4);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6A25);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6A56);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6A5D);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6A6B);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6A87);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6A95);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6AAA);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6AB1);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6AD4);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6AF0);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6B28);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6B83);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6BA6);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6BDE);
		buff.put((byte) 0x14); // 20% boost
		writeToRom(ch, buff, 0x6C63);
	}	
	
	void updateHeldItemRates() throws IOException
	{
		// edited bank: 0x0F
		// original game has 23% and 2% for the wild held item rates
		// this patch increases the likelihood to 50% and 5% respectively
		
		ByteBuffer buff = ByteBuffer.allocate(1);
		buff.put((byte) 0x8D); // 55% percent that it holds an item
		writeToRom(ch, buff, 0x3E933);
		buff.put((byte) 0x16); // 9% percent that it holds the rarer item (0.55*0.09 ~= 0.05)
		writeToRom(ch, buff, 0x3E93C);
	}
	
	void incrementTable(int pos, int length, byte[] ptrInit, int incr) throws IOException
	{
		// writes a pointer table that starts at ptrInit and increments by incr on each entry
		ByteBuffer bByte = ByteBuffer.allocate(1);
		
		byte[] ptrHold = ptrInit;
		for (int i = 0; i < length; i += 2)
		{
			bByte.put(ptrHold[0]);
			writeToRom(ch, bByte, pos + i);
			bByte.put(ptrHold[1]);
			writeToRom(ch, bByte, pos + i + 1);
			
			ptrHold = addToPointer(ptrHold, incr);
		}
	}
	
	byte[] addToPointer(byte[] ptr, int s)
	{
		// adds s to the position that ptr points to
		int pos = byteArrayToInt(ptr, false); // get the offset ptr points to
		pos += s;
		
		byte[] out = new byte[2];
		int x = (pos % (0xFFFF+1)); // take only last four digits
		
		out[0] = valueToByte(x % (0xFF+1)); // less significant byte are the two last bytes
		int y = (int) floor(x/(0xFF+1)); // more significant bytes are the first two
		
		out[1] = valueToByte(y);
		
		return out;
	}
}