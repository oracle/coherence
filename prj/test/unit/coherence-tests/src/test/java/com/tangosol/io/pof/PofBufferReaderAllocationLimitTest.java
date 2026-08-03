/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.SerializationLimitPolicy;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import org.junit.Test;

import java.io.IOException;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for POF reader allocation and structural limits.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
public class PofBufferReaderAllocationLimitTest
        implements PofConstants
    {
    @Test
    public void shouldRejectElementCountBeforePrimitiveArrayAllocation()
            throws IOException
        {
        PofBufferReader reader = reader(limits(64, 2, 1), out ->
            {
            out.writePackedInt(T_ARRAY);
            out.writePackedInt(3);
            });

        IOException thrown = assertThrows(IOException.class, () -> reader.readByteArray(-1));
        assertTrue(thrown.getMessage().contains("byte array element count"));
        }

    @Test
    public void shouldRejectTypedObjectArrayCountBeforeFactoryInvocation()
            throws IOException
        {
        AtomicBoolean   fInvoked = new AtomicBoolean();
        PofBufferReader reader   = reader(limits(64, 2, 1), out ->
            {
            out.writePackedInt(T_ARRAY);
            out.writePackedInt(3);
            });

        assertThrows(IOException.class, () -> reader.readArray(-1, c ->
            {
            fInvoked.set(true);
            return new String[c];
            }));
        assertFalse(fInvoked.get());
        }

    @Test
    public void shouldRejectMapEntryCountBeforeReadingEntries()
            throws IOException
        {
        PofBufferReader reader = reader(limits(64, 8, 1), out ->
            {
            out.writePackedInt(T_MAP);
            out.writePackedInt(2);
            });

        IOException thrown = assertThrows(IOException.class, () -> reader.readMap(-1, new HashMap<>()));
        assertTrue(thrown.getMessage().contains("map entry count"));
        }

    @Test
    public void shouldRejectDeclaredByteLengthBeyondRemainingInput()
            throws IOException
        {
        PofBufferReader reader = reader(limits(64, 8, 8), out ->
            {
            out.writePackedInt(T_OCTET_STRING);
            out.writePackedInt(3);
            out.writeByte(1);
            out.writeByte(2);
            });

        IOException thrown = assertThrows(IOException.class, () -> reader.readBinary(-1));
        assertTrue(thrown.getMessage().contains("remaining input"));
        }

    @Test
    public void shouldReadBinaryFromSuppliedInput()
            throws IOException
        {
        TestReader reader = reader(limits(64, 8, 8), out -> out.writeByte(0));
        ReadBuffer  input  = buffer(out ->
            {
            out.writePackedInt(3);
            out.write(new byte[] {1, 2, 3});
            });

        assertEquals(new Binary(new byte[] {1, 2, 3}), reader.readBinaryForTest(input.getBufferInput()));
        }

    @Test
    public void shouldRejectDerivedByteCountOverflowBeforeAllocation()
            throws IOException
        {
        PofBufferReader reader = reader(new SerializationLimitPolicy(null, null, null), out ->
            {
            out.writePackedInt(T_UNIFORM_ARRAY);
            out.writePackedInt(T_OCTET);
            out.writePackedInt(Integer.MAX_VALUE);
            });

        IOException thrown = assertThrows(IOException.class, () -> reader.readCharArray(-1));
        assertTrue(thrown.getMessage().contains("byte count overflow"));
        }

    @Test
    public void shouldRejectSparseIndexOutsideLogicalLength()
            throws IOException
        {
        PofBufferReader reader = reader(limits(64, 8, 8), out ->
            {
            out.writePackedInt(T_SPARSE_ARRAY);
            out.writePackedInt(2);
            out.writePackedInt(2);
            out.writePackedInt(V_INT_1);
            out.writePackedInt(-1);
            });

        IOException thrown = assertThrows(IOException.class, () -> reader.readIntArray(-1));
        assertTrue(thrown.getMessage().contains("index out of range"));
        }

    @Test
    public void shouldAllowSparseLongArrayIndexBeyondDeclaredLogicalLength()
            throws IOException
        {
        PofBufferReader reader = reader(limits(64, 8, 8), out ->
            {
            out.writePackedInt(T_UNIFORM_SPARSE_ARRAY);
            out.writePackedInt(T_CHAR_STRING);
            out.writePackedInt(5);
            out.writePackedInt(5);
            out.writeSafeUTF("pet");
            out.writePackedInt(-1);
            });

        LongArray array = reader.readLongArray(-1, new SparseArray());
        assertEquals("pet", array.get(5));
        }

    @Test
    public void shouldRejectSparseEntriesPastDeclaredLogicalLength()
            throws IOException
        {
        PofBufferReader reader = reader(limits(64, 8, 8), out ->
            {
            out.writePackedInt(T_SPARSE_ARRAY);
            out.writePackedInt(1);
            out.writePackedInt(0);
            out.writePackedInt(V_INT_1);
            out.writePackedInt(0);
            out.writePackedInt(V_INT_1);
            });

        IOException thrown = assertThrows(IOException.class, () -> reader.readIntArray(-1));
        assertTrue(thrown.getMessage().contains("too many entries"));
        }

    private static SerializationLimitPolicy limits(long cbMax, int cElements, int cMapEntries)
        {
        return new SerializationLimitPolicy(cbMax, cElements, cMapEntries);
        }

    private static TestReader reader(SerializationLimitPolicy policy, Writer writer)
            throws IOException
        {
        SimplePofContext context = new SimplePofContext();
        context.setLimitPolicy(policy);
        return new TestReader(buffer(writer).getBufferInput(), context);
        }

    private static ReadBuffer buffer(Writer writer)
            throws IOException
        {
        ByteArrayWriteBuffer     buffer = new ByteArrayWriteBuffer(64);
        WriteBuffer.BufferOutput out    = buffer.getBufferOutput();

        writer.write(out);
        return buffer.getReadBuffer();
        }

    @FunctionalInterface
    private interface Writer
        {
        void write(WriteBuffer.BufferOutput out)
                throws IOException;
        }

    private static class TestReader
            extends PofBufferReader
        {
        private TestReader(ReadBuffer.BufferInput in, PofContext context)
            {
            super(in, context);
            }

        private Binary readBinaryForTest(ReadBuffer.BufferInput in)
                throws IOException
            {
            return readBinary(in);
            }
        }
    }
