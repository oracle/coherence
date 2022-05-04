/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda.framework;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

/**
 * Common cluster ExternalResource used by all Lambda tests
 *
 * @author phfry 2017.05.22
 */
public class LambdaTestCluster extends CoherenceClusterResource
    {
    public LambdaTestCluster()
        {
        super();
        this.with(ClusterName.of(this.getClass().getSimpleName()),
                  SystemProperty.of("coherence.nameservice.address", LocalPlatform.get().getLoopbackAddress().getHostAddress()),
                  LocalHost.only(),
                  SystemProperty.of("coherence.lambdas", Config.getProperty("coherence.lambdas")),
                  SystemProperty.of("coherence.mode", Config.getProperty("coherence.mode", "dev")),
                  SystemProperty.of("coherence.extend.enabled", "true"),
                  SystemProperty.of("coherence.clusterport", "7574"));
        this.include(2, LocalStorage.enabled());
        }
    }
