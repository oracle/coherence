/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.coherence.component.util.SafeService;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.11.17
 */
public class SafeServiceTest
    {
    @Test
    public void shouldReturnNullImplementationResourceRegistryIfServiceIsNull() throws Exception
        {
        SafeService safeService = new SafeService();

        safeService.setInternalService(null);

        ResourceRegistry registry = safeService.getResourceRegistry();

        assertThat(registry, is(notNullValue()));
        assertThat(registry, is(instanceOf(SimpleResourceRegistry.class)));
        }
    }
