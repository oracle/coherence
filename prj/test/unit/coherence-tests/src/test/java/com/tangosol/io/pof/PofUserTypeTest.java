/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import com.tangosol.io.Serializer;

import com.tangosol.util.ExternalizableHelper;

import java.io.IOException;

import java.util.ArrayList;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for getting POF user types ids.
 * Issue reported in COH-9428
 *
 * @author par 2013.4.08
 */
public class PofUserTypeTest
     {
    /**
     * Test collection that causes an incorrect type id to be returned.
     */
    public static class MyCollection<T> extends ArrayList<T>
        {
        }

    /**
     * Test serializer used to register the test collection.
     */
    public static class MySerializer implements PofSerializer
        {
        /**
         * Deserialize, not actually called.
         *
         * @param reader pof reader
         *
         * @return  deserialized object
         */
        public Object deserialize(PofReader reader) throws IOException
            {
            throw new UnsupportedOperationException("Not implemented");
            }

        /**
         * serialize, not actually called.
         *
         * @param writer  pof writer
         * @param obj     object to be serialized
         */
        public void serialize(PofWriter writer, Object obj) throws IOException
            {
            throw new UnsupportedOperationException("Not implemented");
            }
        }

    //----- PofUserTypeTest methods --------------------------------------

    /**
     * Test type id returned from context.
     *
     */
    @Test
    public void testIdFromContext()
        {
        assertEquals(9999, CONTEXT.getUserTypeIdentifier(MyCollection.class));
        }

    /**
     * Test type id returned from PofHelper.
     *
     */
    @Test
    public void testIdFromPofHelper()
        {
        assertEquals(9999, PofHelper.getPofTypeId(MyCollection.class, CONTEXT));
        }

    /**
     * Test enum serializer with static constant enum.
     *
     * COH-9833
     */
    @Test
    public void testEnumSerialization ()
            throws IOException
        {
        String     sPath      = "com/tangosol/io/pof/enum-pof-config.xml";
        Serializer serializer = new ConfigurablePofContext(sPath);
        TimeUnit   nData      = TimeUnit.SECONDS;

        assertEquals(nData, ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(nData, serializer), serializer));
        }


    //----- constants ----------------------------------------------------

    /**
     * context used to register the user type
     */
    public static final SimplePofContext CONTEXT = new SimplePofContext();

    /**
     * Register user type for this test.
     */
    static
        {
        CONTEXT.registerUserType(9999, MyCollection.class, new MySerializer());
        }
}