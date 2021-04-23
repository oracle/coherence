/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.util.processor.PreloadRequest;

import java.sql.SQLException;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * A test class to exercise the {@link HSQLDbCacheStore}.
 *
 * @author Tim Middleton  2021.02.17
 */
public class HSqlDbCacheStoreTest
        extends AbstractHSqlDbCacheStoreTest {

    // #tag::initial[]
    @BeforeAll
    public static void startup() throws SQLException {
        _startup("Customer");
        reloadCustomersDB();
    }

    @Test
    public void testHSqlDbCacheStore() throws SQLException {
        try {
            NamedMap<Integer, Customer> namedMap = getSession()
                    .getMap(getCacheName(), TypeAssertion.withTypes(Integer.class, Customer.class)); // <2>

            // cache should be empty
            assertEquals(0, namedMap.size());

            // Customer table should contain the correct number of customers
            assertEquals(MAX_CUSTOMERS, getCustomerDBCount());
            // #end::initial[]

            // #tag::load1[]
            long start = System.nanoTime();
            // issue a get and it will load the existing customer
            Customer customer = namedMap.get(1);
            long     duration = System.nanoTime() - start;
            Logger.info(getDurationMessage(duration, "read-through"));

            assertEquals(1, namedMap.size());
            assertNotNull(customer);
            assertEquals(1, customer.getId());
            assertEquals("Customer 1", customer.getName());
            // #end::load1[]

            // #tag::load2[]
            // issue a get again and it should be quicker
            start = System.nanoTime();
            customer = namedMap.get(1);
            duration = System.nanoTime() - start;
            Logger.info(getDurationMessage(duration, "no read-through"));
            // #end::load2[]

            // #tag::remove[]
            // remove a customer number 1
            namedMap.remove(1);

            // we should have one less customer in the database
            assertEquals(MAX_CUSTOMERS - 1, getCustomerDBCount());
            assertNull(namedMap.get(1));

            // customer should not exist in DB
            assertNull(getCustomerFromDB(1));
            // #end::remove[]

            // #tag::update[]
            // Load customer 2
            Customer customer2 = namedMap.get(2);
            assertNotNull(customer2);

            // update customer 2 with "New Address"
            namedMap.compute(2, (k, v)->{
                v.setAddress("New Address");
                return v;
            });

            // customer should have new address in cache and DB
            assertEquals("New Address", namedMap.get(2).getAddress());
            assertEquals("New Address", getCustomerFromDB(2).getAddress());
            // #end::update[]

            // #tag::addRemove[]
            // add a new customer 1010
            namedMap.put(101, new Customer(101, "Customer Name 101", "Customer address 101", 20000));
            assertTrue(namedMap.containsKey(101));
            assertEquals("Customer address 101", getCustomerFromDB(101).getAddress());
            
            namedMap.remove(101);
            assertFalse(namedMap.containsKey(101));
            assertNull(getCustomerFromDB(101));
            // #end::addRemove[]

            // #tag::loadData[]
            // clean the cache and reset the database
            namedMap.clear();
            reloadCustomersDB();

            assertEquals(0, namedMap.size());

            // demonstrate loading the cache from the current contents of the DB
            // this can be done many ways but for this exercise you could fetch all the
            // customer id' from the DB but as we know there are 1..100 we can pretend we have.
            Set<Integer> keySet = IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toSet());
            namedMap.invokeAll(keySet, new PreloadRequest<>());

            // cache should be fully primed
            assertEquals(MAX_CUSTOMERS, namedMap.size());
            // #end::loadData[]
        }
        finally {
            dbConn.close();
        }
    }
}