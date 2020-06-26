/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.Constants;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.cache.Cache;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

/**
 * Represents the JCache metadata for a key and value pair stored in a {@link Cache}.
 * <p>
 * The meta info is stored as a Coherence decoration on the value in {@link }ParitionedCache}.
 * The meta info is a member of a {@link com.tangosol.coherence.jcache.localcache.LocalCacheValue};
 * <p>
 * {@link JCacheEntryMetaInf}s store and provide meta information about
 * a Cache Entry, including information for dealing with expiry.
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class JCacheEntryMetaInf
        implements ExternalizableLite, PortableObject
    {
    /**
     * Constructs a {@link JCacheEntryMetaInf}.
     */
    public JCacheEntryMetaInf()
        {
        // required for serialization
        }

    /**
     * Copy constructor
     *
     * @param metaInf
     */
    public JCacheEntryMetaInf(JCacheEntryMetaInf metaInf)
        {
        this.m_ldtCreation     = metaInf.m_ldtCreation;
        this.m_ldtAccess       = metaInf.m_ldtAccess;
        this.m_cAccess         = metaInf.m_cAccess;
        this.m_ldtModification = metaInf.m_ldtModification;
        this.m_cModification   = metaInf.m_cModification;
        this.m_ldtExpiry       = metaInf.m_ldtExpiry;
        }

    /**
     * Constructs an {@link JCacheEntryMetaInf}.
     *
     * @param ldtCreated    the time when the cache entry was created
     * @param expiryPolicy  the {@link ExpiryPolicy} to determine the expiry time
     */
    public JCacheEntryMetaInf(long ldtCreated, ExpiryPolicy expiryPolicy)
        {
        m_ldtCreation     = ldtCreated;
        m_ldtAccess       = ldtCreated;
        m_ldtModification = ldtCreated;
        m_cAccess         = 0;
        m_cModification   = 0;

        Duration duration;

        try
            {
            duration = expiryPolicy.getExpiryForCreation();
            }
        catch (Throwable e)
            {
            // default if JCache client provided expiry policy throws an exception.
            duration = Constants.DEFAULT_EXPIRY_DURATION;

            Logger.warn("defaulting to implemention-specifc ExpiryForCreation default due to handling unexpected exception in user-provided ExpiryPolicy:\n"
                        + Base.printStackTrace(e));
            }

        m_ldtExpiry = duration.getAdjustedTime(ldtCreated);

        }

    // ------ JCacheEntryMetaInf methods -------------------------------------

    /**
     * Gets the time (since the Epoc) in milliseconds since the internal value
     * was created.
     *
     * @return time in milliseconds (since the Epoc)
     */
    public long getCreationTime()
        {
        return m_ldtCreation;
        }

    /**
     * Gets the time (since the Epoc) in milliseconds since the internal value
     * was last accessed.
     *
     * @return time in milliseconds (since the Epoc)
     */
    public long getAccessTime()
        {
        return m_ldtAccess;
        }

    /**
     * Gets the number of times the internal value has been accessed.
     *
     * @return the access count
     */
    public long getAccessCount()
        {
        return m_cAccess;
        }

    /**
     * Gets the time (since the Epoc) in milliseconds since the internal value
     * was last modified.
     *
     * @return time in milliseconds (since the Epoc)
     */
    public long getModificationTime()
        {
        return m_ldtModification;
        }

    /**
     * Gets the number of times the internal value has been modified (set)
     *
     * @return the modification count
     */
    public long getModificationCount()
        {
        return m_cModification;
        }

    /**
     * Gets the time (since the Epoc) in milliseconds when the Cache Entry
     * associated with this value should be considered expired.
     *
     * @return time in milliseconds (since the Epoc)
     */
    public long getExpiryTime()
        {
        return m_ldtExpiry;
        }

    /**
     * Sets the time (since the Epoc) in milliseconds when the Cache Entry
     * associated with this value should be considered expired.
     *
     * @param expiryTime time in milliseconds (since the Epoc)
     */
    public void setExpiryTime(long expiryTime)
        {
        m_ldtExpiry = expiryTime;
        }

    /**
     * Determines if the Cache Entry associated with this value would be expired
     * at the specified time
     *
     * @param now time in milliseconds (since the Epoc)
     * @return true if the value would be expired at the specified time
     */
    public boolean isExpiredAt(long now)
        {
        return m_ldtExpiry > -1 && m_ldtExpiry <= now;
        }

    /**
     * Gets the internal value with the side-effect of updating the access time
     * to that which is specified and incrementing the access count.
     *
     * @param accessTime the time when the related value was accessed
     */
    public void setAccessTime(long accessTime)
        {
        m_ldtAccess = accessTime;
        m_cAccess++;
        }

    /**
     * Sets update the
     * modification time for the meta data for a key and value pair. Incrementing the
     * modification count.
     *
     *  * @param m_ldtModification the time when the value was modified
     */
    public void setModificationTime(long modificationTime)
        {
        m_ldtModification = modificationTime;
        m_cModification++;
        }

    /**
     * Update the {@link JCacheEntryMetaInf} based on the associated entry
     * being accessed at the specified time.
     *
     * @param ldtAccessed   the time the entry was accessed
     * @param expiryPolicy  the {@link ExpiryPolicy} for the {@link Cache}
     */
    public void accessed(long ldtAccessed, ExpiryPolicy expiryPolicy)
        {
        setAccessTime(ldtAccessed);

        Duration duration;

        try
            {
            duration = expiryPolicy.getExpiryForAccess();
            }
        catch (Throwable e)
            {
            // leave the expiry time untouched if expiry policy throws an exception
            duration = null;

            Logger.warn("handled unexpected exception in user-provided ExpiryPolicy:", e);
            }

        if (duration != null)
            {
            setExpiryTime(duration.getAdjustedTime(ldtAccessed));
            }
        }

    /**
     * Update the {@link JCacheEntryMetaInf} based on the associated entry
     * being modified at the specified time.
     *
     * @param ldtModified   the time the entry was accessed
     * @param expiryPolicy  the {@link ExpiryPolicy} for the {@link Cache}
     */
    public void modified(long ldtModified, ExpiryPolicy expiryPolicy)
        {
        setModificationTime(ldtModified);

        Duration duration;

        try
            {
            duration = expiryPolicy.getExpiryForUpdate();
            }
        catch (Throwable e)
            {
            // default expiry policy being followed.
            duration = null;

            Logger.warn("handled unexpected exception in user-provided ExpiryPolicy:", e);
            }

        if (duration != null)
            {
            setExpiryTime(duration.getAdjustedTime(ldtModified));
            }
        }

    // ------ ExternalizableLite interface ----------------------------------

    /**
     * read from datainput
     *
     * @param dataInput
     *
     * @throws IOException
     */
    @Override
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        m_ldtCreation     = dataInput.readLong();
        m_ldtAccess       = dataInput.readLong();
        m_cAccess         = dataInput.readLong();
        m_ldtModification = dataInput.readLong();
        m_cModification   = dataInput.readLong();
        m_ldtExpiry       = dataInput.readLong();
        }

    /**
     * write to
     *
     * @param dataOutput
     *
     * @throws IOException
     */

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        dataOutput.writeLong(m_ldtCreation);
        dataOutput.writeLong(m_ldtAccess);
        dataOutput.writeLong(m_cAccess);
        dataOutput.writeLong(m_ldtModification);
        dataOutput.writeLong(m_cModification);
        dataOutput.writeLong(m_ldtExpiry);
        }

    // ---- Object interface ------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder buf = new StringBuilder(50);
        long          now = Helper.getCurrentTimeMillis();

        buf.append("JCacheEntryMetaInf:[ created ");
        buf.append(now - m_ldtCreation);
        buf.append(" ms ago:");
        buf.append(" accessed ");
        buf.append(now - m_ldtAccess);
        buf.append(" ms ago:");
        buf.append(" modified ");
        buf.append(now - m_ldtModification);
        buf.append(" ms ago:");
        buf.append(" expires in ");
        buf.append(now - m_ldtExpiry);
        buf.append(" ms ");
        buf.append(" accessCnt=");
        buf.append(m_cAccess);
        buf.append(" modCnt=");
        buf.append(m_cModification);
        buf.append("]");

        return buf.toString();
        }

    // ----- PortableObject interface -----------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        m_ldtCreation     = pofReader.readLong(0);
        m_ldtAccess       = pofReader.readLong(1);
        m_cAccess         = pofReader.readLong(2);
        m_ldtModification = pofReader.readLong(3);
        m_cModification   = pofReader.readLong(4);
        m_ldtExpiry       = pofReader.readLong(5);
        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        pofWriter.writeLong(0, m_ldtCreation);
        pofWriter.writeLong(1, m_ldtAccess);
        pofWriter.writeLong(2, m_cAccess);
        pofWriter.writeLong(3, m_ldtModification);
        pofWriter.writeLong(4, m_cModification);
        pofWriter.writeLong(5, m_ldtExpiry);
        }

    // ----- data members -----------------------------------------------------

    /**
     * The time (since the Epoc) in milliseconds since the internal value was created.
     */
    private long m_ldtCreation;

    /**
     * The time (since the Epoc) in milliseconds since the internal value was
     * last accessed.
     */
    private long m_ldtAccess;

    /**
     * The number of times the interval value has been accessed.
     */
    private long m_cAccess;

    /**
     * The time (since the Epoc) in milliseconds since the internal value was
     * last modified.
     */
    private long m_ldtModification;

    /**
     * The number of times the internal value has been modified.
     */
    private long m_cModification;

    /**
     * The time (since the Epoc) in milliseconds when the Cache Entry associated
     * with this value should be considered expired.
     * <p>
     * A value of -1 indicates that the Cache Entry should never expire.
     */
    private long m_ldtExpiry;
    }
