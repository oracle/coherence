/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configured Coherence REST expression aliases.
 * <p>
 * Makes these aliases the operator-owned contract for hardened URL sort,
 * projection, aggregator, and processor expression inputs.
 *
 * @author Vaso Putica  2026.05.08
 * @since 26.07
 */
public class ExpressionAliasConfig
    {

    /**
     * Construct expression aliases.
     *
     * @param mapSort        sort aliases
     * @param mapProjection  projection aliases
     * @param mapAggregator  aggregator argument aliases
     * @param mapProcessor   processor argument aliases
     */
    private ExpressionAliasConfig(Map<String, String> mapSort,
            Map<String, String> mapProjection,
            Map<String, Map<String, String>> mapAggregator,
            Map<String, Map<String, String>> mapProcessor)
        {
        m_mapSort       = immutable(mapSort);
        m_mapProjection = immutable(mapProjection);
        m_mapAggregator = immutableNested(mapAggregator);
        m_mapProcessor  = immutableNested(mapProcessor);
        }


    /**
     * Return the configured expression for a sort alias.
     *
     * @param sAlias  the alias
     *
     * @return the configured expression, or {@code null}
     */
    public String getSortExpression(String sAlias)
        {
        return m_mapSort.get(sAlias);
        }

    /**
     * Return the configured property set for a projection alias.
     *
     * @param sAlias  the alias
     *
     * @return the configured property set, or {@code null}
     */
    public String getProjectionProperties(String sAlias)
        {
        return m_mapProjection.get(sAlias);
        }

    /**
     * Return the configured expression for an aggregator argument alias.
     *
     * @param sAggregator  the aggregator name
     * @param sAlias       the alias
     *
     * @return the configured expression, or {@code null}
     */
    public String getAggregatorArgumentExpression(String sAggregator, String sAlias)
        {
        return getNested(m_mapAggregator, sAggregator, sAlias);
        }

    /**
     * Return the configured expression for a processor argument alias.
     *
     * @param sProcessor  the processor name
     * @param sAlias      the alias
     *
     * @return the configured expression, or {@code null}
     */
    public String getProcessorArgumentExpression(String sProcessor, String sAlias)
        {
        return getNested(m_mapProcessor, sProcessor, sAlias);
        }

    /**
     * Return {@code true} if there are no aliases.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty()
        {
        return m_mapSort.isEmpty()
                && m_mapProjection.isEmpty()
                && m_mapAggregator.isEmpty()
                && m_mapProcessor.isEmpty();
        }


    /**
     * Merge global and resource-scoped aliases.
     *
     * @param global    global aliases
     * @param resource  resource aliases
     *
     * @return merged aliases with resource aliases taking precedence
     */
    public static ExpressionAliasConfig merge(ExpressionAliasConfig global, ExpressionAliasConfig resource)
        {
        if (global == null || global.isEmpty())
            {
            return resource == null ? EMPTY : resource;
            }
        if (resource == null || resource.isEmpty())
            {
            return global;
            }

        Builder builder = new Builder();
        builder.m_mapSort.putAll(global.m_mapSort);
        builder.m_mapProjection.putAll(global.m_mapProjection);
        copyNested(global.m_mapAggregator, builder.m_mapAggregator);
        copyNested(global.m_mapProcessor, builder.m_mapProcessor);
        builder.m_mapSort.putAll(resource.m_mapSort);
        builder.m_mapProjection.putAll(resource.m_mapProjection);
        copyNested(resource.m_mapAggregator, builder.m_mapAggregator);
        copyNested(resource.m_mapProcessor, builder.m_mapProcessor);
        return builder.build();
        }

    /**
     * Return a new alias builder.
     *
     * @return a builder
     */
    public static Builder builder()
        {
        return new Builder();
        }

    /**
     * Return a nested alias value.
     *
     * @param map      the nested map
     * @param sFamily  the family key
     * @param sAlias   the alias key
     *
     * @return the configured value, or {@code null}
     */
    private static String getNested(Map<String, Map<String, String>> map, String sFamily, String sAlias)
        {
        Map<String, String> mapAliases = map.get(sFamily);
        return mapAliases == null ? null : mapAliases.get(sAlias);
        }

    /**
     * Return an immutable copy of a map.
     *
     * @param map  the source map
     *
     * @return an immutable map
     */
    private static Map<String, String> immutable(Map<String, String> map)
        {
        return map.isEmpty()
               ? Collections.emptyMap()
               : Collections.unmodifiableMap(new HashMap<>(map));
        }

    /**
     * Return an immutable copy of a nested map.
     *
     * @param map  the source map
     *
     * @return an immutable nested map
     */
    private static Map<String, Map<String, String>> immutableNested(Map<String, Map<String, String>> map)
        {
        if (map.isEmpty())
            {
            return Collections.emptyMap();
            }

        Map<String, Map<String, String>> mapCopy = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : map.entrySet())
            {
            mapCopy.put(entry.getKey(), immutable(entry.getValue()));
            }
        return Collections.unmodifiableMap(mapCopy);
        }

    /**
     * Copy nested aliases.
     *
     * @param mapSource  the source map
     * @param mapTarget  the target map
     */
    private static void copyNested(Map<String, Map<String, String>> mapSource,
            Map<String, Map<String, String>> mapTarget)
        {
        for (Map.Entry<String, Map<String, String>> entry : mapSource.entrySet())
            {
            mapTarget.computeIfAbsent(entry.getKey(), k -> new HashMap<>())
                    .putAll(entry.getValue());
            }
        }


    /**
     * Expression alias builder.
     */
    public static class Builder
        {
        /**
         * Add a sort alias.
         *
         * @param sName        the alias
         * @param sExpression  the configured expression
         *
         * @return this builder
         */
        public Builder addSortAlias(String sName, String sExpression)
            {
            putUnique(m_mapSort, sName, sExpression, "sort");
            return this;
            }

        /**
         * Add a projection alias.
         *
         * @param sName        the alias
         * @param sProperties  the configured property set
         *
         * @return this builder
         */
        public Builder addProjectionAlias(String sName, String sProperties)
            {
            putUnique(m_mapProjection, sName, sProperties, "projection");
            return this;
            }

        /**
         * Add an aggregator argument alias.
         *
         * @param sAggregator  the aggregator name
         * @param sName        the alias
         * @param sExpression  the configured expression
         *
         * @return this builder
         */
        public Builder addAggregatorArgumentAlias(String sAggregator, String sName, String sExpression)
            {
            putNestedUnique(m_mapAggregator, sAggregator, sName, sExpression, "aggregator argument");
            return this;
            }

        /**
         * Add a processor argument alias.
         *
         * @param sProcessor   the processor name
         * @param sName        the alias
         * @param sExpression  the configured expression
         *
         * @return this builder
         */
        public Builder addProcessorArgumentAlias(String sProcessor, String sName, String sExpression)
            {
            putNestedUnique(m_mapProcessor, sProcessor, sName, sExpression, "processor argument");
            return this;
            }

        /**
         * Build the immutable configuration.
         *
         * @return expression aliases
         */
        public ExpressionAliasConfig build()
            {
            return new ExpressionAliasConfig(m_mapSort, m_mapProjection, m_mapAggregator, m_mapProcessor);
            }

        /**
         * Put a value, rejecting duplicates.
         *
         * @param map      the map
         * @param sName    the alias name
         * @param sValue   the value
         * @param sFamily  the alias family
         */
        private static void putUnique(Map<String, String> map, String sName, String sValue, String sFamily)
            {
            if (map.containsKey(sName))
                {
                throw new IllegalArgumentException("duplicate " + sFamily + " alias: " + sName);
                }
            map.put(sName, sValue);
            }

        /**
         * Put a nested value, rejecting duplicates.
         *
         * @param map         the nested map
         * @param sOperation  the operation name
         * @param sName       the alias name
         * @param sValue      the value
         * @param sFamily     the alias family
         */
        private static void putNestedUnique(Map<String, Map<String, String>> map, String sOperation,
                String sName, String sValue, String sFamily)
            {
            Map<String, String> mapAliases = map.computeIfAbsent(sOperation, k -> new HashMap<>());
            if (mapAliases.containsKey(sName))
                {
                throw new IllegalArgumentException("duplicate " + sFamily + " alias: "
                        + sOperation + '/' + sName);
                }
            mapAliases.put(sName, sValue);
            }

        private final Map<String, String> m_mapSort = new HashMap<>();

        private final Map<String, String> m_mapProjection = new HashMap<>();

        private final Map<String, Map<String, String>> m_mapAggregator = new HashMap<>();

        private final Map<String, Map<String, String>> m_mapProcessor = new HashMap<>();
        }


    /**
     * Empty aliases.
     */
    public static final ExpressionAliasConfig EMPTY = builder().build();


    private final Map<String, String> m_mapSort;

    private final Map<String, String> m_mapProjection;

    private final Map<String, Map<String, String>> m_mapAggregator;

    private final Map<String, Map<String, String>> m_mapProcessor;
    }
