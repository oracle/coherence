/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.coherence.config.ResolvableParameterList;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.FilterBuilder;
import com.tangosol.coherence.dslquery.Statement;

import com.tangosol.coherence.dsltools.precedence.OPParser;

import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.net.Session;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
* <p>
* QueryHelper is a utility class that provides a set of factory methods
* used for building instances of {@link Filter} or
* {@link ValueExtractor}.
* </p>
* <p>
* The QueryHelper API accepts a String that specifies the creation of rich
* Filters in a format similar to SQL <code>WHERE</code> clauses.  For example,
* the String <code>"street = 'Main' and state = 'TX'"</code> will create a
* tree of Filters similar to the following Java code:
* </p>
* <p>
* <code>
* new AndFilter(<br>
* &nbsp;&nbsp;new EqualsFilter("getStreet","Main"),<br>
* &nbsp;&nbsp;new EqualsFilter("getState","TX"));<br>
* </code>
* </p>
* <p>
* The following keywords are currently supported (words between brackets
* are optional):
* <ul>
* <li>Comparison operators: <code>=, &gt;, &gt;=, &lt;, &lt;=, &lt;&gt; [ NOT ] BETWEEN,
* [ NOT ] LIKE, [ NOT ] IN, IS [ NOT ] NULL , CONTAINS [ ALL | ANY ]</code></li>
* <li>Logical operators: <code>(AND, OR, NOT)</code></li>
* <li>Literal numbers, and the constants <code>true, false</code>, and
* <code>null</code></li>
* </ul>
* <p>
* Each argument to an operator is converted into a
* {@link com.tangosol.util.extractor.ReflectionExtractor}.  Additionally,
* the <code>"."</code> operator will use
* {@link com.tangosol.util.extractor.ChainedExtractor}. Pseudo functions
* <code>key()</code> and <code>value()</code> may be used to specify the use
* of a key as in <code>"key() between 10 and 50"</code>. The
* <code>value()</code> pseudo function is a shorthand for
* {@link com.tangosol.util.extractor.IdentityExtractor}.
* </p>
* <p>
* Query bind variables are supported in two forms. One form is a
* <code>"?"</code> followed by a position number; for example:
* <code>"dept = ?1 and serviceCode in [10,20,30]"</code>. For this usage,
* supply an Object array of parameters to
* {@link #createFilter(String, Object[])}.  <i><b>Note:</b> this scheme
* treats 1 as the starting index into the array.</i>
* </p>
* <p>
* Additionally, named bind parameters are supported. The above example could
* be: <code>"dept = :deptNum and serviceCode in :myList"</code>.
* This style requires the use of a Map with
* {@link #createFilter(String, Map)}.  Both forms can be used in the
* same specification.
* </p>
* <p>
* The factory methods catch a number of Exceptions from the implementation
* stages and subsequently may throw an unchecked
* {@link FilterBuildingException}.
* </p>
* @author djl  2009.9.3
*/
public final class QueryHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
    * No new instances as this is a static helper.
    */
    private QueryHelper()
        {
        }

    // ----- FilterBuilder API ----------------------------------------------

    /**
    * Make a new Filter from the given String.
    *
    * @param sWhereClause  a String in the Coherence Query Language
    *                      representing a Filter
    *
    * @return the constructed Filter
    *
    * @throws  FilterBuildingException may be thrown
    */
    @SuppressWarnings("rawtypes")
    public static Filter createFilter(String sWhereClause)
        {
        return createFilter(sWhereClause, new Object[0], new HashMap(), f_language);
        }

    /**
    * Make a new Filter from the given String.
    *
    * @param sWhereClause  a String in the Coherence Query Language
    *                      representing a Filter
    * @param aoBindings    the array of Objects to use for Bind variables
    *
    * @return the constructed Filter
    *
    * @throws  FilterBuildingException may be thrown
    */
    @SuppressWarnings("rawtypes")
    public static Filter createFilter(String sWhereClause, Object[] aoBindings)
        {
        return createFilter(sWhereClause, aoBindings, new HashMap(), f_language);
        }

    /**
    * Make a new Filter from the given String.
    *
    * @param sWhereClause  a String in the Coherence Query Language
    *                      representing a Filter
    * @param mapBindings   the Map of Objects to use for Bind variables
    *
    * @return the constructed Filter
    *
    * @throws  FilterBuildingException may be thrown
    */
    @SuppressWarnings("rawtypes")
    public static Filter createFilter(String sWhereClause, Map mapBindings)
        {
        return createFilter(sWhereClause, new Object[0], mapBindings, f_language);
        }

    /**
    * Make a new Filter from the given String.
    *
    * @param sWhereClause  a String in the Coherence Query Language
    *                      representing a Filter
    * @param aBindings     the array of Objects to use for Bind variables
    * @param mapBindings   the Map of Objects to use for Bind variables
    *
    * @return the constructed Filter
    *
    * @throws  FilterBuildingException may be thrown
    */
    @SuppressWarnings("rawtypes")
    public static Filter createFilter(String sWhereClause, Object[] aBindings, Map mapBindings)
        {
        return createFilter(sWhereClause, aBindings, mapBindings, f_language);
        }

    /**
     * Make a new Filter from the given String.
     *
     * @param sWhereClause  a String in the Coherence Query Language
     *                      representing a Filter
     * @param aBindings     the array of Objects to use for indexed Bind variables
     * @param mapBindings   the Map of Objects to use for named Bind variables
     * @param language      the CoherenceQueryLanguage instance to use
     *
     * @return the constructed Filter
     *
     * @throws  FilterBuildingException may be thrown
     */
    @SuppressWarnings("rawtypes")
    public static Filter createFilter(String sWhereClause, Object[] aBindings,
                                      Map mapBindings, CoherenceQueryLanguage language)
        {
        try
            {
            FilterBuilder filterBuilder = new FilterBuilder(
                    aBindings == null ? Collections.emptyList() : Arrays.asList(aBindings),
                    new ResolvableParameterList(mapBindings), language);

            return filterBuilder.makeFilter(parse(sWhereClause));
            }
        catch (RuntimeException e)
            {
            throw new FilterBuildingException(e.getMessage(), sWhereClause, e);
            }
        }

    /**
    * Make a new ValueExtractor from the given String.
    *
    * @param s  a String in the Coherence Query Language representing
    *           a ValueExtractor
    *
    * @return the constructed ValueExtractor
    *
    * @throws  FilterBuildingException may be thrown
    */
    @SuppressWarnings("rawtypes")
    public static ValueExtractor createExtractor(String s)
        {
        return createExtractor(s, f_language);
        }

    /**
    * Make a new ValueExtractor from the given String.
    *
    * @param sQuery    a String in the Coherence Query Language representing
    *                  a ValueExtractor
    * @param language  the CoherenceQueryLanguage instance to use
    *
    * @return the constructed ValueExtractor
    *
    * @throws FilterBuildingException may be thrown
    */
    @SuppressWarnings("rawtypes")
    public static ValueExtractor createExtractor(String sQuery, CoherenceQueryLanguage language)
        {
        try
            {
            return new FilterBuilder(language).makeExtractor((NodeTerm) parse(sQuery));
            }
        catch (RuntimeException e)
            {
            throw new FilterBuildingException(e.getMessage(), sQuery, e);
            }
        }

    /**
    * Return the {@link Term} representing the AST produced by
    * parsing the specified CohQL query String.
    *
    * @param sQuery  the CohQL query to be parsed
    *
    * @return the Term representing the AST produced by
    *         parsing the specified CohQL query String
    */
    protected static Term parse(String sQuery)
        {
        return parse(sQuery, f_language);
        }

    /**
    * Return the {@link Term} representing the AST produced by
    * parsing the specified CohQL query String.
    * This method takes a CoherenceQueryLanguage parameter which
    * allows CohQL customisations to be applied to the CohQL syntax.
    *
    * @param sQuery    the CohQL query to be parsed
    * @param language  the CoherenceQueryLanguage instance to use
    *
    * @return the Term representing the AST produced by
    *         parsing the specified CohQL query String
    */
    protected static Term parse(String sQuery, CoherenceQueryLanguage language)
        {
        OPParser parser = new OPParser(sQuery, language.filtersTokenTable(), language.getOperators());
        return parser.parse();
        }

    /**
    * Execute a CohQL statement. This method accepts a complete query
    * string as the argument. The type of object returned depends on the query:
    * <table>
    *   <caption>Examples on CohQL</caption>
    *   <tr>
    *     <th width="25%">Return Type</th>
    *     <th>Query Description</th>
    *   </tr>
    *   <tr>
    *     <td valign="top">Cache entry value</td>
    *     <td valign="top">If the statement is an <code>INSERT</code> operation
    *     the previous entry for the given key will be returned; otherwise null</td>
    *   </tr>
    *   <tr>
    *     <td valign="top">{@link java.lang.Number}</td>
    *     <td valign="top">If the query is an aggregation that returns a single numerical
    *     value; for example: <code>count()</code>, <code>avg()</code>,
    *     <code>max()</code>, <code>min()</code></td>
    *   </tr>
    *   <tr>
    *     <td valign="top">{@link java.util.Map}</td>
    *     <td valign="top">If query has a grouping</td>
    *   </tr>
    *   <tr>
    *     <td valign="top">{@link java.util.Collection}</td>
    *     <td valign="top">For all other <code>SELECT</code>, <code>DELETE</code>,
    *     <code>UPDATE</code> statements</td>
    *   </tr>
    *   <tr>
    *     <td valign="top"><code>null</code></td>
    *     <td valign="top">If the query returns no results or for
    *     <code>create/delete</code>  index or <code>drop/create</code> cache,
    *     or <code>backup/restore</code></td>
    *   </tr>
    * </table>
    *
    * @param sStatement  a Coherence Query Language statement
    *
    * @return the query results
    *
    * @throws RuntimeException if an invalid query is provided
    */
    public static Object executeStatement(String sStatement)
        {
        PrintWriter      out = new PrintWriter(System.out);
        BufferedReader   in  = new BufferedReader(new InputStreamReader(System.in));
        ExecutionContext ctx = createExecutionContext(CacheFactory.getConfigurableCacheFactory());

        ctx.setWriter(out);
        ctx.setReader(in);
        ctx.setCluster(CacheFactory.ensureCluster());

        return executeStatement(sStatement, ctx);
        }

    /**
    * Create a new {@link ExecutionContext}. The following properties will be set on the returned context:
    * <ul>
    *     <li>silent mode is enabled</li>
    *     <li>sanity checking is enabled</li>
    *     <li>cache factory based on provided argument</li>
    * </ul>
    *
    * @param factory  the {@link ConfigurableCacheFactory} the {@link ExecutionContext} should use
    *
    * @return a new {@link ExecutionContext}
    *
    * @throws IllegalArgumentException if {@code factory} is {@code null}
    *
    * @since 21.06
    */
    public static ExecutionContext createExecutionContext(ConfigurableCacheFactory factory)
        {
        if (factory == null)
            {
            throw new IllegalArgumentException("must specify a CacheFactory");
            }

        ExecutionContext ctx = createCommonExecutionContext();
        //noinspection deprecation
        ctx.setCacheFactory(factory);

        return ctx;
        }

    /**
    * Create a new {@link ExecutionContext}. The following properties will be set on the returned context:
    * <ul>
    *     <li>silent mode is enabled</li>
    *     <li>sanity checking is enabled</li>
    *     <li>session based on provided argument</li>
    *     <li>extended language support enabled</li>
    * </ul>
    *
    * @param session  the {@link Session} the {@link ExecutionContext} should use
    *
    * @return a new {@link ExecutionContext}
    *
    * @throws IllegalArgumentException if {@code session} is {@code null}
    *
    * @since 21.06
    */
    public static ExecutionContext createExecutionContext(Session session)
        {
        if (session == null)
            {
            throw new IllegalArgumentException("must specify a Session");
            }

        ExecutionContext ctx = createCommonExecutionContext();
        ctx.setSession(session);

        return ctx;
        }

    /**
    * Create a new {@link ExecutionContext}. The following properties will be set on the returned context:
    * <ul>
    *     <li>silent mode is enabled</li>
    *     <li>sanity checking is enabled</li>
    *     <li>extended language support enabled</li>
    *     <li>the coherence query language</li>
    * </ul>
    *
    * @return a new {@link ExecutionContext}
    *
    * @throws IllegalArgumentException if {@code session} is {@code null}
    *
    * @since 21.06
    */
    protected static ExecutionContext createCommonExecutionContext()
        {
        ExecutionContext ctx = new ExecutionContext();

        ctx.setSilentMode(true);
        ctx.setSanityCheckingEnabled(true);
        ctx.setExtendedLanguage(true);
        ctx.setCoherenceQueryLanguage(f_language);

        return ctx;
        }

    /**
    * Execute a CohQL statement. This method accepts a complete query
    * string as the argument. The type of object returned depends on the query:
    * <table>
    *   <caption>Query return types</caption>
    *   <tr>
    *     <th width="25%">Return Type</th>
    *     <th>Query Description</th>
    *   </tr>
    *   <tr>
    *     <td valign="top">Cache entry value</td>
    *     <td valign="top">If the statement is an <code>INSERT</code> operation
    *     the previous entry for the given key will be returned; otherwise null</td>
    *   </tr>
    *   <tr>
    *     <td valign="top">{@link java.lang.Number}</td>
    *     <td valign="top">If the query is an aggregation that returns a single numerical
    *     value; for example: <code>count()</code>, <code>avg()</code>,
    *     <code>max()</code>, <code>min()</code></td>
    *   </tr>
    *   <tr>
    *     <td valign="top">{@link java.util.Map}</td>
    *     <td valign="top">If query has a grouping</td>
    *   </tr>
    *   <tr>
    *     <td valign="top">{@link java.util.Collection}</td>
    *     <td valign="top">For all other <code>SELECT</code>, <code>DELETE</code>,
    *     <code>UPDATE</code> statements</td>
    *   </tr>
    *   <tr>
    *     <td valign="top"><code>null</code></td>
    *     <td valign="top">If the query returns no results or for
    *     <code>create/delete</code>  index or <code>drop/create</code> cache,
    *     or <code>backup/restore</code></td>
    *   </tr>
    * </table>
    *
    * @param sStatement  a Coherence Query Language statement
    * @param context     the {@link ExecutionContext} to use
     *
    * @return the query results
    *
    * @throws RuntimeException if an invalid query is provided
    */
    public static Object executeStatement(String sStatement, ExecutionContext context)
        {
        if (sStatement == null || sStatement.isEmpty())
            {
            return null;
            }

        return createStatement(sStatement, context).execute(context).getResult();
        }

    /**
    * Creates a new {@code Coherence Query Language} {@link Statement} from the provided query string.
    *
    * @param sStatement  a Coherence Query Language statement
    * @param context     the {@link ExecutionContext} to use
    *
    * @return the parsed {@code Coherence Query Language} statement ready for execution
    *
    * @throws RuntimeException if an invalid query is provided
    *
    * @see #createStatement(String, ExecutionContext, Object[])
    * @see #createStatement(String, ExecutionContext, Map)
    *
    * @since 21.06
    */
    public static Statement createStatement(String sStatement, ExecutionContext context)
        {
        return createStatement(sStatement, context, null, null);
        }

    /**
    * Creates a new {@code Coherence Query Language} {@link Statement} from the provided query string.
    * This query string may contain zero or more parameters in the format of {@code ?{n}} where {@code {n}}
    * is a numeric value indicating the position of the parameter in the provided argument array.
    *
    * @param sStatement          a Coherence Query Language statement
    * @param context             the {@link ExecutionContext} to use
    * @param oaPositionalParams  the positional parameter values
    *
    * @return the parsed {@code Coherence Query Language} statement ready for execution
    *
    * @throws RuntimeException if an invalid query is provided
    *
    * @see #createStatement(String, ExecutionContext)
    * @see #createStatement(String, ExecutionContext, Map)
    *
    * @since 21.06
    */
    public static Statement createStatement(String sStatement, ExecutionContext context, Object[] oaPositionalParams)
        {
        return createStatement(sStatement, context, oaPositionalParams, null);
        }

    /**
    * Creates a new {@code Coherence Query Language} {@link Statement} from the provided query string.
    * This query string may contain zero or more parameters in the format of {@code :{key}} where {@code {key}}
    * is maps to a key in the provided binding parameters.
    *
    * @param sStatement        a Coherence Query Language statement
    * @param context           the {@link ExecutionContext} to use
    * @param mapBindingParams  a {@code Map} keyed by the parameter name within the query string mapped
    *                          to the binding value
    *
    * @return the parsed {@code Coherence Query Language} statement ready for execution
    *
    * @throws RuntimeException if an invalid query is provided
    *
    * @see #createStatement(String, ExecutionContext)
    * @see #createStatement(String, ExecutionContext, Object[])
    *
    * @since 21.06
    */
    public static Statement createStatement(String sStatement,
                                            ExecutionContext context,
                                            Map<String, Object> mapBindingParams)
        {
        return createStatement(sStatement, context, null, mapBindingParams);
        }

    /**
    * Creates a new {@code Coherence Query Language} {@link Statement} from the provided query string.
    *
    * @param sStatement          a Coherence Query Language statement
    * @param context             the {@link ExecutionContext} to use
    * @param aoPositionalParams  parameters, in order, to be applied to query
    * @param mapBindingParams    parameters, mapped to binding names
    *
    * @return the parsed {@code Coherence Query Language} statement ready for execution
    *
    * @throws RuntimeException if an invalid query is provided
    *
    * @see #createStatement(String, ExecutionContext)
    * @see #createStatement(String, ExecutionContext, Object[])
    * @see #createStatement(String, ExecutionContext, Map)
    *
    * @since 21.06
    */
    protected static Statement createStatement(String sStatement,
                                               ExecutionContext context,
                                               Object[] aoPositionalParams,
                                               Map<String, Object> mapBindingParams)
        {
        if (sStatement == null || sStatement.isEmpty())
            {
            return null;
            }

        CoherenceQueryLanguage language       = context.getCoherenceQueryLanguage();
        OPParser               parser         = context.instantiateParser(new StringReader(sStatement));
        Term                   term           = parser.parse();
        List<Object>           listPositional = aoPositionalParams == null || aoPositionalParams.length == 0
                                                    ? null
                                                    : Arrays.asList(aoPositionalParams);
        ResolvableParameterList mapBindings   = mapBindingParams == null || mapBindingParams.isEmpty()
                                                    ? null
                                                    : new ResolvableParameterList(mapBindingParams);
        Statement              statement      = language.prepareStatement((NodeTerm) term,
                                                                          context, listPositional, mapBindings);

        if (context.isSanityChecking())
            {
            statement.sanityCheck(context);
            }

        return statement;
        }

    /**
     * The default {@link CoherenceQueryLanguage} used by this QueryHelper when no language
     * is provided to methods.
     */
    protected static final CoherenceQueryLanguage f_language = new CoherenceQueryLanguage();
    }
