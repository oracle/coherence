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

import com.tangosol.coherence.dslquery.StatementResult;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.TokenTable;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.net.CacheFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

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
public class QueryHelper
    {
    // ----- FilterBuilder API ----------------------------------------------

    /**
    * Make a new Filter from the given String.
    *
    * @param sWhereClause  a String in the Coherence Query Language
    *                      representing a Filter
    *
    * @return   the constructed Filter
    *
    * @throws  FilterBuildingException may be thrown
    */
    public static Filter createFilter(String sWhereClause)
        {
        return createFilter(sWhereClause, new Object[0], new HashMap(), f_language);
        }

    /**
    * Make a new Filter from the given String.
    *
    * @param sWhereClause  a String in the Coherence Query Language
    *                      representing a Filter
    * @param aBindings     the array of Objects to use for Bind variables
    *
    * @return   the constructed Filter
    *
    * @throws  FilterBuildingException may be thrown
    */
    public static Filter createFilter(String sWhereClause, Object[] aBindings)
        {
        return createFilter(sWhereClause, aBindings, new HashMap(), f_language);
        }

    /**
    * Make a new Filter from the given String.
    *
    * @param sWhereClause  a String in the Coherence Query Language
     *                     representing a Filter
    * @param mapBindings   the Map of Objects to use for Bind variables
    *
    * @return   the constructed Filter
    *
    * @throws  FilterBuildingException may be thrown
    */
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
    * @return   the constructed Filter
    *
    * @throws  FilterBuildingException may be thrown
    */
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
    * @return   the constructed ValueExtractor
    *
    * @throws  FilterBuildingException may be thrown
    */
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
        ExecutionContext ctx = new ExecutionContext();

        ctx.setWriter(out);
        ctx.setReader(in);
        ctx.setSilentMode(true);
        ctx.setSanityCheckingEnabled(true);
        ctx.setCoherenceQueryLanguage(f_language);
        ctx.setCacheFactory(CacheFactory.getConfigurableCacheFactory());
        ctx.setCluster(CacheFactory.ensureCluster());

        return executeStatement(sStatement, ctx);
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
        if (sStatement == null || sStatement.length() == 0)
            {
            return null;
            }

        CoherenceQueryLanguage language  = context.getCoherenceQueryLanguage();
        TokenTable             toks      = language.extendedSqlTokenTable();
        OPParser               parser    = new OPParser(sStatement, toks, language.getOperators());
        Term                   term      = parser.parse();
        Statement              statement = language.prepareStatement((NodeTerm) term, context, null, null);

        if (context.isSanityChecking())
            {
            statement.sanityCheck(context);
            }

        StatementResult result = statement.execute(context);

        return result.getResult();
        }

    /**
     * The default {@link CoherenceQueryLanguage} used by this QueryHelper when no language
     * is provided to methods.
     */
    protected static final CoherenceQueryLanguage f_language = new CoherenceQueryLanguage();
    }
