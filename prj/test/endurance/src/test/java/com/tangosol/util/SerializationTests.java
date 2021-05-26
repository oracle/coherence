/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;

import java.util.Arrays;

import static org.junit.Assert.*;


/**
 * ExternalizableHelper tests. Due to the nature of the tests, some of them
 * use "white-box" technique and assume the knowledge of the underlying
 * implementation.
 *
 * @author gg 2015.05.18
 */
public class SerializationTests
        extends Base
    {
    @Ignore("COH-23556")
    @Test
    public void testLargeObject()
        {
        Object oTest;
        Binary binTest;

        // no stats; ExternalizableHelper will use BinaryWriteBuffer and fail
        try
            {
            oTest = new ByteArrayHolder(Integer.MAX_VALUE - 10);
            ExternalizableHelper.toBinary(oTest);

            fail("Unexpected success of the large object serialization");
            }
        catch (UnsupportedOperationException e)
            {
            // expected from ByteArrayWriteBuffer#checkBounds
            }

        // still no stats; ExternalizableHelper will use BinaryWriteBuffer
        // and update the stats so max == avg
        try
            {
            oTest = binTest = null; // release
            oTest = new ByteArrayHolder(Integer.MAX_VALUE/16*15);
            binTest = ExternalizableHelper.toBinary(oTest);

            assertEquals(oTest, ExternalizableHelper.fromBinary(binTest));
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            fail("Failed to serialize a large object");
            }

        // stats: avg == max; ExternalizableHelper will use BinaryWriteBuffer
        // and update the stats so max > 1.25*avg
        try
            {
            oTest = binTest = null; // release
            oTest = new ByteArrayHolder(Integer.MAX_VALUE/4*3);
            binTest = ExternalizableHelper.toBinary(oTest);

            assertEquals(oTest, ExternalizableHelper.fromBinary(binTest));
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            fail("Failed to serialize a large object");
            }

        // stats: max > 1.25*avg; ExternalizableHelper will use MultiBufferWriteBuffer
        try
            {
            oTest = binTest = null; // release
            oTest = new ByteArrayHolder(Integer.MAX_VALUE - 10);
            ExternalizableHelper.toBinary(oTest);

            fail("Unexpected success of the large object serialization");
            }
        catch (UnsupportedOperationException e)
            {
            // expected from ByteArrayWriteBuffer#checkBounds
            }

        // stats: max > 1.25*avg; ExternalizableHelper will use MultiBufferWriteBuffer
        try
            {
            oTest = binTest = null; // release
            oTest = new ByteArrayHolder(Integer.MAX_VALUE/16*15);
            ExternalizableHelper.toBinary(oTest);
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            fail("Failed to serialize a large object");
            }
        }

    /**
     * Serializable byte array holder.
     */
    public static class ByteArrayHolder
            implements Serializable
        {
        public ByteArrayHolder(int cb)
            {
            f_ab = new byte[cb];
            getRandom().nextBytes(f_ab);
            }

        public int hashCode()
            {
            return f_ab.length;
            }

        public boolean equals(Object obj)
            {
            return Arrays.equals(f_ab, ((ByteArrayHolder) obj).f_ab);
            }

        private final byte[] f_ab;
        }

    }
