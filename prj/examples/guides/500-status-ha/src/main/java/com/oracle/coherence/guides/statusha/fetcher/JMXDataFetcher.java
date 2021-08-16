/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha.fetcher;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import static com.tangosol.net.management.Registry.CLUSTER_TYPE;

import com.oracle.coherence.guides.statusha.model.ServiceData;

import com.tangosol.net.management.Registry;


/**
 * An implementation of a {@link DataFetcher} that uses a {@link MBeanServerConnection}
 * to retrieve data from a Coherence cluster via a remote JMX connection.
 *
 * @author tam 2021.08.02
 */
public class JMXDataFetcher
        extends AbstractDataFetcher {

    /**
     * {@link MBeanServerConnection} to connect to.
     */
    private final MBeanServerConnection mbs;

    /**
     * Constructs a {@link JMXDataFetcher} for the given JMX URL and service.
     *
     * @param jmxConnectionURL JMX URL
     * @param serviceName      optional service name
     */
    // #tag::constructor[]
    public JMXDataFetcher(String jmxConnectionURL, String serviceName) {
        super(serviceName);

        try {
            JMXConnector connect = JMXConnectorFactory.connect(new JMXServiceURL(jmxConnectionURL));  // <1>
            mbs = connect.getMBeanServerConnection();  // <2>
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to JMX Url " + jmxConnectionURL, e);
        }
        // #end::constructor[]

        // retrieve cluster name and version
        try {
            AttributeList listAttributes = mbs.getAttributes(new ObjectName("Coherence:" + CLUSTER_TYPE),
                    new String[]{ATTR_CLUSTER_NAME, ATTR_CLUSTER_VERSION});

            setClusterName(getAttributeValue(listAttributes, ATTR_CLUSTER_NAME));
            setClusterVersion(getAttributeValue(listAttributes, ATTR_CLUSTER_VERSION));
        } catch (Exception e) {
            throw new RuntimeException("Unable to get cluster attributes", e);
        }
    }

    // #tag::getStatusHaData[]
    @Override
    public Set<ServiceData> getStatusHaData() {
        Set<ServiceData> setData = new HashSet<>();
        getMBeans().forEach(bean -> {
            String serviceName = extractService(bean);
            AttributeList listServiceAttrs;

            String sQuery = COHERENCE + Registry.SERVICE_TYPE + ",name=" + serviceName + ",*";
            try {
                Set<ObjectName> setServices = mbs.queryNames(new ObjectName(sQuery), null); // <1>
                if (setServices.size() == 0) {
                    throw new RuntimeException("Cannot query for service " + serviceName);
                }
                String mbean = setServices.stream().findAny().map(ObjectName::toString).get();

                listServiceAttrs = mbs.getAttributes(new ObjectName(mbean), new String[]{ // <2>
                        ATTR_PARTITIONS_VULNERABLE, ATTR_PARTITIONS_UNBALANCED, ATTR_PARTITIONS_ENDANGERED,
                        ATTR_STATUS_HA, ATTR_STORAGE_ENABLED_COUNT, ATTR_PARTITION_COUNT
                });
            } catch (Exception e) {
                throw new RuntimeException("Unable to find attributes for " + sQuery, e);
            }

            int partitionCount = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_PARTITION_COUNT));
            int storageCount = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_STORAGE_ENABLED_COUNT));
            String statusHA = getSafeStatusHA(getAttributeValue(listServiceAttrs, ATTR_STATUS_HA));
            int vulnerable = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_PARTITIONS_VULNERABLE));
            int unbalanced = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_PARTITIONS_UNBALANCED));
            int endangered = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_PARTITIONS_ENDANGERED));

            setData.add(new ServiceData(serviceName, storageCount, statusHA, partitionCount,
                    vulnerable, unbalanced, endangered));

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
     * Returns a {@link Set} of MBeans for the services.
     *
     * @return a {@link Set} of MBeans for the services
     */
    private Set<String> getMBeans() {
        try {
            return mbs.queryNames(new ObjectName(getDistributionCoordinatorQuery(getServiceName())), null)
                    .stream()
                    .map(ObjectName::toString)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException("Unable to query MBeans", e);
        }
    }

    /**
     * Return the value for the attribute name in the given AttributeList.
     *
     * @param listAttr an AttributeList
     * @param name    the attribute name
     * @return the value of the attribute
     */
    public static String getAttributeValue(AttributeList listAttr, String name) {
        for (Object oValue : listAttr) {
            javax.management.Attribute attr = (javax.management.Attribute) oValue;
            if (attr.getName().equals(name)) {
                return (attr.getValue() == null ? "" : attr.getValue().toString());
            }
        }

        throw new RuntimeException("Unable to get attribute " + name);
    }
}
