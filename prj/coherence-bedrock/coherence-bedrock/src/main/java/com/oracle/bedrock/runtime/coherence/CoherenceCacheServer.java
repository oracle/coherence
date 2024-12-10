/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationProcess;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.java.JavaApplicationProcess;

public class CoherenceCacheServer
        extends AbstractCoherenceClusterMember
    {
    /**
     * Constructs a {@link CoherenceCacheServer}.
     *
     * @param platform      the {@link Platform} on which the {@link Application} was launched
     * @param process       the underlying {@link ApplicationProcess} representing the {@link Application}
     * @param optionsByType the {@link OptionsByType} used to launch the {@link Application}
     */
    public CoherenceCacheServer(
            Platform platform,
            JavaApplicationProcess process,
            OptionsByType optionsByType)
        {
        super(platform, process, optionsByType);
        }
    }
