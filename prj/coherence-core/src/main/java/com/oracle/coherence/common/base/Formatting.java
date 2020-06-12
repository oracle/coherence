/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.tangosol.util.Base;
import com.tangosol.util.ByteSequence;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.tangosol.util.Base.*;

/**
 * Class for providing formatting functionality for various types.
 *
 * @author cp  2000.08.02
 * @since 20.06
 */
@SuppressWarnings({"UnnecessaryLocalVariable", "DuplicatedCode", "ForLoopReplaceableByForEach"})
public abstract class Formatting
    {
    // ----- formatting support: character/String ---------------------------

    /**
    * Format a double value as a String.
    *
    * @param dfl         a double value
    * @param cMinDigits  the minimum number of digits of precision to display
    *
    * @return the double value formatted as a String
    */
    public static String toString(double dfl, int cMinDigits)
        {
        BigDecimal decVal     = new BigDecimal(dfl);
        BigInteger intVal     = decVal.toBigInteger();
        String     sIntVal    = intVal.toString();
        int        cIntDigits = sIntVal.length() - (intVal.signum() <= 0 ? 1 : 0);
        int        cDecDigits = decVal.scale();
        if (cIntDigits >= cMinDigits || cDecDigits == 0)
            {
            return sIntVal;
            }

        int cRemDigits = cMinDigits - cIntDigits;
        if (cDecDigits > cRemDigits)
            {
            //noinspection deprecation
            decVal = decVal.setScale(cRemDigits, BigDecimal.ROUND_HALF_UP);
            }

        String  sDecVal = decVal.toString();
        int     of      = sDecVal.length() - 1;
        if (sDecVal.length() > 1 && sDecVal.charAt(of) == '0')
            {
            do
                {
                --of;
                }
            while (sDecVal.charAt(of) == '0');
            if (sDecVal.charAt(of) == '.')
                {
                --of;
                }
            sDecVal = sDecVal.substring(0, of + 1);
            }

        return sDecVal;
        }

    /**
     * Format a Unicode character to the Unicode escape sequence of '\'
     * + 'u' + 4 hex digits.
     *
     * @param ch the character
     * @return the Unicode escape sequence
     */
    public static String toUnicodeEscape(char ch)
        {
        int n = ch;
        char[] ach = new char[6];

        ach[0] = '\\';
        ach[1] = 'u';
        ach[2] = HEX[n >> 12];
        ach[3] = HEX[n >> 8 & 0x0F];
        ach[4] = HEX[n >> 4 & 0x0F];
        ach[5] = HEX[n & 0x0F];

        return new String(ach);
        }

    /**
     * Format a char to a printable escape if necessary.
     *
     * @param ch the char
     * @return a printable String representing the passed char
     */
    public static String toCharEscape(char ch)
        {
        char[] ach = new char[6];
        int cch = escape(ch, ach, 0);
        return new String(ach, 0, cch);
        }

    /**
     * Format a char to a printable escape if necessary as it would
     * appear (quoted) in Java source code.
     * <p>
     * This is a replacement for Text.printableChar().
     *
     * @param ch the character
     * @return a printable String in single quotes  representing the
     * passed char
     */
    public static String toQuotedCharEscape(char ch)
        {
        char[] ach = new char[8];
        ach[0] = '\'';
        int cch = escape(ch, ach, 1);
        ach[cch + 1] = '\'';
        return new String(ach, 0, cch + 2);
        }

    /**
     * Format a String escaping characters as necessary.
     *
     * @param s the String
     * @return a printable String representing the passed String
     */
    public static String toStringEscape(String s)
        {
        char[] achSrc = s.toCharArray();
        int cchSrc = achSrc.length;
        int ofSrc = 0;

        int cchDest = cchSrc * 6;            // 100% safe
        char[] achDest = new char[cchDest];
        int ofDest = 0;

        while (ofSrc < cchSrc)
            {
            ofDest += escape(achSrc[ofSrc++], achDest, ofDest);
            }

        return new String(achDest, 0, ofDest);
        }

    /**
     * Format a String as it would appear (quoted) in Java source code,
     * escaping characters as necessary.
     * <p>
     * This is a replacement for Text.printableString().
     *
     * @param s the String
     * @return a printable String in double quotes  representing the
     * passed String
     */
    public static String toQuotedStringEscape(String s)
        {
        char[] achSrc = s.toCharArray();
        int cchSrc = achSrc.length;
        int ofSrc = 0;

        int cchDest = cchSrc * 6 + 2;        // 100% safe
        char[] achDest = new char[cchDest];
        int ofDest = 0;

        achDest[ofDest++] = '\"';
        while (ofSrc < cchSrc)
            {
            ofDest += escape(achSrc[ofSrc++], achDest, ofDest);
            }
        achDest[ofDest++] = '\"';

        return new String(achDest, 0, ofDest);
        }

    /**
     * Format a char to a printable escape if necessary, putting the result
     * into the passed array.  The array must be large enough to accept six
     * characters.
     *
     * @param ch  the character to format
     * @param ach the array of characters to format into
     * @param of  the offset in the array to format at
     * @return the number of characters used to format the char
     */
    public static int escape(char ch, char[] ach, int of)
        {
        switch (ch)
            {
            case '\b':
                ach[of++] = '\\';
                ach[of] = 'b';
                return 2;
            case '\t':
                ach[of++] = '\\';
                ach[of] = 't';
                return 2;
            case '\n':
                ach[of++] = '\\';
                ach[of] = 'n';
                return 2;
            case '\f':
                ach[of++] = '\\';
                ach[of] = 'f';
                return 2;
            case '\r':
                ach[of++] = '\\';
                ach[of] = 'r';
                return 2;
            case '\"':
                ach[of++] = '\\';
                ach[of] = '\"';
                return 2;
            case '\'':
                ach[of++] = '\\';
                ach[of] = '\'';
                return 2;
            case '\\':
                ach[of++] = '\\';
                ach[of] = '\\';
                return 2;

            case 0x00:
            case 0x01:
            case 0x02:
            case 0x03:
            case 0x04:
            case 0x05:
            case 0x06:
            case 0x07:
            case 0x0B:
            case 0x0E:
            case 0x0F:
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x13:
            case 0x14:
            case 0x15:
            case 0x16:
            case 0x17:
            case 0x18:
            case 0x19:
            case 0x1A:
            case 0x1B:
            case 0x1C:
            case 0x1D:
            case 0x1E:
            case 0x1F:
                ach[of++] = '\\';
                ach[of++] = '0';
                ach[of++] = (char) (ch / 8 + '0');
                ach[of] = (char) (ch % 8 + '0');
                return 4;

            default:
                switch (Character.getType(ch))
                    {
                    case Character.CONTROL:
                    case Character.PRIVATE_USE:
                    case Character.UNASSIGNED:
                    {
                    int n = ch;
                    ach[of++] = '\\';
                    ach[of++] = 'u';
                    ach[of++] = HEX[n >> 12];
                    ach[of++] = HEX[n >> 8 & 0x0F];
                    ach[of++] = HEX[n >> 4 & 0x0F];
                    ach[of] = HEX[n & 0x0F];
                    }
                    return 6;
                    }
            }

        // character does not need to be escaped
        ach[of] = ch;
        return 1;
        }

    /**
     * Escapes the string for SQL.
     *
     * @return the string quoted for SQL and escaped as necessary
     */
    public static String toSqlString(String s)
        {
        if (s == null)
            {
            return "NULL";
            }

        if (s.length() == 0)
            {
            return "''";
            }

        if (s.indexOf('\'') < 0)
            {
            return '\'' + s + '\'';
            }

        char[] ach = s.toCharArray();
        int cch = ach.length;

        StringBuilder sb = new StringBuilder(cch + 16);

        // open quotes
        sb.append('\'');

        // scan for characters to escape
        int ofPrev = 0;
        for (int ofCur = 0; ofCur < cch; ++ofCur)
            {
            char ch = ach[ofCur];

            switch (ch)
                {
                case '\n':
                case '\'':
                {
                // copy up to this point
                if (ofCur > ofPrev)
                    {
                    sb.append(ach, ofPrev, ofCur - ofPrev);
                    }

                // process escape
                switch (ch)
                    {
                    case '\n':
                        // close quote, new line, re-open quote
                        sb.append("'\n'");
                        break;
                    case '\'':
                        // escape single quote with a second single quote
                        sb.append("''");
                        break;
                    }

                // processed up to the following character
                ofPrev = ofCur + 1;
                }
                }
            }

        // copy the remainder of the string
        if (ofPrev < cch)
            {
            sb.append(ach, ofPrev, cch - ofPrev);
            }

        // close quotes
        sb.append('\'');

        return sb.toString();
        }

    /**
     * Indent the passed multi-line string.
     *
     * @param sText   the string to indent
     * @param sIndent a string used to indent each line
     * @return the string, indented
     */
    public static String indentString(String sText, String sIndent)
        {
        return indentString(sText, sIndent, true);
        }

    /**
     * Textually indent the passed multi-line string.
     *
     * @param sText      the string to indent
     * @param sIndent    a string used to indent each line
     * @param fFirstLine true indents all lines;
     *                   false indents all but the first
     * @return the string, indented
     */
    public static String indentString(String sText, String sIndent, boolean fFirstLine)
        {
        char[] ach = sText.toCharArray();
        int cch = ach.length;

        StringBuilder sb = new StringBuilder();

        int iLine = 0;
        int of = 0;
        int ofPrev = 0;
        while (of < cch)
            {
            if (ach[of++] == '\n' || of == cch)
                {
                if (iLine++ > 0 || fFirstLine)
                    {
                    sb.append(sIndent);
                    }

                sb.append(sText, ofPrev, of);
                ofPrev = of;
                }
            }

        return sb.toString();
        }

    /**
     * Breaks the specified string into a multi-line string.
     *
     * @param sText   the string to break
     * @param nWidth  the max width of resulting lines (including the indent)
     * @param sIndent a string used to indent each line
     * @return the string, broken and indented
     */
    public static String breakLines(String sText, int nWidth, String sIndent)
        {
        return breakLines(sText, nWidth, sIndent, true);
        }

    /**
     * Breaks the specified string into a multi-line string.
     *
     * @param sText      the string to break
     * @param nWidth     the max width of resulting lines (including the
     *                   indent)
     * @param sIndent    a string used to indent each line
     * @param fFirstLine if true indents all lines;
     *                   otherwise indents all but the first
     * @return the string, broken and indented
     */
    public static String breakLines(String sText, int nWidth, String sIndent, boolean fFirstLine)
        {
        if (sIndent == null)
            {
            sIndent = "";
            }

        nWidth -= sIndent.length();
        if (nWidth <= 0)
            {
            throw new IllegalArgumentException("The width and indent are incompatible");
            }

        char[] ach = sText.toCharArray();
        int cch = ach.length;

        StringBuilder sb = new StringBuilder(cch);

        int ofPrev = 0;
        int of = 0;

        while (of < cch)
            {
            char c = ach[of++];

            boolean fBreak = false;
            int ofBreak = of;
            int ofNext = of;

            if (c == '\n')
                {
                fBreak = true;
                ofBreak--;
                }
            else if (of == cch)
                {
                fBreak = true;
                }
            else if (of == ofPrev + nWidth)
                {
                fBreak = true;

                //noinspection StatementWithEmptyBody
                while (!Character.isWhitespace(ach[--ofBreak]) && ofBreak > ofPrev)
                    {
                    }
                if (ofBreak == ofPrev)
                    {
                    ofBreak = of; // no spaces -- force the break
                    }
                else
                    {
                    ofNext = ofBreak + 1;
                    }
                }

            if (fBreak)
                {
                if (ofPrev > 0)
                    {
                    sb.append('\n')
                            .append(sIndent);
                    }
                else if (fFirstLine)
                    {
                    sb.append(sIndent);
                    }

                sb.append(sText, ofPrev, ofBreak);

                ofPrev = ofNext;
                }

            }

        return sb.toString();
        }

    /**
     * Create a String of the specified length containing the specified
     * character.
     *
     * @param ch  the character to fill the String with
     * @param cch the length of the String
     * @return a String containing the character <ch> repeated <cch> times
     */
    public static String dup(char ch, int cch)
        {
        char[] ach = new char[cch];
        for (int of = 0; of < cch; ++of)
            {
            ach[of] = ch;
            }
        return new String(ach);
        }

    /**
     * Create a String which is a duplicate of the specified number of the
     * passed String.
     *
     * @param s the String to fill the new String with
     * @param c the number of duplicates to put into the new String
     * @return a String containing the String <tt>s</tt> repeated <tt>c</tt>
     * times
     */
    public static String dup(String s, int c)
        {
        if (c < 1)
            {
            return "";
            }
        if (c == 1)
            {
            return s;
            }

        char[] achPat = s.toCharArray();
        int cchPat = achPat.length;
        int cchBuf = cchPat * c;
        char[] achBuf = new char[cchBuf];
        for (int i = 0, of = 0; i < c; ++i, of += cchPat)
            {
            System.arraycopy(achPat, 0, achBuf, of, cchPat);
            }
        return new String(achBuf);
        }

    /**
     * Replace all occurrences of the specified substring in the specified
     * string.
     *
     * @param sText string to change
     * @param sFrom pattern to change from
     * @param sTo   pattern to change to
     * @return modified string
     */
    public static String replace(String sText, String sFrom, String sTo)
        {
        if (sFrom.length() == 0)
            {
            return sText;
            }

        StringBuilder sbTextNew = new StringBuilder();
        int iTextLen = sText.length();
        int iStart = 0;

        while (iStart < iTextLen)
            {
            int iPos = sText.indexOf(sFrom, iStart);
            if (iPos != -1)
                {
                sbTextNew.append(sText, iStart, iPos);
                sbTextNew.append(sTo);
                iStart = iPos + sFrom.length();
                }
            else
                {
                sbTextNew.append(sText.substring(iStart));
                break;
                }
            }

        return sbTextNew.toString();
        }

    /**
     * Parse a character-delimited String into an array of Strings.
     *
     * @param s       character-delimited String to parse
     * @param chDelim character delimiter
     * @return an array of String objects parsed from the passed String
     */
    public static String[] parseDelimitedString(String s, char chDelim)
        {
        if (s == null)
            {
            return null;
            }

        List<String> list = new ArrayList<>();
        int ofPrev = -1;
        while (true)
            {
            int ofNext = s.indexOf(chDelim, ofPrev + 1);
            if (ofNext < 0)
                {
                list.add(s.substring(ofPrev + 1));
                break;
                }
            else
                {
                list.add(s.substring(ofPrev + 1, ofNext));
                }

            ofPrev = ofNext;
            }

        return list.toArray(new String[0]);
        }

    /**
     * Format the content of the passed integer array as a delimited string.
     *
     * @param an     the array
     * @param sDelim the delimiter
     * @return the formatted string
     */
    public static String toDelimitedString(int[] an, String sDelim)
        {
        int c = an.length;
        if (c > 0)
            {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c; i++)
                {
                sb.append(sDelim).append(an[i]);
                }
            return sb.substring(sDelim.length());
            }
        else
            {
            return "";
            }
        }

    /**
     * Format the content of the passed long array as a delimited string.
     *
     * @param al     the array
     * @param sDelim the delimiter
     * @return the formatted string
     */
    public static String toDelimitedString(long[] al, String sDelim)
        {
        int c = al.length;
        if (c > 0)
            {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c; i++)
                {
                sb.append(sDelim).append(al[i]);
                }
            return sb.substring(sDelim.length());
            }
        else
            {
            return "";
            }
        }

    /**
     * Format the content of the passed Object array as a delimited string.
     *
     * @param ao     the array
     * @param sDelim the delimiter
     * @return the formatted string
     */
    public static String toDelimitedString(Object[] ao, String sDelim)
        {
        int c = ao.length;
        if (c > 0)
            {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c; i++)
                {
                sb.append(sDelim).append(ao[i]);
                }
            return sb.substring(sDelim.length());
            }
        else
            {
            return "";
            }
        }

    /**
     * Format the content of the passed Iterator as a delimited string.
     *
     * @param iter   the Iterator
     * @param sDelim the delimiter
     *
     * @return the formatted string
     */
    public static String toDelimitedString(Iterator<?> iter, String sDelim)
        {
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext())
            {
            sb.append(sDelim).append(iter.next());
            }
        return sb.length() == 0 ? "" : sb.substring(sDelim.length());
        }

    /**
     * Capitalize a string.
     *
     * @param s the string to capitalize
     * @return the capitalized string
     */
    public static String capitalize(String s)
        {
        return s.length() > 1
                       ? s.substring(0, 1).toUpperCase() + s.substring(1)
                       : s.toUpperCase();
        }

    /**
     * Truncate a string to the specified character count.
     *
     * @param s      the String to be truncated
     * @param cLimit expected character count
     * @return the truncated String
     */
    public static String truncateString(String s, int cLimit)
        {
        int cChar = s.length();
        return cChar > cLimit
                       ? s.substring(0, cLimit) + "...(" + (cChar - cLimit) + " more)"
                       : s;
        }

    /**
     * Provide a string representation of elements within the collection until
     * the concatenated string length exceeds {@code cLimit}.
     *
     * @param coll   the collection of elements to describe
     * @param cLimit expected character count
     * @return the truncated string representation of the provided collection
     */
    public static String truncateString(Collection<?> coll, int cLimit)
        {
        StringBuilder sb = new StringBuilder(Classes.getSimpleName(coll.getClass()))
                                   .append('[');

        cLimit += sb.length() + 1;

        int c = 1;
        for (Iterator<?> iter = coll.iterator(); iter.hasNext() && sb.length() < cLimit; ++c)
            {
            if (c > 1)
                {
                sb.append(", ");
                }
            sb.append(iter.next());
            }

        if (c < coll.size() && sb.length() >= cLimit)
            {
            sb.append(", ...");
            }

        return sb.append(']').toString();
        }


    // ----- formatting support: hex values ---------------------------------

    /**
     * Returns true if the passed character is a hexadecimal digit.
     *
     * @param ch The character to check
     */
    public static boolean isHex(char ch)
        {
        return ((ch >= '0') && (ch <= '9'))
                       || ((ch >= 'A') && (ch <= 'F'))
                       || ((ch >= 'a') && (ch <= 'f'));
        }

    /**
     * Returns the integer value of a hexadecimal digit.
     *
     * @param ch The character to convert
     */
    public static int hexValue(char ch)
        {
        if ((ch >= '0') && (ch <= '9'))
            {
            return ch - '0';
            }
        else if ((ch >= 'A') && (ch <= 'F'))
            {
            return ch - 'A' + 10;
            }
        else if ((ch >= 'a') && (ch <= 'f'))
            {
            return ch - 'a' + 10;
            }
        else
            {
            throw new IllegalArgumentException("Character \"" + ch + "\" is not a valid hexadecimal digit.");
            }
        }

    /**
     * Calculate the number of hex digits needed to display the passed value.
     *
     * @param n the value
     * @return the number of hex digits needed to display the value
     */
    public static int getMaxHexDigits(int n)
        {
        int cDigits = 0;
        do
            {
            cDigits++;
            n >>>= 4;
            }
        while (n != 0);

        return cDigits;
        }

    /**
     * Format the passed integer as a fixed-length hex string.
     *
     * @param n       the value
     * @param cDigits the length of the resulting hex string
     * @return the hex value formatted to the specified length string
     */
    public static String toHexString(int n, int cDigits)
        {
        char[] ach = new char[cDigits];
        while (cDigits > 0)
            {
            ach[--cDigits] = HEX[n & 0x0F];
            n >>>= 4;
            }

        return new String(ach);
        }

    /**
     * Convert a byte to the hex sequence of 2 hex digits.
     *
     * @param b the byte
     * @return the hex sequence
     */
    public static String toHex(int b)
        {
        int n = b & 0xFF;
        char[] ach = new char[2];

        ach[0] = HEX[n >> 4];
        ach[1] = HEX[n & 0x0F];

        return new String(ach);
        }

    /**
     * Convert a byte array to the hex sequence of 2 hex digits per byte.
     * <p>
     * This is a replacement for Text.toString(char[]).
     *
     * @param ab the byte array
     * @return the hex sequence
     */
    public static String toHex(byte[] ab)
        {
        int cb = ab.length;
        char[] ach = new char[cb * 2];

        for (int ofb = 0, ofch = 0; ofb < cb; ++ofb)
            {
            int n = ab[ofb] & 0xFF;
            ach[ofch++] = HEX[n >> 4];
            ach[ofch++] = HEX[n & 0x0F];
            }

        return new String(ach);
        }

    /**
     * Convert a byte to a hex sequence of '0' + 'x' + 2 hex digits.
     *
     * @param b the byte
     * @return the hex sequence
     */
    public static String toHexEscape(byte b)
        {
        int n = b & 0xFF;
        char[] ach = new char[4];

        ach[0] = '0';
        ach[1] = 'x';
        ach[2] = HEX[n >> 4];
        ach[3] = HEX[n & 0x0F];

        return new String(ach);
        }

    /**
     * Convert a byte array to a hex sequence of '0' + 'x' + 2 hex digits
     * per byte.
     *
     * @param ab the byte array
     * @return the hex sequence
     */
    public static String toHexEscape(byte[] ab)
        {
        return toHexEscape(ab, 0, ab.length);
        }

    /**
     * Convert a byte array to a hex sequence of '0' + 'x' + 2 hex digits
     * per byte.
     *
     * @param ab the byte array
     * @param of the offset into array
     * @param cb the number of bytes to convert
     * @return the hex sequence
     */
    public static String toHexEscape(byte[] ab, int of, int cb)
        {
        char[] ach = new char[2 + cb * 2];

        ach[0] = '0';
        ach[1] = 'x';

        for (int ofb = of, ofch = 2, ofStop = of + cb; ofb < ofStop; ++ofb)
            {
            int n = ab[ofb] & 0xFF;
            ach[ofch++] = HEX[n >> 4];
            ach[ofch++] = HEX[n & 0x0F];
            }

        return new String(ach);
        }

    /**
     * Convert a ByteSequence to a hex sequence of '0' + 'x' + 2 hex digits
     * per byte.
     *
     * @param   seq  the ByteSequence
     * @param   of   the offset into the byte sequence
     * @param   cb   the number of bytes to convert
     *
     * @return  the hex sequence
     *
     * @since Coherence 3.7
     */
    public static String toHexEscape(ByteSequence seq, int of, int cb)
        {
        if (cb > 0)
            {
            char[] ach = new char[2 + cb * 2];

            ach[0] = '0';
            ach[1] = 'x';

            for (int ofb = of, ofch = 2, ofStop = of + cb; ofb < ofStop; ++ofb)
                {
                int n = seq.byteAt(ofb) & 0xFF;
                ach[ofch++] = HEX[n >> 4];
                ach[ofch++] = HEX[n & 0x0F];
                }

            return new String(ach);
            }
        else
            {
            return "";
            }
        }

    /**
     * Convert a byte array to a hex dump.
     * <p>
     * This is a replacement for Text.toString(byte[] ab, int cBytesPerLine).
     *
     * @param ab            the byte array to format as a hex string
     * @param cBytesPerLine the number of bytes to display on a line
     * @return a multi-line hex dump
     */
    public static String toHexDump(byte[] ab, int cBytesPerLine)
        {
        int cb = ab.length;
        if (cb == 0)
            {
            return "";
            }

        // calculate number of digits required to show offset
        int cDigits = 0;
        int cbTemp = cb - 1;
        do
            {
            cDigits += 2;
            cbTemp /= 0x100;
            }
        while (cbTemp > 0);

        // calculate number and size of lines
        int cLines = (cb + cBytesPerLine - 1) / cBytesPerLine;
        int cCharsPerLine = cDigits + 4 * cBytesPerLine + 5;

        // pre-allocate buffer to build hex dump
        int cch = cLines * cCharsPerLine;
        char[] ach = new char[cch];

        // offsets within each line for formatting stuff
        int ofColon = cDigits;
        int ofLF = cCharsPerLine - 1;

        // offsets within each line for data
        int ofHexInLine = ofColon + 3;
        int ofCharInLine = ofLF - cBytesPerLine;

        int ofByte = 0;
        int ofLine = 0;
        for (int iLine = 0; iLine < cLines; ++iLine)
            {
            // format offset
            int nOffset = ofByte;
            int ofDigit = ofLine + cDigits;
            for (int i = 0; i < cDigits; ++i)
                {
                ach[--ofDigit] = HEX[nOffset & 0x0F];
                nOffset >>= 4;
                }

            // formatting
            int ofFmt = ofLine + cDigits;
            ach[ofFmt++] = ':';
            ach[ofFmt++] = ' ';
            ach[ofFmt] = ' ';

            // format data
            int ofHex = ofLine + ofHexInLine;
            int ofChar = ofLine + ofCharInLine;
            for (int i = 0; i < cBytesPerLine; ++i)
                {
                try
                    {
                    int n = ab[ofByte++] & 0xFF;

                    ach[ofHex++] = HEX[(n & 0xF0) >> 4];
                    ach[ofHex++] = HEX[(n & 0x0F)];
                    ach[ofHex++] = ' ';
                    ach[ofChar++] = (n < 32 ? '.' : (char) n);
                    }
                catch (ArrayIndexOutOfBoundsException e)
                    {
                    ach[ofHex++] = ' ';
                    ach[ofHex++] = ' ';
                    ach[ofHex++] = ' ';
                    ach[ofChar++] = ' ';
                    }
                }

            // spacing and newline
            ach[ofHex] = ' ';
            ach[ofChar] = '\n';

            ofLine += cCharsPerLine;
            }

        return new String(ach, 0, cch - 1);
        }

    /**
     * Parse the passed String of hexadecimal characters into a binary
     * value.  This implementation allows the passed String to be prefixed
     * with "0x".
     *
     * @param s the hex String to evaluate
     * @return the byte array value of the passed hex String
     */
    public static byte[] parseHex(String s)
        {
        char[] ach = s.toCharArray();
        int cch = ach.length;
        if (cch == 0)
            {
            return new byte[0];
            }

        if ((cch & 0x1) != 0)
            {
            throw new IllegalArgumentException("invalid length hex string");
            }

        int ofch = 0;
        if (ach[1] == 'x' || ach[1] == 'X')
            {
            ofch = 2;
            }

        int cb = (cch - ofch) / 2;
        byte[] ab = new byte[cb];
        for (int ofb = 0; ofb < cb; ++ofb)
            {
            ab[ofb] = (byte) (parseHex(ach[ofch++]) << 4 | parseHex(ach[ofch++]));
            }

        return ab;
        }

    /**
     * Return the integer value of a hexadecimal digit.
     *
     * @param ch the hex character to evaluate
     * @return the integer value of the passed hex character
     */
    public static int parseHex(char ch)
        {
        switch (ch)
            {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return ch - '0';

            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                return ch - 'A' + 0x0A;

            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
                return ch - 'a' + 0x0A;

            default:
                throw new IllegalArgumentException("illegal hex char: " + ch);
            }
        }

    // ----- formatting support: decimal values -----------------------------

    /**
     * Returns true if the passed character is a decimal digit.
     *
     * @param ch The character to check
     */
    public static boolean isDecimal(char ch)
        {
        return (ch >= '0') && (ch <= '9');
        }

    /**
     * Returns the integer value of a decimal digit.
     *
     * @param ch The character to convert
     */
    public static int decimalValue(char ch)
        {
        if ((ch >= '0') && (ch <= '9'))
            {
            return ch - '0';
            }
        else
            {
            throw new IllegalArgumentException("Character \"" + ch
                                                       + "\" is not a valid decimal digit.");
            }
        }

    /**
     * Calculate the number of decimal digits needed to display the passed
     * value.
     *
     * @param n the value
     * @return the number of decimal digits needed to display the value
     */
    public static int getMaxDecDigits(int n)
        {
        int cDigits = 0;
        do
            {
            cDigits += 1;
            n /= 10;
            }
        while (n != 0);

        return cDigits;
        }

    /**
     * Format the passed non-negative integer as a fixed-length decimal string.
     *
     * @param n       the value
     * @param cDigits the length of the resulting decimal string
     * @return the decimal value formatted to the specified length string
     */
    public static String toDecString(int n, int cDigits)
        {
        char[] ach = new char[cDigits];
        while (cDigits > 0)
            {
            ach[--cDigits] = (char) ('0' + n % 10);
            n /= 10;
            }

        return new String(ach);
        }

    /**
     * Return the smallest value that is not less than the first argument and
     * is a multiple of the second argument. Effectively rounds the first
     * argument up to a multiple of the second.
     *
     * @param lMin      the smallest value to return
     * @param lMultiple the return value will be a multiple of this argument
     * @return the smallest multiple of the second argument that is not less
     * than the first
     */
    public static long pad(long lMin, long lMultiple)
        {
        return ((lMin + lMultiple - 1) / lMultiple) * lMultiple;
        }


    // ----- formatting support: octal values -------------------------------

    /**
     * Returns true if the passed character is an octal digit.
     *
     * @param ch The character to check
     */
    public static boolean isOctal(char ch)
        {
        return (ch >= '0') && (ch <= '7');
        }

    /**
     * Returns the integer value of an octal digit.
     *
     * @param ch The character to convert
     */
    public static int octalValue(char ch)
        {
        if ((ch >= '0') && (ch <= '7'))
            {
            return ch - '0';
            }
        else
            {
            throw new IllegalArgumentException("Character \"" + ch + "\" is not a valid octal digit.");
            }
        }


    // ----- formatting support: bandwidth ----------------------------------

    /**
     * Parse the given string representation of a number of bytes per second.
     * The supplied string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[[B|b][P|p][S|s]]?</tt>
     * <p>
     * where the first non-digit (from left to right) indicates the factor
     * with which the preceding decimal value should be multiplied:
     * <ul>
     * <li><tt>K</tt> or <tt>k</tt> (kilo, 2<sup>10</sup>)</li>
     * <li><tt>M</tt> or <tt>m</tt> (mega, 2<sup>20</sup>)</li>
     * <li><tt>G</tt> or <tt>g</tt> (giga, 2<sup>30</sup>)</li>
     * <li><tt>T</tt> or <tt>t</tt> (tera, 2<sup>40</sup>)</li>
     * </ul>
     * <p>
     * If the string value does not contain a factor, a factor of one is
     * assumed.
     * <p>
     * The optional last three characters indicate the unit of measure,
     * <tt>[b][P|p][S|s]</tt> in the case of bits per second and
     * <tt>[B][P|p][S|s]</tt> in the case of bytes per second. If the string
     * value does not contain a unit, a unit of bits per second is assumed.
     *
     * @param s  a string with the format:
     *           <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[[B|b][P|p][S|s]]?</tt>
     *
     * @return the number of bytes per second represented by the given string
     */
    public static long parseBandwidth(String s)
        {
        return parseBandwidth(s, POWER_0);
        }

    /**
     * Parse the given string representation of a number of bytes per second.
     * The supplied string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[[B|b][P|p][S|s]]?</tt>
     * <p>
     * where the first non-digit (from left to right) indicates the factor
     * with which the preceding decimal value should be multiplied:
     * <ul>
     * <li><tt>K</tt> or <tt>k</tt> (kilo, 2<sup>10</sup>)</li>
     * <li><tt>M</tt> or <tt>m</tt> (mega, 2<sup>20</sup>)</li>
     * <li><tt>G</tt> or <tt>g</tt> (giga, 2<sup>30</sup>)</li>
     * <li><tt>T</tt> or <tt>t</tt> (tera, 2<sup>40</sup>)</li>
     * </ul>
     * <p>
     * If the string value does not contain an explicit or implicit factor, a
     * factor calculated by raising 2 to the given default power is used. The
     * default power can be one of:
     * <ul>
     * <li>{@link #POWER_0}</li>
     * <li>{@link #POWER_K}</li>
     * <li>{@link #POWER_M}</li>
     * <li>{@link #POWER_G}</li>
     * <li>{@link #POWER_T}</li>
     * </ul>
     * <p>
     * The optional last three characters indicate the unit of measure,
     * <tt>[b][P|p][S|s]</tt> in the case of bits per second and
     * <tt>[B][P|p][S|s]</tt> in the case of bytes per second. If the string
     * value does not contain a unit, a unit of bits per second is assumed.
     *
     * @param s              a string with the format:
     *                       <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[[B|b][P|p][S|s]]?</tt>
     * @param nDefaultPower  the exponent used to calculate the factor used in
     *                       the conversion if one is not implied by the given
     *                       string
     *
     * @return the number of bytes per second represented by the given string
     */
    public static long parseBandwidth(String s, int nDefaultPower)
        {
        if (s == null)
            {
            throw new IllegalArgumentException("passed String must not be null");
            }

        switch (nDefaultPower)
            {
            case POWER_0:
            case POWER_K:
            case POWER_M:
            case POWER_G:
            case POWER_T:
                break;
            default:
                throw new IllegalArgumentException("illegal default power: "
                                                           + nDefaultPower);
            }

        // remove trailing "[[P|p][S|s]]?"
        int cch = s.length();
        if (cch >= 2)
            {
            char ch = s.charAt(cch - 1);
            if (ch == 'S' || ch == 's')
                {
                ch = s.charAt(cch - 2);
                if (ch == 'P' || ch == 'p')
                    {
                    cch -= 2;
                    }
                else
                    {
                    throw new IllegalArgumentException("invalid bandwidth: \""
                                                               + s + "\" (illegal bandwidth unit)");
                    }
                }
            }

        // remove trailing "[B|b]?" and store it as a factor
        // (default is "bps" i.e. baud)
        int     cBitShift = -3;
        boolean fDefault  = true;
        if (cch >= 1)
            {
            switch (s.charAt(cch - 1))
                {
                case 'B':
                    cBitShift = 0;
                    // no break;
                case 'b':
                    --cch;
                    fDefault = false;
                    break;
                }
            }

        // remove trailing "[K|k|M|m|G|g|T|t]?[B|b]?" and update the factor
        if (cch >= 1)
            {
            switch (s.charAt(--cch))
                {
                case 'K': case 'k':
                    cBitShift += POWER_K;
                    break;

                case 'M': case 'm':
                    cBitShift += POWER_M;
                    break;

                case 'G': case 'g':
                    cBitShift += POWER_G;
                    break;

                case 'T': case 't':
                    cBitShift += POWER_T;
                    break;

                default:
                    if (fDefault)
                        {
                        cBitShift += nDefaultPower;
                        }
                    ++cch; // oops: shouldn't have chopped off the last char
                    break;
                }
            }

        // make sure that the string contains some digits
        if (cch == 0)
            {
            throw new NumberFormatException("passed String (\"" + s
                                                    + "\") must contain a number");
            }

        // extract the digits (decimal form) to assemble the base number
        long    cb       = 0;
        boolean fDecimal = false;
        int     nDivisor = 1;
        for (int of = 0; of < cch; ++of)
            {
            char ch = s.charAt(of);
            switch (ch)
                {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    cb = (cb * 10) + (ch - '0');
                    if (fDecimal)
                        {
                        nDivisor *= 10;
                        }
                    break;

                case '.':
                    if (fDecimal)
                        {
                        throw new NumberFormatException("invalid bandwidth: \""
                                                                + s + "\" (illegal second decimal point)");
                        }
                    fDecimal = true;
                    break;

                default:
                    throw new NumberFormatException("invalid bandwidth: \""
                                                            + s + "\" (illegal digit: \"" + ch + "\")");
                }
            }

        if (cBitShift < 0)
            {
            cb >>>= -cBitShift;
            }
        else
            {
            cb <<= cBitShift;
            }

        if (fDecimal)
            {
            if (nDivisor == 1)
                {
                throw new NumberFormatException("invalid bandwidth: \""
                                                        + s + "\" (illegal trailing decimal point)");
                }
            else
                {
                cb /= nDivisor;
                }
            }

        return cb;
        }

    /**
     * Format the passed bandwidth (in bytes per second) as a String that can
     * be parsed by {@link #parseBandwidth} such that
     * <tt>cb==parseBandwidth(toBandwidthString(cb))</tt> holds true for
     * all legal values of <tt>cbps</tt>.
     *
     * @param cbps  the number of bytes per second
     *
     * @return a String representation of the given bandwidth
     */
    public static String toBandwidthString(long cbps)
        {
        return toBandwidthString(cbps, true);
        }

    /**
     * Format the passed bandwidth (in bytes per second) as a String. This
     * method will possibly round the memory size for purposes of producing a
     * more-easily read String value unless the <tt>fExact</tt> parameters is
     * passed as true; if <tt>fExact</tt> is true, then
     * <tt>cb==parseBandwidth(toBandwidthString(cb, true))</tt> holds true
     * for all legal values of <tt>cbps</tt>.
     *
     * @param cbps    the number of bytes per second
     * @param fExact  true if the String representation must be exact, or
     *                false if it can be an approximation
     *
     * @return a String representation of the given bandwidth
     */
    public static String toBandwidthString(long cbps, boolean fExact)
        {
        boolean fBits = (cbps & 0xF00000000000000L) == 0L;

        if (fBits)
            {
            cbps <<= 3;
            }

        StringBuilder sb     = new StringBuilder(toMemorySizeString(cbps, fExact));
        int          ofLast = sb.length() - 1;
        if (sb.charAt(ofLast) == 'B')
            {
            if (fBits)
                {
                sb.setCharAt(ofLast, 'b');
                }
            }
        else
            {
            sb.append(fBits ? 'b' : 'B');
            }
        sb.append("ps");

        return sb.toString();
        }


    // ----- formatting support: memory size --------------------------------

    /**
     * Parse the given string representation of a number of bytes. The
     * supplied string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[B|b]?</tt>
     * <p>
     * where the first non-digit (from left to right) indicates the factor
     * with which the preceding decimal value should be multiplied:
     * <ul>
     * <li><tt>K</tt> or <tt>k</tt> (kilo, 2<sup>10</sup>)</li>
     * <li><tt>M</tt> or <tt>m</tt> (mega, 2<sup>20</sup>)</li>
     * <li><tt>G</tt> or <tt>g</tt> (giga, 2<sup>30</sup>)</li>
     * <li><tt>T</tt> or <tt>t</tt> (tera, 2<sup>40</sup>)</li>
     * </ul>
     * <p>
     * If the string value does not contain a factor, a factor of one is
     * assumed.
     * <p>
     * The optional last character <tt>B</tt> or <tt>b</tt> indicates a unit
     * of bytes.
     *
     * @param s  a string with the format
     *           <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[B|b]?</tt>
     *
     * @return the number of bytes represented by the given string
     */
    public static long parseMemorySize(String s)
        {
        return parseMemorySize(s, POWER_0);
        }

    /**
     * Parse the given string representation of a number of bytes. The
     * supplied string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[B|b]?</tt>
     * <p>
     * where the first non-digit (from left to right) indicates the factor
     * with which the preceding decimal value should be multiplied:
     * <ul>
     * <li><tt>K</tt> or <tt>k</tt> (kilo, 2<sup>10</sup>)</li>
     * <li><tt>M</tt> or <tt>m</tt> (mega, 2<sup>20</sup>)</li>
     * <li><tt>G</tt> or <tt>g</tt> (giga, 2<sup>30</sup>)</li>
     * <li><tt>T</tt> or <tt>t</tt> (tera, 2<sup>40</sup>)</li>
     * </ul>
     * <p>
     * If the string value does not contain an explicit or implicit factor, a
     * factor calculated by raising 2 to the given default power is used. The
     * default power can be one of:
     * <ul>
     * <li>{@link #POWER_0}</li>
     * <li>{@link #POWER_K}</li>
     * <li>{@link #POWER_M}</li>
     * <li>{@link #POWER_G}</li>
     * <li>{@link #POWER_T}</li>
     * </ul>
     * <p>
     * The optional last character <tt>B</tt> or <tt>b</tt> indicates a unit
     * of bytes.
     *
     * @param s              a string with the format
     *                       <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[B|b]?</tt>
     * @param nDefaultPower  the exponent used to calculate the factor used in
     *                       the conversion if one is not implied by the given
     *                       string
     *
     * @return the number of bytes represented by the given string
     */
    public static long parseMemorySize(String s, int nDefaultPower)
        {
        if (s == null)
            {
            throw new IllegalArgumentException("passed String must not be null");
            }

        switch (nDefaultPower)
            {
            case POWER_0:
            case POWER_K:
            case POWER_M:
            case POWER_G:
            case POWER_T:
                break;
            default:
                throw new IllegalArgumentException("illegal default power: "
                                                           + nDefaultPower);
            }

        // remove trailing "[K|k|M|m|G|g|T|t]?[B|b]?" and store it as a factor
        int cBitShift = POWER_0;
        int cch       = s.length();
        if (cch > 0)
            {
            boolean fDefault;
            char    ch = s.charAt(cch - 1);
            if (ch == 'B' || ch == 'b')
                {
                // bytes are implicit
                --cch;
                fDefault = false;
                }
            else
                {
                fDefault = true;
                }

            if (cch > 0)
                {
                switch (s.charAt(--cch))
                    {
                    case 'K': case 'k':
                        cBitShift = POWER_K;
                        break;

                    case 'M': case 'm':
                        cBitShift = POWER_M;
                        break;

                    case 'G': case 'g':
                        cBitShift = POWER_G;
                        break;

                    case 'T': case 't':
                        cBitShift = POWER_T;
                        break;

                    default:
                        if (fDefault)
                            {
                            cBitShift = nDefaultPower;
                            }
                        ++cch; // oops: shouldn't have chopped off the last char
                        break;
                    }
                }
            }

        // make sure that the string contains some digits
        if (cch == 0)
            {
            throw new NumberFormatException("passed String (\"" + s
                                                    + "\") must contain a number");
            }

        // extract the digits (decimal form) to assemble the base number
        long    cb       = 0;
        boolean fDecimal = false;
        int     nDivisor = 1;
        for (int of = 0; of < cch; ++of)
            {
            char ch = s.charAt(of);
            switch (ch)
                {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    cb = (cb * 10) + (ch - '0');
                    if (fDecimal)
                        {
                        nDivisor *= 10;
                        }
                    break;

                case '.':
                    if (fDecimal)
                        {
                        throw new NumberFormatException("invalid memory size: \""
                                                                + s + "\" (illegal second decimal point)");
                        }
                    fDecimal = true;
                    break;

                default:
                    throw new NumberFormatException("invalid memory size: \""
                                                            + s + "\" (illegal digit: \"" + ch + "\")");
                }
            }

        cb <<= cBitShift;
        if (fDecimal)
            {
            if (nDivisor == 1)
                {
                throw new NumberFormatException("invalid memory size: \""
                                                        + s + "\" (illegal trailing decimal point)");
                }
            else
                {
                cb /= nDivisor;
                }
            }
        return cb;
        }

    /**
     * Format the passed memory size (in bytes) as a String that can be
     * parsed by {@link #parseMemorySize} such that
     * <tt>cb==parseMemorySize(toMemorySizeString(cb))</tt> holds true for
     * all legal values of <tt>cb</tt>.
     *
     * @param cb  the number of bytes of memory
     *
     * @return a String representation of the given memory size
     */
    public static String toMemorySizeString(long cb)
        {
        return toMemorySizeString(cb, true);
        }

    /**
     * Format the passed memory size (in bytes) as a String. This method will
     * possibly round the memory size for purposes of producing a more-easily
     * read String value unless the <tt>fExact</tt> parameters is passed as
     * true; if <tt>fExact</tt> is true, then
     * <tt>cb==parseMemorySize(toMemorySizeString(cb, true))</tt> holds true
     * for all legal values of <tt>cb</tt>.
     *
     * @param cb      the number of bytes of memory
     * @param fExact  true if the String representation must be exact, or
     *                false if it can be an approximation
     *
     * @return a String representation of the given memory size
     */
    public static String toMemorySizeString(long cb, boolean fExact)
        {
        if (cb < 0)
            {
            throw new IllegalArgumentException("negative quantity: " + cb);
            }

        if (cb < 1024)
            {
            return String.valueOf(cb);
            }

        int cDivs    = 0;
        int cMaxDivs = MEM_SUFFIX.length - 1;

        if (fExact)
            {
            // kilobytes? megabytes? gigabytes? terabytes?
            while (((((int) cb) & KB_MASK) == 0) && cDivs < cMaxDivs)
                {
                cb >>>= 10;
                ++cDivs;
                }
            return cb + MEM_SUFFIX[cDivs];
            }

        // need roughly the 3 most significant decimal digits
        int cbRem = 0;
        while (cb >= KB && cDivs < cMaxDivs)
            {
            cbRem = ((int) cb) & KB_MASK;
            cb >>>= 10;
            ++cDivs;
            }

        StringBuilder sb = new StringBuilder();
        sb.append(cb);
        int cch = sb.length();
        if (cch < 3 && cbRem != 0)
            {
            // need the first digit or two of string value of cbRem / 1024;
            // format the most significant two digits ".xx" as a string "1xx"
            String sDec = String.valueOf((int) (cbRem / 10.24 + 100));
            sb.append('.')
                    .append(sDec, 1, 1 + 3 - cch);
            }
        sb.append(MEM_SUFFIX[cDivs]);

        return sb.toString();
        }

    // ----- formatting support: time ---------------------------------------

    /**
    * Parse the given string representation of a time duration and return its
    * value as a number of milliseconds. The supplied string must be in the
    * format:
    * <p>
    * <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
    * <p>
    * where the first non-digits (from left to right) indicate the unit of
    * time duration:
    * <ul>
    * <li><tt>NS</tt> or <tt>ns</tt> (nanoseconds)</li>
    * <li><tt>US</tt> or <tt>us</tt> (microseconds)</li>
    * <li><tt>MS</tt> or <tt>ms</tt> (milliseconds)</li>
    * <li><tt>S</tt>  or <tt>s</tt>  (seconds)</li>
    * <li><tt>M</tt>  or <tt>m</tt>  (minutes)</li>
    * <li><tt>H</tt>  or <tt>h</tt>  (hours)</li>
    * <li><tt>D</tt>  or <tt>d</tt>  (days)</li>
    * </ul>
    * <p>
    * If the string value does not contain a unit, a unit of milliseconds is
    * assumed.
    *
    * @param s  a string with the format
    *           <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
    *
    * @return the number of milliseconds represented by the given string
    *         rounded down to the nearest millisecond
    *
    * @see #parseTimeNanos(String)
    */
    public static long parseTime(String s)
        {
        return parseTime(s, UNIT_MS);
        }

    /**
    * Parse the given string representation of a time duration and return its
    * value as a number of milliseconds. The supplied string must be in the
    * format:
    * <p>
    * <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
    * <p>
    * where the first non-digits (from left to right) indicate the unit of
    * time duration:
    * <ul>
    * <li><tt>NS</tt> or <tt>ns</tt> (nanoseconds)</li>
    * <li><tt>US</tt> or <tt>us</tt> (microseconds)</li>
    * <li><tt>MS</tt> or <tt>ms</tt> (milliseconds)</li>
    * <li><tt>S</tt>  or <tt>s</tt>  (seconds)</li>
    * <li><tt>M</tt>  or <tt>m</tt>  (minutes)</li>
    * <li><tt>H</tt>  or <tt>h</tt>  (hours)</li>
    * <li><tt>D</tt>  or <tt>d</tt>  (days)</li>
    * </ul>
    * <p>
    * If the string value does not contain a unit, the specified default unit
    * is assumed. The default unit can be one of:
    * <ul>
    * <li>{@link Base#UNIT_NS}</li>
    * <li>{@link Base#UNIT_US}</li>
    * <li>{@link Base#UNIT_MS}</li>
    * <li>{@link Base#UNIT_S}</li>
    * <li>{@link Base#UNIT_M}</li>
    * <li>{@link Base#UNIT_H}</li>
    * <li>{@link Base#UNIT_D}</li>
    * </ul>
    *
    * @param s             a string with the format
    *                      <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
    * @param nDefaultUnit  the unit to use in the conversion to milliseconds
    *                      if one is not specified in the supplied string
    *
    * @return the number of milliseconds represented by the given string
    *         rounded down to the nearest millisecond
    *
    * @see #parseTimeNanos(String, int)
    */
    public static long parseTime(String s, int nDefaultUnit)
        {
        return parseTimeNanos(s, nDefaultUnit) / 1000000L;
        }

    /**
    * Parse the given string representation of a time duration and return its
    * value as a number of nanoseconds. The supplied string must be in the
    * format:
    * <p>
    * <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
    * <p>
    * where the first non-digits (from left to right) indicate the unit of
    * time duration:
    * <ul>
    * <li><tt>NS</tt> or <tt>ns</tt> (nanoseconds)</li>
    * <li><tt>US</tt> or <tt>us</tt> (microseconds)</li>
    * <li><tt>MS</tt> or <tt>ms</tt> (milliseconds)</li>
    * <li><tt>S</tt>  or <tt>s</tt>  (seconds)</li>
    * <li><tt>M</tt>  or <tt>m</tt>  (minutes)</li>
    * <li><tt>H</tt>  or <tt>h</tt>  (hours)</li>
    * <li><tt>D</tt>  or <tt>d</tt>  (days)</li>
    * </ul>
    * <p>
    * If the string value does not contain a unit, a unit of nanoseconds is
    * assumed.
    *
    * @param s  a string with the format
    *           <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
    *
    * @return the number of nanoseconds represented by the given string
    *         rounded down to the nearest nanosecond
    */
    public static long parseTimeNanos(String s)
        {
        return parseTimeNanos(s, UNIT_NS);
        }

    /**
    * Parse the given string representation of a time duration and return its
    * value as a number of nanoseconds. The supplied string must be in the
    * format:
    * <p>
    * <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
    * <p>
    * where the first non-digits (from left to right) indicate the unit of
    * time duration:
    * <ul>
    * <li><tt>NS</tt> or <tt>ns</tt> (nanoseconds)</li>
    * <li><tt>US</tt> or <tt>us</tt> (microseconds)</li>
    * <li><tt>MS</tt> or <tt>ms</tt> (milliseconds)</li>
    * <li><tt>S</tt>  or <tt>s</tt>  (seconds)</li>
    * <li><tt>M</tt>  or <tt>m</tt>  (minutes)</li>
    * <li><tt>H</tt>  or <tt>h</tt>  (hours)</li>
    * <li><tt>D</tt>  or <tt>d</tt>  (days)</li>
    * </ul>
    * <p>
    * If the string value does not contain a unit, the specified default unit
    * is assumed. The default unit can be one of:
    * <ul>
    * <li>{@link Base#UNIT_NS}</li>
    * <li>{@link Base#UNIT_US}</li>
    * <li>{@link Base#UNIT_MS}</li>
    * <li>{@link Base#UNIT_S}</li>
    * <li>{@link Base#UNIT_M}</li>
    * <li>{@link Base#UNIT_H}</li>
    * <li>{@link Base#UNIT_D}</li>
    * </ul>
    *
    * @param s             a string with the format
    *                      <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
    * @param nDefaultUnit  the unit to use in the conversion to nanoseconds
    *                      if one is not specified in the supplied string
    *
    * @return the number of nanoseconds represented by the given string
    *         rounded down to the nearest nanosecond
    */
    public static long parseTimeNanos(String s, int nDefaultUnit)
        {
        if (s == null)
            {
            throw new IllegalArgumentException("passed String must not be null");
            }

        switch (nDefaultUnit)
            {
            case UNIT_NS:
            case UNIT_US:
            case UNIT_MS:
            case UNIT_S:
            case UNIT_M:
            case UNIT_H:
            case UNIT_D:
                break;
            default:
                throw new IllegalArgumentException("illegal default unit: "
                        + nDefaultUnit);
            }

        // remove trailing "[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?" and store it as a factor
        long nMultiplier = nDefaultUnit;
        int cch = s.length();
        if (cch > 0)
            {
            switch (s.charAt(--cch))
                {
                case 'S': case 's':
                    nMultiplier = UNIT_S;
                    if (cch > 1)
                        {
                        char c = s.charAt(cch - 1);
                        switch (c)
                            {
                            case 'N': case 'n':
                                --cch;
                                nMultiplier = UNIT_NS;
                                break;
                            case 'U': case 'u':
                                --cch;
                                nMultiplier = UNIT_US;
                                break;
                            case 'M': case 'm':
                                --cch;
                                nMultiplier = UNIT_MS;
                                break;
                            }
                        }
                    break;

                case 'M': case 'm':
                    nMultiplier = UNIT_M;
                    break;

                case 'H': case 'h':
                    nMultiplier = UNIT_H;
                    break;

                case 'D': case 'd':
                    nMultiplier = UNIT_D;
                    break;

                default:
                    ++cch; // oops: shouldn't have chopped off the last char
                    break;
                }
            }

        // convert multiplier into nanos
        nMultiplier = nMultiplier < 0 ? 1000000L / -nMultiplier
                                      : 1000000L * nMultiplier;

        // make sure that the string contains some digits
        if (cch == 0)
            {
            throw new NumberFormatException("passed String (\"" + s
                    + "\") must contain a number");
            }

        // extract the digits (decimal form) to assemble the base number
        long    cNanos   = 0;
        boolean fDecimal = false;
        int     nDivisor = 1;
        for (int of = 0; of < cch; ++of)
            {
            char ch = s.charAt(of);
            switch (ch)
                {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    cNanos = (cNanos * 10) + (ch - '0');
                    if (fDecimal)
                        {
                        nDivisor *= 10;
                        }
                    break;

                case '.':
                    if (fDecimal)
                        {
                        throw new NumberFormatException("invalid time: \""
                            + s + "\" (illegal second decimal point)");
                        }
                    fDecimal = true;
                    break;

                default:
                    throw new NumberFormatException("invalid time: \""
                            + s + "\" (illegal digit: \"" + ch + "\")");
                }
            }

        cNanos *= nMultiplier;
        if (fDecimal)
            {
            if (nDivisor == 1)
                {
                throw new NumberFormatException("invalid time: \""
                    + s + "\" (illegal trailing decimal point)");
                }
            else
                {
                cNanos /= nDivisor;
                }
            }
        return cNanos;
        }

    /**
    * Format a long value into a human readable date/time string.
    *
    * @param ldt  a Java long containing a date/time value
    *
    * @return a human readable date/time string
    */
    public static String formatDateTime(long ldt)
        {
        return ldt == 0L ? "none" : new Timestamp(ldt).toString();
        }


    // ----- formatting support: percentage ---------------------------------

    /**
    * Parse the given string representation of a percentage value and return
    * its value as a float in the inclusive range of 0.0 and 1.0. The supplied
    * string must be in the format:
    * <p>
    * <tt>[\d]+[[.][\d]+]?[%]</tt>
    * <p>
    * where the digits are within the closed interval [0.0, 100.0].
    *
    * @param s  a string with the format <tt>[\d]+[[.][\d]+]?[%]</tt>
    *
    * @return a float representing the percentage value in the closed interval
    *         [0.0, 1.0]
    */
    public static float parsePercentage(String s)
        {
        int ofPct = s.indexOf('%');
        if (ofPct == -1)
            {
            throw new IllegalArgumentException("The parameter " + s + " does not contain a percentage value.");
            }
        int percent = Integer.parseInt(s.substring(0, ofPct));
        if (percent > 100 || percent < 0)
            {
            throw new IllegalArgumentException("Not a percentage value between 0 - 100:" + s);
            }
        return percent / 100f;
        }

    // ----- CRC32 ----------------------------------------------------------

    /**
     * Calculate a CRC32 value from a byte array.
     *
     * @param ab  an array of bytes
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(byte[] ab)
        {
        return toCrc(ab, 0, ab.length);
        }

    /**
     * Calculate a CRC32 value from a portion of a byte array.
     *
     * @param ab  an array of bytes
     * @param of  the offset into the array
     * @param cb  the number of bytes to evaluate
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(byte[] ab, int of, int cb)
        {
        return toCrc(ab, of, cb, 0xFFFFFFFF);
        }

    /**
     * Continue to calculate a CRC32 value from a portion of a byte array.
     *
     * @param ab    an array of bytes
     * @param of    the offset into the array
     * @param cb    the number of bytes to evaluate
     * @param nCrc  the previous CRC value
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(byte[] ab, int of, int cb, int nCrc)
        {
        while (cb > 0)
            {
            nCrc = (nCrc >>> 8) ^ CRC32_TABLE[(nCrc ^ ab[of++]) & 0xFF];
            --cb;
            }
        return nCrc;
        }

    /**
     * Calculate a CRC32 value from a ByteSequence.
     *
     * @param seq  a ByteSequence
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(ByteSequence seq)
        {
        return toCrc(seq, 0, seq.length(), 0xFFFFFFFF);
        }

    /**
     * Continue to calculate a CRC32 value from a portion of a ByteSequence .
     *
     * @param seq   a ByteSequence
     * @param of    the offset into the ByteSequence
     * @param cb    the number of bytes to evaluate
     * @param nCrc  the previous CRC value
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(ByteSequence seq, int of, int cb, int nCrc)
        {
        while (cb > 0)
            {
            nCrc = (nCrc >>> 8) ^ CRC32_TABLE[(nCrc ^ seq.byteAt(of++)) & 0xFF];
            --cb;
            }
        return nCrc;
        }


    // ----- constants ------------------------------------------------------

    /**
     * Hex digits.
     */
    public static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * Memory size constants.
     */
    private static final int      KB         = 1 << 10;
    private static final int      KB_MASK    = KB - 1;
    private static final String[] MEM_SUFFIX = {"", "KB", "MB", "GB", "TB"};

    /**
     * CRC32 constants.
     */
    private static final int   CRC32_BASE  = 0xEDB88320;
    public static final int[] CRC32_TABLE = new int[256];
    static
        {
        for (int i = 0, c = CRC32_TABLE.length; i < c; ++i)
            {
            int nCrc = i;
            for (int n = 0; n < 8; ++n)
                {
                if ((nCrc & 1) == 1)
                    {
                    nCrc = (nCrc >>> 1) ^ CRC32_BASE;
                    }
                else
                    {
                    nCrc >>>= 1;
                    }
                }
            CRC32_TABLE[i] = nCrc;
            }
        }

    /**
     * Integer constant representing an exponent of zero.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_0 = 0;

    /**
     * Integer constant representing an exponent of 10.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_K = 10;

    /**
     * Integer constant representing an exponent of 20.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_M = 20;

    /**
     * Integer constant representing an exponent of 30.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_G = 30;

    /**
     * Integer constant representing an exponent of 40.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_T = 40;

    }
