/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package lambda;


import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.CacheFactory;
import com.oracle.coherence.testing.SystemPropertyIsolation;

import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jf  2020.07.27
 */
public class LambdasSerializationModeInvalidValueTest
    {
    @Test
    public void ensureInvalidLambdasSerializationModeDefaulted()
        {
        System.setProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, "InvalidValue");

        // as of COH-30388 default lambdas mode is always dynamic, independent of coherence production mode
        assertThat("ensure lambdas default mode",
                   Lambdas.isDynamicLambdas(), is(true));
        }

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }