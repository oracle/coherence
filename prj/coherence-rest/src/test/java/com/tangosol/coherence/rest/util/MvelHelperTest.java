/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


public class MvelHelperTest
    {
    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldRemainDisabledWithoutMvel2InCompatibility()
        {
        assertDisabled(CoherenceModeHelper.securityCompatibility());
        }

    @Test
    public void shouldRemainDisabledWithoutMvel2InHardened()
        {
        assertDisabled(CoherenceModeHelper.securityHardened());
        }

    private static void assertDisabled(CoherenceModeHelper.ModeScope scope)
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            JsonMap map = new JsonMap();
            map.put("age", 41);

            assertFalse(MvelHelper.isEnabled());

            // direct MVEL APIs stay closed unless both compatibility mode and mvel2 are present
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
