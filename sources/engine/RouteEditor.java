package engine;

import java.io.IOException;
import static java.lang.Math.*;

import static data.Constants.*;
import data.Route;
import data.PokemonGame;

class RouteEditor
{
    private Route[] routes;
    private PokemonGame[] mons;

    RouteEditor(RomReader romReader, PokemonGame[] mons) throws IOException
    {
        this.routes = romReader.readRomRoutes();
        this.mons = mons;
    }

    void randomizePokemon(PokemonSorter monSorter, boolean withSimilar, boolean noLeg)
    {
        if (!withSimilar)
        {
            for (int i = 0; i < Route.indexBreaks[Route.indexBreaks.length - 1]; i++)
            {
                for (int j = 0; j < routes[i].getTotalSlots(); j++)
                {
                    routes[i].setPoke(j, (byte) round(random() * 0xFB));
                }
            }
        }
        else
        {
            for (int i = 0; i < Route.indexBreaks[Route.indexBreaks.length - 1]; i++)
            {
                for (int j = 0; j < routes[i].getTotalSlots(); j++)
                {
                    PokemonGame initialMon = PokemonEditor.getPokemonFromByte(routes[i].getPokeByte(j), mons);
                    routes[i].setPoke(j, monSorter.getSameTier(initialMon, Type.NO_TYPE, noLeg, false, false));
                }
            }
        }
    }

    void randomizeSlotPokemon(PokemonSorter monSorter, boolean withSimilar, boolean noLeg, boolean typeRoutes)
    {
        if (!withSimilar)
        {
            for (int i = 0; i < Route.indexBreaks[Route.indexBreaks.length - 1]; i++)
            {
                if ((typeRoutes) && (routes[i].getLandIndex() == 1)) // is a water route
                {
                    int[] waterArray = monSorter.getPokemonOfType(Type.WATER);
                    for (int j = 0; j < routes[i].getNumberSpecies(); j++)
                    {
                        routes[i].setSlot(j, waterArray[(int) floor(random() * waterArray.length)]);
                    }
                }
                else
                {
                    for (int j = 0; j < routes[i].getNumberSpecies(); j++)
                    {
                        routes[i].setSlot(j, (byte) round(random() * 0xFB));
                    }
                }
            }
        }
        else
        {
            for (int i = 0; i < Route.indexBreaks[Route.indexBreaks.length - 1]; i++)
            {
                if ((typeRoutes) && (routes[i].getLandIndex() == 1)) // is a water route
                {
                    for (int j = 0; j < routes[i].getNumberSpecies(); j++)
                    {
                        PokemonGame initialMon = PokemonEditor.getPokemonFromByte(routes[i].getPokeSpeciesByte(j), mons);
                        routes[i].setSlot(j, monSorter.getSameTier(initialMon, Type.WATER, noLeg, false, false));
                    }
                }
                else if ((typeRoutes) && (arrayContains(INDEX_ROUTE_SPECIFIC_TYPES[0], i))) // for Ice Path
                {
                    for (int j = 0; j < routes[i].getNumberSpecies(); j++)
                    {
                        PokemonGame initialMon = PokemonEditor.getPokemonFromByte(routes[i].getPokeSpeciesByte(j), mons);
                        routes[i].setSlot(j, monSorter.getSameTier(initialMon, ROUTE_TYPES[0], noLeg));
                    }
                }
                else if ((typeRoutes) && (arrayContains(INDEX_ROUTE_SPECIFIC_TYPES[1], i))) // for Victory Road
                {
                    for (int j = 0; j < routes[i].getNumberSpecies(); j++)
                    {
                        PokemonGame initialMon = PokemonEditor.getPokemonFromByte(routes[i].getPokeSpeciesByte(j), mons);
                        routes[i].setSlot(j, monSorter.getSameTier(initialMon, ROUTE_TYPES[1], noLeg));
                    }
                }
                else
                {
                    for (int j = 0; j < routes[i].getNumberSpecies(); j++)
                    {
                        PokemonGame initialMon = PokemonEditor.getPokemonFromByte(routes[i].getPokeSpeciesByte(j), mons);
                        routes[i].setSlot(j, monSorter.getSameTier(initialMon, Type.NO_TYPE, noLeg, false, false));
                    }
                }
            }
        }
    }

    void scaleLevel(float mult)
    {
        for (int i = 0; i < Route.indexBreaks[Route.indexBreaks.length - 1]; i++)
        {
            for (int j = 0; j < routes[i].getTotalSlots(); j++)
            {
                int lvlMult = (int) max(min((routes[i].getLvl(j) * mult), 100), 2);
                routes[i].setLvl(j, (byte) lvlMult);
            }
        }
    }

    Route[] getRoutes()
    {
        return routes;
    }
}
