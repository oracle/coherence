/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
* Provides the means to create ObjectInput and ObjectOutput streams based on
* primitive data streams in a layered, pluggable fashion.
*
* @author cp  2002.08.19
*/
public interface ObjectStreamFactory
    {
    /**
    * Obtain an ObjectInput based on the passed DataInput.
    *
    * @param  in         the DataInput to be wrapped
    * @param  loader     the ClassLoader to be used
    * @param  fForceNew  if true, a new ObjectInput must be returned; otherwise,
    *                    if the passed stream is already an ObjectInput, it's
    *                    allowed to be returned instead
    *
    * @return an ObjectInput that delegates to ("wraps") the passed DataInput
    *
    * @throws IOException  if an I/O exception occurs
    */
    public ObjectInput getObjectInput(DataInput in, ClassLoader loader, boolean fForceNew)
        throws IOException;

    /**
    * Obtain an ObjectOutput based on the passed DataOutput.
    *
    * @param  out  the DataOutput to be wrapped
    *
    * @return an ObjectOutput that delegates to ("wraps") the passed DataOutput
    *
    * @throws IOException  if an I/O exception occurs
    */
    public ObjectOutput getObjectOutput(DataOutput out)
        throws IOException;
    }