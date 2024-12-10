/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.io.Resolving;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


/**
* A marker implementation to mirror the ResolvingObjectInputStream
* implementation.
*
* @author cp  2006.08.02
*/
public class ResolvingObjectOutputStream
        extends ObjectOutputStream
        implements Resolving
    {
    public ResolvingObjectOutputStream(OutputStream out)
            throws IOException
        {
        super(out);
        }
    }
