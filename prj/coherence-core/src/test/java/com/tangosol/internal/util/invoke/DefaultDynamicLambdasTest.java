/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.internal.util.invoke.Lambdas.SerializationMode;
import org.junit.Assume;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;

import static com.tangosol.internal.util.invoke.Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Ensure dynamic lambdas enabled by default.
 *
 * @author jf  2020.06.18
 */
@SuppressWarnings("unchecked")
public class DefaultDynamicLambdasTest
    {
    @Test
    public void ensureDynamicLamdbasEnabledByDefault()
        {
        Assume.assumeThat("Skip testing the default when system property " + LAMBDAS_SERIALIZATION_MODE_PROPERTY + " is set",
            System.getProperty(LAMBDAS_SERIALIZATION_MODE_PROPERTY), nullValue());

        assertTrue(Lambdas.isDynamicLambdas());
        }
    }
