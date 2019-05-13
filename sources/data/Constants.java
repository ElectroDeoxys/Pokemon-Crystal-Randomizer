package data;

import static java.lang.Math.*;

public class Constants
{

    public enum Type
    {
        NORMAL   ((byte) 0x00,  0),
        FIGHTING ((byte) 0x01,  1),
        FLYING   ((byte) 0x02,  2),
        POISON   ((byte) 0x03,  3),
        GROUND   ((byte) 0x04,  4),
        ROCK     ((byte) 0x05,  5),
        BUG      ((byte) 0x07,  6),
        GHOST    ((byte) 0x08,  7),
        STEEL    ((byte) 0x09,  8),
        FIRE     ((byte) 0x14,  9),
        WATER    ((byte) 0x15, 10),
        GRASS    ((byte) 0x16, 11),
        ELECTRIC ((byte) 0x17, 12),
        PSYCHIC  ((byte) 0x18, 13),
        ICE      ((byte) 0x19, 14),
        DRAGON   ((byte) 0x1A, 15),
        DARK     ((byte) 0x1B, 16),
        FAIRY    ((byte) 0x1C, 17),
        NO_TYPE  ((byte) 0xFE, -1);

        private final byte byteIndex;
        private final int intIndex;

        private Type(byte byteIndex, int intIndex)
        {
            this.byteIndex = byteIndex;
            this.intIndex = intIndex;
        }

        public byte byteIndex()
        {
            return byteIndex;
        }

        public int intIndex()
        {
            return intIndex;
        }
    }

    public static final int N_POKEMON_DATA = 493; // number of Pokemon in data
    public static final int N_TRAINERS = 541;
    public static final int N_POKEMON = byteToValue((byte) 0xFB);
    public static final int N_TYPES = Type.values().length - 1;
    public static final int N_MOVES = byteToValue((byte) 0xFE);
    public static final int N_TM = 50;
    public static final int N_HM = 7;
    public static final int N_MOVE_TUTOR = 3;
    public static final int N_SPRITE_BANKS = 17; // number of banks for the sprite data
    public static final int N_TRAINER_SPRITES = 67;

    public static final int[] OFFSET_WILD =
    {
        0x2A5EE, 0x2B120, 0x2B279, 0x2B7FA
    }; // offsets where wild Pokemon data start (Johto/Kanto)

    public static final int OFFSET_TRAINERS = 0x399C7;
    public static final int OFFSET_TRAINERS_POINTERS = 0x39941;

    public static final int[][] OFFSET_STARTERS =
    {
        {
            0x78CFD, 0x78CFF, 0x78D16, 0x78D21
        }, // grass
        
        {
            0x78C7F, 0x78C81, 0x78C98, 0x78CA3
        }, // fire
        
        {
            0x78CC1, 0x78CC3, 0x78CDA, 0x78CE5
        }
    }; // water
    public static final int OFFSET_MOVES = 0x41B05;
    public static final int OFFSET_TM_MOVES = 0x1167A;
    public static final int OFFSET_CRIT_MOVES = 0x346A3;
    public static final int OFFSET_MOVE_NAMES = 0x1C9F29;

    public static final int OFFSET_SPRITE_POINTERS = 0x120000;
    public static final int OFFSET_TRAINER_SPRITE_POINTERS = 0x128000;
    public static final int OFFSET_SPRITE_POINTER_EGG = 0x1205E8;
    public static final int OFFSET_SPRITES = 0x1205EE;
    public static final int OFFSET_PAL = 0xA8C4;

    public static final int OFFSET_POKEMON_NAMES = 0x53379;
    public static final int OFFSET_POKEMON_ICONS = 0x8EAC4;

    public static final int OFFSET_SPRITE_POINTERS_U = 0x124000; // Unown
    public static final int N_UNOWN = 26; // number of Unown formes
    public static final int INDEX_UNOWN = 201; // number of Unown index

    public static final int OFFSET_POKEMON_1 = 0x51419; // index, base stats, etc
    public static final int OFFSET_POKEMON_2 = 0x427C6; // evolution data and level-up moves
    public static final int OFFSET_POKEMON_3 = 0x23D07; // egg moves

    public static final int OFFSET_POINTERS_1 = 0x425D0; // pointers to the evolution and move table
    public static final int OFFSET_POINTERS_2 = 0x23B11; // pointers to egg moves

