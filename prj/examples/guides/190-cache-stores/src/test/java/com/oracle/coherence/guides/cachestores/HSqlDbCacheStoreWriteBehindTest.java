/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.util.Base;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * A test class to exercise the {@link HSQLDbCacheStore} but using a write behind cache.
 *
 * @author Tim Middleton  2021.02.17
 */
public class HSqlDbCacheStoreWriteBehindTest
        extends AbstractHSqlDbCacheStoreTest {
    
    // #tag::initial[]
    @BeforeAll
    public static void startup() throws SQLException {
        _startup("CustomerWriteBehind");
    }

    @Test
    public void testHsqlDbCacheStore() throws SQLException {
        try {
            NamedMap<Integer, Customer> namedMap = getSession()
                    .getMap(getCacheName(), TypeAssertion.withTypes(Integer.class, Customer.class));

            // cache should be empty
            assertEquals(0, namedMap.size());

            // Customer table should contain no customers
            assertEquals(0, getCustomerDBCount());
            // #end::initial[]

            // #tag::insert[]
            // add 10 customers
            Map<Integer, Customer> map = new HashMap<>();
            for (int i = 1; i <= 100; i++) {
                map.put(i, new Customer(i, "Name " + i, "Address " + i, i *  1000));
            }
            namedMap.putAll(map);

            // initial check of the database should return 0 as we have write-delay set
            assertEquals(0, getCustomerDBCount());
            // #end::insert[]

            // #tag::wait[]
            // sleep for 15 seconds and the database should be populated as write-delay has elapsed
            Base.sleep(15000L);

            // Issuing Eventually assertThat in case of heavily loaded machine
            Eventually.assertThat(invoking(this).getCustomerDBCount(), is(100));
            // #end::wait[]
        }
        finally {
            dbConn.close();
        }
    }
}
// #end::class[]
