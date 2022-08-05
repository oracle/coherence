/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.loaders;

import com.oracle.coherence.common.base.Converter;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBinaryJdbcPreloadTask<K, V>
        implements Runnable
    {
    /**
     * Create a {@link AbstractBinaryJdbcPreloadTask}
     *
     * @param connection    the database {@link Connection}
     * @param session       the Coherence {@link Session} to use to obtain the cache to load
     * @param batchSize     the number of entries to load in each {@link NamedMap#putAll(Map)} batch
     * @param decorationId  the decoration identifier
     */
    protected AbstractBinaryJdbcPreloadTask(Connection connection, Session session, int batchSize, int decorationId)
        {
        this.connection = connection;
        this.session = session;
        this.batchSize = batchSize;
        this.decorationId = decorationId;
        this.name = getClass().getSimpleName();
        }

    @Override
    public void run()
        {
        // Obtain a binary version of the cache
        NamedMap<Binary, Binary> namedMap = session.getMap(getMapName(),
                WithClassLoader.using(NullImplementation.getClassLoader()));

        try (PreparedStatement statement = prepareStatement();
             ResultSet resultSet = statement.executeQuery())
            {
            Map<K, V> batch = new HashMap<>(batchSize);

            while (resultSet.next())
                {
                K key = keyFromResultSet(resultSet);
                V value = valueFromResultSet(resultSet);
                batch.put(key, value);
                if (batch.size() >= batchSize)
                    {
                    load(batch, namedMap);
                    }
                }

            if (!batch.isEmpty())
                {
                load(batch, namedMap);
                }
            }
        catch (SQLException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public String toString()
        {
        return name + "(mapName=\""
                + getMapName()
                + "\" sql=\"" + getSQL()
                +  "\")";
        }

    @SuppressWarnings("unchecked")
    private void load(Map<K, V> batch, NamedMap<Binary, Binary> namedMap)
        {
        BackingMapManagerContext context = namedMap.getService().getBackingMapManager().getContext();
        Converter<K, Binary> keyConverter = context.getKeyToInternalConverter();
        Converter<V, Binary> valueConverter = context.getValueToInternalConverter();

        Map<Binary, Binary> decoratedBatch = new HashMap<>();
        for (Map.Entry<K, V> entry : batch.entrySet())
            {
            Binary binary = valueConverter.convert(entry.getValue());
            Binary decorated = ExternalizableHelper.decorate(binary, decorationId, Binary.NO_BINARY);
            decoratedBatch.put(keyConverter.convert(entry.getKey()), decorated);
            }

        namedMap.putAll(decoratedBatch);

        Logger.info("Preloader " + name + " loaded "
                            + batch.size() + " entries to cache " + namedMap.getName());
        batch.clear();
        }

    /**
     * Create the {@link PreparedStatement} to execute to obtain data from the database.
     * <p>
     * This can be overridden by tasks that need more than a simple {@link PreparedStatement}
     * created from the query returned by {@link #getSQL()}.
     *
     * @return the {@link PreparedStatement} to execute to obtain data from the database
     *
     * @throws SQLException if there is an error creating the {@link PreparedStatement}
     */
    protected PreparedStatement prepareStatement() throws SQLException
        {
        String query = getSQL();
        return connection.prepareStatement(query);
        }

    /**
     * Return the SQL query to use to get data from the database.
     *
     * @return the SQL query to use to get data from the database
     */
    protected abstract String getSQL();

    /**
     * Return the name of the {@link NamedMap} or {@link com.tangosol.net.NamedCache} to load.
     *
     * @return the name of the {@link NamedMap} or {@link com.tangosol.net.NamedCache} to load
     */
    protected abstract String getMapName();

    /**
     * Create a cache key from the current row of the {@link ResultSet}.
     *
     * @param resultSet  the {@link ResultSet} to create the cache key from
     *
     * @return a cache key from the current row of the {@link ResultSet}
     *
     * @throws SQLException if there is an error creating the key
     */
    protected abstract K keyFromResultSet(ResultSet resultSet) throws SQLException;

    /**
     * Create a cache value from the current row of the {@link ResultSet}.
     *
     * @param resultSet  the {@link ResultSet} to create the cache value from
     *
     * @return a cache value from the current row of the {@link ResultSet}
     *
     * @throws SQLException if there is an error creating the value
     */
    protected abstract V valueFromResultSet(ResultSet resultSet) throws SQLException;

    // -----  ---------------------------------------------------------------
    // ----- data members ---------------------------------------------------

    /**
     * The database connection to use.
     */
    private final Connection connection;

    /**
     * The Coherence {@link Session} to use to obtain the cache to load.
     */
    private final Session session;

    /**
     * The number of entries to load in a single batch.
     */
    private final int batchSize;

    /**
     * The simple name of the loader used in log messages.
     */
    private final String name;

    /**
     * The binary decoration identifier to use.
     */
    private final int decorationId;
    }
