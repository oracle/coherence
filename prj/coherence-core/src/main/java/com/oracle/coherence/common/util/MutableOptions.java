/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

/**
 * A mutable collection of zero or more values, typically called an option,
 * internally arranged as a map, keyed by the concrete type of each option in
 * the collection.
 *
 * @param <T>  the base type of the options in the collection
 *
 * @author Jonathan Knight  2020.11.04
 * @since 20.12
 */
public class MutableOptions<T>
        extends Options<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an options collection of the specified option type.
     *
     * @param clsType  the type of option this in this options collection
     */
    public MutableOptions(Class<T> clsType)
        {
        super(clsType);
        }

    // ----- MutableOptions methods ------------------------------------------------

    /**
     * Adds an option to the collection, replacing an
     * existing option of the same concrete type if one exists.
     *
     * @param option  the option to add
     *
     * @return the {@link Options} to permit fluent-style method calls
     */
    public MutableOptions<T> add(T option)
        {
        Class<T> clz = getClassOf(option);

        m_mapOptions.put(clz, option);

        return this;
        }

    /**
     * Adds an array of options to the collection, replacing
     * existing options of the same concrete type where they exist.
     *
     * @param aOptions  the options to add
     *
     * @return the {@link Options} to permit fluent-style method calls
     */
    public Options<T> addAll(T[] aOptions)
        {
        if (aOptions != null)
            {
            for (T option : aOptions)
                {
                add(option);
                }
            }

        return this;
        }

    /**
     * Adds all of the options in the specified {@link Options}
     * to this collection, replacing existing options of the same concrete
     * type where they exist.
     *
     * @param options  the {@link Options} to add
     *
     * @return the {@link Options} to permit fluent-style method calls
     */
    public Options<T> addAll(Options<? extends T> options)
        {
        for (T option : options.asArray())
            {
            add(option);
            }

        return this;
        }
    }
