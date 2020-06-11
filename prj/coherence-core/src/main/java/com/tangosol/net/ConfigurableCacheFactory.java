/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.util.ResourceRegistry;

import static com.tangosol.net.cache.TypeAssertion.withRawTypes;


/**
* An interface for cache factory.
*
* @see DefaultConfigurableCacheFactory
*
* @author gg 2003.05.26
* @author jh 2006.06.28
*
* @since Coherence 2.2
*/
public interface ConfigurableCacheFactory
    {
    /**
     * Activate prepares the factory to be used within a container and should be
     * called before any other factory method. Note that this factory can only be
     * activated once and disposed of once.
     *
     * @throws IllegalStateException  if this factory has already been activated
     *
     * @since Coherence 12.1.2
     */
    public void activate();

    /**
     * Dispose of this factory. This will stop all services that were started by this
     * factory and dispose of all resources registered via {@link #getResourceRegistry()}.
     *
     * This factory may not be used after invoking dispose.
     *
     * @throws IllegalStateException  if this factory is not active
     *
     * @since Coherence 12.1.2
     */
    public void dispose();

    /**
    * Ensure an cache for the given name and classloader (using raw types).
    *
    * @param sCacheName  the cache name
    * @param loader      ClassLoader that should be used to deserialize
    *                    objects in the cache
    *
    * @return  a NamedCache created
    */
    public default <K, V> NamedCache<K, V> ensureCache(String sCacheName, ClassLoader loader)
        {
        return ensureTypedCache(sCacheName, loader, withRawTypes());
        }

    /**
     * Ensure an cache for the given name, classloader and options.
     *
     * @param sCacheName  the cache name
     * @param loader      ClassLoader that should be used to deserialize
     *                    objects in the cache
     * @param options     the {@link com.tangosol.net.NamedCache.Option}s
     *
     * @return  a NamedCache created
     */
    public <K, V> NamedCache<K, V> ensureCache(String sCacheName,
                                               ClassLoader loader,
                                               NamedCache.Option... options);

    /**
    * Ensure a cache for the given name satisfying the specified type assertion.
    *
    * @param sCacheName  the cache name
    * @param loader      the {@link ClassLoader} to use for deserializing
    *                    cache entries
    * @param assertion   the {@link TypeAssertion}
     *                   for asserting the type of keys and values for the
     *                   NamedCache
    *
    * @return  a NamedCache created
    *
    * @since Coherence 12.2.1
    */
    public default <K, V> NamedCache<K, V> ensureTypedCache(String sCacheName,
                                                            ClassLoader loader,
                                                            TypeAssertion<K, V> assertion)
        {
        return ensureCache(sCacheName, loader, assertion);
        }


    /**
    * Release a cache and its associated resources.
    * <p>
    * Releasing a cache makes it no longer usable, but does not affect the
    * cache itself. In other words, all other references to the cache will still
    * be valid, and the cache data is not affected by releasing the reference.
    * Any attempt to use the released cache reference afterword will result in
    * an exception.
    *
    * @param cache  the cache to release
    *
    * @since Coherence 3.5.1
    */
    public void releaseCache(NamedCache<?, ?> cache);

    /**
    * Release and destroy this instance of NamedCache.
    * <p>
    * <b>Warning:</b> This method is used to completely destroy the specified
    * cache across the cluster. All references in the entire cluster to this
    * cache will be invalidated, the cached data will be cleared, and all
    * internal and associated resources will be released.
    *
    * @param cache  the cache to release
    *
    * @since Coherence 3.5.1
    */
    public void destroyCache(NamedCache<?, ?> cache);


    /**
    * Ensure an Object-based topic for the given name.
    *
    * @param sName       the topic name
    * @param options     the {@link NamedTopic.Option}s to control any optional
    *                   topic configuration
    *
    * @return  a NamedTopic created
    *
    * @since Coherence 14.1.1
    */
    public default  <V> NamedTopic<V> ensureTopic(String sName, NamedTopic.Option... options)
        {
        return ensureTopic(sName, (ClassLoader)null, options);
        }

    /**
    * Ensure an Object-based topic for the given name.
    *
    * @param sName       the topic name
    * @param loader      ClassLoader that should be used to deserialize
    *                    objects in the cache
    * @param options     the {@link NamedTopic.Option}s to control any optional
    *                   topic configuration
    *
    * @return  a NamedTopic created
    *
    * @since Coherence 14.1.1
    */
    public <V> NamedTopic<V> ensureTopic(String sName, ClassLoader loader, NamedTopic.Option... options);

    /**
    * Release a {@link NamedTopic} and its associated resources.
    * <p>
    * Releasing a topic makes it no longer usable, but does not affect the
    * topic itself. In other words, all other references to the topic will still
    * be valid, and the topic data is not affected by releasing the reference.
    * Any attempt to use the released topic reference afterword will result in
    * an exception.
    *
    * @param topic  the topic to release
    *
    * @since Coherence 14.1.1
    */
    public void releaseTopic(NamedTopic<?> topic);

    /**
    * Release and destroy this instance of {@link NamedTopic}.
    * <p>
    * <b>Warning:</b> This method is used to completely destroy the specified
    * topic across the cluster. All references in the entire cluster to this
    * topic will be invalidated, the topic data will be cleared, and all
    * internal and associated resources will be released.
    *
    * @param topic  the topic to release
    *
    * @since Coherence 14.1.1
    */
    public void destroyTopic(NamedTopic<?> topic);

    /**
    * Ensure a service for the given name.
    *
    * @param sServiceName  the service name
    *
    * @return  a Service created
    */
    public Service ensureService(String sServiceName);

    /**
    * Return the {@link InterceptorRegistry} for this factory.
    * {@link com.tangosol.net.events.EventInterceptor}s registered with
    * this registry will be scoped to services and caches created by
    * this factory.
    *
    * @return the {@link InterceptorRegistry} for this factory
    *
    * @since Coherence 12.1.2
    */
    public InterceptorRegistry getInterceptorRegistry();

    /**
     * Return the {@link ResourceRegistry} for this factory.
     *
     * @return the ResourceRegistry for this factory
     *
     * @since Coherence 12.1.2
     */
    public ResourceRegistry getResourceRegistry();

    /**
     * Validate whether a cache with the given name is active in the context
     * of the given {@link ClassLoader}. The ClassLoader should be the same
     * as provided to a previous call to {@link #ensureCache(String, ClassLoader, NamedCache.Option...)}.
     *
     * @param sCacheName  the cache name
     * @param loader      the ClassLoader that should be used to deserialize
     *                    objects in the cache
     *
     * @return true if cache is active in context of the provided ClassLoader
     *
     * @since Coherence 12.2.1
     */
    public boolean isCacheActive(String sCacheName, ClassLoader loader);

    /**
     * Return the scope name of this cache factory, if available.
     *
     * @return the scope name of this cache factory, if available; {@code null} otherwise
     */
    public default String getScopeName()
        {
        return getResourceRegistry().getResource(String.class, "scope-name");
        }
    }
