/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.io.IOException;

/**
* The PofSerializer interface provides the capability of reading and writing a
* Java object from and to a POF stream.
* <p>
* In order to support hot-deploying containers, it is important in the case
* of a PofSerializer implementation that requires ClassLoader context that it
* <b>not</b> hold any strong references to that ClassLoader, or to any Class
* objects obtained from that ClassLoader.
* <p>
* <b>Note:</b> it is extremely important that objects that are equivalent
* according to their "equals()" implementation produce equivalent serialized
* streams. Violating this relationship will result in non-deterministic behavior
* for many Coherence services.
*
* @author cp/jh  2007.07.14
*
* @see PofReader
* @see PofWriter
*
* @since Coherence 3.2
*/
public interface PofSerializer<T>
    {
    /**
    * Serialize a user type instance to a POF stream by writing its state using
    * the specified PofWriter object.
    * <p>
    * An implementation of PofSerializer is required to follow the following
    * steps in sequence for writing out an object of a user type:
    * <ol>
    * <li>If the object is evolvable, the implementation must set the version
    *     by calling {@link PofWriter#setVersionId}.</li>
    * <li>The implementation may write any combination of the properties of
    *     the user type by using the "write" methods of the PofWriter, but it
    *     must do so in the order of the property indexes.</li>
    * <li>After all desired properties of the user type have been written,
    *     the implementation must terminate the writing of the user type by
    *     calling {@link PofWriter#writeRemainder}.</li>
    * </ol>
    *
    * @param out    the PofWriter with which to write the object's state
    * @param value  the object to serialize
    *
    * @exception IOException  if an I/O error occurs
    */
    public void serialize(PofWriter out, T value)
            throws IOException;

    /**
    * Deserialize a user type instance from a POF stream by reading its state
    * using the specified PofReader object.
    * <p>
    * An implementation of PofSerializer is required to follow the following
    * steps in sequence for reading in an object of a user type:
    * <ol>
    * <li>If the object is evolvable, the implementation must get the version
    *     by calling {@link PofReader#getVersionId}.</li>
    * <li>The implementation may read any combination of the properties of
    *     the user type by using "read" methods of the PofReader, but it must
    *     do so in the order of the property indexes. Additionally, the
    *     implementation must call {@link PofReader#registerIdentity}
    *     with the new instance prior to reading any properties which are
    *     user type instances themselves.</li>
    * <li>After all desired properties of the user type have been read, the
    *     implementation must terminate the reading of the user type by
    *     calling {@link PofReader#readRemainder}.</li>
    * </ol>
    *
    * @param in  the PofReader with which to read the object's state
    *
    * @return the deserialized user type instance
    *
    * @exception IOException  if an I/O error occurs
    */
    public T deserialize(PofReader in)
            throws IOException;
    }
