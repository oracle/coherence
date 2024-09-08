/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import org.graalvm.polyglot.Context;

/**
 * A {@link ScriptHandler} for a language handles evaluation (or execution) of a
 * script implemented in that language. The {@link ScriptHandler} is also
 * responsible for configuring a Graal {@link Context} in a language specific
 * way.
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
     * Called to initialize the {@link Context}.
     * <p>
     * This is where language-specific scripts should be loaded, evaluated, and
     * the results bound to the context.
     *
     * @param context  the {@link Context} to be initialized
     */
    void init(Context context);
    }
