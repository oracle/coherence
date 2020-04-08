/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author jk 2016.03.10
 */
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(CustomParameterizedRunner.Factory.class)
public abstract class BasePerformanceTest
    {
    public BasePerformanceTest(PerformanceEnvironment<?> environment)
        {
        f_environment = environment;
        }

    protected final PerformanceEnvironment<?> f_environment;

    @Rule
    public TestName m_testName = new TestName();
    }
