package engine;

import static data.Constants.*;
import data.Pokemon;
import data.Trainer;
import data.Move;

public class Names
{
    private String[] pokemonNames;
    private String[] trainerNames;
    private String[] moveNames;

    Names(Pokemon[] mons, Trainer[] trainers, Move[] moves)
    {
        pokemonNames = new String[mons.length + 1];
        trainerNames = new String[trainers.length + 1];
        moveNames = new String[moves.length + 1];

        pokemonNames[0] = "NA";
        trainerNames[0] = "NA";
        moveNames[0] = "NA";

        for (int i = 0; i < mons.length; i++)
        {
            pokemonNames[i + 1] = new String(convertBytesToText(mons[i].getName()));
        }

        for (int i = 0; i < trainers.length; i++)
        {
            trainerNames[i + 1] = new String(convertBytesToText(trainers[i].getName()));
        }

        for (int i = 0; i < moves.length; i++)
        {
            moveNames[i + 1] = new String(convertBytesToText(moves[i].getName()));
        }
    }

    String pokemon(int n)
    {
        return pokemonNames[n + 1];
    }

    String pokemon(byte b)
    {
        return pokemon(byteToValue(b) - 1);
    }

    String trainer(int n)
    {
        return trainerNames[n + 1];
    }

    String trainer(byte b)
    {
        return trainer(byteToValue(b) - 1);
    }

    String move(int n)
    {
        return moveNames[n + 1];
    }

    String move(byte b)
    {
        return move(byteToValue(b) - 1);
    }

    public static byte[] parseTextBytes(byte[] strIn)
    {
        // converts text characters to in-game text bytes
        byte[] nameOut = new byte[strIn.length];
        for (int i = 0; i < strIn.length; i++)
        {
            //special characters
            switch (strIn[i])
            {
                case 0x20:
                    nameOut[i] = (byte) 0x7F; // space
                    break;
                case 0x27:
                    nameOut[i] = (byte) 0xE0; // apostrophe
                    break;
                case 0x2D:
                    nameOut[i] = (byte) 0xE3; // dash
                    break;
                case 0x2E:
                    nameOut[i] = (byte) 0xE8; // dot
                    break;
                default:
                    break;
            }

            int charI = byteToValue(strIn[i]);

            if (charI >= 0x30 && charI < 0x40) // number
            {
                nameOut[i] = (byte) (strIn[i] + 0xC6);
            }
            else if (charI >= 0x41 && charI < 0x5B) // letter
            {
                nameOut[i] = (byte) (strIn[i] + 0x3F);
            }
            else // placeholder
            {
                nameOut[i] = (byte) 0xE6;
            }
        }

        return nameOut;
    }

    public static char[] convertBytesToText(byte[] byteStr)
    {
        char[] str = new char[byteStr.length];
        char[] lookupAlpha =
        {
            'A', 'B', 'C', 'D', 'E',
            'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O',
            'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z'
        };

        char[] lookupNum =
        {
            '0', '1', '2', '3', '4',
            '5', '6', '7', '8', '9'
        };

        for (int i = 0; i < str.length; i++)
        {
            int byteGet = byteToValue(byteStr[i]);

            int[] indexChar = new int[2];
            indexChar[0] = (byteGet - 0x80);
            indexChar[1] = (byteGet - 0xF6);

            if ((indexChar[0] >= 0) && (indexChar[0] < lookupAlpha.length)) // is an alpha char
            {
                str[i] = lookupAlpha[indexChar[0]];
            }
            else if ((indexChar[1] >= 0) && (indexChar[1] < lookupNum.length)) // is a numerical char
            {
                str[i] = lookupNum[indexChar[1]];
            }
            else
            {
                switch (byteStr[i]) //special characters
                {
                    case (byte) 0x7F:
                        str[i] = ' '; // space
                        break;
                    case (byte) 0xE0:
                        str[i] = '\''; // apostrophe
                        break;
                    case (byte) 0xE3:
                        str[i] = '-'; // dash
                        break;
                    case (byte) 0xE8:
                        str[i] = '.'; // dot
                        break;
                    default:
                        break;
                }
            }
        }

        return str;
    }
}
