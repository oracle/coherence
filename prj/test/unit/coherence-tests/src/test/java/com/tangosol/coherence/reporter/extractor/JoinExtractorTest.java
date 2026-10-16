/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;

import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.WrapperException;
import com.tangosol.util.extractor.IdentityExtractor;
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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import static com.tangosol.util.ExternalizableHelper.ensureSerializer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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

    @Test
    public void shouldPreserveDirectQueryAndCardinalityHandling() throws Exception
        {
        ObjectName  pattern = new ObjectName("Coherence:type=Node,nodeId=1,*");
        ObjectName  first   = new ObjectName("Coherence:type=Node,nodeId=1,member=first");
        ObjectName  second  = new ObjectName("Coherence:type=Node,nodeId=1,member=second");
        MBeanServer server  = mock(MBeanServer.class);
        when(server.queryNames(pattern, null)).thenReturn(Collections.emptySet(), Set.of(first), Set.of(first, second));
        JoinExtractor join = new JoinExtractor(new ValueExtractor[] {IdentityExtractor.INSTANCE},
                "Coherence:type=Node,nodeId={node},*", IdentityExtractor.INSTANCE, server);

        assertEquals(pattern, join.extract("1"));
        assertEquals(first, join.extract("1"));
        assertEquals(pattern, join.extract("1"));
        verify(server, times(3)).queryNames(pattern, null);
        }

    @Test
    public void shouldUseResolverAndExtractValueEveryTime() throws Exception
        {
        ObjectName pattern = new ObjectName("Coherence:type=Node,nodeId=1,*");
        ObjectName target  = new ObjectName("Coherence:type=Node,nodeId=1,member=one");
        MBeanServer server = mock(MBeanServer.class);
        AtomicInteger cResolutions = new AtomicInteger();
        AtomicInteger cValues      = new AtomicInteger();
        Function<ObjectName, ObjectName> resolver = name ->
            {
            assertEquals(pattern, name);
            cResolutions.incrementAndGet();
            return target;
            };
        ValueExtractor source = name ->
            {
            assertEquals(target, name);
            return cValues.incrementAndGet();
            };
        JoinExtractor join = new JoinExtractor(new ValueExtractor[] {IdentityExtractor.INSTANCE},
                "Coherence:type=Node,nodeId={node},*", source, server, resolver);

        assertEquals(1, join.extract("1"));
        assertEquals(2, join.extract("1"));
        assertEquals(2, cResolutions.get());
        verifyNoInteractions(server);
        }

    @Test
    public void shouldBypassResolverForExactAndQuestionMarkNames() throws Exception
        {
        MBeanServer server = mock(MBeanServer.class);
        Function<ObjectName, ObjectName> resolver = name ->
            {
            throw new AssertionError("unexpected wildcard resolution");
            };
        for (String sNode : new String[] {"1", "?"})
            {
            JoinExtractor join = new JoinExtractor(new ValueExtractor[] {IdentityExtractor.INSTANCE},
                    "Coherence:type=Node,nodeId={node}", IdentityExtractor.INSTANCE, server, resolver);
            assertEquals(new ObjectName("Coherence:type=Node,nodeId=" + sNode), join.extract(sNode));
            }
        verifyNoInteractions(server);
        }

    @Test
    public void shouldReturnNullForMalformedExpandedName()
        {
        MBeanServer server = mock(MBeanServer.class);
        JoinExtractor join = new JoinExtractor(new ValueExtractor[] {IdentityExtractor.INSTANCE},
                "Coherence::nodeId={node},*", IdentityExtractor.INSTANCE, server,
                name -> { throw new AssertionError("invalid name reached resolver"); });
        assertNull(join.extract("1"));
        verifyNoInteractions(server);
        }

    @Test(expected = SecurityException.class)
    public void shouldPropagateResolverSecurityFailure()
        {
        JoinExtractor join = new JoinExtractor(new ValueExtractor[] {IdentityExtractor.INSTANCE},
                "Coherence:type=Node,nodeId={node},*", IdentityExtractor.INSTANCE, null,
                name -> { throw new SecurityException("denied"); });
        join.extract("1");
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
