/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.dslquery.FilterBuilder;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.coherence.rest.config.QueryConfig;

import com.tangosol.util.CoherenceMode;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterBuildingException;
import com.tangosol.util.QueryHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

import jakarta.ws.rs.core.Response;

/**
 * Central mode-aware policy for REST direct query input.
 * <p>
 * Keeps HTTP direct query behind one gate and one strict parse/validate/build
 * path so SSE and non-SSE resources cannot diverge.
 *
 * @author Vaso Putica  2026.05.08
 * @since 26.07
 */
public final class RestQueryPolicy
    {
    private RestQueryPolicy()
        {
        }

    /**
     * Validate a direct-query request and return a REST rejection response.
     *
     * @param queryConfig  the query config
     * @param sQuery       the HTTP query text
     *
     * @return a rejection response, or {@code null}
     */
    public static Response checkDirectQuery(QueryConfig queryConfig, String sQuery)
        {
        if (!hasDirectQuery(sQuery))
            {
            return null;
            }
        if (CoherenceMode.isLegacy())
            {
            logLegacy();
            }
        if (queryConfig == null || !queryConfig.isDirectQueryEnabled())
            {
            return directQueryForbiddenResponse();
            }

        try
            {
            validateDirectQuery(sQuery);
            }
        catch (IllegalArgumentException e)
            {
            return directQueryBadRequestResponse();
            }
        return null;
        }

    /**
     * Assert that a direct-query request is allowed.
     *
     * @param queryConfig  the query config
     * @param sQuery       the HTTP query text
     */
    public static void assertDirectQueryAllowed(QueryConfig queryConfig, String sQuery)
        {
        if (!hasDirectQuery(sQuery))
            {
            return;
            }
        if (CoherenceMode.isLegacy())
            {
            logLegacy();
            return;
            }
        if (queryConfig == null || !queryConfig.isDirectQueryEnabled())
            {
            throw new ForbiddenException(directQueryForbiddenResponse());
            }
        try
            {
            validateDirectQuery(sQuery);
            }
        catch (IllegalArgumentException e)
            {
            throw new BadRequestException(directQueryBadRequestResponse(), e);
            }
        }

    /**
     * Validate HTTP direct query text.
     *
     * @param sQuery  the query text
     */
    public static void validateDirectQuery(String sQuery)
        {
        if (!isDirectQuerySubsetRequired() || !hasDirectQuery(sQuery))
            {
            return;
            }

        parseAndValidateDirectQuery(sQuery, LANGUAGE);
        }

    /**
     * Create a filter for HTTP direct query text.
     *
     * @param sQuery  the query text
     *
     * @return the constructed filter
     */
    public static Filter createDirectQueryFilter(String sQuery)
        {
        return createDirectQueryFilter(sQuery, null, null, LANGUAGE);
        }

    /**
     * Create a filter for HTTP direct query text.
     *
     * @param sQuery       the query text
     * @param aBindings    the indexed bindings
     * @param mapBindings  the named bindings
     * @param language     the query language
     *
     * @return the constructed filter
     */
    public static Filter createDirectQueryFilter(String sQuery, Object[] aBindings,
            Map mapBindings, CoherenceQueryLanguage language)
        {
        CoherenceQueryLanguage queryLanguage = language == null ? LANGUAGE : language;
        if (!isDirectQuerySubsetRequired())
            {
            return QueryHelper.createFilter(sQuery, aBindings, mapBindings == null ? Collections.emptyMap() : mapBindings,
                    queryLanguage);
            }

        try
            {
            Term term = parseAndValidateDirectQuery(sQuery, queryLanguage);
            return buildDirectQueryFilter(term, aBindings, mapBindings, queryLanguage);
            }
        catch (FilterBuildingException e)
            {
            throw e;
            }
        catch (RuntimeException e)
            {
            throw new FilterBuildingException(e.getMessage(), sQuery, e);
            }
        }

    /**
     * Create a filter for an operator-configured named query.
     *
     * @param sExpression  the named-query expression
     * @param mapParams    the named-query parameters
     *
     * @return the constructed filter
     */
    public static Filter createNamedQueryFilter(String sExpression, Map<String, Object> mapParams)
        {
        return QueryHelper.createFilter(sExpression, mapParams);
        }

    /**
     * Execute a direct-query operation with REST-facing type-hint policy.
     *
     * @param supplier  operation to execute
     * @param <T>       result type
     *
     * @return the operation result
     */
    public static <T> T withDirectQueryTypePolicy(Supplier<T> supplier)
        {
        Boolean previous = DIRECT_QUERY_TYPE_POLICY.get();
        DIRECT_QUERY_TYPE_POLICY.set(Boolean.TRUE);
        try
            {
            return supplier.get();
            }
        finally
            {
            if (previous == null)
                {
                DIRECT_QUERY_TYPE_POLICY.remove();
                }
            else
                {
                DIRECT_QUERY_TYPE_POLICY.set(previous);
                }
            }
        }

    /**
     * Return {@code true} when REST-facing type hints should be constrained.
     *
     * @return {@code true} if direct-query type-hint policy is active
     */
    public static boolean isDirectQueryTypePolicyActive()
        {
        return Boolean.TRUE.equals(DIRECT_QUERY_TYPE_POLICY.get());
        }

    /**
     * Return {@code true} if the supplied text has a direct query.
     *
     * @param sQuery  the query text
     *
     * @return {@code true} if a direct query is present
     */
    public static boolean hasDirectQuery(String sQuery)
        {
        return sQuery != null && !sQuery.isBlank();
        }

    /**
     * Return {@code true} if the REST-safe subset is required.
     *
     * @return {@code true} if the subset is required
     */
    public static boolean isDirectQuerySubsetRequired()
        {
        return !CoherenceMode.isLegacy();
        }

    /**
     * Build a filter from an already parsed REST direct-query term.
     *
     * @param term         the query term
     * @param aBindings    the indexed bindings
     * @param mapBindings  the named bindings
     * @param language     the query language
     *
     * @return the constructed filter
     */
    static Filter buildDirectQueryFilter(Term term, Object[] aBindings,
            Map mapBindings, CoherenceQueryLanguage language)
        {
        CoherenceQueryLanguage queryLanguage = language == null ? LANGUAGE : language;
        List                   listBindings  = aBindings == null
                ? Collections.emptyList()
                : Arrays.asList(aBindings);

        return new RestFilterBuilder(listBindings,
                new ResolvableParameterList(mapBindings == null ? Collections.emptyMap() : mapBindings),
                queryLanguage)
                .makeFilter(term);
        }

    /**
     * Parse and validate a direct query.
     *
     * @param sQuery    the query text
     * @param language  the query language
     *
     * @return the parsed term
     */
    private static Term parseAndValidateDirectQuery(String sQuery, CoherenceQueryLanguage language)
        {
        String   sSafeQuery = stripAndValidateTypeHints(sQuery);
        OPParser parser     = new OPParser(sSafeQuery, language.filtersTokenTable(), language.getOperators());
        Term     term       = parser.parse();

        validateTerm(term);
        return term;
        }

    /**
     * Validate an AST term.
     *
     * @param term  the term
     */
    private static void validateTerm(Term term)
        {
        if (term == null)
            {
            throw new IllegalArgumentException("null query term");
            }

        if (term.isAtom())
            {
            validateAtom((AtomicTerm) term);
            return;
            }

        String sFunctor = term.getFunctor();
        switch (sFunctor)
            {
            case "binaryOperatorNode":
                validateBinaryOperator(term);
                break;

            case "unaryOperatorNode":
                validateUnaryOperator(term);
                break;

            case "identifier":
                validateIdentifier(term);
                break;

            case "literal":
            case "listNode":
                validateChildren(term);
                break;

            case "bindingNode":
                validateBinding(term);
                break;

            case "callNode":
                validateCall(term);
                break;

            default:
                throw new IllegalArgumentException("unsupported REST query term: " + sFunctor);
            }
        }

    /**
     * Validate an atomic term.
     *
     * @param term  the atomic term
     */
    private static void validateAtom(AtomicTerm term)
        {
        String sFunctor = term.getFunctor();
        if (!ALLOWED_ATOMS.contains(sFunctor))
            {
            throw new IllegalArgumentException("unsupported REST query atom: " + sFunctor);
            }
        }

    /**
     * Validate a binary operator term.
     *
     * @param term  the term
     */
    private static void validateBinaryOperator(Term term)
        {
        String sOperator = atomicValue(term.termAt(1));
        if (!ALLOWED_OPERATORS.contains(sOperator))
            {
            throw new IllegalArgumentException("unsupported REST query operator: " + sOperator);
            }
        validateTerm(term.termAt(2));
        validateTerm(term.termAt(3));
        }

    /**
     * Validate a unary operator term.
     *
     * @param term  the term
     */
    private static void validateUnaryOperator(Term term)
        {
        String sOperator = atomicValue(term.termAt(1));
        if ("!".equals(sOperator))
            {
            validateTerm(term.termAt(2));
            return;
            }
        if ("+".equals(sOperator) || "-".equals(sOperator))
            {
            Term termValue = term.termAt(2);
            if (isNumericLiteral(termValue))
                {
                return;
                }
            }
        throw new IllegalArgumentException("unsupported REST query unary operator: " + sOperator);
        }

    /**
     * Validate an identifier term.
     *
     * @param term  the term
     */
    private static void validateIdentifier(Term term)
        {
        String sIdentifier = atomicValue(term.termAt(1));
        if (sIdentifier == null || sIdentifier.isBlank() || sIdentifier.indexOf('.') >= 0)
            {
            throw new IllegalArgumentException("unsupported REST query identifier");
            }
        }

    /**
     * Validate a binding term.
     *
     * @param term  the term
     */
    private static void validateBinding(Term term)
        {
        String sBinding = atomicValue(term.termAt(1));
        if (!":".equals(sBinding) && !"?".equals(sBinding))
            {
            throw new IllegalArgumentException("unsupported REST query binding");
            }
        validateTerm(term.termAt(2));
        }

    /**
     * Validate a call term.
     *
     * @param term  the term
     */
    private static void validateCall(Term term)
        {
        Term termCall = term.termAt(1);
        String sName = termCall.getFunctor();
        if (!("key".equals(sName) || "value".equals(sName)) || termCall.length() != 0)
            {
            throw new IllegalArgumentException("unsupported REST query call");
            }
        }

    /**
     * Validate all child terms.
     *
     * @param term  the term
     */
    private static void validateChildren(Term term)
        {
        for (Term child : term.children())
            {
            validateTerm(child);
            }
        }

    /**
     * Return an atomic value.
     *
     * @param term  the term
     *
     * @return the atomic value
     */
    private static String atomicValue(Term term)
        {
        if (!(term instanceof AtomicTerm))
            {
            throw new IllegalArgumentException("expected atomic term");
            }
        return ((AtomicTerm) term).getValue();
        }

    /**
     * Return {@code true} for numeric literal term shapes.
     *
     * @param term  the term
     *
     * @return {@code true} for numeric literals
     */
    private static boolean isNumericLiteral(Term term)
        {
        if (term instanceof AtomicTerm)
            {
            return term.isNumber();
            }
        if (!"literal".equals(term.getFunctor()) || term.length() != 1)
            {
            return false;
            }
        Term termValue = term.termAt(1);
        return termValue instanceof AtomicTerm && termValue.isNumber();
        }

    /**
     * Strip built-in type hints before AST validation and reject FQN hints.
     *
     * @param sQuery  the query
     *
     * @return query with type hints removed
     */
    private static String stripAndValidateTypeHints(String sQuery)
        {
        if (MALFORMED_QUERY_PARAMS_PATTERN.matcher(sQuery).find())
            {
            throw new IllegalArgumentException("malformed REST query parameter type hint");
            }

        Matcher matcher = QUERY_PARAMS_PATTERN.matcher(sQuery);
        while (matcher.find())
            {
            String sType = matcher.group(3);
            if (sType != null && !ALLOWED_TYPE_HINTS.contains(sType))
                {
                throw new IllegalArgumentException("unsupported REST query parameter type hint");
                }
            }
        return matcher.replaceAll(":$1");
        }

    /**
     * Log the LEGACY direct-query compatibility warning once.
     */
    private static void logLegacy()
        {
        if (LEGACY_WARNING_LOGGED.compareAndSet(false, true))
            {
            Logger.warn("Using legacy Coherence REST direct-query behavior. Configure explicit resources and enable DEV or PROD mode to require the REST-safe query subset.");
            }
        }

    /**
     * Return the standard disabled direct-query response.
     *
     * @return the forbidden response
     */
    private static Response directQueryForbiddenResponse()
        {
        return Response.status(Response.Status.FORBIDDEN)
                .entity("Direct query is not allowed").build();
        }

    /**
     * Return the standard invalid direct-query response.
     *
     * @return the bad-request response
     */
    private static Response directQueryBadRequestResponse()
        {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(CacheResource.BAD_REQUEST_MSG).build();
        }


    /**
     * REST-safe filter builder for already validated direct-query terms.
     */
    private static class RestFilterBuilder
            extends FilterBuilder
        {
        /**
         * Construct a REST-safe filter builder.
         *
         * @param indexedBindVars  the indexed bind variables
         * @param namedBindVars    the named bind variables
         * @param language         the query language
         */
        private RestFilterBuilder(List indexedBindVars, ResolvableParameterList namedBindVars,
                CoherenceQueryLanguage language)
            {
            super(indexedBindVars, namedBindVars, language);
            }

        @Override
        protected void acceptIdentifier(String sIdentifier)
            {
            if (sIdentifier == null || sIdentifier.isBlank() || sIdentifier.indexOf('.') >= 0)
                {
                throw new IllegalArgumentException("unsupported REST query identifier");
                }
            super.acceptIdentifier(sIdentifier);
            }

        @Override
        protected void acceptBinaryOperator(String sOperator, Term termLeft, Term termRight)
            {
            if (!ALLOWED_OPERATORS.contains(sOperator))
                {
                throw new IllegalArgumentException("unsupported REST query operator: " + sOperator);
                }
            super.acceptBinaryOperator(sOperator, termLeft, termRight);
            }

        @Override
        protected void acceptUnaryOperator(String sOperator, Term term)
            {
            switch (sOperator)
                {
                case "!":
                    super.acceptUnaryOperator(sOperator, term);
                    return;

                case "+":
                case "-":
                    if (!isNumericLiteral(term))
                        {
                        throw new IllegalArgumentException("unsupported REST query unary operand");
                        }
                    super.acceptUnaryOperator(sOperator, term);
                    return;

                default:
                    throw new IllegalArgumentException("unsupported REST query unary operator: " + sOperator);
                }
            }

        @Override
        protected void acceptCall(String sFunctionName, NodeTerm term)
            {
            if (!("key".equals(sFunctionName) || "value".equals(sFunctionName)) || term.length() != 0)
                {
                throw new IllegalArgumentException("unsupported REST query call");
                }
            super.acceptCall(sFunctionName, term);
            }

        @Override
        protected void acceptPath(NodeTerm term)
            {
            throw new IllegalArgumentException("unsupported REST query path");
            }
        }


    /**
     * CohQL language used for REST direct-query validation.
     */
    private static final CoherenceQueryLanguage LANGUAGE = new CoherenceQueryLanguage();

    /**
     * Allowed conditional operators.
     */
    private static final Set<String> ALLOWED_OPERATORS = Set.of(
            "&&", "||",
            "==", "!=", "<", "<=", ">", ">=",
            "between", "like", "ilike", "in",
            "contains", "contains_all", "contains_any");

    /**
     * Allowed atomic terms.
     */
    private static final Set<String> ALLOWED_ATOMS = Set.of(
            "String", "Short", "Integer", "Float", "Long", "Double",
            "Boolean", "Null", "Symbol");

    /**
     * Built-in REST-safe parameter type hints.
     */
    private static final Set<String> ALLOWED_TYPE_HINTS = Set.of(
            "i", "int", "s", "short", "l", "long", "f", "float",
            "d", "double", "I", "BigInteger", "D", "BigDecimal",
            "date", "uuid", "uid");

    /**
     * Capture query template parameter names.
     */
    private static final Pattern QUERY_PARAMS_PATTERN =
            Pattern.compile(":([a-zA-Z][a-zA-Z_0-9]*)(;([a-zA-Z][a-zA-Z_0-9\\.\\$]*))?");

    /**
     * Capture REST query type hints split by whitespace.
     */
    private static final Pattern MALFORMED_QUERY_PARAMS_PATTERN =
            Pattern.compile(":([a-zA-Z][a-zA-Z_0-9]*)(\\s+;\\s*|\\s*;\\s+)[a-zA-Z]");

    /**
     * Direct query type-hint policy marker.
     */
    private static final ThreadLocal<Boolean> DIRECT_QUERY_TYPE_POLICY = new ThreadLocal<>();

    /**
     * LEGACY warning guard.
     */
    private static final AtomicBoolean LEGACY_WARNING_LOGGED = new AtomicBoolean();
    }
