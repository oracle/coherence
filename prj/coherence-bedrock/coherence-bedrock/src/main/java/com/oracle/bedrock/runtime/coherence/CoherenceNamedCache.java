/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.runtime.Assembly;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.runtime.concurrent.callable.RemoteCallableStaticMethod;
import com.oracle.bedrock.runtime.concurrent.callable.RemoteMethodInvocation;

import com.oracle.bedrock.util.ReflectionHelper;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;

import java.io.Serializable;

import java.lang.reflect.Method;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.CompletableFuture;

public class CoherenceNamedCache<K, V>
        implements NamedCache<K, V>
    {
    /**
     * The {@link CoherenceClusterMember} that owns the {@link NamedCache}
     * that this {@link CoherenceNamedCache} represents.
     */
    private CoherenceClusterMember member;

    /**
     * The {@link Optional} {@link CoherenceCluster} that owns the {@link CoherenceClusterMember}.
     * (this is optional as the {@link CoherenceClusterMember} may not have been defined as part of a cluster).
     */
    private Optional<CoherenceCluster> cluster;

    /**
     * The name of the {@link NamedCache}.
     */
    private String cacheName;

    /**
     * The type of the keys for the {@link NamedCache}.
     */
    private Class<K> keyClass;

    /**
     * The type of the values for the {@link NamedCache}.
     */
    private Class<V> valueClass;

    /**
     * The {@link RemoteCallableStaticMethod} to use in the
     * {@link CoherenceClusterMember} to acquire the {@link NamedCache}.
     */
    private RemoteCallable<NamedCache> producer;

    /**
     * The {@link RemoteMethodInvocation.Interceptor} to use for intercepting
     * and transforming remote method invocations.
     */
    private RemoteMethodInvocation.Interceptor interceptor;


    /**
     * Constructs a {@link CoherenceNamedCache}.
     *
     * @param member     the {@link CoherenceClusterMember} that owns the {@link NamedCache}
     * @param cacheName  the name of the {@link NamedCache}
     * @param keyClass   the type of the keys for the {@link NamedCache}
     * @param valueClass the type of the values for the {@link NamedCache}
     */
    public CoherenceNamedCache(
            CoherenceClusterMember member,
            String cacheName,
            Class<K> keyClass,
            Class<V> valueClass)
        {
        this(member,
             cacheName,
             keyClass,
             valueClass,
             new RemoteCallableStaticMethod<>("com.tangosol.net.CacheFactory", "getCache", cacheName));
        }

    /**
     * Constructs a {@link CoherenceNamedCache}.
     *
     * @param member     the {@link CoherenceClusterMember} that owns the {@link NamedCache}
     * @param cacheName  the name of the {@link NamedCache}
     * @param keyClass   the type of the keys for the {@link NamedCache}
     * @param valueClass the type of the values for the {@link NamedCache}
     * @param producer   the {@link RemoteCallable} to use to obtain a {@link NamedCache} instance
     */
    public CoherenceNamedCache(
            CoherenceClusterMember member,
            String cacheName,
            Class<K> keyClass,
            Class<V> valueClass,
            RemoteCallable<NamedCache> producer)
        {
        this.member = member;
        this.cacheName = cacheName;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.producer = producer;
        this.interceptor = new NamedCacheMethodInterceptor();

        // determine the CoherenceCluster that the CoherenceClusterMember is part of
        Assembly assembly = member.get(Assembly.class);

        this.cluster = assembly instanceof CoherenceCluster
                ? Optional.of((CoherenceCluster) assembly) : Optional.empty();
        }


    /**
     * Invoke the specified method remotely in the {@link CoherenceClusterMember} on the
     * {@link NamedCache} provided by the {@link #producer}.
     *
     * @param methodName the name of the method
     * @param arguments  the arguments for the method
     * @return the result of the remote method execution
     * @throws RuntimeException if any exception occurs remotely
     */
    protected <T> T remotelyInvoke(
            String methodName,
            Object... arguments)
        {
        // notify the interceptor that we're about make a remote invocation
        Method method = ReflectionHelper.getCompatibleMethod(NamedCache.class, methodName, arguments);

        if (method == null)
            {
            throw new UnsupportedOperationException("Unable to locate method [" + methodName + "] with arguments ["
                                                            + Arrays.toString(arguments) + "] on NamedCache interface");
            }
        else
            {
            interceptor.onBeforeRemoteInvocation(method, arguments);
            }

        int retryCount = 0;

        // we try the request until we've run out of operational cluster members
        while (retryCount < (cluster.isPresent() ? cluster.get().count() : 1))
            {
            // we'll need to choose a new member to perform the request when the current one is no longer operational
            // or we have to retry
            boolean chooseNewMember = !member.isOperational() || retryCount > 0;

            if (chooseNewMember)
                {
                if (cluster.isPresent())
                    {
                    Optional<CoherenceClusterMember> optional = cluster.get().findAny();

                    if (optional.isPresent())
                        {
                        this.member = optional.get();
                        }
                    else
                        {
                        // there's no longer a member we can use
                        throw new IllegalStateException("The underlying Cluster no longer has available Cluster Members to perform the request ["
                                                                + methodName + "]");
                        }
                    }
                else
                    {
                    // we just re-throw if the member is not part of a cluster
                    throw new IllegalStateException("The underlying Cluster Member [" + member.getName()
                                                            + "] is no longer available to perform the request [" + methodName
                                                            + "]");
                    }
                }

            try
                {
                // submit the remote method invocation
                CompletableFuture future = member.submit(new RemoteMethodInvocation(producer,
                                                                                    methodName,
                                                                                    arguments,
                                                                                    interceptor));

                // intercept the result after the remote invocation
                return (T) interceptor.onAfterRemoteInvocation(method, arguments, future.get());
                }
            catch (IllegalStateException e)
                {
                // retry the request with a different cluster member when the request when the submission fails
                retryCount++;
                }
            catch (RuntimeException e)
                {
                // re-throw runtime exceptions
                throw e;
                }
            catch (Exception e)
                {
                throw new RuntimeException("Failed to execute [" + methodName + "] with arguments "
                                                   + Arrays.toString(arguments),
                                           interceptor.onRemoteInvocationException(method, arguments, e));
                }

            }

        throw new IllegalStateException("Failed to perform request [" + methodName + "] with arguments "
                                                + Arrays.toString(arguments) + " using [" + retryCount + "] Cluster Members");
        }


    @Override
    public String getCacheName()
        {
        return cacheName;
        }


    @Override
    public CacheService getCacheService()
        {
        throw new UnsupportedOperationException("The method NamedCache.getCacheService is not supported for remote execution");
        }


    @Override
    public boolean isActive()
        {
        return remotelyInvoke("isActive");
        }


    @Override
    public boolean isReady()
        {
        return remotelyInvoke("isReady");
        }


    @Override
    public void release()
        {
        remotelyInvoke("release");
        }


    @Override
    public void destroy()
        {
        remotelyInvoke("destroy");
        }


    @Override
    public V put(
            K key,
            V value,
            long expiry)
        {
        return remotelyInvoke("put", key, value, expiry);
        }


    @Override
    public Map<K, V> getAll(Collection<? extends K> keys)
        {
        return remotelyInvoke("getAll", keys);
        }


    @Override
    public boolean lock(
            Object key,
            long duration)
        {
        return remotelyInvoke("lock", key, duration);
        }


    @Override
    public boolean lock(Object key)
        {
        return remotelyInvoke("lock", key);
        }


    @Override
    public boolean unlock(Object key)
        {
        return remotelyInvoke("unlock", key);
        }


    @Override
    public <R> R invoke(
            K key,
            EntryProcessor<K, V, R> processor)
        {
        return (R) remotelyInvoke("invoke", key, processor);
        }


    @Override
    public <R> Map<K, R> invokeAll(
            Collection<? extends K> keys,
            EntryProcessor<K, V, R> processor)
        {
        return remotelyInvoke("invokeAll", keys, processor);
        }


    @Override
    public <R> Map<K, R> invokeAll(
            Filter filter,
            EntryProcessor<K, V, R> processor)
        {
        return remotelyInvoke("invokeAll", filter, processor);
        }


    @Override
    public <R> R aggregate(
            Collection<? extends K> keys,
            EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return (R) remotelyInvoke("aggregate", keys, aggregator);
        }


    @Override
    public <R> R aggregate(
            Filter filter,
            EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return (R) remotelyInvoke("aggregate", filter, aggregator);
        }


    @Override
    public void addMapListener(MapListener listener)
        {
        throw new UnsupportedOperationException("The method NamedCache.addMapListener is not supported for remote execution");
        }


    @Override
    public void removeMapListener(MapListener listener)
        {
        throw new UnsupportedOperationException("The method NamedCache.removeMapListener is not supported for remote execution");
        }


    @Override
    public void addMapListener(
            MapListener listener,
            Object key,
            boolean lite)
        {
        throw new UnsupportedOperationException("The method NamedCache.addMapListener is not supported for remote execution");
        }


    @Override
    public void removeMapListener(
            MapListener listener,
            Object key)
        {
        throw new UnsupportedOperationException("The method NamedCache.removeMapListener is not supported for remote execution");
        }


    @Override
    public void addMapListener(
            MapListener listener,
            Filter filter,
            boolean lite)
        {
        throw new UnsupportedOperationException("The method NamedCache.addMapListener is not supported for remote execution");
        }


    @Override
    public void removeMapListener(
            MapListener listener,
            Filter filter)
        {
        throw new UnsupportedOperationException("The method NamedCache.removeMapListener is not supported for remote execution");
        }


    @Override
    public Set<K> keySet(Filter filter)
        {
        return remotelyInvoke("keySet", filter);
        }


    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter)
        {
        return remotelyInvoke("entrySet", filter);
        }


    @Override
    public Set<Map.Entry<K, V>> entrySet(
            Filter filter,
            Comparator comparator)
        {
        return remotelyInvoke("entrySet", filter, comparator);
        }


    @Override
    public <T, E> void addIndex(
            com.tangosol.util.ValueExtractor<? super T, ? extends E> valueExtractor,
            boolean ordered,
            Comparator<? super E> comparator)
        {
        remotelyInvoke("addIndex", valueExtractor, ordered, comparator);
        }


    @Override
    public <T, E> void removeIndex(com.tangosol.util.ValueExtractor<? super T, ? extends E> valueExtractor)
        {
        remotelyInvoke("removeIndex", valueExtractor);
        }


    @Override
    public int size()
        {
        return remotelyInvoke("size");
        }


    @Override
    public boolean isEmpty()
        {
        return remotelyInvoke("isEmpty");
        }


    @Override
    public boolean containsKey(Object key)
        {
        return remotelyInvoke("containsKey", key);
        }


    @Override
    public boolean containsValue(Object value)
        {
        return remotelyInvoke("containsValue", value);
        }


    @Override
    public V get(Object key)
        {
        return remotelyInvoke("get", key);
        }


    @Override
    public V put(
            K key,
            V value)
        {
        return (V) remotelyInvoke("put", key, value);
        }


    @Override
    public V remove(Object key)
        {
        return (V) remotelyInvoke("remove", key);
        }


    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        remotelyInvoke("putAll", map);
        }


    @Override
    public void clear()
        {
        remotelyInvoke("clear");
        }


    @Override
    public void truncate()
        {
        remotelyInvoke("truncate");
        }


    @Override
    public Set<K> keySet()
        {
        return remotelyInvoke("keySet");
        }


    @Override
    public Collection<V> values()
        {
        return remotelyInvoke("values");
        }


    @Override
    public Set<Map.Entry<K, V>> entrySet()
        {
        return remotelyInvoke("entrySet");
        }


    /**
     * A Coherence specific {@link RemoteMethodInvocation.Interceptor} for {@link NamedCache} methods.
     */
    public static class NamedCacheMethodInterceptor
            implements RemoteMethodInvocation.Interceptor
        {
        @Override
        public void onBeforeRemoteInvocation(
                Method method,
                Object[] arguments)
            {
            // ensure that the arguments for specific methods are serializable
            String name = method.getName();

            if ((name.equals("getAll") || name.equals("invokeAll") || name.equals("aggregate"))
                    && !(arguments[0] instanceof Serializable))
                {
                // ensure invocations of NamedCache.getAll / invokeAll / aggregate using collections are serializable
                arguments[0] = new ArrayList((Collection) arguments[0]);
                }
            else if (name.equals("putAll") && !(arguments[0] instanceof Serializable))
                {
                arguments[0] = new HashMap((Map) arguments[0]);
                }
            }


        @Override
        public Object onAfterRemoteInvocation(
                Method method,
                Object[] arguments,
                Object result)
            {
            return result;
            }


        @Override
        public Exception onRemoteInvocationException(
                Method method,
                Object[] arguments,
                Exception exception)
            {
            return exception;
            }


        @Override
        public void onBeforeInvocation(
                Object instance,
                Method method,
                Object[] arguments)
            {
            // nothing to do before invocation
            }


        @Override
        public Object onAfterInvocation(
                Object instance,
                Method method,
                Object[] arguments,
                Object result)
            {
            // ensure that the result of the method is serializable, including transforming it if necessary
            String name = method.getName();

            if (name.equals("invokeAll") || name.equals("getAll"))
                {
                // the result of invokeAll may not be serializable,
                // so copy them into a serializable map
                result = new HashMap((Map) result);
                }
            else if (name.equals("keySet"))
                {
                // the result of keySet may not be serializable,
                // so copy them into a serializable set
                result = new HashSet((Set) result);
                }
            else if (name.equals("entrySet"))
                {
                // the result of entrySet may not be serializable,
                // so copy the entries into a serializable set
                Set<Map.Entry> set = (Set<Map.Entry>) result;
                Set<Map.Entry> resultSet = new HashSet();

                for (Map.Entry entry : set)
                    {
                    resultSet.add(new AbstractMap.SimpleEntry(entry.getKey(), entry.getValue()));
                    }

                result = resultSet;
                }
            else if (name.equals("values"))
                {
                // the result of values may not be serializable,
                // so copy them into a serializable set
                result = new ArrayList((Collection) result);
                }

            return result;
            }


        @Override
        public Exception onInvocationException(
                Object instance,
                Method method,
                Object[] arguments,
                Exception exception)
            {
            return exception;
            }
        }
    }
