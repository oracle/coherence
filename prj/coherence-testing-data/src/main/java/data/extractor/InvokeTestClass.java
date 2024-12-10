/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.extractor;


import java.io.Serializable;


/**
* This class is used in the Reflection extractor testing program for unit
* testing different method invocation.
*
* @author ewilliams Jan 30, 2007
*/
public class InvokeTestClass implements Serializable
    {
    public int sumIntTest(int i1, int i2)
        {
        return i1 + i2;
        }

    public int sumIntTest(Integer i1, Integer i2, Integer i3)
        {
        return (i1 == null ? 0 : i1.intValue())
             + (i2 == null ? 0 : i2.intValue())
             + (i3 == null ? 0 : i3.intValue());
        }

    public String retVal()
        {
        return "Return Value";
        }

    public int retVal(int i)
        {
        return i;
        }

    public boolean retVal(boolean b)
        {
        return b;
        }


    public int sumIntTest(Object o, Integer i2, Integer i3)
        {
        Integer i1 = (Integer) o;
        return i1.intValue() * i2.intValue() * i3.intValue();
        }

    public int sumIntTest(Object ao[])
        {
        int iSum = 0;
        for (int j = 0, c = ao == null ? 0 : ao.length; j < c; j++)
            {
            iSum += ((Integer)ao[j]).intValue();
            }
        return iSum;
        }
    }
