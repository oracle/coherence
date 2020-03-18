/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal.js;

import com.tangosol.internal.util.graal.ScriptDescriptor;

import com.tangosol.util.ScriptException;

import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * Module object wraps a JavaScript file and provides a {@link #load} method
 * that performs the necessary steps to load a JavaScript file.
 *
 * For pre ES6 modules, each module is loaded by first wrapping the JavaScript
 * file in a function and then executing it.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class Module
    {
    // ------ constructor ----------------------------------------------------

    /**
     * Create a new {@link Module} from the specified {@link ScriptDescriptor}.
     *
     * @param descriptor  the {@link ScriptDescriptor} that represents the
     *                   JavaScript to be loaded
     */
    public Module(ScriptDescriptor descriptor)
        {
        m_sName      = descriptor.getScriptUrl().toString();
        m_descriptor = descriptor;
        }

    // ------ Module methods -------------------------------------------------

    /**
     * Returns the name of this {@link Module}. This is typically the url (in
     * String form) of the JavaScript file.
     *
     * @return the name of this {@link Module}
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Get the exported values.
     *
     * @return the {@link Map} of exported objects
     */
    public Map<Object, Object> getExports()
        {
        return mapExports;
        }

    /**
     * Set the values to be exported. This will be called from JavaScript code
     * if the script performs {@code module.exports = foo}.
     *
     * @param exports  the map of objects to be exported
     */
    public void setExports(Map<Object, Object> exports)
        {
        this.mapExports = exports;
        }

    /**
     * Return the {@link Value} returned by this Module when it  was loaded.
     *
     * @return the {@link Value} returned by this Module when it was loaded
     */
    public Value getValue()
        {
        return m_value;
        }

    /**
     * Indicates if this Module was loaded successfully (true) or not.
     *
     * @return {@code true} if this Module was loaded successfully,
     *         {@code false} otherwise
     */
    public boolean isLoaded()
        {
        return m_fLoaded;
        }

    /**
     * Return the {@link ScriptDescriptor} of this Module.
     *
     * @return the {@link ScriptDescriptor} of this Module
     */
    public ScriptDescriptor getDescriptor()
        {
        return m_descriptor;
        }

    /**
     * Loads this Module (if it is not already loaded) using the specified
     * {@link Context}.
     *
     * @param context  the {@link Context} to be used for evaluating the script
     *
     * @throws ScriptException if any error was encountered while
     *         loading this {@link Module}
     */
    public void load(Context context)
            throws ScriptException
        {
        String scriptPath = m_descriptor.getScriptPath();
        if (!m_descriptor.exists())
            {
            throw new ScriptException(scriptPath);
            }
        if (m_descriptor.isDirectory())
            {
            throw new IllegalArgumentException(scriptPath + " is a directory");
            }

        CharSequence wrappedText = m_descriptor.getScriptSource().getCharacters();

        try
            {
            if (m_descriptor.getScriptSource().getName().endsWith(".json"))
                {
                // wrap the content (which is a .json) and return it.
                wrappedText = "module.exports = " + wrappedText;

                Source wrappedSource = Source.newBuilder(
                        "js", wrappedText, m_descriptor.getScriptUrl().toString())
                        .buildLiteral();

                m_value = context.eval(wrappedSource);
                }
            else if (m_descriptor.getScriptSource().getName().endsWith(".mjs"))
                {
                Source src = Source.newBuilder("js", wrappedText, m_descriptor.getScriptUrl().toString()).buildLiteral();
                m_value = context.eval(src);
                }
            else
                {
                // Now wrap the specified module into a JavaScript function so that
                // members declared at the global scope don't leak into the Graal
                // Context.
                wrappedText = String.join(
                        System.getProperty("line.separator"), "",
                        "(function (exports, module, __filename, __dirname) {",
                        wrappedText,
                        "",
                        "return module.exports;",
                        "",
                        "});",
                        "");

                Source wrappedSource = Source.newBuilder(
                        "js", wrappedText, m_descriptor.getScriptUrl().toString()).buildLiteral();

                Value  wrapperFunc  = context.eval(wrappedSource);
                String fileNamePath = m_descriptor.getResourcePath();
                m_value = wrapperFunc.execute(
                        getExports(),
                        this,
                        fileNamePath,
                        m_descriptor.getDirectory());
                }
            m_fLoaded = true;
            }
        catch (PolyglotException polyEx)
            {
            polyEx.printStackTrace();
            throw new ScriptException("Exception while evaluating script", polyEx);
            }
        }

    /**
     * Get the dependent {@link Module}s.
     *
     * @return a {@link Map} where keys are module names and the values are
     *         {@link Module}
     */
    public Map<String, Module> getDependents()
        {
        return m_mapDependents;
        }

    // ------ helpers --------------------------------------------------------

    /*package*/

    /**
     * Add a dependent {@link Module}.
     *
     * @param dependent  the dependent {@link Module}
     */
    void addDependentModule(Module dependent)
        {
        m_mapDependents.put(dependent.getDescriptor().getScriptUrl().toString(), dependent);
        }

    // ------ Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "Module {desciptor= " + m_descriptor + "}";
        }

    // ------ data members ---------------------------------------------------

    /**
     * The name of this {@link Module}. This is typically the url (in String
     * form) of the JavaScript file.
     */
    private final String m_sName;

    /**
     * The {@link Map} that represents the exported objects.
     */
    private Map<Object, Object> mapExports = new HashMap<>();

    /**
     * The {@link ScriptDescriptor} for this JavaScript module.
     */
    private final ScriptDescriptor m_descriptor;

    /**
     * Default export.
     */
    private Value m_value;

    /**
     * {@code true} if this {@link Module} has been loaded successfully.
     */
    private boolean m_fLoaded;

    /**
     * The {@link Map} of module name to the (dependent) {@link Module}.
     */
    private final Map<String, Module> m_mapDependents = new HashMap<>();
    }
