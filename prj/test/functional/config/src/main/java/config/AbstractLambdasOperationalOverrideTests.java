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

public abstract class AbstractLambdasOperationalOverrideTests
    {
    // ----- Constructor -----------------------------------------------------

    public AbstractLambdasOperationalOverrideTests(String sOverrideFile, boolean fIsStatic)
        {
        f_sOverrideFile = sOverrideFile;
        f_fIsStatic     = fIsStatic;
        }

    @Test
    public void testLambdas()
        {
        try (SystemPropertyResource r = new SystemPropertyResource("coherence.override", f_sOverrideFile))
            {
            assertThat(Lambdas.isStaticLambdas(), is(f_fIsStatic));
            }
        }

    // ----- constants -------------------------------------------------------

    final protected String  f_sOverrideFile;

    final protected boolean f_fIsStatic;

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
