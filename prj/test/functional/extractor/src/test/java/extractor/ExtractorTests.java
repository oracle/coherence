/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package extractor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.WrapperException;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.DeserializationAccelerator;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.LessEqualsFilter;

import com.tangosol.util.processor.ExtractorProcessor;

import common.AbstractFunctionalTest;

import data.Person;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Calendar;
import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


/**
* A collection of functional tests for the {@link ValueExtractor}.
*
* @author oew 01/22/2007
*/
public class ExtractorTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    /**
    * A simple test for the {@link ReflectionExtractor}.
    */
    @Test
    public void testReflection()
        {
        NamedCache cache       = getNamedCache();
        int       iCurrentYear = Calendar.getInstance().get(Calendar.YEAR);

        for (int i = 0; i < 100; i++)
            {
            Person customers = new Person();
            customers.setFirstName("FirstName"+i);
            customers.setLastName("LastName"+i);
            customers.setId(""+i);
            customers.setBirthYear(iCurrentYear - i);

            cache.put(Integer.valueOf(i), customers);
            }

        Integer  nYear  = Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR) + 5);
        Object[] arParm = {nYear};
        Integer  nAge   = Integer.valueOf(30);

        ValueExtractor itTestExt = new ReflectionExtractor("getAge", arParm);
        Filter         filter    = new LessEqualsFilter(itTestExt, nAge);
        assertTrue("ReflextionExtractor : Error invoking cache query Extractor",
                (cache.entrySet(filter).size() == 26));

        cache.destroy();
        }

    /**
     * Test for {@link DeserializationAccelerator}.
     */
    @Test
    public void testDeserializationAccelerator()
        {
        NamedCache cache = getNamedCache();

        for (int i = 0; i < 10*100; i++)
            {
            cache.put(i, new Value(i % 10));
            }

        Filter filter = new EqualsFilter("getValue", Integer.valueOf(3));

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("Standard use: wrong result", 100, cache.keySet(filter).size());
        int count = Value.s_nDeserializationCount;
        assertEquals("Standard use: wrong deserialization count", 1000, count);

        // install the pro-active accelerator
        Value.s_nDeserializationCount = 0;
        cache.addIndex(new DeserializationAccelerator(), false, null);
        Assert.assertEquals("Pro-active use: wrong result", cache.keySet(filter).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("Pro-active use: wrong deserialization count after query", 1000, count);

        cache.removeIndex(new DeserializationAccelerator());

        // install the on-demand accelerator
        Value.s_nDeserializationCount = 0;
        cache.addIndex(new DeserializationAccelerator(IdentityExtractor.INSTANCE, true), false, null);
        count = Value.s_nDeserializationCount;
        assertEquals("Pro-active use: wrong deserialization count after install", 0, count);

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("On-demand use: wrong result", cache.keySet(filter).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("On-demand use: wrong deserialization count after the first query", 1000, count);

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("On-demand use: wrong result", cache.keySet(filter).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("On-demand use: wrong deserialization count after the second query", 0, count);

        cache.removeIndex(new DeserializationAccelerator(IdentityExtractor.INSTANCE, true));

        cache.destroy();
        }

    /**
     * Test of lambda value extractor (COH-12711)
     */
    @Test
    public void testLambdaExtractor()
        {
        NamedCache<Integer, Value> cache = getNamedCache();

        for (int i = 0; i < 10*100; i++)
            {
            cache.put(i, new Value(i % 10));
            }

        ValueExtractor<Value, Integer> veReflect = new ReflectionExtractor<>("getValue");
        ValueExtractor<Value, Integer> veMethRef = Value::getValue;
        ValueExtractor<Value, Integer> veLambda  = v -> v.getValue();

        Filter filterReflect = new EqualsFilter<>("getValue", 3);
        Filter filterMethRef = new EqualsFilter<>(Value::getValue, 3);
        Filter filterLambda  = new EqualsFilter<>(veLambda, 3);

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("MethRef use: wrong result", 100, cache.keySet(filterMethRef).size());
        int count = Value.s_nDeserializationCount;
        assertEquals("MethRef use: wrong deserialization count: " + count, 1000, count);

        // install the deserialization accelerator
        Value.s_nDeserializationCount = 0;
        cache.addIndex(new DeserializationAccelerator(), false, null);
        Assert.assertEquals("MethRef use: wrong result", cache.keySet(filterMethRef).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("MethRef use: accelerator not used", 1000, count);

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("Lambda use: wrong result", cache.keySet(filterLambda).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("Lambda use: accelerator not used", 0, count);

        cache.removeIndex(new DeserializationAccelerator());

        // install a standard index and check whether the MethRef query use it
        Value.s_nDeserializationCount = 0;
        cache.addIndex(veReflect, false, null);
        Assert.assertEquals("MethRef use: wrong result ", cache.keySet(filterMethRef).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("MethRef use: index not used", 1000, count);

        // remove as a MethRef index
        cache.removeIndex(veMethRef);

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("Standard use: wrong result ", cache.keySet(filterReflect).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("index is not removed", 1000, count);

        // install a MethRef index and check whether the standard query uses it
        Value.s_nDeserializationCount = 0;
        cache.addIndex(veMethRef, false, null);
        Assert.assertEquals("MethRef use: wrong result ", cache.keySet(filterMethRef).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("MethRef use: index not used", 1000, count);

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("Standard use: wrong result ", cache.keySet(filterReflect).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("Standard use: index not used", 0, count);

        // remove as a standard index
        cache.removeIndex(veReflect);

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("Standard use: wrong result ", cache.keySet(filterReflect).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("index is not removed", 1000, count);

        // install a lambda index and check whether it is being used
        Value.s_nDeserializationCount = 0;
        cache.addIndex(veLambda, false, null);
        Assert.assertEquals("Lambda use: wrong result ", cache.keySet(filterLambda).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("MethRef use: index not used", 1000, count);

        // remove lambda index
        cache.removeIndex(veLambda);

        Value.s_nDeserializationCount = 0;
        Assert.assertEquals("Lambda use: wrong result ", cache.keySet(filterLambda).size(), 100);
        count = Value.s_nDeserializationCount;
        assertEquals("index is not removed", 1000, count);

        cache.destroy();
        }

    /**
     * Test to ensure any indices for ValueExtractors within a MultiExtractor
     * are used.
     */
    @Test
    public void testMultiExtractorIndexUsage()
        {
        NamedCache cache = getNamedCache();
        try
            {
            CountingExtractor extractor = new CountingExtractor();

            cache.addIndex(extractor, false, null);

            cache.put(1, 1);

            List listResult = (List) cache.invoke(1, new ExtractorProcessor(
                    new MultiExtractor(new ValueExtractor[] {extractor})));

            assertEquals(Integer.valueOf(1), listResult.get(0));
            assertEquals(1, CountingExtractor.f_counter.get());
            }
        finally
            {
            cache.destroy();
            CountingExtractor.f_counter.set(0);
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void testMustNotExtractFromRuntime()
        throws Throwable
        {
        ValueExtractor<Runtime, String> extractor = new UniversalExtractor<>("toString()");
        try
            {
            String s = extractor.extract(Runtime.getRuntime());
            fail("must throw exception");
            }
        catch (WrapperException e)
            {
            System.out.println(e.getOriginalException().getLocalizedMessage());
            throw e.getOriginalException();
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void testMustNotExtractFromClass()
        throws Throwable
        {
        ValueExtractor<Class, String> extractor = new UniversalExtractor<>("toString()");
        try
            {
            String s = extractor.extract(String.class);
            fail("must throw exception");
            }
        catch (WrapperException e)
            {
            System.out.println(e.getOriginalException().getLocalizedMessage());
            throw e.getOriginalException();
            }
        }

    /**
    * Return the cache used by all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        return getNamedCache("dist-test");
        }

    public static class Value
            implements ExternalizableLite, PortableObject
        {
        public Value()
            {
            }

        public Value(int iValue)
            {
            m_iValue = iValue;
            }

        public int getValue()
            {
            return m_iValue;
            }

        public void readExternal(DataInput in)
            throws IOException
            {
            m_iValue = in.readInt();
            s_nDeserializationCount++;
            }

        public void writeExternal(DataOutput out)
            throws IOException
            {
            out.writeInt(m_iValue);
            }

        public void readExternal(PofReader in)
            throws IOException
            {
            m_iValue = in.readInt(0);
            s_nDeserializationCount++;
            }

        public void writeExternal(PofWriter out)
            throws IOException
            {
            out.writeInt(0, m_iValue);
            }

        private int m_iValue;

        public static int s_nDeserializationCount;
        }

    // ----- inner class: CountingExtractor ---------------------------------

    public static class CountingExtractor
            extends AbstractExtractor
            implements ExternalizableLite, PortableObject
        {
        // ----- ValueExtractor interface -----------------------------------

        @Override
        public Object extract(Object oValue)
            {
            f_counter.incrementAndGet();
            return oValue;
            }

        // ----- object methods ---------------------------------------------

        @Override
        public int hashCode()
            {
            return 17;
            }

        @Override
        public boolean equals(Object obj)
            {
            return obj != null && getClass() == obj.getClass();
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }

        // ----- data members -----------------------------------------------

        protected static final AtomicInteger f_counter = new AtomicInteger();
        }
    }