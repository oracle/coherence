/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* LiteralBaseToken is the BaseToken that represents literals such as String,
* Integer, Long, Float, and Double.
*
* @author djl  2009.03.14
*/
public class LiteralBaseToken
        extends LeafBaseToken
    {
   // ----- constructors ---------------------------------------------------

    /**
    * Construct a new LiteralBaseToken with the given parameters.
    *
    * @param nType  the type code for the token
    * @param s      the string representation for the token
    */
    public LiteralBaseToken(int nType, String s)
        {
        m_nType  = nType;
        m_sValue = s;
        }


    // ----- LeafBaseToken interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public String getValue()
        {
        return m_sValue;
        }


    // ----- BaseToken interface --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean isLiteral()
        {
        return true;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the type code of this token.
    *
    * @return the type code
    */
    public int getType()
        {
        return m_nType;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this token.
    *
    * @return a String description of the token
    */
    public String toString()
        {
        String s     = "Literal{";
        int    nType = m_nType;

        if (nType == STRINGLITERAL)
            {
            s = s + "\"";
            }
        s = s + getValue();
        if (nType == DOUBLELITERAL)
            {
            s = s + "d";
            }
        else if (nType == LONGLITERAL)
            {
            s = s + "l";
            }
        else if (nType == SHORTLITERAL)
            {
            s = s + "s";
            }
        else if (nType == STRINGLITERAL)
            {
            s = s + "\"";
            }
        return s + "}";
        }


    // ----- static literal creation healper's  -----------------------------

    /**
    * Create new LiteralBaseToken representing a String with given value
    *
    * @param s  the text of the literal
    *
    * @return a LiteralBaseToken for a String
    */
    public static LiteralBaseToken createString(String s)
        {
        return new LiteralBaseToken(STRINGLITERAL, s);
        }

    /**
    * Create new LiteralBaseToken representing a Integer with given value
    *
    * @param s  the text of the literal
    *
    * @return a LiteralBaseToken for a Integer
    */
    public static LiteralBaseToken createShort(String s)
        {
         return new LiteralBaseToken(SHORTLITERAL, s);
        }

    /**
    * Create new LiteralBaseToken representing a Integer with given value
    *
    * @param s  the text of the literal
    *
    * @return a LiteralBaseToken for a Integer
    */
    public static LiteralBaseToken createInteger(String s)
        {
        return new LiteralBaseToken(INTEGERLITERAL, s);
        }

    /**
    * Create new LiteralBaseToken representing a Long with given value
    *
    * @param s  the text of the literal
    *
    * @return a LiteralBaseToken for a Long
    */
    public static LiteralBaseToken createLong(String s)
        {
        return new LiteralBaseToken(LONGLITERAL, s);
        }

    /**
    * Create new LiteralBaseToken representing a float with given value
    *
    * @param s  the text of the literal
    *
    * @return a LiteralBaseToken for a Float
    */
    public static LiteralBaseToken createFloat(String s)
        {
        return new LiteralBaseToken(FLOATLITERAL, s);
        }

    /**
    * Create new LiteralBaseToken representing a Double with given value
    *
    * @param s  the text of the literal
    *
    * @return a LiteralBaseToken for a Double
    */
    public static LiteralBaseToken createDouble(String s)
        {
        return new LiteralBaseToken(DOUBLELITERAL, s);
        }

    /**
    * Create new LiteralBaseToken representing a Boolean with given value
    *
    * @param s  the text of the literal
    *
    * @return a LiteralBaseToken for a Double
    */
    public static LiteralBaseToken createBoolean(String s)
        {
        return new LiteralBaseToken(BOOLEANLITERAL, s);
        }

    /**
    * Create new LiteralBaseToken representing a null with given value
    *
    * @param s  the text of the literal
    *
    * @return a LiteralBaseToken for a Double
    */
    public static LiteralBaseToken createNull(String s)
        {
        return new LiteralBaseToken(NULLLITERAL, s);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The type code for this literal token
    */
    private int m_nType = -1;

    /**
    * The string representation of this literal token
    */
    private String m_sValue = "";


    // ----- constants ------------------------------------------------------

    /**
    * The numberic code for a string literal
    */
    public static final int STRINGLITERAL = 1;

    /**
    * The numberic code for a integer literal
    */
    public static final int INTEGERLITERAL = 2;

    /**
    * The numberic code for a long literal
    */
    public static final int LONGLITERAL = 3;

    /**
    * The numberic code for a float literal
    */
    public static final int FLOATLITERAL = 4;

    /**
    * The numberic code for a double literal
    */
    public static final int DOUBLELITERAL = 5;

    /**
    * The numberic code for a boolean literal
    */
    public static final int BOOLEANLITERAL = 6;

    /**
    * The numberic code for a boolean literal
    */
    public static final int NULLLITERAL = 7;

    /**
    * The numberic code for a short literal
    */
    public static final int SHORTLITERAL = 8;
    }
