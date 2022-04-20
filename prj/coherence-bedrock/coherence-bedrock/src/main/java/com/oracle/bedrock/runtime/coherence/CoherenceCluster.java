/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.options.Decoration;
import com.oracle.bedrock.options.Decorations;
import com.oracle.bedrock.runtime.AbstractAssembly;
import com.oracle.bedrock.runtime.Assembly;
import com.oracle.bedrock.runtime.coherence.callables.GetAutoStartServiceNames;
import com.oracle.bedrock.runtime.coherence.callables.GetServiceStatus;
import com.oracle.bedrock.runtime.coherence.callables.IsServiceStorageEnabled;
import com.oracle.bedrock.runtime.concurrent.options.Caching;
import com.oracle.bedrock.util.Trilean;
import com.tangosol.net.NamedCache;
import com.tangosol.util.UID;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
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
     * Obtains the current number of {@link CoherenceClusterMember}s in the underlying
     * {@link CoherenceCluster} by asking a {@link CoherenceClusterMember}.
     *
     * @return the current number of {@link CoherenceClusterMember}s
     */
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
    public Set<UID> getClusterMemberUIDs()
        {
        Iterator<CoherenceClusterMember> members = iterator();

        return members.hasNext() ? members.next().getClusterMemberUIDs() : new TreeSet<UID>();
        }


    /**
     * Obtains a proxy of the specified {@link NamedCache} available in the
     * {@link CoherenceCluster}.
     *
     * @param cacheName the name of the {@link NamedCache}
     * @return a proxy to the {@link NamedCache}
     */
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


    /**
     * Useful {@link Predicate}s for a {@link CoherenceCluster}.
     */
    public interface Predicates
        {
        /**
         * A {@link Predicate} to determine if all of the services of
         * {@link CoherenceClusterMember}s are safe.
         *
         * @return a {@link Predicate}
         */
        static Predicate<CoherenceCluster> autoStartServicesSafe()
            {
            return (cluster) -> {

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
            };
            }
        }
    }
