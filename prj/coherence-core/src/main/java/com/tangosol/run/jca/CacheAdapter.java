/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.jca;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.TransactionMap;

import javax.naming.Context;
import javax.naming.InitialContext;

import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

/**
* CacheAdapter encapsulates the operations neccessary to communicate
* with <b>Coherence&#8482;</b> resource adapter (coherence-tx.rar).
* The Coherence resource adapter, in turn, is a gateway into the Coherence
* clustered services, so some methods closely resemble methods at
* {@link CacheFactory} class.
* <p>
* In a simplest form a client code would look like:
* <pre><code>
*   Context ctx = new InitialContext();
*
*   CacheAdapter adapter = new CacheAdapter(ctx, "tangosol.coherenceTx",
*       CacheAdapter.CONCUR_OPTIMISTIC, CacheAdapter.TRANSACTION_REPEATABLE_GET, 0);
*   try
*       {
*       NamedCache map = adapter.getNamedCache("MyCache", getClass().getClassLoader(),
*           new SimpleValidator(), false);
*
*       // perform operations on the map
*       }
*   finally
*       {
*       adapter.close();
*       }
* </code></pre>
*
* Note: the CacheAdapter is intended to be used by one and only one thread
* at the time and is not thread safe.
*
* @author gg 2002.04.14
* @since Coherence 1.2
*/

