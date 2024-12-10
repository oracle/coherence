/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.TypeAssertion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A test class to exercise the {@link HSQLDbCacheStore}
 *
 * @author Tim Middleton  2021.02.23
 */
public class HibernateCacheStoreTest
        extends AbstractCacheStoreTest {

    /**
     * HSqlDb Connection.
     */
    private static Connection connection;

    // #tag::initial[]
    @BeforeAll
    public static void startup() throws SQLException {
        startupCoherence("hibernate-cache-store-cache-config.xml");
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:test");
    }
    // #end::initial[]
    
    @Test
    public void testHibernateCacheStore() throws SQLException {
        NamedMap<Long, Person> namedMap = getSession()
                .getMap("Person", TypeAssertion.withTypes(Long.class, Person.class));


        // #tag::put[]
        Person person1 = new Person(1L, 50, "Tom", "Jones");
        namedMap.put(person1.getId(), person1);
        assertEquals(1, namedMap.size());
        // #end::put[]

        // #tag::get[]
        Person person2 = getPersonFromDB(1L);
        person1 = namedMap.get(1L);
        assertNotNull(person2);
        assertEquals(person2, person1);
        // #end::get[]

        // #tag::update[]
        person2.setAge(100);
        namedMap.put(person2.getId(), person2);
        Person person3 = getPersonFromDB(1L);
        assertNotNull(person2);
        assertEquals(person3.getAge(), 100);
        // #end::update[]

        // #tag::remove[]
        namedMap.remove(1L);
        Person person4 = getPersonFromDB(1L);
        assertNull(person4);
        // #end::remove[]
    }

    /**
     * Returns an individual {@link Person} from the database.
     *
     * @param id person id
     *
     * @return a {@link Person}
     *
     * @throws SQLException if any SQL errors
     */
    protected Person getPersonFromDB(long id) throws SQLException {
        String            query     = "SELECT id, age, firstName, lastName FROM PERSON where id = ?";
        PreparedStatement statement = null;
        ResultSet         resultSet = null;
        try {
            statement = connection.prepareStatement(query);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                return new Person(resultSet.getLong(1),
                        resultSet.getInt(2),
                        resultSet.getString(3),
                        resultSet.getString(4));
            }
            // nothing found
            return null;

        }
        finally {
            closeQuiet(resultSet);
            closeQuiet(statement);
        }
    }
}