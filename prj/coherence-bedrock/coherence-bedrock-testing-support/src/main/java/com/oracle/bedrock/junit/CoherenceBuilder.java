/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;

/**
 * A builder that produces {@link Coherence} instances.
 *
 * @author  Jonathan Knight 2022.06.25
 * @since 22.06
 */
public interface CoherenceBuilder
    {
    /**
     * Creates a {@link Coherence} instance with a single default {@link com.tangosol.net.Session}.
     *
     * @param platform      the {@link LocalPlatform} on which the {@link ConfigurableCacheFactory} will be established
     * @param cluster       the {@link CoherenceCluster} for which the session will be created
     * @param optionsByType the {@link OptionsByType}s provided to all of the {@link CoherenceClusterMember}s
     *                      when establishing the {@link CoherenceCluster}
     * @return a {@link Coherence} instance
     */
    Coherence build(LocalPlatform platform, CoherenceCluster cluster, OptionsByType optionsByType);

    /**
     * Creates a {@link Coherence} instance with a single default {@link com.tangosol.net.Session}.
     *
     * @param platform  the {@link LocalPlatform} on which the {@link ConfigurableCacheFactory} will be established
     * @param cluster   the {@link CoherenceCluster} for which the session will be created
     * @param options   the {@link OptionsByType}s provided to all of the {@link CoherenceClusterMember}s
     *                  when establishing the {@link CoherenceCluster}
     * @return a {@link Coherence} instance
     */
    default Coherence build(LocalPlatform platform, CoherenceCluster cluster, Option... options)
        {
        return build(platform, cluster, OptionsByType.of(options));
        }

    /**
     * Returns a {@link CoherenceBuilder} that builds a {@link Coherence} instance
     * that is a cluster member.
     *
     * @return a {@link CoherenceBuilder} that builds a {@link Coherence} instance
     *         that is a cluster member
     */
    static CoherenceBuilder clusterMember()
        {
        return withMode(Coherence.Mode.ClusterMember);
        }

    /**
     * Returns a {@link CoherenceBuilder} that builds a {@link Coherence} instance
     * that is a client.
     *
     * @return a {@link CoherenceBuilder} that builds a {@link Coherence} instance
     *         that is a client
     */
    static CoherenceBuilder client()
        {
        return withMode(Coherence.Mode.Client);
        }

    /**
     * Returns a {@link CoherenceBuilder} that builds a {@link Coherence} instance
     * that is a client.
     *
     * @return a {@link CoherenceBuilder} that builds a {@link Coherence} instance
     *         that is a client
     */
    static CoherenceBuilder fixedClient()
        {
        return withMode(Coherence.Mode.ClientFixed);
        }

    /**
     * Returns a {@link CoherenceBuilder} that builds a {@link Coherence} instance
     * that is a cluster member.
     *
     * @return a {@link CoherenceBuilder} that builds a {@link Coherence} instance
     *         that is a cluster member
     */
    static CoherenceBuilder withMode(Coherence.Mode mode)
        {
        if (mode == null)
            {
            throw new IllegalArgumentException("The mode parameter cannot be null");
            }

        return new AbstractCoherenceBuilder()
            {
            @Override
            protected Coherence.Mode getMode()
                {
                return mode;
                }
            };
        }
    }
