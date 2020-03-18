/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.util.LiteSet;

import java.io.PrintWriter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A default implementation of a {@link StatementResult}.
 *
 * @author jk 2014.07.15
 * @since Coherence 12.2.1
 */
public class DefaultStatementResult
        implements StatementResult
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultStatementResult with the specified result
     * value.
     *
     * @param oResult  the result Object that this DefaultStatementResult contains
     */
    public DefaultStatementResult(Object oResult)
        {
        this(oResult, true);
        }

    /**
     * Construct a DefaultStatementResult with the specified result
     * value.
     *
     * @param oResult    the result Object that this DefaultStatementResult contains
     * @param fShowKeys  if true and oResult is a {@link Map} then the keys of the Map
     *                   will be printed by the {@link #print(java.io.PrintWriter, String)} method,
     *                   if false, no keys will be printed
     */
    public DefaultStatementResult(Object oResult, boolean fShowKeys)
        {
        f_oResult   = oResult;
        f_fShowKeys = fShowKeys;
        }

    // ----- StatementResult interface --------------------------------------

    @Override
    public Object getResult()
        {
        return f_oResult;
        }

    @Override
    public void print(PrintWriter writer, String sTitle)
        {
        printResults(writer, sTitle, f_oResult, f_fShowKeys);
        writer.flush();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Print the specified result value to the specified {@link PrintWriter}.
     *
     * @param writer     the PrintWriter to print the results to
     * @param sTitle     the optional title to print before the results
     * @param oResult    the result object to print
     * @param fShowKeys  a flag to determine whether to print keys if the result object is a map
     */
    protected void printResults(PrintWriter writer, String sTitle, Object oResult, boolean fShowKeys)
        {
        if (oResult == null)
            {
            return;
            }

        if (sTitle != null)
            {
            writer.println(sTitle);
            }

        if (oResult instanceof Map)
            {
            printResultsMap(writer, (Map) oResult, fShowKeys);
            }
        else if (oResult instanceof LiteSet)
            {
            printResultsCollection(writer, (Collection) oResult, fShowKeys);
            }
        else if (oResult instanceof Collection)
            {
            printResultsCollection(writer, (Collection) oResult, false);
            }
        else
            {
            printStringOrObject(writer, oResult);
            writer.println();
            }
        }

    /**
     * Print out the given Object on the given {@link PrintWriter}.
     *
     * @param writer         a PrintWriter to print on
     * @param oResult        the object to print
     * @param fPrintNewLine  a flag controlling whether to print a new line
     * @param fTopObject     a flag to tell whether the object is outermost
     */
    protected void printObject(PrintWriter writer, Object oResult, boolean fPrintNewLine, boolean fTopObject)
        {
        writer.flush();

        if (oResult instanceof Object[])
            {
            Object[] aoTuple = (Object[]) oResult;

            writer.print("[");

            boolean first = true;

            for (Object t : aoTuple)
                {
                if (!first)
                    {
                    writer.print(", ");
                    }

                first = false;
                printStringOrObject(writer, t);
                }

            writer.print("]");
            }
        else if (oResult instanceof List)
            {
            if (!fTopObject)
                {
                writer.print("[");
                }

            printCommaSeparatedCollection(writer, (Collection) oResult);

            if (!fTopObject)
                {
                writer.print("]");
                }
            }
        else if (oResult instanceof Map)
            {
            boolean first = true;

            writer.print("{");

            for (Map.Entry me : (Set<Map.Entry>) ((Map) oResult).entrySet())
                {
                if (!first)
                    {
                    writer.print(", ");
                    }

                first = false;
                printStringOrObject(writer, me.getKey(), false);
                writer.print(": ");
                printStringOrObject(writer, me.getValue());
                }

            writer.print("}");
            }
        else if (oResult instanceof Set)
            {
            if (!fTopObject)
                {
                writer.print("{");
                }

            printCommaSeparatedCollection(writer, (Collection) oResult);

            if (!fTopObject)
                {
                writer.print("}");
                }
            }
        else
            {
            writer.print(oResult);
            }

        if (fPrintNewLine)
            {
            writer.println();
            }
        }

    /**
     * Print the contents of the given {@link Collection} to the specified
     * {@link PrintWriter} as a comma separated list.
     *
     * @param writer  the PrintWriter to print the Collection to
     * @param col     the Collection to print
     */
    protected void printCommaSeparatedCollection(PrintWriter writer, Collection col)
        {
        boolean first = true;

        for (Object value : col)
            {
            if (!first)
                {
                writer.print(", ");
                }

            first = false;
            printStringOrObject(writer, value);
            }
        }

    /**
     * If the given Object is a String print it within double quotes around otherwise
     * pass the Object to the {@link #printObject(PrintWriter, Object, boolean, boolean)} method.
     *
     * @param writer  a PrintWriter to print on
     * @param o       the object to print
     */
    protected void printStringOrObject(PrintWriter writer, Object o)
        {
        printStringOrObject(writer, o, false);
        }

    /**
     * If the given Object is a String print it within double quotes around otherwise
     * pass the Object to the {@link #printObject(PrintWriter, Object, boolean, boolean)} method.
     *
     * @param writer        a PrintWriter to print on
     * @param o             the object to print
     * @param fNewLine  a flag controlling whether to print a new line
     */
    protected void printStringOrObject(PrintWriter writer, Object o, boolean fNewLine)
        {
        if (o instanceof String)
            {
            writer.print("\"" + o + "\"");

            if (fNewLine)
                {
                writer.println();
                }
            }
        else
            {
            printObject(writer, o, fNewLine, false);
            }
        }

    /**
     * Print the given {@link Collection} of Objects on the given {@link PrintWriter}.
     *
     * @param writer     a PrintWriter to print on
     * @param col        the Collection to print
     * @param fShowKeys  true to show keys
     */
    protected void printResultsCollection(PrintWriter writer, Collection col, boolean fShowKeys)
        {
        if (col == null)
            {
            return;
            }

        for (Object o : col)
            {
            if (!fShowKeys && o instanceof Map.Entry)
                {
                printStringOrObject(writer, ((Map.Entry) o).getValue(), true);
                }
            else
                {
                printStringOrObject(writer, o, true);
                }
            }
        }

    /**
     * Print the contents of the specified {@link Map} to the specified
     * {@link PrintWriter}.
     *
     * @param writer    a PrintWriter to print on
     * @param map       the Map to print
     * @param fShowKey  a flag controlling whether to print the Maps keys
     */
    protected void printResultsMap(PrintWriter writer, Map map, boolean fShowKey)
        {
        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet())
            {
            if (fShowKey)
                {
                printStringOrObject(writer, entry.getKey(), false);
                writer.print(": ");
                }

            printStringOrObject(writer, entry.getValue(), true);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The actual result of executing a CohQL {@link com.tangosol.coherence.dslquery.Statement}.
     */
    protected final Object f_oResult;

    /**
     * A flag to determine whether to print keys in the {@link #print(java.io.PrintWriter, String)}
     * method if the value in {@link #f_oResult} is a {@link Map}.
     */
    protected final boolean f_fShowKeys;
    }
