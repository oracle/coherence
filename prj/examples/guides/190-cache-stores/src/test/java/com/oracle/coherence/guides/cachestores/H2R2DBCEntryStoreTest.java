/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.NonBlockingEntryStore;
import com.tangosol.net.cache.TypeAssertion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test class demonstrating the use of {@link NonBlockingEntryStore}
 */
public class H2R2DBCEntryStoreTest
        extends AbstractCacheStoreTest
    {

    /**
     * Set up of the test.
     *
     * @throws SQLException if any SQL errors occur
     */
    // #tag::initial[]
    @BeforeAll
    public static void startup() throws SQLException
        {
        createTable();

        startupCoherence("h2r2dbc-entry-store-cache-config.xml");
        }

    /**
     * Performs some cache manipulations.
     */
    @Test
    public void testNonBlockingEntryStore()
        {
        NamedMap<Long, Person> namedMap = getSession()
                .getMap("H2Person", TypeAssertion.withTypes(Long.class, Person.class));

        Person person1 = namedMap.get(Long.valueOf(101));
        assertEquals("Robert", person1.getFirstname());
        // #end::initial[]

        // #tag::insert[]
        Person person2 = new Person(Long.valueOf(102), 40, "Tony", "Soprano");
        namedMap.put(Long.valueOf(102), person2);

        Person person3 = namedMap.get(Long.valueOf(102));
        assertEquals("Tony", person3.getFirstname());
        // #end::insert[]

        // #tag::remove[]
        namedMap.remove(Long.valueOf(101));
        namedMap.remove(Long.valueOf(102));
        assertEquals(null, namedMap.get(Long.valueOf(101)));
        assertEquals(null, namedMap.get(Long.valueOf(102)));
        // #end::remove[]

        // #tag::insertmulti[]
        Map<Long, Person> map = new HashMap<>();
        for (int i = 1; i <= 10; i++)
            {
            map.put(Long.valueOf(i), new Person(Long.valueOf(i), 20 + i, "firstname" + i, "lastname" + i));
            }
        namedMap.putAll(map);
        Person person5 = namedMap.get(Long.valueOf(5));
        assertEquals("firstname5", person5.getFirstname());
        assertEquals(10, namedMap.size());
        // #end::insertmulti[]
        }

    /**
     * Creates the table.
     *
     * @throws SQLException if any SQL errors occur
     */
    protected static void createTable() throws SQLException
        {
        Connection connection= DriverManager.getConnection("jdbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1");
        Statement s=connection.createStatement();
        try
            {
            s.execute("DROP TABLE PERSON");
            }
        catch(SQLException sqle)
            {
            Logger.info("Table not found, not dropping");
            }
        s.execute("CREATE TABLE PERSON (ID LONG PRIMARY KEY, AGE INT, FIRSTNAME VARCHAR(64), LASTNAME VARCHAR(64));");
        s.execute("INSERT INTO PERSON VALUES (101, 70, 'Robert', 'Redford');");
        }
    }
