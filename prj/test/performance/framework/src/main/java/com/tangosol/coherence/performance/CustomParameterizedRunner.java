/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import org.junit.internal.runners.model.ReflectiveCallable;

import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;

import org.junit.runner.Runner;

import org.junit.runner.notification.RunNotifier;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.List;

/**
 * @author jk 2015.11.26
 */
public class CustomParameterizedRunner
        extends BlockJUnit4ClassRunnerWithParameters
    {
    public CustomParameterizedRunner(TestWithParameters test)
            throws InitializationError
        {
        super(test);
        }

    protected Statement classBlock(final RunNotifier notifier)
        {
        Object test;

        try
            {
            test = new ReflectiveCallable()
                {
                @Override
                protected Object runReflectiveCall()
                        throws Throwable
                    {
                    return createTest();
                    }
                }.run();
            }
        catch (Throwable e)
            {
            return new Fail(e);
            }

        Statement statement = super.classBlock(notifier);

        statement = withBeforeParameters(statement, test);
        statement = withAfterParameters(statement, test);

        return statement;
        }

    @Override
    public Object createTest()
            throws Exception
        {
        if (m_test == null)
            {
            m_test = super.createTest();
            }

        return m_test;
        }

    protected Statement withBeforeParameters(Statement statement, Object target)
        {
        List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(BeforeParmeterizedRun.class);

        return befores.isEmpty()
               ? statement
               : new RunBefores(statement, befores, target);
        }

    protected Statement withAfterParameters(Statement statement, Object target)
        {
        List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(AfterParmeterizedRun.class);

        return afters.isEmpty()
               ? statement
               : new RunAfters(statement, afters, target);
        }

    public static class Factory
            implements ParametersRunnerFactory
        {
        @Override
        public Runner createRunnerForTestWithParameters(TestWithParameters test)
                throws InitializationError
            {
            return new CustomParameterizedRunner(test);
            }
        }

    private Object m_test;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface BeforeParmeterizedRun
        {
        }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface AfterParmeterizedRun
        {
        }
    }
