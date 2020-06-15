/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import com.tangosol.internal.util.graal.js.JavaScriptModuleManager;
import com.tangosol.internal.util.graal.js.Module;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModuleTest
    {
    @Test
    public void testModuleLoading()
        {
        Path scriptPath = Paths.get("util.mjs");

        ScriptDescriptor parentDesc = new ScriptDescriptor(
                "js",
                scriptPath.toString());

        assertTrue(parentDesc.exists());

        Module module = new Module(parentDesc);
        assertFalse(module.isLoaded());

        ScriptManager.getInstance()
                .getHandler("js")
                .evaluateScript(scriptPath.toString());

        Map<String, Module> cachedModules = JavaScriptModuleManager.getInstance().getCachedModules();
        assertTrue(cachedModules.size() == 4);

        ScriptManager.getInstance().getContext().getBindings("js").getMemberKeys().containsAll(
                Arrays.asList("LowerCaseProcessor", "UpperCaseProcessor", "ES6UpperCaseProcessor", "CaseConverter", "Util"));

        Module m = cachedModules.get(parentDesc.getScriptUrl().toString());
        assertNotNull(m);
        }
    }
