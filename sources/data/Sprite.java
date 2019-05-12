package data;

import static java.lang.Math.*;

import static data.Constants.*;

public class Sprite
{
    byte[] front; // front sprite
    byte[] back; // back sprite
    byte dim; // dimension of sprite (55, 66 or 77)
    int[] offset; // offset [front sprite, back sprite]

    public Sprite(byte[] front, byte[] back, byte dim, int[] offset)
    {
        this.front = front;
        this.back = back;
        this.dim = dim;
        this.offset = offset;
    }

    public Sprite(byte[] front, int[] offset)
    {
        this.front = front;
        this.back = new byte[0];
        this.dim = 0;
        this.offset = offset;
    }

    public byte[] getFront()
    {
        return this.front;
    }

    public byte[] getBack()
    {
        return this.back;
    }

    public byte getDim()
    {
        return this.dim;
    }

    public int[] getOffset()
    {
        return this.offset;
    }

    public byte[][] getSpritePointer()
    {
        byte[][] out = new byte[2][];
        out[0] = offsetToPointer(this.offset[0]);
        out[1] = offsetToPointer(this.offset[1]);
        return out;
    }

    public void setOffset(int n, int pos)
    {
        this.offset[n] = pos;
    }

    public static int pointerToOffset(byte[] ptr)
    {
        // converts the 3-byte pointers to offset
        int out = 0;
        out += (byteToValue(ptr[0]) + (0x48 - 0x12)) * 0x4000; // get to the specified bank
        out += byteToValue(ptr[1]); // least significant
        out += (byteToValue(ptr[2]) - 0x40) * 0x100;
        return out;
    }

    public static byte[] offsetToPointer(int pos)
    {
        // converts the 3-byte pointers to offset
        byte[] out = new byte[3];
        out[0] = valueToByte((int) floor(pos / 0x4000) - (0x48 - 0x12));

        byte[] ptr = getPointer(pos);
        out[1] = ptr[0];
        out[2] = ptr[1];

        return out;
    }
}
