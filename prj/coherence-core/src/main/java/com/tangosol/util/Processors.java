/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.processor.ScriptProcessor;

/**
 * Simple {@link EntryProcessor} DSL.
 * <p>
 * Contains factory methods and entry processor classes that are used to
 * implement functionality exposed via different variants of {@link NamedCache}
 * API.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class Processors
    {
    /**
     * Instantiate an EntryProcessor that is implemented in a script using
     * the specified language.
     *
     * @param sLanguage  the string specifying one of the supported languages
     * @param sName      the name of the {@link EntryProcessor} that needs to
     *                   be evaluated
     * @param aoArgs     the arguments to be passed to the {@link EntryProcessor}
     * @param <K>        the type of key that the {@link EntryProcessor}
     *                   will receive
     * @param <V>        the type of value that the {@link EntryProcessor}
     *                   will receive
     * @param <R>        the type of result that the {@link EntryProcessor}
     *                   will return
     *
     * @return An instance of {@link EntryProcessor}
     *
     * @throws ScriptException          if the {@code script} cannot be loaded or
     *                                  any errors occur during its execution
     * @throws IllegalArgumentException if the specified language is not supported
     */
    public static <K, V, R> EntryProcessor<K, V, R> script(String sLanguage, String sName, Object... aoArgs)
        {
        return new ScriptProcessor<>(sLanguage, sName, aoArgs);
        }
    }
