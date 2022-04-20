/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;

import java.io.IOException;


/**
* Utilities for dealing with POF data classes.
*
* @author as  2009.01.31
*/
public abstract class PofDataUtils
        extends Base
    {
    /**
    * Creates a SimplePofContext with necessary test type registrations.
    *
    * @return a configured test POF context
    */
    public static PofContext getPofContext()
        {
        return getPofContext(false);
        }

    /**
    * Creates a POF context with necessary test type registrations.
    *
    * @param fRefEnabled  flag to indicate if object identity/reference is enabled
    *
    * @return a configured test POF context
    */
    public static PofContext getPofContext(boolean fRefEnabled)
        {
        if (fRefEnabled)
            {
            return new ConfigurablePofContext("data/pof/test-pof-config.xml");
            }

        SimplePofContext simpleCtx = new SimplePofContext();
        simpleCtx.registerUserType(1, Address.class, new PortableObjectSerializer(1));
        simpleCtx.registerUserType(2, PortablePerson.class, new PortableObjectSerializer(2));
        simpleCtx.registerUserType(3, TestValue.class, new PortableObjectSerializer(3));
        simpleCtx.registerUserType(4, ObjectWithAllTypes.class, new PortableObjectSerializer(4));
        return simpleCtx;
        }

    /**
    * Serializes object using POF.
    *
    * @param o      object to serialize
    * @param nMode  decoration mode
    *
    * @return POF-encoded serialized binary value
    *
    * @throws java.io.IOException if an error occurs writing to POF stream
    */
    public static Binary serialize(Object o, int nMode)
            throws IOException
        {
        return serialize(o, nMode, false);
        }

    /**
    * Serializes object using POF.
    *
    * @param o            object to serialize
    * @param nMode        decoration mode
    * @param fRefEnabled  a flag to indicate if POF references are enabled
    *
    * @return POF-encoded serialized binary value
    *
    * @throws IOException if an error occurs writing to POF stream
    */
    public static Binary serialize(Object o, int nMode, boolean fRefEnabled)
            throws IOException
        {
        WriteBuffer buf = new BinaryWriteBuffer(1000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        if (nMode != MODE_PLAIN)
            {
            out.writeByte(21); // FMT_EXT
            }

        PofContext ctx = getPofContext(fRefEnabled);
        ctx.serialize(out, o);

        Binary result = buf.toBinary();
        if (nMode == MODE_FMT_IDO)
            {
            result = ExternalizableHelper.decorateBinary(result, 5).toBinary();
            }
        else if (nMode == MODE_FMT_DECO)
            {
            result = ExternalizableHelper.decorate(result, 1, new Binary("decoration".getBytes()));
            }

        return result;
        }

    /**
    * Deserializes object using POF.
    *
    * @param bin  POF-encoded binary value to deserialize
    *
    * @return deserialized object instance
    *
    * @throws IOException if an error occurs reading from POF stream
    */
    public static Object deserialize(Binary bin)
            throws IOException
        {
        return deserialize(bin, false);
        }

    /**
    * Deserializes object using POF.
    *
    * @param bin          POF-encoded binary value to deserialize
    * @param fRefEnabled  a flag to indicate if POF references are enabled
    *
    * @return deserialized object instance
    *
    * @throws IOException if an error occurs reading from POF stream
    */
    public static Object deserialize(Binary bin, boolean fRefEnabled)
            throws IOException
        {
        if (bin.byteAt(0) == 2)  // MODE_PLAIN
            {
            return getPofContext(fRefEnabled).deserialize(bin.getBufferInput());
            }
        else
            {
            return ExternalizableHelper.fromBinary(bin, getPofContext(fRefEnabled));
            }
        }


    // ----- constants ------------------------------------------------------

    public static final int MODE_PLAIN = 0;
    public static final int MODE_FMT_EXT = 1;
    public static final int MODE_FMT_IDO = 2;
    public static final int MODE_FMT_DECO = 3;
    }
