/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.NamedCache;

import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;

import java.lang.IllegalArgumentException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;

/**
 * A functional tests for a Coherence*Extend client that uses
 * configured trigger listeners.
 *
 * @author par  2013.09.23
 *
 * @since @BUILDVERSION@
 */
public class TriggerListenerTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public TriggerListenerTests()
        {
        super("client-cache-config.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("TriggerListenerTests", "extend",
                                                "server-cache-config.xml");
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("TriggerListenerTests");
        }

    // ----- MapTrigger Extend tests ----------------------------------------

    /**
     * Test configured MapTrigger operation.
     */
    @Test
    public void testConfiguredTriggerListener()
        {
        final NamedCache cache = getNamedCache(CACHE_NAME);
        cache.clear();

        Person pIn = new Person("123-45-6789", "Eddie", "Van Halen", 1955, 
                "987-65-4321", new String[] {"456-78-9123"});

        cache.put(1, pIn);
        Person pOut = (Person) cache.get(1);
  
        assertEquals(pIn.getLastName().toUpperCase(), pOut.getLastName());
        }


    /**
     * Factory method to instantiate configured MapTrigger
     */
    public static MapTriggerListener createTriggerListener(String sCacheName)
        {
        if (CACHE_NAME.equals(sCacheName))
            {
            return new MapTriggerListener(new PersonMapTrigger());
            }
        else
            {
            throw new IllegalArgumentException("Unknown cache name " + sCacheName);
            }
        }
 
    // ----- Inner class: PersonMapTrigger ---------------------------------

    /**
     * Trigger class to modify value being put into the cache.
     * Changes last name of Person instance to all upper case.
     */
    public static class PersonMapTrigger
            implements MapTrigger, PortableObject
        {

        /*
         * Default constructor.
         */
        public PersonMapTrigger()
            {
            }

        /**
         * {@inheritDoc}
         */
        public void process(MapTrigger.Entry entry)
            {
            Person person  = (Person) entry.getValue();
            String sName   = person.getLastName();
            String sNameUC = sName.toUpperCase();

            if (!sNameUC.equals(sName))
               {
               person.setLastName(sNameUC);
               entry.setValue(person);
               }
            }

        // ----- PortableObject implementation ----------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
            {
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
            {
            }

        // ---- Object implementation -------------------------------------

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o)
            {
            return o != null && o.getClass() == this.getClass();
            }

        /**
         * {@inheritDoc}
         */
        public int hashCode()
            {
            return getClass().getName().hashCode();
            }
        }

    // ----- fields and constants ------------------------------------------

    static final String CACHE_NAME = "dist-extend-trigger-listener";
    }
