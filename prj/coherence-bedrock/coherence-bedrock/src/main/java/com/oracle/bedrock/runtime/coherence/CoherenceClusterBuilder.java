/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.AbstractAssemblyBuilder;
import com.oracle.bedrock.runtime.AssemblyBuilder;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

public class CoherenceClusterBuilder
        extends AbstractAssemblyBuilder<CoherenceClusterMember, CoherenceCluster, CoherenceClusterBuilder>
    {
    @Override
    public CoherenceCluster createAssembly(OptionsByType optionsByType)
        {
        // introduce a StabilityPredicate
        optionsByType.addIfAbsent(StabilityPredicate.of(CoherenceCluster.Predicates.autoStartServicesSafe()));

        return new CoherenceCluster(optionsByType);
        }
    }
                                    