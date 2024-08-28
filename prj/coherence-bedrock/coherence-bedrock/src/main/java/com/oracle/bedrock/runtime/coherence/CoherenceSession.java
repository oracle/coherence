/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.runtime.Assembly;
import com.oracle.bedrock.runtime.coherence.callables.GetSession;
import com.oracle.bedrock.runtime.coherence.callables.GetSessionCache;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.callable.RemoteMethodInvocation;
import com.oracle.bedrock.util.ReflectionHelper;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CoherenceSession
        implements Session
    {
    /**
     * The {@link CoherenceClusterMember} that owns the {@link Session}
     * that this {@link CoherenceSession} represents.
     */
    private CoherenceClusterMember member;

    /**
     * The {@link Optional} {@link CoherenceCluster} that owns the {@link CoherenceClusterMember}.
     * (this is optional as the {@link CoherenceClusterMember} may not have been defined as part of a cluster).
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<CoherenceCluster> cluster;

    /**
     * The name of the {@link com.tangosol.net.Coherence} instance that owns the {@link Session}.
     */
    private final String coherenceName;

    /**
     * The name of this {@link Session}.
     */
    private final String sessionName;

    /**
     * The {@link RemoteCallable} to use in the {@link CoherenceClusterMember}
     * to acquire the {@link Session}.
     */
    private final RemoteCallable<Session> producer;

    /**
     * The {@link RemoteMethodInvocation.Interceptor} to use for intercepting
     * and transforming remote method invocations.
     */
    private final RemoteMethodInvocation.Interceptor interceptor;

    /**
     * This session's {@link ResourceRegistry}.
     */
    private final ResourceRegistry registry;

    /**
     * Constructs a {@link CoherenceSession}.
     *
     * @param member        the {@link CoherenceClusterMember} that owns the {@link Session}
     * @param coherenceName the name of the {@link com.tangosol.net.Coherence} instance to get the {@link Session} from
     * @param sessionName   the name of the {@link Session}
     */
    public CoherenceSession(
            CoherenceClusterMember member,
            String coherenceName,
            String sessionName)
        {
        this(member, coherenceName, sessionName, new GetSession(coherenceName, sessionName));
        }

    /**
     * Constructs a {@link CoherenceSession}.
     *
     * @param member        the {@link CoherenceClusterMember} that owns the {@link Session}
     * @param coherenceName the name of the {@link com.tangosol.net.Coherence} instance to get the {@link Session} from
     * @param sessionName   the name of the {@link Session}
     * @param producer      the {@link RemoteCallable} to use to obtain a {@link Session} instance
     */
    public CoherenceSession(
            CoherenceClusterMember member,
            String coherenceName,
            String sessionName,
            RemoteCallable<Session> producer)
        {
        this.member = member;
        this.coherenceName = coherenceName;
        this.sessionName = sessionName;
        this.producer = producer;
        this.interceptor = new SessionMethodInterceptor();
        this.registry = new SimpleResourceRegistry();

        // determine the CoherenceCluster that the CoherenceClusterMember is part of
        Assembly<?> assembly = member.get(Assembly.class);

        this.cluster = assembly instanceof CoherenceCluster
                ? Optional.of((CoherenceCluster) assembly) : Optional.empty();
        }


    /**
     * Invoke the specified method remotely in the {@link CoherenceClusterMember} on the
     * {@link Session} provided by the {@link #producer}.
     *
     * @param methodName the name of the method
     * @param arguments  the arguments for the method
     * @return the result of the remote method execution
     * @throws RuntimeException if any exception occurs remotely
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T> T remotelyInvoke(
            String methodName,
            Object... arguments)
        {
        // notify the interceptor that we're about make a remote invocation
        Method method = ReflectionHelper.getCompatibleMethod(Session.class, methodName, arguments);

        if (method == null)
            {
            throw new UnsupportedOperationException("Unable to locate method [" + methodName + "] with arguments ["
                                                            + Arrays.toString(arguments) + "] on Session interface");
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K, V> NamedCache<K, V> getCache(String cacheName, NamedCache.Option... options)
        {
        return new CoherenceNamedCache(member,
                                       cacheName,
                                       Object.class,
                                       Object.class,
                                       new GetSessionCache(coherenceName, sessionName, cacheName));
        }

    @Override
    public <V> NamedTopic<V> getTopic(String sName, NamedCollection.Option... options)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public void close(NamedCollection col)
        {
        col.close();
        }

    @Override
    public void destroy(NamedCollection col)
        {
        col.destroy();
        }

    @Override
    public void close() throws Exception
        {
        throw new UnsupportedOperationException("The method Session.close() is not supported for remote execution");
        }

    @Override
    public <K, V> NamedMap<K, V> getMap(String mapName, NamedMap.Option... options)
        {
        return getCache(mapName, options);
        }

    @Override
    public ResourceRegistry getResourceRegistry()
        {
        return registry;
        }

    @Override
    public InterceptorRegistry getInterceptorRegistry()
        {
        throw new UnsupportedOperationException("The method Session.getInterceptorRegistry() is not supported for remote execution");
        }

    @Override
    public boolean isMapActive(String mapName, ClassLoader loader)
        {
        return remotelyInvoke("isMapActive", mapName, null);
        }

    @Override
    public boolean isCacheActive(String cacheName, ClassLoader loader)
        {
        return remotelyInvoke("isCacheActive", cacheName, null);
        }

    @Override
    public boolean isTopicActive(String topicName, ClassLoader loader)
        {
        return remotelyInvoke("isTopicActive", topicName, null);
        }

    @Override
    public String getName()
        {
        return sessionName;
        }

    @Override
    public String getScopeName()
        {
        return remotelyInvoke("getScopeName");
        }

    @Override
    public boolean isActive()
        {
        return remotelyInvoke("isActive");
        }

    @Override
    public void activate()
        {
        remotelyInvoke("activate");
        }

    @Override
    public Service getService(String sServiceName)
        {
        throw new UnsupportedOperationException("The method Session.getService() is not supported for remote execution");
        }


    /**
     * A Coherence specific {@link RemoteMethodInvocation.Interceptor} for {@link Session} methods.
     */
    public static class SessionMethodInterceptor
            implements RemoteMethodInvocation.Interceptor
        {
        @Override
        public void onBeforeRemoteInvocation(Method method, Object[] arguments)
            {
            // nothing to do before invocation
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
