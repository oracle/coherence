/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package config;


import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class StaticLambdasOperationalOverrideTests
        extends AbstractLambdasOperationalOverrideTests
    {
    @BeforeClass
    public static void setup()
        {
        // ensure system property is not set for this test
        System.clearProperty("coherence.lambdas");
        }

    // ----- Constructor -----------------------------------------------------

    public StaticLambdasOperationalOverrideTests()
        {
        super("tangosol-coherence-override-static-lambdas-config.xml", true);
        }
    }
