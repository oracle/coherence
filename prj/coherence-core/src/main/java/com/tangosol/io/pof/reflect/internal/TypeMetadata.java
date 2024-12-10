/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.tangosol.io.pof.reflect.Codec;

import java.util.Iterator;

/**
 * TypeMetadata represents the definition of a type including uniqueness
 * (@{link TypeKey}) and all structural properties. This definition is used
 * to uniformly define types and their internal structures. Uniformity in
 * this context is in relation to the supported languages.
 * <p>
 * This interface defines the contract required by users of TypeMetadata.
 * This includes the ability to have a predictable order for both getter and
 * setter methods, the ability to retrieve a method, and to create a new
 * instance of a type this metadata describes.
 *
 * @author hr
 * @since 3.7.1
 *
 * @param <T>  the user type this metadata instance describes
 */
public interface TypeMetadata<T>
    {
    /**
     * Return a unique key for this TypeMetaData.
     *
     * @return TypeKey uniquely identifying an instance of TypeMetadata
     */
    public TypeKey getKey();

    /**
     * Create a new instance of the object represented by this type.
     *
     * @return new object instance represented by this metadata
     */
    public T newInstance();

    /**
     * Provides a predictable {@link Iterator} over {@link AttributeMetadata}
     * for the attributes of the type represented by this TypeMetadata.
     *
     * @return Iterator of {@link AttributeMetadata}
     */
    public Iterator<AttributeMetadata<T>> getAttributes();

    /**
     * Provides a {@link AttributeMetadata} encapsulating either the field or
     * method requested.
     *
     * @param sName  name of the attribute
     *
     * @return AttributeMetadata reprOpenProjeesenting the annotated method
     *         or field
     */
    public AttributeMetadata<T> getAttribute(String sName);

    // ----- inner interface: TypeKey ---------------------------------------

    /**
     * A type key embodies contributors to the uniqueness representing a
     * TypeMetadata instance. This is the sum of typeId, versionId and a
     * hash.
     *
     * @author hr
     * @since 3.7.1
     */
    public interface TypeKey
        {
        /**
         * An integer identifying a unique pof user type providing the
         * ability to distinguish between types using a compact form.
         *
         * @return pof user type identifier
         */
        public int getTypeId();

        /**
         * The version specified by the serializer when this object was
         * serialized.
         *
         * @return integer representing the version of this POF type
         */
        public int getVersionId();

        /**
         * A unique hash representing the TypeMetadata structure.
         *
         * @return hash of TypeMetadata
         */
        public int getHash();
        }

    // ----- inner interface: AttributeMetadata -----------------------------

    /**
     * AttributeMetadata represents all appropriate information relating to
     * an attribute within a type. This contract has similar forms in all
     * supported languages providing a language agnostic mechanism to
     * describe elements within a structure and an invocation mechanism for
     * setting or retrieving the value for an attribute.
     *
     * @author hr
     * @since 3.7.1
     *
     * @param <T>  the container type of which this attribute is a member
     */
    public interface AttributeMetadata<T>
        {
        /**
         * Name of the attribute this metadata describes.
         *
         * @return attribute name
         */
        public String getName();

        /**
         * Returns the versionId assigned to this attributes metadata
         * instance. This versionId is not required however is used as an
         * indicator to determine the version this attribute was introduced
         * in.
         *
         * @return integer representing the version of this attribute
         *         metadata
         */
        public int getVersionId();

        /**
         * The index used to order the attributes when iterated by the 
         * containing {@link TypeMetadata} class.
         *
         * @return index to identify this attribute's position in a sequence
         */
        public int getIndex();

        /**
         * The codec assigned to this attribute which will perform type safe
         * (de)serialization.
         *
         * @return the {@link Codec} used to (de)serialize this attribute
         */
        public Codec getCodec();

        /**
         * Returns the value of the attribute contained within the given
         * object.
         *
         * @param container  the containing object
         * 
         * @return the attribute value stored on the object passed in
         */
        public Object get(T container);

        /**
         * Sets the {@code value} of this attribute within the given object.
         *
         * @param container  the containing object
         * @param o          the value to set this attribute to
         */
        public void set(T container, Object o);
        }
    }
