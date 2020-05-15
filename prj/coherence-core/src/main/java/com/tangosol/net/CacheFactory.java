/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.security.LocalPermission;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.TransactionMap;

import java.lang.reflect.Method;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import java.util.function.Supplier;


/**
* Factory for the <b>Coherence&#8482;</b> cache product.
* <p>
* One of the most common functions provided by the CacheFactory is ability
* to obtain an instance of a cache. To get a cache reference use the
* {@link #getCache(String, ClassLoader)} or {@link #getCache(String)} methods.
* <p>
* When a cache retrieved by any of the above methods is no longer used,
* it is recommended to call {@link #releaseCache(NamedCache)} to release
* the associated resources.  To destroy all instances of the cache
* across the cluster, use {@link #destroyCache(NamedCache)}.
* <p>
* Applications that require more control when obtaining cache and service
* references should use the {@link #getCacheFactoryBuilder CacheFactoryBuilder}
* API.
*
* @author cp, gg  2001.12.14
*/
public abstract class CacheFactory
        extends Base
    {
    // ----- ConfigurableCacheFactory ---------------------------------------

    /**
    * Obtain the CacheFactoryBuilder singleton using the configuration
    * info from the "cache-factory-builder-config" element.
    *
    * @return an instance of CacheFactoryBuilder
    *
    * @since Coherence 3.5.1
    */
    public static CacheFactoryBuilder getCacheFactoryBuilder()
        {
        return System.getSecurityManager() == null
                ? getCacheFactoryBuilderInternal()
                : AccessController.doPrivileged((PrivilegedAction<CacheFactoryBuilder>)
                    CacheFactory::getCacheFactoryBuilderInternal);
        }

    /**
     * Implementation of {@link #getCacheFactoryBuilder()}.
     */
    private static CacheFactoryBuilder getCacheFactoryBuilderInternal()
        {
        CacheFactoryBuilder cfb = s_builder;
        if (cfb == null)
            {
            synchronized (CacheFactory.class)
                {
                cfb = s_builder;
                if (cfb == null)
                    {
                    XmlElement xml    = getCacheFactoryBuilderConfig();
                    String     sClass = xml.getSafeElement("class-name").
                        getString("com.tangosol.net.DefaultCacheFactoryBuilder");
                    try
                        {
                        Class clz = Class.forName(sClass);
                        Object[] aoParam = XmlHelper.parseInitParams(
                            xml.getSafeElement("init-params"));
                        cfb = (CacheFactoryBuilder)
                            ClassHelper.newInstance(clz, aoParam);
                        }
                    catch (Exception e)
                        {
                        throw ensureRuntimeException(e, "Failed to load the CacheFactoryBuilder");
                        }
                    setCacheFactoryBuilder(cfb);
                    }
                }
            }

        return cfb;
        }

    /**
    * Specify a singleton CacheFactoryBuilder.
    *
    * @param cfb  an instance of CacheFactoryBuilder
    *
    * @since Coherence 3.5.1
    */
    public static synchronized void setCacheFactoryBuilder(CacheFactoryBuilder cfb)
        {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(
                new LocalPermission("CacheFactory.setCacheFactoryBuilder"));
            }

        s_builder = cfb;
        checkConsistentCCFUsage();
        }

    /**
    * Obtain the ConfigurableCacheFactory singleton using the configuration
    * info from the "configurable-cache-factory-config" element located in the
    * tangosol-coherence.xml configuration file.
    *
    * @return an instance of ConfigurableCacheFactory
    *
    * @since Coherence 2.2
    */
    public static ConfigurableCacheFactory getConfigurableCacheFactory()
        {
        return getConfigurableCacheFactory(getContextClassLoader());
        }

    /**
    * Obtain the ConfigurableCacheFactory associated with the specified class
    * loader.
    * <p>
    * Note: if {@link #setConfigurableCacheFactory} has been called, the same
    *       factory instance will be returned.
    *
    * @param loader  the class loader for which to return a configurable cache
    *                factory
    *
    * @return the configurable cache factory for the specified loader
    */
    public static ConfigurableCacheFactory getConfigurableCacheFactory(ClassLoader loader)
        {
        return s_factory == null ?
            getCacheFactoryBuilder().getConfigurableCacheFactory(loader) :
            s_factory;
        }

    /**
    * Specify a singleton of ConfigurableCacheFactory.
    *
    * @param factory  an instance of ConfigurableCacheFactory
    *
    * @since Coherence 2.2
    *
    * @deprecated As of 12.2.1 deprecated with no replacement
    */
    public static synchronized void setConfigurableCacheFactory(
            ConfigurableCacheFactory factory)
        {
        s_factory = factory;
        checkConsistentCCFUsage();
        }

    /**
    * Return an instance of a service configured by the current
    * ConfigurableCacheFactory. This helper method is a simple wrapper around
    * the {@link ConfigurableCacheFactory#ensureService(String)} method.
    *
    * @param sName   service name (unique for a given configurable cache
    *                factory). If the Service with the specified name
    *                already exists, a reference to the same object will be
    *                returned
    *
    * @return the Service object
    *
    * @since Coherence 3.3
    */
    public static Service getService(String sName)
        {
        return getConfigurableCacheFactory().ensureService(sName);
        }

    /**
    * Return an instance of an cache configured by the current ConfigurableCacheFactory
    * <p>
    * This helper method is a simple wrapper around the
    * {@link ConfigurableCacheFactory#ensureCache(String, ClassLoader, NamedCache.Option...)}
    * method, using {@link #ensureClassLoader(ClassLoader)} to determine the
    * {@link ClassLoader} and no options.
    *
    * @param sName   cache name (unique for a given configurable cache
    *                factory). If the NamedCache with the specified name
    *                already exists, a reference to the same object will be
    *                returned
    *
    * @return the NamedCache object
    *
    * @since Coherence 2.2
    */
    public static <K, V> NamedCache<K, V> getCache(String sName)
        {
        return getCache(sName, (ClassLoader) null, null);
        }

    /**
    * Return an instance of an cache configured by the current ConfigurableCacheFactory,
    * using the specified {@link com.tangosol.net.NamedCache.Option}s.
    * <p>
    * This helper method is a simple wrapper around the
    * {@link ConfigurableCacheFactory#ensureCache(String, ClassLoader, NamedCache.Option...)}
    * method, using {@link #ensureClassLoader(ClassLoader)} to determine the
    * {@link ClassLoader}.
    * <p>
    * To enable type-safety checking, applications may specify the
    * {@link TypeAssertion} {@link com.tangosol.net.NamedCache.Option}.
    *
    * @param sName   cache name (unique for a given configurable cache
    *                factory). If the NamedCache with the specified name
    *                already exists, a reference to the same object will be
    *                returned
    *
    * @return the NamedCache object
    *
    * @since Coherence 12.2.1.1.0
    */
    public static <K, V> NamedCache<K, V> getCache(String sName, NamedCache.Option... options)
        {
        return getCache(sName, null, options);
        }

    /**
     * Return an instance of a cache configured by the current ConfigurableCacheFactory
     * with a specific {@link ClassLoader} no options.
     * <p>
     * This helper method is a simple wrapper around the
     * {@link ConfigurableCacheFactory#ensureCache(String, ClassLoader, NamedCache.Option...)}
     * method.
     *
     * @param sName    cache name (unique for a given configurable cache
     *                 factory). If the NamedCache with the specified name
     *                 already exists, a reference to the same object will be
     *                 returned
     * @param loader   ClassLoader that should be used to deserialize objects
     *                 inserted in the map by other members of the cluster
     *
     * @return the NamedCache reference
     *
     * @since Coherence 2.2
     */
    public static <K, V> NamedCache<K, V> getCache(String sName,
                                                   ClassLoader loader)
        {
        // a null-loader may have special meaning to a custom cache factory
        return getConfigurableCacheFactory(ensureClassLoader(loader)).ensureCache(sName, loader, null);
        }

    /**
    * Return an instance of a cache configured by the current ConfigurableCacheFactory
    * with a specific {@link ClassLoader} and {@link com.tangosol.net.NamedCache.Option}s
    * <p>
    * This helper method is a simple wrapper around the
    * {@link ConfigurableCacheFactory#ensureCache(String, ClassLoader, NamedCache.Option...)}
    * method.
    * <p>
    * To enable type-safety checking, applications may specify the
    * {@link TypeAssertion} {@link com.tangosol.net.NamedCache.Option}.
    *
    * @param sName    cache name (unique for a given configurable cache
    *                 factory). If the NamedCache with the specified name
    *                 already exists, a reference to the same object will be
    *                 returned
    * @param loader   ClassLoader that should be used to deserialize objects
    *                 inserted in the map by other members of the cluster
    * @param options  the {@link com.tangosol.net.NamedCache.Option}s
     *
    * @return the NamedCache reference
    *
    * @since Coherence 2.2
    */
    public static <K, V> NamedCache<K, V> getCache(String sName,
                                                   ClassLoader loader,
                                                   NamedCache.Option... options)
        {
        // a null-loader may have special meaning to a custom cache factory
        return getConfigurableCacheFactory(ensureClassLoader(loader)).
            ensureCache(sName, loader, options);
        }

    /**
    * Return an instance of a cache with the given name satisfying the specified
    * {@link TypeAssertion}.
    * <p>
    * This helper method is a simple wrapper around the
    * {@link ConfigurableCacheFactory#ensureTypedCache(String, ClassLoader, TypeAssertion)}
    * method, using {@link #ensureClassLoader(ClassLoader)} to determine the
    * {@link ClassLoader}.
    *
    * @param sCacheName  the cache name
    * @param assertion   the {@link TypeAssertion}
    *
    * @see TypeAssertion#withTypes(Class, Class)
    * @see TypeAssertion#withoutTypeChecking()
    * @see TypeAssertion#withRawTypes()
    *
    * @return  a NamedCache reference
    *
    * @since Coherence 12.2.1
    */
    public static <K, V> NamedCache<K, V> getTypedCache(String sCacheName,
                                                        TypeAssertion<K, V> assertion)
        {
        return getCache(sCacheName, (ClassLoader) null, assertion);
        }

    /**
    * Return an instance of a cache with the given name satisfying the specified
    * {@link TypeAssertion}.
    * <p>
    * This helper method is a simple wrapper around the
    * {@link ConfigurableCacheFactory#ensureTypedCache(String, ClassLoader, TypeAssertion)}
    * method.
    *
    * @param sCacheName  the cache name
    * @param loader      ClassLoader that should be used to deserialize
    *                    objects in the cache
    * @param assertion   the {@link TypeAssertion}
    *
    * @see TypeAssertion#withTypes(Class, Class)
    * @see TypeAssertion#withoutTypeChecking()
    * @see TypeAssertion#withRawTypes()
    *
    * @return  a NamedCache reference
    *
    * @since Coherence 12.2.1
    */
    public static <K, V> NamedCache<K, V> getTypedCache(String sCacheName,
                                                        ClassLoader loader,
                                                        TypeAssertion<K, V> assertion)
        {
        return getCache(sCacheName, loader, assertion);
        }


    // ----- transaction support  -------------------------------------------

    /**
    * Factory method returning an instance of the TransactionMap that is
    * based on the specified NamedCache and is local to this JVM.
    * <p>
    * <b>Note:</b> TransactionMap instance returned by this method will also
    * implement the NamedCache interface, allowing a client code to chain
    * local transaction by using the returned TransactionMap as a parameter
    * for another <code>getLocalTransaction()</code> call.
    *
    * @param map the NamedCache object to be used as a base for transaction
    *
    * @return a TransactionMap instance
    *
    * @since Coherence 1.2
    */
    public static TransactionMap getLocalTransaction(NamedCache map)
        {
        if (map == null)
            {
            throw new IllegalArgumentException("NamedCache must be specified");
            }

        if (INIT_FAILURE != null)
            {
            throw ensureRuntimeException(INIT_FAILURE);
            }

        try
            {
            return (TransactionMap) METHOD_GETLOCALTX.invoke(null, map);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Commit the transaction represented by a collection of TransactionMap
    * objects. Due to any of a number of reasons, the transaction could
    * fail to commit; if it fails to commit, the transaction is rolled back.
    *
    * @param collCaches a collection of TransactionMap objects
    * @param cRetry     the number of times [0..100] to retry a stage of the
    *                   transaction if a concurrency conflict occurs
    *
    * @return true if the transaction could be committed; false otherwise
    */
    public static boolean commitTransactionCollection(Collection collCaches, int cRetry)
        {
        if (cRetry < 0 || cRetry > 100)
            {
            throw new IllegalArgumentException("illegal retry count: " + cRetry);
            }
        int cTry = 1 + cRetry;

        List listCaches = collCaches instanceof List
                ? (List) collCaches
                : new ImmutableArrayList(collCaches);
        int cCaches = listCaches.size();

        // prepare
        for (int iCache = 0; iCache < cCaches; ++iCache)
            {
            TransactionMap cacheTx = (TransactionMap) listCaches.get(iCache);
            boolean fPrepared = false;
            for (int iTry = 0; iTry < cTry; ++iTry)
                {
                try
                    {
                    cacheTx.prepare();
                    fPrepared = true;
                    break;
                    }
                catch (ConcurrentModificationException e)
                    {
                    // exception is ignored; prepare will be retried
                    // momentarily
                    if (iTry < cRetry)
                        {
                        try
                            {
                            // sleep somewhere between 1..5ms
                            Blocking.sleep(1 + getRandom().nextInt(5));
                            }
                        catch (Throwable eIgnored) {}
                        }
                    else
                        {
                        // for debugging purposes, the exception is logged
                        log("Unable to prepare transaction:\n"
                                + getStackTrace(e), 4);
                        }
                    }
                catch (RuntimeException e)
                    {
                    break;
                    }
                catch (Error e)
                    {
                    log("Error during prepare (tx will rollback):\n"
                            + getStackTrace(e), 1);
                    break;
                    }
                }

            if (!fPrepared)
                {
                rollbackTransactionCollection(collCaches);
                return false;
                }
            }

        // commit
        for (int iCache = 0; iCache < cCaches; ++iCache)
            {
            TransactionMap cacheTx = (TransactionMap) listCaches.get(iCache);
            try
                {
                cacheTx.commit();
                }
            catch (Throwable e)
                {
                String sCategory = e instanceof Error ? "Error" : "Exception";
                if (iCache == 0)
                    {
                    log(sCategory + " during commit (tx will rollback):\n"
                            + getStackTrace(e), 1);
                    rollbackTransactionCollection(collCaches);
                    return false;
                    }
                else
                    {
                    log(sCategory + " during commit (tx will continue, "
                            + "but tx outcome is nondeterministic):\n"
                            + getStackTrace(e), 1);
                    }
                }
            }

        return true;
        }

    /**
    * Roll back the transaction represented by a collection of TransactionMap
    * objects.
    *
    * @param collCaches a collection of TransactionMap objects
    */
    public static void rollbackTransactionCollection(Collection collCaches)
        {
        for (Iterator iter = collCaches.iterator(); iter.hasNext(); )
            {
            TransactionMap cacheTx = (TransactionMap) iter.next();
            try
                {
                cacheTx.rollback();
                }
            catch (Throwable e)
                {
                log((e instanceof Error ? "Error" : "Exception")
                        + " during rollback (ignored):\n"
                        + getStackTrace(e), 1);
                }
            }
        }


    // ----- common ---------------------------------------------------------

    /**
    * Return a Cluster object for Coherence services.
    *
    * @return a Cluster object which may or may not be running
    *
    * @since Coherence 1.1
    */
    public static Cluster getCluster()
        {
        if (INIT_FAILURE != null)
            {
            throw ensureRuntimeException(INIT_FAILURE);
            }

        try
            {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<Cluster>()
                    {
                    public Cluster run() throws Exception
                        {
                        return (Cluster) METHOD_GETSAFECLUSTER.invoke(null, ClassHelper.VOID);
                        }
                    });
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Obtain a Cluster object running Coherence services.
    *
    * Calling this method is optional. The cluster member carrying
    * caching services will be lazily initialized when a first cache
    * is about to be created. However, that operation could take
    * significant time (especially for a first cluster member).
    * This method forces initialization, making all cache creating
    * requests predictably faster.
    *
    * @since Coherence 1.1
    *
    * @return a Cluster object
    */
    public static Cluster ensureCluster()
        {
        Cluster cluster = getCluster();
        synchronized (cluster)
            {
            if (!cluster.isRunning())
                {
                // Note: when not configured the cluster is able to derive
                //       the configuration that should be used (getClusterConfig())
                cluster.start();
                }
            return cluster;
            }
        }

    /**
    * Shutdown all clustered services.
    *
    * @since Coherence 1.0
    */
    public static void shutdown()
        {
        try
            {
            METHOD_SHUTDOWN.invoke(null, ClassHelper.VOID);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        finally
            {
            try
                {
                AccessController.doPrivileged(new PrivilegedAction()
                    {
                    public Object run()
                        {
                        setCacheFactoryBuilder(null);
                        return null;
                        }
                    });
                }
            catch (Exception e) {} // ignore security exceptions
        
            setConfigurableCacheFactory(null);
            }
        }

    /**
    * Release local resources associated with the specified instance of the
    * cache. This invalidates a reference obtained by any of getCache() methods.
    * <p>
    * Releasing a NamedCache reference makes it no longer usable, but does
    * not affect the content of the cache.
    * In other words, all other references to the cache will still be valid,
    * and the cache data is not affected by releasing the reference.
    * <p>
    * The reference that is released using this method can no longer be used;
    * any attempt to use the reference will result in an exception.
    * <p>
    * The purpose for releasing a cache reference is to allow the cache
    * implementation to release the ClassLoader used to deserialize items
    * in the cache. The cache implementation ensures that all references to
    * that ClassLoader are released. This implies that objects in the cache
    * that were loaded by that ClassLoader will be re-serialized to release
    * their hold on that ClassLoader. The result is that the ClassLoader can
    * be garbage-collected by Java in situations where the cache is operating
    * in an application server and applications are dynamically loaded and
    * unloaded.
    *
    * @param cache  the NamedCache object to be released
    *
    * @see CacheService#releaseCache(NamedCache)
    * @see #destroyCache(NamedCache)
    *
    * @since Coherence 1.1
    */
    public static void releaseCache(NamedCache cache)
        {
        getConfigurableCacheFactory(getContextClassLoader()).releaseCache(cache);
        }

    /**
    * Releases and destroys the specified NamedCache.
    * <p>
    * <b>Warning:</b> This method is used to completely destroy the specified
    * cache across the cluster. All references in the entire cluster to this
    * cache will be invalidated, the cached data will be cleared, and all
    * resources will be released.
    *
    * @param cache  the NamedCache object to be destroyed
    *
    * @see CacheService#destroyCache(NamedCache)
    * @see #releaseCache(NamedCache)
    *
    * @since Coherence 1.1
    */
    public static void destroyCache(NamedCache cache)
        {
        getConfigurableCacheFactory(getContextClassLoader()).destroyCache(cache);
        }

    /**
     * Returns the edition currently loaded based on the operational
     * configuration.
     *
     * @return the abbreviated string representation of the current edition.
     *         Potential values include <tt>(GE,EE,SE,RTC,DC)</tt>
     *
     * @since Coherence 12.1.2
     */
    public static String getEdition()
        {
        XmlElement xmlLicense = getServiceConfig("$License");
        try
            {
            METHOD_RESOLVEEDITION.invoke(null, xmlLicense);
            }
        catch (Exception e)
            {
            }
        return xmlLicense.getSafeElement("edition-name").getString();
        }


    // ----- logging helpers ------------------------------------------------

    /**
    * Check the consistency of usage of CCF/CFB.
    *
    * @deprecated As of 12.2.1 deprecated with no replacement
    */
    private static void checkConsistentCCFUsage()
        {
        if (s_factory != null && s_builder != null)
            {
            log("Mixed usage of getCacheFactoryBuilder() and " +
                "setConfigurableCacheFactory() is not recommended.", LOG_WARN);
            }
        }

    /**
    * Log a message using Coherence logging facility which is driven by
    * the "logging-config" element located in the tangosol-coherence.xml
    * configuration file.
    *
    * @param sMessage   a message to log
    * @param nSeverity  the severity of the logged message;
    *                   0=default, 1=error; 2=warning; 3=info; 4-9=debug
    *
    * @since Coherence 2.0
    */
    public static void log(String sMessage, int nSeverity)
        {
        try
            {
            METHOD_TRACE.invoke(null, sMessage, Integer.valueOf(nSeverity));
            }
        catch (Throwable e)
            {
            // default logging to stderr/stdout if Coherence logging is unavailable
            if (nSeverity > 0)
                {
                System.err.println(sMessage);
                }
            else
                {
                System.out.println(sMessage);
                }
            }
        }

    /**
     * Log a message using Coherence logging facility which is driven by
     * the "logging-config" element located in the tangosol-coherence.xml
     * configuration file.
     *
     * @param supplierMessage  the {@link Supplier} that will be evaluated
     *                         if the severity is being logged
     * @param nSeverity        the severity of the logged message;
     *                         0=default, 1=error; 2=warning; 3=info; 4-9=debug
     *
     * @throws IllegalArgumentException if {@code supplierMessage} is {@code null}
     *
     * @since 14.1.2
     */
    public static void log(Supplier<String> supplierMessage, int nSeverity)
        {
        if (supplierMessage == null)
            {
            throw new IllegalArgumentException("supplierMessage cannot be null");
            }
        if (isLogEnabled(nSeverity))
            {
            log(supplierMessage.get(), nSeverity);
            }
        }

    /**
    * Check if a message of the specified severity level will be logged using
    * the Coherence logging facility.
    *
    * @param nSeverity  the severity of a message
    *
    * @return true if a message with the specified severity level will be
    *          logged; false otherwise
    *
    * @see #log
    * @since Coherence 3.2
    */
    public static boolean isLogEnabled(int nSeverity)
        {
        try
            {
            return ((Boolean) METHOD_ISTRACEENABLED.
                invoke(null, Integer.valueOf(nSeverity))).booleanValue();
            }
        catch (Throwable e)
            {
            return true;
            }
        }


    // ----- configuration helpers ------------------------------------------

    /**
    * Return the default cluster configuration as defined by the
    * "cluster-config" element in the tangosol-coherence.xml configuration
    * file.
    *
    * @return XmlElement representing the default cluster configuration
    */
    public static XmlElement getClusterConfig()
        {
        return getServiceConfig("Cluster");
        }

    /**
    * Return the default replicated cache configuration.
    *
    * @return XmlElement representing the replicated cache configuration
    *
    * @see #getServiceConfig(String)
    */
    public static XmlElement getReplicatedCacheConfig()
        {
        return getServiceConfig(CacheService.TYPE_REPLICATED);
        }

    /**
    * Return the default distributed (partitioned) cache configuration.
    *
    * @return XmlElement representing the partitioned cache configuration
    *
    * @see #getServiceConfig(String)
    */
    public static XmlElement getDistributedCacheConfig()
        {
        return getServiceConfig(CacheService.TYPE_DISTRIBUTED);
        }

    /**
    * Return the configuration info for the logging facility as defined
    * by the "logging-config" element in the tangosol-coherence.xml
    * configuration file.
    *
    * @return XmlElement representing the logging facility configuration
    *
    * @since Coherence 2.2
    */
    public static XmlElement getLoggingConfig()
        {
        // pseudo-service
        return getServiceConfig("$Logger");
        }

    /**
    * Set the configuration info for the logging facility. If the Logger daemon
    * is already running, this call will also force its restart.
    *
    * @param xmlCfg  an XmlElement representing the new logging configuration
    *
    * @since Coherence 3.4.1
    */
    public static void setLoggingConfig(XmlElement xmlCfg)
        {
        setServiceConfig("$Logger", xmlCfg);
        }

    /**
    * Return the cache factory builder configuration as defined by the
    * "cache-factory-builder-config" element in the tangosol-coherence.xml
    * configuration file.
    *
    * @return XmlElement representing the cache factory builder
    *         configuration
    *
    * @since Coherence 3.5.1
    */
    public static XmlElement getCacheFactoryBuilderConfig()
        {
        // pseudo-service
        return getServiceConfig("$CacheFactoryBuilder");
        }

    /**
    * Set the cache factory builder configuration. If the Cluster
    * service is already running, this call will not have any effect.
    *
    * @param xmlCfg  an XmlElement representing the cache factory builder
    *                configuration
    *
    * @since Coherence 3.5.1
    */
    public static void setCacheFactoryBuilderConfig(XmlElement xmlCfg)
        {
        setServiceConfig("$CacheFactoryBuilder", xmlCfg);
        }

    /**
    * Return the configurable cache factory configuration as defined by the
    * "configurable-cache-factory-config" element in the tangosol-coherence.xml
    * configuration file.
    *
    * @return XmlElement representing the configurable cache factory
    *         configuration
    *
    * @since Coherence 2.2
    */
    public static XmlElement getConfigurableCacheFactoryConfig()
        {
        // pseudo-service
        return getServiceConfig("$CacheFactory");
        }

    /**
    * Set the configurable cache factory configuration. If the Cluster
    * service is already running, this call will not have any effect.
    *
    * @param xmlCfg  an XmlElement representing the configurable cache
    *                factory configuration
    *
    * @since Coherence 3.4.1
    */
    public static void setConfigurableCacheFactoryConfig(XmlElement xmlCfg)
        {
        setServiceConfig("$CacheFactory", xmlCfg);
        }

    /**
    * Return the security framework configuration.
    *
    * @return XmlElement representing the security framework configuration
    *
    * @since Coherence 2.5
    */
    public static XmlElement getSecurityConfig()
        {
        // pseudo-service
        return getServiceConfig("$Security");
        }

    /**
    * Set the security framework configuration.  If the Cluster service is
    * already running, this call will not have any effect.
    *
    * @param xmlCfg  an XmlElement representing the security framework
    *                configuration
    *
    * @since Coherence 3.4.1
    */
    public static void setSecurityConfig(XmlElement xmlCfg)
        {
        setServiceConfig("$Security", xmlCfg);
        }

    /**
    * Return the management framework configuration.
    *
    * @return XmlElement representing the management framework configuration
    *
    * @since Coherence 3.3
    */
    public static XmlElement getManagementConfig()
        {
        // pseudo-service
        return getServiceConfig("$Management");
        }

    /**
    * Set the management framework configuration.  If the Cluster service is
    * already running, this call will not have any effect.
    *
    * @param xmlCfg  an XmlElement representing the management framework
    *                configuration
    *
    * @since Coherence 3.4.1
    */
    public static void setManagementConfig(XmlElement xmlCfg)
        {
        setServiceConfig("$Management", xmlCfg);
        }

    /**
    * Return the configuration for the specified service type.
    *
    * @param  sServiceType  the service type
    *
    * @return XmlElement representing the service configuration
    *
    * @since Coherence 2.2
    */
    public static XmlElement getServiceConfig(final String sServiceType)
        {
        if (INIT_FAILURE != null)
            {
            throw ensureRuntimeException(INIT_FAILURE);
            }

        try
            {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<XmlElement>()
                    {
                    public XmlElement run() throws Exception
                        {
                        return (XmlElement) METHOD_GETSERVICECONFIG.
                            invoke(null, sServiceType);
                        }
                    });
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Set the configuration for the specified service type.  If the Cluster
    * service is already running, this call may not have any effect.
    *
    * @param sServiceType  the service type
    * @param xmlCfg        an XmlElement representing the service configuration
    *
    * @since Coherence 3.4.1
    */
    public static void setServiceConfig(String sServiceType, XmlElement xmlCfg)
        {
        if (INIT_FAILURE != null)
            {
            throw ensureRuntimeException(INIT_FAILURE);
            }

        try
            {
            METHOD_SETSERVICECONFIG.invoke(null, sServiceType, xmlCfg);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- unit test ------------------------------------------------------

    /**
    * Invoke the Coherence command line tool.
    */
    public static void main(String[] asArg)
            throws Exception
        {
        if (INIT_FAILURE != null)
            {
            throw ensureRuntimeException(INIT_FAILURE);
            }

        METHOD_MAIN.invoke(null, new Object[] {asArg});
        }


    // ----- constants ------------------------------------------------------

    /**
    * The product name string.
    */
    public static final String PRODUCT;

    /**
    * Software version string.
    */
    public static final String VERSION;

    /**
    * The Class name of the Coherence application.
    */
    private static final String COHERENCE =
            "com.tangosol.coherence.component.application.console.Coherence";


    // ----- data fields ----------------------------------------------------

    /**
    * CacheFactoryBuilder singleton.
    */
    private static CacheFactoryBuilder s_builder;

    /**
    * ConfigurableCacheFactory singleton.
    *
    * @deprecated As of 12.2.1 deprecated with no replacement
    */
    private static ConfigurableCacheFactory s_factory;

    /**
    * Coherence getSafeCluster() method.
    */
    private static final Method METHOD_GETSAFECLUSTER;

    /**
    * Coherence shutdown(Cluster) method.
    */
    private static final Method METHOD_SHUTDOWN;

    /**
    * Coherence getLocalTransaction(NamedCache) method.
    */
    private static final Method METHOD_GETLOCALTX;

    /**
    * Coherence _trace(String,int) method.
    */
    private static final Method METHOD_TRACE;

    /**
    * Coherence isTraceEnabled(int) method.
    */
    private static final Method METHOD_ISTRACEENABLED;

    /**
    * Coherence getServiceConfig(String) method.
    */
    private static final Method METHOD_GETSERVICECONFIG;

    /**
    * Coherence getServiceConfig(String, XmlElement) method.
    */
    private static final Method METHOD_SETSERVICECONFIG;

    /**
    * Coherence main(String[]) method.
    */
    private static final Method METHOD_MAIN;

    /**
    * Coherence resolveEdition(XmlElement) method.
    */
    private static final Method METHOD_RESOLVEEDITION;

    /**
    * Last reflection exception.
    */
    private static final Throwable INIT_FAILURE;

    static
        {
        final EntryPoints ep = new EntryPoints();

        AccessController.doPrivileged(
            new PrivilegedAction<EntryPoints>()
                {
                public EntryPoints run()
                    {
                    return ep.getEntryPoints();
                    }
                });

        PRODUCT                 = ep.m_sProduct;
        VERSION                 = ep.m_sVersion;
        METHOD_GETSAFECLUSTER   = ep.m_methGetSafeCluster;
        METHOD_SHUTDOWN         = ep.m_methShutdown;
        METHOD_GETLOCALTX       = ep.m_methGetLocalTransaction;
        METHOD_TRACE            = ep.m_methTrace;
        METHOD_ISTRACEENABLED   = ep.m_methIsTraceEnabled;
        METHOD_GETSERVICECONFIG = ep.m_methGetServiceConfig;
        METHOD_SETSERVICECONFIG = ep.m_methSetServiceConfig;
        METHOD_MAIN             = ep.m_methMain;
        METHOD_RESOLVEEDITION   = ep.m_methResolveEdition;
        INIT_FAILURE            = ep.m_eInitFailure;
        }

    /**
     * Data structure of coherence entry points.
     */
    private static class EntryPoints
        {
        private EntryPoints() {}

        private EntryPoints getEntryPoints()
            {
            try
                {
                Class clzLibrary = Class.forName(COHERENCE);
                m_sProduct = (String) clzLibrary.getField("TITLE").get(null);
                m_sVersion = (String) clzLibrary.getField("VERSION").get(null);

                // look up methods (cache for faster use)
                m_methGetSafeCluster      = clzLibrary.getMethod("getSafeCluster",
                                            new Class[0]);
                m_methShutdown            = clzLibrary.getMethod("shutdown",
                                            new Class[0]);
                m_methGetLocalTransaction = clzLibrary.getMethod("getLocalTransaction",
                                            new Class[] {NamedCache.class});
                m_methTrace               = clzLibrary.getMethod("_trace",
                                            new Class[] {String.class, int.class});
                m_methIsTraceEnabled      = clzLibrary.getMethod("_isTraceEnabled",
                                            new Class[] {int.class});
                m_methGetServiceConfig    = clzLibrary.getMethod("getServiceConfig",
                                            new Class[] {String.class});
                m_methSetServiceConfig    = clzLibrary.getMethod("setServiceConfig",
                                            new Class[] {String.class, XmlElement.class});
                m_methMain                = clzLibrary.getMethod("main",
                                            new Class[] {String[].class});
                m_methResolveEdition      = clzLibrary.getMethod("resolveEdition",
                                            new Class[] {XmlElement.class});
                }
            catch (Throwable e)
                {
                m_eInitFailure = e;
                }
            return this;
            }

        String    m_sProduct;
        String    m_sVersion;
        Method    m_methGetSafeCluster;
        Method    m_methShutdown;
        Method    m_methGetLocalTransaction;
        Method    m_methTrace;
        Method    m_methIsTraceEnabled;
        Method    m_methGetServiceConfig;
        Method    m_methSetServiceConfig;
        Method    m_methMain;
        Method    m_methResolveEdition;
        Throwable m_eInitFailure;
        }
    }
