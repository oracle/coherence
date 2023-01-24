/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.bank;

/**
 * Constants to use to safely identify cache names.
 *
 * @author Jonathan Knight 2023.01.14
 * @since 22.06.4
 */
public interface BankCacheNames
    {
    /**
     * The name of the customers cache.
     */
    String CUSTOMERS = "customers";

    /**
     * The name of the customers cache.
     */
    String ACCOUNTS = "accounts";
    }
