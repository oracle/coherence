/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.ConfigurableCacheFactory;

public interface SessionBuilder
{
    /**
     * Creates a {@link ConfigurableCacheFactory} for a Coherence Session.
     *
     * @param platform       the {@link LocalPlatform} on which the {@link ConfigurableCacheFactory} will be established
     * @param cluster        the {@link CoherenceCluster} for which the session will be created
     * @param optionsByType  the {@link OptionsByType}s provided to all of the {@link CoherenceClusterMember}s
     *                       when establishing the {@link CoherenceCluster}
     *
     * @return a {@link ConfigurableCacheFactory}
     */
    ConfigurableCacheFactory build(LocalPlatform platform,
                                   CoherenceCluster cluster,
                                   OptionsByType optionsByType);
}
