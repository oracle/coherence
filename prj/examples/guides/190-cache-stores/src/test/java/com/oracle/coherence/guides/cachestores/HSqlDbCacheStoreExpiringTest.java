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
import com.tangosol.util.Base;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * A test class to exercise the {@link HSQLDbCacheStore} but using an expiring cache.
 *
 * @author Tim Middleton  2021.02.17
 */
public class HSqlDbCacheStoreExpiringTest
        extends AbstractHSqlDbCacheStoreTest {

    // #tag::initial[]
    @BeforeAll
    public static void startup() throws SQLException {
        _startup("CustomerExpiring");
        reloadCustomersDB();
    }

    @Test
    public void testHSQLDbCacheStore() throws SQLException {
        try {
            NamedMap<Integer, Customer> namedMap = getSession()
                    .getMap(getCacheName(), TypeAssertion.withTypes(Integer.class, Customer.class)); // <2>

            // cache should be empty
            assertEquals(0, namedMap.size());

            // Customer table should contain the correct number of customers
            assertEquals(MAX_CUSTOMERS, getCustomerDBCount());
            // #end::initial[]

            // #tag::readThrough1[]
            // expiry delay is setup to 20s for the cache and refresh ahead is 0.5 which
            // means that after 10s if the entry is read the old value is returned but after which a
            // refresh is done which means that subsequents reads will be fast as the new value is already present
            long start = System.nanoTime();
            Customer customer = namedMap.get(1);
            long duration = System.nanoTime() - start;
            Logger.info(getDurationMessage(duration, "read-through"));
            assertEquals(1, customer.getId());
            // #end::readThrough1[]

            // #tag::readThrough2[]
            // update the database
            updateCustomerCreditLimitInDB(1, 10000);

            // sleep for 11 seconds get the cache entry, we should still get the original value
            Base.sleep(11000L);
            assertEquals(5000, namedMap.get(1).getCreditLimit());
            // #end::readThrough2[]

            // #tag::readThrough3[]
            // wait for another 10 seconds and the refresh-ahead should have completed
            Base.sleep(10000L);

            start = System.nanoTime();
            customer = namedMap.get(1);
            duration = System.nanoTime() - start;
            Logger.info(getDurationMessage(duration, "after refresh-ahead"));
            // #end::readThrough3[]
        }
        finally {
            dbConn.close();
        }
    }

    /**
     * Update a customers credit limit in the database.
     * @param id    customer to update
     * @param newLimit new credit limit
     * @throws SQLException if and SQL errors
     */
    protected void updateCustomerCreditLimitInDB(int id, int newLimit) throws SQLException {
        PreparedStatement statement = null;

        try {
            statement = dbConn.prepareStatement( "UPDATE CUSTOMER SET creditLimit = ? where id = ?");
            statement.setInt(1, id);
            statement.setInt(2, newLimit);

            statement.execute();
            dbConn.commit();
        }
        catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
        finally {
            statement.close();
        }
    }
}
// #end::class[]
