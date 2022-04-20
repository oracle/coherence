/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataOutputStream;
import java.io.DataOutput;
import java.io.IOException;


/**
* Compare Buffer performance versus Stream performance.
*
* @author cp  2009.04.30
*/
public class BufferPerfTest
        extends Base
    {
    /**
    * Command line execution.
    *
    * @param asArg an array of command line arguments
    */
    public static void main(String[] asArg)
            throws IOException
        {
        // testWriteInt();
        // testMultiBuffer();
        // testBlockSize();
        testToBinary();
        }

    public static void testWriteInt()
            throws IOException
        {
        MultiBufferWriteBuffer   buf = new MultiBufferWriteBuffer(
                new ByteArrayWriteBuffer.Allocator(15));
        WriteBuffer.BufferOutput out = buf.getBufferOutput();
        for (int i = 0; i < 20; ++i)
            {
            out.writeInt(i);
            }
        }

    public static void testBlockSize()
            throws IOException
        {
        int[] SIZES = {256, 512, 1024, 2048, 4096, 8192};
        for (int i = 0; i < SIZES.length; ++i)
            {
            int cb = SIZES[i];
            out("buffer size=" + cb);
            testMultiBuffer(1, cb);
            }
        }

    public static void testMultiBuffer()
            throws IOException
        {
        long lStart = System.currentTimeMillis();
        for (int i = 0; i < 20; ++i)
            {
            // int nTest = 4;
            for (int nTest = 4; nTest <= MAX_TESTS; ++nTest)
                {
                // note: results below were from size 1468; increasing size
                // by 10x puts MBWB in the lead or tied for all tests!!!
                testMultiBuffer(nTest, 14500);
                }
            }
        long lStop = System.currentTimeMillis();
        out();
        out("elapsed time = " + (lStop - lStart) + "ms");
        }

    public static void testMultiBuffer(int nTest, int cb)
            throws IOException
        {
        int[] ai = new int[] {0, 1, 2};
        // TODO
        // randomize(ai);
        // for (int i = 0, c = ai.length; i < c; ++i)
        int i = 1;
            {
            String sDesc = "?";
            long ldtStart = System.currentTimeMillis();
            for (int iIter = 0; iIter < 20; ++iIter)
                {
                switch (ai[i])
                    {
                    case 0:
                        {
                        sDesc = "MBAOS";
                        MultiByteArrayOutputStream streamRaw  = new MultiByteArrayOutputStream(cb);
                        DataOutputStream           streamData = new DataOutputStream(streamRaw);
                        testDataOutput(nTest, streamData);
                        streamData.close();
                        break;
                        }

                    case 1:
                        {
                        sDesc = "oldMB";
                        OldMultiBufferWriteBuffer bufOld     = new OldMultiBufferWriteBuffer(new ByteArrayWriteBuffer.Allocator(cb));
                        WriteBuffer.BufferOutput   outOld     = bufOld.getBufferOutput();
                        testDataOutput(nTest, outOld);
                        outOld.close();
                        break;
                        }

                    case 2:
                        {
                        sDesc = "MBWB ";
                        MultiBufferWriteBuffer     buf        = new MultiBufferWriteBuffer(new ByteArrayWriteBuffer.Allocator(cb));
                        WriteBuffer.BufferOutput   out        = buf.getBufferOutput();
                        testDataOutput(nTest, out);
                        out.close();
                        break;
                        }
                    }
                }
            out(sDesc + "[" + nTest + "]=" + (System.currentTimeMillis() - ldtStart) + "ms");
            }

        out();
        }

    public static void testDataOutput(int nTest, DataOutput out)
            throws IOException
        {
        switch (nTest)
            {
            default:
                throw new IllegalStateException("test="+nTest);

            case 0:
                // 1.4: buf is 300% faster (was +/-10%, but generally a little slower)
                // 1.5: buf is 15-35% FASTER!
                // 1.6: buf typically 40% slower, but varies to 10% faster
                for (int i = 0; i < 3000000; ++i)
                    {
                    out.write((byte) i);
                    }
                break;

            case 1:
                // 1.4: buf is 100% (was 15%) slower
                // 1.5: buf is 100% faster (was 15% slower)
                // 1.6: buf is 50+% faster (was 30% slower)
                for (int i = 0; i < 2000000; ++i)
                    {
                    out.writeInt(i);
                    }
                break;

            case 2:
                // 1.4: buf is 300% (was 75%) slower
                // 1.5: buf is 100% (was 25-30%) slower
                // 1.6: buf is 25% (was 10%) FASTER!
                for (int i = 0; i < 1000000; ++i)
                    {
                    ExternalizableHelper.writeInt(out, i);
                    }
                break;

            case 3:
                // 1.4: buf is up to 35% (was 200%) slower
                // 1.5: buf is 15% faster (was 90% slower)
                // 1.6: buf is 6% (was 100%) slower
                for (int i = 0; i < 400000; ++i)
                    {
                    // ExternalizableHelper.writeByteArray(out, ARRAYS[i & 0x3]);
                    out.write(ARRAYS[i & 0x3]);
                    }
                break;

            case 4:
                // 1.4:
                // 1.5:
                // 1.6:
                for (int i = 0; i < 40000; ++i)
                    {
                    out.writeUTF(SMALL_ASCII_STRINGS[i & 0x3]);
                    }
                break;

            case 5:
                // 1.4:
                // 1.5:
                // 1.6:
                for (int i = 0; i < 40000; ++i)
                    {
                    out.writeUTF(SMALL_UNICODE_STRINGS[i & 0x3]);
                    }
                break;

            case 6:
                // 1.4:
                // 1.5:
                // 1.6:
                for (int i = 0; i < 4000; ++i)
                    {
                    out.writeUTF(BIG_ASCII_STRINGS[i & 0x3]);
                    }
                break;

            case 7:
                // 1.4:
                // 1.5:
                // 1.6:
                for (int i = 0; i < 4000; ++i)
                    {
                    out.writeUTF(BIG_UNICODE_STRINGS[i & 0x3]);
                    }
                break;
            }
        }

    public static void testToBinary()
            throws IOException
        {
        testToBinary(1, 1450);
        }

    public static void testToBinary(int iImpl, int cb)
            throws IOException
        {
        String sDesc;
        WriteBuffer buf;

        switch (iImpl)
            {
            case 1:
                {
                sDesc = "oldMB";
                buf = new OldMultiBufferWriteBuffer(new ByteArrayWriteBuffer.Allocator(cb));
                break;
                }

            case 2:
                {
                sDesc = "MBWB ";
                buf = new MultiBufferWriteBuffer(new ByteArrayWriteBuffer.Allocator(cb));
                break;
                }

            default:
                throw new IllegalStateException("impl=" + iImpl);
            }

        buf.write(0, new byte[2000]);

        long lStart = System.currentTimeMillis();
        for (int i = 0; i < 20; ++i)
            {
            testToBinary(sDesc, buf);
            }
        long lStop = System.currentTimeMillis();
        out();
        out("elapsed time = " + (lStop - lStart) + "ms");
        }

    public static int testToBinary(String sDesc, WriteBuffer buf)
            throws IOException
        {
        long ldtStart = System.currentTimeMillis();
        int c = 0;
        for (int i = 0; i < 100000; ++i)
            {
            c += buf.toBinary().length();
            }
        out(sDesc + "=" + (System.currentTimeMillis() - ldtStart) + "ms");
        return c;
        }

    static final int MAX_TESTS = 7;
    static final byte[][] ARRAYS = new byte[][]
        {
        new byte[5],
        new byte[8],
        new byte[25],
        new byte[130],
        };

    static final String[] SMALL_ASCII_STRINGS = new String[]
        {
        "this is a test",
        Base.getRandomString(25, 25, true),
        Base.getRandomString(50, 50, true),
        Base.getRandomString(100, 100, true),
        };

    static final String[] SMALL_UNICODE_STRINGS = new String[]
        {
        Base.getRandomString(15, 15, false),
        Base.getRandomString(25, 25, false),
        Base.getRandomString(50, 50, false),
        Base.getRandomString(100, 100, false),
        };

    static final String[] BIG_ASCII_STRINGS = new String[]
        {
        Base.getRandomString(500, 500, true),
        Base.getRandomString(1000, 1000, true),
        Base.getRandomString(1500, 1500, true),
        Base.getRandomString(2000, 2000, true),
        };

    static final String[] BIG_UNICODE_STRINGS = new String[]
        {
        Base.getRandomString(500, 500, false),
        Base.getRandomString(1000, 1000, false),
        Base.getRandomString(1500, 1500, false),
        Base.getRandomString(2000, 2000, false),
        };
    }
