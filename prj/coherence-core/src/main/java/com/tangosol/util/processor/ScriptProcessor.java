/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;

import com.tangosol.internal.util.graal.ScriptManager;

import com.tangosol.util.AbstractScript;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryProcessor;

/**
 * ScriptProcessor is an {@link InvocableMap.EntryProcessor} that wraps a script
 * written in one of the languages supported by Graal VM.
 *
 * @param <K> the type of the Map entry key
 * @param <V> the type of the Map entry value
 * @param <R> the type of value returned by the {@link EntryProcessor}
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class ScriptProcessor<K, V, R>
        extends AbstractScript
        implements EntryProcessor<K, V, R>
    {
    // ------ constructors ---------------------------------------------------

    /**
     * Default constructor for deserialization.
     */
    public ScriptProcessor()
        {
        }

    /**
     * Create a {@link ScriptProcessor} that wraps a script written in the
     * specified language and identified by the specified name. The specified
     * args will be passed during execution of the script.
     *
     * @param language the language the script is written. Currently, only
     *                 "js" (for JavaScript) is supported
     * @param name     the name of the {@link EntryProcessor} that needs to
     *                 be executed
     * @param args     the arguments to be passed to the {@link EntryProcessor}
     *
     */
    public ScriptProcessor(String language, String name, Object... args)
        {
        super(language, name, args);
        }

    // ----- EntryProcessor interface ----------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public R process(InvocableMap.Entry<K, V> entry)
        {
        EntryProcessor<K, V, R> ep = ScriptManager.getInstance()
                .execute(m_sLanguage, m_sName, m_aoArgs)
                .as(EntryProcessor.class);

        return ep.process(entry);
        }
    }

