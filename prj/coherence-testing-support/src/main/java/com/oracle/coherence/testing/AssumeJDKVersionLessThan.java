/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;


/**
 * A Utilty to skip entire Junit test for current JDK jvm version equal to or greater than specified version.
 */
public class AssumeJDKVersionLessThan
        implements TestRule
    {
    // ----- constructors ------------------------------------------------------

    /**
     *
     * @param nVersion provide version 5, 7, 8, 9, 11, 15, 16, 17, ...
     */
    public AssumeJDKVersionLessThan(int nVersion)
     {
        f_nVersion = nVersion;
     }

    // ----- TestRule methods --------------------------------------------------

    @Override
    public Statement apply(Statement base, Description description)
        {
        return new Statement()
            {
            @Override
            public void evaluate() throws Throwable
                {
                // look up current JVM version
                int nCurrentVersion = CheckJDK.computeVersion(System.getProperty("java.version"));

                if (nCurrentVersion >= f_nVersion)
                    {
                    throw new AssumptionViolatedException("Skipping test since detected JDK version "
                                                          + nCurrentVersion + ", only run test with JDK version less than " + f_nVersion);
                    }
                else
                    {
                    base.evaluate();
                    }
                }
            };
        }

    // ----- data members ------------------------------------------------------

    /**
     * Assume support for JDK versions less than this version
     */
    private final int f_nVersion;
    }
