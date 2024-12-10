/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.io.IOException;


/**
* The PortableObject interface is implemented by Java classes that can self-
* serialize and deserialize their state to and from a POF data stream.
* <p>
* The {@link #readExternal} and {@link #writeExternal} methods of the
* PortableObject interface are implemented by a class to give the class
* complete control its own POF serialization and deserialization.
*
* @author cp/jh  2006.07.13
*
* @see PofReader
* @see PofWriter
*
* @since Coherence 3.2
*/
public interface PortableObject
    {
    /**
    * Restore the contents of a user type instance by reading its state using
    * the specified PofReader object.
    *
    * @param in  the PofReader from which to read the object's state
    *
    * @exception IOException  if an I/O error occurs
    */
    public void readExternal(PofReader in)
            throws IOException;

    /**
    * Save the contents of a POF user type instance by writing its state using
    * the specified PofWriter object.
    *
    * @param out  the PofWriter to which to write the object's state
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeExternal(PofWriter out)
            throws IOException;
    }
