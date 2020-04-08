/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.coherence.config.Config;

import com.tangosol.util.Base;

import java.io.Serializable;

import java.lang.invoke.SerializedLambda;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * RemotableSupport provides support for class remoting.
 * <p>
 * While this class extends {@code ClassLoader}, it is only used to define
 * remotable classes and is not intended to be used as a general purpose
 * class loader.
 *
 * @author hr/as  2015.03.31
 * @since 12.2.1
 */
@SuppressWarnings("unchecked")
public class RemotableSupport
        extends ClassLoader
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a RemotableSupport with the provided ClassLoader.
     *
     * @param parent  the parent ClassLoader
     */
    public RemotableSupport(ClassLoader parent)
        {
        super(parent);
        }

    // ----- static helpers -------------------------------------------------

    /**
     * Obtain a RemotableSupport for the specified ClassLoader.
     *
     * @param loader  a ClassLoader to get RemotableSupport for
     *
     * @return the RemotableSupport instance
     */
    public static RemotableSupport get(ClassLoader loader)
        {
        return loader instanceof RemotableSupport
               ? (RemotableSupport) loader
               : s_mapByClassLoader.computeIfAbsent(Base.ensureClassLoader(loader), RemotableSupport::new);
        }

    // ----- public methods -------------------------------------------------

    /**
     * Create a {@link RemoteConstructor} for a raw {@link Serializable} lambda instance.
     *
     * @param <T>     the functional interface
     * @param lambda  the raw {@link Serializable} lambda
     *
     * @return a {@code RemoteConstructor} for the specified lambda
     */
    public <T extends Serializable> RemoteConstructor<T> createRemoteConstructor(T lambda)
        {
        assert Lambdas.isLambda(lambda);

        SerializedLambda lambdaMetadata = Lambdas.getSerializedLambda(lambda);
        String           sImplClassFile = lambdaMetadata.getImplClass() + ".class";
        ClassLoader      loaderTmp      = getParent();

        if (loaderTmp.getResource(sImplClassFile) == null)
            {
            loaderTmp = lambda.getClass().getClassLoader();
            if (loaderTmp.getResource(sImplClassFile) == null)
                {
                throw new IllegalStateException("ClassFile for the remote lambda could not be introspected. " +
                    loaderTmp + ".getResource(" + sImplClassFile + ") unexpectedly returned null");
                }
            }

        ClassLoader   loader   = loaderTmp;
        ClassIdentity identity = Lambdas.createIdentity(lambdaMetadata, getParent());

        ClassDefinition definition = f_mapDefinitions.computeIfAbsent(
                identity, id -> Lambdas.createDefinition(id, lambda, loader));

        return new RemoteConstructor<>(definition, Lambdas.getCapturedArguments(lambdaMetadata));
        }

    /**
     * Create a {@link RemoteConstructor} for the specified Class.
     *
     * @param <T>      the interface type
     * @param clzImpl  the local class to create constructor for
     *                 (must implement the specified interface type)
     * @param args     the arguments that should be passed to the constructor of
     *                 the remote class
     *
     * @return a {@code RemoteConstructor} for the specified class
     */
    public <T> RemoteConstructor<T> createRemoteConstructor(Class<? extends T> clzImpl, Object... args)
        {
        ClassDefinition definition =
                f_mapDefinitions.computeIfAbsent(new ClassIdentity(clzImpl),
                                                 id -> Classes.createDefinition(id, clzImpl));

        return new RemoteConstructor<>(definition, args);
        }

    /**
     * Return the raw instance represented by the provided {@link RemoteConstructor}.
     * The raw instance will be an identical type and definition to that sent
     * by the submitter.
     *
     * @param <T>          the type of the raw instance
     * @param constructor  the RemoteConstructor to realize
     *
     * @return the raw instance
     */
    public <T> T realize(RemoteConstructor<T> constructor)
        {
        ClassDefinition            definition = registerIfAbsent(constructor.getDefinition());
        Class<? extends Remotable> clz        = definition.getRemotableClass();
        if (clz == null)
            {
            synchronized (definition)
                {
                clz = definition.getRemotableClass();
                if (clz == null)
                    {
                    definition.setRemotableClass(defineClass(definition));
                    }
                }
            }

        Remotable<T> instance = (Remotable<T>) definition.createInstance(constructor.getArguments());
        instance.setRemoteConstructor(constructor);

        return (T) instance;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Register a {@link ClassDefinition} if not already registered.
     *
     * @param definition  the ClassDefinition to register
     *
     * @return the registered ClassDefinition instance
     */
    protected ClassDefinition registerIfAbsent(ClassDefinition definition)
        {
        assert definition != null;

        ClassDefinition rtn = f_mapDefinitions.putIfAbsent(definition.getId(), definition);

        return rtn == null ? definition : rtn;
        }

    /**
     * Define the Class for the provided {@link ClassDefinition}.
     *
     * @param definition  the definition to define the Class for
     *
     * @return a Class for the specified ClassDefinition
     */
    protected Class<? extends Remotable> defineClass(ClassDefinition definition)
        {
        String  sBinClassName = definition.getId().getName();
        String  sClassName    = sBinClassName.replace('/', '.');
        byte[]  abClass       = definition.getBytes();

        definition.dumpClass(DUMP_REMOTABLE);

        return (Class<? extends Remotable>) defineClass(sClassName, abClass, 0, abClass.length);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "RemotableSupport{" +
               "parent=" + getParent() +
               ", definitions=" + f_mapDefinitions.keySet() +
               '}';
        }

    // ----- constants ------------------------------------------------------

    /**
     * An undocumented system property for a file system path to store ClassFiles
     * that will be defined by this RemotableSupport.
     * <p>
     * This property allows manual inspection of the ClassFiles sent to the
     * server.
     */
    private static final String DUMP_REMOTABLE = Config.getProperty("coherence.server.remotable.dumpClasses");

    /**
     * A WeakHashMap of {@link RemotableSupport instances}, keyed by ClassLoader.
     */
    private static final Map<ClassLoader, RemotableSupport> s_mapByClassLoader
            = Collections.synchronizedMap(new WeakHashMap<>());

    // ----- data members ---------------------------------------------------

    /**
     * A Map of known {@link Remotable} class definitions.
     */
    protected final Map<ClassIdentity, ClassDefinition> f_mapDefinitions = new ConcurrentHashMap<>();
    }
