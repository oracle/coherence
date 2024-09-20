/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BinaryDecoratedObjectTest
    {
    @Test
    public void shouldSerializeJava()
        {
        assertSerialization(new DefaultSerializer());
        }

    @Test
    public void shouldSerializePof()
        {
        assertSerialization(new ConfigurablePofContext("deco-aware-pof-config.xml"));
        }

    public void assertSerialization(Serializer serializer)
        {
        Binary     binDeco      = new Binary("12345".getBytes(StandardCharsets.UTF_8));
        DecoAware  bdo          = new DecoAware("abcdefghijk");
        Binary     binNoDeco    = ExternalizableHelper.toBinary(bdo, serializer);

        assertThat(ExternalizableHelper.isDecorated(binNoDeco), is(false));

        bdo.setDecoration(binDeco);
        Binary binWithDeco = ExternalizableHelper.toBinary(bdo, serializer);
        assertThat(ExternalizableHelper.isDecorated(binWithDeco), is(true));

        ReadBuffer bufExpected = ExternalizableHelper.decorate(binNoDeco, DECO_ID, binDeco);
        assertThat(ExternalizableHelper.isDecorated(bufExpected), is(true));

        ReadBuffer bufDeco = ExternalizableHelper.getDecoration((ReadBuffer) binWithDeco, DECO_ID);
        assertThat(bufDeco, is(notNullValue()));
        assertThat(bufDeco.toBinary(), is(binDeco));

        DecoAware oResult = ExternalizableHelper.fromBinary(binWithDeco, serializer);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.m_sValue, is(bdo.m_sValue));
        assertThat(oResult.getDecoration(), is(binDeco));
        }

    // ----- inner class: DecoAware -----------------------------------------

    public static class DecoAware
            implements ExternalizableHelper.DecorationAware, ExternalizableLite, PortableObject
        {
        public DecoAware()
            {
            }

        public DecoAware(String m_sValue)
            {
            this.m_sValue = m_sValue;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sValue = in.readString(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sValue);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sValue = ExternalizableHelper.readSafeUTF(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sValue);
            }

        @Override
        public void storeDecorations(ReadBuffer buffer)
            {
            if (ExternalizableHelper.isDecorated(buffer))
                {
                m_deco = ExternalizableHelper.getDecoration(buffer, DECO_ID);
                }
            }

        @Override
        public ReadBuffer applyDecorations(ReadBuffer buffer)
            {
            if (m_deco != null)
                {
                return ExternalizableHelper.decorate(buffer, DECO_ID, m_deco);
                }
            return buffer;
            }

        public ReadBuffer getDecoration()
            {
            return m_deco;
            }

        public void setDecoration(Binary binDeco)
            {
            m_deco = binDeco;
            }

        // ----- data members -----------------------------------------------

        private String m_sValue;

        private transient ReadBuffer m_deco;
        }

    // ----- constants ------------------------------------------------------

    public static final int DECO_ID = ExternalizableHelper.DECO_RSVD_1;
    }
