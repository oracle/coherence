/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.guides.preload.cachestore.CustomerCacheStore;
import com.oracle.coherence.guides.preload.cachestore.OrderCacheStore;
import com.oracle.coherence.guides.preload.db.HsqldbRunner;
import com.oracle.coherence.guides.preload.model.Customer;
import com.oracle.coherence.guides.preload.model.Order;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public abstract class AbstractPreloadIT
    {
    @BeforeAll
    public static void setupDatabase() throws Exception
        {
        connection = DriverManager.getConnection(hsqldbRunner.getJdbcURL());
        createTables(connection);
        }

    protected Map<Integer, Customer> createTestCustomers()
        {
        Map<Integer, Customer> customers = new HashMap<>();
        for (int i = 0; i < 123; i++)
            {
            String name = UUID.randomUUID().toString();
            String address = UUID.randomUUID().toString();
            int creditLimit = i * 100;
            customers.put(i, new Customer(i, name, address, creditLimit));
            }
        return customers;
        }

    protected Map<Integer, Customer> loadCustomersToDB()
        {
        Map<Integer, Customer> customers = createTestCustomers();
        CustomerCacheStore store = new CustomerCacheStore(connection);
        store.storeAll(customers);
        return customers;
        }

    protected Map<Integer, Order> createTestOrders()
        {
        Map<Integer, Order> orders = new HashMap<>();
        for (int i = 0; i < 123; i++)
            {
            int customerId = random.nextInt(50);
            int itemId = random.nextInt(10);
            int qty = 1 + random.nextInt(10);
            BigDecimal price = new BigDecimal("10.99");

            orders.put(i, new Order(i, customerId, itemId, price, qty));
            }
        return orders;
        }

    protected Map<Integer, Order> loadOrdersToDB()
        {
        Map<Integer, Order> orders = createTestOrders();
        OrderCacheStore store = new OrderCacheStore(connection);
        store.storeAll(orders);
        return orders;
        }

    protected static void createTables(Connection connection) throws SQLException
        {
        try (Statement statement = connection.createStatement())
            {
            DatabaseMetaData metaData = connection.getMetaData();

            Map<String, String> tables = new HashMap<>();

            tables.put("CUSTOMERS", "CREATE TABLE CUSTOMERS " +
                    "(id int primary key, " +
                    "name varchar(128), " +
                    "address varchar(265), " +
                    "creditLimit int, " +
                    "updatedAt timestamp default now() on update current_timestamp not null)");

            tables.put("ORDERS", "CREATE TABLE ORDERS " +
                    "(id int primary key, customerId int, itemId int, itemPrice decimal(10,2), quantity int)");

            for (Map.Entry<String, String> entry : tables.entrySet())
                {
                String table = entry.getKey();
                try (ResultSet resultSet = metaData.getTables(null, null, table, null))
                    {
                    // check if table exists and if so, drop it
                    if (resultSet.next())
                        {
                        statement.execute("DROP TABLE " + table);
                        }
                    }

                // create the table
                statement.execute(entry.getValue());
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * A Bedrock JUnit 5 extension to create {@link com.oracle.bedrock.runtime.ApplicationConsole}
     * instances to capture output to the build directory.
     */
    @RegisterExtension
    static final TestLogsExtension testLogs = new TestLogsExtension(AbstractPreloadIT.class);

    /**
     * A JUnit 5 extension to start the HSQLDB for testing.
     */
    @RegisterExtension
    protected static final HsqldbRunner hsqldbRunner = new HsqldbRunner();

    /**
     * The DB {@link Connection} to the HSQLDb Database.
     */
    protected static Connection connection;

    protected static final Random random = new Random(System.currentTimeMillis());
    }
