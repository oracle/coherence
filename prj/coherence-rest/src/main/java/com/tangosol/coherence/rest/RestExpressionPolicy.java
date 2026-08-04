/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.rest.config.ExpressionAliasConfig;
import com.tangosol.coherence.rest.util.PropertySet;

import com.tangosol.util.CoherenceMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central mode-aware policy for REST URL expression aliases.
 * <p>
 * Moves caller-supplied expressions to operator-configured aliases in DEV/PROD
 * while keeping raw URL expression compatibility only in LEGACY.
 *
 * @author Vaso Putica  2026.05.08
 * @since 26.07
 */
public final class RestExpressionPolicy
    {
    private RestExpressionPolicy()
        {
        }

    /**
     * Resolve a sort request.
     *
     * @param aliases  configured aliases
     * @param sSort    the URL sort request
     *
     * @return the configured sort expression
     */
    public static String resolveSort(ExpressionAliasConfig aliases, String sSort)
        {
        if (sSort == null)
            {
            return sSort;
            }
        if (sSort.isBlank())
            {
            if (isLegacyExpressionAllowed())
                {
                return sSort;
                }
            throw new IllegalArgumentException("bad sort alias syntax");
            }
        if (isLegacyExpressionAllowed())
            {
            logLegacy("sort");
            return sSort;
            }

        List<String> listResolved = new ArrayList<>();
        for (String sPart : sSort.split(",", -1))
            {
            SortAlias sortAlias = parseSortAlias(sPart);

            String sExpression = aliases(aliases).getSortExpression(sortAlias.getAlias());
            if (sExpression == null)
                {
                throw new IllegalArgumentException("unknown sort alias");
                }

            if (sortAlias.getDirection() == null)
                {
                listResolved.add(sExpression);
                }
            else
                {
                listResolved.add(sExpression + ':' + sortAlias.getDirection());
                }
            }
        return String.join(",", listResolved);
        }

    /**
     * Resolve a projection request.
     *
     * @param aliases       configured aliases
     * @param sProjection   the URL projection request
     *
     * @return the configured property set
     */
    public static PropertySet resolveProjection(ExpressionAliasConfig aliases, String sProjection)
        {
        if (sProjection == null || sProjection.isBlank())
            {
            return null;
            }
        if (isLegacyExpressionAllowed())
            {
            logLegacy("projection");
            return PropertySet.fromString(sProjection);
            }

        String sProperties = aliases(aliases).getProjectionProperties(sProjection.trim());
        if (sProperties == null)
            {
            throw new IllegalArgumentException("unknown projection alias");
            }
        return PropertySet.fromString(sProperties);
        }

    /**
     * Resolve an aggregator request.
     *
     * @param aliases   configured aliases
     * @param sRequest  the URL aggregator request
     *
     * @return the registry request
     */
    public static String resolveAggregator(ExpressionAliasConfig aliases, String sRequest)
        {
        if (isLegacyExpressionAllowed())
            {
            logLegacy("aggregator");
            return sRequest;
            }

        ParsedRequest request = parseRequest(sRequest, "aggregator");
        return request.withArguments(resolveArguments(request, aliases(aliases), true));
        }

    /**
     * Resolve a processor request.
     *
     * @param aliases   configured aliases
     * @param sRequest  the URL processor request
     *
     * @return the registry request
     */
    public static String resolveProcessor(ExpressionAliasConfig aliases, String sRequest)
        {
        if (isLegacyExpressionAllowed())
            {
            logLegacy("processor");
            return sRequest;
            }

        ParsedRequest request = parseRequest(sRequest, "processor");
        if (isBuiltInNumericProcessor(request.getName()) && request.getArguments().size() == 1)
            {
            return sRequest;
            }
        if (isBuiltInNumericProcessor(request.getName()) && request.getArguments().size() == 2)
            {
            String sExpression = aliases(aliases)
                    .getProcessorArgumentExpression(request.getName(), request.getArguments().get(0));
            if (sExpression == null)
                {
                throw new IllegalArgumentException("unknown processor argument alias");
                }
            List<String> listResolved = new ArrayList<>(2);
            listResolved.add(sExpression);
            listResolved.add(request.getArguments().get(1));
            return request.withArguments(listResolved);
            }
        return request.withArguments(resolveArguments(request, aliases(aliases), false));
        }

    /**
     * Return {@code true} if legacy URL expressions are allowed.
     *
     * @return {@code true} if legacy behavior is active
     */
    public static boolean isLegacyExpressionAllowed()
        {
        return CoherenceMode.isLegacy();
        }

    /**
     * Resolve request arguments.
     *
     * @param request      parsed request
     * @param aliases      aliases
     * @param fAggregator  {@code true} for aggregators
     *
     * @return resolved arguments
     */
    private static List<String> resolveArguments(ParsedRequest request,
            ExpressionAliasConfig aliases, boolean fAggregator)
        {
        List<String> listArgs     = request.getArguments();
        List<String> listResolved = new ArrayList<>(listArgs.size());
        for (String sArg : listArgs)
            {
            String sExpression = fAggregator
                    ? aliases.getAggregatorArgumentExpression(request.getName(), sArg)
                    : aliases.getProcessorArgumentExpression(request.getName(), sArg);
            if (sExpression == null)
                {
                throw new IllegalArgumentException("unknown "
                        + (fAggregator ? "aggregator" : "processor") + " argument alias");
                }
            listResolved.add(sExpression);
            }
        return listResolved;
        }

    /**
     * Return non-null aliases.
     *
     * @param aliases  configured aliases
     *
     * @return configured aliases
     */
    private static ExpressionAliasConfig aliases(ExpressionAliasConfig aliases)
        {
        return aliases == null ? ExpressionAliasConfig.EMPTY : aliases;
        }

    /**
     * Parse a DEV/PROD sort alias segment.
     *
     * @param sPart  the comma-delimited sort segment
     *
     * @return the parsed sort alias
     */
    private static SortAlias parseSortAlias(String sPart)
        {
        String sSegment = sPart.trim();
        if (sSegment.isEmpty())
            {
            throw new IllegalArgumentException("bad sort alias syntax");
            }

        int ofColon = sSegment.indexOf(':');
        if (ofColon != sSegment.lastIndexOf(':'))
            {
            throw new IllegalArgumentException("bad sort alias syntax");
            }
        if (ofColon < 0)
            {
            return new SortAlias(sSegment, null);
            }

        String sAlias     = sSegment.substring(0, ofColon).trim();
        String sDirection = sSegment.substring(ofColon + 1).trim();
        if (sAlias.isEmpty() || sDirection.isEmpty())
            {
            throw new IllegalArgumentException("bad sort alias syntax");
            }

        sDirection = sDirection.toLowerCase(Locale.ROOT);
        if (!"asc".equals(sDirection) && !"desc".equals(sDirection))
            {
            throw new IllegalArgumentException("bad sort alias direction");
            }
        return new SortAlias(sAlias, sDirection);
        }

    /**
     * Return {@code true} for built-in processors where a single argument is a
     * scalar value, not a manipulator expression.
     *
     * @param sName  the processor name
     *
     * @return {@code true} for built-in numeric processors
     */
    private static boolean isBuiltInNumericProcessor(String sName)
        {
        return "increment".equals(sName)
                || "multiply".equals(sName)
                || "post-increment".equals(sName)
                || "post-multiply".equals(sName);
        }

    /**
     * Parse a registry request.
     *
     * @param sRequest  the request
     * @param sFamily   the family name
     *
     * @return parsed request
     */
    private static ParsedRequest parseRequest(String sRequest, String sFamily)
        {
        Matcher matcher = REQUEST_PATTERN.matcher(sRequest);
        if (!matcher.matches())
            {
            throw new IllegalArgumentException("bad " + sFamily + " request syntax");
            }

        String       sName    = matcher.group(1);
        String       sArgs    = matcher.group(2);
        List<String> listArgs = new ArrayList<>();
        if (sArgs != null && !sArgs.isBlank())
            {
            for (String sArg : sArgs.split(","))
                {
                listArgs.add(sArg.trim());
                }
            }
        return new ParsedRequest(sName, listArgs);
        }

    /**
     * Log legacy URL expression usage.
     *
     * @param sFamily  the expression family
     */
    private static void logLegacy(String sFamily)
        {
        if (LEGACY_WARNINGS.add(sFamily))
            {
            Logger.warn("Using legacy Coherence REST " + sFamily
                    + " URL expression behavior. Configure REST expression aliases before enabling DEV or PROD mode.");
            }
        }


    /**
     * Parsed sort alias request.
     */
    private static class SortAlias
        {
        /**
         * Construct a parsed sort alias request.
         *
         * @param sAlias      the alias token
         * @param sDirection  the optional direction
         */
        private SortAlias(String sAlias, String sDirection)
            {
            m_sAlias     = sAlias;
            m_sDirection = sDirection;
            }

        /**
         * Return the alias token.
         *
         * @return the alias token
         */
        public String getAlias()
            {
            return m_sAlias;
            }

        /**
         * Return the optional direction.
         *
         * @return the optional direction
         */
        public String getDirection()
            {
            return m_sDirection;
            }

        private final String m_sAlias;

        private final String m_sDirection;
        }


    /**
     * Parsed aggregator or processor request.
     */
    private static class ParsedRequest
        {
        /**
         * Construct a parsed request.
         *
         * @param sName     the operation name
         * @param listArgs  the arguments
         */
        private ParsedRequest(String sName, List<String> listArgs)
            {
            m_sName    = sName;
            m_listArgs = listArgs;
            }

        /**
         * Return the operation name.
         *
         * @return the operation name
         */
        public String getName()
            {
            return m_sName;
            }

        /**
         * Return the arguments.
         *
         * @return the arguments
         */
        public List<String> getArguments()
            {
            return m_listArgs;
            }

        /**
         * Return a request with the supplied arguments.
         *
         * @param listArgs  the arguments
         *
         * @return the request
         */
        public String withArguments(List<String> listArgs)
            {
            return m_sName + '(' + String.join(",", listArgs) + ')';
            }

        private final String m_sName;

        private final List<String> m_listArgs;
        }


    /**
     * Regex pattern that defines a registry request.
     */
    private static final Pattern REQUEST_PATTERN = Pattern.compile("^\\s*(\\w(?:\\w|-)*)\\((.*)\\)");

    /**
     * LEGACY warnings already logged by family.
     */
    private static final Set<String> LEGACY_WARNINGS = ConcurrentHashMap.newKeySet();
    }
