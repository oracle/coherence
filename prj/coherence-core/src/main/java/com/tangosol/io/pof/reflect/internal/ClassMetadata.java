/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.tangosol.io.pof.reflect.Codec;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.HashHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link TypeMetadata} implementation coupled to the Java type metadata
 * definition language: {@link Class}, {@link java.lang.reflect.Field}, and
 * {@link java.lang.reflect.Method}.
 *
 * @author hr
 * @since 3.7.1
 *
 * @param <T>  the user type this metadata instance describes
 */
public class ClassMetadata<T>
        implements TypeMetadata<T>
    {

    // ----- TypeMetadata interface -----------------------------------------

    /**
     * {@inheritDoc}
     */
    public TypeKey getKey()
        {
        return m_key;
        }

    /**
     * {@inheritDoc}
     */
    public T newInstance()
        {
        try
            {
            return m_clz == null ? null : m_clz.newInstance();
            }
        catch (InstantiationException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        catch (IllegalAccessException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    public Iterator<AttributeMetadata<T>> getAttributes()
        {
        return m_setAttr.iterator();
        }

    /**
     * {@inheritDoc}
     */
    public AttributeMetadata<T> getAttribute(String sName)
        {
        return m_mapAttrByName.get(sName);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Specify the {@link Class} that uniquely identifies this metadata.
     *
     * @param clz  class that defines this type
     */
    void setClass(Class<T> clz)
        {
        m_clz = clz;
        }

    /**
     * Specify the {@link TypeKey}.
     *
     * @param key  unique identifier of this metadata instance
     */
    void setKey(TypeKey key)
        {
        m_key = key;
        }

    /**
     * Specify all {@link AttributeMetadata} instances that represent this
     * type.
     *
     * @param colAttr  attribute metadata information to enrich this
     *                    metadata
     */
    void setAttributes(Collection<AttributeMetadata<T>> colAttr)
        {
        Set<AttributeMetadata<T>> setAttrs      = m_setAttr;
        Map<String, AttributeMetadata>   mapAttrByName = m_mapAttrByName;
        setAttrs.clear();
        setAttrs.addAll(colAttr);

        mapAttrByName.clear();
        for (AttributeMetadata attribute : colAttr)
            {
            mapAttrByName.put(attribute.getName(), attribute);
            }
        }

    /**
     * Add an attribute to this TypeMetadata.
     *
     * @param attribute  attribute metadata definition to add
     *
     * @return whether the attribute metadata was added
     */
    boolean addAttribute(AttributeMetadata attribute)
        {
        boolean fAdded = m_setAttr.add(attribute);

        // maintain reference
        if (fAdded)
            {
            m_mapAttrByName.put(attribute.getName(), attribute);
            }
        
        return fAdded;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public int hashCode()
        {
        TypeKey key = m_key;
        int hash = HashHelper.hash(key.getTypeId(), 31);
            hash = HashHelper.hash(key.getVersionId(), hash);
            hash = HashHelper.hash(m_setAttr, hash);
        return hash;
        }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object oThat)
        {
        if (this == oThat)
            {
            return true;
            }
        
        if (!(oThat instanceof TypeMetadata))
            {
            return false;
            }

        TypeMetadata that = (TypeMetadata) oThat;
        // check key and version
        if (!Base.equals(this.getKey(), that.getKey()))
            {
            return false;
            }

        for (Iterator<AttributeMetadata<T>> iterThis = m_setAttr.iterator(), iterThat = that.getAttributes();
            iterThis.hasNext() || iterThat.hasNext(); )
            {
            AttributeMetadata attrThis = iterThis.hasNext() ? iterThis.next() : null;
            AttributeMetadata attrThat = iterThat.hasNext() ? iterThat.next() : null;
            if (!Base.equals(attrThis, attrThat))
                {
                return false;
                }
            }
        return true;
        }

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return String.format("%s (key=%s, attributes=%s)",
                ClassHelper.getSimpleName(getClass()),
                m_key, m_setAttr);
        }

    // ----- inner class: ClassKey ------------------------------------------

    /**
     * A ClassKey contains information to uniquely identify this class type
     * instance. Specifically unique identification is a product of
     * {@code typeId + versionId + class-hash}.
     */
    class ClassKey
            implements TypeKey
        {
        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return m_nTypeId;
            }

        /**
         * {@inheritDoc}
         */
        public int getVersionId()
            {
            return m_nVersionId;
            }

        /**
         * {@inheritDoc}
         */
        public int getHash()
            {
            return m_nHash;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Sets the type id of this type to uniquely identify this type.
         *
         * @param nTypeId  type id of this type
         */
        void setTypeId(int nTypeId)
            {
            m_nTypeId = nTypeId;
            }

        /**
         * Sets the version of this type.
         *
         * @param nVersionId  version identifier
         */
        void setVersion(int nVersionId)
            {
            m_nVersionId = nVersionId;
            }

        /**
         * Sets the hash to uniquely identify a ClassMetadata instance.
         *
         * @param nHash  hash of a ClassMetadata instance
         */
        void setHash(int nHash)
            {
            m_nHash = nHash;
            }

        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        public int hashCode()
            {
            // the class key will use the hash to determine uniqueness
            // whereas the ClassMetadata will not call this hashCode to
            // determine its hash but instead uses the appropriate elements
            int hash = HashHelper.hash(m_nTypeId, 31);
                hash = HashHelper.hash(m_nVersionId, hash);
                hash = HashHelper.hash(m_nHash, hash);
            return hash;
            }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object oThat)
            {
            if (this == oThat)
                {
                return true;
                }
            
            if (!(oThat instanceof TypeKey))
                {
                return false;
                }

            TypeKey that = (TypeKey) oThat;

            return m_nVersionId == that.getVersionId() && m_nTypeId == that.getTypeId()
                    && m_nHash == that.getHash();
            }

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            return String.format("%s (typeId=%d, versionId=%d, hash=%d)",
                    ClassHelper.getSimpleName(getClass()),
                    m_nTypeId, m_nVersionId, m_nHash);
            }

        // ----- data members -----------------------------------------------

        /**
         * The POF user type id this key represents.
         */
        private int m_nTypeId;

        /**
         *  The version of this type.
         */
        private int m_nVersionId;

        /**
         * A hash that uniquely identifies the structure of this class.
         */
        private int m_nHash;
        }

    // ----- inner class: ClassAttribute ------------------------------------

    /**
     * An {@link AttributeMetadata} implementation acting as a container for
     * attribute inspection and invocation.
     *
     * @see AttributeMetadata
     */
    public class ClassAttribute
            implements AttributeMetadata<T>, Comparable<ClassAttribute>
        {
        
        // ----- AttributeMetadata interface --------------------------------

        /**
         * {@inheritDoc}
         */
        public String getName()
            {
            return m_sName;
            }

        /**
         * {@inheritDoc}
         */
        public int getVersionId()
            {
            return m_nVersionId;
            }

        /**
         * {@inheritDoc}
         */
        public int getIndex()
            {
            return m_nIndex;
            }

        /**
         * {@inheritDoc}
         */
        public Codec getCodec()
            {
            return m_codec;
            }

        /**
         * {@inheritDoc}
         */
        public Object get(T pofType)
            {
            assertState();

            return m_invocationStrategy.get(pofType);
            }

        /**
         * {@inheritDoc}
         */
        public void set(T pofType, Object value)
            {
            assertState();

            m_invocationStrategy.set(pofType, value);
            }

        // ----- Comparable interface ---------------------------------------

        /**
         * Sorting of attributes is determined by:
         * <ol>
         *   <li>version</li>
         *   <li>index</li>
         *   <li>name</li>
         * </ol>
         *
         * @param  o  class attribute to compare against
         *
         * @return a  negative integer, zero, or a positive integer as this
         *		      object is less than, equal to, or greater than the
         *            specified object.
         */
        public int compareTo(ClassAttribute o)
            {
            if (o == this)
                {
                return 0;
                }

            int n = m_nVersionId - o.getVersionId();
            if (n == 0)
                {
                n = m_nIndex - o.getIndex();
                if (n == 0)
                    {
                    String sThis = m_sName;
                    String sThat = o.getName();
                    n = sThis == null
                            ? (sThat == null ? 0 : -1)
                            : (sThat == null ? 1 : sThis.compareTo(sThat));
                    }
                }

            return n;
            }

        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        public int hashCode()
            {
            int hash = HashHelper.hash(m_nVersionId, 31);
                hash = HashHelper.hash(m_nIndex, hash);
                hash = HashHelper.hash(m_sName, hash);
            return hash;
            }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object oThat)
            {
            if (this == oThat)
                {
                return true;
                }
            if (!(oThat instanceof AttributeMetadata))
                {
                return false;
                }

            AttributeMetadata that = (AttributeMetadata) oThat;

            return m_nVersionId == that.getVersionId() && m_nIndex == that.getIndex()
                   && Base.equals(m_sName, that.getName());
            }

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            return String.format("%s (name=%s, version=%d, index=%d)",
                    ClassHelper.getSimpleName(getClass()),
                    m_sName, m_nVersionId, m_nIndex);
            }

        // ----- accessors --------------------------------------------------

        /**
         * Specify the attribute name.
         *
         * @param sName  the normalized name of the attribute
         */
        void setName(String sName)
            {
            m_sName = sName;
            }

        /**
         * Specify the version.
         *
         * @param nVersionId  integer representing the version of this 
         *                    attribute metadata
         */
        void setVersionId(int nVersionId)
            {
            m_nVersionId = nVersionId;
            }

        /**
         * Specify the index.
         *
         * @param nIndex  to identify this attribute's position in a sequence
         */
        void setIndex(int nIndex)
            {
            m_nIndex = nIndex;
            }

        /**
         * Specify the codec.
         *
         * @param codec  the {@link Codec} used to (de)serialize this
         *               attribute
         */
        void setCodec(Codec codec)
            {
            m_codec = codec;
            }

        /**
         * Specify an {@link InvocationStrategy}.
         *
         * @param invocationStrategy  the invocation strategy to use to get
         *                            and set values
         */
        void setInvocationStrategy(InvocationStrategy<T,Object> invocationStrategy)
            {
            m_invocationStrategy = invocationStrategy;
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Ensures required state has been correctly set or throws an
         * {@link IllegalStateException}.
         *
         * @throws {@link IllegalStateException}  if member variables m_class
         *         or m_invocationStrategy are null
         */
        protected void assertState()
            {
            if (m_invocationStrategy == null)
                {
                throw new IllegalStateException("ClassMetadata attribute requires an invocation strategy");
                }
            }
        
        // ----- data members -----------------------------------------------

        /**
         * The unique name within this type representing this attribute.
         */
        private String m_sName;

        /**
         * The version of this type information.
         */
        private int m_nVersionId;

        /**
         * Index to identify this attribute's position in a sequence.
         */
        private int m_nIndex;

        /**
         * A {@link Codec} allows reading/writing specific types from/to
         * {@code PofReader}s and {@code PofWriter}s.
         */
        private Codec m_codec;
        
        /**
         * An abstraction on how an attribute is invoked to return its value
         * or replace its value.
         */
        private InvocationStrategy<T, Object> m_invocationStrategy;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Java representation of the type structure.
     */
    private Class<T> m_clz;

    /**
     * The unique identifier for this type.
     */
    private TypeKey m_key;

    /**
     * All attributes within this typeIterator.
     */
    private Set<AttributeMetadata<T>> m_setAttr = new TreeSet<AttributeMetadata<T>>();
    
    /**
     * A reference store for efficient lookup from attribute name to metadata
     */
    private Map<String, AttributeMetadata> m_mapAttrByName = new HashMap<String, AttributeMetadata>();
    }
