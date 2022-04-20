/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import org.junit.rules.ExternalResource;

import org.junit.runner.Description;

import org.junit.runners.model.Statement;

import java.util.Properties;

/**
 * A JUnit {@link org.junit.Rule} that will ensure each test has the same set of properties
 * (and any changes during the test are removed after the test has completed).
 *
 * @author Brian Oliver
 */
public class SystemPropertyIsolation extends ExternalResource
{
    /**
     * The original system properties.
     */
    private Properties originalProperties;


    @Override
    public Statement apply(Statement   base,
                           Description description)
    {
        return super.apply(base, description);
    }


    @Override
    protected void before() throws Throwable
    {
        // make a copy of the system properties as they are now
        originalProperties = new Properties();

        // NOTE: we access/copy/modify individual properties one at a time here
        // instead of replacing the entire collection as we can't be guaranteed
        // some other part of the testing infrastructure/rules etc have replaced
        // the properties implementation
        for (String name : System.getProperties().stringPropertyNames())
        {
            String value = System.getProperty(name);

            originalProperties.setProperty(name, value);
        }

        System.out.println("SystemPropertyIsolation: Captured " + originalProperties.size()
                           + " Original System Properties");

        super.before();
    }


    @Override
    protected void after()
    {
        super.after();

        System.out.println("SystemPropertyIsolation: Restoring Original System Properties...");

        int nrReplaced = 0;
        int nrRemoved  = 0;

        // remove all of the system properties not in the original properties
        // (and update those that have changed)

        // NOTE: we access/copy/modify individual properties one at a time here
        // instead of replacing the entire collection as we can't be guaranteed
        // some other part of the testing infrastructure/rules etc have replaced
        // the properties implementation
        for (String name : System.getProperties().stringPropertyNames())
        {
            if (originalProperties.containsKey(name))
            {
                String value         = System.getProperty(name);
                String originalValue = originalProperties.getProperty(name);

                if (!originalValue.equals(value))
                {
                    System.out.println("SystemPropertyIsolation: Restoring Property: " + name + " = \"" + originalValue
                                       + "\"");
                    System.getProperties().setProperty(name, originalValue);

                    nrReplaced++;
                }
            }
            else
            {
                System.out.println("SystemPropertyIsolation: Removing Property: " + name + "");
                System.getProperties().remove(name);

                nrRemoved++;
            }
        }

        System.out.println("SystemPropertyIsolation: Restored Original System Properties (removed " + nrRemoved
                           + ", replaced " + nrReplaced + ")");
    }
}
