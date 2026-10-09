/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util.daemon.queueProcessor.service;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.SerializationLimitPolicy;
import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.WrapperDataInputStream;
import com.tangosol.io.internal.DefaultObjectInputFilter;
import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.io.pof.PofConstants;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.net.CompressionFilter;

import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.ExternalizableHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for serialization filtering through compressed Extend input.
 *
 * @author phf  2026.09.30
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class PeerFilterBufferInputTest
    {
    @Parameterized.Parameters(name = "security={0}")
    public static Object[] modes()
        {
        return new Object[] {null, "hardened", "compatibility"};
        }

    public PeerFilterBufferInputTest(String sSecurityMode)
        {
        f_sSecurityMode = sSecurityMode;
        }

    @Before
    public void setUp()
        {
        m_scope = CoherenceModeHelper.securityMode(f_sSecurityMode);
        }

    @After
    public void tearDown()
        {
        m_scope.close();
        }

    @Test
    public void shouldReadCompressedProtocolVersions() throws IOException
        {
        ReadBuffer.BufferInput original = versionMap().getBufferInput();
        ReadBuffer.BufferInput filtered = peer(true).filterBufferInput(original);
        Map<?, ?> versions = reader(filtered).readMap(-1, new HashMap<>());

        assertEquals(1, versions.size());
        assertArrayEquals(new Object[] {14, 3}, (Object[]) versions.get("NamedCacheProtocol"));
        if (!"compatibility".equals(f_sSecurityMode))
            {
            assertNotNull(original.getObjectInputFilter());
            assertSame(original.getObjectInputFilter(), filtered.getObjectInputFilter());
            }
        }

    @Test
    public void shouldPreserveConfiguredFilterAndSetOnceContract() throws IOException
        {
        ReadBuffer.BufferInput original = versionMap().getBufferInput();
        ObjectInputFilter filter = info -> info.arrayLength() > 1
                ? ObjectInputFilter.Status.REJECTED : ObjectInputFilter.Status.UNDECIDED;
        original.setObjectInputFilter(filter);
        ReadBuffer.BufferInput filtered = peer(true).filterBufferInput(original);

        assertSame(original.getObjectInputFilter(), filtered.getObjectInputFilter());
        assertThrows(IllegalStateException.class,
                () -> filtered.setObjectInputFilter(DefaultObjectInputFilter.create()));
        assertThrows(InvalidClassException.class, () -> reader(filtered).readMap(-1, new HashMap<>()));
        }

    @Test
    public void shouldFindFilterThroughAdditionalDataInputWrapper() throws IOException
        {
        ReadBuffer.BufferInput original = versionMap().getBufferInput();
        original.setObjectInputFilter((ObjectInputFilter) info -> ObjectInputFilter.Status.REJECTED);
        ReadBuffer.BufferInput filtered = peer(true).filterBufferInput(original);

        assertThrows(InvalidClassException.class, () -> ExternalizableHelper.validateLoadArray(
                Object[].class, 2, new WrapperDataInputStream(filtered)));
        }

    @Test
    public void shouldFindFilterPastWrapperWithoutFilterStorage()
        {
        ReadBuffer.BufferInput original = new ByteArrayReadBuffer(new byte[0]).getBufferInput();
        original.setObjectInputFilter((ObjectInputFilter) info -> ObjectInputFilter.Status.REJECTED);
        WrapperBufferInput wrapper = new WrapperBufferInput(new WrapperDataInputStream(original));

        assertThrows(InvalidClassException.class,
                () -> ExternalizableHelper.validateLoadArray(Object[].class, 2, wrapper));
        }

    @Test
    public void shouldRetainBridgeFilterRejection() throws IOException
        {
        try (DefaultObjectInputFilter.Scope ignored = DefaultObjectInputFilter.bridge(info ->
                info.serialClass() == Object[].class
                        ? ObjectInputFilter.Status.REJECTED : ObjectInputFilter.Status.UNDECIDED))
            {
            ReadBuffer.BufferInput filtered = peer(true).filterBufferInput(versionMap().getBufferInput());
            if ("compatibility".equals(f_sSecurityMode))
                {
                assertEquals(1, reader(filtered).readMap(-1, new HashMap<>()).size());
                }
            else
                {
                assertThrows(InvalidClassException.class, () -> reader(filtered).readMap(-1, new HashMap<>()));
                }
            }
        }

    @Test
    public void shouldFailClosedWhenOriginalInputCannotRetainFilter() throws IOException
        {
        ReadBuffer.BufferInput original = new WrapperBufferInput(
                new DataInputStream(new ByteArrayInputStream(versionMap().toByteArray())));
        ReadBuffer.BufferInput filtered = peer(true).filterBufferInput(original);

        if ("compatibility".equals(f_sSecurityMode))
            {
            assertEquals(1, reader(filtered).readMap(-1, new HashMap<>()).size());
            }
        else
            {
            assertThrows(InvalidClassException.class, () -> reader(filtered).readMap(-1, new HashMap<>()));
            }
        }

    @Test
    public void shouldLeaveUncompressedInputUnchanged() throws IOException
        {
        ReadBuffer.BufferInput original = new ByteArrayReadBuffer(new byte[0]).getBufferInput();
        original.setObjectInputFilter((ObjectInputFilter) info -> ObjectInputFilter.Status.REJECTED);

        assertSame(original, peer(false).filterBufferInput(original));
        assertThrows(InvalidClassException.class,
                () -> ExternalizableHelper.validateLoadArray(Object[].class, 2, original));
        }

    @Test
    public void shouldRejectInvalidCompressedArrayLengths() throws IOException
        {
        for (int cElements : new int[] {-1, 5})
            {
            ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(32);
            var out = buffer.getBufferOutput();
            out.writePackedInt(PofConstants.T_ARRAY);
            out.writePackedInt(cElements);
            ReadBuffer.BufferInput filtered = peer(true).filterBufferInput(compress(buffer).getBufferInput());

            assertThrows(IOException.class, () -> reader(filtered).readObject(-1));
            }
        }

    @Test
    public void shouldRejectTruncatedCompressedArray() throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(32);
        var out = buffer.getBufferOutput();
        out.writePackedInt(PofConstants.T_ARRAY);
        out.writePackedInt(2);
        out.writePackedInt(PofConstants.V_INT_1);
        ReadBuffer.BufferInput filtered = peer(true).filterBufferInput(compress(buffer).getBufferInput());

        assertThrows(IOException.class, () -> reader(filtered).readObject(-1));
        }

    @Test
    public void shouldRejectUnregisteredPayloadBeforeReadObject() throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(128);
        ExternalizableHelper.writeObject(buffer.getBufferOutput(), new MaterializationProbe());
        ReadBuffer.BufferInput filtered = peer(true).filterBufferInput(compress(buffer).getBufferInput());

        MaterializationProbe.s_fRead = false;
        if ("compatibility".equals(f_sSecurityMode))
            {
            assertTrue(ExternalizableHelper.readObject(filtered) instanceof MaterializationProbe);
            assertTrue(MaterializationProbe.s_fRead);
            }
        else
            {
            assertThrows(IOException.class, () -> ExternalizableHelper.readObject(filtered));
            assertFalse(MaterializationProbe.s_fRead);
            }
        }

    private static Peer peer(boolean fCompress)
        {
        Peer peer = new Peer(null, null, false) {};
        if (fCompress)
            {
            CompressionFilter filter = new CompressionFilter();
            filter.setConfig(XmlHelper.loadXml("<compression><strategy>gzip</strategy></compression>"));
            peer.setWrapperStreamFactoryList(List.of(filter));
            }
        return peer;
        }

    private static ReadBuffer versionMap() throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(128);
        new PofBufferWriter(buffer.getBufferOutput(), new SimplePofContext()).writeMap(-1,
                Map.of("NamedCacheProtocol", new Integer[] {14, 3}), String.class, Integer[].class);
        return compress(buffer);
        }

    private static ReadBuffer compress(ByteArrayWriteBuffer buffer) throws IOException
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes))
            {
            gzip.write(buffer.toByteArray());
            }
        return new ByteArrayReadBuffer(bytes.toByteArray());
        }

    private static PofBufferReader reader(ReadBuffer.BufferInput in)
        {
        SimplePofContext context = new SimplePofContext();
        context.setLimitPolicy(new SerializationLimitPolicy(1024L, 4, 4));
        return new PofBufferReader(in, context);
        }

    /**
     * Unregistered payload that detects premature materialization.
     *
     * @author phf  2026.09.30
     * @since 26.10
     */
    public static class MaterializationProbe
            implements Serializable
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fRead = true;
            in.defaultReadObject();
            }

        private static boolean s_fRead;
        }

    private final String f_sSecurityMode;

    private CoherenceModeHelper.ModeScope m_scope;
    }
