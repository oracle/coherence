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
 * @since 26.04
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
            // rest-01 prompt 04 design/features/security-bugs/plans/rest-01/prompts/04-slice-c-query-expression-implementation.md:
            // accepted syntax must parse, validate, and build through the single strict REST direct-query path
            assertStrictQueryBuilds("age == 36");
            assertStrictQueryBuilds("age == 36 && name == \"Ivan\"");
            assertStrictQueryBuilds("age == 36 || name == \"Ivan\"");
            assertStrictQueryBuilds("!(age == 36)");
            assertStrictQueryBuilds("age > -1");
            assertStrictQueryBuilds("age in (36, 39)");
            assertStrictQueryBuilds("name == :name", null, Collections.<String, Object>singletonMap("name", "Ivan"));
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
            // rest-01 prompt 04 design/features/security-bugs/plans/rest-01/prompts/04-slice-c-query-expression-implementation.md:
            // rejection here proves unsupported direct-query syntax cannot pass validation and reach execution
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
            // rest-01 prompt 04 design/features/security-bugs/plans/rest-01/prompts/04-slice-c-query-expression-implementation.md:
            // builder-level rejection is the second line of defense if validation coverage drifts
            }
        }

    private static final CoherenceQueryLanguage LANGUAGE = new CoherenceQueryLanguage();
    }
