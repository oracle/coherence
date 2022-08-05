/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.db;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.common.base.Exceptions;
import org.hsqldb.server.Server;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.hamcrest.CoreMatchers.is;

/**
 * A JUnit 5 extension to start and stop a HSQLDB instance.
 */
public class HsqldbRunner
        implements BeforeAllCallback, AfterAllCallback
    {
    /**
     * Create a {@link HsqldbRunner}.
     */
    public HsqldbRunner()
        {
        this(DB_NAME, createDefaultPath());
        }

    /**
     * Create a {@link HsqldbRunner}.
     *
     * @param dbName  the name of the test database
     * @param dbPath  the path to the directory to contain the database files
     */
    public HsqldbRunner(String dbName, Path dbPath)
        {
        this.dbName = dbName;
        this.dbPath = dbPath;
        }

    /**
     * Returns the name of the database.
     *
     * @return the name of the database
     */
    public String getDBName()
        {
        return dbName;
        }

    /**
     * Returns the JDBC URL to use to connect to the database.
     *
     * @return the JDBC URL to use to connect to the database
     */
    public String getJdbcURL()
        {
        return DB_URL_PREFIX + dbName;
        }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception
        {
        Class<?> testClass = context.getTestClass().orElse(getClass());

        TestLogsExtension testLogs = new TestLogsExtension(testClass);
        testLogs.beforeAll(context);

        String dbFile = dbPath.resolve(dbName).toUri().toASCIIString();
        System.out.println();

        LocalPlatform platform = LocalPlatform.get();

        dbApp = platform.launch(JavaApplication.class,
                                ClassName.of(Server.class),
                                testLogs,
                                DisplayName.of("hsqldb"),
                                Argument.of("--database.0", dbFile),
                                Argument.of("--dbname.0", dbName));

        // wait for the DB to start by ensuring we can connect
        Eventually.assertDeferred(this::canConnect, is(true));
        }

    @Override
    public void afterAll(ExtensionContext context)
        {
        dbApp.close();
        }

    /**
     * Returns {@code true} if it is possible to connect to the database.
     *
     * @return {@code true} if it is possible to connect to the database
     */
    private boolean canConnect()
        {
        try
            {
            try (Connection ignored = DriverManager.getConnection(getJdbcURL()))
                {
                return true;
                }
            }
        catch (Throwable ignored)
            {
            }
        return false;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create the default path for the DB files.
     *
     * @return  the default path for the DB files
     */
    private static Path createDefaultPath()
        {
        try
            {
            return Files.createTempDirectory("DBTest");
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the HSQLDB test database instance.
     */
    public static final String DB_NAME = "testdb";

    /**
     * The URL prefix to use to connect to the HSQLDB test database instance.
     */
    private static final String DB_URL_PREFIX = "jdbc:hsqldb:hsql://localhost/";

    // ----- data members ---------------------------------------------------

    /**
     * The database name;
     */
    private final String dbName;

    /**
     * The path to the directory containing the DB files.
     */
    private final Path dbPath;

    private JavaApplication dbApp;
    }
