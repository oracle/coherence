/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ScriptManagerTest
    {
    @Test
    public void shouldBeAbleToInstantiate()
        {
        assertNotNull(ScriptManager.getInstance());
        }

    @Test
    public void shouldBeAbleToGetContext()
        {
        assertNotNull(ScriptManager.getInstance().getContext());
        }

    @Test
    public void testSupportedLanguages()
        {
        ScriptManager scriptManager = ScriptManager.getInstance();

        assertEquals(scriptManager.getSupportedLanguages().size(), 1);
        assertTrue(scriptManager.getSupportedLanguages().contains("js"));
        }

    @Test(expected = IllegalArgumentException.class)
    public void testUnSupportedLanguages()
        {
        ScriptManager.getInstance().getHandler("_unknown_");
        }

    }
