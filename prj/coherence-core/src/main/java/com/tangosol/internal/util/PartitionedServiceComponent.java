/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;


import com.tangosol.net.PartitionedService;


/**
 * PartitionedServiceComponent is an internal interface to expose internal
 * methods in the PartitionedService TDE component.
 *
 * @author gg 2015.12.10
 */
public interface PartitionedServiceComponent
        extends GridComponent,
                PartitionedService
    {
    }
