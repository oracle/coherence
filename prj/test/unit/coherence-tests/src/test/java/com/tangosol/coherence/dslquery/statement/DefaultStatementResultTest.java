/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.StatementResult;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2014.07.15
 */
public class DefaultStatementResultTest
    {
    @Test
    public void shouldPrintStringInQuotes()
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        DefaultStatementResult result = new DefaultStatementResult("Hello World");

        result.printStringOrObject(out, "Hello World");
        out.flush();

        assertThat(writer.toString(), is("\"Hello World\""));
        }

    @Test
    public void shouldPrintStringInQuotesWithNewLine()
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        DefaultStatementResult result = new DefaultStatementResult(null);

        result.printStringOrObject(out, "Hello World", true);
        out.flush();

        assertThat(writer.toString(), is("\"Hello World\"" + CR));
        }

    @Test
    public void shouldPrintNonStringStringWithoutQuotes()
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        DefaultStatementResult result = new DefaultStatementResult(null);

        result.printStringOrObject(out, 1966);
        out.flush();

        assertThat(writer.toString(), is("1966"));
        }

    @Test
    public void shouldPrintNonStringStringWithoutQuotesWithNewLine()
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        DefaultStatementResult result = new DefaultStatementResult(null);

        result.printStringOrObject(out, 1966, true);
        out.flush();

        assertThat(writer.toString(), is("1966" + CR));
        }

    @Test
    public void shouldPrintResultsWithNullResults()
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        DefaultStatementResult result = new DefaultStatementResult(null);

        result.print(out, "Results:");
        out.flush();

        assertThat(writer.toString(), is(""));
        }

    @Test
    public void shouldPrintResultsWithObjectResults()
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        Object                 o      = new Object();
        DefaultStatementResult result = new DefaultStatementResult(o);

        result.print(out, "Results:");
        out.flush();

        assertThat(writer.toString(), is("Results:" + CR + o.toString() + CR));
        }

    @Test
    public void shouldPrintResultsWithCollectionResults()
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        Object                 o      = Arrays.asList("Hello", "World", 100);
        DefaultStatementResult result = new DefaultStatementResult(o);

        result.print(out, "Results:");
        out.flush();

        assertThat(writer.toString(), is("Results:" + CR + "\"Hello\"" + CR + "\"World\"" + CR + "100" + CR));
        }

    @Test
    public void shouldPrintResultsWithMapResults()
            throws Exception
        {
        StringWriter    writer = new StringWriter();
        PrintWriter     out    = new PrintWriter(writer);
        Map             map    = new LinkedHashMap();
        StatementResult result = new DefaultStatementResult(map);

        map.put("Key-1", "Value-1");
        map.put(100, 200);

        result.print(out, "Results:");
        out.flush();

        assertThat(writer.toString(), is("Results:" + CR + "\"Key-1\": \"Value-1\"" + CR + "100: 200" + CR));
        }

    @Test
    public void shouldPrintResultsWithMapResultsWithoutKeys()
            throws Exception
        {
        StringWriter    writer = new StringWriter();
        PrintWriter     out    = new PrintWriter(writer);
        Map             map    = new LinkedHashMap();
        StatementResult result = new DefaultStatementResult(map, false);

        map.put("Key-1", "Value-1");
        map.put(100, 200);

        result.print(out, "Results:");
        out.flush();

        assertThat(writer.toString(), is("Results:" + CR + "\"Value-1\"" + CR + "200" + CR));
        }

    @Test
    public void shouldPrintNothingIfResultCollectionIsNull()
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        DefaultStatementResult result = new DefaultStatementResult(null);

        result.printResultsCollection(out, null, false);
        out.flush();

        assertThat(writer.toString(), is(""));
        }

    @Test
    public void shouldPrintNothingIfResultCollectionIsEmpty()
            throws Exception
        {
        StringWriter           writer  = new StringWriter();
        PrintWriter            out     = new PrintWriter(writer);
        Collection             results = Collections.emptyList();
        DefaultStatementResult result  = new DefaultStatementResult(results);

        result.printResultsCollection(out, results, false);
        out.flush();

        assertThat(writer.toString(), is(""));
        }

    @Test
    public void shouldPrintResultCollection()
            throws Exception
        {
        StringWriter           writer  = new StringWriter();
        PrintWriter            out     = new PrintWriter(writer);
        Collection             results = Arrays.asList("One", "Two", 3);
        DefaultStatementResult result  = new DefaultStatementResult(results);

        result.printResultsCollection(out, results, false);
        out.flush();

        assertThat(writer.toString(), is("\"One\"" + CR + "\"Two\"" + CR + "3" + CR));
        }

    @Test
    public void shouldPrintObjectThatIsNull()
            throws Exception
        {
        assertPrintsObject(null, "null");
        }

    @Test
    public void shouldPrintObject()
            throws Exception
        {
        assertPrintsObject(12345, "12345");
        }

    @Test
    public void shouldPrintObjectThatIsEmptyArray()
            throws Exception
        {
        assertPrintsObject(new Object[0], "[]");
        }

    @Test
    public void shouldPrintObjectThatIsArray()
            throws Exception
        {
        assertPrintsObject(new Object[] {1, "two", 3}, "[1, \"two\", 3]");
        }

    @Test
    public void shouldPrintObjectThatIsEmptyList()
            throws Exception
        {
        assertPrintsObject(new ArrayList(), "[]");
        }

    @Test
    public void shouldPrintObjectThatIsList()
            throws Exception
        {
        assertPrintsObject(Arrays.asList(1, "two", 3), "[1, \"two\", 3]");
        }

    @Test
    public void shouldPrintObjectThatIsEmptySet()
            throws Exception
        {
        assertPrintsObject(new HashSet(), "{}");
        }

    @Test
    public void shouldPrintObjectThatIsSet()
            throws Exception
        {
        LinkedHashSet set = new LinkedHashSet();

        set.add(1);
        set.add("two");
        set.add(3);

        assertPrintsObject(set, "{1, \"two\", 3}");
        }

    @Test
    public void shouldPrintObjectThatIsEmptyMap()
            throws Exception
        {
        assertPrintsObject(new HashSet(), "{}");
        }

    @Test
    public void shouldPrintObjectThatIsMap()
            throws Exception
        {
        LinkedHashMap map = new LinkedHashMap();

        map.put(1, "one");
        map.put("two", 2);
        map.put(3, "three");

        assertPrintsObject(map, "{1: \"one\", \"two\": 2, 3: \"three\"}");
        }

    public void assertPrintsObject(Object o, String expected)
            throws Exception
        {
        StringWriter           writer = new StringWriter();
        PrintWriter            out    = new PrintWriter(writer);
        DefaultStatementResult result = new DefaultStatementResult(o);

        result.printObject(out, o, false, false);
        out.flush();

        assertThat(writer.toString(), is(expected));
        }

    public static final String CR = System.getProperty("line.separator");
    }
