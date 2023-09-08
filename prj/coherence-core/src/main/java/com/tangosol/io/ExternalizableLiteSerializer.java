/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The ExternalizableLiteSerializer interface provides the capability of reading and writing a
 * Java object from and to a DataInput/DataOutput stream.
 * <p>
 * <b>Note:</b> it is extremely important that objects that are equivalent
 * according to their "equals()" implementation produce equivalent serialized
 * streams. Violating this relationship will result in non-deterministic behavior
 * for many Coherence services.
 *
 * @param <T> the type of the object used by this serializer
 *
 * @see com.tangosol.util.ExternalizableHelper#readExternalizableLite(DataInput,ClassLoader)
 * @see com.tangosol.util.ExternalizableHelper#writeExternalizableLite(DataOutput,ExternalizableLite)
 * @see ExternalizableLite
 * @see ExternalizableType
 *
 * @author jf  2023.06.05
 * @since 23.09
 */

public interface ExternalizableLiteSerializer<T>
    {
    /**
     * Deserialize the contents of <code>T</code> instance by loading the object's state from
     * the passed DataInput object. The instance's properties must be read in the same order as written
     * by {@link #serialize(DataOutput, T)}. The properties are read using {@link DataInput} and
     * {@link com.tangosol.util.ExternalizableHelper} read methods.
     *
     * @param in  the DataInput stream to read data from in order to restore
     *            the state of this object
     *
     * @return the deserialized user type instance
     *
     * @exception IOException  if an I/O error occurs
     */
    public T deserialize(DataInput in)
            throws IOException;

    /**
     * Serialize a user type instance by storing its properties into
     * the passed DataOutput object. The instance's properties are written using {@link DataOutput} and
     * {@link com.tangosol.util.ExternalizableHelper} write methods.
     *
     * @param out    the DataOutput stream to write the state of this object to
     * @param value  the object to serialize
     *
     * @exception IOException if an I/O exception occurs
     */
    public void serialize(DataOutput out, T value)
            throws IOException;
    }
