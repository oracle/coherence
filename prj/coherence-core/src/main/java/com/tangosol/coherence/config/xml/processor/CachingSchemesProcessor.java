/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.ServiceSchemeRegistry;
import com.tangosol.coherence.config.scheme.AbstractServiceScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.service.ServiceDependencies;
import com.tangosol.internal.net.service.grid.DefaultPartitionedServiceDependencies;
import com.tangosol.internal.net.service.grid.PartitionedServiceDependencies;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.util.Map;

import static com.tangosol.internal.net.service.grid.DefaultPartitionedServiceDependencies.DEFAULT_SERVICE_PARTITIONS;
import static com.tangosol.internal.net.service.grid.DefaultPartitionedServiceDependencies.PROP_SERVICE_PARTITIONS;

/**
 * A {@link CachingSchemesProcessor} is an {@link ElementProcessor} for the
 * &lt;caching-schemes%gt; element of Coherence Cache Configuration files.
 *
 * @author bo  2012.05.02
 * @since Coherence 12.1.2
 */
@XmlSimpleName("caching-schemes")
public class CachingSchemesProcessor
        implements ElementProcessor<ServiceSchemeRegistry>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceSchemeRegistry process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // process the children of the <caching-schemes>
        Map<String, ?> mapProcessedChildren = context.processElementsOf(xmlElement);

        // add all of the ServiceSchemes to the ServiceSchemeRegistry
        CacheConfig cacheConfig = context.getCookie(CacheConfig.class);
        Base.azzert(cacheConfig != null);

        ServiceSchemeRegistry registry = cacheConfig.getServiceSchemeRegistry();
        Base.azzert(registry != null);

        for (Object oChild : mapProcessedChildren.values())
            {
            if (oChild instanceof ServiceScheme)
                {
                ServiceScheme       scheme       = (ServiceScheme) oChild;
                ServiceDependencies depsService  = (ServiceDependencies) ((AbstractServiceScheme) scheme).getServiceDependencies();
                String              sNameService = scheme.getScopedServiceNameForProperty();

                if (depsService instanceof PartitionedServiceDependencies)
                    {
                    // Note: service resource injection has already configured depsService.getPreferredPartitionCount() from distributed service cache configuration element <partition-count>.

                    int cPartitions = ((PartitionedServiceDependencies) depsService).getPreferredPartitionCount();

                    // override distributed service partition count using "coherence.service.*" system properties.
                    int cPartitionsOverride = Config.getInteger(String.format(PROP_SERVICE_PARTITIONS, sNameService),
                                                        DEFAULT_SERVICE_PARTITIONS == -1
                                                            ? cPartitions
                                                            : DEFAULT_SERVICE_PARTITIONS);

                    ((DefaultPartitionedServiceDependencies) depsService).setPreferredPartitionCount(cPartitionsOverride);
                    if (cPartitionsOverride != cPartitions)
                        {
                        Logger.config("Configured service " + scheme.getScopedServiceName() + " using partition count of " + cPartitions);
                        }
                    }

                registry.register((ServiceScheme) oChild);
                }
            }

        // process all of the foreign elements
        // (this allows the elements to modify the configuration)
        context.processForeignElementsOf(xmlElement);

        return registry;
        }
    }
