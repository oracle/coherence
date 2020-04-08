/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.base.Disposable;

/**
 * A registry of strongly typed and possibly named {@link ParameterizedBuilder}s.
 * <p>
 * When a {@link ParameterizedBuilder} is registered with a {@link ParameterizedBuilderRegistry}, the
 * {@link ParameterizedBuilderRegistry} assumes ownership of the said builder, up until
 * at which point the {@link ParameterizedBuilderRegistry} is {@link #dispose() disposed}.
 * <p>
 * <strong>Important:</strong> Although a {@link ParameterizedBuilderRegistry} manages
 * builders in a thread-safe manner, it is possible for a thread calling
 * {@link #getBuilder(Class, String)} to receive a <code>null</code> return
 * value while another thread is registering a builder.
 *
 * @author bo  2014.10.27
 * @since Coherence 12.1.3
 */
public interface ParameterizedBuilderRegistry
        extends Disposable, Iterable<ParameterizedBuilderRegistry.Registration>
    {
    /**
     * Attempts to retrieve the builder that was registered with the
     * specified class.
     *
     * @param <T>          the type of the instance that will be produced by the builder
     * @param clzInstance  the class of the instance
     *
     * @return the registered builder or <code>null</code> if the builder is
     *         unknown to the {@link ParameterizedBuilderRegistry}
     */
    public <T> ParameterizedBuilder<T> getBuilder(Class<T> clzInstance);

    /**
     * Attempts to retrieve the builder that was registered with the
     * specified class and name.
     *
     * @param <T>           the type of the instance that will be produced by the builder
     * @param clzInstance   the class of the instance
     * @param sBuilderName  the name of the builder
     *
     * @return the registered builder or <code>null</code> if the builder is
     *         unknown to the {@link ParameterizedBuilderRegistry}
     */
    public <T> ParameterizedBuilder<T> getBuilder(Class<T> clzInstance, String sBuilderName);

    /**
     * Registers a {@link ParameterizedBuilder} for later retrieval with {@link #getBuilder(Class)}.
     * <p>
     * Notes:
     * <ol>
     *  <li>Multiple builders for the same class can be registered if each
     *      builder is registered with a unique name via
     *      {@link #registerBuilder(Class, String, ParameterizedBuilder)}
     *  <li>Registration of builders will occur in a thread-safe manner.
     *  <li>Builders that are {@link Disposable} will be disposed when the
     *      {@link ParameterizedBuilderRegistry} is disposed.
     * </ol>
     *
     * @param clzInstance  the class of instances produced by the builder
     * @param builder      the builder
     *
     * @return  the actual name used to register the builder
     *
     * @throws IllegalArgumentException  if a builder of the same specified type
     *                                   is already registered
     */
    public <T> String registerBuilder(Class<T> clzInstance, ParameterizedBuilder<? extends T> builder)
            throws IllegalArgumentException;

    /**
     * Registers a {@link ParameterizedBuilder} with the specified name for later retrieval with
     * {@link #getBuilder(Class, String)}.
     * <p>
     * Notes:
     * <ol>
     *  <li>Registration of builders will occur in a thread-safe manner.
     *  <li>Builders that are {@link Disposable} will be disposed when the
     *      {@link ParameterizedBuilderRegistry} is disposed.
     * </ol>
     *
     * @param clzInstance   the class of instances produced by the builder
     * @param builder       the builder
     * @param sBuilderName  the proposed name of the builder
     *
     * @return  the actual name used to register the builder
     *
     * @throws IllegalArgumentException  if a builder of the same specified type
     *                                   and name is already registered
     */
    public <T> String registerBuilder(Class<T> clzInstance, String sBuilderName,
                                      ParameterizedBuilder<? extends T> builder)
            throws IllegalArgumentException;

    // ----- Registration interface ------------------------------------------------------

    /**
     * Defines a single {@link ParameterizedBuilder} registration with a
     * {@link ParameterizedBuilderRegistry}.
     */
    public static interface Registration<T>
        {
        /**
         * Obtains the name of the builder.
         *
         *  @return  the name of the builder
         */
        public String getName();

        /**
         * Obtains the class of instance produced by the builder.
         *
         * @return the class of instance
         */
        public Class<T> getInstanceClass();

        /**
         * Obtains the {@link ParameterizedBuilder}.
         *
         * @return  the {@link ParameterizedBuilder}
         */
        public ParameterizedBuilder<T> getBuilder();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name to use for the registration of a singleton and thus default resource.
     */
    public static final String DEFAULT_NAME = "default";
    }
