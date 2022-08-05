/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal.js;

import com.tangosol.util.ScriptException;

import com.tangosol.internal.util.graal.ScriptDescriptor;
import com.tangosol.internal.util.graal.ScriptManager;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;

import jakarta.json.stream.JsonParser;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;


/**
 * JavaScriptModuleManager manages loading of JavaScript files. Each JavaScript
 * file is considered a module (just as in Node.js).
 *
 * <p> Each JavaScript file that is loaded is represented by a {@link Module}.
 * This class also maintains a cache of loaded modules.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class JavaScriptModuleManager
    {
    // ----- constructors ----------------------------------------------------

    private JavaScriptModuleManager()
        {
        }

    // ----- JavaScriptModuleManager methods  --------------------------------

    /**
     * Load the specified module. This is the method that is called when a
     * JavaScript code calls the {@code require("module-name")} method.
     *
     * @param sModuleName  the name of the module to load
     *
     * @return the module's exported Object
     */
    public synchronized Object require(String sModuleName)
        {
        Module result;

        if (sModuleName.startsWith("./") || sModuleName.startsWith("/") || sModuleName.startsWith("../"))
            {
            ScriptDescriptor descriptor = getParent().getDescriptor().resolve(sModuleName);
            result = loadAsFile(descriptor);
            if (result == null)
                {
                result = loadAsDirectory(descriptor);
                }
            }
        else
            {
            result = loadAsNodeModule(sModuleName);
            }

        if (result != null)
            {
            f_cachedModules.put(result.getDescriptor().getScriptUrl().toString(), result);
            getParent().addDependentModule(result);

            return result.getExports();
            }

        throw new ScriptException("Cannot load module: " + sModuleName);
        }

    /**
     * Attempt to load the specified Path as a file by trying to load it as
     * 1. specified file,
     * 2. as .js or
     * 3. as .json file in that order.
     *
     * @param desc  the descriptor
     *
     * @return the loaded {@link Module}
     *
     * @throws ScriptException if the script specified in the descriptor
     *                         cannot be found or loaded
     */
    private Module loadAsFile(ScriptDescriptor desc)
            throws ScriptException
        {
        for (String extn : new String[] {"", ".js", ".mjs", ".json"})
            {
            String           resourceName = desc.getSimpleName() + extn;
            ScriptDescriptor descriptor   = desc.resolve(resourceName);

            if (descriptor.exists() && !descriptor.isDirectory())
                {
                return createAndLoadModule(descriptor);
                }
            }
        return null;
        }

    /**
     * Attempt to load this module (file) by treating the specified name as
     * directory. Then, within that directory, try to load the package.json
     * whose main field (if any) will tell the file name to use.
     *
     * If no package.json or main field doesn't exist, then try to
     * load the index file
     *
     * @param module  the {@link Module}
     *
     * @return the loaded {@link Module} or {@code null}
     */
    private Module loadAsDirectory(ScriptDescriptor module)
        {
        ScriptDescriptor descriptor = module.resolve("package.json");
        if (descriptor.exists() && !descriptor.isDirectory())
            {
            String mainField = getMainFieldFromJSON(descriptor.getScriptUrl());
            if (mainField != null)
                {
                ScriptDescriptor mainFieldDesc = module.resolve(mainField);
                Module           m             = loadAsFile(mainFieldDesc);
                if (m != null)
                    {
                    return m;
                    }

                // If no package.json or main field doesn't exist, then try to
                // load the index file.
                m = loadIndex(module);
                if (m != null)
                    {
                    return m;
                    }
                }
            }

        return loadIndex(module);
        }

    /**
     * Load the specified module as a NodeJS module.
     *
     * @param sModuleName  the module name
     *
     * @return the loaded {@link Module} or {@code null}
     */
    private Module loadAsNodeModule(String sModuleName)
        {
        Module result = null;

        for (ScriptDescriptor nodeModulesDir : toNodeModulesPaths(getParent().getDescriptor()))
            {
            ScriptDescriptor desc = nodeModulesDir.resolve(sModuleName);

            result = loadAsFile(desc);
            if (result == null)
                {
                result = loadAsDirectory(desc);
                }
            if (result != null)
                {
                break;
                }
            }

        return result;
        }

    /**
     * Load index.js or index.json. Use the specified module as a directory
     * and try to load index.js in that directory. If not found then try to
     * load index.json in that directory.
     *
     * @param module  the {@link Module}
     *
     * @return the loaded {@link Module} or {@code null}
     *
     * @throws ScriptException if index.js or index.json file exists but
     *                         couldn't be loaded
     */
    private Module loadIndex(ScriptDescriptor module)
        {
        for (String indexFileName : new String[] {"index.js", "index.json"})
            {
            ScriptDescriptor descriptor = module.resolve(indexFileName);
            if (descriptor.exists() && !descriptor.isDirectory())
                {
                return createAndLoadModule(descriptor);
                }
            }
        return null;
        }

    /**
     * Parse the 'package.json' file (whose URL is specified in the parameter.
     *
     * @param pkgJsonUrl  the {@link URL} of the package.json file
     *
     * @return the value of the {@code main} if it exists in the json file
     *         or {@code null}
     */
    public String getMainFieldFromJSON(URL pkgJsonUrl)
        {
        String  mainFieldValue = null;
        int     level          = 0;
        boolean inMainField    = false;

            try (InputStream is = pkgJsonUrl.openStream();
                 JsonParser parser = Json.createParser(is))
                {
                while (parser.hasNext())
                    {
                    final JsonParser.Event event = parser.next();
                    switch (event)
                        {
                        case START_OBJECT:
                            level++;
                            break;
                        case END_OBJECT:
                            level--;
                            break;
                        case KEY_NAME:
                            inMainField = "main".equals(parser.getString());
                            break;
                        case VALUE_STRING:
                            String string = parser.getString();
                            if (level == 1 && inMainField)
                                {
                                mainFieldValue = string;
                                }
                            break;
                        default:
                            if (inMainField)
                                {
                                throw new IllegalStateException("main field value specified but it is not a string");
                                }
                        }
                    }
                }
        catch (IOException ioEx)
            {
            throw new ScriptException("Error while parsing: " + pkgJsonUrl);
            }

        return mainFieldValue;
        }

    /**
     * Get the parent Module.
     *
     * @return the parent {@link Module}
     */
    private Module getParent()
        {
        return f_partialModules.peekLast();
        }

    /**
     * Create and load the {@link Module} for the
     * specified {@link ScriptDescriptor}.
     *
     * @param descriptor  the descriptor for which a {@link Module} needs to
     *                    be created
     *
     * @return {@link Module} for the specified descriptor
     *
     * @throws ScriptException if the script specified in the descriptor
     *         is not found
     */
    /*package*/
    Module createAndLoadModule(ScriptDescriptor descriptor)
            throws ScriptException
        {
        if (!descriptor.exists())
            {
            throw new IllegalArgumentException("specified script does not exist. Descriptor: " + descriptor);
            }
        if (descriptor.isDirectory())
            {
            throw new IllegalArgumentException("script path must point to a file. Descriptor: " + descriptor);
            }

        String cachingKey = descriptor.getScriptUrl().toString();
        Module module = f_cachedModules.get(cachingKey);

        if (module != null)
            {
            return module;
            }

        Context context = ScriptManager.getInstance().getContext();
        try
            {
            module = new Module(descriptor);

            // This allows cycles in `require()`
            f_cachedModules.put(cachingKey, module);

            f_partialModules.addLast(module);
            setupContextForModule(context, module);
            module.load(context);
            getParent().addDependentModule(module);
            }
        finally
            {
            Module prevModule = f_partialModules.pollLast();
            setupContextForModule(context, prevModule);
            }

        return module;
        }

    /**
     * Get all the node_modules directory walking up the tree from the specified
     * directory the root of the tree.
     *
     * @param base  the base {@link ScriptDescriptor}
     *
     * @return an array of {@link ScriptDescriptor} one for each node modules
     *         directory
     */
    public List<ScriptDescriptor> toNodeModulesPaths(ScriptDescriptor base)
        {
        ArrayList<ScriptDescriptor> dirs = new ArrayList<>();
        for(ScriptDescriptor current = base.getParentDescriptor(); current != null; current = current.getParentDescriptor())
            {
            if (!current.getSimpleName().equals("node_modules"))
                {
                dirs.add(current.resolve("node_modules/"));
                }
            }

        return dirs;
        }

    /**
     * Return a {@link Map} cached modules where the keys are url of the module
     * and values are the {@link Module} objects.
     *
     * @return a {@link Map} cached modules where the keys are url of the module
     *         and values are the {@link Module} objects
     */
    public HashMap<String, Module> getCachedModules()
        {
        return f_cachedModules;
        }

    // ----- JavaScriptModuleManager class methods----------------------------

    /**
     * Return the singleton {@link JavaScriptModuleManager}.
     *
     * @return the singleton {@link JavaScriptModuleManager}
     */
    public static JavaScriptModuleManager getInstance()
        {
        return INSTANCE;
        }

    // ----- helpers ---------------------------------------------------------

    /**
     * Set up the {@link Context} with {@link Module} specific bindings.
     *
     * @param context the {@link Context} to setup
     * @param module  the {@link Module} object
     */
    private void setupContextForModule(Context context, Module module)
        {
        Value bindings = context.getBindings("js");
        if (module != null)
            {
            bindings.putMember("module", module);
            bindings.putMember("exports", module.getExports());
            bindings.putMember("__dirname", module.getDescriptor().getDirectory());
            bindings.putMember("__filename", module.getDescriptor().getScriptPath());
            bindings.putMember("require", JavaScriptHandler.getRequireFunction());
            }
        else
            {
            bindings.removeMember("module");
            bindings.removeMember("exports");
            bindings.removeMember("__filename");
            bindings.removeMember("__dirname");
            bindings.removeMember("require");
            }
        }

    // ----- data members ----------------------------------------------------

    /**
     * The singleton instance.
     */
    private static final JavaScriptModuleManager INSTANCE = new JavaScriptModuleManager();

    /**
     * The chain of {@link Module}s being loaded. Implemented using a {@link
     * Deque} with the last element being the top of the stack. The last element
     * is the {@link Module} that is currently being loaded.
     */
    private final ArrayDeque<Module> f_partialModules = new ArrayDeque<>();

    /**
     * Map of script url to loaded modules.
     */
    private final HashMap<String, Module> f_cachedModules = new HashMap<>();
    }
