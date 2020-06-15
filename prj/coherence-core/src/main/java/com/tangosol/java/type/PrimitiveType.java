/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.java.type;


/**
* The primitive type implementation.
*
* @author cp  2000.10.13
*/
public class PrimitiveType
        extends Type
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct a primitive type object.
    */
    PrimitiveType(String sSig)
        {
        super(sSig);

        azzert(sSig.length() == 1);
        switch (sSig.charAt(0))
            {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                break;
            default:
                throw azzert();
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine if the primitive type is a number.
    *
    * @return true if type is a number
    */
    public boolean isNumeric()
        {
        switch (getSignature().charAt(0))
            {
            case 'C':
            case 'B':
            case 'S':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                return true;

            default:
                return false;
            }
        }

    /**
    * Determine if the primitive type is an integral number.
    *
    * @return true if type is an integral number
    */
    public boolean isIntegral()
        {
        switch (getSignature().charAt(0))
            {
            case 'C':
            case 'B':
            case 'S':
            case 'I':
            case 'J':
                return true;

            default:
                return false;
            }
        }

    /**
    * Determine if the primitive type is a floating point number.
    *
    * @return true if type is a floating point number
    */
    public boolean isFloatingPoint()
        {
        switch (getSignature().charAt(0))
            {
            case 'F':
            case 'D':
                return true;

            default:
                return false;
            }
        }

    /**
    * Determine the number of stack words used to load this type.
    *
    * @return the number of words used on the stack to hold this type
    */
    public int getWordCount()
        {
        switch (getSignature().charAt(0))
            {
            case 'J':
            case 'D':
                return 2;

            default:
                return 1;
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the type into its source code form.
    *
    * @return a source code representation of the type
    */
    public String toString()
        {
        String sSig = getSignature();

        switch (sSig.charAt(0))
            {
            case 'Z': return "boolean";
            case 'C': return "char";
            case 'B': return "byte";
            case 'S': return "short";
            case 'I': return "int";
            case 'J': return "long";
            case 'F': return "float";
            case 'D': return "double";
            }

        throw new IllegalStateException(
                "Illegal primitive signature: " + sSig);
        }
    }