/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


/**
* Test class: Comparing the results of two different stream implementations.
*
* java -server -Xms512m -Xmx512m -Xnoclassgc com.tangosol.tests.io.ExternalizableTest 100000 10
*
* @author cp  2004.09.09
*/
public class ExternalizableTest
        extends Base
    {
    // ---- test methods ----------------------------------------------------

    @Test
    public void test()
        {
        main(new String[] {"100000", "10"});
        }


    // ----- command line test ----------------------------------------------

    public static void main(String[] asArgs)
        {
        try
            {
            int cIters = 100000;
            int cRuns  = 10;
            try
                {
                cIters = Integer.parseInt(asArgs[0]);
                cRuns  = Integer.parseInt(asArgs[1]);
                }
            catch (Exception e)
                {
                }
            out("Running " + cRuns + " runs of " + cIters + " iterations");

            for (int iRun = 0; iRun < cRuns; ++iRun)
                {
                out("Run #" + iRun);
                // build list of test steps
                List list = new ArrayList(cIters);
                for (int i = 0; i < cIters; ++i)
                    {
                    Class clz = STEP_CLASSES[rnd.nextInt(STEP_CLASSES.length)];
                    list.add(clz.newInstance());
                    }

                testBaseline(list);
                testPacked(list);
                testBinary(list);

                /*
                gc();
                if ((iRun & 0x01) == 0)
                    {
                    testPacked(list);
                    gc();
                    testBaseline(list);
                    }
                else
                    {
                    testBaseline(list);
                    gc();
                    testPacked(list);
                    }
                gc();
                */
                }

            out("test done");
            }
        catch (Throwable e)
            {
            err("Test threw:");
            err(e);
            }
        }

    static void gc() throws Exception
        {
        System.gc();
        Thread.yield();
        }

    static void testBaseline(List list) throws Exception
        {
        // test the steps using the Java implementations
        start();

        ByteArrayOutputStream outRaw   = new ByteArrayOutputStream(list.size() * 8);
        DataOutputStream      outData  = new DataOutputStream(outRaw);
        writeTest(list, outData);

        ByteArrayInputStream inRaw    = new ByteArrayInputStream(outRaw.toByteArray());
        DataInputStream      inData   = new DataInputStream(inRaw);
        readTest(list, inData);

        stop("baseline; used " + outRaw.toByteArray().length + " bytes");
        }

    static void testPacked(List list) throws Exception
        {
        // test the steps using the packed implementations
        start();

        ByteArrayOutputStream  outRaw  = new ByteArrayOutputStream(list.size() * 8);
        PackedDataOutputStream outData = new PackedDataOutputStream(outRaw);
        writeTest(list, outData);

        ByteArrayInputStream  inRaw    = new ByteArrayInputStream(outRaw.toByteArray());
        PackedDataInputStream inData   = new PackedDataInputStream(inRaw);
        readTest(list, inData);

        stop("packed; used " + outRaw.toByteArray().length + " bytes");
        }

    static void testBinary(final List list) throws Exception
        {
        // test the steps using the WriteBuffer/ReadBuffer implementations
        // see ExternalizableHelper#(toBinary(), fromBinary())
        start();

        BinaryWriteBuffer outBuffer = new BinaryWriteBuffer(32);
        DataOutput        outData   = outBuffer.getBufferOutput();
        writeTest(list, outData);

        final Binary binData = outBuffer.toBinary();

        Thread[] athread = new Thread[2];
        for (int i = 0, c = athread.length; i < c; i++)
            {
            Runnable task = new Runnable()
                {
                public void run()
                    {
                    DataInput inData  = binData.getBufferInput();
                    try
                        {
                        readTest(list, inData);
                        }
                    catch (Exception e)
                        {
                        e.printStackTrace();
                        }
                    }
                };
            athread[i] = new Thread(task);
            athread[i].start();
            }
        for (int i = 0, c = athread.length; i < c; i++)
            {
            athread[i].join();
            }

        stop("binary; used " + binData.length() + " bytes");
        }

    static void readTest(List list, DataInput in) throws Exception
        {
        for (int i = 0, c = list.size(); i < c; ++i)
            {
            Step step = (Step) list.get(i);
            try
                {
                step.read(in);
                }
            catch (Exception e)
                {
                out("exception occurred while processing step #" + i + ": " + step);
                throw e;
                }
            }

        // should be empty
        try
            {
            in.readByte();
            throw new IllegalStateException("stream not exhausted!");
            }
        catch (EOFException e)
            {
            // this is expected == good
            }
        }

    static void writeTest(List list, DataOutput out) throws Exception
        {
        for (Iterator iter = list.iterator(); iter.hasNext(); )
            {
            Step step = (Step) iter.next();
            step.write(out);
            }
        }

    public abstract static class Step
        {
        public abstract void write(DataOutput out) throws IOException;
        public abstract void read(DataInput in) throws IOException;
        public abstract String toString();
        }

    public static class BooleanStep extends Step
        {
        public BooleanStep()
            {
            m_f = rnd.nextBoolean();
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeBoolean(m_f);
            }
        public void read(DataInput in) throws IOException
            {
            boolean f = in.readBoolean();
            if (f != m_f)
                {
                throw new IllegalStateException("read boolean " + f + " but expecting " + m_f);
                }
            }
        public String toString()
            {
            return "BooleanStep " + m_f;
            }
        boolean m_f;
        }

    public static class ByteStep extends Step
        {
        public ByteStep()
            {
            // 50% chance of a common byte, such as those around 0
            if (rnd.nextBoolean())
                {
                m_b = COMMON[rnd.nextInt(COMMON.length)];
                }
            else
                {
                m_b = (byte) rnd.nextInt();
                }
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeByte(m_b);
            }
        public void read(DataInput in) throws IOException
            {
            byte b = in.readByte();
            if (b != m_b)
                {
                throw new IllegalStateException("read byte " + b + " but expecting " + m_b);
                }
            }
        public String toString()
            {
            return "ByteStep " + m_b;
            }
        static final byte[] COMMON = new byte[] {0, 1, 2, 3, -1,
            Byte.MIN_VALUE, Byte.MAX_VALUE, };
        byte m_b;
        }

    public static class CharStep extends Step
        {
        public CharStep()
            {
            if (rnd.nextBoolean())
                {
                // 50% chance of a common char, such as those around 0
                m_ch = COMMON[rnd.nextInt(COMMON.length)];
                }
            else if (rnd.nextBoolean())
                {
                // 25% chance of being ASCII
                m_ch = (char) (rnd.nextInt(127 - 32) + 32);
                }
            else
                {
                // 25% chance of being any unicode char
                m_ch = (char) rnd.nextInt();
                }
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeChar(m_ch);
            }
        public void read(DataInput in) throws IOException
            {
            char ch = in.readChar();
            if (ch != m_ch)
                {
                throw new IllegalStateException("read char " + ch + " but expecting " + m_ch);
                }
            }
        public String toString()
            {
            return "CharStep " + m_ch;
            }
        static final char[] COMMON = new char[] {0, 1, 2, 3,
            'a', 'e', 'i', 'o', 'u', '~', ' ',
            Character.MIN_VALUE, Character.MAX_VALUE, };
        char m_ch;
        }

    public static class ShortStep extends Step
        {
        public ShortStep()
            {
            // 50% chance of a common short, such as those around 0
            if (rnd.nextBoolean())
                {
                m_n = COMMON[rnd.nextInt(COMMON.length)];
                }
            else
                {
                m_n = (short) rnd.nextInt();
                }
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeShort(m_n);
            }
        public void read(DataInput in) throws IOException
            {
            short n = in.readShort();
            if (n != m_n)
                {
                throw new IllegalStateException("read short " + n + " but expecting " + m_n);
                }
            }
        public String toString()
            {
            return "ShortStep " + m_n;
            }
        static final short[] COMMON = new short[] {0, 1, 2, 3, -1,
            -255, -256, -32767, -32768, 32767,
            Short.MIN_VALUE, Short.MAX_VALUE, };
        short m_n;
        }

    public static class IntStep extends Step
        {
        public IntStep()
            {
            // 50% chance of a common int, such as those around 0
            if (rnd.nextBoolean())
                {
                m_n = COMMON[rnd.nextInt(COMMON.length)];
                }
            else
                {
                m_n = rnd.nextInt();
                }
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeInt(m_n);
            }
        public void read(DataInput in) throws IOException
            {
            int n = in.readInt();
            if (n != m_n)
                {
                throw new IllegalStateException("read int " + n + " but expecting " + m_n);
                }
            }
        public String toString()
            {
            return "IntStep " + m_n;
            }
        static final int[] COMMON = new int[] {0, 1, 2, 3, -1,
            -255, -256, -32767, -32768, 32767, 32768, 65535, 65536,
            Integer.MIN_VALUE, Integer.MAX_VALUE, };
        int m_n;
        }

    public static class LongStep extends Step
        {
        public LongStep()
            {
            // 50% chance of a common long, such as those around 0
            if (rnd.nextBoolean())
                {
                m_l = COMMON[rnd.nextInt(COMMON.length)];
                }
            else
                {
                m_l = rnd.nextLong();
                }
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeLong(m_l);
            }
        public void read(DataInput in) throws IOException
            {
            long l = in.readLong();
            if (l != m_l)
                {
                throw new IllegalStateException("read long " + l + " but expecting " + m_l);
                }
            }
        public String toString()
            {
            return "LongStep " + m_l;
            }
        static final long[] COMMON = new long[] {0, 1, 2, 3, -1,
            -255, -256, -32767, -32768, 32767, 32768, 65535, 65536,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE,};
        long m_l;
        }

    public static class FloatStep extends Step
        {
        public FloatStep()
            {
            // 50% chance of a common float, such as those around 0
            if (rnd.nextBoolean())
                {
                m_fl = COMMON[rnd.nextInt(COMMON.length)];
                }
            else
                {
                m_fl = rnd.nextFloat();
                }
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeFloat(m_fl);
            }
        public void read(DataInput in) throws IOException
            {
            float fl = in.readFloat();
            if (fl != m_fl)
                {
                // NaN check
                if (!(Float.isNaN(fl) && Float.isNaN(m_fl)))
                    {
                    throw new IllegalStateException("read float " + fl + " but expecting " + m_fl);
                    }
                }
            }
        public String toString()
            {
            return "FloatStep " + m_fl;
            }
        static final float[] COMMON = new float[] {0.0f, 1.0f, -1.0f, 0.1f, 2f, 3f,
            Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
            };
        float m_fl;
        }

    public static class DoubleStep extends Step
        {
        public DoubleStep()
            {
            // 50% chance of a common double, such as those around 0
            if (rnd.nextBoolean())
                {
                m_dfl = COMMON[rnd.nextInt(COMMON.length)];
                }
            else
                {
                m_dfl = rnd.nextDouble();
                }
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeDouble(m_dfl);
            }
        public void read(DataInput in) throws IOException
            {
            double dfl = in.readDouble();
            if (dfl != m_dfl)
                {
                // NaN check
                if (!(Double.isNaN(dfl) && Double.isNaN(m_dfl)))
                    {
                    throw new IllegalStateException("read double " + dfl + " but expecting " + m_dfl);
                    }
                }
            }
        public String toString()
            {
            return "DoubleStep " + m_dfl;
            }
        static final double[] COMMON = new double[] {0.0d, 1.0d, -1.0d, 0.1d, 2d, 3d,
            Double.MAX_VALUE, Double.MIN_VALUE, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            };
        double m_dfl;
        }

    public static class StringStep extends Step
        {
        public StringStep()
            {
            // 50% chance of a common string
            if (rnd.nextBoolean())
                {
                m_s = COMMON[rnd.nextInt(COMMON.length)];
                }
            else
                {
                int nType  = rnd.nextInt(3);
                int    cch = rnd.nextInt(512);
                char[] ach = new char[cch];
                for (int i = 0; i < cch; ++i)
                    {
                    switch (nType)
                        {
                        case 0:
                            // ascii
                            ach[i] = (char) (rnd.nextInt(127 - 32) + 32);
                            break;
                        case 1:
                            // random
                           ach[i] = (char) rnd.nextInt(65536);
                           break;
                        case 2:
                            // 50% ascii front/non-ascii tail
                            ach[i] = i < cch/2 || rnd.nextBoolean() ?
                                (char) (rnd.nextInt(127 - 32) + 32) :
                                (char) rnd.nextInt(65536);
                            break;
                        }
                    }
                m_s = new String(ach);
                }
            }
        public void write(DataOutput out) throws IOException
            {
            out.writeUTF(m_s);
            }
        public void read(DataInput in) throws IOException
            {
            String s = in.readUTF();
            if (!s.equals(m_s))
                {
                throw new IllegalStateException("\nread string " + s +
                                                "\nexpecting   " + m_s);
                }
            }
        public String toString()
            {
            return "StringStep " + m_s;
            }
        static final String[] COMMON = new String[] {"", " ", "\u0000", "\u0000\u0000", };
        String m_s;
        }

    static final Class[] STEP_CLASSES = new Class[]
        {
        BooleanStep.class,
        ByteStep.class,
        CharStep.class,
        ShortStep.class,
        IntStep.class,
        LongStep.class,
        FloatStep.class,
        DoubleStep.class,
        StringStep.class,
        };

    static void start()
        {
        lStart = System.currentTimeMillis();
        }
    static void stop(String s)
        {
        long lStop = System.currentTimeMillis();
        long lElapsed = lStop - lStart;
        out(s + " (" + lElapsed + "ms)");
        }
    static long lStart;
    static Random rnd = new Random();
    }