/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor.common;

import java.util.Date;

import org.junit.AssumptionViolatedException;

import org.junit.rules.TestWatcher;

import org.junit.runner.Description;

/**
 * Test Watcher.
 *
 * @author  rl 2022.4.1
 * @since 14.1.2.0
 */
public class Watcher
        extends TestWatcher
    {
    // ----- methods from TestWatcher ---------------------------------------

    @Override
    protected void starting(Description d)
        {
        m_sName = d.getMethodName();
        System.err.println(">>>>> Starting test: " + m_sName + " in class " + d.getTestClass() + ", now=" + new Date());
        System.err.flush();
        }

    @Override
    protected void succeeded(Description description)
        {
        System.err.println(">>>>> Test Passed: " + m_sName + ", now=" + new Date());
        System.err.flush();
        }

    @Override
    protected void failed(Throwable e, Description description)
        {
        System.err.println(">>>>> Test Failed: " + m_sName + ", now=" + new Date());
        e.printStackTrace();
        System.err.println("<<<<<");
        System.err.flush();
        }

    @Override
    protected void skipped(AssumptionViolatedException e, Description description)
        {
        String sMsg = ">>>>> Test Skipped: " + m_sName;
        System.err.println(sMsg);
        System.err.flush();
        }

    @Override
    protected void finished(Description description)
        {
        super.finished(description);
        }

    /**
     * @return the name of the currently-running test method
     */
    public String getMethodName() {
    return m_sName;
    }

    // ----- data members -----------------------------------------------

    private volatile String m_sName;
    }
