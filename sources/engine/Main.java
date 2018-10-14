package engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.*;
import java.io.RandomAccessFile;
import java.io.IOException;

import static data.Constants.*;
import data.Route;
import data.Trainer;
import data.Pokemon;
import data.Move;
import data.Sprite;

class Main
{
	public static void main(String[] args) throws IOException
	{
		if (Boolean.parseBoolean(args[0])) // reset ROM file
			Files.copy(Paths.get("rom\\source.gbc"), Paths.get("rom\\rom.gbc"), REPLACE_EXISTING);
		
		File file = new File("rom\\rom.gbc");
		File fileSrc = new File("rom\\source.gbc");
		File fileSav = new File("rom\\rom.sav");
		
		try
		(
			RandomAccessFile stream = new RandomAccessFile(file, "rw");
			FileChannel ch = stream.getChannel();
			
			RandomAccessFile streamSrc = new RandomAccessFile(fileSrc, "r");
			FileChannel chSrc = streamSrc.getChannel();
			
			RandomAccessFile streamSav = new RandomAccessFile(fileSav, "rw");
			FileChannel chSav = streamSav.getChannel();
		)
		{	
			/////////////////////////////////////
			// randomizer settings
			/////////////////////////////////////
			
			int starterKind = 2; // kinds of starters (0 = totally random, 1 = at least 1 evolution, 2 = 3-stage only)
			
			boolean routeWSimilar = true; // route Pokemon with similar strength Pokemon
			boolean routeNoLeg = true; // whether to have no legendary in wild or yes
			boolean routeType = true; // whether to have routes with type-specific Pokemon
			
			boolean trainerWSimilar = true; // trainer Pokemon with similar strength Pokemon
			int typeExpert = 2; // typeExpert (0 = no type specialists, 1 = preserve type specialists, 2 = randomize type specialists)
			boolean persRival = true; // have a Rival team with persistent Pokemon or not
			boolean trainerNoLeg = true; // whether to have no legendary in Trainer parties or yes
			boolean extraCust = true; // whether to have customized Trainer Pokemon moves
			
			/////////////////////////////////////
			// ROM patching
			/////////////////////////////////////
			
			RomPatcher romPatcher = new RomPatcher(ch, chSrc);
			
			romPatcher.updateHeldItemRates();
			romPatcher.updateTypeEnhanceItems();
			
			/////////////////////////////////////
			// read data and randomize Pokedex Pokemon
			/////////////////////////////////////
			
			DataReader dataReader = new DataReader();
			PokedexRandomizer dexRand = new PokedexRandomizer(dataReader.getPokemonData(), dataReader.getSprites(), dataReader.getPalettes());
			
			RomReader romReader = new RomReader(ch);
			RomWriter romWriter = new RomWriter(ch);
			
			PokemonEditor monEditor = new PokemonEditor(dexRand);
			MoveEditor moveEditor = new MoveEditor(romReader);
			RouteEditor routeEditor = new RouteEditor(romReader, monEditor.getAllPokemon());
			TrainerEditor trainerEditor = new TrainerEditor(romReader, monEditor.getAllPokemon());
			SpriteEditor spriteEditor = new SpriteEditor(dexRand, romReader);
			
			moveEditor.updateMoves();
			//moveEditor.randomizeTMs();
			romWriter.replaceAllMoves(moveEditor.getAllMoves(), moveEditor.getAllLearnable());
			
			MoveSorter moveSorter = new MoveSorter(moveEditor.getAllMoves(), moveEditor.getAllLearnable(), moveEditor.getCritAnims());
			
			//monEditor.randomizeMovesets();
			//monEditor.randomizeCompatibilities();
			monEditor.fitEggMoves(moveEditor.getAllLearnableBytes());
			romWriter.replaceAllPokemon(monEditor.getAllPokemon());
			
			spriteEditor.packSprites();
			romWriter.replaceAllSprites(spriteEditor.getAllSprites(), spriteEditor.getAllTrainerSprites(), spriteEditor.getEggSprite(), spriteEditor.getAllPalettes());
			
			PokemonSorter monSorter = new PokemonSorter(monEditor.getAllPokemon(), romReader.readRomStarters());
			
			romWriter.randomizeStarters(monSorter, starterKind);
			
			routeEditor.scaleLevel((float) 1.0);
			routeEditor.randomizeSlotPokemon(monSorter, routeWSimilar, routeNoLeg, routeType);
			romWriter.replaceAllRoutePokemon(routeEditor.getRoutes());
			
			trainerEditor.buffKanto(monSorter, moveSorter);
			trainerEditor.scaleLevel((float) 1.0);
			trainerEditor.randomizePokemon(monSorter, trainerWSimilar, typeExpert, persRival, trainerNoLeg);
			trainerEditor.kantoForceEvolved(monSorter);
			trainerEditor.applyMovesets(moveSorter, extraCust);
			romWriter.replaceAllTrainers(trainerEditor.getTrainers());
			
			/////////////////////////////////////
			// manipulate save data
			/////////////////////////////////////
			
			SavePatcher savPatcher = new SavePatcher(chSav);
			savPatcher.generateTeam(monSorter, moveSorter, monEditor.getAllPokemon(), 4, 50);
			savPatcher.updateChecksums();
		}
	}
}