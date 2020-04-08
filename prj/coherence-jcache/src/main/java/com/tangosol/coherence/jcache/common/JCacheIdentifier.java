/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.coherence.jcache.CoherenceBasedCache;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An internal class to represent the unique identity of a JCache cache.
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class JCacheIdentifier
        implements ExternalizableLite, PortableObject
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Constructs {@link JCacheIdentifier}
     *
     */
    public JCacheIdentifier()
        {
        // for PortableObject
        }

    /**
     * Constructs {@link JCacheIdentifier} from the internal encoded CoherenceBased cache map name.
     *
     *
     * @param sCanonicalCacheName canonical encoded CoherenceBased cache map name. Name encodes JCache Adapter
     *                            CacheManager URI, the JCache Adapter implementation type for the JCache map.
     *                            and the JCache Adapter JCache name.
     */
    public JCacheIdentifier(String sCanonicalCacheName)
        {
        final String DELIMITER = "[" + URI_NAME_SEPARATOR + "]+";
        final String PREFIXES = "(" + CoherenceBasedCache.JCACHE_PARTITIONED_CACHE_NAME_PREFIX + "|"
                                + CoherenceBasedCache.JCACHE_LOCAL_CACHE_NAME_PREFIX + ")";
        String   cacheNamePrefixStripped = sCanonicalCacheName.replaceAll(PREFIXES, "");
        String[] tokens                  = cacheNamePrefixStripped.split(DELIMITER);

        if (tokens.length == 2)
            {
            m_sCacheMgrURI = tokens[0];
            m_sCacheName   = tokens[1];
            }
        else
            {
            throw new IllegalAccessError("expected form of \"CacheManagerURI" + URI_NAME_SEPARATOR + "cacheName"
                                         + "\" for parameter sCanonicalCacheName, it had this invalid value instead "
                                         + sCanonicalCacheName);
            }

        }

    /**
     * Constructs  {@link JCacheIdentifier}
     *
     *
     * @param sMgrUri unique identifier for {@link javax.cache.CacheManager}
     * @param sCacheName JCache name
     */
    public JCacheIdentifier(String sMgrUri, String sCacheName)
        {
        m_sCacheMgrURI = sMgrUri;
        m_sCacheName   = sCacheName;
        }

    // ----- JCacheIdentifier methods ---------------------------------------

    /**
     * Get the JCache map name
     *
     * @return JCache name
     */
    public String getName()
        {
        return m_sCacheName;
        }

    /**
     * Get the JCache CacheManager URI, a unique identifier for the JCache CacheManager
     *
     * @return {@link javax.cache.CacheManager} URI context
     */
    public String getCacheManagerURI()
        {
        return m_sCacheMgrURI;
        }

    /**
     * Get the JCache Adapter internal name for this {@link JCacheIdentifier}
     *
     * @return internal JCache Adapter coherence-based map name.
     */
    public String getCanonicalCacheName()
        {
        return m_sCacheMgrURI + URI_NAME_SEPARATOR + m_sCacheName;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o == null || !(o instanceof JCacheIdentifier))
            {
            return false;
            }
        else
            {
            JCacheIdentifier id = (JCacheIdentifier) o;

            return m_sCacheMgrURI.equals(id.m_sCacheMgrURI) && m_sCacheName.equals(m_sCacheName);
            }
        }

    @Override
    public int hashCode()
        {
        return m_sCacheMgrURI.hashCode() + m_sCacheName.hashCode();
        }

    @Override
    public String toString()
        {
        return getCanonicalCacheName();
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        m_sCacheMgrURI = pofReader.readString(0);
        m_sCacheName   = pofReader.readString(1);
        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        pofWriter.writeObject(0, m_sCacheMgrURI);
        pofWriter.writeObject(1, m_sCacheName);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        m_sCacheMgrURI = dataInput.readUTF();
        m_sCacheName   = dataInput.readUTF();
        }

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        dataOutput.writeUTF(m_sCacheMgrURI);
        dataOutput.writeUTF(m_sCacheName);
        }

    // ----- constants ------------------------------------------------------

    private static final Character URI_NAME_SEPARATOR = '$';

    // ----- data members ---------------------------------------------------
    private String m_sCacheMgrURI;
    private String m_sCacheName;
    }
