/*
 * Copyright (c) 2000, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;

import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.WrapperException;
import com.tangosol.util.extractor.ReflectionExtractor;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.URL;

import static com.tangosol.util.ExternalizableHelper.ensureSerializer;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for the {@link JoinExtractor}.
 *
 * @author jf 2020.04.27
 */
public class JoinExtractorTest
    {
    @Test(expected = IOException.class)
    public void shouldNotDeserializeByExternalizableLite()
        throws Throwable
        {
        InputStream     is  = getResourceAsInputStream("com/tangosol/coherence/reporter/extractor/JoinExtractor_externalizablelite.ser");
        DataInputStream das = new DataInputStream(is);

        try
            {
            ExternalizableHelper.readObject(das);
            fail("instances of JoinExtractor must not deserialize");
            }
        catch (WrapperException e)
            {
            Throwable t = getChildCause(e);
            assertTrue(t instanceof IOException);
            throw t;
            }
        }

    @Test(expected = NotSerializableException.class)
    public void shouldNotSerializeByExternalizableLite()
        throws Throwable
        {
        try
            {
            ExternalizableHelper.toByteArray(createJoinExtractor(), ensureSerializer(null));
            fail("instances of JoinExtractor must not serialize");
            }
        catch (WrapperException e)
            {
            Throwable t = getChildCause(e);
            assertTrue(t instanceof NotSerializableException);
            throw t;
            }
        }

    @Test(expected = IOException.class)
    public void shouldNotDeserializeByPof()
        throws Throwable
        {
        InputStream     is  = getResourceAsInputStream("com/tangosol/coherence/reporter/extractor/JoinExtractor_pof.ser");
        DataInputStream dis = new DataInputStream(is);
        try
            {
            s_ctxPof.deserialize(new WrapperBufferInput(dis, null));
            }
        catch (WrapperException e)
            {
            Throwable t = getChildCause(e);
            assertTrue(t instanceof IOException);
            throw t;
            }
        }

    @Test(expected = NotSerializableException.class)
    public void shouldNotSerializeByPof()
        throws Throwable
        {
        try
            {
            ExternalizableHelper.toByteArray(createJoinExtractor(), s_ctxPof);
            fail("instances of JoinExtractor must not serialize");
            }
        catch (WrapperException e)
            {
            Throwable t = getChildCause(e);
            assertTrue(t.getMessage().contains("JoinExtractor is not serializable"));
            throw t;
            }
        }

    @Test(expected = IOException.class)
    public void shouldNotJavaDeserialize()
        throws Throwable
        {
        InputStream       is  = getResourceAsInputStream("com/tangosol/coherence/reporter/extractor/JoinExtractor_java.ser");
        ObjectInputStream ois = new ObjectInputStream(is);
        ois.readObject();
        fail("instances of JoinExtractor must not deserialize");
        }

    @Test(expected = NotSerializableException.class)
    public void shouldNotJavaSerialize()
        throws Throwable
        {
        ObjectOutputStream os = new ObjectOutputStream(new ByteArrayOutputStream());
        os.writeObject(createJoinExtractor());
        fail("instances of JoinExtractor must not serialize");
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return InputStream for specified resource.
     * *
     * Since expected failure for these tests is {@link IOException}, throw a different
     * exception if this method fails to locate or open input resource.
     *
     * @param sResourceName  the resource name
     *
     * @return InputStream for specified resource name
     */
    private InputStream getResourceAsInputStream(String sResourceName)
        {
        try
            {
            URL url = this.getClass().getClassLoader().getResource(sResourceName);
            InputStream is = url.openStream();
            return is;
            }
        catch (IOException e)
            {
            // ignored
            }
        throw new IllegalArgumentException("failed to locate and open deserialization input resource: " + sResourceName);
        }

    /**
     * Unwrapper WrapperException and IOExceptions and return initial exception.
     *
     * @param e  handled exception, possibly a WrapperException or IOException.
     * @return initial exception
     */
    private Throwable getChildCause(Throwable e)
        {
        Throwable t = e;
        while (t != null && t.getCause() != null)
            {
            t = t.getCause();
            }
        return t;
        }

    /**
     * Create an instance of {@link JoinExtractor}.
     *
     * @return an instance of {@link JoinExtractor}
     */
    private JoinExtractor createJoinExtractor()
        {
        ValueExtractor[] aVE = new ValueExtractor[1];

        return new JoinExtractor(aVE, "joinTemplate", new ReflectionExtractor("someMethod"), null);
        }

    // ----- data members ---------------------------------------------------

    static private ConfigurablePofContext s_ctxPof = new ConfigurablePofContext("coherence-pof-config.xml");
    }
