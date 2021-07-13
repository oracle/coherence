/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.net.CacheFactory;
import com.tangosol.util.ExternalizableHelper;
import common.SystemPropertyIsolation;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * Validate the default lambdas configuration based on coherence license-mode.
 *
 * @author jf  2020.06.18
 */
@SuppressWarnings("unchecked")
public class DefaultLambdasTest
    {
    @Test
    public void ensureDefaultLamdbasByLicenseMode()
        {
        Assume.assumeThat("Skip testing the lambda serialization mode default when ExternalizableHelper.LAMBDA_SERIALIZATION is not empty string",
                          ExternalizableHelper.LAMBDA_SERIALIZATION, is(""));

        String  sMode = CacheFactory.getLicenseMode();
        boolean fProdMode = "prod".equalsIgnoreCase(sMode);

        assertThat("assert lamdba serialization mode give coherence license-mode of " + sMode,
                   Lambdas.isStaticLambdas(), is(fProdMode));

        assertThat("assert lamdba serialization mode give coherence license-mode of " + sMode,
                   Lambdas.isDynamicLambdas(), is(!fProdMode));

        System.out.println("Validated coherence production mode: " + fProdMode);
        }

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
