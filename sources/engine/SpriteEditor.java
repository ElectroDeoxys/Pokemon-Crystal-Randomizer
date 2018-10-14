package engine;

import java.util.ArrayList;
import java.io.IOException;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Sprite;

class SpriteEditor
{
	private Sprite[][] sprites = new Sprite[2][]; // [regular sprites, Unown sprites][sprite index]
	private Sprite[] spritesTrn;
	private Sprite spriteEgg;
	private byte[][][] pal; // palettes [Pokemon index][regular, shiny]
	
	SpriteEditor(RomReader romReader) throws IOException
	{
		this.sprites[0] = romReader.readRomSprites(false);
		this.sprites[1] = romReader.readRomSprites(true);
		this.spritesTrn = romReader.readRomTrainerSprites();
		this.spriteEgg = romReader.readRomEggSprite();
		this.pal = romReader.readRomPalettes();
	}
	
	SpriteEditor(PokedexRandomizer dexRand, RomReader romReader) throws IOException
	{
		this.sprites = dexRand.getAllSprites();
		this.spritesTrn = romReader.readRomTrainerSprites();
		this.spriteEgg = romReader.readRomEggSprite();
		this.pal = dexRand.getAllPalettes();
	}
	
	void minOffset()
	{
		int[] min = sprites[0][0].getOffset();
		
		for (int i = 0; i < sprites.length; i++) // for each sprite group (regular/Unown)
			for (int j = 0; j < sprites[i].length; j++) // for each index
			{
				int[] off = sprites[i][j].getOffset();
				
				if (off[0] < min[0])
					min[0] = off[0];
				if (off[1] < min[1])
					min[1] = off[1];
			}

		System.out.println(String.format("0x%06X", min[0]));
		System.out.println(String.format("0x%06X", min[1]));
	}
	
	void maxOffset()
	{
		int[] max = sprites[0][0].getOffset();
		
		for (int i = 0; i < sprites.length; i++)
			for (int j = 0; j < sprites[i].length; j++) // for each index
			{
				if (i == 0 && j == 201 - 1)
					continue; // skip Unown
				
				int[] off = sprites[i][j].getOffset();
				
				if (off[0] > max[0])
					max[0] = off[0];
				if (off[1] > max[1])
					max[1] = off[1];
			}

		System.out.println(String.format("0x%06X", max[0]));
		System.out.println(String.format("0x%06X", max[1]));
	}
	
	Sprite[][] getAllSprites()
	{
		return this.sprites;
	}
	
	Sprite[] getAllTrainerSprites()
	{
		return this.spritesTrn;
	}
	
	Sprite getEggSprite()
	{
		return this.spriteEgg;
	}
	
	byte[][][] getAllPalettes()
	{
		return this.pal;
	}
	
