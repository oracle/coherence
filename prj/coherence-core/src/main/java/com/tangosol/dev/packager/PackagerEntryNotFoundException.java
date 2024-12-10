/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


/**
* This kind of Exception is throw when a specified PackagerEntry
* cannot be found.
*/
public class PackagerEntryNotFoundException
        extends Exception
    {
    /**
    * Construct a PackagerEntryNotFoundException.
    */
    public PackagerEntryNotFoundException()
        {
        super();
        }

    /**
    * Construct a PackagerEntryNotFoundException with the specified message.
    */
    public PackagerEntryNotFoundException(String message)
        {
        super(message);
        }
    }
