/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
 * An AddressProvider that additionally allows to retrieve a human readable
 * description of underlying addresses without doing a DNS lookup.
 */
public interface DescribableAddressProvider
        extends AddressProvider
    {
    /**
     * Retrieve a human readable description of underlying addresses.
     *
     * @return a string array of addresses in human readable format
     */
    public String[] getAddressDescriptions();
    }
