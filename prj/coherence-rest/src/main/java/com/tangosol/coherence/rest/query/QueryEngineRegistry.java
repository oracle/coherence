/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

import com.tangosol.coherence.rest.config.QueryEngineConfig;

import com.tangosol.util.Base;
import com.tangosol.util.SafeHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A registry for {@link QueryEngine} instances.
 *
 * @author ic  2011.12.03
 */
public class QueryEngineRegistry
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Construct a <tt>QueryEngineRegistry</tt> instance.
     * <p>
     * Invoking this constructor will register a <tt>CoherenceQueryLanguageEngine</tt>
     * as the default query engine.</p>
     *
     * @see CoherenceQueryLanguageEngine
     */
    public QueryEngineRegistry()
        {
        this(Collections.emptyList());
        }

     /**
     * Construct a <tt>QueryEngineRegistry</tt> instance.
     * <p>
     * Invoking this constructor will register a <tt>CoherenceQueryLanguageEngine</tt>
     * as the default query engine. Any of provided configurations with name "DEFAULT"
     * will override this one as default query engine.</p>
     *
     * @param  colConfig  query engine configurations to be registered
     */
    public QueryEngineRegistry(Collection<? extends QueryEngineConfig> colConfig)
        {
        // register CohQL as default query language
        registerQueryEngine(DEFAULT, new CoherenceQueryLanguageEngine());

        for (QueryEngineConfig config : colConfig)
            {
            registerQueryEngine(config.getQueryEngineName(),
                    createQueryEngine(config.getQueryEngineClass()));
            }
        }


    // ---- public API ------------------------------------------------------

    /**
     * Register a query engine for the specified name.
     *
     * @param sName           query engine name
     * @param clzQueryEngine  query engine class
     */
    public void registerQueryEngine(String sName, Class clzQueryEngine)
        {
        registerQueryEngine(sName, createQueryEngine(clzQueryEngine));
        }

    /**
     * Register a query engine for the specified name.
     *
     * @param sName        query engine name
     * @param queryEngine  query engine
     */
    public void registerQueryEngine(String sName, QueryEngine queryEngine)
        {
        m_mapQueryEngines.put(sName, queryEngine);
        }

    /**
     * Return a query engine for the specified name.
     * <p>
     * If none found, the default query engine will be returned.</p>
     *
     * @param sName  name of the query engine
     *
     * @return query engine for the specified name or default query engine
     */
    public QueryEngine getQueryEngine(String sName)
        {
        QueryEngine engine = m_mapQueryEngines.get(sName);
        if (engine == null)
            {
            engine = getDefaultQueryEngine();
            }
        return engine;
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Return a default query engine.
     *
     * @return default query engine
     */
    private QueryEngine getDefaultQueryEngine()
        {
        return m_mapQueryEngines.get(DEFAULT);
        }

    /**
     * Create a query engine instance.
     *
     * @param clzQueryEngine  query engine class
     *
     * @return query engine instance
     */
    protected static QueryEngine createQueryEngine(Class clzQueryEngine)
        {
        if (clzQueryEngine == null ||
                !QueryEngine.class.isAssignableFrom(clzQueryEngine))
            {
            throw new IllegalArgumentException("invalid query engine class: "
                    + clzQueryEngine);
            }

        try
            {
            return (QueryEngine) clzQueryEngine.newInstance();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "error creating query engine");
            }
        }

    // ---- constants -------------------------------------------------------

    /**
     * The name of the default query engine. Query engines registered with this
     * name will be used as fail safe implementations.
     */
    private static final String DEFAULT = "DEFAULT";

    // ---- data members ----------------------------------------------------

    /**
     * A map of registered <tt>QueryEngine</tt>s, keyed by engine names.
     */
    protected Map<String, QueryEngine> m_mapQueryEngines = new SafeHashMap<>();
    }
