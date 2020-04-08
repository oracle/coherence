/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.security.Principal;


/**
* Generic Principal implementation that can be used to represent the identity
* of any remote entity.
*
* @author jh  2008.08.11
*
* @see PrincipalPofSerializer
*/
public class PofPrincipal
        extends PofHelper
        implements Principal
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new PortablePrincipal with the given name.
    *
    * @param sName  the name of the identity represented by this
    *               PortablePrincipal
    */
    public PofPrincipal(String sName)
        {
        m_sName = sName;
        }


    // ----- Principal interface --------------------------------------------

    /**
    * Return the name of this Principal.
    *
    * @return the name of this Principal
    */
    public String getName()
        {
        return m_sName;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compares this Principal to the specified object.
    *
    * @param o  a Principal to compare with
    *
    * @return true if the Principal passed in is the same as that
    *         encapsulated by this Principal, and false otherwise
    */
    public boolean equals(Object o)
        {
        return o == this ||
               o instanceof PofPrincipal &&
               equals(getName(), ((PofPrincipal) o).getName());
        }

    /**
    * Return a string representation of this Principal.
    *
    * @return a string representation of this Principal
    */
    public String toString()
        {
        return "PofPrincipal(" + getName() + ")";
        }

    /**
    * Return a hashcode for this Principal.
    *
    * @return a hashcode for this Principal
    */
    public int hashCode()
        {
        String sName = m_sName;
        return sName == null ? 0 : sName.hashCode();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of the identity represented by this Principal.
    */
    protected String m_sName;
    }