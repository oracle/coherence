/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import com.tangosol.util.ScriptException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;

/**
 * ScriptManager handles execution of a scripts. The actual execution of each
 * script is handled by a language specific {@link ScriptHandler}. This class
 * also provides a {@link Context} that is used to evaluate all scripts in all
 * supported languages.
 *
 * ScriptManager maintains a singleton Graal {@link Context} that is configured
 * by calling {@link ScriptHandler#configure(Context.Builder)} on each of the
 * registered handlers and then initialized by calling
 * {@link ScriptHandler#init(Context)} on each of the registered handlers.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class ScriptManager
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Create the (singleton) instance of {@link ScriptManager}.
     */
    private ScriptManager()
        {
        // Discover ScriptHandlers through ServiceLoader.
        for (ScriptHandler handler : ServiceLoader.load(ScriptHandler.class))
            {
            f_handlers.put(handler.getLanguage(), handler);
            }

        // Create the Context builder which can further be configured
        // by the ScriptHandlers.
        Context.Builder builder = Context.newBuilder()
                .allowAllAccess(true)
                .allowHostAccess(HostAccess.ALL)
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .allowExperimentalOptions(true);

        // Pass the {@code Context.Builder} so that each handler
        // can configure it (by adding Options)
        for (ScriptHandler handler : f_handlers.values())
            {
            handler.configure(builder);
            }

        f_context = builder.build();

        // Now, pass the {@code Context} to each handler so that
        // they have a chance to initialize it (by adding bindings)
        for (ScriptHandler handler : f_handlers.values())
            {
            handler.init(f_context);
            }
        }

    // ----- ScriptManager methods  ------------------------------------------

    /**
     * Return the Graal {@link Context} that will be used to evaluate scripts.
     *
     * @return The Graal {@link Context}
     */
    public Context getContext()
        {
        return f_context;
        }

    /**
     * Return a {@link Collection} of supported languages.
     *
     * @return a {@link Collection} of supported languages
     */
    public Collection<String> getSupportedLanguages()
        {
        return f_handlers.values().stream()
                .map(ScriptHandler::getLanguage)
                .collect(Collectors.toCollection(HashSet::new));
        }

    /**
     * Lookup the object that is implemented in the specified {@code language}
     * that is bound to the {@link Context} using the specified name and execute
     * it using the specified {@code args}.
     *
     * If the result of the evaluation is a {@code Value} can be instantiated
     * it is instantiated and the resulting instance is returned. For example,
     * if the result of the evaluation of the script returns a {@code Class}
     * then the {@code Class} is instantiated with the specified {@code args}
     * and the resulting instance is returned.
     *
     * Else, if the result of the evaluation is a {@code Value} can be executed
     * it is executed and the result is returned. For example, if the result
     * of the evaluation of the script returns a {@code function}, then the
     * {@code function} is invoke with the specified {@code args} and the
     * resulting instance is returned.
     *
     * Else, the result (of the script evaluation) is returned.
     *
     * @param sLanguage  the language in which the script is implemented
     * @param sName      the name of the object
     * @param aoArgs     the args to use while instantiating the value
     *
     * @return the result {@link Value} from evaluating the specified script
     *
     * @throws ScriptException if any error occurs during script execution
     */
    public Value execute(String sLanguage, String sName, Object... aoArgs)
        {
        try
            {
            Value value = f_context.getBindings(sLanguage).getMember(sName);

            if (value == null)
                {
                throw new ScriptException("Value '" + sName + "' is not bound to the context");
                }

            if (value.canInstantiate())
                {
                value = value.newInstance(aoArgs);
                }
            else if (value.canExecute())
                {
                value = value.execute(aoArgs);
                }

            return value;
            }
        catch (Throwable e)
            {
            throw new ScriptException("error ", e);
            }
        }

    /**
     * Return the {@link ScriptHandler} for the specified language.
     *
     * @param sLanguage  the language of the {@link ScriptHandler}
     *
     * @return the {@link ScriptHandler} for the specified language
     *
     * @throws IllegalArgumentException if the specified language is not supported
     */
    public ScriptHandler getHandler(String sLanguage)
        {
        ScriptHandler handler = f_handlers.get(sLanguage);
        if (handler != null)
            {
            return handler;
            }

        throw new IllegalArgumentException("Unknown language: " + sLanguage);
        }

    // ----- ScriptManager ---------------------------------------------------

    /**
     * Return the singleton instance.
     *
     * @return The singleton {@link ScriptManager}
     */
    public static ScriptManager getInstance()
        {
        return INSTANCE;
        }

    // ----- data members ----------------------------------------------------

    /**
     * The singleton instance.
     */
    private static final ScriptManager INSTANCE = new ScriptManager();

    /**
     * The Graal {@link Context} to be used for script evaluation.
     */
    private final Context f_context;

    /**
     * A {@link Map} of language name to {@link ScriptHandler}.
     */
    private final ConcurrentMap<String, ScriptHandler> f_handlers = new ConcurrentHashMap<>();
    }
