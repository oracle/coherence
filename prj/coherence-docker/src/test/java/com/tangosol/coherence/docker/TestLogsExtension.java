/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.docker;

import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;

public class TestLogsExtension
        extends AbstractTestLogs
        implements BeforeAllCallback, BeforeEachCallback {

    /**
     * Create a {@link com.oracle.bedrock.testsupport.junit.TestLogsExtension}.
     */
    public TestLogsExtension() {
    }

    /**
     * Create a {@link com.oracle.bedrock.testsupport.junit.TestLogsExtension}.
     *
     * @param testClass the test class
     */
    public TestLogsExtension(Class<?> testClass) {
        this.testClass = testClass;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> cls = context.getTestClass().orElse(testClass);
        init(cls, null);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String methodName = context.getTestMethod().map(Method::getName).orElse("unknown");
        Class<?> cls = context.getTestClass().orElse(testClass);
        init(cls, methodName);
    }
}
