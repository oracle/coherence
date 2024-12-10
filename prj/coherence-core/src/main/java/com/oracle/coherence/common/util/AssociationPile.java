/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import com.oracle.coherence.common.base.Associated;

/**
 * The AssociationPile defines an abstract data structure holding elements in a
 * loosely ordered way with a queue-like contract that respects the possibility
 * that some elements can be {@link Associated associated} with one another.
 * Elements associated to the same {@link Associated#getAssociatedKey()
 * associated key} will maintain FIFO ordering, but may be re-ordered with
 * respect to elements with different associations.
 * <p>
 * Moreover, the AssociationPile assumes that {@link #poll polled} elements are
 * being processed in parallel on multiple threads and prevents polling of an
 * element with an association until any previously polled associated element
 * has been explicitly {@link #release released}.
 * <p>
 * <b>Note:</b> any element returned by the {@link #poll} methods must be passed
 * to a subsequent {@link #release} call.
 *
 * @param <T> the element type
 *
 * @author gg 2013.02.27
 */
public interface AssociationPile<T>
    {
    /**
     * Add the specified element to the pile.
     *
     * @param element  element to be added
     *
     * @return true if the element was accepted; false if it was rejected for
     *         any reason
     */
    public boolean add(T element);

    /**
     * Remove the first element without any association or with an uncontended
     * association from the front of this pile. An element is considered
     * uncontended if any of the following holds true:
     * <ul>
     *   <li>it has no association;
     *   <li>it has an association, but no other associated element has been
     *       polled;
     *   <li>it has an association, but all associated element that have been
     *       previously polled, have been released.
     * </ul>
     * If the pile is empty or all elements are contended, null is returned.
     * When the caller has finished processing the returned element, it must
     * release it by calling the {@link #release} method.
     * <p>
     * There is a special {@link #ASSOCIATION_ALL} object that is associated with
     * any non-null associations. In regard to this association, the previous
     * rules apply. As a result, the following holds true:
     * <ul>
     *   <li>an element associated with {@link #ASSOCIATION_ALL} is considered
     *       contended if any elements with a non-null association have been
     *       previously polled, but have not yet been released;
     *   <li>any elements that have any association which were added to the pile
     *       after an element associated with with {@link #ASSOCIATION_ALL}
     *       are considered to be contended and will remain contended until that
     *       element is released.
     * </ul>
     *
     * @return the first uncontended element in the front of this pile or null
     *         otherwise
     */
    public T poll();

    /**
     * Release a previously polled element. This will allow an associated
     * element (if it exists) in this pile to be {@link #poll polled}.
     *
     * @param element  an element to release
     */
    public void release(T element);

    /**
     * Determine the number of elements in this pile.
     *
     * @return the number of elements in this pile
     */
    public int size();

    /**
     * Check whether or not there are any uncontended elements in this pile.
     * <p>
     * <b>Note:</b> implementations are permitted to return "true" if the pile
     * contains only contended elements; however, implementations must not
     * return "false" if the pile contains one or more uncontended elements.
     *
     * @return true iff there are potentially uncontended elements
     */
    public boolean isAvailable();

    /**
     * A special association value that is associated with all elements that
     * have any associations.
     */
    public final static Object ASSOCIATION_ALL = new Object();
    }
