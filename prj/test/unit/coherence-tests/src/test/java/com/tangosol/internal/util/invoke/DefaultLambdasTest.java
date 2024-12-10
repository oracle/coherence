/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.net.CacheFactory;
import com.tangosol.util.ExternalizableHelper;
import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * Validate the default lambdas.
 *
 * @author jf  2020.06.18
 */
public class DefaultLambdasTest
    {
    @Test
    public void ensureDefaultLamdbas()
        {
        Assume.assumeThat("Skip testing the lambda serialization mode default when ExternalizableHelper.LAMBDA_SERIALIZATION is not empty string",
                          ExternalizableHelper.LAMBDA_SERIALIZATION, is(""));

        assertThat("default lambdas must default to dynamic lambdas", true, is(Lambdas.isDynamicLambdas()));
        assertThat("default lambdas must default to dynamic lambdas", false, is(Lambdas.isStaticLambdas()));
        }

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
