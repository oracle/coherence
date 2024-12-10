/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package config;


import com.oracle.coherence.testing.SystemPropertyIsolation;
import com.oracle.coherence.testing.SystemPropertyResource;
import com.tangosol.internal.util.invoke.Lambdas;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class DynamicLambdasOperationalOverrideTests
        extends AbstractLambdasOperationalOverrideTests
    {
    @BeforeClass
    public static void setup()
        {
        // ensure system property is not set for this test
        System.clearProperty("coherence.lambdas");
        }

    // ----- Constructor -----------------------------------------------------

    public DynamicLambdasOperationalOverrideTests()
        {
        super("tangosol-coherence-override-dynamic-lambdas-config.xml", false);
        }

    @Test
    public void testLambdas()
        {
        try (SystemPropertyResource r = new SystemPropertyResource("coherence.override", f_sOverrideFile))
            {
            assertThat(Lambdas.isStaticLambdas(), is(f_fIsStatic));
            }
        }
    }
