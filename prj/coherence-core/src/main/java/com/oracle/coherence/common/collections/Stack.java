/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;

/**
 * Stack describes a classic LIFO collection.
 *
 * @param <V>  the type of the values that will be stored in the Stack
 *
 * @author mf 2012.03.22
 */
public interface Stack<V>
    {
    /**
     * Push an element onto the stack.
     *
     * @param element  the element
     */
    public void push(V element);

    /**
     * Pop an element off of the stack.
     *
     * @return the element, or null if empty
     */
    public V pop();

    /**
     * Return but don't remove the top element of the stack.
     *
     * @return the element, or null if empty
     */
    public V peek();

    /**
     * Return true iff stack is empty.
     *
     * @return true iff stack is empty
     */
    public boolean isEmpty();

    /**
     * Return the number of elements in the stack.
     *
     * @return the number of elements in the stack
     */
    public int size();
    }
