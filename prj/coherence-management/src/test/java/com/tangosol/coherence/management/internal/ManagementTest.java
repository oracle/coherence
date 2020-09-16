/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import com.tangosol.internal.net.management.HttpHelper;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.Base;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2019.05.15
 */
public class ManagementTest
    {
    @Test
    public void shouldBeHttpCapable()
        {
        boolean fEnabled = HttpHelper.isHttpCapable(Base.getContextClassLoader());
        assertThat(fEnabled, is(true));
        }

    @Test
    public void shouldStartManagement()
        {
        boolean fStarted = HttpHelper.startService(CacheFactory.ensureCluster());
        assertThat(fStarted, is(true));
        }

    }
