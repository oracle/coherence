/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import com.tangosol.internal.util.graal.ScriptManager;

import com.tangosol.util.AbstractScript;
import com.tangosol.util.Filter;

/**
 * ScriptFilter is a {@link Filter} that wraps a script written in one of the
 * languages supported by Graal VM.
 *
 * @param <V> the type of the value this Filter receives
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class ScriptFilter<V>
        extends AbstractScript
        implements Filter<V>
    {
    // ------ constructors ---------------------------------------------------

    /**
     * Default constructor for deserialization.
     */
    public ScriptFilter()
        {
        }

    /**
     * Create a {@link Filter} that wraps the specified script.
     *
     * @param language the language the script is written. Currently, only
     *                 {@code "js"} (for JavaScript) is supported
     * @param name     the name of the {@link Filter} that needs to
     *                 be evaluated
     * @param args     the arguments to be passed to the script during
     *                 evaluation
     */
    public ScriptFilter(String language, String name, Object... args)
        {
        super(language, name, args);
        }

    // ----- Filter interface ------------------------------------------------

    @SuppressWarnings("unchecked")
    public boolean evaluate(V entry)
        {
        return ScriptManager.getInstance()
                            .execute(m_sLanguage, m_sName, m_aoArgs)
                            .as(Filter.class).evaluate(entry);
        }

    }
