/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SimpleParameterList;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termtrees.TermWalker;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.util.extractor.ReflectionExtractor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jk  2013.12.06
 */
public class ConstructorQueryWalkerTest
    {
    @Test
    public void shouldProcessFullyQualifiedNoArgsConstructor()
            throws Exception
        {
        String                 query  = "java.util.Date()";
        ConstructorQueryWalker walker = new ConstructorQueryWalker(null, null, m_language);

        walkQuery(walker, query);

        Object expected = new Object[] {"java", "util", new ReflectionExtractor("Date")};

        assertThat(walker.getResult(), is(expected));
        }

    @Test
    public void shouldProcessFullyQualifiedConstructorWithArg()
            throws Exception
        {
        String                 query  = "java.util.Date(1000L)";
        ConstructorQueryWalker walker = new ConstructorQueryWalker(null, null, m_language);

        walkQuery(walker, query);

        Object expected = new Object[] {"java", "util", new ReflectionExtractor("Date", new Object[] {1000L})};

        assertThat(walker.getResult(), is(expected));
        }

    @Test
    public void shouldProcessFullyQualifiedConstructorWithArgFromNumberedBinding()
            throws Exception
        {
        List                   indexedVars = Arrays.asList(1000L, 2000L, 3000L);
        String                 query       = "java.util.Date(?2)";
        ConstructorQueryWalker walker      = new ConstructorQueryWalker(indexedVars, null, m_language);

        walkQuery(walker, query);

        Object expected = new Object[] {"java", "util", new ReflectionExtractor("Date", new Object[] {2000L})};

        assertThat(walker.getResult(), is(expected));
        }

    @Test
    public void shouldProcessFullyQualifiedConstructorWithArgFromNamedBinding()
            throws Exception
        {
        List          indexedVars = new ArrayList();
        ParameterList params      = new SimpleParameterList(new Parameter("A", 1000L), new Parameter("B", 2000L),
                                        new Parameter("C", 3000L));
        ParameterResolver      namedVars = new ResolvableParameterList(new SimpleParameterList(params));
        String                 query     = "java.util.Date(:B)";
        ConstructorQueryWalker walker    = new ConstructorQueryWalker(indexedVars, namedVars, m_language);

        walkQuery(walker, query);

        Object expected = new Object[] {"java", "util", new ReflectionExtractor("Date", new Object[] {2000L})};

        assertThat(walker.getResult(), is(expected));
        }

    private void walkQuery(TermWalker walker, String query)
        {
        new OPParser(query, m_language.filtersTokenTable(), m_language.getOperators()).parse().accept(walker);
        }

    private CoherenceQueryLanguage m_language = new CoherenceQueryLanguage();
    }
