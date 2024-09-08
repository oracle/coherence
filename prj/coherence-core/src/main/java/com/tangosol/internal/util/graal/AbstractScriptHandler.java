/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.ScriptException;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * Abstract base class for language-specific script handlers.
 * <p>
 * Implements script loading as a template method, allowing language-specific
 * implementations to simply override trivial abstract methods defined by this class.
 *
 * @author er  2024.09.06
 * @author as  2024.09.06
 *
 * @since 24.09
 */
public abstract class AbstractScriptHandler
        implements ScriptHandler
    {
    @Override
    public void init(Context context)
        {
        String sCurrentScript = null;

        // load all scripts for this language and bind exported values to the context
        try (ScanResult scanResult = new ClassGraph().acceptPathsNonRecursive(getScriptRoot()).scan())
            {
            ResourceList scripts = scanResult.getResourcesWithExtension(getScriptExtension());
            for (Resource script : scripts)
                {
                sCurrentScript = script.getPathRelativeToClasspathElement();
                Logger.config("Loading script %s".formatted(sCurrentScript));
                Source source = Source.newBuilder(getLanguage(), script.getContentAsString(), sCurrentScript).build();
                Value exports = context.eval(source);
                if (exports != null)
                    {
                    for (String key : exports.getMemberKeys())
                        {
                        context.getBindings(getLanguage()).putMember(key, exports.getMember(key));
                        }
                    }
                }
            }
        catch (Throwable e)
            {
            throw new ScriptException("Failed to load script " + sCurrentScript, e);
            }
        }

    protected abstract String getScriptExtension();

    protected abstract String getScriptRoot();
    }