    public static final int[] OFFSET_TYPE_ENHANCING_ITEMS =
    {
        0x69D1, 0x69D8, 0x69F4, 0x6A25,
        0x6A56, 0x6A5D, 0x6A6B, 0x6A87,
        0x6A95, 0x6AAA, 0x6AB1, 0x6AD4,
        0x6AF0, 0x6B28, 0x6B83, 0x6BA6,
        0x6BDE, 0x6C63
    };

    public static final int[] OFFSET_WILD_ITEM_RATE =
    {
        0x3E921, 0x3E92A
    };

    public static final int SIZE_EGG_MOVES_MEM = 0x2F9; // size of memory capable of holding egg moves

    public static final byte MOVE_PHYSICAL_CATEGORY = (byte) 0b01000000;
    public static final byte MOVE_SPECIAL_CATEGORY = (byte) 0b10000000;
    public static final byte MOVE_OTHER_CATEGORY = (byte) 0b11000000;

    public static final int[] N_WILD_SLOTS =
    {
        21, 3
    }; // number of Pokemon slots in a land/water route
    public static final int[] N_BYTES_WILD =
    {
        ((N_WILD_SLOTS[0] * 2) + 5), ((N_WILD_SLOTS[1] * 2) + 3)
    }; // number of bytes per land/water route
    public static final int[] N_WILD_ROUTES =
    {
        61, 38, 30, 24
    }; // number of routes with encounters (Johto/Kanto)

    public static final int N_TIERS = 8 + 1;
    public static final int N_TYPE_TIERS = 6;

    public static final int BOT_BST = 200;
    public static final int TOP_BST = 600;

    public static final int N_MOVE_TIERS = 5;
    public static final int MOVE_BOT_TIER = 0;
    public static final int MOVE_MID_TIER = round((N_MOVE_TIERS - 1) / 2);
    public static final int MOVE_2ND_TIER = max(N_MOVE_TIERS - 2, 0);
    public static final int MOVE_TOP_TIER = N_MOVE_TIERS - 1;
    public static final int MOVE_DAM_MARGIN = 10; // margin of damage to replace a similar move

    public static final int NAME_LEN = 10; // length of names stored

    public static final int[] TRAINER_GROUPS =
    {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 23, 23, 24, 27, 28, 29, 30, 31, 32, 33, 34, 39, 40,
        54, 78, 97, 114, 115, 135, 156, 173, 188, 219, 224, 226, 229, 230, 249, 274,
        295, 314, 327, 341, 347, 349, 371, 380, 381, 384, 392, 398, 407, 411, 423,
        449, 471, 473, 485, 492, 495, 509, 515, 525, 531, 532, 533, 535, 540
    }; // start indices of each trainer group

    public static final int[][] INDEX_TRAINER_CLASSES =
    {
        {
            78, 96
        }, 
        {
            188, 218
        }, 
        {
            230, 248
        }, 
        {
            274, 313
        }, 
        {
            314, 326
        }, 
        {
            349, 370
        }, 
        {
            371, 379
        },
        {
            381, 383
        }, 
        {
            384, 391
        }, 
        {
            398, 406
        }, 
        {
            407, 410
        }, 
        {
            411, 422
        }, 
        {
            471, 472
        },
        {
            485, 491
        }, 
        {
            535, 539
        }
    }; // start and ending indexes (inclusive) of trainer classes
    public static final Type[][] TRAINER_CLASS_TYPES =
    {
        {
            Type.FLYING
        }, 
        {
            Type.NORMAL, Type.POISON, Type.DARK
        }, 
        {
            Type.BUG
        }, 
        {
            Type.WATER
        }, 
        {
            Type.NORMAL, Type.FIGHTING, Type.WATER
        },
        {
            Type.FIGHTING, Type.GROUND, Type.ROCK
        }, 
        {
            Type.POISON, Type.FIRE
        }, 
        {
            Type.POISON, Type.FIRE
        }, 
        {
            Type.FIRE
        }, 
        {
            Type.FIGHTING
        },
        {
            Type.NORMAL, Type.POISON, Type.DARK
        }, 
        {
            Type.PSYCHIC
        }, 
        {
            Type.NORMAL, Type.POISON, Type.DARK
        }, 
        {
            Type.GHOST, Type.PSYCHIC
        },
        {
            Type.NORMAL, Type.POISON, Type.DARK
        }
    }; // type arrays for each trainer classes defined above
    public static final int[] INDEX_TRAINER_FISHER =
    {
        249, 274
    }; // start and ending indexes (inclusive) of fishers

