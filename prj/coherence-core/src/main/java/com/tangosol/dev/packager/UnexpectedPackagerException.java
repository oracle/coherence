/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.util.WrapperException;


/**
* UnexpectedPackagerException is a kind of WrapperException
* that is used to propagate exceptions from implementation classes
* in the packager to the outside, in cases where the packager API
* would not indicate that such an exception is expected to occur.
*/
public class UnexpectedPackagerException
        extends WrapperException
    {
    /**
    *  Construct an UnexpectedPackagerException from the specified original
    *  exception.
    */
    public UnexpectedPackagerException(Throwable originalException)
        {
        super(originalException);
        }
    }
