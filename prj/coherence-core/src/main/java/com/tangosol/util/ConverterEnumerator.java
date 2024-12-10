/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Iterator;
import java.util.Enumeration;


/**
* Provide an implementation of an enumerator which converts each of the
* items which it enumerates.
*
* @author Cameron Purdy
* @version 1.00, 2002.02.07
*
* @deprecated As of Coherence 12.1.2, replaced by {@link ConverterCollections.ConverterEnumerator}
*/
@Deprecated
public class ConverterEnumerator
        extends ConverterCollections.ConverterEnumerator
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the Converter enumerator based on an Enumeration.
    *
    * @param enmr  java.util.Enumeration of objects to convert
    * @param conv  a Converter
    */
    public ConverterEnumerator(Enumeration enmr, Converter conv)
        {
        super(enmr, conv);
        }

    /**
    * Construct the Converter enumerator based on an Iterator.
    *
    * @param iter  java.util.Iterator of objects to convert
    * @param conv  a Converter
    */
    public ConverterEnumerator(Iterator iter, Converter conv)
        {
        super(iter, conv);
        }

    /**
    * Construct the Converter enumerator based on an array of objects.
    *
    * @param aoItem  array of objects to enumerate
    * @param conv    a Converter
    */
    public ConverterEnumerator(Object[] aoItem, Converter conv)
        {
        super(aoItem, conv);
        }
    }