public class CacheAdapter
        extends Base
    {
    /**
    * Construct the adapter using a default InitialContext,
    * standard JNDI name, "optimistic" concurrency, "committed" isolation
    * level an no timeout.
    */
    public CacheAdapter()
        {
        this(null, null, CONCUR_OPTIMISTIC, TRANSACTION_GET_COMMITTED, 0);
        }

    /**
    * Construct the adapter using the specified Context and JNDI name
    *
    * @param ctx        the Context object to use for lookup operation;
    *                   if null, the default InitialiContext is used
    * @param sJndiName  the JNDI name of the Coherence resource adapter;
    *                   if null, the default name ("tangosol.coherenceTx")
    *                   is used
    * @param nConcur    the default concurrency value
    * @param nIsolation the default transaction isolation value
    * @param nTimeout   the default transaction timeout value (in seconds)
    */
    public CacheAdapter(Context ctx, String sJndiName,
                        int nConcur, int nIsolation, int nTimeout)
        {
        try
            {
            if (ctx == null)
                {
                ctx = new InitialContext();
                }
            if (sJndiName == null)
                {
                sJndiName = "tangosol.coherenceTx";
                }

            m_factory     = (ConnectionFactory) ctx.lookup(sJndiName);
            m_nConcur     = nConcur;
            m_nIsolation  = nIsolation;
            m_nTimeout    = nTimeout;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }

        if (m_factory == null)
            {
            throw new IllegalArgumentException(
                    "Failed to find a ConnectionFactory: " + sJndiName);
            }
        }

    /**
    * Obtain an instance of a ReplicatedCache.
    * <p>
    * When called within a transaction, the returned cache will be
    * transactional, controlled by the container's transaction coordinator.
    * When called outside of a transaction, the returned cache will be
    * non-transactional, which is equivalent to obtaining the instance
    * directly from CacheFactory.
    *
    * @param sName   cache name (unique across the cluster).  If the NamedCache
    *                with the specified name already exists, a reference to
    *                the same object will be returned in the same transactional
    *                context
    * @param loader  ClassLoader that should be used to deserialize objects
    *                inserted in the map by other members of the cluster
    *
    * @return NamedCache interface of the ReplicatedCache service
    *
    * @throws IllegalStateException if the adapter is already connected to a
    *                               different service
    *
    * @deprecated use {@link #getNamedCache} instead.
    */
    public NamedCache getReplicatedCache(String sName, ClassLoader loader)
        {
        if (isConnected())
            {
            if (!CacheService.TYPE_REPLICATED.equals(m_sServiceType))
                {
                throw new IllegalStateException("Service type mismatch: " + m_sServiceType);
                }
            }
        else
            {
            connect("ReplicatedCache", CacheService.TYPE_REPLICATED, null, null);
            }
        return getNamedCache(sName, loader, null, false);
        }

    /**
    * Obtain an instance of a DistributedCache.
    * <p>
    * When called within a transaction, the returned cache will be
    * transactional, controlled by the container's transaction coordinator.
    * When called outside of a transaction, the returned cache will be
    * non-transactional, which is equivalent to obtaining the instance
    * directly from CacheFactory.
    *
    * @param sName   cache name (unique across the cluster).  If the NamedCache
    *                with the specified name already exists, a reference to
    *                the same object will be returned in the same transactional
    *                context
    * @param loader  ClassLoader that should be used to deserialize objects
    *                inserted in the map by other members of the cluster
    *
    * @return NamedCache interface of the DistributedCache service
    *
    * @throws IllegalStateException if the adapter is already connected to a
    *                               different service
    *
    * @deprecated use {@link #getNamedCache} instead.
    */
    public NamedCache getDistributedCache(String sName, ClassLoader loader)
        {
        if (isConnected())
            {
            if (!CacheService.TYPE_DISTRIBUTED.equals(m_sServiceType))
                {
                throw new IllegalStateException("Service type mismatch: " + m_sServiceType);
                }
            }
        else
            {
            connect("DistributedCache", CacheService.TYPE_DISTRIBUTED, null, null);
            }
        return getNamedCache(sName, loader, null, true);
        }

    /**
    * Connect the adapter to the specified clustered service. The service
    * with the specified name must be running for this call to succeed.
    *
    * @param sServiceName service name (unique across the cluster)
    * @param sUserName    name of the user establishing a connection (optional)
    * @param sPassword    password for the user establishing a connection (optional)
    *
    * @throws IllegalArgumentException if the specified service is not running
    * @throws IllegalStateException if the adapter is already connected or
    */
    public void connect(String sServiceName, String sUserName, String sPassword)
        {
        connect(sServiceName, null, sUserName, sPassword);
        }

    /**
    * Connect the adapter to the specified clustered service. If the service
    * with the specified name is not running an attempt will be made to start
    * that service [default configuration]. If the name and type are not
    * specified, a ConfigurableCacheFactory will be used to obtain the
    * NamedCache instances.
    *
    * @param sServiceName service name (unique across the cluster)
    * @param sServiceType service type (i.e. CacheService.TYPE_REPLICATED or
    *                     CacheService.TYPE_DISTRIBUTED)
    * @param sUserName    name of the user establishing a connection (optional)
    * @param sPassword    password for the user establishing a connection (optional)
    *
    * @throws IllegalArgumentException if the specified service cannot be found
    * @throws IllegalStateException if the adapter is already connected
    */
    public void connect(String sServiceName, String sServiceType, String sUserName, String sPassword)
        {
        if (isConnected())
            {
            throw new IllegalStateException("Already connected");
            }

        try
            {
            ConnectionSpec infoCache = new CacheConnectionSpec(
                sServiceName, sServiceType, sUserName, sPassword,
                m_nConcur, m_nIsolation, m_nTimeout);

            m_connection   = m_factory.getConnection(infoCache);
            m_sServiceName = sServiceName;
            m_sServiceType = sServiceType;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Obtain an instance of a NamedCache. The NamedCache will be retrieved
    * from the current ConfigurableCacheFactory using
    * {@link CacheFactory#getCache(String sName, ClassLoader loader)
    * CacheFactory.getCache(sName, loader)} unless the adapter is already
    * connected to a specific cache service, in which case the
    * corresponding NamedCache will be retrieved using
    * {@link CacheService#ensureCache(String sName, ClassLoader loader)
    * service.ensureCache(sName, loader)}.
    * <p>
    * When called within a transaction, the returned cache will be
    * transactional, controlled by the container's transaction coordinator.
    * When called outside of a transaction, the returned cache will be
    * non-transactional, which is equivalent to obtaining the instance
    * directly from CacheFactory.
    *
    * @param sName   cache name (unique across the cluster).
    *                If the NamedCache with the specified name already
    *                exists, a reference to the same object will be
    *                returned in the same transactional context
    * @param loader  ClassLoader that should be used to deserialize objects
    *                inserted in the map by other members of the cluster
    * @return NamedCache object
    */
    public NamedCache getNamedCache(String sName, ClassLoader loader)
        {
        return getNamedCache(sName, loader, null, false);
        }

    /**
    * Obtain an instance of a NamedCache. The NamedCache will be retrieved
    * from the current ConfigurableCacheFactory using
    * {@link CacheFactory#getCache(String sName, ClassLoader loader)
    * CacheFactory.getCache(sName, loader)} unless the adapter is already
    * connected to a specific cache service, in which case the
    * corresponding NamedCache will be retrieved using
    * {@link CacheService#ensureCache(String sName, ClassLoader loader)
    * service.ensureCache(sName, loader)}.
    * <p>
    * When called within a transaction, the returned cache will be
    * transactional, controlled by the container's transaction coordinator.
    * When called outside of a transaction, the returned cache will be
    * non-transactional, which is equivalent to obtaining the instance
    * directly from CacheFactory.
    *
    * @param sName      cache name (unique across the cluster).
    *                   If the NamedCache with the specified name already
    *                   exists, a reference to the same object will be
    *                   returned in the same transactional context
    * @param loader     ClassLoader that should be used to deserialize objects
    *                   inserted in the map by other members of the cluster
    * @param validator  the Validator object to be used to enlist and validate
    *                   transactional resources; this parameter is only used
    *                   within transactional context and only for optimistic
    *                   concurrency
    * @param fImmutable specifies whether or not the values kept in this cache
    *                   are known to be immutable; this parameter is only used
    *                   within transactional context
    * @return NamedCache object
    *
    * @see TransactionMap#setValidator(TransactionMap.Validator) TransactionMap.setValidator()
    * @see TransactionMap#setValuesImmutable(boolean) TransactionMap.setValuesImmutable()
    *
    * @since Coherence 2.3
    */
    public NamedCache getNamedCache(String sName, ClassLoader loader,
            TransactionMap.Validator validator, boolean fImmutable)
        {
        if (!isConnected())
            {
            connect(null, null, null, null);
            }

        try
            {
            Interaction   ix    = m_connection.createInteraction();
            RecordFactory rf    = m_factory.getRecordFactory();
            MappedRecord  mapIn = rf.createMappedRecord("InputRecord");

            mapIn.put("CacheName",   sName);
            mapIn.put("ClassLoader", loader);
            mapIn.put("Validator",   validator);
            mapIn.put("Immutable",   fImmutable ? Boolean.TRUE : Boolean.FALSE);

            return (NamedCache) ix.execute(null, mapIn);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Close the connection associated with this adapter.
    */
    public void close()
        {
        try
            {
            Connection con = m_connection;
            if (con != null)
                {
                con.close();
                m_connection = null;
                }
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Return the connection status.
    *
    * @return true if the adapter is currently connected; false otherwise.
    */
    protected boolean isConnected()
        {
        return m_connection != null;
        }


    // ----- Object methods  ------------------------------------------------

    /**
    * Return a human readable description of the CacheAdapter.
    *
    * @return a String representation of the CacheAdapter
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append("CacheAdapter{State=")
          .append(isConnected() ? "connected" : "closed");

        if (m_sServiceName != null)
            {
            sb.append(", CacheService=")
              .append(m_sServiceName);
            }

        sb.append(", Concurrency=");
        switch (m_nConcur)
            {
            case CONCUR_PESSIMISTIC:
                sb.append("CONCUR_PESSIMISTIC");
                break;
            case CONCUR_OPTIMISTIC:
                sb.append("CONCUR_OPTIMISTIC");
                break;
            case CONCUR_EXTERNAL:
                sb.append("CONCUR_EXTERNAL");
                break;
            default:
                sb.append("invalid");
                break;
            }

        sb.append(", Isolation=");
        switch (m_nIsolation)
            {
            case TRANSACTION_GET_COMMITTED:
                sb.append("TRANSACTION_GET_COMMITTED");
                break;
            case TRANSACTION_REPEATABLE_GET:
                sb.append("TRANSACTION_REPEATABLE_GET");
                break;
            case TRANSACTION_SERIALIZABLE:
                sb.append("TRANSACTION_SERIALIZABLE");
                break;
            default:
                sb.append("invalid");
                break;
            }

        sb.append(", Timeout=")
          .append(m_nTimeout);

        if (isConnected())
            {
            sb.append(", Connection=(")
              .append(m_connection.getClass().getName())
              .append(") ")
              .append(m_connection);
            }

        return sb.toString();
        }

    /**
    * Perform cleanup during garbage collection.
    */
    protected void finalize()
        {
        close();
        }


    // ----- inner class ----------------------------------------------------

    /**
    * CacheConnectionSpec is an implementation of {@link javax.resource.cci.ConnectionSpec}
    * interface used to pass the connection info to the adapter.
    *
    * @see javax.resource.cci.ConnectionFactory
    */
    /*
    * CacheConnectionSpec could not be implemented as an inner class due to the
    * invocation access exception while calling the accessors.
    */
    public static class CacheConnectionSpec
            implements ConnectionSpec
        {
        /**
        * Construct a ConnectionSpec based on the specified parameters.
        */
        public CacheConnectionSpec(String sServiceName, String sServiceType, String sUserName,
                String sPassword, int nConcur, int nIsolation, int nTimeout)
            {
            m_sServiceName = sServiceName;
            m_sServiceType = sServiceType;
            m_sUserName    = sUserName;
            m_sPassword    = sPassword;
            m_nConcur      = nConcur;
            m_nIsolation   = nIsolation;
            m_nTimeout     = nTimeout;
            }

        public String getServiceName() {return m_sServiceName;}
        public String getServiceType() {return m_sServiceType;}
        public String getUserName()    {return m_sUserName;   }
        public String getPassword()    {return m_sPassword;   }
        public int    getConcurrency() {return m_nConcur;     }
        public int    getIsolation()   {return m_nIsolation;  }
        public int    getTimeout()     {return m_nTimeout;    }

        private String m_sServiceName, m_sServiceType, m_sUserName, m_sPassword;
        private int    m_nConcur, m_nIsolation, m_nTimeout;
        }


    // ----- data fields ----------------------------------------------------

    /**
    * Default concurrency setting for transactions created using
    * this adapter.
    */
    private int                 m_nConcur;

    /**
    * Default isolation setting for transactions created using
    * this adapter.
    */
    private int                 m_nIsolation;

    /**
    * Default timeout setting for transactions created using
    * this adapter.
    */
    private int                 m_nTimeout;

    /**
    * ConnectionFactory for the Coherence resource adapter.
    */
    private ConnectionFactory   m_factory;

    /**
    * Currently active Connection for this adapter.
    */
    private Connection          m_connection;

    /**
    * CacheService name for the currently active connection.
    */
    private String              m_sServiceName;

    /**
    * CacheService type for the currently active connection.
    */
    private String              m_sServiceType;


    // ---- constants -------------------------------------------------------

    /**
    * Same as {@link TransactionMap#TRANSACTION_GET_COMMITTED}
    */
    public static final int TRANSACTION_GET_COMMITTED  = TransactionMap.TRANSACTION_GET_COMMITTED;

    /**
    * Same as {@link TransactionMap#TRANSACTION_REPEATABLE_GET}
    */
    public static final int TRANSACTION_REPEATABLE_GET = TransactionMap.TRANSACTION_REPEATABLE_GET;;

    /**
    * Same as {@link TransactionMap#TRANSACTION_SERIALIZABLE}
    */
    public static final int TRANSACTION_SERIALIZABLE   = TransactionMap.TRANSACTION_SERIALIZABLE;

    /**
    * Same as {@link TransactionMap#CONCUR_PESSIMISTIC}
    */
    public static final int CONCUR_PESSIMISTIC         = TransactionMap.CONCUR_PESSIMISTIC;

    /**
    * Same as {@link TransactionMap#CONCUR_OPTIMISTIC}
    */
    public static final int CONCUR_OPTIMISTIC          = TransactionMap.CONCUR_OPTIMISTIC;

    /**
    * Same as {@link TransactionMap#CONCUR_EXTERNAL}
    */
    public static final int CONCUR_EXTERNAL            = TransactionMap.CONCUR_EXTERNAL;
    }