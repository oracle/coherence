/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.java.type;


import com.tangosol.util.Base;


/**
* The base for all Type implementations.  Additional types are possible for
* purposes such as compilation, which typically adds null, unknown and error
* types.
*
* @see PrimitiveType
* @see ReferenceType
* @see ArrayType
* @see ClassType
* @see VoidType
*
* @author cp  2000.10.13
*/
public abstract class Type
        extends Base
        // TODO implements Signable
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct a type object.
    */
    protected Type(String sSig)
        {
        m_sSig = sSig;
        }


    // ----- default instances ----------------------------------------------

    public static final ClassType     OBJECT  = new ClassType("java.lang.Object");
    public static final PrimitiveType BOOLEAN = new PrimitiveType("Z");
    public static final PrimitiveType CHAR    = new PrimitiveType("C");
    public static final PrimitiveType BYTE    = new PrimitiveType("B");
    public static final PrimitiveType SHORT   = new PrimitiveType("S");
    public static final PrimitiveType INT     = new PrimitiveType("I");
    public static final PrimitiveType LONG    = new PrimitiveType("J");
    public static final PrimitiveType FLOAT   = new PrimitiveType("F");
    public static final PrimitiveType DOUBLE  = new PrimitiveType("D");
    public static final VoidType      VOID    = new VoidType();
    public static final NullType      NULL    = new NullType();
    public static final UnknownType   UNKNOWN = new UnknownType();


    // ----- factory --------------------------------------------------------

    /**
    * Parse the passed type signature and return the Type that represents
    * the signature.
    *
    * @param sSig  the signature to parse
    *
    * @return the corresponding Type object
    */
    public static Type parseSignature(String sSig)
        {
        int cch = sSig.length();
        switch (cch)
            {
            case 0:
                break;

            case 1:
                switch (sSig.charAt(0))
                    {
                    case 'Z': return BOOLEAN;
                    case 'C': return CHAR;
                    case 'B': return BYTE;
                    case 'S': return SHORT;
                    case 'I': return INT;
                    case 'J': return LONG;
                    case 'F': return FLOAT;
                    case 'D': return DOUBLE;
                    case 'V': return VOID;
                    }
                break;

            default:
                switch (sSig.charAt(0))
                    {
                    case '[':
                        return new ArrayType(parseSignature(sSig.substring(1)));

                    case 'L':
                        if (sSig.charAt(cch-1) == ';')
                            {
                            return new ClassType(sSig.substring(1, sSig.length() - 1));
                            }
                        break;
                    }
                break;
            }

        throw new IllegalArgumentException("Illegal signature: " + sSig);
        }

    /**
    * Helper to construct an ArrayType to hold elements of this Type.
    *
    * @return an array type object whose elements are this type
    */
    public ArrayType getArrayType()
        {
        return new ArrayType(this);
        }


    // ----- Signable interface ---------------------------------------------

    /**
    * Determine the signature of the Element.
    *
    * @return the signature of the Element
    */
    public String getSignature()
        {
        return m_sSig;
        }

    /**
    * Modify the signature of the Element.
    *
    * @param sSig  the new Element signature
    *
    * @exception IllegalArgumentException is thrown if the value is not
    *            valid for this Element
    * @exception UnsupportedOperationException is thrown if the Element's
    *            signature is immutable
    */
    public void setSignature(String sSig)
        {
        // types are immutable objects
        throw new UnsupportedOperationException();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the number of stack words used to load this type.
    *
    * @return the number of words used on the stack to hold this type
    */
    public int getWordCount()
        {
        return 1;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the type into its source code form.
    *
    * @return a source code representation of the type
    */
    public abstract String toString();

    /**
    * Calculate a hash code for the Type.
    *
    * @return the hash code for the Type
    */
    public int hashCode()
        {
        return m_sSig.hashCode();
        }

    /**
    * Determine if two Type instances are equal.
    *
    * @param o  the Type to compare with
    *
    * @return true if this Type equals the passed Type
    */
    public boolean equals(Object o)
        {
        return m_sSig.equals(((Type) o).m_sSig);
        }


    // ----- data members ---------------------------------------------------

    private String m_sSig;
    }