    public static final int[][] INDEX_GYM_TRAINERS =
    {
        {
            79, 78, 0
        }, 
        {
            515, 518, 235, 234, 236, 2
        }, 
        {
            156, 157, 97, 98, 1
        }, 
        {
            478, 486, 477, 485, 3
        },
        {
            399, 401, 402, 404, 6
        }, 
        {
            5
        }, 
        {
            224, 492, 225, 493, 494, 4
        }, 
        {
            117, 137, 118, 119, 138, 7
        },
        {
            466, 31
        }, 
        {
            294, 313, 312, 32
        }, 
        {
            394, 348, 221, 33
        }, 
        {
            519, 520, 441, 105, 169, 39
        },
        {
            102, 427, 453, 99, 114
        }, 
        {
            490, 421, 491, 412, 229
        }, 
        {
            380
        }, 
        {
        }
    }; // last gym has no type specialist
    public static final Type[] GYM_TYPES =
    {
        Type.FLYING, Type.BUG, Type.NORMAL, Type.GHOST, Type.FIGHTING, Type.STEEL, Type.ICE, Type.DRAGON,
        Type.ROCK, Type.WATER, Type.ELECTRIC, Type.GRASS, Type.POISON, Type.PSYCHIC, Type.FIRE, Type.NORMAL
    };
    public static final int[] INDEX_ELITE_FOUR =
    {
        23, 29, 27, 28
    };
    public static final Type[] ELITE_TYPES =
    {
        Type.PSYCHIC, Type.POISON, Type.FIGHTING, Type.DARK
    };
    public static final int[] INDEX_MIXED_TRAINERS =
    {
        26, 30, 531, 532
    }; // trainer teams to guarantee mixed party
    public static final int[][] INDEX_RIVAL =
    {
        {
            8, 11, 14, 17, 20, 341, 344
        },
        {
            9, 12, 15, 18, 21, 342, 345
        },
        {
            10, 13, 16, 19, 22, 343, 346
        }
    }; // rival teams divided by starters [grass, fire, water][party]

    public static final int[] INDEX_KANTO_TRAINERS =
    {
        48, 49, 50, 51, 55, 57, 58, 59, 60, 61, 62, 85, 86, 87, 88, 92, 93, 103, 104, 107, 131,
        148, 218, 226, 227, 231, 232, 241, 252, 261, 262, 263, 274, 287, 288, 290, 291, 306, 308,
        309, 311, 332, 333, 334, 361, 363, 364, 365, 373, 374, 375, 376, 377, 379, 384, 387, 413,
        414, 428, 429, 435, 436, 454, 455, 456, 497, 498, 499, 500, 506, 507, 508
    };

    public static final int[] RIVAL_PARTY_SIZES =
    {
        1, 3, 4, 5, 6, 6, 6
    }; // rival team size for each battle

    public static final int[][] INDEX_ROUTE_SPECIFIC_TYPES =
    {
        {
            26, 27, 28, 29
        }, 
        {
            103
        }
    }; // indexes of routes for each given type 
    public static final Type[][] ROUTE_TYPES =
    {
        {
            Type.ICE
        }, 
        {
            Type.GROUND, Type.ROCK
        }
    }; // type arrays for each routes defined above

    public static final int LEGENDARY_BST = 580; // minimum base stat total of legendary Pokemon
    public static final int STARTER_BST = 350; // maximum base stat total of starter Pokemon
    public static final int OTHER_METHODS_LEVEL = 40; // level at which the evolutionary methods other than leveling up cause evolution
    public static final int OTHER_METHODS_LEVEL_LOWER = 20; // for Pokemon that evolve into Pokemon that can still evolve

    public static int byteToValue(byte x)
    {
        int res = (int) x;

        if (x < 0)
        {
            res = (x + 0xFF) + 1;
        }

        return res;
    }

    public static int[] byteToValue(byte[] x)
    {
        int[] res = new int[x.length];
        for (int i = 0; i < x.length; i++)
        {
            res[i] = byteToValue(x[i]);
        }
        return res;
    }

    public static byte valueToByte(int x)
    {
        byte res;

        if (x < 0x80)
        {
            res = (byte) x;
        }
        else
        {
            res = (byte) (x - 0xFF - 1);
        }

        return res;
    }

