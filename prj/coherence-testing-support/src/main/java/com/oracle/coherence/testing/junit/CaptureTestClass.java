/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.junit;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CaptureTestClass
        extends TestWatcher
    {
    @Override
    public Statement apply(Statement base, Description description)
        {
        m_clazz = description.getTestClass();
        return super.apply(base, description);
        }

    public Class<?> getgetTestClass()
        {
        return m_clazz;
        }

    private Class<?> m_clazz;
    }
