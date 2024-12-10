/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ArrayList;


/**
* This abstract class contains helper functions for
* manipulating collections and enumerations.
*
* @author Pat McNerthney
* @version 1.00, 04/25/00
*
* @deprecated As of Coherence 12.1.2
*/
@Deprecated
@SuppressWarnings("deprecation")
public abstract class CollectionHelper
    {
    /**
    * Convert a collection from one set of objects
    * to a new set of objects.
    *
    * @param colOriginal the original collection of objects
    * @param converter an object which will perform the conversion
    *                  of elements of the collection
    *
    * @return the converted collection
    */
    static public Collection convert(Collection colOriginal, Converter converter)
        {
        if (colOriginal == null)
            {
            return null;
            }

        ArrayList list = new ArrayList(colOriginal.size());
        for (Iterator iter = colOriginal.iterator(); iter.hasNext(); )
            {
            Object obj = iter.next();
            if (obj != null)
                {
                obj = converter.convert(obj);
                }
            list.add(obj);
            }
        return list;
        }

    /**
    * Convert an Iterator from one set of objects to a new set of objects.
    *
    * @param iter       the original Iterator of objects
    * @param converter  an object which will perform the conversion
    *
    * @return the converted Iterator
    */
    static public Iterator convert(Iterator iter, Converter converter)
        {
        if (iter == null)
            {
            return null;
            }

        ArrayList list = new ArrayList();
        while (iter.hasNext())
            {
            Object obj = iter.next();
            if (obj != null)
                {
                obj = converter.convert(obj);
                }
            list.add(obj);
            }
        return list.iterator();
        }

    /**
    * Convert an Enumeration from one set of objects to a new set of objects.
    *
    * @param enmr       the original Enumeration of objects
    * @param converter  an object which will perform the conversion
    *
    * @return the converted Enumeration
    */
    static public Enumeration convert(Enumeration enmr, Converter converter)
        {
        if (enmr == null)
            {
            return null;
            }

        ArrayList list = new ArrayList();
        while (enmr.hasMoreElements())
            {
            Object obj = enmr.nextElement();
            if (obj != null)
                {
                obj = converter.convert(obj);
                }
            list.add(obj);
            }
        return new SimpleEnumerator(list.toArray());
        }
    }
