/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.AddressProviderFactory;

/**
 * AddressProviderBuilder interface
 *
 * @author pfm  2013.09.12
 * @since Coherence 12.1.3
 */
public interface AddressProviderBuilder
    extends ParameterizedBuilder<AddressProvider>, AddressProviderFactory
    {
    }
