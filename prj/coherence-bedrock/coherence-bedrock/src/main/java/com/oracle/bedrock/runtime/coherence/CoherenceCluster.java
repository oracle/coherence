/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.deferred.DeferredHelper;
import com.oracle.bedrock.deferred.DeferredPredicate;
import com.oracle.bedrock.deferred.PermanentlyUnavailableException;
import com.oracle.bedrock.options.Decoration;
import com.oracle.bedrock.options.Decorations;
import com.oracle.bedrock.runtime.AbstractAssembly;
import com.oracle.bedrock.runtime.Assembly;
import com.oracle.bedrock.runtime.coherence.callables.GetAutoStartServiceNames;
import com.oracle.bedrock.runtime.coherence.callables.GetServiceStatus;
import com.oracle.bedrock.runtime.coherence.callables.IsCoherenceRunning;
import com.oracle.bedrock.runtime.coherence.callables.IsReady;
import com.oracle.bedrock.runtime.coherence.callables.IsSafe;
import com.oracle.bedrock.runtime.coherence.callables.IsServiceStorageEnabled;
import com.oracle.bedrock.runtime.concurrent.options.Caching;
import com.oracle.bedrock.runtime.concurrent.runnable.ThreadDump;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.util.Trilean;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.util.UID;
import com.tangosol.util.function.Remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.oracle.bedrock.deferred.DeferredHelper.ensure;
import static com.oracle.bedrock.deferred.DeferredHelper.eventually;
import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.predicate.Predicates.contains;
import static com.oracle.bedrock.predicate.Predicates.doesNotContain;
import static com.oracle.bedrock.predicate.Predicates.greaterThan;

