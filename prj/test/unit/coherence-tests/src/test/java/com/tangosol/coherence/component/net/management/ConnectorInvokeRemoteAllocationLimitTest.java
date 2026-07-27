/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.management;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Connector.InvokeRemote} parameter allocation limits.
 *
 * @author OpenAI  2026.05.21
 * @since 26.04
 */
public class ConnectorInvokeRemoteAllocationLimitTest
    {
    @Test
    public void shouldDeserializeZeroInvokeParameters()
            throws IOException
        {
        Connector.InvokeRemote task = readInvoke(input(out ->
            {
            writeInvokeHeader(out, 0);
            out.writeBoolean(false);
            }));

        assertEquals(Connector.InvokeRemote.ACTION_INVOKE, task.getAction());
        assertEquals("Coherence:type=Test", task.getName());
        assertEquals("test", task.getMethodName());
        assertNull(task.getParameters());
        assertNull(task.getSignatures());
        }

    @Test
    public void shouldDeserializeSmallInvokeParameters()
            throws IOException
        {
        Connector.InvokeRemote expected = new Connector.InvokeRemote();
        expected.setAction(Connector.InvokeRemote.ACTION_INVOKE);
        expected.setName("Coherence:type=Test");
        expected.setMethodName("test");
        expected.setParameters(new Object[] {"one", Integer.valueOf(2)});
        expected.setSignatures(new String[] {String.class.getName(), Integer.class.getName()});

        Connector.InvokeRemote actual = readInvoke(writeInvoke(expected));

        assertArrayEquals(expected.getParameters(), actual.getParameters());
        assertArrayEquals(expected.getSignatures(), actual.getSignatures());
        }

    @Test
    public void shouldDeserializeZeroInvokeSignatures()
            throws IOException
        {
        Connector.InvokeRemote task = readInvoke(input(out ->
            {
            writeInvokeHeader(out, 0);
            out.writeBoolean(true);
            ExternalizableHelper.writeInt(out, 0);
            }));

        assertEquals(Connector.InvokeRemote.ACTION_INVOKE, task.getAction());
        assertNull(task.getParameters());
        assertArrayEquals(new String[0], task.getSignatures());
        }

    @Test
    public void shouldRejectNegativeInvokeParameterCount()
            throws IOException
        {
        IOException thrown = assertThrows(IOException.class, () -> readInvoke(input(out -> writeInvokeHeader(out, -1))));

        assertTrue(thrown.getMessage().contains("parameter count is negative"));
        }

    @Test
    public void shouldRejectInvokeParameterCountExceedingProductCap()
            throws IOException
        {
        IOException thrown = assertThrows(IOException.class, () -> readInvoke(input(out -> writeInvokeHeader(out, 100000))));

        assertTrue(thrown.getMessage().contains("parameter count exceeds maximum"));
        }

    @Test
    public void shouldRejectAcceptedInvokeParameterCountRejectedBySerialFilter()
            throws IOException
        {
        ReadBuffer.BufferInput in = input(out -> writeInvokeHeader(out, 3));
        in.setObjectInputFilter(maxArrayFilter(2));

        InvalidClassException thrown = assertThrows(InvalidClassException.class, () -> readInvoke(in));

        assertTrue(thrown.getMessage().contains("array length 3"));
        }

    @Test
    public void shouldRejectNegativeInvokeSignatureCount()
            throws IOException
        {
        IOException thrown = assertThrows(IOException.class, () -> readInvoke(input(out ->
            {
            writeInvokeHeader(out, 0);
            out.writeBoolean(true);
            ExternalizableHelper.writeInt(out, -1);
            })));

        assertTrue(thrown.getMessage().contains("signature count is negative"));
        }

    @Test
    public void shouldRejectInvokeSignatureCountExceedingProductCap()
            throws IOException
        {
        IOException thrown = assertThrows(IOException.class, () -> readInvoke(input(out ->
            {
            writeInvokeHeader(out, 0);
            out.writeBoolean(true);
            ExternalizableHelper.writeInt(out, 100000);
            })));

        assertTrue(thrown.getMessage().contains("signature count exceeds maximum"));
        }

    @Test
    public void shouldRejectAcceptedInvokeSignatureCountRejectedBySerialFilter()
            throws IOException
        {
        ReadBuffer.BufferInput in = input(out ->
            {
            writeInvokeHeader(out, 0);
            out.writeBoolean(true);
            ExternalizableHelper.writeInt(out, 3);
            });
        in.setObjectInputFilter(maxArrayFilter(2));

        InvalidClassException thrown = assertThrows(InvalidClassException.class, () -> readInvoke(in));

        assertTrue(thrown.getMessage().contains("array length 3"));
        }

    private static Connector.InvokeRemote readInvoke(ReadBuffer.BufferInput in)
            throws IOException
        {
        Connector.InvokeRemote task = new Connector.InvokeRemote();
        task.readExternal(in);
        return task;
        }

    private static ReadBuffer.BufferInput writeInvoke(Connector.InvokeRemote task)
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(256);
        task.writeExternal(buffer.getBufferOutput());
        return buffer.getReadBuffer().getBufferInput();
        }

    private static void writeInvokeHeader(WriteBuffer.BufferOutput out, int cParams)
            throws IOException
        {
        ExternalizableHelper.writeInt(out, Connector.InvokeRemote.ACTION_INVOKE);
        ExternalizableHelper.writeSafeUTF(out, "Coherence:type=Test");
        ExternalizableHelper.writeSafeUTF(out, "test");
        ExternalizableHelper.writeInt(out, cParams);
        }

    private static ReadBuffer.BufferInput input(Writer writer)
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(128);
        writer.write(buffer.getBufferOutput());
        return buffer.getReadBuffer().getBufferInput();
        }

    private static ObjectInputFilter maxArrayFilter(int cMax)
        {
        return info -> info.arrayLength() > cMax
                       ? ObjectInputFilter.Status.REJECTED
                       : ObjectInputFilter.Status.ALLOWED;
        }

    @FunctionalInterface
    private interface Writer
        {
        void write(WriteBuffer.BufferOutput out)
                throws IOException;
        }
    }
