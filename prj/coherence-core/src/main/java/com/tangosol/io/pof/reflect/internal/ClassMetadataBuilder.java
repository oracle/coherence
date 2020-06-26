/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.pof.reflect.Codec;

import com.tangosol.io.pof.reflect.internal.ClassMetadata.ClassAttribute;
import com.tangosol.io.pof.reflect.internal.ClassMetadata.ClassKey;
import com.tangosol.io.pof.reflect.internal.TypeMetadata.AttributeMetadata;
import com.tangosol.io.pof.reflect.internal.TypeMetadata.TypeKey;
import com.tangosol.io.pof.reflect.internal.Visitor.Recipient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ClassMetadataBuilder provides a simple mechanism to
 * instantiate and inject state into a {@link ClassMetadata} instance.
 * Parsers that read a source will use this builder to derive a
 * {@link ClassMetadata} destination.
 * <p>
 * The general usage of this class is to perform multiple chained set calls
 * with a final build call which will realize a {@link ClassMetadata}
 * instance.
 *
 * @author hr
 *
 * @param <T>  the type the ClassMetadataBuilder will be enriched using
 *
 * @since 3.7.1
 */
public class ClassMetadataBuilder<T>
        implements Recipient<ClassMetadataBuilder<T>>
    {
    
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new ClassMetadataBuilder.
     */
    public ClassMetadataBuilder()
        {
        m_cmd = new ClassMetadata();
        m_cmd.setKey(m_key = m_cmd.new ClassKey());
        }

    // ----- ClassMetadataBuilder methods -----------------------------------

    /**
     * Specify the class type this {@link ClassMetadata} is assigned to.
     *
     * @see ClassMetadata#setClass(Class)
     *
     * @return a reference to this for chained set calls
     */
    public ClassMetadataBuilder<T> setClass(Class<T> clz)
        {
        m_cmd.setClass(clz);
        return this;
        }

    /**
     * Add an {@link AttributeMetadata} instance that is a child of
     * the {@link ClassMetadata} instance.
     *
     * @see ClassMetadata#addAttribute(AttributeMetadata)
     *
     * @return a reference to this for chained set calls
     */
    public ClassMetadataBuilder<T> addAttribute(AttributeMetadata attribute)
        {
        m_cmd.addAttribute(attribute);
        return this;
        }

    /**
     * Creates a new attribute builder for populating an
     * {@link AttributeMetadata} instance.
     *
     * @return a {@link ClassAttributeBuilder} that builds an attribute
     */
    public ClassAttributeBuilder newAttribute()
        {
        return new ClassAttributeBuilder();
        }

    /**
     * Based on the state that the builder has been informed of create and
     * return a {@link ClassMetadata} instance.
     *
     * @return the built {@link ClassMetadata} instance
     */
    public ClassMetadata<T> build() 
        {
        ClassMetadata<T> cmd = m_cmd;
        ClassKey         key = (ClassKey) cmd.getKey();

        // now that we are aware of entirety of this ClassMetadata instance
        // determine the appropriate indexes or ensure they are explicitly
        // defined
        List<AttributeMetadata<T>> listNonSorted = new ArrayList<AttributeMetadata<T>>();
        
        // create an exclusion list of indexes that are explicitly defined 
        // i.e. we must be aware of indexes that have been explicitly
        // requested and ensure we only allocate around these reserved blocks
        Set<Integer> setReserved = new HashSet<Integer>();
        for (Iterator<AttributeMetadata<T>> iterAttr = cmd.getAttributes(); iterAttr.hasNext(); )
            {
            ClassAttribute attr = (ClassAttribute) iterAttr.next();
            if (attr.getIndex() >= 0)
                {
                int iProp, iAttr = iProp = attr.getIndex();
                while (setReserved.contains(Integer.valueOf(iProp)))
                    {
                    ++iProp;
                    }

                if (iProp != iAttr)
                    {
                    final int nProp = iProp;
                    Logger.fine(() -> String.format("The requested index "
                                              + "%d on a PortableProperty annotation "
                                              + "for [typeId=%d, version=%d, property-name=%s] is "
                                              + "already allocated to an existing PortableProperty. "
                                              + "Allocated index %d instead.",
                                              iAttr, key.getTypeId(), key.getVersionId(),
                                              attr.getName(), nProp));
                    attr.setIndex(iProp);
                    }
                setReserved.add(iProp);
                }
            }
        
        int i = 0;
        for (Iterator<AttributeMetadata<T>> attributes = cmd.getAttributes(); attributes.hasNext(); ++i)
            {
            ClassAttribute attr = (ClassAttribute) attributes.next();

            if (attr.getIndex() < 0)
                {
                for (; setReserved.contains(i); ++i) { }
                attr.setIndex(i);
                }

            listNonSorted.add((AttributeMetadata<T>) attr);
            }
        cmd.setAttributes(listNonSorted);

        // inform key of the hash of the class structure now that we are
        // primed
        key.setHash(cmd.hashCode());
        
        m_cmd = new ClassMetadata();
        m_cmd.setKey(m_key = m_cmd.new ClassKey());

        return cmd;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the {@link TypeMetadata} in its current form, i.e. prior to
     * {@link #build()}.
     *
     * @return the {@link TypeMetadata} instance being enriched
     */
    public TypeMetadata<T> getTypeMetadata()
        {
        return m_cmd;
        }

    /**
     * Specify the unique type id for the {@link TypeKey}.
     *
     * @see TypeKey#getTypeId()
     *
     * @param nTypeId  type id used in uniquely identifying a
     *                 {@link TypeMetadata} instance
     *
     * @return a reference to this for chained set calls
     */
    public ClassMetadataBuilder<T> setTypeId(int nTypeId)
        {
        m_key.setTypeId(nTypeId);
        return this;
        }

    /**
     * Specify the version for this {@link TypeMetadata} instance.
     *
     * @see TypeMetadata.TypeKey#getVersionId()
     *
     * @param nVersionId  the version of this {@link TypeMetadata} instance
     *
     * @return a reference to this for chained set calls
     */
    public ClassMetadataBuilder<T> setVersionId(int nVersionId)
        {
        m_key.setVersion(nVersionId);
        return this;
        }

    /**
     * Specify the hash for this {@link TypeMetadata} instance.
     *
     * @see TypeMetadata.TypeKey#getHash()
     *
     * @param nHash  a hash value of the {@link TypeMetadata} instance
     *
     * @return a reference to this for chained set calls
     */
    public ClassMetadataBuilder<T> setHash(int nHash)
        {
        m_key.setHash(nHash);
        return this;      
        }

    // ----- Recipient interface --------------------------------------------
    
    //FIXME: generic type T==C but we can not use T due to a compile error -
    //        work out why

    /**
     * {@inheritDoc}
     */
    public <C> void accept(Visitor<ClassMetadataBuilder<T>> visitor, Class<C> clz)
        {
        Class<?>       clzRecipient  = clz;
        List<Class<?>> listHierarchy = new ArrayList<Class<?>>();
        while (clzRecipient != null && !Object.class.equals(clzRecipient))
            {
            listHierarchy.add(clzRecipient);
            clzRecipient = clzRecipient.getSuperclass();
            }

        // walk the hierarchy from the root
        for (int i = listHierarchy.size() - 1; i >= 0; --i)
            {
            visitor.visit(this, listHierarchy.get(i));
            }
        }

    // ----- inner class: ClassAttributeBuilder -----------------------------

    /**
     * The ClassAttributeBuilder provide the ability to build a
     * {@link AttributeMetadata} implementation.
     */
    public class ClassAttributeBuilder
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a ClassAttributeBuilder instance.
         */
        public ClassAttributeBuilder()
            {
            m_attribute = ClassMetadataBuilder.this.m_cmd.new ClassAttribute();
            }

        /**
         * Specify the normalized name of the {@link ClassAttribute}
         * instance.
         *
         * @see ClassAttribute#getName()
         *
         * @param sName  the normalized name of the {@link ClassAttribute}
         *               instance
         *
         * @return a reference to this for chained set calls
         */
        public ClassAttributeBuilder setName(String sName)
            {
            m_attribute.setName(sName);
            return this;
            }

        /**
         * Specify the versionId of this {@link ClassAttribute} instance.
         *
         * @see ClassAttribute#getVersionId()
         *
         * @param  nVersionId  version of the {@link ClassAttribute}
         *                     instance
         *
         * @return a reference to this for chained set calls
         */
        public ClassAttributeBuilder setVersionId(int nVersionId)
            {
            m_attribute.setVersionId(nVersionId);
            return this;
            }

        /**
         * Specify the index of this {@link ClassAttribute} instance used to
         * sequence many {@link ClassAttribute} instances.
         *
         * @see ClassAttribute#getIndex()
         *
         * @param nIndex  index to specify this attributes sequence number
         *
         * @return a reference to this for chained set calls
         */
        public ClassAttributeBuilder setIndex(int nIndex)
            {
            m_attribute.setIndex(nIndex);
            return this;
            }

        /**
         * Specify the {@link Codec} to use for this {@link ClassAttribute}
         * instance.
         *
         * @see ClassAttribute#getCodec()
         *
         * @param codec  the codec to use for this {@link ClassAttribute}
         *               instance
         *
         * @return a reference to this for chained set calls
         */
        public ClassAttributeBuilder setCodec(Codec codec)
            {
            m_attribute.setCodec(codec);
            return this;
            }

        /**
         * Specify the {@link InvocationStrategy} implementation that allows
         * values to be written and received to the attribute.
         *
         * @see ClassAttribute#setInvocationStrategy(InvocationStrategy)
         *
         * @param strategy  the strategy provides an implementation to write
         *                  and receive values
         *
         * @return a reference to this for chained set calls
         */
        public ClassAttributeBuilder setInvocationStrategy(InvocationStrategy<T,Object> strategy)
            {
            m_attribute.setInvocationStrategy(strategy);
            return this;
            }

        /**
         * Create a {@link ClassAttribute} instance based on the values set
         * during the lifetime of this builder.
         *
         * @return an enriched {@link ClassAttribute} instance
         */
        public ClassMetadata<T>.ClassAttribute build()
            {
            ClassMetadata<T>.ClassAttribute attribute = m_attribute;
            m_attribute = m_cmd.new ClassAttribute();
            
            return attribute;
            }

        // ----- data members -----------------------------------------------

        /**
         * {@link ClassAttribute} that is built across the duration of 
         * {@link ClassAttributeBuilder} calls until it is returned via the
         * {@link #build()} method.
         */
        private ClassMetadata<T>.ClassAttribute m_attribute;
        }

    // ----- data members ---------------------------------------------------

    /**
     * {@link ClassMetadata} that is built across the duration of
     * ClassMetadataBuilder calls until it is returned via the
     * {@link #build()} method.
     */
    private ClassMetadata<T> m_cmd;

    /**
     * {@link ClassKey} that is built across the duration of
     * ClassMetadataBuilder calls until it is returned via the
     * {@link #build()} method.
     */
    private ClassKey m_key;
    }
