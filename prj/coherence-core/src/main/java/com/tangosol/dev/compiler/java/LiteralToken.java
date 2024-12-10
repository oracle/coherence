/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


/**
 * A Java literal (not including the null and boolean literals).
 *
 * @version 1.00, 11/21/96
 * @author     Cameron Purdy
 */
public class LiteralToken
        extends Token
    {
    // ----- constructors  --------------------------------------------------

    /**
    * Constructs an integer LiteralToken object.
    *
    * @param iValue Integer value of the token
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public LiteralToken(int iValue, int iLine, int ofInLine, int cchToken)
        {
        super(LITERAL, INT, LIT_INT, iValue, null, iLine, ofInLine, cchToken);
        }

    /**
    * Constructs an integer LiteralToken object which may be stored as a
    * negative number even though the tokenized value is positive.
    *
    * @param iValue Integer value of the token
    * @param bStoredNeg Pass as true to specify that the value holds the
    *        unary negative of the actual tokenized value
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    *
    * @see #fStoredNeg
    */
    public LiteralToken(int iValue, boolean bStoredNeg, int iLine, int ofInLine, int cchToken)
        {
        super(LITERAL, INT, LIT_INT, iValue, null, iLine, ofInLine, cchToken);
        fStoredNeg = bStoredNeg;
        }

    /**
    * Constructs a long integer LiteralToken object.
    *
    * @param lValue Long integer value of the token
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public LiteralToken(long lValue, int iLine, int ofInLine, int cchToken)
        {
        super(LITERAL, LONG, LIT_LONG, lValue, null, iLine, ofInLine, cchToken);
        }

    /**
    * Constructs a long integer LiteralToken object which may be stored as a
    * negative number even though the tokenized value is positive.
    *
    * @param lValue Long integer value of the token
    * @param bStoredNeg Pass as true to specify that the value holds the
    *        unary negative of the actual tokenized value
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    *
    * @see #fStoredNeg
    */
    public LiteralToken(long lValue, boolean bStoredNeg, int iLine, int ofInLine, int cchToken)
        {
        super(LITERAL, LONG, LIT_LONG, lValue, null, iLine, ofInLine, cchToken);
        fStoredNeg = bStoredNeg;
        }

    /**
    * Constructs a float LiteralToken object.
    *
    * @param flValue float value of the token
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public LiteralToken(float flValue, int iLine, int ofInLine, int cchToken)
        {
        super(LITERAL, FLOAT, LIT_FLOAT, flValue, null, iLine, ofInLine, cchToken);
        }

    /**
    * Constructs a double float LiteralToken object.
    *
    * @param dflValue double float value of the token
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public LiteralToken(double dflValue, int iLine, int ofInLine, int cchToken)
        {
        super(LITERAL, DOUBLE, LIT_DOUBLE, dflValue, null, iLine, ofInLine, cchToken);
        }

    /**
    * Constructs a character LiteralToken object.
    *
    * @param chValue character value of the token
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public LiteralToken(char chValue, int iLine, int ofInLine, int cchToken)
        {
        super(LITERAL, CHAR, LIT_CHAR, chValue, null, iLine, ofInLine, cchToken);
        }

    /**
    * Constructs a string LiteralToken object.
    *
    * @param sValue String value of the token
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public LiteralToken(String sValue, int iLine, int ofInLine, int cchToken)
        {
        super(LITERAL, STRING, LIT_STRING, sValue, null, iLine, ofInLine, cchToken);
        }

    /**
    * Constructs a literal Java token based on an existing token; this is
    * like cloning a token, but allows the caller to provide position/length
    * information.
    *
    * @param that the Java literal token to clone from
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    *
    * @see com.tangosol.dev.compiler.java.Token
    */
    public LiteralToken(LiteralToken that, int iLine, int ofInLine, int cchToken)
        {
        super(that, iLine, ofInLine, cchToken);
        fStoredNeg = that.fStoredNeg;
        }


    // ----- token interface ------------------------------------------------

    /**
    * Provides the value of the token if applicable.  This is used for
    * for Java literals.
    *
    * @return the value of the token or null if the token has no value
    */
    public Object getValue()
        {
        Object value = this.value;
        if (fStoredNeg)
            {
            switch (subcat)
                {
                case INT:
                    if (isOutOfRange())
                        {
                        value = 0;
                        }
                    else
                        {
                        value = -((Number) value).intValue();
                        }
                    break;
                case LONG:
                    if (isOutOfRange())
                        {
                        value = 0L;
                        }
                    else
                        {
                        value = -((Number) value).longValue();
                        }
                    break;
                }
            }
        return value;
        }

    /**
    * Converts the integer to a string.
    *
    * @return the integer value converted to a string
    */
    public String getText()
        {
        if (text == null)
            {
            switch (subcat)
                {
                case INT:
                    if (isOutOfRange())
                        {
                        text = "0";
                        }
                    else if (fStoredNeg)
                        {
                        int i = ((Integer) value).intValue();
                        text = Integer.toString(-i);
                        }
                    else
                        {
                        text = ((Integer) value).toString();
                        }
                    break;
                case LONG:
                    if (isOutOfRange())
                        {
                        text = "0";
                        }
                    else if (fStoredNeg)
                        {
                        long l = ((Long) value).longValue();
                        text = Long.toString(-l);
                        }
                    else
                        {
                        text = ((Long) value).toString();
                        }
                    break;
                case FLOAT:
                    text = ((Float) value).toString() + "F";
                    break;
                case DOUBLE:
                    text = ((Double) value).toString();
                    break;
                case CHAR:
                    text = printableChar(charValue());
                    break;
                case STRING:
                    text = printableString(stringValue());
                    break;
                }
            }

        return text;
        }


    // ----- value extraction methods ---------------------------------------

    /**
    * Returns the token's integer value.
    *
    * @return the integer value
    */
    public int intValue()
        {
        int iValue;
        switch (subcat)
            {
            case CHAR:
                iValue = ((Character) value).charValue();
                break;
            case STRING:
                iValue = Integer.decode((String) value).intValue();
                break;
            default:
                iValue = ((Number) value).intValue();
                break;
            }
        return (fStoredNeg ? -iValue : iValue);
        }

    /**
    * Returns the token's long integer value.
    *
    * @return the long integer value
    */
    public long longValue()
        {
        long lValue;
        switch (subcat)
            {
            case CHAR:
                lValue = ((Character) value).charValue();
                break;
            case STRING:
                lValue = Long.decode((String) value).longValue();
                break;
            default:
                lValue = ((Number) value).longValue();
                break;
            }
        return (fStoredNeg ? -lValue : lValue);
        }

    /**
    * Returns the token's float value.
    *
    * @return the float value
    */
    public float floatValue()
        {
        float flValue;
        switch (subcat)
            {
            case CHAR:
                flValue = ((Character) value).charValue();
                break;
            case STRING:
                flValue = Float.valueOf((String) value).floatValue();
                break;
            default:
                flValue = ((Number) value).floatValue();
                break;
            }
        return (fStoredNeg ? -flValue : flValue);
        }

    /**
    * Returns the token's double float value.
    *
    * @return the double float value
    */
    public double doubleValue()
        {
        double dflValue;
        switch (subcat)
            {
            case CHAR:
                dflValue = ((Character) value).charValue();
                break;
            case STRING:
                dflValue = Double.valueOf((String) value).doubleValue();
                break;
            default:
                dflValue = ((Number) value).doubleValue();
                break;
            }
        return (fStoredNeg ? -dflValue : dflValue);
        }

    /**
    * Returns the token's char value.
    *
    * @return the char value
    */
    public char charValue()
        {
        char chValue;
        switch (subcat)
            {
            case CHAR:
                chValue = ((Character) value).charValue();
                break;
            case STRING:
                String sValue = (String) value;
                chValue = (sValue.length() > 0 ? sValue.charAt(0) : '\0');
                break;
            default:
                int iValue = ((Number) value).intValue();
                chValue = (char) (fStoredNeg ? -iValue : iValue);
                break;
            }
        return chValue;
        }

    /**
    * Returns the token's string value.
    *
    * @return the string value
    */
    public String stringValue()
        {
        return value.toString();
        }


    // ----- numeric methods ------------------------------------------------

    /**
    * Applies the unary minus operator to the integer value.
    */
    public void negate()
        {
        fStoredNeg = !fStoredNeg;
        text       = null;
        }

    /**
    * Returns true if the integer value is out-of-range due to its sign
    * (ie. the integer value is the unary minus of Integer.MIN_VALUE)
    *
    * @return true if integer value is out of range
    */
    public boolean isOutOfRange()
        {
        switch (subcat)
            {
            case INT:
                int iValue = ((Integer) value).intValue();
                return (fStoredNeg && iValue == Integer.MIN_VALUE);
            case LONG:
                long lValue = ((Long) value).longValue();
                return (fStoredNeg && lValue == Long.MIN_VALUE);
            }

        return false;
        }


    // ----- string-related methods -----------------------------------------

    /**
    * Converts the char to a printable string.
    *
    * @return the char converted to a string
    */
    public static String printableChar(char ch)
        {
        return toQuotedCharEscape(ch);
        }

    /**
    * Converts the string to a printable string (escapes are de-escaped).
    *
    * @return the string (escaped as necessary)
    */
    public static String printableString(String s)
        {
        return toQuotedStringEscape(s);
        }


    // ----- data members ---------------------------------------------------

    /**
    * Integers are stored as network-order (big endian) 2's complement
    * signed 4-byte values.  The most negative integer value (-2^31) is valid
    * as a decimal in a Java script only if it is preceded by the unary minus,
    * because it cannot exist as a positive integer.  In order to store this
    * number, the token is created as the negative value, although until a
    * syntactical pass occurs, it is unknown whether the token is meant to be
    * positive or negative.  If the value is stored as a negative although it
    * is actually considered a positive (until the unary minus is evaluated),
    * then fStoredNeg is set to true.
    */
    protected boolean fStoredNeg;
    }
