/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha.fetcher;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.coherence.guides.statusha.model.ServiceData;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Filters;

import static com.tangosol.net.management.Registry.CLUSTER_TYPE;

/**
 * An implementation of a {@link DataFetcher} that uses the {@link MBeanServerProxy}
 * to retrieve data from the cluster.
 *
 * @author tam 2021.08.02
 */
public class MBeanServerProxyDataFetcher
        extends AbstractDataFetcher {

    /**
     * {@link MBeanServerProxy} used to query MBeans.
     */
    private final MBeanServerProxy proxy;

    /**
     * Constructs the fetcher and optionally provide a service name.
     *
     * @param serviceName optional service name.
     */
    // #tag::constructor[]
    public MBeanServerProxyDataFetcher(String serviceName) {
        super(serviceName);

        // be as quiet as we can 
        System.setProperty("coherence.log.level", "1");
        Registry registry = CacheFactory.ensureCluster().getManagement(); // <1>
        if (registry == null) {
            throw new RuntimeException("Unable to get registry from cluster");
        }

        proxy = registry.getMBeanServerProxy(); // <2>

        if (proxy == null) {
            throw new RuntimeException("Unable to get MBeanServerProxy");
        }
        // #end::constructor[]

        // retrieve cluster name and version
        Optional<String> cluster = proxy.queryNames(COHERENCE + CLUSTER_TYPE, null).stream().findAny();
        if (cluster.isPresent()) {
            String sMBean = cluster.get();
            Map<String, Object> mapAttrs = proxy.getAttributes(sMBean, Filters.always());
            setClusterName((String) mapAttrs.get(ATTR_CLUSTER_NAME));
            setClusterVersion((String) mapAttrs.get(ATTR_CLUSTER_VERSION));
        } else {
            throw new RuntimeException("Unable to find Cluster MBean");
        }
    }

    // #tag::getStatusHaData[]
    @Override
    public Set<ServiceData> getStatusHaData() {
        Set<ServiceData> setData = new HashSet<>();
        getMBeans().forEach(bean -> {  // <1>
            String sServiceName = extractService(bean);

            // retrieve values from one node as all of them will have the same values
            Optional<String> serviceMBean =
                    proxy.queryNames(COHERENCE + Registry.SERVICE_TYPE + ",name=" + sServiceName + ",*", null)
                         .stream().findAny(); // <2>

            if (!serviceMBean.isPresent()) {
                throw new RuntimeException("Unable to find ServiceMBean for service " + sServiceName);
            }

            String sServiceMbean = serviceMBean.get();
            Map<String, Object> mapServiceAttr = proxy.getAttributes(sServiceMbean, Filters.always()); // <3>

            String sStatusHA = getSafeStatusHA((String) mapServiceAttr.get(ATTR_STATUS_HA));
            int nPartitionCount = Integer.parseInt(mapServiceAttr.get(ATTR_PARTITION_COUNT).toString());
            int nStorageCount = Integer.parseInt(mapServiceAttr.get(ATTR_STORAGE_ENABLED_COUNT).toString());
            int nVulnerable = Integer.parseInt(mapServiceAttr.get(ATTR_PARTITIONS_VULNERABLE).toString());
            int nUnbalanced = Integer.parseInt(mapServiceAttr.get(ATTR_PARTITIONS_UNBALANCED).toString());
            int nEndangered = Integer.parseInt(mapServiceAttr.get(ATTR_PARTITIONS_ENDANGERED).toString());

            setData.add(new ServiceData(sServiceName, nStorageCount, sStatusHA, nPartitionCount,
                    nVulnerable, nUnbalanced, nEndangered)); // <4>
        });
        return setData;
    }
    // #end::getStatusHaData[]

    @Override
    public Set<String> getServiceNames() {
        return getMBeans().stream().map(MBeanServerProxyDataFetcher::extractService)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the set of MBeans for the distribution coordinator.
     *
     * @return the set of MBeans for the distribution coordinator
     */
    private Set<String> getMBeans() {
        return proxy.queryNames(getDistributionCoordinatorQuery(getServiceName()), null);
    }
}
