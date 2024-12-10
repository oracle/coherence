/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Processors;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Coh22443Tests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Coh22443Tests()
        {
        super();
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        AbstractFunctionalTest._startup();
        }


    // ----- unit tests -----------------------------------------------------

    /**
     * Test to ensure ValueUpdater is Serializable.
     */
    @Test
    public void testBug32168588()
        {
        NamedCache cache = getNamedCache("dist-std-test");
        cache.clear();
        AbstractEntryProcessorTests.TestValue testValue = new AbstractEntryProcessorTests.TestValue();
        cache.put(1, testValue);
        assertEquals(cache.size(), 1);
        assertEquals(cache.get(1), testValue);
        cache.invoke(1, Processors.update(AbstractEntryProcessorTests.TestValue::setLongValue, 1L));
        testValue = (AbstractEntryProcessorTests.TestValue) cache.get(1);
        assertEquals(testValue.getLongValue().longValue(), 1L);
        }
    }
