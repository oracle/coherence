/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import com.tangosol.util.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * A {@link ScriptHandler} for a language handles evaluation (or execution) of a
 * script implemented in that language. The {@link ScriptHandler} is also
 * responsible for configuring a Graal {@link Context} in a language specific
 * way. For example, a JavaScript {@link ScriptHandler} may bind a few objects
 * like 'module', 'exports' and 'require' etc. to the {@link Context} before a
 * JavaScript executes.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public interface ScriptHandler
    {
    /**
     * Returns the language that this handler handles.
     *
     * @return the language that this handler handles
     */
    String getLanguage();

    /**
     * Called to configure a {@link Context.Builder}. This
     * allows addition of any language specific options to the {@link
     * Context.Builder}. This builder will be used to
     * create a {@link Context} that will be passed to the
     * other methods in this interface.
     *
     * @param builder  the {@link Context.Builder} to be configured
     */
    void configure(Context.Builder builder);

    /**
     * Called to initialize / populate the {@link Context}. This call can be
     * used to perform any language specific bindings to the specified context.
     * Note that the {@link ScriptHandler}s should only use this callback
     * to initialize the {@link Context}. They shouldn't try to execute any
     * scripts in this method.
     *
     * @param context  the {@link Context} to be initialized
     */
    void initContext(Context context);

    /**
     * Called to indicate that the {@link Context} is fully initialized by all
     * available {@link ScriptHandler}s. {@link ScriptHandler}s can preload
     * any scripts they may want to in this callback.
     *
     * @param context  the {@link Context} to be used by the {@link ScriptHandler}s
     */
    void onReady(Context context);

    /**
     * Execute the specified script with the specified arguments and return
     * the result.
     *
     * @param scriptPath  the path to the script
     *
     * @return the result of evaluating the specified script
     *
     * @throws ScriptException if any exception occurs while evaluating
     *         the specified script
     */
    Value evaluateScript(String scriptPath)
            throws ScriptException;
    }
