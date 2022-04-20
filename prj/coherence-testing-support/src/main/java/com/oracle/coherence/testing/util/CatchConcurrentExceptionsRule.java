/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;


import org.junit.rules.MethodRule;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;


/**
 * This rule catches exceptions on all threads and fails the test if such
 * exceptions are caught.
 *
 * This is based on a code sample authored by Johannes Schneider
 * (http://blog.cedarsoft.com/2011/12/junit-rule-fail-tests-on-exceptionsfailed-assertions-in-other-threads/)
 */
public class CatchConcurrentExceptionsRule
        implements MethodRule
    {
    // ----- MethodRule methods -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public Statement apply(final Statement statement, FrameworkMethod frameworkMethod, Object o)
        {
        return new Statement()
            {
            public void evaluate()
                    throws Throwable
                {
                before();
                try
                    {
                    statement.evaluate();
                    }
                catch (Throwable t)
                    {
                    afterFailing();
                    throw t;
                    }

                afterSuccess();
                }
            };
      }


    // ----- helpers ------------------------------------------------------

    /**
     * Called before the statement is evaluated.
     */
    private void before()
        {
        m_handlerOrig = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
            {
            public void uncaughtException(Thread t, Throwable e)
                {
                f_listExc.add(e);
                if (m_handlerOrig != null)
                    {
                    m_handlerOrig.uncaughtException( t, e );
                    }
                }
            });
        }

    /**
     * Called after successful completion.
     */
    private void afterSuccess()
        {
        Thread.setDefaultUncaughtExceptionHandler(m_handlerOrig);

        if (f_listExc.isEmpty())
            {
            return;
            }

        throw new AssertionError(buildMessage());
        }

    /**
     * Called on exception.
     */
    private void afterFailing()
        {
        Thread.setDefaultUncaughtExceptionHandler(m_handlerOrig);
        if (!f_listExc.isEmpty())
            {
            System.out.println(buildMessage());
            }
        }

    /**
     * Return the failure message.
     *
     * @return the failure message
     */
    private String buildMessage()
        {
        StringBuilder sb = new StringBuilder();
        sb.append(f_listExc.size()).append(" exceptions thrown but not caught in other threads:\n");

        for (Throwable throwable : f_listExc)
            {
            if (throwable != null)
                {
                sb.append("---------------------\n");

                StringWriter out = new StringWriter();
                throwable.printStackTrace(new PrintWriter(out));
                sb.append(out.toString());
                }
            }

        sb.append("---------------------\n");

        return sb.toString();
        }

    // ----- data members -------------------------------------------------

    /**
     * The cached exception handler.
     */
    private Thread.UncaughtExceptionHandler m_handlerOrig;

    /**
     * The list of exceptions that were caught.
     */
    private final List<Throwable> f_listExc = new ArrayList<Throwable>();
    }