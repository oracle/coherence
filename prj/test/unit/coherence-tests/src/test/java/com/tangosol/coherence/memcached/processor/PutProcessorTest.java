/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.processor;

import com.tangosol.coherence.memcached.Response.ResponseCode;

import com.tangosol.coherence.memcached.server.DataHolder;
import com.tangosol.coherence.memcached.server.MemcachedHelper;

import com.tangosol.io.DefaultSerializer;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PutProcessor} and sibling Memcached processors.
 *
 * @author Aleks Seovic  2026.05.15
 *
 * @since 26.07
 */
public class PutProcessorTest
    {
    @Test
    public void shouldRejectPassThroughPutBeforeStorage()
        {
        BackingMapManagerContext ctx       = createManagerContext();
        ExposedPutProcessor      processor = new ExposedPutProcessor(
                new byte[] {(byte) ExternalizableHelper.FMT_OBJ_SER}, 0, 0, 0, true);

        assertThrows(RuntimeException.class, () -> processor.getDecoratedBinary(ctx, 1));
        verify(ctx, never()).addInternalValueDecoration(any(), eq(ExternalizableHelper.DECO_MEMCACHED), any());
        }

    @Test
    public void shouldAllowPofPassThroughPut()
        {
        BackingMapManagerContext ctx       = createManagerContext();
        ExposedPutProcessor      processor = new ExposedPutProcessor(
                new byte[] {(byte) ExternalizableHelper.FMT_EXT}, 7, 0, 0, true);

        Binary binStored = processor.getDecoratedBinary(ctx, 1);
        Binary binValue  = ExternalizableHelper.getUndecorated(binStored);

        assertTrue(ExternalizableHelper.isDecorated(binStored, ExternalizableHelper.DECO_MEMCACHED));
        assertEquals(ExternalizableHelper.FMT_EXT, binValue.byteAt(0) & 0xFF);
        }

    @Test
    public void shouldRejectRemotePutProcessorBeforeStorage()
        {
        BinaryEntry entry = createEntry(null);
        PutProcessor processor = new PutProcessor(
                new byte[] {(byte) ExternalizableHelper.FMT_OBJ_SER}, 0, 0, 0, true);

        assertThrows(RuntimeException.class, () -> processor.process(entry));
        verify(entry, never()).updateBinaryValue(any(Binary.class));
        }

    @Test
    public void shouldRejectAddReplaceProcessorBeforeStorage()
        {
        BinaryEntry entry = createEntry(null);
        AddReplaceProcessor processor = new AddReplaceProcessor(
                new byte[] {(byte) ExternalizableHelper.FMT_XML_SER}, 0, 0, 0, true, true);

        assertThrows(RuntimeException.class, () -> processor.process(entry));
        verify(entry, never()).updateBinaryValue(any(Binary.class));
        }

    @Test
    public void shouldRejectGetProcessorMaterialization()
        {
        BinaryEntry  entry     = createEntry(decorate(new byte[] {(byte) ExternalizableHelper.FMT_OBJ_SER}));
        GetProcessor processor = new GetProcessor(false);

        assertThrows(RuntimeException.class, () -> processor.process(entry));
        verify(entry.getBackingMapContext().getManagerContext().getValueFromInternalConverter(), never())
                .convert(any());
        }

    @Test
    public void shouldRejectGetProcessorFmtExtDefaultSerializerMaterialization()
        {
        MaterializationSentinel.reset();
        Binary       binNested = ExternalizableHelper.toBinary(new MaterializationSentinel());
        BinaryEntry  entry     = createEntry(MemcachedHelper.decorateBinary(fmtExt(binNested), 0, 1));
        GetProcessor processor = new GetProcessor(false);

        assertEquals(ExternalizableHelper.FMT_OBJ_SER, binNested.byteAt(0) & 0xFF);
        assertThrows(RuntimeException.class, () -> processor.process(entry));
        verify(entry.getBackingMapContext().getManagerContext().getValueFromInternalConverter(), never())
                .convert(any());
        assertFalse(MaterializationSentinel.wasMaterialized());
        }

    @Test
    public void shouldAllowGetProcessorPassThroughRead()
        {
        byte[]       abValue   = new byte[] {(byte) ExternalizableHelper.FMT_OBJ_SER};
        BinaryEntry  entry     = createEntry(decorate(abValue));
        GetProcessor processor = new GetProcessor(true);

        DataHolder holder = (DataHolder) processor.process(entry);

        assertArrayEquals(abValue, holder.getValue());
        }

    @Test
    public void shouldRejectTouchProcessorMaterialization()
        {
        BinaryEntry    entry     = createEntry(decorate(new byte[] {(byte) ExternalizableHelper.FMT_OBJ_EXT}));
        TouchProcessor processor = new TouchProcessor(0, false, false);

        assertThrows(RuntimeException.class, () -> processor.process(entry));
        verify(entry.getBackingMapContext().getManagerContext().getValueFromInternalConverter(), never())
                .convert(any());
        }

    @Test
    public void shouldRejectAppendPrependMaterialization()
        {
        BinaryEntry             entry     = createEntry(decorate(new byte[] {(byte) ExternalizableHelper.FMT_XML_BEAN}));
        AppendPrependProcessor  processor = new AppendPrependProcessor(new byte[] {(byte) ExternalizableHelper.FMT_EXT},
                0, true, false);

        assertThrows(RuntimeException.class, () -> processor.process(entry));
        verify(entry, never()).updateBinaryValue(any(Binary.class));
        }

    @Test
    public void shouldRejectIncrDecrMaterializationWithoutMutation()
        {
        BinaryEntry        entry     = createEntry(decorate(new byte[] {(byte) ExternalizableHelper.FMT_UNKNOWN}));
        IncrDecrProcessor  processor = new IncrDecrProcessor(1, 1, true, 0, 0, false);

        assertEquals(ResponseCode.NAN, processor.process(entry));
        verify(entry, never()).updateBinaryValue(any(Binary.class));
        }

    private static BinaryEntry createEntry(Binary binValue)
        {
        BackingMapManagerContext ctxMgr = createManagerContext();
        BackingMapContext        ctx    = mock(BackingMapContext.class);
        BinaryEntry              entry  = mock(BinaryEntry.class);

        when(ctx.getManagerContext()).thenReturn(ctxMgr);
        when(entry.getBackingMapContext()).thenReturn(ctx);
        when(entry.getBinaryValue()).thenReturn(binValue);

        return entry;
        }

    private static BackingMapManagerContext createManagerContext()
        {
        BackingMapManagerContext ctx       = mock(BackingMapManagerContext.class);
        CacheService             service   = mock(CacheService.class);
        com.tangosol.util.Converter converter = mock(com.tangosol.util.Converter.class);

        when(service.getSerializer()).thenReturn(new DefaultSerializer());
        when(ctx.getCacheService()).thenReturn(service);
        when(ctx.addInternalValueDecoration(any(), eq(ExternalizableHelper.DECO_MEMCACHED), any()))
                .thenAnswer(invocation -> ExternalizableHelper.decorate(
                        (Binary) invocation.getArgument(0),
                        invocation.getArgument(1),
                        (Binary) invocation.getArgument(2)));
        when(ctx.getValueFromInternalConverter()).thenReturn(converter);

        return ctx;
        }

    private static Binary decorate(byte[] abValue)
        {
        return MemcachedHelper.decorateBinary(new Binary(abValue), 0, 1);
        }

    private static Binary fmtExt(Binary binNested)
        {
        byte[] abNested = binNested.toByteArray();
        byte[] abValue  = new byte[abNested.length + 1];

        abValue[0] = (byte) ExternalizableHelper.FMT_EXT;
        System.arraycopy(abNested, 0, abValue, 1, abNested.length);

        return new Binary(abValue);
        }

    public static class MaterializationSentinel
            implements Serializable
        {
        public static void reset()
            {
            s_fMaterialized = false;
            }

        public static boolean wasMaterialized()
            {
            return s_fMaterialized;
            }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }

        private static boolean s_fMaterialized;
        }

    public static class ExposedPutProcessor
            extends PutProcessor
        {
        public ExposedPutProcessor(byte[] abValue, int nFlag, long lVersion, int nExpiry, boolean fBinaryPassThru)
            {
            super(abValue, nFlag, lVersion, nExpiry, fBinaryPassThru);
            }

        public Binary getDecoratedBinary(BackingMapManagerContext mgrCtx, long lVersion)
            {
            return super.getDecoratedBinary(mgrCtx, lVersion);
            }
        }
    }
