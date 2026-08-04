/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.rest.config.ExpressionAliasConfig;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for {@link RestExpressionPolicy}.
 *
 * @author Vaso Putica  2026.05.08
 * @since 26.07
 */
public class RestExpressionPolicyTest
    {
    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldResolveSortAliasesInDev()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            ExpressionAliasConfig aliases = createAliases();

            assertEquals("name", RestExpressionPolicy.resolveSort(aliases, "by-name"));
            assertEquals("name:asc", RestExpressionPolicy.resolveSort(aliases, "by-name:asc"));
            assertEquals("name:desc", RestExpressionPolicy.resolveSort(aliases, "by-name:desc"));
            assertEquals("name:desc", RestExpressionPolicy.resolveSort(aliases, "by-name:DESC"));
            assertEquals("name:asc,age:desc", RestExpressionPolicy.resolveSort(aliases, "by-name:asc, by-age:desc"));
            }
        }

    @Test
    public void shouldRejectBadSortAliasGrammarInDev()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            ExpressionAliasConfig aliases = createAliases();

            for (String sSort : Arrays.asList(
                    "by-name:",
                    ":asc",
                    "foo:bar:baz",
                    "by-name,",
                    "by-name,,by-age",
                    "by-name,   ,by-age",
                    "by-name:sideways",
                    "   "))
                {
                assertSortRejected(aliases, sSort);
                }
            }
        }

    @Test
    public void shouldPreserveCompatibilitySortPassthrough()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            String sSort = "foo:bar:baz,";

            assertEquals(sSort, RestExpressionPolicy.resolveSort(createAliases(), sSort));
            assertEquals("   ", RestExpressionPolicy.resolveSort(createAliases(), "   "));
            }
        }

    private void assertSortRejected(ExpressionAliasConfig aliases, String sSort)
        {
        try
            {
            RestExpressionPolicy.resolveSort(aliases, sSort);
            fail("expected sort alias rejection for [" + sSort + ']');
            }
        catch (IllegalArgumentException expected)
            {
            }
        }

    private ExpressionAliasConfig createAliases()
        {
        return ExpressionAliasConfig.builder()
                .addSortAlias("by-name", "name")
                .addSortAlias("by-age", "age")
                .build();
        }
    }
