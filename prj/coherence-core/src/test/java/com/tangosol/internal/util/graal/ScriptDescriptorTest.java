/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import com.tangosol.internal.util.graal.js.JavaScriptModuleManager;
import com.tangosol.internal.util.graal.js.Module;

import com.tangosol.util.InvocableMap;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Map;

import org.graalvm.polyglot.Value;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ScriptDescriptorTest
    {
    @Test
    public void testEvalOfSub2HelloJS()
        {
        String scriptPath = "sub1/sub2/util.js";
        ScriptDescriptor descriptor = new ScriptDescriptor("js", scriptPath);

        assertNotNull(descriptor);

        assertTrue(descriptor.exists());
        assertFalse(descriptor.isDirectory());
        assertEquals(scriptPath, descriptor.getScriptPath());

        Value value = ScriptManager.getInstance().evaluateScript("js", scriptPath);
        assertNotNull(value);
        }

    @Test
    public void testModuleLoading()
        {
        Path scriptPath = Paths.get("util.mjs");

        ScriptDescriptor desc = new ScriptDescriptor("js", scriptPath.toString());

        assertTrue(desc.exists());

        Module module = new Module(desc);
        assertFalse(module.isLoaded());

        ScriptManager.getInstance()
                .getHandler("js")
                .evaluateScript(scriptPath.toString());

        Map<String, Module> cachedModules = JavaScriptModuleManager.getInstance().getCachedModules();
        assertTrue(cachedModules.size() == 4);

        ScriptManager.getInstance().getContext().getBindings("js").getMemberKeys().containsAll(
                Arrays.asList("LowerCaseProcessor", "UpperCaseProcessor", "ES6UpperCaseProcessor", "CaseConverter", "Util"));

        Module m = cachedModules.get(desc.getScriptUrl().toString());
        assertNotNull(m);
        }

    @Test
    public void testEvalOfUtilMjs()
        {
        String scriptPath = "util.mjs";
        ScriptDescriptor descriptor = new ScriptDescriptor(
                "js", scriptPath);

        assertNotNull(descriptor);

        assertTrue(descriptor.exists());
        assertFalse(descriptor.isDirectory());
        assertEquals(scriptPath, descriptor.getScriptPath());

        Value value = ScriptManager.getInstance().evaluateScript("js", scriptPath);

        InvocableMap.EntryProcessor proc = value.getMember("UpperCaseProcessor")
                .newInstance()
                .as(InvocableMap.EntryProcessor.class);
        assertNotNull(proc);
        }

    @Test
    public void testEvalOfSub2ConfigJson()
        {
        String scriptPath = "sub1/sub2/config.json";
        ScriptDescriptor descriptor = new ScriptDescriptor("js", scriptPath);

        assertNotNull(descriptor);

        assertTrue(descriptor.exists());
        assertFalse(descriptor.isDirectory());
        assertEquals(scriptPath, descriptor.getScriptPath());

        Value value = ScriptManager.getInstance().evaluateScript("js", scriptPath);
        assertTrue(value.hasMember("name"));
        assertEquals(value.getMember("name").asString(), "config.json");
        }
    }
