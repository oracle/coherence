/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Base;

import java.io.Serializable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.WeakHashMap;


/**
 * Enable Mvel to be optional by only accessing module via reflection with implementation provided on classpath.
 * {@code org.mvel.mvel2} module is removed by default for security reasons, but one can override and
 * have backwards compatible support by adding the mvel module to the path.
 *
 * @author jf  2022/01/19
 *
 * @since 12.2.1.3.18
 */
public class MvelHelper
    {
    /**
     * Return true if optional {@code org.mvel.mvel2} module is loaded from path.
     * Due to security concerns, Mvel is removed from default implementation and
     * only optionally available for backwards compatibility mode.
     *
     * @return true if mvel implementation is available
     */
    public static boolean isEnabled()
        {
        return s_fEnabled;
        }

    /**
     * Return an instance of ParserContext to be used for MVEL compilation.
     *
     * @return an instance of ParserContext to be used for MVEL compilation
     */
    public static Object getMvelParserContext()
        {
        ensureMvel2();

        ClassLoader contextLoader = Base.getContextClassLoader();

        return s_mapParserContextByLoader.computeIfAbsent(contextLoader, MvelHelper::makeParserContext);
        }

    /**
     * Return a MVEL ParserContext for the provided {@code contextLoader}.
     *
     * @param contextLoader  a classloader
     *
     * @return a ParserContext as an Object
     */
    private static Object makeParserContext(ClassLoader contextLoader)
        {
        Object ctx = null;

        try
            {
            ctx = s_clzParserContext.getConstructor().newInstance();

            HANDLE_PARSER_CONTEXT_ADD_PACKAGE_IMPORT.invoke(ctx, "java.util");

            // set the context ClassLoader so that Mvel and ASM uses the correct ClassLoader
            // for optimizations
            Object config = HANDLE_PARSER_CONTEXT_GET_PARSER_CONFIGURATION.invoke(ctx);
            HANDLE_PARSER_CONFIGURATION_SET_CLASS_LOADER.invoke(config, contextLoader);
            }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException |
               NoSuchMethodException e)
            {
            Logger.err("MvelHelper.makeParserContext: handled unexpected exception " + e.getClass().getName() + " : " + e.getLocalizedMessage());
            }
        catch (Throwable throwable)
            {}

        return ctx;
        }

    /**
     * Compile {@code sExpr} within ParserContext {@code ctx}.
     *
     * @param sExpr  expression
     * @param ctx    ParserContext
     *
     * @return a {@link Serializable} compiled expression
     */
    public static Serializable compileExpression(String sExpr, Object ctx)
        {
        ensureMvel2();

        try
            {
            return (Serializable) HANDLE_COMPILE_EXPRESSION.invoke(sExpr, ctx);
            }
        catch (Throwable e) {}

        return null;
        }

    /**
     * Executes a compiledExpression against target.
     *
     * @param compiledExpr  compiled expression to select data from target
     * @param oTarget       target object
     *
     * @return the extracted data from {@code oTarget}
     */
    public static Object executeExpression(Object compiledExpr, Object oTarget)
        {
        ensureMvel2();

        try
            {
            return HANDLE_EXECUTE_EXPRESSION.invoke(compiledExpr, oTarget);
            }
        catch (Throwable e) {}

        return null;
        }

    /**
     * Compile {@code sExpr} using ParserContext {@code ctx}.
     *
     * @param sExpr  expression
     * @param ctx    ParserContext
     *
     * @return a {@link Serializable} compiled expression
     */
    public static Serializable compileSetExpression(String sExpr, Object ctx)
        {
        ensureMvel2();

        try
            {
            return (Serializable) HANDLE_COMPILE_SET_EXPRESSION.invoke(sExpr, ctx);
            }
        catch (Throwable e) {}

        return null;
        }

    /**
     * Set property referenced by {@code compiledSet} expression in {@code oTarget} to {@code Ovalue}..
     *
     * @param compiledSet  result of {@link #compileSetExpression(String, Object)}
     * @param oTarget      object to be updated
     * @param oValue       value to update the computed property
     */
    public static void executeSetExpression(Serializable compiledSet, Object oTarget, Object oValue)
        {
        ensureMvel2();

        try
            {
            HANDLE_EXECUTE_SET_EXPRESSION.invoke(compiledSet, oTarget, oValue);
            }
        catch (Throwable e) {}
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate that optional {@code org.mvel.mvel2} module is available.
     *
     * @throws UnsupportedOperationException if optional {@code org.mvel.mvel2} is not found
     */
    private static void ensureMvel2()
        {
        if (!s_fEnabled)
            {
            throw new UnsupportedOperationException("Invalid usage of optional module \"org.mvel.mvel2\" without its implementation being provided on path");
            }
        }

    // ----- static initialization ------------------------------------------

    static
        {
        boolean   fMvelEnabled           = false;
        Class<?>  clzMVEL                = null;
        Class<?>  clzParserContext       = null;
        Class<?>  clzParserConfiguration = null;

        MethodHandle handleParserCtxAddPackageImport  = null;
        MethodHandle handleParserCtxGetParserConfig   = null;
        MethodHandle handleParserConfigSetClassLoader = null;
        MethodHandle handleCompileExpression          = null;
        MethodHandle handleExecuteExpression          = null;
        MethodHandle handleCompileSetExpression       = null;
        MethodHandle handleExecuteSetExpression       = null;

        try
            {
            clzMVEL                = Class.forName("org.mvel2.MVEL");
            clzParserContext       = Class.forName("org.mvel2.ParserContext");
            clzParserConfiguration = Class.forName("org.mvel2.ParserConfiguration");

            // lookup these methods:
            Method methodCompileExpr     = clzMVEL.getDeclaredMethod("compileExpression", String.class, clzParserContext);
            Method methodExecuteExpr     = clzMVEL.getDeclaredMethod("executeExpression", Object.class, Object.class);
            Method methodCompileSetExpr  = clzMVEL.getDeclaredMethod("compileSetExpression", String.class, clzParserContext);
            Method methodExecuteSetExpr  = clzMVEL.getDeclaredMethod("executeSetExpression", Serializable.class, Object.class, Object.class);

            Method methodParserCtxAddPackageImport  = clzParserContext.getDeclaredMethod("addPackageImport", String.class);
            Method methodParserCtxGetParserConfig   = clzParserContext.getDeclaredMethod("getParserConfiguration");
            Method methodParserConfigSetClassLoader = clzParserConfiguration.getDeclaredMethod("setClassLoader", ClassLoader.class);

            MethodHandles.Lookup lookup = MethodHandles.lookup();

            handleCompileExpression         = lookup.unreflect(methodCompileExpr);
            handleExecuteExpression         = lookup.unreflect(methodExecuteExpr);
            handleCompileSetExpression      = lookup.unreflect(methodCompileSetExpr);
            handleExecuteSetExpression      = lookup.unreflect(methodExecuteSetExpr);
            handleParserCtxAddPackageImport = lookup.unreflect(methodParserCtxAddPackageImport);
            handleParserCtxGetParserConfig  = lookup.unreflect(methodParserCtxGetParserConfig);
            handleParserConfigSetClassLoader = lookup.unreflect(methodParserConfigSetClassLoader);

            // static initialization moved from MvelExtractor static initializer
            Field fieldCOMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = clzMVEL.getDeclaredField("COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING");

            fieldCOMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING.set(clzMVEL, true);

            fMvelEnabled = true;
            }
        catch (ClassNotFoundException e)
            {
            // ignore since org.mvel2 jar is optionally added by user
            assert(e.getMessage().contains("org.mvel2."));
            fMvelEnabled = false;
            }
        catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e)
            {
            Logger.warn("Deprecated org.mvel2.mvel implementation jar found on classpath but MVEL is disabled due to unexpected exception " +
                        e.getClass().getName() + " : " + e.getLocalizedMessage());
            fMvelEnabled = false;
            }

        s_fEnabled = fMvelEnabled;
        if (fMvelEnabled)
            {
            Logger.warn("Deprecated org.mvel2.mvel jar found on classpath. Backwards compatible REST query using MVEL evaluation is enabled.");
            }

        s_clzMVEL                                      = clzMVEL;
        s_clzParserContext                             = clzParserContext;
        HANDLE_COMPILE_EXPRESSION                      = handleCompileExpression;
        HANDLE_EXECUTE_EXPRESSION                      = handleExecuteExpression;
        HANDLE_COMPILE_SET_EXPRESSION                  = handleCompileSetExpression;
        HANDLE_EXECUTE_SET_EXPRESSION                  = handleExecuteSetExpression;
        HANDLE_PARSER_CONTEXT_ADD_PACKAGE_IMPORT       = handleParserCtxAddPackageImport;
        HANDLE_PARSER_CONTEXT_GET_PARSER_CONFIGURATION = handleParserCtxGetParserConfig;
        HANDLE_PARSER_CONFIGURATION_SET_CLASS_LOADER   = handleParserConfigSetClassLoader;

        s_mapParserContextByLoader = fMvelEnabled ? new WeakHashMap<>() :  null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * True iff mvel2 jar is on classpath and all reflection lookups succeeded.
     */
    public static final boolean s_fEnabled;

    /**
     * Reflection lookup of class org.mvel2.MVEL.
     */
    public static final Class<?> s_clzMVEL;

    /**
     * Reflection lookup of class org.mvel2.ParserContext.
     */
    public static final Class<?> s_clzParserContext;

    /**
     * MethodHandle for method addPackageImport(String) from org.mvel2.ParserContext.
     */
    public static final MethodHandle HANDLE_PARSER_CONTEXT_ADD_PACKAGE_IMPORT;

    /**
     * MethodHandle for method getParserConfiguration() from org.mvel2.ParserContext.
     */
    public static final MethodHandle HANDLE_PARSER_CONTEXT_GET_PARSER_CONFIGURATION;

    /**
     * MethodHandle for method setClassLoader() from org.mvel2.ParserConfiguration.
     */
    public static final MethodHandle HANDLE_PARSER_CONFIGURATION_SET_CLASS_LOADER;

    /**
     * MethodHandle for method compileExpression(String expr, ParserContext ctx) from org.mvel2.MVEL.
     */
    public static final MethodHandle HANDLE_COMPILE_EXPRESSION;

    /**
     * MethodHandle for method executeExpression(Object compiledExpr, Object ctx) from org.mvel2.MVEL.
     */
    public static final MethodHandle HANDLE_EXECUTE_EXPRESSION;

    /**
     * MethodHandle for method compileSetExpression(String expr, ParserContext ctx) from org.mvel2.MVEL.
     */
    public static final MethodHandle HANDLE_COMPILE_SET_EXPRESSION;

    /**
     * MethodHandle for method executeSetExpression(Serializable compiledExpr, Object ctx, Object value) from org.mvel2.MVEL.
     */
    public static final MethodHandle HANDLE_EXECUTE_SET_EXPRESSION;

    // ----- data members ---------------------------------------------------

    /**
     * Mapping(Weak reference) used to associate a ClassLoader with an instance of MVEL ParserContext.
     */
    private static final WeakHashMap<ClassLoader, Object> s_mapParserContextByLoader;
    }
