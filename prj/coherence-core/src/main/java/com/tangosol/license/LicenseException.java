/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.license;


import com.tangosol.io.pof.PortableException;


/**
* Signals that an operation has failed due to a licensing error, such as a
* missing license or license limit being exceeded.
*
* @author jh  2006.09.27
*/
public class LicenseException
        extends PortableException
    {
    /**
    * Construct a new LicenseException.
    */
    public LicenseException()
        {
        super();
        }

    /**
    * Construct a LicenseException with the given detail message.
    *
    * @param message  a detail message
    */
    public LicenseException(String message)
        {
        super(message);
        }
    }