/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termtrees.Term;

import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests for {@link RestQueryPolicy}.
 *
 * @author Vaso Putica  2026.05.09
 * @since 26.07
 */
public class RestQueryPolicyTest
    {
    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldBuildAcceptedStrictDirectQueries()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertStrictQueryBuilds("age == 36");
            assertStrictQueryBuilds("age == 36 && name == \"Ivan\"");
            assertStrictQueryBuilds("age == 36 || name == \"Ivan\"");
            assertStrictQueryBuilds("!(age == 36)");
            assertStrictQueryBuilds("age > -1");
            assertStrictQueryBuilds("age in (36, 39)");
            assertStrictQueryBuilds("name == :name", null, Map.of("name", "Ivan"));
            assertStrictQueryBuilds("name == ?1", new Object[] {"Ivan"}, null);
            assertStrictQueryBuilds("key() == 1");
            assertStrictQueryBuilds("value() == 1");
            }
        }

    @Test
    public void shouldRejectUnsafeStrictDirectQueries()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertStrictQueryRejected("new Person() == \"Ivan\"");
            assertStrictQueryRejected("address.city == \"Tampa\"");
            assertStrictQueryRejected("name.length() == 4");
            assertStrictQueryRejected("~age == 36");
            assertStrictQueryRejected("age + 1 == 37");
            assertStrictQueryRejected("key(1) == 1");
            assertStrictQueryRejected("value(1) == 1");
            assertStrictQueryRejected("person == :person;data.pof.Person");
            assertStrictQueryRejected("age == :age ;i");
            assertStrictQueryRejected("age == :age; i");
            }
        }

    @Test
    public void shouldRejectUnsafeTermsInBuilderDefense()
        {
        assertStrictBuilderRejects("new Person() == \"Ivan\"");
        assertStrictBuilderRejects("address.city == \"Tampa\"");
        assertStrictBuilderRejects("name.length() == 4");
        assertStrictBuilderRejects("~age == 36");
        assertStrictBuilderRejects("age + 1 == 37");
        }

    private void assertStrictQueryBuilds(String sQuery)
        {
        assertStrictQueryBuilds(sQuery, null, null);
        }

    private void assertStrictQueryBuilds(String sQuery, Object[] aBindings, Map<String, Object> mapBindings)
        {
        assertNotNull(RestQueryPolicy.createDirectQueryFilter(sQuery, aBindings, mapBindings, LANGUAGE));
        }

    private void assertStrictQueryRejected(String sQuery)
        {
        try
            {
            RestQueryPolicy.createDirectQueryFilter(sQuery, null, Collections.emptyMap(), LANGUAGE);
            fail("expected strict REST direct-query rejection for " + sQuery);
            }
        catch (RuntimeException expected)
            {
            }
        }

    private void assertStrictBuilderRejects(String sQuery)
        {
        Term term = new OPParser(sQuery, LANGUAGE.filtersTokenTable(), LANGUAGE.getOperators()).parse();
        try
            {
            RestQueryPolicy.buildDirectQueryFilter(term, new Object[0], Collections.emptyMap(), LANGUAGE);
            fail("expected strict REST direct-query builder rejection for " + sQuery);
            }
        catch (IllegalArgumentException expected)
            {
            }
        }

    private static final CoherenceQueryLanguage LANGUAGE = new CoherenceQueryLanguage();
    }
