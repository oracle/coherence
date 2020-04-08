/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal.js;

import com.tangosol.internal.util.graal.ScriptDescriptor;
import com.tangosol.internal.util.graal.ScriptHandler;

import com.tangosol.util.ScriptException;

import java.util.function.Function;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * Implementation of {@link ScriptHandler} that is specific to JavaScript
 * language. This class handles registration of the 'require' function in
 * the {@link Context}.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class JavaScriptHandler
        implements ScriptHandler
    {
    // ----- JavaScriptHandler methods ----------------------------------

    @Override
    public String getLanguage()
        {
        return "js";
        }

    @Override
    public void configure(Context.Builder builder)
        {
        // This allows JavaScript code to access getters and setters using
        // simple property names.
        builder.option("js.nashorn-compat", "true");
        }

    @Override
    public void initContext(Context context)
        {
        // Register 'require' function
        context.getBindings("js").putMember("require", getRequireFunction());
        }

    @Override
    public void onReady(Context context)
        {
        try
            {
            // Remember all resources are resolved w.r.t. /scripts/js
            ScriptDescriptor desc = new ScriptDescriptor("js", "/");
            for (String script : desc.listScripts())
                {
                Value global = evaluateScript(script);
                if (global != null)
                    {
                    for (String key : global.getMemberKeys())
                        {
                        context.getBindings("js").putMember(key, global.getMember(key));
                        }
                    }
                }
            }
        catch (Throwable e)
            {
            throw new ScriptException("error during script preloading", e);
            }
        }

    @Override
    public Value evaluateScript(String script)
            throws ScriptException
        {
        // In JavaScript each script is a Module. The module loading is a
        // non trivial task and hence delegated to JavaScriptModuleManager.
        return JavaScriptModuleManager.getInstance().createAndLoadModule(
                new ScriptDescriptor("js", script)).getValue();
        }

    // ----- helpers ---------------------------------------------------------

    /**
     * Returns a {@link Function} handles the "require()" calls made from
     * JavaScript modules.
     *
     * @return a {@link Function} handles the "require()" calls made from
     *         JavaScript modules
     */
    /*package*/
    static Function<String, Object> getRequireFunction()
        {
        return moduleName -> JavaScriptModuleManager.getInstance().require(moduleName);
        }
    }

