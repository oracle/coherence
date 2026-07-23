/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.SerializationLimitPolicy;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.io.pof.SimplePofContext;

import org.junit.Test;

import java.io.IOException;
import java.io.InvalidClassException;

import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AbstractScript} argument allocation limits.
 *
 * @author OpenAI  2026.05.21
 * @since 26.04
 */
public class AbstractScriptAllocationLimitTest
    {
    @Test
    public void shouldDeserializeZeroDataInputArguments()
            throws IOException
        {
        AbstractScript script = readDataInput(writeDataInput(new AbstractScript("js", "noArgs")));

        assertEquals("js", script.getLanguage());
        assertEquals("noArgs", script.getName());
        assertEquals(0, script.getArgs().length);
        }

    @Test
    public void shouldDeserializeSmallDataInputArguments()
            throws IOException
        {
        AbstractScript script = readDataInput(writeDataInput(new AbstractScript("js", "small", "one", 2)));

        assertEquals("js", script.getLanguage());
        assertEquals("small", script.getName());
        assertArrayEquals(new Object[] {"one", Integer.valueOf(2)}, script.getArgs());
        }

    @Test
    public void shouldRejectNegativeDataInputArgumentCount()
            throws IOException
        {
        IOException thrown = assertThrows(IOException.class, () -> readDataInput(input(out ->
            {
            out.writeUTF("js");
            out.writeUTF("bad");
            out.writeInt(-1);
            })));

        assertTrue(thrown.getMessage().contains("Unexpected number of script arguments"));
        }

    @Test
    public void shouldRejectLargeDataInputArgumentCount()
            throws IOException
        {
        IOException thrown = assertThrows(IOException.class, () -> readDataInput(input(out ->
            {
            out.writeUTF("js");
            out.writeUTF("bad");
            out.writeInt(256);
            })));

        assertTrue(thrown.getMessage().contains("Unexpected number of script arguments"));
        }

    @Test
    public void shouldRejectDataInputArgumentCountRejectedBySerialFilter()
            throws IOException
        {
        ReadBuffer.BufferInput in = input(out ->
            {
            out.writeUTF("js");
            out.writeUTF("bad");
            out.writeInt(3);
            });
        in.setObjectInputFilter(maxArrayFilter(2));

        InvalidClassException thrown = assertThrows(InvalidClassException.class, () -> readDataInput(in));

        assertTrue(thrown.getMessage().contains("array length 3"));
        }

    @Test
    public void shouldRejectOversizedPofArgumentArrayBySerializerLimits()
            throws IOException
        {
        ByteArrayWriteBuffer wb = new ByteArrayWriteBuffer(128);
        PofBufferWriter.UserTypeWriter writer = new PofBufferWriter.UserTypeWriter(
                wb.getBufferOutput(), new SimplePofContext(), 0, -1);

        writer.writeString(0, "js");
        writer.writeString(1, "pof");
        writer.writeObjectArray(2, new Object[] {"one", "two", "three"});
        writer.writeRemainder(null);

        SimplePofContext context = new SimplePofContext();
        context.setLimitPolicy(new SerializationLimitPolicy(null, 2, null));

        IOException thrown = assertThrows(IOException.class, () ->
            {
            AbstractScript script = new AbstractScript();
            script.readExternal(userTypeReader(wb, context));
            });

        assertTrue(thrown.getMessage().contains("exceeds maximum"));
        }

    private static AbstractScript readDataInput(ReadBuffer.BufferInput in)
            throws IOException
        {
        AbstractScript script = new AbstractScript();
        script.readExternal(in);
        return script;
        }

    private static ReadBuffer.BufferInput writeDataInput(AbstractScript script)
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(256);
        script.writeExternal(buffer.getBufferOutput());
        return buffer.getReadBuffer().getBufferInput();
        }

    private static ReadBuffer.BufferInput input(Writer writer)
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(128);
        writer.write(buffer.getBufferOutput());
        return buffer.getReadBuffer().getBufferInput();
        }

    private static PofBufferReader.UserTypeReader userTypeReader(ByteArrayWriteBuffer wb, SimplePofContext context)
            throws IOException
        {
        ReadBuffer.BufferInput in       = wb.getReadBuffer().getBufferInput();
        int                    nType    = in.readPackedInt();
        int                    nVersion = in.readPackedInt();
        return new PofBufferReader.UserTypeReader(in, context, nType, nVersion);
        }

    private static Object maxArrayFilter(int cMax)
        {
        String sFilter = "maxarray=" + cMax + ";!*";
        String[] asConfig = {"java.io.ObjectInputFilter$Config", "sun.misc.ObjectInputFilter$Config"};
        for (int i = 0; i < asConfig.length; i++)
            {
            try
                {
                Class<?> clzConfig = Class.forName(asConfig[i]);
                Method   method    = clzConfig.getDeclaredMethod("createFilter", String.class);
                method.setAccessible(true);
                return method.invoke(null, sFilter);
                }
            catch (ReflectiveOperationException | LinkageError ignored)
                {
                }
            }
        return null;
        }

    @FunctionalInterface
    private interface Writer
        {
        void write(WriteBuffer.BufferOutput out)
                throws IOException;
        }
    }
