/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package lambda.framework;

import com.oracle.bedrock.junit.CoherenceClusterOrchestration;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.options.LocalHost;

import com.oracle.bedrock.runtime.java.options.SystemProperty;

/**
 * Common cluster ExternalResource used by all Lambda tests
 *
 * @author phfry 2017.05.22
 */
public class LambdaTestCluster extends CoherenceClusterOrchestration
    {
    public LambdaTestCluster()
        {
        super();
        this.withOptions(SystemProperty.of("coherence.nameservice.address",
                LocalPlatform.get().getLoopbackAddress().getHostAddress()))
            .withOptions(LocalHost.only());
        }
    }
