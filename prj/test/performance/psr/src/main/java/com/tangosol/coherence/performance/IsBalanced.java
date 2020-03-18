/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.coherence.callables.GetServiceStatus;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Service;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.util.Base;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Enumeration;

/**
 * @author jk 2015.11.27
 */
public class IsBalanced
        implements RemoteCallable<Boolean>
    {

    public IsBalanced(ServiceStatus status)
        {
        m_status = status;
        }

    @Override
    public Boolean call() throws Exception
        {
        try
            {
            Cluster     cluster     = CacheFactory.ensureCluster();
            Enumeration enumeration = cluster.getServiceNames();
            int         memberID    = cluster.getLocalMember().getId();
            MBeanServer mBeanServer = MBeanHelper.findMBeanServer();

            while(enumeration.hasMoreElements())
                {
                String  sServiceName = (String) enumeration.nextElement();
                Service service      = cluster.getService(sServiceName);

                if (service instanceof SafeService)
                    {
                    service = ((SafeService) service).getService();
                    }

                if (service instanceof PartitionedCache)
                    {
                    GetServiceStatus getServiceStatus = new GetServiceStatus(sServiceName);
                    ServiceStatus    serviceStatus    = getServiceStatus.call();

                    if (serviceStatus != m_status)
                        {
                        return false;
                        }

                    String     sMBeanName = "Coherence:type=Service,name=" + sServiceName + ",nodeId=" + memberID;
                    ObjectName objectName = new ObjectName(sMBeanName);
                    Integer    unbalanced = (Integer) mBeanServer.getAttribute(objectName, "PartitionsUnbalanced");

                    if (unbalanced == null || unbalanced != 0)
                        {
                        return false;
                        }
                    }
                }

            return true;
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            throw Base.ensureRuntimeException(e);
            }
        }

    private ServiceStatus m_status;
    }
