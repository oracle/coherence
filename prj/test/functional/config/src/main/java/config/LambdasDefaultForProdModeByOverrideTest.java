/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package config;

import com.tangosol.internal.util.invoke.Lambdas;
import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 * Validate dynamic lambda in production mode.
 *
 * @author jf  2021.06.15
 */
@SuppressWarnings("unchecked")
public class LambdasDefaultForProdModeByOverrideTest
    {
    @BeforeClass
    public static void setup()
        {
        // ensure license mode is configured purely by override file setting license-mode to prod
        System.clearProperty("coherence.mode");

        // ensure validating default lambdas
        System.clearProperty("coherence.lambdas");

        System.setProperty("coherence.override", "tangosol-coherence-override-prod-mode.xml");
        }

    @Test
    public void shouldDefaultToDynamicLambdaForProductionMode()
        {
        assertTrue(Lambdas.isDynamicLambdas());
        }

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
