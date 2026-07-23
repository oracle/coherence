/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MemcachedHelper}.
 *
 * @author Aleks Seovic  2026.05.15
 *
 * @since 14.1.1.0
 */
public class MemcachedHelperTest
    {
    @Test
    public void shouldRejectDangerousTerminalFormats()
        {
        assertRejected(ExternalizableHelper.FMT_OBJ_SER);
        assertRejected(ExternalizableHelper.FMT_UNKNOWN);
        assertRejected(ExternalizableHelper.FMT_XML_SER);
        assertRejected(ExternalizableHelper.FMT_XML_BEAN);
        assertRejected(ExternalizableHelper.FMT_OBJ_EXT);
        }

    @Test
    public void shouldRejectDangerousDecoratedInnerFormat()
        {
        Binary binDecorated = ExternalizableHelper.decorate(
                new Binary(new byte[] {(byte) ExternalizableHelper.FMT_OBJ_SER}),
                ExternalizableHelper.DECO_APP_1,
                new Binary(new byte[] {(byte) ExternalizableHelper.FMT_STRING}));

        assertThrows(RuntimeException.class, () -> MemcachedHelper.validatePassThroughValue(binDecorated));
        }

    @Test
    public void shouldRejectDangerousIntegerDecoratedInnerFormat()
        {
        Binary binDecorated = ExternalizableHelper.decorateBinary(
                new Binary(new byte[] {(byte) ExternalizableHelper.FMT_OBJ_EXT}), 123).toBinary();

        assertThrows(RuntimeException.class, () -> MemcachedHelper.validatePassThroughValue(binDecorated));
        }

    @Test
    public void shouldRejectDangerousOptionalInnerFormat()
            throws IOException
        {
        BinaryWriteBuffer        buffer = new BinaryWriteBuffer(8);
        WriteBuffer.BufferOutput out    = buffer.getBufferOutput();

        out.writeByte(ExternalizableHelper.FMT_OPT);
        out.writeBoolean(true);
        out.writeByte(ExternalizableHelper.FMT_OBJ_SER);

        assertThrows(RuntimeException.class, () -> MemcachedHelper.validatePassThroughValue(buffer.toBinary()));
        }

    @Test
    public void shouldAllowPofAndPassiveFormats()
        {
        MemcachedHelper.validatePassThroughValue(new byte[] {(byte) ExternalizableHelper.FMT_EXT});
        MemcachedHelper.validatePassThroughValue(new byte[] {(byte) ExternalizableHelper.FMT_STRING});
        MemcachedHelper.validatePassThroughValue(new byte[] {(byte) ExternalizableHelper.FMT_BINARY});
        MemcachedHelper.validatePassThroughValue(new byte[] {(byte) ExternalizableHelper.FMT_B_ARRAY});
        MemcachedHelper.validatePassThroughValue(new byte[] {(byte) ExternalizableHelper.FMT_BOOLEAN});
        }

    @Test
    public void shouldRejectLegacyDecoratedValueBeforeMaterialization()
        {
        Binary                   binStored = MemcachedHelper.decorateBinary(
                new Binary(new byte[] {(byte) ExternalizableHelper.FMT_OBJ_SER}), 0, 1);
        BackingMapManagerContext ctx       = mock(BackingMapManagerContext.class);
        Converter                converter = mock(Converter.class);

        when(ctx.getValueFromInternalConverter()).thenReturn(converter);

        assertThrows(RuntimeException.class, () -> MemcachedHelper.convertToDataHolder(binStored, ctx, false));
        verify(converter, never()).convert(any());
        }

    @Test
    public void shouldRejectFmtExtDefaultSerializerBeforeOrdinaryMaterialization()
        {
        s_fMaterialized = false;
        Binary binNested = ExternalizableHelper.toBinary(new MaterializationSentinel());
        Binary binStored = MemcachedHelper.decorateBinary(fmtExt(binNested), 0, 1);

        assertEquals(ExternalizableHelper.FMT_OBJ_SER, binNested.byteAt(0) & 0xFF);
        assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(binStored,
                new DefaultSerializer()));
        assertFalse(s_fMaterialized);
        }

    @Test
    public void shouldRejectFmtExtDefaultSerializerNestedExternalizableLiteFormat()
        {
        Binary binStored = MemcachedHelper.decorateBinary(
                fmtExt(new Binary(new byte[] {(byte) ExternalizableHelper.FMT_OBJ_EXT})), 0, 1);

        assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(binStored,
                new DefaultSerializer()));
        }

    @Test
    public void shouldRejectFmtExtDefaultSerializerBeforeProcessorMaterialization()
        {
        s_fMaterialized = false;
        Binary                   binNested = ExternalizableHelper.toBinary(new MaterializationSentinel());
        Binary                   binStored = MemcachedHelper.decorateBinary(fmtExt(binNested), 0, 1);
        BackingMapManagerContext ctx       = createManagerContext(new DefaultSerializer());
        Converter                converter = ctx.getValueFromInternalConverter();

        assertThrows(RuntimeException.class, () -> MemcachedHelper.convertToDataHolder(binStored, ctx, false));
        verify(converter, never()).convert(any());
        assertFalse(s_fMaterialized);
        }

    @Test
    public void shouldAllowFmtExtProcessorMaterializationWithConfiguredSerializer()
        {
        Binary                   binStored = MemcachedHelper.decorateBinary(
                new Binary(new byte[] {(byte) ExternalizableHelper.FMT_EXT}), 0, 1);
        BackingMapManagerContext ctx       = createManagerContext(new ConfigurablePofContext());
        Converter                converter = ctx.getValueFromInternalConverter();

        when(converter.convert(any())).thenReturn(new byte[] {1, 2, 3});

        assertArrayEquals(new byte[] {1, 2, 3},
                MemcachedHelper.convertToDataHolder(binStored, ctx, false).getValue());
        verify(converter).convert(any());
        }

    @Test
    public void shouldReturnRawBytesForPassThroughReads()
        {
        byte[] abValue   = new byte[] {(byte) ExternalizableHelper.FMT_OBJ_SER};
        Binary binStored = MemcachedHelper.decorateBinary(new Binary(abValue), 99, 123);

        DataHolder holder = MemcachedHelper.convertToDataHolder(binStored, null, true);

        assertArrayEquals(abValue, holder.getValue());
        }

    @Test
    public void shouldRejectOrdinaryMaterializationOfDangerousMemcachedDecoratedValue()
        {
        s_fMaterialized = false;
        Binary binValue  = ExternalizableHelper.toBinary(new MaterializationSentinel());
        Binary binStored = MemcachedHelper.decorateBinary(binValue, 0, 1);

        assertEquals(ExternalizableHelper.FMT_OBJ_SER, binValue.byteAt(0) & 0xFF);
        assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(binStored));
        assertFalse(s_fMaterialized);
        }

    @Test
    public void shouldAllowOrdinaryMaterializationOfPassiveMemcachedDecoratedValue()
        {
        Binary binStored = MemcachedHelper.decorateBinary(ExternalizableHelper.toBinary("legacy-ok"), 0, 1);

        assertEquals("legacy-ok", ExternalizableHelper.fromBinary(binStored));
        }

    private static void assertRejected(int nFormat)
        {
        assertThrows(RuntimeException.class,
                () -> MemcachedHelper.validatePassThroughValue(new byte[] {(byte) nFormat}));
        }

    private static BackingMapManagerContext createManagerContext(Serializer serializer)
        {
        BackingMapManagerContext ctx       = mock(BackingMapManagerContext.class);
        CacheService             service   = mock(CacheService.class);
        Converter                converter = mock(Converter.class);

        when(service.getSerializer()).thenReturn(serializer);
        when(ctx.getCacheService()).thenReturn(service);
        when(ctx.getValueFromInternalConverter()).thenReturn(converter);

        return ctx;
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
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    private static boolean s_fMaterialized;
    }
