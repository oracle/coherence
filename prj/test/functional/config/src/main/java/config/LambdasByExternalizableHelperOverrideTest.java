/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package config;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.util.ExternalizableHelper;
import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Validate dynamic lambda specified in custom externalizable config.
 *
 * @author jf  2021.06.15
 */
@SuppressWarnings("unchecked")

public class LambdasByExternalizableHelperOverrideTest
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty("coherence.externalizable.config", "ExternalizableHelperDynamicLambdas.xml");
        }

    @Test
    public void shouldBeDynamicLambdas()
        {
        assertEquals("dynamic", ExternalizableHelper.LAMBDA_SERIALIZATION);
        assertTrue(Lambdas.isDynamicLambdas());
        }

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
