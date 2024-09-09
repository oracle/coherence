/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal.js;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.graal.AbstractScriptHandler;
import com.tangosol.internal.util.graal.ScriptHandler;

import org.graalvm.polyglot.Context;

/**
 * Implementation of a {@link ScriptHandler} for JavaScript.
 *
 * @author mk  2019.07.26
 * @author er  2024.09.06
 * @author as  2024.09.06
 *
 * @since 14.1.1.0
 */
public class JavaScriptHandler
        extends AbstractScriptHandler
    {
    // ----- ScriptHandler methods ------------------------------------------

    @Override
    public String getLanguage()
        {
        return "js";
        }

    @Override
    public void configure(Context.Builder builder)
        {
        builder.option("js.nashorn-compat", "true")  // allows JavaScript code to access getters and setters using simple property names
               .option("js.ecmascript-version", Config.getProperty("coherence.js.ecmascript.version", "2023"))
               .option("js.esm-eval-returns-exports", "true");
        }

    protected String getScriptExtension()
        {
        return "mjs";
        }

    protected String getScriptRoot()
        {
        return "scripts/js";
        }
    }

