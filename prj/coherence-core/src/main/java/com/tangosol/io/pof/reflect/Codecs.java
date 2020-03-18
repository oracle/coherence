/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.Base;
import com.tangosol.util.LongArray;

import java.io.IOException;

import java.lang.reflect.Array;

import java.util.Collection;
import java.util.Map;

/**
 * Codecs is a container for accessing default {@link Codec} implementations.
 *
 * @author hr
 * @since 3.7.1
 */
public class Codecs
    {
    // ----- factory methods ------------------------------------------------

    /**
     * Return a {@link Codec} based on the provided Class.
     * <p>
     * The provided Class should implement one of:
     * <ol>
     *     <li>Codec - the Codec implementation should have a no-arg constructor
     *                 which will be instantiated and returned.</li>
     *     <li>Collection - the provided class should implement the {@link Collection}
     *                      interface and have a no-arg constructor. A Codec
     *                      supporting Collections will be returned</li>
     *     <li>Map - the provided class should implement the {@link Map} interface
     *               and have a no-arg constructor. A Codec supporting Maps
     *               will be returned</li>
     *     <li>LongArray - the provided class should implement the {@link LongArray} interface
     *               and have a no-arg constructor. A Codec supporting LongArrays
     *               will be returned</li>
     *     <li>T[] - the provided class should be an array and the component
     *               type of the array should have a no-arg constructor. A Codec
     *               supporting arrays will be returned</li>
     * </ol>
     *
     * @param clz  a Class that implements Codec or is one of the supported
     *             types
     *
     * @return a Codec that supports encoding and decoding of objects of the
     *         specified type
     */
    @SuppressWarnings("unchecked")
    public static Codec getCodec(Class<?> clz)
        {
        if (DefaultCodec.class.equals(clz))
            {
            return DEFAULT_CODEC;
            }

        if (Codec.class.isAssignableFrom(clz))
            {
            try
                {
                return (Codec) clz.newInstance();
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e, "Unable to instantiate class (" + clz +
                    ") that should implement Codec and have a no-arg constructor");
                }
            }
        if (clz.isArray())
            {
            return new ArrayCodec(clz.getComponentType());
            }
        if (Collection.class.isAssignableFrom(clz))
            {
            return new CollectionCodec((Class<? extends Collection<Object>>) clz);
            }
        if (Map.class.isAssignableFrom(clz))
            {
            return new MapCodec((Class<? extends Map<Object, Object>>) clz);
            }
        if (LongArray.class.isAssignableFrom(clz))
            {
            return new LongArrayCodec((Class<LongArray<Object>>) clz);
            }

        throw new IllegalArgumentException("Provided Class (" + clz +
                " is not a Codec or a supported type");
        }

    // ----- inner class: AbstractCodec -------------------------------------

    /**
     * Abstract {@link Codec} implementations that encodes objects by simply
     * delegating to {@link PofWriter#writeObject(int, Object)}. Generally
     * the default writeObject implementation does not need to be modified as
     * the current accommodation of types and conversion to POF is generally
     * accepted, with the deserialization being more likely to be specific.
     *
     * @since 3.7.1
     */
    public static abstract class AbstractCodec
            implements Codec
        {
        // ----- Codec interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        public void encode(PofWriter out, int index, Object value) throws IOException
            {
            out.writeObject(index, value);
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Return a new instance of T based on the provided Class&lt;T&gt;, or
         * throw a RuntimeException.
         *
         * @param clz  the class to instantiate
         *
         * @param <T>  the type to instantiate and return
         *
         * @return a new instance of type T
         */
        protected static <T> T newInstance(Class<T> clz)
            {
            try
                {
                return clz.newInstance();
                }
            catch (InstantiationException | IllegalAccessException e)
                {
                throw Base.ensureRuntimeException(e, "Unable to instantiate class (" + clz +
                        ") that should implement Codec and have a no-arg constructor");
                }
            }
        }

    // ----- inner class: DefaultCodec --------------------------------------

    /**
     * Implementation of {@link Codec} that simply delegates to
     * {@link PofReader#readObject(int)} and
     * {@link PofWriter#writeObject(int, Object)} to deserialize and serialize
     * an object.
     *
     * @since 3.7.1
     */
    public static class DefaultCodec
            extends AbstractCodec
        {
        // ----- Codec interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        public Object decode(PofReader in, int index) throws IOException
            {
            return in.readObject(index);
            }
        }

    // ----- inner class: CollectionCodec -----------------------------------

    /**
     * Implementation of {@link Codec} that delegates to
     * {@link PofReader#readCollection(int, Collection)} and
     * {@link PofWriter#writeCollection(int, Collection)} to deserialize and
     * serialize an object.
     *
     * @since 3.7.1
     */
    public static class CollectionCodec
            extends AbstractCodec
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a CollectionCodec.
         *
         * @param clzCol  the Class that represents the Collection implementation
         *                that will be serialized and deserialized into
         */
        public CollectionCodec(Class<? extends Collection<Object>> clzCol)
            {
            f_clzCol = clzCol;
            }

        // ----- Codec interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        public Object decode(PofReader in, int index) throws IOException
            {
            return in.readCollection(index, newInstance(f_clzCol));
            }

        // ----- data members -----------------------------------------------

        /**
         * Class that represents the Collection implementation that will be
         * serialized and deserialized into.
         */
        protected Class<? extends Collection<Object>> f_clzCol;
        }

    // ----- inner class: MapCodec --------------------------------------

    /**
     * Implementation of {@link Codec} that delegates to
     * {@link PofReader#readMap(int, Map)} and
     * {@link PofWriter#writeMap(int, Map)} to deserialize and serialize
     * an object.
     *
     * @since 3.7.1
     */
    public static class MapCodec
            extends AbstractCodec
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a MapCodec.
         *
         * @param clzMap  the Class that represents the Map implementation that
         *                will be serialized and deserialized into
         */
        public MapCodec(Class<? extends Map<Object, Object>> clzMap)
            {
            f_clzMap = clzMap;
            }

        // ----- Codec interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        public Object decode(PofReader in, int index) throws IOException
            {
            return in.readMap(index, newInstance(f_clzMap));
            }

        // ----- data members -----------------------------------------------

        /**
         * Class that represents the Map implementation that will be serialized
         * and deserialized into.
         */
        protected Class<? extends Map<Object, Object>> f_clzMap;
        }

    // ----- inner class: LongArrayCodec ------------------------------------

    /**
     * Implementation of {@link Codec} that delegates to
     * {@link PofReader#readLongArray(int, LongArray)} and
     * {@link PofWriter#writeLongArray(int, LongArray)} to deserialize and serialize
     * an object.
     *
     * @since 3.7.1
     */
    public static class LongArrayCodec
            extends AbstractCodec
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a LongArrayCodec.
         *
         * @param clzLa  the Class that represents the LongArray implementation
         *               that will be serialized and deserialized into
         */
        public LongArrayCodec(Class<LongArray<Object>> clzLa)
            {
            f_clzLa = clzLa;
            }

        // ----- Codec interface ----------------------------------------

        /**
         * {@inheritDoc}
         */
        public Object decode(PofReader in, int index) throws IOException
            {
            return in.readLongArray(index, newInstance(f_clzLa));
            }

        // ----- data members -----------------------------------------------

        /**
         * Class that represents the LongArray implementation that will be
         * serialized and deserialized into.
         */
        protected Class<LongArray<Object>> f_clzLa;
        }

    // ----- inner class: ArrayCodec ----------------------------------------

    /**
     * Implementation of {@link Codec} that delegates to
     * {@link PofReader#readObjectArray(int, Object[])} and
     * {@link PofWriter#writeObjectArray(int, Object[])} to deserialize and serialize
     * an object.
     *
     * @since 3.7.1
     */
    public static class ArrayCodec
            extends AbstractCodec
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an ArrayCodec.
         *
         * @param clzComponent  the Class type for each element in the array
         */
        public ArrayCodec(Class<?> clzComponent)
            {
            f_clzComponent = clzComponent;
            }

        // ----- Codec interface ----------------------------------------

        /**
         * {@inheritDoc}
         */
        public Object decode(PofReader in, int index) throws IOException
            {
            return in.readArray(index,
                    cSize -> (Object[]) Array.newInstance(f_clzComponent, cSize));
            }

        /**
         * Class that represents the type for each element in the array that
         * will be serialized and deserialized into.
         */
        protected Class<?> f_clzComponent;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of a {@link DefaultCodec}
     */
    public static final Codec DEFAULT_CODEC = new DefaultCodec();
    }
