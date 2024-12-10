/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import java.util.concurrent.Callable;
import java.util.function.Function;

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

    @SuppressWarnings("unchecked")
    @Test
    public void testScriptExecution() throws Exception
        {
        Function<String, String> echo = ScriptManager.getInstance()
                .execute("js", "Echo")
                .as(Function.class);

        assertEquals("echoooo", echo.apply("echoooo"));

        Callable<String> ping = ScriptManager.getInstance()
                .execute("js", "Ping")
                .as(Callable.class);

        assertEquals("pong", ping.call());
        }
    
    @Test(expected = IllegalArgumentException.class)
    public void testUnSupportedLanguages()
        {
        ScriptManager.getInstance().getHandler("_unknown_");
        }

    }
