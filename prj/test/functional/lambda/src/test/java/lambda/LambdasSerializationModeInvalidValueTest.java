/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package lambda;


import com.tangosol.internal.util.invoke.Lambdas;

import common.SystemPropertyIsolation;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author jf  2020.07.27
 */
public class LambdasSerializationModeInvalidValueTest
    {
    @Test
    public void ensureInvalidLambdasSerializationModeDefaulted()
        {
        System.setProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, "InvalidValue");
        assertTrue("ensure dynamic is default mode", Lambdas.isDynamicLambdas());
        assertFalse("ensure statis is not default lambdas serialization mode", Lambdas.isStaticLambdas());
        }

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }