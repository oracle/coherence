/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.oracle.coherence.common.base.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.oracle.coherence.guides.cachestores.HSQLDbCacheStore.DB_URL;


/**
 * A abstract test class with common functionality {@link HSQLDbCacheStore}.
 *
 * @author Tim Middleton  2021.02.17
 */
public class AbstractHSqlDbCacheStoreTest
        extends AbstractCacheStoreTest {

    /**
     * Database connection.
     */
    protected static Connection dbConn;

    /**
     * Maximum customers to add.
     */
    protected static final int MAX_CUSTOMERS = 100;

    /**
     * Convert nanos to millis.
     */
    protected static final float NANOS = 1000000.0f;

    /**
     * Cache name.
     */
    protected static String cacheName;

    /**
     * Startup the test specifying a cache name.
     *
     * @param cache cache name
     *
     * @throws SQLException if any SQL errors
     */
    public static void _startup(String cache) throws SQLException {
        cacheName = cache;
        // get the embedded db and populate
        dbConn = DriverManager.getConnection(DB_URL);
        createTable();

        startupCoherence("hsqldb-cache-store-cache-config.xml"); // <1>
    }

    /**
     * Returns the cache name.
     *
     * @return the cache name.
     */
    protected static String getCacheName() {
        return cacheName;
    }

    /**
     * Returns the count of customers from the database.
     *
     * @return the count of customers from the database
     *
     * @throws SQLException if any SQL errors
     */
    public int getCustomerDBCount() {
        int count = 0;
        try {
            Statement statement = dbConn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM CUSTOMER");
            while (resultSet.next()) {
                count = resultSet.getInt(1);
                break;
            }
            closeQuiet(resultSet);
            closeQuiet(statement);
        }
        catch (SQLException sqlException) {
        }
        return count;
    }

    /**
     * Returns an individual {@link Customer} from the database.
     *
     * @param key customer id
     *
     * @return a {@link Customer}
     *
     * @throws SQLException if any SQL errors
     */
    protected Customer getCustomerFromDB(int key) throws SQLException {
        String            query     = "SELECT id, name, address, creditLimit FROM CUSTOMER where id = ?";
        PreparedStatement statement = null;
        ResultSet         resultSet = null;
        try {
            statement = dbConn.prepareStatement(query);
            statement.setInt(1, key);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                return new Customer(resultSet.getInt(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getInt(4));
            }
            // nothing found
            return null;

        }
        finally {
            closeQuiet(resultSet);
            closeQuiet(statement);
        }
    }

    /**
     * Load {@link Customer}s into the DB.
     *
     * @throws SQLException if any SQL errors
     */
    protected static void reloadCustomersDB() throws SQLException {
        dbConn.prepareStatement("DELETE FROM CUSTOMER WHERE TRUE").execute();

        // insert
        for (int i = 1; i <= MAX_CUSTOMERS; i++) {
            PreparedStatement preparedStatement = dbConn.prepareStatement(
                    "INSERT INTO CUSTOMER (id,name,address,creditLimit) VALUES(?, ?, ?, ?)");
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "Customer " + i);
            preparedStatement.setString(3, "Customer address " + i);
            preparedStatement.setInt(4, 5000);
            preparedStatement.execute();
        }

        dbConn.commit();

        Logger.info("Inserted " + MAX_CUSTOMERS + " Customers");
    }

    /**
     * Creates the table.
     *
     * @throws SQLException if any SQL errors
     */
    protected static void createTable() throws SQLException {
        Statement        statement = dbConn.createStatement();
        DatabaseMetaData metaData  = dbConn.getMetaData();
        ResultSet        resultSet = metaData.getTables(null, null, "CUSTOMER", null);

        // check if table exists and drop it
        if (resultSet.next()) {
            statement.execute("DROP TABLE CUSTOMER");
        }

        String sql = "CREATE TABLE CUSTOMER (id int primary key, name varchar(128), address varchar(265), creditLimit int)";
        statement.execute(sql);
        closeQuiet(statement);
    }

    /**
     * Returns a message to display the duration of an event.
     *
     * @param duration duration in nanos
     * @param type     the type to display
     *
     * @return a message
     */
    protected String getDurationMessage(long duration, String type) {
        return "Time for " + type + " " + String.format("%.3f ms", duration / NANOS);
    }
}

