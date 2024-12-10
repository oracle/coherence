/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.cachestores;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.util.Base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Map;


/**
 * An implementation of a {@link CacheStore} which stores values into an embedded HSQLDb database.
 * Note: This is not be a normal use case as a database would be a separate process, but we are just using embedded option for
 * convenience.
 *
 * @author Tim Middleton 2020.02.19
 */
// #tag::class[]
public class HSQLDbCacheStore
        extends Base
        implements CacheStore<Integer, Customer> {
// #end::class[]
    /**
     * Table name.
     */
    private final String tableName;

    /**
     * Database connection.
     */
    private Connection dbConn;

    /**
     * Connection URL for in-memory embedded HSQLDb Database.
     */
    public static final String DB_URL = "jdbc:hsqldb:mem:test";

    // #tag::constructor[]
    /**
     * Construct a cache store.
     *
     * @param cacheName cache name
     *
     * @throws SQLException if any SQL errors
     */
    public HSQLDbCacheStore(String cacheName) throws SQLException {
        this.tableName = cacheName;
        dbConn = DriverManager.getConnection(DB_URL);
        Logger.info("HSQLDbCacheStore constructed with cache Name " + cacheName);
    }
    // #end::constructor[]

    // #tag::load[]
    @Override
    public Customer load(Integer key) {
        String            query     = "SELECT id, name, address, creditLimit FROM " + tableName + " where id = ?";
        PreparedStatement statement = null;
        ResultSet         resultSet = null;

        try {
            statement = dbConn.prepareStatement(query);
            statement.setInt(1, key);
            resultSet = statement.executeQuery();

            return resultSet.next() ? createFromResultSet(resultSet) : null;
        }
        catch (SQLException sqle) {
            throw ensureRuntimeException(sqle);
        }
        finally {
            close(resultSet);
            close(statement);
        }
    }
    // #end::load[]

    // #tag::store[]
    @Override
    public void store(Integer key, Customer customer) {
        try {
            storeInternal(key, customer);
            dbConn.commit();
        }
        catch (Exception e) {
            throw ensureRuntimeException(e);
        }
    }
    // #end::store[]

    // #tag::storeInternal[]
    /**
     * Store a {@link Customer} object using the id. This method does not issue a
     * commit so that either the store or storeAll method can reuse this.
     *
     * @param key      customer id
     * @param customer {@link Customer} object
     */
    private void storeInternal(Integer key, Customer customer) {
        // the following is very inefficient; it is recommended to use DB
        // specific functionality that is, REPLACE for MySQL or MERGE for Oracle
        String query = load(key) != null
                       ? "UPDATE " + tableName + " SET name = ?, address = ?, creditLimit = ? where id = ?"
                       : "INSERT INTO " + tableName + " (name, address, creditLimit, id) VALUES(?, ?, ?, ?)";
        PreparedStatement statement = null;

        try {
            statement = dbConn.prepareStatement(query);
            statement.setString(1, customer.getName());
            statement.setString(2, customer.getAddress());
            statement.setInt(3, customer.getCreditLimit());
            statement.setInt(4, customer.getId());
            statement.execute();
        }
        catch (SQLException sqle) {
            throw ensureRuntimeException(sqle);
        }
        finally {
            close(statement);
        }
    }
    // #end::storeInternal[]

    // #tag::storeAll[]
    @Override
    public void storeAll(Map<? extends Integer, ? extends Customer> mapEntries) {
        try {
            for (Customer customer : mapEntries.values()) {
                storeInternal(customer.getId(), customer);
            }

            dbConn.commit();
            Logger.info("Ran storeAll on " + mapEntries.size() + " entries");
        }
        catch (Exception e) {
            try {
                dbConn.rollback();
            }
            catch (SQLException ignore) { }
            throw ensureRuntimeException(e);
        }
    }
    // #end::storeAll[]

    // #tag::erase[]
    @Override
    public void erase(Integer key) {  // <7>
        String            query     = "DELETE FROM " + tableName + " where id = ?";
        PreparedStatement statement = null;

        try {
            statement = dbConn.prepareStatement(query);
            statement.setInt(1, key);
            statement.execute();
            dbConn.commit();
        }
        catch (SQLException sqle) {
            throw ensureRuntimeException(sqle);
        }
        finally {
            close(statement);
        }
    }
    // #end::erase[]

    /**
     * Creates a {@link Customer} from a {@link ResultSet}.
     *
     * @param resultSet the active {@link ResultSet}
     *
     * @return a new {@link Customer}
     *
     * @throws SQLException if any SQL Errors.
     */
    private Customer createFromResultSet(ResultSet resultSet) throws SQLException {
        return new Customer(resultSet.getInt(1),
                resultSet.getString(2),
                resultSet.getString(3),
                resultSet.getInt(4));
    }

    /**
     * Close a resource silently.
     *
     * @param closeable the {@link AutoCloseable} to close
     */
    private void close(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (Exception ignore) {
            }
        }
    }
}

