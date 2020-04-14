/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.annotation;

import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.reflect.Codec;
import com.tangosol.io.pof.reflect.Codecs.DefaultCodec;
import com.tangosol.io.pof.schema.annotation.Portable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A PortableProperty marks a member variable or method accessor as a
 * POF serialized attribute. Whilst the {@link PortableProperty#value()} and
 * {@link PortableProperty#codec} can be explicitly specified they can be
 * determined by classes that use this annotation. Hence these attributes
 * serve as hints to the underlying parser.
 *
 * @deprecated Since Coherence 14.1.2. Use {@link Portable} annotation instead.
 *
 * @author hr
 * @since  3.7.1
 */
@Target(value={ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface PortableProperty
    {
    /**
     * The index of this property.
     *
     * @return POF index
     *
     * @see PofWriter
     */
    int value() default -1;

    /**
     * A {@link Codec} to use to override the default behavior in serializing
     * and deserializing a property.
     * <p>
     * The Class specified must have a no-arg constructor and must implement
     * one of:
     * <ol>
     *     <li>Codec      - the specified Codec implementation will be instantiated
     *                      and returned.</li>
     *     <li>Collection - the specified class should implement the {@link
     *                      java.util.Collection Collection} interface.
     *                      A Codec implementation that supports the Collection
     *                      type will be used.</li>
     *     <li>Map        - the specified class should implement the {@link
     *                      java.util.Map Map} interface.
     *                      A Codec implementation that supports the Map
     *                      type will be used.</li>
     *     <li>LongArray  - the specified class should implement the {@link
     *                      com.tangosol.util.LongArray LongArray} interface.
     *                      A Codec implementation that supports the LongArray
     *                      type will be used.</li>
     *     <li>T[]        - the provided class should be an array and the component
     *                      type of the array should have a no-arg constructor.
     *                      A Codec implementation that supports arrays will
     *                      be used.</li>
     * </ol>
     * For example, to override the default serialization/deserialization to
     * use a LinkedList implementation could be as trivial as:
     * <pre>{@code
     *     {@literal @}PortableProperty(value = 0, codec = LinkedList.class)
     *     protected List m_listPeople;
     * }</pre>
     * A more complex example could be to specify a custom Codec implementation:
     * <pre>{@code
     *     {@literal @}PortableProperty(value = 0, codec = MyArrayListCodec.class)
     *     protected List m_listPeople;
     *
     *     class MyArrayListCodec
     *          implements Codec
     *         {
     *         public Object decode(PofReader in, int index)
     *             throws IOException
     *             {
     *             in.readCollection(index, new ArrayList(16);
     *             }
     *         ...
     *         }
     * }</pre>
     *
     * @return a Class representing the codec to use for this property, or a
     *         an implementation of a well known type (Collection, Map, LongArray,
     *         or an array)
     */
    Class<?> codec() default DefaultCodec.class;
    }
