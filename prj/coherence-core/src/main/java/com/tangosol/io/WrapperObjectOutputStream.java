/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.ObjectOutput;
import java.io.IOException;


/**
* This is an imitation ObjectOutputStream class that provides the
* ObjectOutput interface by delegating to an object that implements the
* ObjectOutput interface. Primarily, this is intended as a base class for
* building specific-purpose ObjectOutput wrappers.
*
* @author cp  2004.08.20
*/
public class WrapperObjectOutputStream
        extends WrapperDataOutputStream
        implements ObjectOutput
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperObjectOutputStream that will output to the specified
    * object implementing the ObjectOutput interface.
    *
    * @param out  an object implementing ObjectOutput to write to
    */
    public WrapperObjectOutputStream(ObjectOutput out)
        {
        super(out);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying object providing the ObjectOutput interface that
    * this object is delegating to.
    *
    * @return the underlying ObjectOutput
    */
    public ObjectOutput getObjectOutput()
        {
        return (ObjectOutput) getDataOutput();
        }

    /**
    * {@inheritDoc}
    * <p>
    * <b>This method is unsupported.</b>
    * @throws UnsupportedOperationException always
    */
    public long getBytesWritten()
        {
        throw new UnsupportedOperationException();
        }


    // ----- ObjectOutput methods -------------------------------------------

    /**
    * Writes the Object <code>o</code> so that the corresponding
    * {@link java.io.ObjectInput#readObject} method can reconstitute an Object from
    * the written data.
    *
    * @param o  the Object to write
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeObject(Object o)
            throws IOException
        {
        getObjectOutput().writeObject(o);
        }
    }
