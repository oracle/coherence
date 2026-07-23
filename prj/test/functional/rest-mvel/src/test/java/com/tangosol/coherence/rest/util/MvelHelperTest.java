/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import java.io.Serializable;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Unit test when explicit mvel2 dependency provided for backwards compatibilty testing.
 */
public class MvelHelperTest
    {
    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldEnableMvelOnlyInLegacyMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            JsonMap map = new JsonMap();
            map.put("age", 41);

            assertTrue(MvelHelper.isEnabled());

            Object       ctx     = MvelHelper.getMvelParserContext();
            Serializable expr    = MvelHelper.compileExpression("age", ctx);
            Serializable setExpr = MvelHelper.compileSetExpression("age", ctx);

            assertNotNull(ctx);
            assertEquals(41, MvelHelper.executeExpression(expr, map));

            MvelHelper.executeSetExpression(setExpr, map, 43);
            assertEquals(43, map.get("age"));
            }
        }

    @Test
    public void shouldDisableMvelInDevEvenWhenMvel2IsPresent()
        {
        assertDisabled(CoherenceModeHelper.dev());
        }

    @Test
    public void shouldDisableMvelInProdEvenWhenMvel2IsPresent()
        {
        assertDisabled(CoherenceModeHelper.prod());
        }

    private static void assertDisabled(CoherenceModeHelper.ModeScope scope)
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            JsonMap map = new JsonMap();
            map.put("age", 41);

            assertFalse(MvelHelper.isEnabled());

            // rest-01 Slice C 14.1.1.2206 MVEL POF backport: direct MVEL APIs stay closed in hardened modes even with mvel2 present
            assertUnsupported(() -> MvelHelper.getMvelParserContext());
            assertUnsupported(() -> MvelHelper.compileExpression("age", null));
            assertUnsupported(() -> MvelHelper.executeExpression("age", map));
            assertUnsupported(() -> MvelHelper.compileSetExpression("age", null));
            assertUnsupported(() -> MvelHelper.executeSetExpression(null, map, 43));
            }
        }

    private static void assertUnsupported(Runnable action)
        {
        try
            {
            action.run();
            fail("expected UnsupportedOperationException");
            }
        catch (UnsupportedOperationException expected)
            {
            // expected
            }
        }
    }
