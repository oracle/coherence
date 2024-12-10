/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Service;

public class GetServiceStatus
        implements RemoteCallable<ServiceStatus>
    {
    /**
     * The name of the service.
     */
    private final String serviceName;


    /**
     * Constructs an {@link GetServiceStatus}
     *
     * @param serviceName the name of the service
     */
    public GetServiceStatus(String serviceName)
        {
        this.serviceName = serviceName;
        }


    @Override
    public ServiceStatus call() throws Exception
        {
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();
        Service service = cluster == null ? null : cluster.getService(serviceName);

        if (service == null)
            {
            return null;
            }
        else if (service.isRunning())
            {
            // SafeDistributedCacheServices provide a lot more fidelity!
            if (service instanceof PartitionedService)
                {
                PartitionedService partitionedService = (PartitionedService) service;

                // the service may be initializing, and thus we might fail asking it for information
                try
                    {
                    int backupStrength = partitionedService.getBackupStrength();

                    switch (backupStrength)
                        {
                        case 0:
                            return ServiceStatus.ORPHANED;

                        case 1:
                            return ServiceStatus.ENDANGERED;

                        case 2:
                            return ServiceStatus.NODE_SAFE;

                        case 3:
                            return ServiceStatus.MACHINE_SAFE;

                        case 4:
                            return ServiceStatus.RACK_SAFE;

                        case 5:
                            return ServiceStatus.SITE_SAFE;

                        default:
                            return ServiceStatus.UNKNOWN;
                        }
                    }
                catch (Exception e)
                    {
                    return ServiceStatus.UNKNOWN;
                    }
                }
            else
                {
                return ServiceStatus.RUNNING;
                }

            }
        else
            {
            return ServiceStatus.STOPPED;
            }
        }
    }
