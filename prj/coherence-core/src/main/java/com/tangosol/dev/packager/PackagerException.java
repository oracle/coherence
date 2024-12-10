/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.util.CheckedWrapperException;


/**
* PackagerException is a CheckedWrapperException that is used when an Exception
* is handled by an implementation class in the packager that is not
* appropriate to be thrown by the packager.
*/
public class PackagerException
        extends CheckedWrapperException
    {
    /**
    * Construct a PackagerException from the specified original exception.
    */
    public PackagerException(Throwable originalException)
        {
        super(originalException);
        }
    }