    public static byte[] valueToByte(int[] x)
    {
        byte[] res = new byte[x.length];
        for (int i = 0; i < x.length; i++)
        {
            res[i] = valueToByte(x[i]);
        }
        return res;
    }

    public static Byte[] convertByteArray(byte[] byteArray)
    {
        Byte[] out = new Byte[byteArray.length];
        int n = 0;

        for (byte i : byteArray)
        {
            out[n] = i;
            n++;
        }

        return out;
    }

    public static byte[] convertByteArray(Byte[] byteArray)
    {
        byte[] out = new byte[byteArray.length];
        int n = 0;

        for (Byte b : byteArray)
        {
            out[n] = b;
            n++;
        }

        return out;
    }

    public static int[] convertIntArray(Integer[] intArray)
    {
        int[] out = new int[intArray.length];
        int n = 0;

        for (Integer i : intArray)
        {
            out[n] = i;
            n++;
        }

        return out;
    }

    public static int[] shuffleArray(int[] array)
    {
        int n = array.length;
        int[] arrayOut = array;

        // Loop over array.
        for (int i = 0; i < arrayOut.length; i++)
        {
            // Get a random index of the array past the current index.
            int randomValue = i + (int) floor(random() * (n - i));
            // Swap the random element with the present element.
            int randomElement = arrayOut[randomValue];
            arrayOut[randomValue] = arrayOut[i];
            arrayOut[i] = randomElement;
        }

        return arrayOut;
    }

    public static byte[] shuffleArray(byte[] array)
    {
        int n = array.length;
        byte[] arrayOut = array;

        // Loop over array.
        for (int i = 0; i < arrayOut.length; i++)
        {
            // Get a random index of the array past the current index.
            int randomValue = i + (int) floor(random() * (n - i));
            // Swap the random element with the present element.
            byte randomElement = arrayOut[randomValue];
            arrayOut[randomValue] = arrayOut[i];
            arrayOut[i] = randomElement;
        }

        return arrayOut;
    }

    public static Type[] shuffleArray(Type[] array)
    {
        int n = array.length;
        Type[] arrayOut = array;

        // Loop over array.
        for (int i = 0; i < arrayOut.length; i++)
        {
            // Get a random index of the array past the current index.
            int randomValue = i + (int) floor(random() * (n - i));
            // Swap the random element with the present element.
            Type randomElement = arrayOut[randomValue];
            arrayOut[randomValue] = arrayOut[i];
            arrayOut[i] = randomElement;
        }

        return arrayOut;
    }

    public static byte getBit(byte b, int k)
    {
        byte out = (byte) ((b >> k) & 0b1);
        return out;
    }

    public static byte[] getPointer(int pos)
    {
        byte[] out = new byte[2];
        int x = (pos % (0xFFFF + 1)); // take only last four digits

        out[0] = valueToByte(x % (0xFF + 1)); // less significant bytes are the two last bytes
        int y = (int) floor(x / (0xFF + 1)); // more significant bytes are the first two

        if (x < 0x4000)
        {
            y += 0x40;
        }
        else if (x >= 0x8000 && x < 0xC000)
        {
            y -= 0x40;
        }
        else if (x >= 0xC000)
        {
            y -= 0x80;
        }

        out[1] = valueToByte(y);

        return out;
    }

    public static int getOffset(byte[] ptr, int thisPos)
    {
        int out = 0;
        out += byteToValue(ptr[0]); // least significant
        out += (byteToValue(ptr[1]) - 0x40) * 0x100;

        // get current bank number to get associated offset
        int curBank = (int) floor(thisPos / 0x4000);
        out += curBank * 0x4000;

        return out;
    }

    public static int byteArrayToInt(byte[] b, boolean bigEndian)
    {
        int out = 0;

        if (bigEndian)
        {
            for (int i = 0; i < b.length; i++)
            {
                out += byteToValue(b[b.length - 1 - i]) * pow(0x100, i);
            }
        }
        else
        {
            for (int i = 0; i < b.length; i++)
            {
                out += byteToValue(b[i]) * pow(0x100, i);
            }
        }

        return out;
    }

    public static boolean arrayContains(int[] a, int n)
    {
        boolean res = false;
        for (int i = 0; i < a.length; i++)
        {
            if (a[i] == n)
            {
                res = true;
                break;
            }
        }
        return res;
    }
}
