/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.util.EventObject;


/**
* An AnyEvent is an event used when no specific event implementation fits
* and it is not worth making one.
*
* @author cp  1999.08.24
*/
public class AnyEvent
        extends EventObject
        implements Serializable
    {
    /**
    * Construct a new <code>AnyEvent</code>.
    *
    * @param source  the event source
    * @param value   some value to provide to event listeners
    */
    public AnyEvent(Object source, Object value)
        {
        super(source);
        this.value = value;
        }

    /**
    * Gets the associated value.
    *
    * @return  the value associated with the event
    */
    public Object getValue()
        {
        return value;
        }

    /**
    * Gets the associated value as a java.lang.String.
    *
    * @return  the string value associated with the event
    *
    * @throws ClassCastException  if the event's value is not a String
    */
    public String getString()
        {
        return (String) value;
        }

    /**
    * Gets the associated value as a java int.
    *
    * @return  the int value associated with the event
    *
    * @throws ClassCastException  if the event's value is not a Number
    */
    public int getInt()
        {
        return ((Number) value).intValue();
        }

    /**
    * The value associated with this event.
    */
    protected Object value;
    }
