/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import org.junit.Test;

import java.io.IOException;

import java.math.BigInteger;

import java.util.Random;


/**
* Test for POF Handlers.
*
* @author cp  2006.07.12
*/
public class PofHandlerTest
        extends Base
    {
    public static void main(String[] asArg) throws Exception
        {
        new PofHandlerTest().testPOF();
        }

    @Test
    public void testPOF()
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        PofHandler pofOut = new DuplexingPofHandler(
                new DuplexingPofHandler(new ValidatingPofHandler(), new LoggingPofHandler()),
                new WritingPofHandler(out));

        out();
        out("tediously writing data ..");
        pofOut.beginUserType(-1, 1, 2);
        pofOut.onBoolean(0, false);
        pofOut.onBoolean(1, true);
        pofOut.onInt32(3, 12345);
        pofOut.onCharString(4, "hello world");
        pofOut.beginArray(6, 4);
        pofOut.onChar(0, 'x');
        pofOut.registerIdentity(99);
        pofOut.onOctetString(1, new Binary(new byte[] {1,2,3}));
        pofOut.onDate(2, 2006, 7, 13);
        pofOut.onIdentityReference(3, 99);
        pofOut.endComplexValue();
        pofOut.endComplexValue();

        Binary bin = buf.toBinary();
        out();
        out("result:");
        out(toHexDump(bin.toByteArray(), 16));

        // same thing again
        buf = new BinaryWriteBuffer(2000);
        out = buf.getBufferOutput();
        pofOut = new DuplexingPofHandler(
                new DuplexingPofHandler(new ValidatingPofHandler(), new LoggingPofHandler()),
                new WritingPofHandler(out));
        PofParser parser = new PofParser(pofOut);

        out();
        out("parsing the result back through the same pipes ..");
        parser.parse(bin.getBufferInput());

        Binary bin2 = buf.toBinary();
        out();
        out("result:");
        out(toHexDump(bin2.toByteArray(), 16));

        out();
        out("comparing the results");
        if (bin.equals(bin2))
            {
            out("identical!!!");
            }
        else
            {
            out("different :-(");
            }
        }

    public void testRandomBigintSer(int c) throws IOException
        {
        Random rnd = getRandom();
        for (int i = 0; i < c; ++i)
            {
            long l = rnd.nextLong();
            if (!serLong(l).equals(serBigInt(BigInteger.valueOf(l))))
                {
                out("failed on long=" + l);
                return;
                }
            }
        }

    public void testRandomBigintDeser(int c) throws IOException
        {
        Random rnd = getRandom();
        for (int i = 0; i < c; ++i)
            {
            BigInteger bigint;
            switch (i)
                {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    bigint = BigInteger.valueOf(i-3);
                    break;

                default:
                    // gen rnd bigint
                    byte[] ab = new byte[rnd.nextInt(16)+1];
                    rnd.nextBytes(ab);
                    bigint = new BigInteger(ab);
                    break;
                }

            Binary bin = serBigInt(bigint);
            BigInteger bigint2 = deserBigInt(bin);

            if (!bigint.equals(bigint2))
                {
                out("failed on bigint=" + bigint);
                out("serialized=" + bin);
                out("deserialized=" + bigint2);
                return;
                }
            }
        }

    public void testSpecificBigintSer() throws IOException
        {
        show("max long, ser=" + serLong(Long.MAX_VALUE), BigInteger.valueOf(Long.MAX_VALUE));
        show("min long, ser=" + serLong(Long.MIN_VALUE), BigInteger.valueOf(Long.MIN_VALUE));
        show("0, ser=" + serLong(0L), BigInteger.valueOf(0L));
        show("-1, ser=" + serLong(-1L), BigInteger.valueOf(-1L));

        BigInteger nPos = new BigInteger("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
        show("positive", nPos);

        BigInteger nNeg = new BigInteger("-80000000000000000000000000000000", 16);
        show("negative", nNeg);

        BigInteger nMax = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
        show("too big", nMax);
        }

    static void show(String s, BigInteger bigint) throws IOException
        {
        out();
        out(s + ", value=" + bigint + ", bitLength=" + bigint.bitLength() + ", byte[]=" + new Binary(bigint.toByteArray()));
        out("ser=" + serBigInt(bigint));
        }

    static Binary serBigInt(BigInteger bigint) throws IOException
        {
        // out("value=" + bigint + ", bitLength=" + bigint.bitLength() + ", byte[]=" + new Binary(bigint.toByteArray()));
        WriteBuffer buf = new BinaryWriteBuffer(20);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        // writeInt128(out, bigint)
        byte[] ab = bigint.toByteArray();
        int    cb = ab.length;
        if (cb > 16)
            {
            throw new IOException("integer exceeds 128 bits");
            }

        int b = 0;

        // check for negative
        if ((ab[0] & 0x80) != 0)
            {
            b = 0x40;
            for (int of = 0; of < cb; ++of)
                {
                ab[of] = (byte) ~ab[of];
                }
            }

        // trim off the leading zeros
        int    ofMSB = 0;
        while (ofMSB < cb && ab[ofMSB] == 0)
            {
            ++ofMSB;
            }

        if (ofMSB < cb)
            {
            int of    = cb - 1;
            int nBits = ab[of] & 0xFF;

            b |= (byte) (nBits & 0x3F);
            nBits >>>= 6;
            int cBits = 2;  // only 2 data bits left in nBits

            while (nBits != 0 || of > ofMSB)
                {
                b |= 0x80;
                out.writeByte(b);

                // load more data bits if necessary
                if (cBits < 7)
                    {
                    nBits |= (--of < 0 ? 0 : ab[of] & 0xFF) << cBits;
                    cBits += 8;
                    }

                b = (nBits & 0x7F);
                nBits >>>= 7;
                cBits -= 7;
                }
            }

        out.writeByte(b);

        // out("ser=" + buf.toBinary());
        return buf.toBinary();
        }

    static BigInteger deserBigInt(Binary bin) throws IOException
        {
        ReadBuffer.BufferInput in = bin.getBufferInput();

        int     cb   = 16;
        byte[]  ab   = new byte[cb];
        int     b    = in.readUnsignedByte();
        boolean fNeg = (b & 0x40) != 0;

        int of = cb - 1;
        ab[of] = (byte) (b & 0x3F);
        int cBits = 6;

        while ((b & 0x80) != 0)
            {
            b = in.readUnsignedByte();
            ab[of] = (byte) ((ab[of] & 0xFF) | ((b & 0x7F) << cBits));
            cBits += 7;
            if (cBits >= 8)
                {
                cBits -= 8;
                --of;

                if (cBits > 0 && of >= 0)
                    {
                    // some bits left over
                    ab[of] = (byte) ((b & 0x7F) >>> (7 - cBits));
                    }
                }
            }

        if (fNeg)
            {
            for (of = 0; of < 16; ++of)
                {
                ab[of] = (byte) ~ab[of];
                }
            }

        return new BigInteger(ab);
        }

    static Binary serLong(long l) throws IOException
        {
        WriteBuffer buf = new BinaryWriteBuffer(20);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();
        out.writePackedLong(l);
        return buf.toBinary();
        }
    }
/*
// reverse the "big endian" to "little endian"
for (int ofFront = 0, ofBack = ab.length - 1; ofFront < ofBack; ++ofFront, --ofBack)
    {
    byte bFront = ab[ofFront];
    ab[ofFront] = ab[ofBack];
    ab[ofBack ] = bFront;
    }
*/