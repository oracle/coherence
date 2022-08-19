/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.preload.cachestore;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.guides.preload.model.Customer;

import com.tangosol.net.cache.CacheStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.util.stream.Collectors;

/**
 * An implementation of a {@link CacheStore} to store {@link Customer customers}.
 * <p>
 * This is a very basic implementation of {@link CacheStore}.
 * A better implementation would use some form of connection pooling and a better way to
 * determine whether a store was an insert or and update.
 * <p>
 * To improve efficiency, this implementation does implement {#link {@link #loadAll(Collection)}},
 * {@link #storeAll(Map)} and {@link #eraseAll(Collection)}. These could be even better if the
 * DB supported some sort of bulk update.
 */
public class CustomerCacheStore
        implements CacheStore<Integer, Customer>
    {
    /**
     * Construct a cache store.
     *
     * @param dbURL  the database connection URL
     *
     * @throws SQLException if there is an error creating the DB connection
     */
    public CustomerCacheStore(String dbURL) throws SQLException
        {
        this(DriverManager.getConnection(dbURL));
        }

    /**
     * Construct a cache store.
     *
     * @param connection  the database connection
     */
    public CustomerCacheStore(Connection connection)
        {
        this.connection = connection;
        }

    @Override
    public Customer load(Integer key)
        {
        try (PreparedStatement statement = connection.prepareStatement(LOAD_SQL))
            {
            statement.setInt(1, key);
            try (ResultSet resultSet = statement.executeQuery())
                {
                if (resultSet.next())
                    {
                    return customerFromResultSet(resultSet);
                    }
                return null;
                }
            }
        catch (SQLException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public Map<Integer, Customer> loadAll(Collection<? extends Integer> keys)
        {
        String inClause = keys.stream().map(String::valueOf).collect(Collectors.joining(","));
        String query = String.format(LOAD_ALL_SQL, inClause);

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery())
            {
            Map<Integer, Customer> entries = new HashMap<>();
            while (resultSet.next())
                {
                Customer customer = customerFromResultSet(resultSet);
                entries.put(customer.getId(), customer);
                }
            Logger.info("Loaded " + keys + " customer(s) from the database");
            return entries;
            }
        catch (SQLException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public void store(Integer key, Customer customer)
        {
        storeAll(Collections.singletonMap(key, customer));
        }

    @Override
    public void storeAll(Map<? extends Integer, ? extends Customer> entries)
        {
        try (PreparedStatement insert = connection.prepareStatement(INSERT_SQL);
             PreparedStatement update = connection.prepareStatement(UPDATE_SQL))
            {
            for (Map.Entry<? extends Integer, ? extends Customer> entry : entries.entrySet())
                {
                int id = entry.getKey();
                Customer customer = entry.getValue();

                // the following is very inefficient; it is recommended to use DB
                // specific functionality that is, REPLACE for MySQL or MERGE for Oracle, etc
                PreparedStatement statement = load(entry.getKey()) != null ? update : insert;

                statement.setString(1, customer.getName());
                statement.setString(2, customer.getAddress());
                statement.setInt(3, customer.getCreditLimit());
                statement.setInt(4, id);
                statement.execute();
                }
            connection.commit();
            Logger.info("Stored " + entries.size() + " customer(s) to the database");
            }
        catch (Exception e)
            {
            rollback(e);
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public void erase(Integer key)
        {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL))
            {
            statement.setInt(1, key);
            statement.execute();
            connection.commit();
            Logger.info("Erased 1 customer from the database");
            }
        catch (SQLException e)
            {
            rollback(e);
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public void eraseAll(Collection<? extends Integer> keys)
        {
        String inClause = keys.stream().map(String::valueOf).collect(Collectors.joining(","));
        String query = String.format(DELETE_ALL_SQL, inClause);

        try (PreparedStatement statement = connection.prepareStatement(query))
            {
            statement.execute();
            connection.commit();
            Logger.info("Erased " + keys.size() + " customer(s) from the database");
            }
        catch (SQLException e)
            {
            rollback(e);
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    // ----- helper methods -------------------------------------------------

    private void rollback(Throwable thrown)
        {
        try
            {
            connection.rollback();
            }
        catch (SQLException e)
            {
            thrown.addSuppressed(e);
            }
        }

    /**
     * Creates a {@link Customer} from a {@link ResultSet}.
     *
     * @param resultSet the active {@link ResultSet}
     * @return a new {@link Customer}
     * @throws SQLException if any SQL Errors.
     */
    private Customer customerFromResultSet(ResultSet resultSet) throws SQLException
        {
        return new Customer(resultSet.getInt(1),
                            resultSet.getString(2),
                            resultSet.getString(3),
                            resultSet.getInt(4));
        }

    // ----- constants ---------------------------------------------------------------

    public static final String LOAD_SQL = "SELECT id, name, address, creditLimit FROM CUSTOMERS where id = ?";

    public static final String LOAD_ALL_SQL = "SELECT id, name, address, creditLimit FROM CUSTOMERS where id in (%s)";

    public static final String INSERT_SQL = "INSERT INTO CUSTOMERS (name, address, creditLimit, id) VALUES(?, ?, ?, ?)";

    public static final String UPDATE_SQL = "UPDATE CUSTOMERS SET name = ?, address = ?, creditLimit = ? where id = ?";

    public static final String DELETE_SQL = "DELETE FROM CUSTOMERS where id = ?";

    public static final String DELETE_ALL_SQL = "DELETE FROM CUSTOMERS where id in (%s)";

    // ----- data members ---------------------------------------------------

    /**
     * Database connection.
     */
    private final Connection connection;
    }

