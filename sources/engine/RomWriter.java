package engine;

import java.util.ArrayList;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

import static data.Constants.*;
import data.Route;
import data.Trainer;
import data.Pokemon;
import data.Move;
import data.Sprite;

class RomWriter
{
	private FileChannel ch = null;
	
	RomWriter(FileChannel ch)
	{
		this.ch = ch;
	}
	
	static void writeToRom(FileChannel ch, ByteBuffer buffer, int pos) throws IOException
	{
		buffer.flip();
		ch.write(buffer, pos);
		buffer.rewind();
		buffer.clear();
	}
	
	void randomizeStarters(PokemonSorter monSorter, int starterKind) throws IOException
	{
		byte[] str = monSorter.getRandomStarters(starterKind); // array with the new starters
		
		ByteBuffer buffer = ByteBuffer.allocate(1);
		
		for (int i = 0; i < 3; i ++)
			for (int pos : OFFSET_STARTERS[i])
			{
				buffer.put(str[i]);
				writeToRom(ch, buffer, pos);
			}
	}

	void replaceRoutePokemon(Route route) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(route.getTotalSlots() * 2);
		
		for (int i = 0; i < route.getTotalSlots(); i++)
		{
			buffer.put(route.getLvl(i));
			buffer.put(route.getPokeByte(i));
		}
		
