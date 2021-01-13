package jree.mongo_base;

import java.util.function.Supplier;

public class IDBuilder {

    private static final char[][] values = new char[256][2];

    static {
        for(int i=0;i<values.length;i++)
        {
            char[] chars = Integer.toHexString(i).toCharArray();
            if(chars.length>2)
                throw new IllegalStateException("size more than 2");

            if(chars.length<2)
            {
                values[i][0] = '0';
                values[i][1] = chars[0];
            }else {
                values[i][0] = chars[0];
                values[i][1] = chars[1];
            }
        }
    }



    private final int id;
    private int counter;
    private long lastTime;
    private long now;
    private final Supplier<Long> timestampSupplier;

    char[] chars = new char[32];

    public IDBuilder(int ID, Supplier<Long> timestampSupplier) {
        this.id = ID;
        this.timestampSupplier = timestampSupplier;

        char[] cap = values[(byte)(id>>24)&0xff];
        chars[16] = cap[0];chars[17] = cap[1];
        cap = values[(byte)(id>>16)&0xff];
        chars[18] = cap[0];chars[19] = cap[1];
        cap = values[(byte)(id>>8)&0xff];
        chars[20] = cap[0];chars[21] = cap[1];
        cap = values[(byte)(id)&0xff];
        chars[22] = cap[0];chars[23] = cap[1];
    }

    public synchronized String newId()
    {
        now = timestampSupplier.get();
        if(now>lastTime) {
            lastTime = now;
            counter = 0;
        }else {
            ++counter;
        }

        char[] cap = values[(byte)(now>>56)&0xff];
        chars[0] = cap[0];chars[1] = cap[1];
        cap = values[(byte)(now>>48)&0xff];
        chars[2] = cap[0];chars[3] = cap[1];
        cap = values[(byte)(now>>40)&0xff];
        chars[4] = cap[0];chars[5] = cap[1];
        cap = values[(byte)(now>>32)&0xff];
        chars[6] = cap[0];chars[7] = cap[1];
        cap = values[(byte)(now>>24)&0xff];
        chars[8] = cap[0];chars[9] = cap[1];
        cap = values[(byte)(now>>16)&0xff];
        chars[10] = cap[0];chars[11] = cap[1];
        cap = values[(byte)(now>>8)&0xff];
        chars[12] = cap[0];chars[13] = cap[1];
        cap = values[(byte)(now)&0xff];
        chars[14] = cap[0];chars[15] = cap[1];

        //----------------------------------------

        cap = values[(byte)(counter>>24)&0xff];
        chars[24] = cap[0];chars[25] = cap[1];
        cap = values[(byte)(counter>>16)&0xff];
        chars[26] = cap[0];chars[27] = cap[1];
        cap = values[(byte)(counter>>8)&0xff];
        chars[28] = cap[0];chars[29] = cap[1];
        cap = values[(byte)(counter)&0xff];
        chars[30] = cap[0];chars[31] = cap[1];

        return new String(chars);
    }

}
