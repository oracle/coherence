/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha.fetcher;

import com.oracle.coherence.guides.statusha.model.ServiceData;

import java.util.Set;

/**
 * An interface that defines a contract to fetch data.
 *
 * @author tam 2021.07.30
 */
// #tag::class[]
public interface DataFetcher {
    /**
     * Returns the cluster name.
     *
     * @return the cluster name
     */
    String getClusterName();

    /**
     * Returns the cluster version.
     *
     * @return the cluster version.
     */
    String getClusterVersion();

    /**
     * Returns the {@link ServiceData}.
     *
     * @return the {@link ServiceData}
     */
    Set<ServiceData> getStatusHaData();

    /**
     * Returns the {@link Set} of service names that are partitioned services.
     *
     * @return the {@link Set} of service names that are partitioned services
     */
    Set<String> getServiceNames();
}
// #end::class[]
