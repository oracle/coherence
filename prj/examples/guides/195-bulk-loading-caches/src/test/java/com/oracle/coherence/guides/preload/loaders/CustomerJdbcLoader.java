/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.loaders;

import com.oracle.coherence.guides.preload.model.Customer;
import com.tangosol.net.Session;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerJdbcLoader
        extends AbstractJdbcPreloadTask<Integer, Customer>
    {
    public CustomerJdbcLoader(Connection connection, Session session, int batchSize)
        {
        super(connection, session, batchSize);
        }

    @Override
    protected String getMapName()
        {
        return MAP_NAME;
        }

    @Override
    protected String getSQL()
        {
        return LOAD_SQL;
        }

    @Override
    protected Integer keyFromResultSet(ResultSet resultSet) throws SQLException
        {
        return resultSet.getInt(1);
        }

    @Override
    protected Customer valueFromResultSet(ResultSet resultSet) throws SQLException
        {
        return new Customer(resultSet.getInt(1),
                            resultSet.getString(2),
                            resultSet.getString(3),
                            resultSet.getInt(4));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the Coherence {@link com.tangosol.net.NamedMap} or {@link com.tangosol.net.NamedCache} to load.
     */
    public static final String MAP_NAME = "customers";

    /**
     * The SQL statement to use to retrieve the data to load.
     */
    public static final String LOAD_SQL = "SELECT id, name, address, creditLimit FROM customers";
    }
