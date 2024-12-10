/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.io.nio.ByteBufferWriteBuffer;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
* Generic test for ReadBuffer and WriteBuffer implementations.
*
* @author cp  2006.04.18
*/
public class BufferTest
        extends Base
    {
    public static void main(String[] asArg)
        {
        // tests of WriteBuffer
        for (int cbMax = 100; cbMax <= 2000; cbMax += 253)
            {
            out("testSequentialWrites(" + cbMax + ")");
            for (int i = 0; i < 10; ++i)
                {
                testSequentialWrites(getTestBuffers(cbMax), cbMax, 10000);
                }
            }

        // tests of ReadBuffer
        for (int cbMax = 20, cTests = 5000; cbMax <= 20000; cbMax *= 10, cTests /= 2)
            {
            out("testSequentialReads(" + cbMax + ")");
            for (int i = 0; i < cTests; ++i)
                {
                testSequentialReads(getTestBuffers(cbMax), cbMax);
                }
            }

        out("testReads(200)");
        testReads(getTestBuffers(200), 200);

        out("testReads(20)");
        testReads(getTestBuffers(20), 20);

        // testRandomAccess(getTestBuffers(cbMax), cbMax, 1000000);
        }

    /**
    * Create some WriteBuffer objects to test.
    *
    * @param cbMax  size of the write buffers to create
    *
    * @return an array of WriteBuffer objects to test
    */
    public static WriteBuffer[] getTestBuffers(final int cbMax)
        {
        return new WriteBuffer[]
            {
            new BinaryWriteBuffer(cbMax, cbMax),
            new ByteArrayWriteBuffer(cbMax, cbMax),
            new ByteBufferWriteBuffer(ByteBuffer.allocate(cbMax)),
            new ByteBufferWriteBuffer(ByteBuffer.allocateDirect(cbMax)),
            new OldMultiBufferWriteBuffer(new MultiBufferWriteBuffer.WriteBufferPool()
                {
                public int getMaximumCapacity() {return cbMax;}
                public WriteBuffer allocate(int cbPreviousTotal)
                    {
                    return new BinaryWriteBuffer(cbMax / 10, cbMax / 10);
                    }
                public void release(WriteBuffer buffer) {}
                }),
            new MultiBufferWriteBuffer(new MultiBufferWriteBuffer.WriteBufferPool()
                {
                public int getMaximumCapacity() {return cbMax;}
                public WriteBuffer allocate(int cbPreviousTotal)
                    {
                    return new BinaryWriteBuffer(cbMax / 10, cbMax / 10);
                    }
                public void release(WriteBuffer buffer) {}
                }),
/*
            new MultiBufferWriteBuffer(new MultiBufferWriteBuffer.WriteBufferPool()
                {
                public int getMaximumCapacity() {return cbMax;}
                public WriteBuffer allocate(int cbPreviousTotal)
                    {
                    return new ByteArrayWriteBuffer(cbMax / 10, cbMax / 10);
                    }
                public void release(WriteBuffer buffer) {}
                }),
            new MultiBufferWriteBuffer(new MultiBufferWriteBuffer.WriteBufferPool()
                {
                public int getMaximumCapacity() {return cbMax;}
                public WriteBuffer allocate(int cbPreviousTotal)
                    {
                    return new ByteBufferWriteBuffer(ByteBuffer.allocate(cbMax / 10));
                    }
                public void release(WriteBuffer buffer) {}
                }),
            new MultiBufferWriteBuffer(new MultiBufferWriteBuffer.WriteBufferPool()
                {
                public int getMaximumCapacity() {return cbMax;}
                public WriteBuffer allocate(int cbPreviousTotal)
                    {
                    return new ByteBufferWriteBuffer(ByteBuffer.allocateDirect(cbMax / 10));
                    }
                public void release(WriteBuffer buffer) {}
                }),
*/
            };
        }

    public static void testSequentialWrites(WriteBuffer[] abuf, int cbMax, int cIters)
        {
        int cBufs = abuf.length;
        WriteBuffer.BufferOutput[] aout = new WriteBuffer.BufferOutput[cBufs];
        for (int iBuf = 0; iBuf < cBufs; ++iBuf)
            {
            aout[iBuf] = abuf[iBuf].getBufferOutput();
            }

        StreamOp opPrev = null;
        boolean fPrevOpWasOffset = false;
        for (int iIter = 0; iIter < cIters || fPrevOpWasOffset; ++iIter)
            {
            StreamOp op = rndStreamOp(cbMax);

            // don't allow two offset ops in a row
            if (op instanceof OffsetOp)
                {
                if (fPrevOpWasOffset)
                    {
                    continue;
                    }
                fPrevOpWasOffset = true;
                }
            else
                {
                if (aout[0].getOffset() + op.getSize() > aout[0].getBuffer().getCapacity())
                    {
                    if (fPrevOpWasOffset)
                        {
                        op = new ByteOp();
                        op.init(cbMax);
                        fPrevOpWasOffset = false;
                        }
                    else
                        {
                        op = new OffsetOp();
                        op.init(cbMax);
                        fPrevOpWasOffset = true;
                        }
                    }
                else
                    {
                    fPrevOpWasOffset = false;
                    }
                }

            for (int iBuf = 0; iBuf < cBufs; ++iBuf)
                {
                op.write(aout[iBuf]);
                }

            if (!fPrevOpWasOffset)
                {
                compareIntermediate(abuf, op, opPrev);
                }

            opPrev = op;
            }

        compare(abuf);
        }

    public static void testSequentialReads(WriteBuffer[] abuf, int cbMax)
        {
        // build list of test ops
        List listOps = new ArrayList();
        int cbEach  = cbMax; // / 10;
        int cbTotal = 0;
        while (true)
            {
            StreamOp op = rndStreamOp(cbEach);
            if (op instanceof OffsetOp)
                {
                continue;
                }

            if (cbTotal + op.getSize() > cbMax)
                {
                break;
                }

            listOps.add(op);
            cbTotal += op.getSize();
            }

        // perform the write operations
        int cBuffers = abuf.length;
        WriteBuffer.BufferOutput[] aout = new WriteBuffer.BufferOutput[cBuffers];
        for (int iBuf = 0; iBuf < cBuffers; ++iBuf)
            {
            aout[iBuf] = abuf[iBuf].getBufferOutput();
            }
        for (Iterator iter = listOps.iterator(); iter.hasNext(); )
            {
            StreamOp op = (StreamOp) iter.next();
            for (int iBuf = 0; iBuf < cBuffers; ++iBuf)
                {
                op.write(aout[iBuf]);
                }
            }
        compare(abuf);

        // convert to read buffers
        ReadBuffer[] abufRead = new ReadBuffer[cBuffers];
        for (int i = 0; i < cBuffers; ++i)
            {
            abufRead[i] = abuf[i].getUnsafeReadBuffer();
            }

        int nStep = testSequentialReadsUpToBreak(abufRead, cbMax, listOps, -1);
        if (nStep >= 0)
            {
            testSequentialReadsUpToBreak(abufRead, cbMax, listOps, nStep);
            }
        }

    public static int testSequentialReadsUpToBreak(ReadBuffer[] abufRead, int cbMax, List listOps, int nBreakStep)
        {
        int cBuffers = abufRead.length;
        ReadBuffer.BufferInput[] ain = new ReadBuffer.BufferInput[cBuffers];
        for (int iBuf = 0; iBuf < cBuffers; ++iBuf)
            {
            ain[iBuf] = abufRead[iBuf].getBufferInput();
            }

        for (int nCurStep = 0, cSteps = listOps.size(); nCurStep < cSteps; ++nCurStep)
            {
            StreamOp op = (StreamOp) listOps.get(nCurStep);
            if (nCurStep == nBreakStep)
                {
                int x = 1; // place breakpoint here
                }

            int    of1      = ain[0].getOffset();
            Object oResult1 = op.read(ain[0]);

            for (int iBuf = 1; iBuf < cBuffers; ++iBuf)
                {
                int of2 = ain[iBuf].getOffset();
                if (of1 != of2)
                    {
                    out("offset mismatch! control of=" + of1 + ", compare of=" + of2);
                    return nCurStep - 1;
                    }

                Object oResult2 = op.read(ain[iBuf]);
                if (!equals(oResult1, oResult2))
                    {
                    out("step=" + nCurStep + ", of=" + of1 + ", op=" + op);
                    if (nCurStep > 0)
                        {
                        out("previous op=" + listOps.get(nCurStep-1));
                        }

                    out("control buffer=" + abufRead[0].getClass().getName()
                        + ", compare buffer=" + abufRead[iBuf].getClass().getName());
                    out(abufRead[0].toBinary());
                    out(abufRead[iBuf].toBinary());
                    out("control result=" + oResult1 + " (of=" + of1 + ")");
                    out("compare result=" + oResult2 + " (of=" + of2 + ")");

                    return nCurStep;
                    }
                }
            }

        return -1; // no problem
        }

    public static void testReads(WriteBuffer[] abuf, int cbMax)
        {
        // perform the write operations
        int cBuffers = abuf.length;
        WriteBuffer.BufferOutput[] aout = new WriteBuffer.BufferOutput[cBuffers];
        for (int iBuf = 0; iBuf < cBuffers; ++iBuf)
            {
            aout[iBuf] = abuf[iBuf].getBufferOutput();
            }
        for (int iBuf = 0; iBuf < cBuffers; ++iBuf)
            {
            try
                {
                aout[iBuf].writeUTF("hello");
                aout[iBuf].writeUTF("world");
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        compare(abuf);

        // perform the read operations, comparing the results of each
        ReadBuffer[] abufRead = new ReadBuffer[cBuffers];
        for (int i = 0; i < cBuffers; ++i)
            {
            abufRead[i] = abuf[i].getUnsafeReadBuffer();
            }
        ReadBuffer.BufferInput[] ain = new ReadBuffer.BufferInput[cBuffers];
        for (int iBuf = 0; iBuf < cBuffers; ++iBuf)
            {
            ain[iBuf] = abufRead[iBuf].getBufferInput();
            }
        for (int iBuf = 0; iBuf < cBuffers; ++iBuf)
            {
            try
                {
                String s1 = ain[iBuf].readUTF();
                String s2 = ain[iBuf].readUTF();
                azzert(s1.equals("hello") && s2.equals("world"));
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    public static void testRandomAccess(WriteBuffer[] abuf, int cbMax, int cIters)
        {
        int cBufs = abuf.length;
        for (int iIter = 0; iIter < cIters; ++iIter)
            {
            BufferOp op = rndBufferOp(cbMax);
            for (int iBuf = 0; iBuf < cBufs; ++iBuf)
                {
                op.write(abuf[iBuf]);
                }
            }

        compare(abuf);
        }

    public static void compareIntermediate(WriteBuffer[] abuf, StreamOp op, StreamOp opPrev)
        {
        int cBuffers = abuf.length;
        ReadBuffer[] abufRead = new ReadBuffer[cBuffers];
        for (int i = 0; i < cBuffers; ++i)
            {
            abufRead[i] = abuf[i].getUnsafeReadBuffer();
            }
        try
            {
            compare(abufRead);
            }
        catch (RuntimeException e)
            {
            out("op: " + op + " (" + op.getClass().getName() + ")");
            if (opPrev != null)
                {
                out("prev: " + opPrev + " (" + opPrev.getClass().getName() + ")");
                }
            throw e;
            }
        }

    public static void compare(WriteBuffer[] abuf)
        {
        int cBuffers = abuf.length;

        WriteBuffer buf = abuf[0];
        int         cb  = buf.length();
        Binary      bin = buf.toBinary();
        for (int i = 1, c = abuf.length; i < c; ++i)
            {
            buf = abuf[i];
            try
                {
                azzert(cb == buf.length());
                azzert(bin.equals(buf.toBinary()));
                }
            catch (RuntimeException e)
                {
                reportDifference(abuf, i, e);
                }
            }

        ReadBuffer[] abufRead = new ReadBuffer[cBuffers];
        for (int i = 0; i < cBuffers; ++i)
            {
            abufRead[i] = abuf[i].getReadBuffer();
            }
        compare(abufRead);

        for (int i = 0; i < cBuffers; ++i)
            {
            abufRead[i] = abuf[i].getUnsafeReadBuffer();
            }
        compare(abufRead);
        }

    public static void compare(ReadBuffer[] abuf)
        {
        ReadBuffer buf = abuf[0];
        int        cb  = buf.length();
        Binary     bin = buf.toBinary();
        for (int i = 1, c = abuf.length; i < c; ++i)
            {
            buf = abuf[i];
            try
                {
                azzert(cb == buf.length());
                azzert(bin.equals(buf.toBinary()));
                }
            catch (RuntimeException e)
                {
                reportDifference(abuf, i, e);
                }
            }
        }

    static void reportDifference(WriteBuffer[] abuf, int iBuf, RuntimeException e)
        {
        out("Comparison failed between " + abuf[0].getClass().getName()
            + " and " + abuf[iBuf].getClass().getName());
        out(abuf[0].toBinary());
        out(abuf[iBuf].toBinary());
        throw e;
        }
    static void reportDifference(ReadBuffer[] abuf, int iBuf, RuntimeException e)
        {
        out("Comparison failed between " + abuf[0].getClass().getName()
            + " and " + abuf[iBuf].getClass().getName());
        out(abuf[0].toBinary());
        out(abuf[iBuf].toBinary());
        throw e;
        }

    // ----- test operations ------------------------------------------------

    public static BufferOp rndBufferOp(int cbCap)
        {
        Class[] aclz = BUFFER_OPS;
        BufferOp op;
        try
            {
            op = (BufferOp) aclz[getRandom().nextInt(aclz.length)].newInstance();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        op.init(cbCap);
        return op;
        }

    static Class[] BUFFER_OPS = new Class[]
        {
        ByteAtOp.class,
        };

    /**
    * Base class for buffer test operations.
    */
    public static abstract class BufferOp
            extends Base
        {
        public BufferOp()
            {
            }

        public void init(int cbCap)
            {
            cbMax = cbCap;
            of    = getRandom().nextInt(cbCap - getSize());
            }

        public int getSize() {return 1;}
        public Object read(ReadBuffer buf) {return null;}
        public void write(WriteBuffer buf) {}

        public String toString()
            {
            String sClass = getClass().getName();
            sClass = sClass.substring(sClass.lastIndexOf('$') + 1);
            return sClass + " of=" + of + ", " + getDescription();
            }
        public abstract String getDescription();

        protected int cbMax;
        protected int of;
        }

    // TODO length()

    /**
    * Single-byte operation.
    */
    public static class ByteAtOp
            extends BufferOp
        {
        public Object read(ReadBuffer buf)
            {
            try
                {
                return buf.byteAt(of);
                }
            catch (IndexOutOfBoundsException e)
                {
                return "IndexOutOfBoundsException";
                }
            }

        public void write(WriteBuffer buf)
            {
            buf.write(of, b);
            }

        public String getDescription()
            {
            return "byte=" + toHexEscape(b);
            }

        protected byte b = (byte) getRandom().nextInt(256);
        }

    // TODO ReadBuffer
    //  copyBytes(..)
    //  readBuffer(int, int)
    //  toByteArray()
    //  toByteArray(int, int)
    //  toBinary
    //  toBinary(int, int)

    // TODO WriteBuffer
    //    write(int ofDest, byte f);
    //    write(int ofDest, byte[] abSrc);
    //    write(int ofDest, byte[] abSrc, int ofSrc, int cbSrc);
    //    write(int ofDest, ReadBuffer bufSrc);
    //    write(int ofDest, ReadBuffer bufSrc, int ofSrc, int cbSrc);
    //    write(int ofDest, InputStreaming stream)
    //    write(int ofDest, InputStreaming stream, int cbSrc)
    //    int length();
    //    retain(int of);
    //    retain(int of, int cb);
    //    clear();
    //    int getCapacity();
    //    int getMaximumCapacity();
    //    WriteBuffer getWriteBuffer(int of);
    //    WriteBuffer getWriteBuffer(int of, int cb);
    //    BufferOutput getBufferOutput();
    //    BufferOutput getBufferOutput(int of);
    //    BufferOutput getAppendingBufferOutput();
    //    ReadBuffer getReadBuffer();
    //    ReadBuffer getUnsafeReadBuffer();
    //    byte[] toByteArray();
    //    Binary toBinary();


    // ----- BufferInput / BufferOutput operations --------------------------

    /**
    * Create a random operation to inflict upon a BufferInput or BufferOutput
    * object.
    *
    * @param cbCap  the capacity / size of the underlying buffer
    *
    * @return a StreamOp instance
    */
    public static StreamOp rndStreamOp(int cbCap)
        {
        Class[] aclz = STREAM_OPS;
        StreamOp op;
        try
            {
            op = (StreamOp) aclz[getRandom().nextInt(aclz.length)].newInstance();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        op.init(cbCap);
        return op;
        }

    /**
    * All StreamOp implementing classes.
    */
    static Class[] STREAM_OPS = new Class[]
        {
        StreamByteOp.class,
                /*
        StreamByteArrayOp.class,
        StreamPartialByteArrayOp.class,
        BooleanOp.class,
        ByteOp.class,
        UnsignedByteOp.class,
        ShortOp.class,
        UnsignedShortOp.class,
        CharOp.class,
        IntOp.class,
        LongOp.class,
        FloatOp.class,
        DoubleOp.class,
        UtfOp.class,
        SafeUtfOp.class,
        PackedIntOp.class,
        PackedLongOp.class,
        FullBufferOp.class,
        PartialBufferOp.class,
        FullStreamOp.class,
        PartialStreamOp.class,
        OffsetOp.class,
        */
        };

    /**
    * Base class for buffer input/output test operations.
    */
    public static abstract class StreamOp
            extends Base
        {
        public StreamOp()
            {
            }

        public void init(int cbCap)
            {
            cbMax = cbCap;
            }

        public int getSize()
            {
            return 1;
            }

        public Object read(ReadBuffer.BufferInput in)
            {
            try
                {
                return readInternal(in);
                }
            catch (Exception e)
                {
                out(e);
                return e.getMessage();
                }
            }

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            return null;
            }

        public void write(WriteBuffer.BufferOutput out)
            {
            try
                {
                writeInternal(out);
                }
            catch (Exception e)
                {
                err(e);
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            }

        public String toString()
            {
            String sClass = getClass().getName();
            sClass = sClass.substring(sClass.lastIndexOf('$') + 1);
            return sClass + " " + getDescription();
            }

        public abstract String getDescription();

        protected int cbMax;
        }

    /**
    * Single-byte operation.
    */
    public static class StreamByteOp
            extends StreamOp
        {
        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.read();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.write(b);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Byte=" + toHexEscape(b);
            }

        protected byte b = (byte) getRandom().nextInt(256);
        }

    /**
    * Java "byte[]" operation.
    */
    public static class StreamByteArrayOp
            extends StreamOp
        {
        public int getSize() {return bin.length();}

        public void init(int cbCap)
            {
            super.init(cbCap);
            bin = new Binary(getRandomBinary(0, cbCap));
            }

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                byte[] ab = new byte[bin.length()];
                int    cb = in.read(ab);
                return new Binary(ab, 0, cb);
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.write(bin.toByteArray());
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Binary=" + bin.toString();
            }

        Binary bin;
        }

    /**
    * Java "partial byte[]" operation.
    */
    public static class StreamPartialByteArrayOp
            extends StreamByteArrayOp
        {
        public int getSize() {return cb;}

        public void init(int cbCap)
            {
            super.init(cbCap);
            int cbActual = bin.length();
            if (cbActual > 0)
                {
                cb = getRandom().nextInt(cbActual);
                of = getRandom().nextInt(cbActual - cb);
                }
            }

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                byte[] ab = new byte[bin.length()];
                int    cb = in.read(ab, of, this.cb);
                return new Binary(ab, of, cb);
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.write(bin.toByteArray(), of, cb);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return super.getDescription() + ", of=" + of + ", cb=" + cb;
            }

        int of;
        int cb;
        }

    // TODO readInternal operations
    //  skip
    //  available()
    //  mark
    //  reset
    //  readFullyByteArray
    //  readFullyPartialbyteArray
    //  skipBytes

    /**
    * Java "boolean" operation.
    */
    public static class BooleanOp
            extends StreamOp
        {
        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readBoolean();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeBoolean(f);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Boolean=" + Boolean.toString(f);
            }

        protected boolean f = getRandom().nextBoolean();
        }

    /**
    * Single-byte operation.
    */
    public static class ByteOp
            extends StreamByteOp
        {
        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readByte();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeByte(b);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * Single-byte operation.
    */
    public static class UnsignedByteOp
            extends ByteOp
        {
        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readUnsignedByte();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }
        }

    /**
    * Java "short" operation.
    */
    public static class ShortOp
            extends StreamOp
        {
        public int getSize() {return 2;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readShort();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeShort(n);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Short=" + n;
            }

        protected short n = (short) getRandom().nextInt(65536);
        }

    /**
    * Java unsigned "short" operation.
    */
    public static class UnsignedShortOp
            extends ShortOp
        {
        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readUnsignedShort();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }
        }

    /**
    * Java "short" operation.
    */
    public static class CharOp
            extends StreamOp
        {
        public int getSize() {return 2;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readChar();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeChar(ch);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Character='" + ch + "' (0x" + toHexString(ch, 4) + ")";
            }

        protected char ch = (char) getRandom().nextInt(65536);
        }

    /**
    * Java "int" operation.
    */
    public static class IntOp
            extends StreamOp
        {
        public int getSize() {return 4;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readInt();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeInt(n);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Int=" + n;
            }

        protected int n = getRandom().nextInt();
        }

    /**
    * Java "long" operation.
    */
    public static class LongOp
            extends StreamOp
        {
        public int getSize() {return 8;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readLong();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeLong(l);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Long=" + l;
            }

        protected long l = getRandom().nextLong();
        }

    /**
    * Java "float" operation.
    */
    public static class FloatOp
            extends StreamOp
        {
        public int getSize() {return 4;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readFloat();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeFloat(fl);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Float=" + fl + " (0x" + toHexString(Float.floatToIntBits(fl), 8) + ")";
            }

        protected float fl = getRandom().nextFloat();
        }

    /**
    * Java "double" operation.
    */
    public static class DoubleOp
            extends StreamOp
        {
        public int getSize() {return 8;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readDouble();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeDouble(dfl);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "Double=" + dfl + " (0x"
                   + toHexString((int) (Double.doubleToLongBits(dfl) >>> 32), 8)
                   + toHexString((int) Double.doubleToLongBits(dfl), 8) + ")";
            }

        protected double dfl = getRandom().nextDouble();
        }

    /**
    * Java "String" as UTF-8 operation.
    */
    public static class UtfOp
            extends StreamOp
        {
        public int getSize() {return 4 + s.length() * (fAscii ? 1 : 3);}

        public void init(int cbCap)
            {
            super.init(cbCap);
            fAscii = getRandom().nextBoolean();
            int cbMax = Math.max(0, fAscii ? cbCap - 4 : (cbCap - 4) / 3);
            s = getRandomString(0, cbMax, fAscii);
            }

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readUTF();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeUTF(s);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return "String=\"" + s + "\", length=" + s.length();
            }

        protected boolean fAscii;
        protected String s;
        }

    /**
    * Java "String" as safe UTF-8 operation.
    */
    public static class SafeUtfOp
            extends StreamOp
        {
        public int getSize() {return s == null ? 5 : (5 + s.length() * (fAscii ? 1 : 3));}

        public void init(int cbCap)
            {
            super.init(cbCap);

            if (getRandom().nextInt(5) != 0)
                {
                fAscii = getRandom().nextBoolean();
                int cbMax = Math.max(0, fAscii ? cbCap - 5 : (cbCap - 5) / 3);
                s = getRandomString(0, cbMax, fAscii);
                }
            }

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readSafeUTF();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeSafeUTF(s);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return s == null
                   ? "String=null"
                   : "String=\"" + s + "\", length=" + s.length();
            }

        protected boolean fAscii;
        protected String s;
        }

    /**
    * Coherence "packed int" operation.
    */
    public static class PackedIntOp
            extends IntOp
        {
        public int getSize() {return 5;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readPackedInt();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writePackedInt(n);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * Coherence "packed long" operation.
    */
    public static class PackedLongOp
            extends LongOp
        {
        public int getSize() {return 10;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readPackedLong();
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writePackedLong(l);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * Coherence "buffer" operation.
    */
    public static class FullBufferOp
            extends StreamByteArrayOp
        {
        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readBuffer(bin.length());
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeBuffer(bin);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * Coherence "partial buffer" operation.
    */
    public static class PartialBufferOp
            extends StreamPartialByteArrayOp
        {
        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readBuffer(cb);
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeBuffer(bin, of, cb);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * Coherence "write from stream" operation.
    */
    public static class FullStreamOp
            extends FullBufferOp
        {
        public int getSize() {return bin.length();}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readBuffer(bin.length());
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeStream(bin.getBufferInput());
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * Coherence "write certain number of bytes from stream" operation.
    */
    public static class PartialStreamOp
            extends FullBufferOp
        {
        public int getSize() {return cb;}

        public void init(int cbCap)
            {
            super.init(cbCap);
            if (bin.length() > 0)
                {
                cb = getRandom().nextInt(bin.length());
                }
            }

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                return in.readBuffer(cb);
                }
            catch (IOException e)
                {
                return "IOException";
                }
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                out.writeStream(bin.getBufferInput(), cb);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        public String getDescription()
            {
            return super.getDescription() + ", cb=" + cb;
            }

        int cb;
        }

    /**
    * Offset operation.
    */
    public static class OffsetOp
            extends StreamOp
        {
        public int getSize() {return 0;}

        public Object readInternal(ReadBuffer.BufferInput in)
                throws IOException
            {
            in.setOffset(of);
            return null;
            }

        public void writeInternal(WriteBuffer.BufferOutput out)
                throws IOException
            {
            out.setOffset(of);
            }

        public void init(int cbCap)
            {
            super.init(cbCap);
            of = getRandom().nextInt(cbCap / 2);
            }

        public String getDescription()
            {
            return "Offset=" + of;
            }

        int of;
        }
    }