	void packSprites()
	{
		// packs the sprites in the available banks
		// there is space after the pointer table of regular sprites 
		// and right after the Unown pointer table and trainer tables
		// until the end of the 0x164000 bank (0x167FFF)
		
		int pos = OFFSET_SPRITES; // keep track of position
		int minSize = 0xFFFF; // record the minimum size of all sprites
		int totalSpr = (sprites[0].length + sprites[1].length) * 2 + spritesTrn.length + 1; // add up all the sprites
		ArrayList<Byte[]> sprList = new ArrayList<Byte[]>(); // initialize the sprite array list
		ArrayList<Integer> indexList = new ArrayList<Integer>(); // store the indexes of each sprite
		// indexes transition linearly from sprites[0] to sprites[1]
		ArrayList<Boolean> isFrontList = new ArrayList<Boolean>(); // stores booleans of whether it's front or back sprite
		
		// get all the sprites in the list
		
		// Pokemon sprites
		for (int i = 0; i < sprites.length; i++)
			for (int j = 0; j < sprites[i].length; j++)
			{
				byte[] sprHold = sprites[i][j].getFront();
				sprList.add(convertByteArray(sprHold));
				indexList.add(i * sprites[0].length + j);
				isFrontList.add(true);
				if ((sprHold.length < minSize) || (sprHold.length > 0)) minSize = sprHold.length;
				
				sprHold = sprites[i][j].getBack();
				sprList.add(convertByteArray(sprHold));
				indexList.add(i * sprites[0].length + j);
				isFrontList.add(false);
				if ((sprHold.length < minSize) || (sprHold.length > 0)) minSize = sprHold.length;
			}
		
		// Trainer sprites
		for (int i = 0; i < spritesTrn.length; i++)
		{
			byte[] sprHold = spritesTrn[i].getFront();
			sprList.add(convertByteArray(sprHold));
			indexList.add(sprites[0].length + sprites[1].length + i);
			isFrontList.add(true);
			if (sprHold.length < minSize) minSize = sprHold.length;
		}
		
		// Egg sprite
		for (int i = 0; i < 1; i++)
		{
			byte[] sprHold = spriteEgg.getFront();
			sprList.add(convertByteArray(sprHold));
			indexList.add(sprites[0].length + sprites[1].length + spritesTrn.length);
			isFrontList.add(true);
			if (sprHold.length < minSize) minSize = sprHold.length;
		}
		
		int count = 0;
		while (sprList.size() > 0) // while there are sprites to add to the banks
		{
			byte[] thisSpr = convertByteArray(sprList.get(0)); // get first sprite on the list
			int thisIndex = indexList.get(0).intValue(); // get associated sprite index
			boolean isFront = isFrontList.get(0).booleanValue();
			
			int[] metaData = getMetaData(thisIndex);
			int offsetIndex = (isFront) ? 0 : 1;
			
			if (floor(pos / 0x4000) == floor((pos + thisSpr.length) / 0x4000)) // if sprite length doesn't change banks
			{
				switch (metaData[0])
				{
					case 0: 
					case 1:
						sprites[metaData[0]][metaData[1]].setOffset(offsetIndex, pos);
						break;
					case 2:
						spritesTrn[metaData[1]].setOffset(offsetIndex, pos);
						break;
					case 3:
						spriteEgg.setOffset(offsetIndex, pos);
						break;
					default: break;
				}
			}
			else // it is too big for the current bank
			{
				int gap = 0x4000 - (pos % 0x4000);
				
				if (gap >= minSize) // there may be another later sprite that fills the bank
				{
					// look into later sprites to see the one that fits leaving the least amount of space
					
					int minGap = 0xFFFF; // keep track of the minimum gap 
					int minGapIndex = 0;
					boolean found = false; // whether any candidates were found at all
					
					for (int i = 1; i < sprList.size(); i++) // cycle all later sprites
					{
						byte[] gapSpr = convertByteArray(sprList.get(i));
						if (gapSpr.length > gap) continue; // skip big sprites
						
						if (gapSpr.length == gap) // if it's exactly the gap size
						{
							minGapIndex = i;
							found = true;
							break; // no need to look for a new one
						}
						else if (gap - gapSpr.length < minGap)
						{
							minGap = gap - gapSpr.length;
							minGapIndex = i;
							found = true;
						}
					}
					
					if (found) // if any were found, place it in the bank
					{
						byte[] gapSpr = convertByteArray(sprList.get(minGapIndex)); // get sprite from the list
						int gapIndex = indexList.get(minGapIndex).intValue(); // get associated sprite index
						boolean gapIsFront = isFrontList.get(minGapIndex).booleanValue();
						
						int[] gapMetaData = getMetaData(gapIndex);
						int gapOffsetIndex = (gapIsFront) ? 0 : 1;
						
						switch (gapMetaData[0]) // place sprite
						{
							case 0: 
							case 1:
								sprites[gapMetaData[0]][gapMetaData[1]].setOffset(gapOffsetIndex, pos);
								break;
							case 2:
								spritesTrn[gapMetaData[1]].setOffset(gapOffsetIndex, pos);
								break;
							case 3:
								spriteEgg.setOffset(gapOffsetIndex, pos);
								break;
							default: break;
						}
						
						// remove the sprite from the lists
						sprList.remove(minGapIndex);
						indexList.remove(minGapIndex);
						isFrontList.remove(minGapIndex);
						
						pos += gapSpr.length; // adjust position
					}
				}
				
				pos = (int) floor((pos + thisSpr.length) / 0x4000) * 0x4000; // adjust position for the next bank
				if (pos == OFFSET_SPRITE_POINTERS_U)
					pos += 6 * N_UNOWN; // don't overwrite Unown pointer table
				if (pos == OFFSET_TRAINER_SPRITE_POINTERS)
					pos += 3 * N_TRAINER_SPRITES; // don't overwrite trainer pointer table
				
				switch (metaData[0])
				{
					case 0: 
					case 1:
						sprites[metaData[0]][metaData[1]].setOffset(offsetIndex, pos);
						break;
					case 2:
						spritesTrn[metaData[1]].setOffset(offsetIndex, pos);
						break;
					case 3:
						spriteEgg.setOffset(offsetIndex, pos);
						break;
					default: break;
				}
			}
			
			// remove the sprite from the lists
			sprList.remove(0);
			indexList.remove(0);
			isFrontList.remove(0);
			
			pos += thisSpr.length; // adjust position for next sprite
			//if (count == 0) printOffsets();
			count++;
		}
	}
	
	private int[] getMetaData(int index)
	{
		int[] out = new int[2]; // sprite group and true index
		
		if (index < sprites[0].length + sprites[1].length) // for Pokemon
		{
			out[0] = (index < sprites[0].length) ? 0 : 1; // whether it's regular or Unown sprites
			out[1] = index % sprites[0].length; // translate it to the true index
		}
		else if (index < sprites[0].length + sprites[1].length + spritesTrn.length) // for trainers
		{
			out[0] = 2;
			out[1] = index - (sprites[0].length + sprites[1].length);
		}
		else // egg
		{
			out[0] = 3;
			out[1] = -1;
		}
		
		return out;
	}
	
	void printOffsets()
	{
		for (int i = 0; i < sprites[0].length; i++)
		{
			int[] pos = sprites[0][i].getOffset();
			System.out.println(i + ":" + pos[0] + " / " + pos[1]);
		}
	}
}