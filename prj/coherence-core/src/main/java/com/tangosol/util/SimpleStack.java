/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.ArrayList;


/**
* A LIFO (last in, first out) unbounded stack of objects. A SimpleStack supports
* the following operations:
* <ul>
*   <li>{@link #push add} a new element to the top of the stack</li>
*   <li>{@link #pop remove} and return the element on the top of the stack</li>
*   <li>{@link #peek return} the element on the top of the stack</li>
* </ul>
* This class is similar to {@link java.util.Stack} except that it extends
* {@link java.util.ArrayList} rather than {@link java.util.Vector} and is
* therefore not thread safe. The top of the stack corresponds to the last
* element in the underlying list, whereas the bottom corresponds to the first.
* <p>
* The SimpleStack implementation supports null elements; however, if null
* elements are added, care must be taken to distinguish a null element returned
* from {@link #pop()} or {@link #peek()} from a null value that indicates the
* stack is empty.
*
* @author jh  2006.05.04
*
* @since Coherence 3.2 
*/
public class SimpleStack
        extends ArrayList
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new empty SimpleStack.
    */
    public SimpleStack()
        {
        super();
        }

    /**
    * Create a new empty SimpleStack with the specified initial capacity.
    *
    * @param nCapacity  the initial capacity of the underlying list used to
    *                   store elements
    */
    public SimpleStack(int nCapacity)
        {
        super(nCapacity);
        }


    // ----- stack operations -----------------------------------------------

    /**
    * Add the given object to the top of the stack.
    *
    * @param oElement  the object to place on top of the stack
    */
    public void push(Object oElement)
        {
        add(oElement);
        }

    /**
    * Remove and return the object that is currently on top of the stack.
    *
    * @return the object removed from the top of the stack or null if the stack
    *         is empty
    */
    public Object pop()
        {
        int cElements = size();
        return cElements == 0 ? null : remove(cElements - 1);
        }

    /**
    * Return the object that is currently on top of the stack.
    *
    * @return the object on top of the stack or null if the stack is empty
    */
    public Object peek()
        {
        int cElements = size();
        return cElements == 0 ? null : get(cElements - 1);
        }
    }