		writeToRom(ch, buffer, route.getOffset());
	}
	
	void replaceAllRoutePokemon(Route[] routes) throws IOException
	{
		for (int i = 0; i <  Route.indexBreaks[Route.indexBreaks.length-1]; i++)
			replaceRoutePokemon(routes[i]);
	}	
	
	void replaceTrainer(Trainer trainer, int size, int pos) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(size);
		
		buffer.put(trainer.getName());
		buffer.put((byte) 0x50); // name terminator
		buffer.put(trainer.getKind());
		buffer.put(trainer.getStatExp());
		
		for (int i = 0; i < trainer.getPartySize(); i++)
			buffer.put(trainer.getPokeBytes(i));
		
		writeToRom(ch, buffer, pos);
		//System.out.println(new String(convertBytesToText(trainer.getName())) + ": " + byteToValue(trainer.getStatExp()));
	}
	
	void replaceAllTrainers(Trainer[] trainers) throws IOException
	{
		int pos = OFFSET_TRAINERS;
		int ptrPos = OFFSET_TRAINERS_POINTERS;
		
		for (int i = 0; i <  TRAINER_GROUPS.length; i++) // cycle trainer groups
		{
			// update pointer table
			
			byte[] pointer = getPointer(pos);
			ByteBuffer bufferPtr = ByteBuffer.allocate(2); 
			bufferPtr.put(pointer);
			writeToRom(ch, bufferPtr, ptrPos);
			
			ptrPos += 2;
			
			if (i == 9) continue; // unused trainer group
			
			int next = (i == TRAINER_GROUPS.length - 1) ? N_TRAINERS : TRAINER_GROUPS[i+1] ; // protect against undefined array
			
			for (int j = TRAINER_GROUPS[i]; j < next; j++) // cycle indices of this trainer group
			{
				int size = trainers[j].getTotalSize();
				replaceTrainer(trainers[j], size, pos);
				pos += size;
				
				ByteBuffer bufferB = ByteBuffer.allocate(1);
				byte[] term = {(byte) 0xFF};
				bufferB.put(term);
				writeToRom(ch, bufferB, pos);
				
				pos++;
			}
		}
		

	}	
	
	void replaceAllPokemon(Pokemon[] mons) throws IOException
	{
		int pos1 = OFFSET_POKEMON_2; // keep track of positions evo/moves
		int pos2 = OFFSET_POKEMON_3; // keep track of position egg moves
		
		for (int i = 0; i <  N_POKEMON; i++)
		{
			/////////////////////////////////////
			// first set of data at OFFSET_POKEMON_1
			// covering base stats, types, and other misc data
			/////////////////////////////////////
			
			ByteBuffer buffer1 = ByteBuffer.allocate(0x20);
			
			byte[] misc = mons[i].getMisc();
			byte[] padding = {(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0};
			
			buffer1.put(mons[i].getIndex());
			buffer1.put(mons[i].getBase());
			buffer1.put(mons[i].getTypes());
			buffer1.put(misc[0]);
			buffer1.put(misc[1]);
			buffer1.put(misc[2]);
			buffer1.put(misc[3]);
			buffer1.put(misc[4]);
			buffer1.put((byte) 0x64); // padding byte
			buffer1.put(misc[5]);
			buffer1.put((byte) 0x05); // padding byte
			buffer1.put(mons[i].getGfx());
			buffer1.put(padding); // padding
			buffer1.put(misc[6]);
			buffer1.put(misc[7]);
			buffer1.put(mons[i].getCompatibilitiesByte());
			
			writeToRom(ch, buffer1, mons[i].getOffset1());
			
			/////////////////////////////////////
			// second set of data at OFFSET_POKEMON_2
			// covering evolution and level-up move data
			/////////////////////////////////////
			
			ByteBuffer buffer2 = ByteBuffer.allocate(mons[i].getNBytes() + 2); // plus 2 terminator bytes
			
			byte[][] evo = mons[i].getEvos();
			byte[][] move = mons[i].getMoves();
			
			if (mons[i].hasEvos())
				for (int j = 0; j < evo.length; j++)
					buffer2.put(evo[j]);
				
			buffer2.put((byte) 0x00); // evolution terminator
			
			for (int j = 0; j < move.length; j++)
				buffer2.put(move[j]);		
			
			buffer2.put((byte) 0x00); // move terminator
			
			writeToRom(ch, buffer2, pos1);
			
			byte[] pointer = getPointer(pos1);
			
			// update pointer table
			
			ByteBuffer buffer3 = ByteBuffer.allocate(2); 
			buffer3.put(pointer);
			writeToRom(ch, buffer3, OFFSET_POINTERS_1 + 2*i);
			
			pos1 += mons[i].getNBytes() + 2; // set position for the next entry
			
			/////////////////////////////////////
			// third set of data at OFFSET_POKEMON_3
			// covering egg move data
			/////////////////////////////////////
			
			byte[] eggMoves = mons[i].getEggMoves();
			
			if (eggMoves.length > 0) // if it has egg moves
			{
				ByteBuffer buffer4 = ByteBuffer.allocate(eggMoves.length + 1); // plus a terminator byte
					
				buffer4.put(eggMoves);
				buffer4.put((byte) 0xFF); // terminator
				
				writeToRom(ch, buffer4, pos2);
				pointer = getPointer(pos2);

				// update pointer table
				
				ByteBuffer buffer5 = ByteBuffer.allocate(2);
				
				buffer5.put(pointer);
				writeToRom(ch, buffer5, OFFSET_POINTERS_2 + 2*i);
				
				pos2 += eggMoves.length + 1; // set position for the next entry
			}
			
			/////////////////////////////////////
			// name
			/////////////////////////////////////
			
			byte[] nameWrite = new byte[NAME_LEN];
			byte[] name = mons[i].getName();
			
			for (int j = 0; j < name.length; j++) // fill with the name
				nameWrite[j] = name[j];
				
			for (int j = name.length; j < nameWrite.length; j++) // fill with terminating bytes
				nameWrite[j] = (byte) 0x50;
			
			ByteBuffer bufferName = ByteBuffer.allocate(nameWrite.length);
			bufferName.put(nameWrite);
			
			writeToRom(ch, bufferName, OFFSET_POKEMON_NAMES + 10 * i);
			
			/////////////////////////////////////
			// icon
			/////////////////////////////////////
			
			ByteBuffer bufferMisc = ByteBuffer.allocate(1);
			bufferMisc.put(mons[i].getIcon());
			
			writeToRom(ch, bufferMisc, OFFSET_POKEMON_ICONS + i);			
		}
		
		// Pokemon with no egg moves need an 0xFF byte terminator at the end of the egg move list to point to
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put((byte) 0xFF);
		writeToRom(ch, buffer, pos2); // pos2 is already the ending offset
		
		for (int i = 0; i <  N_POKEMON; i++)
		{
			byte[] eggMoves = mons[i].getEggMoves();
			
			if (eggMoves.length > 0) // if it has egg moves
				continue; // skip
			
			ByteBuffer buffer1 = ByteBuffer.allocate(2);
			buffer1.put(getPointer(pos2));
			writeToRom(ch, buffer1, OFFSET_POINTERS_2 + 2*i);
		}
	}
	
	void replaceMove(Move move) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(7);

		byte[] effect = move.getEffect();
		
		buffer.put(move.getIndex());
		buffer.put(effect[0]);
		buffer.put(move.getBasePower());
		buffer.put(move.getTypeCat());
		buffer.put(move.getAcc());
		buffer.put(move.getPP());
		buffer.put(effect[1]);
		
		writeToRom(ch, buffer, move.getOffset());
	}
	
	void replaceLearnableMove(Move move, int n) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put(move.getIndex());
		
		writeToRom(ch, buffer, OFFSET_TM_MOVES + n);
	}
	
	void replaceAllMoves(Move[] moves, Move[] movesTM) throws IOException
	{
		for (int i = 0; i <  N_MOVES; i++)
			replaceMove(moves[i]);
		
		for (int i = 0; i <  N_TM + N_HM + N_MOVE_TUTOR; i++)
			replaceLearnableMove(movesTM[i], i);
	}
	
	void replaceSprite(Sprite sprite, int n, boolean isUnown) throws IOException
	{
		int[] pos = sprite.getOffset();
		byte[] front = sprite.getFront();
		byte[] back = sprite.getBack();
		
		ByteBuffer buffer1 = ByteBuffer.allocate(front.length);
		buffer1.put(front);
		writeToRom(ch, buffer1, pos[0]);
		
		ByteBuffer buffer2 = ByteBuffer.allocate(back.length);
		buffer2.put(back);
		writeToRom(ch, buffer2, pos[1]);
		
		// update the sprite dimensions
		ByteBuffer bufferDim = ByteBuffer.allocate(1);
		bufferDim.put(sprite.getDim());
		if (isUnown)
			writeToRom(ch, bufferDim, OFFSET_POKEMON_1 + 0x11 + (0x20 * (INDEX_UNOWN - 1)));
		else
			writeToRom(ch, bufferDim, OFFSET_POKEMON_1 + 0x11 + (0x20 * n));
		
		// update pointer
		if (!(n == (INDEX_UNOWN - 1) && !isUnown))
		{
			byte[][] ptrs = sprite.getSpritePointer();
			ByteBuffer bufferPtr = ByteBuffer.allocate(3);
			bufferPtr.put(ptrs[0]);
			
			if (isUnown) // if it's the Unown slot
				writeToRom(ch, bufferPtr, OFFSET_SPRITE_POINTERS_U + 6 * n);
			else
				writeToRom(ch, bufferPtr, OFFSET_SPRITE_POINTERS + 6 * n);
			
			bufferPtr.put(ptrs[1]);
			
			if (isUnown) // if it's the Unown slot
				writeToRom(ch, bufferPtr, OFFSET_SPRITE_POINTERS_U + 6 * n + 3);
			else
				writeToRom(ch, bufferPtr, OFFSET_SPRITE_POINTERS + 6 * n + 3);
		}
		
		if (n == (INDEX_UNOWN - 1) && !isUnown) // replace Unown's pointer with 0xFF
		{
			byte[] ptr = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
			ByteBuffer bufferPtr = ByteBuffer.allocate(6);
			bufferPtr.put(ptr);
			writeToRom(ch, bufferPtr, OFFSET_SPRITE_POINTERS + 6 * n);
		}
	}
	
	void replaceTrainerSprite(Sprite sprite, int n) throws IOException
	{
		int[] pos = sprite.getOffset();
		byte[] front = sprite.getFront();
		
		ByteBuffer buffer1 = ByteBuffer.allocate(front.length);
		buffer1.put(front);
		writeToRom(ch, buffer1, pos[0]);
		
		byte[][] ptrs = sprite.getSpritePointer();
		ByteBuffer bufferPtr = ByteBuffer.allocate(3);
		bufferPtr.put(ptrs[0]);
		
		writeToRom(ch, bufferPtr, OFFSET_TRAINER_SPRITE_POINTERS + 3 * n);
	}
	
	void replaceEggSprite(Sprite sprite) throws IOException
	{
		int[] pos = sprite.getOffset();
		byte[] front = sprite.getFront();
		
		ByteBuffer buffer1 = ByteBuffer.allocate(front.length);
		buffer1.put(front);
		writeToRom(ch, buffer1, pos[0]);
		
		byte[][] ptrs = sprite.getSpritePointer();
		ByteBuffer bufferPtr = ByteBuffer.allocate(3);
		bufferPtr.put(ptrs[0]);
		
		writeToRom(ch, bufferPtr, OFFSET_SPRITE_POINTER_EGG);
	}	
	
	void replaceAllSprites(Sprite[][] sprites, Sprite[] spritesTrn, Sprite spriteEgg, byte[][][] pal) throws IOException
	{
		// clear memory banks and fill it with 0x00
		for (int i = 0; i < N_SPRITE_BANKS; i++)
		{
			ByteBuffer bufferEmpty = ByteBuffer.allocate(0x4000);
			for (int j = 0; j < 0x4000; j++)
				bufferEmpty.put((byte) 0x00);
			
			writeToRom(ch, bufferEmpty, OFFSET_SPRITE_POINTERS + 0x4000 * i);
		}
		
		for (int i = 0; i < sprites.length; i++)
		{
			int len = (i==0) ? sprites[i].length : N_UNOWN; // check how many times to cycle
			
			for (int j = 0; j < len; j++)
			{
				int k = j % sprites[i].length; // ensure the cycle happens when reaches end of array
				replaceSprite(sprites[i][k], j, (i == 1));
			}
		}
		
		for (int i = 0; i <  spritesTrn.length; i++)
			replaceTrainerSprite(spritesTrn[i], i);
		
		// fill gap after pointer table
		byte[] fill = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
		ByteBuffer bufferFill = ByteBuffer.allocate(6);
		bufferFill.put(fill);
		writeToRom(ch, bufferFill, OFFSET_SPRITE_POINTERS + 6 * N_POKEMON);
		bufferFill.put(fill);
		writeToRom(ch, bufferFill, OFFSET_SPRITE_POINTERS + 6 * N_POKEMON + 6);
		
		replaceEggSprite(spriteEgg);
		
		// overwrite palettes
		
		for (int i = 0; i < pal.length; i++)
		{
			ByteBuffer bufferPal = ByteBuffer.allocate(8);
			bufferPal.put(pal[i][0]); // regular
			bufferPal.put(pal[i][1]); // shiny
			writeToRom(ch, bufferPal, OFFSET_PAL + i*8);
		}
	}
}