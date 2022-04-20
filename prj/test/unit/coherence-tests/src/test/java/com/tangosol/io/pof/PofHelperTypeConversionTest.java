/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
* Unit tests of various PofHelper POF type conversion methods.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class PofHelperTypeConversionTest
    {
    @Test
    public void testJavaTypeID()
        {
        SimplePofContext ctx = new SimplePofContext();

        Object o = new byte[0];
        assertEquals(PofConstants.J_BYTE_ARRAY, PofHelper.getJavaTypeId(o, ctx));

        o = new Object[0];
        assertEquals(PofConstants.J_OBJECT_ARRAY, PofHelper.getJavaTypeId(o, ctx));

        o = new String[0];
        assertEquals(PofConstants.J_OBJECT_ARRAY, PofHelper.getJavaTypeId(o, ctx));

        ctx.registerUserType(1000, o.getClass(), new PortableObjectSerializer(1000));
        o = new Object[0];
        assertEquals(PofConstants.J_OBJECT_ARRAY, PofHelper.getJavaTypeId(o, ctx));

        o = new String[0];
        assertEquals(PofConstants.J_USER_TYPE, PofHelper.getJavaTypeId(o, ctx));
        }

    @Test
    public void testPofTypeID()
        {
        assertEquals(PofConstants.T_BOOLEAN, PofHelper.getPofTypeId(
                Boolean.class, new SimplePofContext()));
        assertEquals(PofConstants.T_CHAR, PofHelper.getPofTypeId(
                Character.class, new SimplePofContext()));
        assertEquals(PofConstants.T_INT16,
                PofHelper.getPofTypeId(Short.class, new SimplePofContext()));
        assertEquals(PofConstants.T_INT32, PofHelper.getPofTypeId(
                Integer.valueOf(-1).getClass(), new SimplePofContext()));
        assertEquals(PofConstants.T_INT64,
                PofHelper.getPofTypeId(
                        Long.valueOf(Integer.MAX_VALUE).getClass(),
                        new SimplePofContext()));
        assertEquals(PofConstants.T_DATETIME,
                PofHelper.getPofTypeId(
                        new Date(11, 11, 11, 11, 11, 11).getClass(),
                        new SimplePofContext()));
        assertEquals(PofConstants.T_CHAR_STRING,
                PofHelper.getPofTypeId("test".getClass(),
                        new SimplePofContext()));

        double[] ad = new double[]{Double.MAX_VALUE, 0, -1, Double.NEGATIVE_INFINITY};
        assertEquals(PofConstants.T_UNIFORM_ARRAY,
                PofHelper.getPofTypeId(ad.getClass(),
                        new SimplePofContext()));

        Object[] objArray = new Object[]{new Date(11, 11, 11), 13, Double.NaN};
        assertEquals(PofConstants.T_ARRAY, PofHelper.getPofTypeId(
                objArray.getClass(), new SimplePofContext()));

        List list = new ArrayList();
        list.add(new Date(11, 11, 11));
        list.add((float) 5.55);
        assertEquals(PofConstants.T_COLLECTION,
                PofHelper.getPofTypeId(list.getClass(),
                        new SimplePofContext()));

        Map map = new HashMap();
        map.put("now", new Date(2006, 8, 11, 12, 49, 0));
        assertEquals(PofConstants.T_MAP, PofHelper.getPofTypeId(
                map.getClass(), new SimplePofContext()));

        SimplePofContext ctx = new SimplePofContext();

        assertEquals(PofConstants.T_UNIFORM_ARRAY, PofHelper.getPofTypeId(byte[].class, ctx));
        assertEquals(PofConstants.T_ARRAY, PofHelper.getPofTypeId(Object[].class, ctx));
        assertEquals(PofConstants.T_ARRAY, PofHelper.getPofTypeId(String[].class, ctx));

        ctx.registerUserType(1000, String[].class, new PortableObjectSerializer(1000));

        assertEquals(PofConstants.T_UNIFORM_ARRAY, PofHelper.getPofTypeId(byte[].class, ctx));
        assertEquals(PofConstants.T_ARRAY, PofHelper.getPofTypeId(Object[].class, ctx));
        assertEquals(1000, PofHelper.getPofTypeId(String[].class, ctx));
        }

    @Test
    public void testConvertNumber()
        {
        assertEquals(null, PofHelper.convertNumber(null, PofConstants.J_BYTE));
        assertEquals((byte) 111, PofHelper.convertNumber(111, PofConstants.J_BYTE));
        assertEquals((short) 6, PofHelper.convertNumber((float) 6.0001, PofConstants.J_SHORT));
        assertEquals(11, PofHelper.convertNumber(11, PofConstants.J_INTEGER));
        assertEquals(Long.MAX_VALUE, PofHelper.convertNumber(Long.MAX_VALUE, PofConstants.J_LONG));
        assertEquals((double) Long.MAX_VALUE, PofHelper.convertNumber(Long.MAX_VALUE, PofConstants.J_DOUBLE));
        assertEquals((float) -0.1, PofHelper.convertNumber(-0.1, PofConstants.J_FLOAT));
        }

    @Test
    public void testConvertNumberWithException()
        {
        try
            {
            PofHelper.convertNumber(1, 100);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testResizeArray()
        {
        Object[] ao = new Object[]{new Date(1999, 1, 1, 12, 0, 0), Long.MAX_VALUE, (float) -0.1};
        assertEquals(ao.length + 10, PofHelper
                .resizeArray(ao, ao.length + 10).length);
        assertEquals(ao.length,
                PofHelper.resizeArray(ao, ao.length).length);
        //TODO: if the second argument in  resizeArray is number smaller then passed arrays length
        // it justs writes null at the first index bigger then new size!
        //   assertEquals(objArray.length - 2, PofHelper.resizeArray(objArray, objArray.length - 2).length);
        assertEquals(5, PofHelper.resizeArray(null, 5).length);
        }
    }
