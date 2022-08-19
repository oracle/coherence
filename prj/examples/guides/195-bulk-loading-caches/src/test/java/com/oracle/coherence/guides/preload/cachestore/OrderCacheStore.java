/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.preload.cachestore;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.guides.preload.model.Order;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.util.Base;

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
 * An implementation of a {@link CacheStore} to store {@link Order orders}.
 * <p>
 * This is a very basic implementation of {@link CacheStore}.
 * A better implementation would use some form of connection pooling and a better way to
 * determine whether a store was an insert or and update.
 * <p>
 * To improve efficiency, this implementation does implement {#link {@link #loadAll(Collection)}},
 * {@link #storeAll(Map)} and {@link #eraseAll(Collection)}. These could be even better if the
 * DB supported some sort of bulk update.
 */
public class OrderCacheStore
        implements CacheStore<Integer, Order>
    {
    /**
     * Construct a cache store.
     *
     * @param dbURL  the database connection URL
     *
     * @throws SQLException if there is an error creating the DB connection
     */
    public OrderCacheStore(String dbURL) throws SQLException
        {
        this(DriverManager.getConnection(dbURL));
        }

    /**
     * Construct a cache store.
     *
     * @param connection  the database connection
     */
    public OrderCacheStore(Connection connection)
        {
        this.connection = connection;
        }

    @Override
    public Order load(Integer key)
        {
        try (PreparedStatement statement = connection.prepareStatement(LOAD_SQL))
            {
            statement.setInt(1, key);
            try (ResultSet resultSet = statement.executeQuery())
                {
                if (resultSet.next())
                    {
                    return orderFromResultSet(resultSet);
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
    public Map<Integer, Order> loadAll(Collection<? extends Integer> keys)
        {
        String inClause = keys.stream().map(String::valueOf).collect(Collectors.joining(","));
        String query = String.format(LOAD_ALL_SQL, inClause);

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery())
            {
            Map<Integer, Order> entries = new HashMap<>();
            while (resultSet.next())
                {
                Order order = orderFromResultSet(resultSet);
                entries.put(order.getId(), order);
                }
            Logger.info("Loaded " + keys + " order(s) from the database");
            return entries;
            }
        catch (SQLException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public void store(Integer key, Order order)
        {
        storeAll(Collections.singletonMap(key, order));
        }

    @Override
    public void storeAll(Map<? extends Integer, ? extends Order> entries)
        {
        try (PreparedStatement insert = connection.prepareStatement(INSERT_SQL);
             PreparedStatement update = connection.prepareStatement(UPDATE_SQL))
            {
            for (Map.Entry<? extends Integer, ? extends Order> entry : entries.entrySet())
                {
                int id = entry.getKey();
                Order order = entry.getValue();

                // the following is very inefficient; it is recommended to use DB
                // specific functionality that is, REPLACE for MySQL or MERGE for Oracle, etc
                PreparedStatement statement = load(entry.getKey()) != null ? update : insert;

                statement.setInt(1, order.getCustomerId());
                statement.setInt(2, order.getItemId());
                statement.setBigDecimal(3, order.getItemPrice());
                statement.setInt(4, order.getQuantity());
                statement.setInt(5, id);
                statement.execute();
                }
            connection.commit();
            Logger.info("Stored " + entries.size() + " order(s) to the database");
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
            Logger.info("Erased 1 order from the database");
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
            Logger.info("Erased " + keys.size() + " order(s) from the database");
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
     * Creates an {@link Order} from a {@link ResultSet}.
     *
     * @param resultSet the active {@link ResultSet}
     * @return a new {@link Order}
     * @throws SQLException if any SQL Errors.
     */
    private Order orderFromResultSet(ResultSet resultSet) throws SQLException
        {
        return new Order(resultSet.getInt(1),
                            resultSet.getInt(2),
                            resultSet.getInt(3),
                            resultSet.getBigDecimal(4),
                            resultSet.getInt(5));
        }

    // ----- constants ---------------------------------------------------------------

    public static final String LOAD_SQL = "SELECT id, customerId, itemId, itemPrice, quantity FROM orders where id = ?";

    public static final String LOAD_ALL_SQL = "SELECT id, customerId, itemId, itemPrice, quantity FROM orders where id in (%s)";

    public static final String INSERT_SQL = "INSERT INTO orders (customerId, itemId, itemPrice, quantity, id) VALUES(?, ?, ?, ?, ?)";

    public static final String UPDATE_SQL = "UPDATE orders SET customerId = ?, itemId = ?, itemPrice = ?, quantity = ? where id = ?";

    public static final String DELETE_SQL = "DELETE FROM orders where id = ?";

    public static final String DELETE_ALL_SQL = "DELETE FROM orders where id in (%s)";

    // ----- data members ---------------------------------------------------

    /**
     * Database connection.
     */
    private final Connection connection;
    }

