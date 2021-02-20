/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import javax.inject.Named;

/**
* A factory for {@link Serializer} objects.
*
* @author lh/jh  2010.11.30
*
* @since Coherence 3.7
*/
public interface SerializerFactory
    {
    /**
    * Create a new Serializer.
    * <p>
    * If the new Serializer is an instance of {@link ClassLoaderAware} and
    * the specified ClassLoader is non-null, the new Serializer will be
    * configured with the ClassLoader before it is returned to the caller.
    *
    * @param loader  the optional ClassLoader with which to configure the
    *                new Serializer.
    *
    * @return the new Serializer
    */
    public Serializer createSerializer(ClassLoader loader);

    /**
     * Return the name of this serializer factory.
     *
     * @return the name of this serializer serializer factory
     *
     * @since 20.12
     */
    default String getName()
        {
        Named named = getClass().getAnnotation(Named.class);
        return named == null ? null : named.value();
        }
    }
