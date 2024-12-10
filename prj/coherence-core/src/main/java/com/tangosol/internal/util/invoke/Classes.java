/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.coherence.config.Config;

import com.tangosol.util.Base;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helpers for class remoting.
 *
 * @author as  2015.08.21
 * @since 12.2.1
 */
public abstract class Classes
    {
    /**
     * Create a {@link ClassDefinition} for the provided raw Class.
     *
     * @param id     the remote class identity
     * @param clazz  the raw class
     *
     * @return a ClassDefinition that represents the specified raw Class
     */
    public static ClassDefinition createDefinition(ClassIdentity id, Class<?> clazz)
        {
        try
            {
            String      sClassName = clazz.getName().replace('.', '/');
            InputStream in         = clazz.getClassLoader().getResourceAsStream(sClassName + ".class");
            byte[]      abClass    = Base.read(in);

            abClass = RemotableClassGenerator.createRemoteClass(sClassName, id.getName(), abClass);

            ClassDefinition definition = new ClassDefinition(id, abClass);
            definition.dumpClass(DUMP_CLASSES);

            return definition;
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Create a {@link Remotable} instance of a specified class.
     *
     * @param <T>          the type of the interface to return
     * @param clzRetValue  the type of this method's return value
     * @param clzTarget    the Class to make {@code Remotable}
     * @param args         the constructor arguments that should be use when creating
     *                     an instance
     *
     * @return a {@code Remotable} instance of a specified class
     */
    public static <T> T createRemotable(Class<T> clzRetValue, Class<? extends T> clzTarget, Object... args)
        {
        RemotableSupport support = RemotableSupport.get(clzTarget.getClassLoader());
        return support.realize(support.createRemoteConstructor(clzRetValue, clzTarget, args));
        }

    // ---- static members --------------------------------------------------

    /**
     * Path to the file system where generated lambdas should be written; purely for debugging.
     */
    private static final String DUMP_CLASSES = Config.getProperty("coherence.remotable.dumpClasses",
            Config.getProperty("coherence.remotable.dumpAll"));
    }
