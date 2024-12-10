/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

import com.tangosol.config.expression.Value;

import com.tangosol.util.Base;
import com.tangosol.util.UID;
import com.tangosol.util.UUID;


import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for QueryEngine implementations.
 *
 * @author as  2012.01.19
 */
public abstract class AbstractQueryEngine
        implements QueryEngine
    {

    // ----- helper methods -------------------------------------------------

    /**
     * Parse a query string containing parameter type hints.
     * <p>
     * Parameter type hints allow users to specify the type conversion that
     * should be performed on the string value of the parameter defined in
     * the query string portion of the URL before it is bound to the query.
     *
     * @param sQuery  a query string to parse, which contains zero or more
     *                parameter bindings, possibly with type hints
     *
     * @return a ParsedQuery instance containing both the original query
     *         string with parameter type hints removed, and the map of
     *         parameter types keyed by parameter name
     */
    protected ParsedQuery parseQueryString(String sQuery)
        {
        Matcher                  matcher = QUERY_PARAMS_PATTERN.matcher(sQuery);
        String                   sFinal  = matcher.replaceAll(":$1");
        Map<String, ParsedQuery> map     = m_mapParsedQuery;

        // check to see if we've already created a ParsedQuery for the
        // given final query string
        ParsedQuery query = map.get(sFinal);
        if (query == null)
            {
            matcher.reset();

            Map<String, Class> mapParamTypes = new HashMap<String, Class>();
            while (matcher.find())
                {
                String sName   = matcher.group(1);
                String sType   = matcher.group(3);
                Class  clzType = resolveParameterType(sType);

                mapParamTypes.put(sName, clzType);
                }

            query = new ParsedQuery(sFinal, mapParamTypes);
            map.put(sFinal, query);
            }

        return query;
        }

    /**
     * Return the Class that corresponds with the given type hint.
     *
     * @param sType  the type hint
     *
     * @return the Class that corresponds with the given type hint
     */
    protected Class resolveParameterType(String sType)
        {
        if (sType == null)
            {
            return String.class;
            }
        Class clz = s_mapTypeNames.get(sType);
        if (clz == null)
            {
            try
                {
                clz = Base.getContextClassLoader(this).loadClass(sType);
                }
            catch (ClassNotFoundException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        return clz;
        }

    /**
     * Converts string parameters to their required types.
     *
     * @param mapParams      a map of string parameter values
     * @param mapParamTypes  a map of required parameter types
     *
     * @return a map of parameters converted to a required type
     */
    protected Map<String, Object> createBindings(Map<String, Object> mapParams,
            Map<String, Class> mapParamTypes)
        {
        Map<String, Object> mapBindings = new HashMap<String, Object>(mapParamTypes.size());
        for (Map.Entry<String, Class> entry : mapParamTypes.entrySet())
            {
            String sParamName   = entry.getKey();
            Class  clzParamType = entry.getValue();

            Object oValue = mapParams.get(sParamName);
            oValue = oValue instanceof List
                     ? convertList((List<String>) oValue, clzParamType)
                     : new Value((String) oValue).as(clzParamType);

            mapBindings.put(sParamName, oValue);
            }
        return mapBindings;
        }

    /**
     * Convert all elements of the source list into the specified type.
     *
     * @param listSource      source list
     * @param clzElementType  element type
     *
     * @return converted list
     */
    protected List convertList(List<String> listSource, Class clzElementType)
        {
        List listConverted = new ArrayList(listSource.size());
        for (String sValue : listSource)
            {
            listConverted.add(new Value(sValue).as(clzElementType));
            }
        return listConverted;
        }

    // ----- inner class: ParsedQuery ---------------------------------------

    /**
     * Immutable helper class that wraps both final query string and a map
     * of parameter types, keyed by parameter name.
     */
    protected static class ParsedQuery
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct ParsedQuery instance.
         *
         * @param sQuery             query string without parameter names
         * @param mapParameterTypes  a map of parameter types keyed by parameter
         *                           name
         */
        public ParsedQuery(String sQuery, Map<String, Class> mapParameterTypes)
            {
            m_sQuery            = sQuery;
            m_mapParameterTypes = Collections.unmodifiableMap(mapParameterTypes);
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the query string with parameter types removed.
         *
         * @return query string without parameter types
         */
        public String getQuery()
            {
            return m_sQuery;
            }

        /**
         * Return a map of parameter types keyed by parameter name.
         *
         * @return a map of parameter types keyed by parameter name
         */
        public Map<String, Class> getParameterTypes()
            {
            return m_mapParameterTypes;
            }

        // ----- data members -----------------------------------------------

        private final String             m_sQuery;
        private final Map<String, Class> m_mapParameterTypes;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Map of cached ParsedQuery instances, keyed by their corresponding
     * final query string.
     */
    protected final Map m_mapParsedQuery = new ConcurrentHashMap<String, ParsedQuery>();

    // ----- constants ------------------------------------------------------

    /**
     * Capture query template parameter names.
     */
    private static final Pattern QUERY_PARAMS_PATTERN =
            Pattern.compile(":([a-zA-Z][a-zA-Z_0-9]*)(;([a-zA-Z][a-zA-Z_0-9\\.\\$]*))?");

    /**
     * Map of built-in type names to corresponding classes.
     */
    private static final Map<String, Class> s_mapTypeNames;

    /**
     * Class initializer.
     */
    static
        {
        Map<String, Class> mapTypeNames = new HashMap<>();
        mapTypeNames.put("i", Integer.class);
        mapTypeNames.put("int", Integer.class);
        mapTypeNames.put("s", Short.class);
        mapTypeNames.put("short", Short.class);
        mapTypeNames.put("l", Long.class);
        mapTypeNames.put("long", Long.class);
        mapTypeNames.put("f", Float.class);
        mapTypeNames.put("float", Float.class);
        mapTypeNames.put("d", Double.class);
        mapTypeNames.put("double", Double.class);
        mapTypeNames.put("I", BigInteger.class);
        mapTypeNames.put("BigInteger", BigInteger.class);
        mapTypeNames.put("D", BigDecimal.class);
        mapTypeNames.put("BigDecimal", BigDecimal.class);
        mapTypeNames.put("date", Date.class);
        mapTypeNames.put("uuid", UUID.class);
        mapTypeNames.put("uid", UID.class);

        s_mapTypeNames = mapTypeNames;
        }
    }