public class CoherenceCluster
        extends AbstractAssembly<CoherenceClusterMember>
    {
    /**
     * Constructs a {@link CoherenceCluster} given a list of {@link CoherenceClusterMember}s.
     *
     * @param optionsByType the {@link OptionsByType} for the {@link CoherenceCluster}
     */
    public CoherenceCluster(OptionsByType optionsByType)
        {
        super(optionsByType);
        }

    /**
     * Returns {@code true} if all members of the cluster pass the ready health check.
     *
     * @return {@code true} if all members of the cluster pass the ready health check
     */
    public boolean isReady()
        {
        for (CoherenceClusterMember member : this)
            {
            if (!member.invoke(IsReady.INSTANCE))
                {
                return false;
                }
            }
        return true;
        }

    /**
     * Returns {@code true} if all members of the cluster pass the "safe" health check.
     *
     * @return {@code true} if all members of the cluster pass the "safe" health check
     */
    public boolean isSafe()
        {
        for (CoherenceClusterMember member : this)
            {
            if (!member.invoke(IsSafe.INSTANCE))
                {
                return false;
                }
            }
        return true;
        }

    /**
     * Obtains the current number of {@link CoherenceClusterMember}s in the underlying
     * {@link CoherenceCluster} by asking a {@link CoherenceClusterMember}.
     *
     * @return the current number of {@link CoherenceClusterMember}s
     */
    @SuppressWarnings("resource")
    public int getClusterSize()
        {
        Iterator<CoherenceClusterMember> members = iterator();

        return members.hasNext() ? members.next().getClusterSize() : 0;
        }


    /**
     * Obtains the member {@link UID}s for the {@link CoherenceCluster}.
     *
     * @return a {@link Set} of {@link UID}, one for each {@link CoherenceClusterMember}
     */
    @SuppressWarnings("resource")
    public Set<UID> getClusterMemberUIDs()
        {
        Iterator<CoherenceClusterMember> members = iterator();

        return members.hasNext() ? members.next().getClusterMemberUIDs() : Collections.emptySet();
        }


    /**
     * Obtains a proxy of the specified {@link NamedCache} available in the
     * {@link CoherenceCluster}.
     *
     * @param cacheName the name of the {@link NamedCache}
     * @return a proxy to the {@link NamedCache}
     */
    @SuppressWarnings({"resource", "rawtypes"})
    public NamedCache getCache(String cacheName)
        {
        Iterator<CoherenceClusterMember> members = iterator();

        return members.hasNext() ? members.next().getCache(cacheName) : null;
        }


    /**
     * Obtains a proxy of the specified {@link NamedCache} available in the
     * {@link CoherenceCluster}.
     *
     * @param cacheName  the name of the {@link NamedCache}
     * @param keyClass   the type of the keys for the {@link NamedCache}
     * @param valueClass the type of the values for the {@link NamedCache}
     * @param <K>        the type of the key class
     * @param <V>        the type of the value class
     * @return a proxy to the {@link NamedCache}
     */
    @SuppressWarnings("resource")
    public <K, V> NamedCache<K, V> getCache(
            String cacheName,
            Class<K> keyClass,
            Class<V> valueClass)
        {
        Iterator<CoherenceClusterMember> members = iterator();

        return members.hasNext() ? members.next().getCache(cacheName, keyClass, valueClass) : null;
        }


    @Override
    protected void onRelaunching(
            CoherenceClusterMember member,
            OptionsByType optionsByType)
        {
        // get the current MemberUID and record it (or make the application remember it)
        UID memberUID = member.getLocalMemberUID();

        // add the member as a decoration to the OptionsByType
        optionsByType.add(Decoration.of(memberUID));

        // notify the assembly of the change
        onChanged(optionsByType);
        }


    @Override
    protected void onRelaunched(
            CoherenceClusterMember original,
            CoherenceClusterMember restarted,
            OptionsByType optionsByType)
        {
        // ensure that the original member UID is no longer in the cluster
        Decorations decorations       = optionsByType.get(Decorations.class);
        Option[]    options           = optionsByType.asArray();
        UID         originalMemberUID = decorations.get(UID.class);

        if (originalMemberUID != null)
            {
            // ensure that the restarted member is in the member set of the cluster
            ensure(eventually(invoking(this).getClusterMemberUIDs()), doesNotContain(originalMemberUID), options);
        }

        // ensure the restarted member has joined the cluster
        // (without doing this the local member id returned below may be different from
        // the one when the member joins the cluster)
        ensure(eventually(invoking(restarted).getClusterSize()), greaterThan(1), options);

        // determine the UID of the restarted member
        UID restartedMemberUID = restarted.getLocalMemberUID();

        // ensure that the restarted member is in the member set of the cluster
        ensure(eventually(invoking(this).getClusterMemberUIDs()), contains(restartedMemberUID), options);

        // notify the assembly of the change
        onChanged(optionsByType);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void onChanged(OptionsByType options) {
    try
        {
        StabilityPredicate<Assembly<?>> stabilityPredicate = options.getOrDefault(StabilityPredicate.class, null);
        if (stabilityPredicate != null)
            {
            DeferredPredicate<?> deferredPredicate = new DeferredPredicate(this, stabilityPredicate.get());
            DeferredHelper.ensure(DeferredHelper.eventually(deferredPredicate), com.oracle.bedrock.predicate.Predicates.is(true), options.asArray());
            }
        }
    catch (PermanentlyUnavailableException e)
        {
        CoherenceClusterMember[] aMember = this.applications.toArray(CoherenceClusterMember[]::new);
        CompletableFuture[]      aFuture = new CompletableFuture[aMember.length];

        for (int i = 0; i < aMember.length; i++)
            {
            try
                {
                aFuture[i] = aMember[i].submit(ThreadDump.toStdErr());
                }
            catch (Exception ignored)
                {
                // ignored
                }
            }

        try
            {
            CompletableFuture.allOf(aFuture).get(2, TimeUnit.MINUTES);
            }
        catch (Exception ex)
            {
            System.err.println("Caught exception waiting for thread dumps to complete " + ex.getMessage());
            }
        throw e;
        }
    }


    /**
     * Useful {@link Predicate}s for a {@link CoherenceCluster}.
     */
    public interface Predicates
        {
        /**
         * A {@link Predicate} to determine if all the services of
         * {@link CoherenceClusterMember}s are safe.
         *
         * @return a {@link Predicate}
         */
        static Predicate<CoherenceCluster> autoStartServicesSafe()
            {
            return IsAutoStartServicesSafePredicate.INSTANCE;
            }

        /**
         * A {@link Predicate} to determine if {@link CoherenceCluster}
         *  is running.
         *
         * @return a {@link Predicate}
         */
        static Predicate<CoherenceCluster> isCoherenceRunning()
            {
            return new IsCoherenceRunningPredicate(Set.of(Coherence.DEFAULT_NAME));
            }

        /**
         * A {@link Predicate} to determine if all health checks in the
         * {@link CoherenceCluster} are ready.
         *
         * @return a {@link Predicate}
         */
        static Predicate<CoherenceCluster> isReady()
            {
            return IsReadyPredicate.INSTANCE;
            }

        /**
         * A {@link Predicate} to determine if all health checks in the
         * {@link CoherenceCluster} are ready.
         *
         * @param sHealthCheck  the name of an additional health check to verify
         *
         * @return a {@link Predicate}
         */
        static Predicate<CoherenceCluster> isReady(String sHealthCheck)
            {
            return isCoherenceRunning(Set.of(Coherence.DEFAULT_NAME));
            }

        /**
         * A {@link Predicate} to determine if {@link CoherenceCluster}
         *  is running.
         *
         * @param asName  the names of the Coherence instances to verify
         *
         * @return a {@link Predicate}
         */
        static Predicate<CoherenceCluster> isCoherenceRunning(String... asName)
            {
            return isCoherenceRunning(Set.of(asName));
            }

        /**
         * A {@link Predicate} to determine if {@link CoherenceCluster}
         *  is running.
         *
         * @param setName  the names of the Coherence instances to verify
         *
         * @return a {@link Predicate}
         */
        static Predicate<CoherenceCluster> isCoherenceRunning(Set<String> setName)
            {
            return new IsCoherenceRunningPredicate(setName);
            }
        }

    // ----- IsAutoStartServicesSafePredicate -------------------------------

    static class IsAutoStartServicesSafePredicate
            implements Remote.Predicate<CoherenceCluster>
        {
        @Override
        public boolean test(CoherenceCluster cluster)
            {
            // determine the number of each auto start service is defined by the cluster
            HashMap<String, Integer> serviceCountMap = new HashMap<>();

            for (CoherenceClusterMember member : cluster)
                {
                Set<String> serviceNames = member.invoke(new GetAutoStartServiceNames(), Caching.enabled());

                for (String serviceName : serviceNames)
                    {
                    // determine if the service is storage enabled?
                    Trilean trilean = member.invoke(new IsServiceStorageEnabled(serviceName),
                                                    Caching.enabled());

                    // only adjust the service count when it's not storage disabled
                    // (ie: we count storage enabled and unknown service types)
                    int adjust = trilean == Trilean.FALSE ? 0 : 1;

                    serviceCountMap.compute(serviceName,
                                            (name, count) -> count == null ? adjust : count + adjust);
                    }
                }

            // ensure the autostart services defined by each member are safe
            // according to the number of required services
            for (CoherenceClusterMember member : cluster)
                {
                Set<String> serviceNames = member.invoke(new GetAutoStartServiceNames(), Caching.enabled());

                for (String serviceName : serviceNames)
                    {
                    int count = serviceCountMap.get(serviceName);

                    if (count > 1)
                        {
                        ServiceStatus status = member.invoke(new GetServiceStatus(serviceName));

                        if (status == null
                                || status == ServiceStatus.ENDANGERED
                                || status == ServiceStatus.ORPHANED
                                || status == ServiceStatus.STOPPED
                                || status == ServiceStatus.UNKNOWN)
                            {
                            return false;
                            }
                        }
                    else if (count == 1)
                        {
                        ServiceStatus status = member.invoke(new GetServiceStatus(serviceName));

                        if (status == null
                                || status == ServiceStatus.STOPPED
                                || status == ServiceStatus.UNKNOWN)
                            {
                            return false;
                            }
                        }
                    }
                }

            return true;
            }

        @Override
        public String toString()
            {
            return "IsReadyPredicate";
            }

        // ----- data members -----------------------------------------------

        static final IsAutoStartServicesSafePredicate INSTANCE = new IsAutoStartServicesSafePredicate();
        }

    // ----- IsReadyPredicate -----------------------------------------------

    static class IsReadyPredicate
            implements Remote.Predicate<CoherenceCluster>
        {
        @Override
        public boolean test(CoherenceCluster cluster)
            {
            for (CoherenceClusterMember member : cluster)
                {
                if (!member.invoke(IsReady.INSTANCE))
                    {
                    return false;
                    }
                }
            return true;
            }

        @Override
        public String toString()
            {
            return "IsReadyPredicate";
            }

        // ----- data members -----------------------------------------------

        static final IsReadyPredicate INSTANCE = new IsReadyPredicate();
        }

    // ----- IsCoherenceRunningPredicate ------------------------------------

    static class IsCoherenceRunningPredicate
            implements Remote.Predicate<CoherenceCluster>
        {
        public IsCoherenceRunningPredicate(Set<String> setNames)
            {
            f_setNames = setNames;
            }

        @Override
        public boolean test(CoherenceCluster cluster)
            {
            for (CoherenceClusterMember member : cluster)
                {
                if (!member.invoke(new IsCoherenceRunning(f_setNames)))
                    {
                    return false;
                    }
                }
            return true;
            }

        @Override
        public String toString()
            {
            return "IsCoherenceRunningPredicate(coherence=" + f_setNames + ")";
            }

        // ----- data members -----------------------------------------------

        private final Set<String> f_setNames;
        }
    }
