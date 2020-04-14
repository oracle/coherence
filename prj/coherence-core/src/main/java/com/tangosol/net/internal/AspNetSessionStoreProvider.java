/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.io.pof.reflect.PofValue;
import com.tangosol.io.pof.reflect.PofValueParser;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.DaemonThreadFactory;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;
import com.tangosol.util.MapEvent;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.ConditionalRemove;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
* Support classes needed by ASP.NET session store implementation.
*
* @author as  2009.09.21
*/
public class AspNetSessionStoreProvider
    {
    // ----- inner class: SessionKey ----------------------------------------

    /**
    * Session key.
    */
    public static class SessionKey
            implements PortableObject
        {
        // ---- PortableObject implementation ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sApplicationId = in.readString(0);
            m_sSessionId     = in.readString(1);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sApplicationId);
            out.writeString(1, m_sSessionId);
            }

        // ---- Object methods ------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o instanceof SessionKey)
                {
                SessionKey key = (SessionKey) o;
                return m_sApplicationId.equals(key.m_sApplicationId)
                       && m_sSessionId.equals(key.m_sSessionId);
                }

            return false;
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return m_sApplicationId.hashCode() ^ m_sSessionId.hashCode();
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return m_sApplicationId + ":" + m_sSessionId;
            }

        // ---- accessors -----------------------------------------------

        /**
        * Return application identifier.
        *
        * @return application identifier
        */
        public String getApplicationId()
            {
            return m_sApplicationId;
            }

        /**
        * Return session identifier.
        *
        * @return session identifier
        */
        public String getSessionId()
            {
            return m_sSessionId;
            }

        // ---- data members --------------------------------------------

        /**
        * Application identifier.
        */
        private String m_sApplicationId;

        /**
        * Session identifier.
        */
        private String m_sSessionId;
        }


    // ----- inner class: ExternalAttributeKey ------------------------------

    /**
    * External attribute key.
    */
    public static class ExternalAttributeKey
            implements PortableObject, KeyAssociation
        {
        // ---- constructors --------------------------------------------

        /**
        * Deserialization constructor.
        */
        public ExternalAttributeKey()
            {
            }

        /**
        * Construct ExternalAttributeKey instance.
        *
        * @param sessionKey     session key
        * @param sAttribute  attribute name
        */
        public ExternalAttributeKey(SessionKey sessionKey, String sAttribute)
            {
            m_sessionKey = sessionKey;
            m_sAttribute = sAttribute;
            }

        // ---- KeyAssociation implementation ---------------------------

        /**
        * {@inheritDoc}
        */
        public Object getAssociatedKey()
            {
            return m_sessionKey;
            }

        // ---- PortableObject implementation ---------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sessionKey     = (SessionKey) in.readObject(0);
            m_sAttribute = in.readString(1);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_sessionKey);
            out.writeString(1, m_sAttribute);
            }

        // ---- Object methods ------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o instanceof ExternalAttributeKey)
                {
                ExternalAttributeKey key = (ExternalAttributeKey) o;
                return m_sAttribute.equals(key.m_sAttribute)
                       && m_sessionKey.equals(key.m_sessionKey);
                }

            return false;
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return m_sessionKey.hashCode() ^ m_sAttribute.hashCode();
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return m_sessionKey + ":" + m_sAttribute;
            }

        // ---- accessors -----------------------------------------------

        /**
        * Return session key.
        *
        * @return session key
        */
        public SessionKey getSessionKey()
            {
            return m_sessionKey;
            }

        /**
        * Return attribute name.
        *
        * @return attribute name
        */
        public String getAttributeName()
            {
            return m_sAttribute;
            }

        // ---- data members --------------------------------------------

        /**
        * Session key.
        */
        private SessionKey m_sessionKey;

        /**
        * Attribute name.
        */
        private String m_sAttribute;
        }


    // ----- inner class: SessionHolder -------------------------------------

    /**
    * Session holder.
    */
    public static class SessionHolder
        implements PortableObject
        {
        /**
        * Increment the current version.
        */
        public void incrementVersion()
            {
            ++m_nVersion;
            }

        /**
        * Lock the holder.
        */
        public void lock()
            {
            assert m_nLockId == 0;

            m_nLockId = ++m_nVersion;
            m_dtLock  = new Date();
            }

        /**
        * Unlock the holder.
        */
        public void unlock()
            {
            // no assertion here -- unlock is always safe and can be called
            // on a holder that is not locked
            m_nLockId = 0;
            m_dtLock  = null;
            }

        // ---- PortableObject implementation ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader reader)
                throws IOException
            {
            m_nVersion       = reader.readLong(VERSION);
            m_nLockId        = reader.readLong(LOCK_ID);
            m_dtLock         = reader.readDate(LOCK_TIME);
            m_fInitialized   = reader.readBoolean(INITIALIZED);
            m_cTimeoutMillis = reader.readLong(TIMEOUT);
            m_binItems       = reader.readBinary(ITEMS);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer)
                throws IOException
            {
            writer.writeLong    (VERSION, m_nVersion);
            writer.writeLong    (LOCK_ID, m_nLockId);
            writer.writeDateTime(LOCK_TIME, m_dtLock);
            writer.writeBoolean (INITIALIZED, m_fInitialized);
            writer.writeLong    (TIMEOUT, m_cTimeoutMillis);
            writer.writeBinary  (ITEMS, m_binItems);
            }

        // ---- Object methods ------------------------------------------

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return "SessionHolder{" +
                   "Version="       + m_nVersion +
                   ", LockId="      + m_nLockId +
                   ", LockTime="    + m_dtLock +
                   ", Initialized=" + m_fInitialized +
                   ", Timeout="     + m_cTimeoutMillis +
                   ", Items="       + m_binItems +
                   '}';
            }

        // ---- accessors -----------------------------------------------

        /**
        * Return the current version identifier.
        *
        * @return the current version identifier
        */
        public long getVersion()
            {
            return m_nVersion;
            }

        /**
        * Return the current lock identifier.
        *
        * @return the current lock identifier
        */
        public long getLockId()
            {
            return m_nLockId;
            }

        /**
        * Determine if the holder is locked.
        *
        * @return true if the holder is locked; false otherwise
        */
        public boolean isLocked()
            {
            return m_nLockId != 0;
            }

        /**
        * Return the date that the holder was locked.
        *
        * @return the date that the holder was locked
        */
        public Date getLockTime()
            {
            return m_dtLock;
            }

        /**
        * Determine if the holder has been initialized.
        *
        * @return true if the holder has been initialized; false otherwise
        */
        public boolean isInitialized()
            {
            return m_fInitialized;
            }

        /**
        * Set the initialized flag.
        *
        * @param fInitialized  the new value of the initialized flag
        */
        public void setInitialized(boolean fInitialized)
            {
            m_fInitialized = fInitialized;
            }

        /**
        * Return the session timeout in milliseconds.
        *
        * @return the session timeout in milliseconds
        */
        public long getTimeoutMillis()
            {
            return m_cTimeoutMillis;
            }

        /**
        * Set the session timeout in milliseconds.
        *
        * @param cMillis  the session timeout in milliseconds
        */
        public void setTimeoutMillis(long cMillis)
            {
            m_cTimeoutMillis = cMillis;
            }

        /**
        * Return the serialized session items.
        *
        * @return  the serialized session items
        */
        public Binary getItems()
            {
            return m_binItems;
            }

        /**
        * Set the serialized session items.
        *
        * @param binItems  the serialized session items
        */
        public void setItems(Binary binItems)
            {
            m_binItems = binItems;
            }

        // ---- data members --------------------------------------------

        /**
        * Object version.
        */
        private long m_nVersion;

        /**
        * Lock identifier.
        */
        private long m_nLockId;

        /**
        * Lock time.
        */
        private Date m_dtLock;

        /**
        * Flag specifying whether this session is initialized.
        */
        private boolean m_fInitialized;

        /**
        * Session timeout (in milliseconds).
        */
        private long m_cTimeoutMillis;

        /**
        * Serialized session items.
        */
        private Binary m_binItems;

        // ---- POF constants -------------------------------------------

        /**
        * POF index for the version property.
        */
        public static final int VERSION     = 0;

        /**
        * POF index for the lock ID property.
        */
        public static final int LOCK_ID     = 1;

        /**
        * POF index for the lock time property.
        */
        public static final int LOCK_TIME   = 2;

        /**
        * POF index for the initialized property.
        */
        public static final int INITIALIZED = 3;

        /**
        * POF index for the session item property.
        */
        public static final int TIMEOUT     = 4;

        /**
        * POF index for the session items property.
        */
        public static final int ITEMS       = 5;
        }


    // ----- inner class: AbstractSessionProcessor --------------------------

    /**
    * Abstract base class for session processors.
    */
    public static abstract class AbstractSessionProcessor
            extends AbstractProcessor
            implements PortableObject
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        protected AbstractSessionProcessor()
            {
            }

        // ----- EntryProcessor interface -------------------------------

        /**
        * {@inheritDoc}
        */
        public Object process(InvocableMap.Entry entry)
            {
            m_binEntry = (BinaryEntry) entry;
            return processInternal(m_binEntry);
            }

        // ----- PortableObject interface -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        // ----- helper methods -----------------------------------------


        /**
        * Abstract process method that must be impleented by all concrete
        * subclasses.
        *
        * @param binEntry  the BinaryEntry being processed
        *
        * @return the result of processing
        */
        protected abstract Object processInternal(BinaryEntry binEntry);

        /**
        * Reset the expiry for the current entry.
        *
        * @param cMillis  the expiry timeout in milliseconds
        */
        protected void resetSessionTimeout(long cMillis)
            {
            BinaryEntry binEntry = m_binEntry;
            binEntry.expire(cMillis);
            }


        // ---- data members --------------------------------------------

        /**
        * The BinaryEntry being processed.
        */
        private transient BinaryEntry m_binEntry;
        }


    // ----- inner class: AcquireSessionProcessor ---------------------------

    /**
    * Entry processor that acquires a session for exclusive access.
    */
    public static class AcquireSessionProcessor
            extends AbstractSessionProcessor
        {
        // ----- AbstractSessionProcessor methods -----------------------

        /**
        * {@inheritDoc}
        */
        protected Object processInternal(BinaryEntry binEntry)
            {
            SessionHolder holder = (SessionHolder) binEntry.getValue();
            if (holder == null)
                {
                return null;
                }

            if (holder.isLocked())
                {
                // remove items, as we don't need to send them over the wire
                holder.setItems(null);
                }
            else
                {
                holder.lock();
                binEntry.setValue(holder, false);
                }

            resetSessionTimeout(holder.getTimeoutMillis());

            return holder;
            }
        }


    // ----- inner class: ReleaseSessionProcessor ---------------------------

    /**
    * Entry processor that releases the exclusive lock on a session.
    */
    public static class ReleaseSessionProcessor
            extends AbstractSessionProcessor
        {
        // ----- AbstractSessionProcessor methods -----------------------

        /**
        * {@inheritDoc}
        */
        protected Object processInternal(BinaryEntry binEntry)
            {
            SessionHolder holder = (SessionHolder) binEntry.getValue();

            if (holder != null && m_nLockId == holder.getLockId())
                {
                holder.unlock();
                binEntry.setValue(holder, false);
                resetSessionTimeout(holder.getTimeoutMillis());
                }

            return null;
            }

        // ----- PortableObject interface -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_nLockId = in.readLong(0);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeLong(0, m_nLockId);
            }

        // ----- data members -------------------------------------------

        /**
        * The identifier of the lock to release.
        */
        private long m_nLockId;
        }


    // ----- inner class: SaveSessionProcessor ------------------------------

    /**
     * Entry processor that updates session items and releases the lock.
     */
    public static class SaveSessionProcessor
            extends AbstractSessionProcessor
        {
        // ----- AbstractSessionProcessor methods -----------------------

        /**
        * {@inheritDoc}
        */
        protected Object processInternal(BinaryEntry binEntry)
            {
            SessionHolder holder = (SessionHolder) binEntry.getValue();
            if (holder == null || m_fNewSession)
                {
                holder = new SessionHolder();
                }
            else if (m_nLockId != holder.getLockId())
                {
                // lock id doesn't match, return
                return null;
                }

            holder.setTimeoutMillis(m_cTimeoutMillis);
            holder.setInitialized(m_fInitialized);
            if (m_binItems != null)
                {
                holder.setItems(m_binItems);
                }
            if (m_mapExternalAttributes != null && m_mapExternalAttributes.size() > 0)
                {
                storeExternalAttributes(binEntry);
                }
            if (m_collObsoleteExternalAttributes != null
                    && m_collObsoleteExternalAttributes.size() > 0)
                {
                removeObsoleteExternalAttributes(binEntry);
                }

            holder.unlock();
            binEntry.setValue(holder, false);
            resetSessionTimeout(holder.getTimeoutMillis());

            return null;
            }

        // ----- PortableObject interface -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_nLockId                        = in.readLong(0);
            m_fNewSession                    = in.readBoolean(1);
            m_fInitialized                   = in.readBoolean(2);
            m_cTimeoutMillis                 = in.readLong(3);
            m_binItems                       = in.readBinary(4);
            m_mapExternalAttributes          = in.readMap(5, new LiteMap());
            m_collObsoleteExternalAttributes = in.readCollection(6, new ArrayList());
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeLong      (0, m_nLockId);
            out.writeBoolean   (1, m_fNewSession);
            out.writeBoolean   (2, m_fInitialized);
            out.writeLong      (3, m_cTimeoutMillis);
            out.writeBinary    (4, m_binItems);
            out.writeMap       (5, m_mapExternalAttributes, String.class, Binary.class);
            out.writeCollection(6, m_collObsoleteExternalAttributes, String.class);
            }

        // ----- helper methods -----------------------------------------

        /**
        * Store any external or "split" session attributes.
        *
        * @param binEntry  the entry being processed
        */
        protected void storeExternalAttributes(BinaryEntry binEntry)
            {
            BackingMapManagerContext ctx = binEntry.getContext();

            SessionKey sessionKey = (SessionKey) binEntry.getKey();
            Map        mapCache   = ctx.getBackingMapContext(EXT_ATTR_CACHE_NAME).getBackingMap();
            Map        mapAttr    = m_mapExternalAttributes;
            Converter  convKey    = ctx.getKeyToInternalConverter();
            Converter  convValue  = ctx.getValueToInternalConverter();

            for (Iterator iter = mapAttr.keySet().iterator(); iter.hasNext(); )
                {
                String sAttr = (String) iter.next();
                ExternalAttributeKey key = new ExternalAttributeKey(sessionKey, sAttr);
                mapCache.put(convKey.convert(key),
                             convValue.convert(mapAttr.get(sAttr)));
                }
            }

        /**
        * Remove any obsolete external or "split" session attributes.
        *
        * @param binEntry  the entry being processed
        */
        protected void removeObsoleteExternalAttributes(BinaryEntry binEntry)
            {
            BackingMapManagerContext ctx = binEntry.getContext();

            SessionKey sessionKey  = (SessionKey) binEntry.getKey();
            Map        mapCache    = ctx.getBackingMapContext(EXT_ATTR_CACHE_NAME).getBackingMap();
            Collection collObsAttr = m_collObsoleteExternalAttributes;
            Converter  convKey     = ctx.getKeyToInternalConverter();

            for (Iterator iter = collObsAttr.iterator(); iter.hasNext(); )
                {
                String sAttr = (String) iter.next();
                ExternalAttributeKey key = new ExternalAttributeKey(sessionKey, sAttr);
                mapCache.remove(convKey.convert(key));
                }
            }

        // ----- data members -------------------------------------------

        /**
        * The session lock ID.
        */
        private long m_nLockId;

        /**
        * True if the session is new.
        */
        private boolean m_fNewSession;

        /**
        * True if the holder has been initialized.
        */
        private boolean m_fInitialized;

        /**
        * The session timeout in milliseconds.
        */
        private long m_cTimeoutMillis;

        /**
        * The serialize session items.
        */
        private Binary  m_binItems;

        /**
        * Optional external or "split" session attributes.
        */
        private Map m_mapExternalAttributes;

        /**
        * Optional obsolete external or "split" session attributes.
        */
        private Collection m_collObsoleteExternalAttributes;
        }


    // ----- inner class: ResetSessionTimeoutProcessor ----------------------

    /**
    * Entry processor that resets session timeout.
    */
    public static class ResetSessionTimeoutProcessor
            extends AbstractSessionProcessor
        {
        // ----- AbstractSessionProcessor methods -----------------------

        /**
        * {@inheritDoc}
        */
        protected Object processInternal(BinaryEntry binEntry)
            {
            Binary value = binEntry.getBinaryValue();
            if (value != null)
                {
                PofValue pofEntry = PofValueParser.parse(value,
                        (PofContext) binEntry.getSerializer());

                long cMillis = ((Long) pofEntry.getChild(SessionHolder.TIMEOUT)
                        .getValue()).longValue();
                resetSessionTimeout(cMillis);
                }

            return null;
            }
        }


    // ----- inner class: SessionCleanupListener ----------------------------

    /**
    * Session cleanup backing map listener.
    * <p>
    * This listener is responsible for cleaning up external attributes when
    * the session is removed from the cache, regardless of whether the
    * removal was triggered by explicit client request or by eviction.
    */
    public static class SessionCleanupListener
            extends AbstractMapListener
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a new SessionCleanupListener that will register a backing
        * map listener for the given context.
        *
        * @param context  the context
        */
        public SessionCleanupListener(BackingMapManagerContext context)
            {
            m_ctx = context;
            }

        // ----- MapListener interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public void entryDeleted(MapEvent evt)
            {
            final Converter  conv       = m_ctx.getKeyFromInternalConverter();
            final SessionKey sessionKey = (SessionKey) conv.convert(evt.getKey());
            final Filter     filter     = new EqualsFilter(new ReflectionExtractor(
                    "getSessionKey", null, ReflectionExtractor.KEY), sessionKey);

            EXECUTOR.execute(new Runnable()
                {
                public void run()
                    {
                    NamedCache cache = CacheFactory.getCache(EXT_ATTR_CACHE_NAME);
                    cache.invokeAll(new KeyAssociatedFilter(filter, sessionKey),
                                    new ConditionalRemove(AlwaysFilter.INSTANCE));
                    }
                });
            }

        // ----- data members -------------------------------------------

        /**
        * The context used to register a backing map listener.
        */
        private BackingMapManagerContext m_ctx;

        // ----- constants ----------------------------------------------

        /**
        * An Executor used to remove external attributes.
        */
        private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(
                new DaemonThreadFactory("AspNetSessionCleanupThread-"));
        }


    // ----- constants ------------------------------------------------------

    /**
    * The name of the cache that holds external session attributes.
    */
    private static final String EXT_ATTR_CACHE_NAME   = "aspnet-session-overflow";
    }
