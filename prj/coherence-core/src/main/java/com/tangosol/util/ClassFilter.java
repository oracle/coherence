/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* Filter which discards all objects that are not of a certain class.
*
* @author cp  1999.08.26
*/
public class ClassFilter
        extends Base
        implements Filter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Constructor.
    *
    * @param clz  the class
    */
    public ClassFilter(Class clz)
        {
        azzert(clz != null);
        m_clz = clz;
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * Filter interface:  evaluate().
    */
    public boolean evaluate(Object o)
        {
        return m_clz.isInstance(o);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ClassFilter with another object to determine equality.
    *
    * @return true iff this ClassFilter and the passed object are
    *         equivalent ClassFilters
    */
    public boolean equals(Object o)
        {
        if (o instanceof ClassFilter)
            {
            ClassFilter that = (ClassFilter) o;
            return this.m_clz == that.m_clz;
            }

        return false;
        }

    /**
    * Determine a hash value for the ClassFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ClassFilter object
    */
    public int hashCode()
        {
        return m_clz.hashCode();
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        String sClass = getClass().getName();

        return "ClassFilter: " + m_clz.getName();
        }


    // ----- data members ---------------------------------------------------

    /**
    * Class to include (to NOT filter out).
    */
    private Class m_clz;
    }
