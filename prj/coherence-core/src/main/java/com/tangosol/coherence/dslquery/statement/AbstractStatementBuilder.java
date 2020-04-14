/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.FilterBuilder;
import com.tangosol.coherence.dslquery.Statement;
import com.tangosol.coherence.dslquery.StatementBuilder;

import com.tangosol.coherence.dsltools.precedence.OPToken;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.Filter;

import com.tangosol.util.filter.AlwaysFilter;

import java.util.List;

/**
 * A base class for {@link com.tangosol.coherence.dslquery.StatementBuilder} implementations.
 *
 * @author jk  2013.12.09
 * @since Coherence 12.2.1
 */
public abstract class AbstractStatementBuilder<T extends Statement>
        implements StatementBuilder<T>
    {
    // ----- helper methods -------------------------------------------------

    /**
     * Build a {@link Filter} for the given cache using the given where clause,
     * alias and bind environments.
     *
     * @param termWhere        the {@link NodeTerm} containing the where clause
     * @param sCacheName       the name of the cache that the filter will be built for
     * @param sAlias           the table/cache alias used in the where clause, may be null
     * @param listBindVars     bind variables to be used to replace any numeric bind
     *                         variables in the where clause
     * @param namedBindVars    named bind variables to be used to replace any named bind
     *                         variables in the where clause
     * @param ctx              the {@link ExecutionContext} to use
     *
     * @return a {@link Filter} created from the given where clause
     */
    protected static Filter ensureFilter(NodeTerm termWhere, String sCacheName, String sAlias, List listBindVars,
            ParameterResolver namedBindVars, ExecutionContext ctx)
        {
        if (termWhere == null)
            {
            return AlwaysFilter.INSTANCE;
            }

        FilterBuilder bldrFilter = new FilterBuilder(listBindVars, namedBindVars, ctx.getCoherenceQueryLanguage());

        bldrFilter.setAlias(sAlias);

        return bldrFilter.makeFilterForCache(sCacheName, termWhere, listBindVars, namedBindVars);
        }

    /**
     * Return a String that is the value of the given Term.
     *
     * @param t  the Term that is atomic
     *
     * @return return the String found in the AtomicTerm
     */
    protected static String atomicStringValueOf(Term t)
        {
        return (t != null && t.isAtom())
               ? ((AtomicTerm) t).getValue()
               : null;
        }

    /**
     * Return the String that represents the cache name from the given AST
     * node by looking for the "from" term AST.
     *
     * @param sn  the syntax node
     *
     * @return return the String found in the AST node
     */
    protected static String getCacheName(NodeTerm sn)
        {
        return atomicStringValueOf(sn.findAttribute("from"));
        }

    /**
     * Return the String that represents the cache name alias from the given AST
     * node by looking for the "alias" term in the AST.
     *
     * @param sn  the syntax node
     *
     * @return return the String found in the AST node
     */
    protected static String getAlias(NodeTerm sn)
        {
        return atomicStringValueOf(sn.findAttribute("alias"));
        }

    /**
     * Return the String that represents the filename from the given AST
     * node by looking for the "file" term in the AST.
     *
     * @param sn  the syntax node
     *
     * @return return the String found in the AST node
     */
    protected static String getFile(NodeTerm sn)
        {
        return atomicStringValueOf(sn.findAttribute("file"));
        }

    /**
     * Return the boolean that indicates whether distinctness in indicated
     * in the given AST node.
     *
     * @param sn  the syntax node
     *
     * @return return the boolean result of testing the node
     */
    protected static boolean getIsDistinct(NodeTerm sn)
        {
        String dist = atomicStringValueOf(sn.findAttribute("isDistinct"));

        return dist != null && dist.equals("true");
        }

    /**
     * Return the AST node that represents the where clause from the given AST
     * node.
     *
     * @param sn  the syntax node
     *
     * @return return the AST node found in the parent AST node
     */
    protected static NodeTerm getWhere(NodeTerm sn)
        {
        Term t = sn.findChild("whereClause");

        if (t == null)
            {
            return null;
            }

        if (t.length() == 1)
            {
            return (NodeTerm) t.termAt(1);
            }

        return null;
        }

    /**
     * Return the AST node that represents the fields to select from the
     * given AST node.
     *
     * @param sn  the syntax node
     *
     * @return return the AST node found in the parent AST node
     */
    protected static NodeTerm getFields(NodeTerm sn)
        {
        return (NodeTerm) sn.findChild(OPToken.FIELD_LIST);
        }

    /**
     * Return the AST node that represents the key to insert from the
     * given AST node.
     *
     * @param sn  the syntax node
     *
     * @return return the AST node found in the parent AST node
     */
    protected static Term getInsertKey(NodeTerm sn)
        {
        return sn.findAttribute("key");
        }

    /**
     * Return the AST node that represents the value to insert from the
     * given AST node.
     *
     * @param sn  the syntax node
     *
     * @return return the AST node found in the parent AST node
     */
    protected static Term getInsertValue(NodeTerm sn)
        {
        return sn.findAttribute("value");
        }

    /**
     * Return the AST node that represents the group by fields from the
     * given AST node.
     *
     * @param sn  the syntax node
     *
     * @return return the AST node found in the parent AST node
     */
    protected static NodeTerm getGroupBy(NodeTerm sn)
        {
        NodeTerm t = (NodeTerm) sn.findChild("groupBy");

        if (t == null)
            {
            return null;
            }

        if (t.length() == 0)
            {
            return null;
            }

        return t;
        }

    /**
     * Return the AST node that represents the list of "Set statements" from the
     * given AST node.
     *
     * @param sn  the syntax node
     *
     * @return return the AST node found in the parent AST node
     */
    protected static Term getSetList(NodeTerm sn)
        {
        return sn.findChild("setList");
        }

    /**
     * Return the AST node that represents the extractor for an index from the
     * given AST node.
     *
     * @param sn  the syntax node
     *
     * @return return the AST node found in the parent AST node
     */
    protected static Term getExtractor(NodeTerm sn)
        {
        return sn.findChild("extractor");
        }

    /**
     * Test to see if the AST for the group-by is equal to the head of
     * the list from the select clause AST.
     *
     * @param fieldList    the list of fields in a select list
     * @param groupByList  the list of fields in a group by clause
     *
     * @return return the results of matching
     */
    protected static boolean headsMatch(NodeTerm fieldList, NodeTerm groupByList)
        {
        return groupByList.headChildrenTermEqual(fieldList);
        }
    }
