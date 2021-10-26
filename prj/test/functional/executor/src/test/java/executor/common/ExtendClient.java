/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;

import com.tangosol.net.ConfigurableCacheFactory;

/**
 * Custom {@link ExtendClient} that will add a logging destination based on the provided label.
 *
 * @author  rl 8.4.2021
 * @since 21.12
 */
public class ExtendClient
        extends com.oracle.bedrock.junit.ExtendClient
    {
    // ----- constructors ---------------------------------------------------

    public ExtendClient(String sCacheConfigURI, String sLabel)
        {
        super(sCacheConfigURI);
        this.sLabel = sLabel;
        }

    // ----- ExtendClient methods -------------------------------------------

    public ConfigurableCacheFactory build(LocalPlatform platform, CoherenceCluster cluster, OptionsByType optionsByType)
        {
        optionsByType.add(LogOutput.to(sLabel, "ExtendClient"));
        return super.build(platform, cluster, optionsByType);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Log destination label.
     */
    protected String sLabel;
    }
