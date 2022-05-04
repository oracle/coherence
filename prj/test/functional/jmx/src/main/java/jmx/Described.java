/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

/**
* A {@link Described} object is an implementation of the {@link DescribedMBean}
* interface used in testing the implementation of COH-4200.
*
* @author cf 2011-01-10
*/
public class Described implements DescribedMBean
    {
    /**
    * {@inheritDoc}
    */
    @Override
    public int getAttribute()
        {
        return 4217;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public void operation()
        {
        }
    }
