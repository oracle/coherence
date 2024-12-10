/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Binary;


/**
* The Evolvable interface is implemented by classes that require forwards-
* and backwards-compatibility of their serialized form. An Evolvable class
* has an integer version identifier <tt>n</tt>, where <tt>n &gt;= 0</tt>. When
* the contents and/or semantics of the serialized form of the Evolvable class
* changes, the version identifier is increased. Two versions identifiers,
* <tt>n1</tt> and <tt>n2</tt>, indicate the same version iff
* <tt>n1 == n2</tt>; the version indicated by <tt>n2</tt> is newer than the
* version indicated by  <tt>n1</tt> iff <tt>n2 &gt; n1</tt>.
* <p>
* The Evolvable interface is designed to support the evolution of classes by
* the addition of data. Removal of data cannot be safely accomplished as long
* as a previous version of the class exists that relies on that data.
* Modifications to the structure or semantics of data from previous versions
* likewise cannot be safely accomplished as long as a previous version of the
* class exists that relies on the previous structure or semantics of the
* data.
* <p>
* When an Evolvable object is deserialized, it retains any unknown data that
* has been added to newer versions of the class, and the version identifier
* for that data format. When the Evolvable object is subsequently serialized,
* it includes both that version identifier and the unknown future data.
* <p>
* When an Evolvable object is deserialized from a data stream whose version
* identifier indicates an older version, it must default and/or calculate the
* values for any data fields and properties that have been added since that
* older version. When the Evolvable object is subsequently serialized, it
* includes its own version identifier and all of its data. Note that there
* will be no unknown future data in this case; future data can only exist
* when the version of the data stream is newer than the version of the
* Evolvable class.
*
* @author cp/jh  2006.07.14
*
* @since Coherence 3.2
*/
public interface Evolvable
    {
    /**
    * Determine the serialization version supported by the implementing
    * class.
    *
    * @return the serialization version supported by this object
    */
    public int getImplVersion();

    /**
    * Obtain the version associated with the data stream from which this
    * object was deserialized. If the object was constructed (not
    * deserialized), the data version is the same as the implementation
    * version.
    *
    * @return the version of the data used to initialize this object, greater
    *         than or equal to zero
    */
    public int getDataVersion();

    /**
    * Set the version associated with the data stream with which this object
    * is being deserialized.
    *
    * @param nVersion  the version of the data in the data stream that will
    *                  be used to deserialize this object; greater than or
    *                  equal to zero
    *
    * @throws IllegalArgumentException  if the specified version is negative
    * @throws IllegalStateException  if the object is not in a state in which
    *         the version can be set, for example outside of deserialization
    */
    public void setDataVersion(int nVersion);

    /**
    * Return all the unknown remainder of the data stream from which this
    * object was deserialized. The remainder is unknown because it is data
    * that was originally written by a future version of this object's class.
    *
    * @return future data in binary form
    */
    public Binary getFutureData();

    /**
    * Store the unknown remainder of the data stream from which this object
    * is being deserialized. The remainder is unknown because it is data that
    * was originally written by a future version of this object's class.
    *
    * @param binFuture  future data in binary form
    *
    * @throws IllegalStateException  if the object is not in a state in which
    *         the data can be set, for example outside of deserialization
    */
    public void setFutureData(Binary binFuture);
    }
