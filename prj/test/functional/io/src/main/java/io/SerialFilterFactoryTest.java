/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.util.ExternalizableHelper;

import data.BlobExternalizableLite;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import com.oracle.coherence.testing.CheckJDK;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Validate {@link ReadBuffer.BufferInput BufferInput} class deserialization
 * when SerialFilterFactory is configured.
 * <p>
 * Relies on ObjectInputFilter$Config#getSerialFilterFactory and
 * ObjectInputFilter$Config#setSerialFilterFactory(ObjectInputFilter) added
 * in JDK 17.
 *
 * @author jf  2021.09.29
 */
public class SerialFilterFactoryTest
    {
    @BeforeClass
    public static void setup() throws Throwable
        {
        // configure a process-wide ObjectInputFilter
        ObjectInputFilterHelper.setConfigObjectInputStreamFilter(PROCESS_WIDE_FILTER);
        }

    @Test
    public void testConfigGetSerialFilter()
        {
        assertThat(ExternalizableHelper.getConfigSerialFilter().toString().contains(PROCESS_WIDE_FILTER), is(true));
        }

    @Test(expected=IllegalStateException.class)
    public void mustThrowIfSetConfigObjectInputStreamFilterTwice()
            throws Throwable
        {
        ObjectInputFilterHelper.setConfigObjectInputStreamFilter("data.*");
        fail("must throw IllegalStateException if try to set a process wide ObjectInputFilter a second time in a process");
        }

    @Test(expected = IllegalStateException.class)
    public void mustThrowIfSetObjectFilterToNull()
        {
        CheckJDK.assumeJDKVersionEqualOrGreater(17);
        BlobExternalizableLite blob = new BlobExternalizableLite(40);
        ByteArrayWriteBuffer   baos = new ByteArrayWriteBuffer(0);

        try
            {
            ExternalizableHelper.writeObject(baos.getBufferOutput(), blob);
            }
        catch (IOException e)
            {
            e.printStackTrace();
            }

        ByteArrayReadBuffer    bais = new ByteArrayReadBuffer(baos.toByteArray());
        ReadBuffer.BufferInput in   = bais.getBufferInput();

        assertTrue("must have process-wide filter",
                   in.getObjectInputFilter().toString().contains("examples.*"));
        assertNotNull(in.getObjectInputFilter());

        in.setObjectInputFilter(null);
        fail("calling setObjectInputFilter to null should have thrown IllegalStateExeception");
        }

    @Test(expected = IllegalStateException.class)
    public void mustThrowIfSetObjectFilterTwice()
        {
        BlobExternalizableLite blob = new BlobExternalizableLite(40);
        ByteArrayWriteBuffer baos = new ByteArrayWriteBuffer(0);

        try
            {
            ExternalizableHelper.writeObject(baos.getBufferOutput(), blob);
            }
        catch (IOException e)
            {
            e.printStackTrace();
            }

        ByteArrayReadBuffer bais = new ByteArrayReadBuffer(baos.toByteArray());
        ReadBuffer.BufferInput in = bais.getBufferInput();

        assertTrue("must have process-wide filter",
                   in.getObjectInputFilter().toString().contains("examples.*"));
        assertNotNull(in.getObjectInputFilter());

        in.setObjectInputFilter(ObjectInputFilterHelper.createObjectInputFilter("data.*"));
        in.setObjectInputFilter(ObjectInputFilterHelper.createObjectInputFilter("data.*;com.acme.*"));
        fail("calling setObjectInputFilter second time should have thrown IllegalStateExeception");
        }

    // ----- constants ---------------------------------------------------------

    public final static String PROCESS_WIDE_FILTER = "examples.*";
    }
