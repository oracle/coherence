/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;


import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.config.xml.preprocessor.SystemPropertyPreprocessor;
import com.tangosol.io.Base64InputStream;
import com.tangosol.io.Base64OutputStream;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.Resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import java.math.BigDecimal;

import java.net.URL;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.XMLConstants;


/**
* This abstract class contains XML manipulation methods.
*
* @author cp/gg  2000.10.25
*/
public abstract class XmlHelper extends Base
    {
    // ----- Xml loading helpers --------------------------------------------

    /**
    * Load XML from a file for a class.
    *
    * @param clz  the class for which to load the XML
    *
    * @return the XML content or null
    */
    public static XmlDocument loadXml(Class clz)
        {
        return loadXml(clz, null);
        }

    /**
    * Load XML from a file for a class using a given charset.
    *
    * @param clz  the class for which to load the XML
    * @param sCharset  the charset name; pass null to use the default charset
    *
    * @return the XML content or null
    */
    public static XmlDocument loadXml(Class clz, String sCharset)
        {
        String sName = clz.getName();
        sName = sName.substring(sName.lastIndexOf('.') + 1);
        return loadXml(clz, sName + ".xml", sCharset);
        }

    /**
    * Load XML from a file that is collocated with the specified class with a
    * given charset.
    *
    * @param clz       the class for which to load the XML
    * @param sName     the XML file name (including extension if any)
    *                   that exists in the package from which the class
    *                   was loaded
    * @param sCharset  the charset name; pass null to use the default charset
    *
    * @return the XML content or null
    */
    public static XmlDocument loadXml(Class clz, String sName, String sCharset)
        {
        InputStream stream = clz.getResourceAsStream(sName);
        if (stream == null)
            {
            return null;
            }

        try
            {
            return sCharset == null ? loadXml(stream) : loadXml(stream, sCharset);
            }
        finally
            {
            try
                {
                stream.close();
                }
            catch (IOException eIgnore) {}
            }
        }

    /**
    * Load XML from a stream.
    *
    * @param stream  the InputStream object
    *
    * @return the XML content
    */
    public static XmlDocument loadXml(InputStream stream)
        {
        try
            {
            return new SimpleParser().parseXml(stream);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Load XML from a stream using the specified charset.
    *
    * @param stream    the InputStream object
    * @param sCharset  the charset name
    *
    * @return the XML content
    */
    public static XmlDocument loadXml(InputStream stream, String sCharset)
        {
        try
            {
            return new SimpleParser().parseXml(stream, sCharset);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Load the XML from the specified url using the default character set.
    *
    * @param url  the url from which to load the XML
    *
    * @return the XML content
    */
    public static XmlDocument loadXml(URL url)
        {
        try
            {
            return loadXml(url.openStream());
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Load the XML from the specified url using the specified character set.
    *
    * @param url       the url from which to load the XML
    * @param sCharset  the charset name
    *
    * @return the XML content
    */
    public static XmlDocument loadXml(URL url, String sCharset)
        {
        try
            {
            return loadXml(url.openStream(), sCharset);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Load XML from a string.
    *
    * @param sXml  the string containing an XML
    *
    * @return the XML content
    */
    public static XmlDocument loadXml(String sXml)
        {
        try
            {
            return new SimpleParser().parseXml(sXml);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Load XML from a String into the specified XmlElement.
    *
    * @param sXml     the string containing an XML
    * @param xmlRoot  the root XmlElement to parse the string into
    */
    public static void loadXml(String sXml, XmlDocument xmlRoot)
        {
        loadXml(sXml, xmlRoot, true);
        }

    /**
    * Load XML from a String into the specified XmlElement.
    *
    * @param sXml       the string containing an XML
    * @param xmlRoot    the root XmlElement to parse the string into
    * @param fValidate  whether to validate the loaded XML
    *
    * @since 14.1.1.0
*/
    public static void loadXml(String sXml, XmlDocument xmlRoot, boolean fValidate)
        {
        try
            {
            new SimpleParser(fValidate).parseXml(sXml, xmlRoot);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Load the configuration from a resource.
    *
    * @param sName   the name of the resource
    * @param sDescr  a description of the resource being loaded (e.g.
    *                "cache configuration").  The description is only used in
    *                logging and error messages related to loading the resource
    *
    * @return the configuration XML
    */
    public static XmlDocument loadResource(String sName, String sDescr)
        {
        return loadResourceInternal(sName, sDescr, /*loader*/null, /*fFile*/false,
                /*fWarnNoSchema*/true);
        }

    /**
    * Load the configuration from resource.
    *
    * @param sName   the name of the resource
    * @param sDescr  a description of the resource being loaded (e.g.
    *                "cache configuration").  The description is only used in
    *                logging and error messages related to loading the resource
    * @param loader  (optional) ClassLoader that should be used to load the
    *                configuration resource
    *
    * @return the configuration XML
    */
    public static XmlDocument loadResource(String sName, String sDescr, ClassLoader loader)
        {
        return loadResourceInternal(sName, sDescr, loader, /*fFile*/false,
                /*fWarnNoSchema*/true);
        }

    /**
     * Load the configuration from a URL resource.
     *
     * @param url     the resource URL
     * @param sDescr  a description of the resource being loaded (e.g.
     *                "cache configuration").  The description is only used in
     *                logging and error messages related to loading the resource
     * @param loader  (optional) ClassLoader that should be used to load the
     *                configuration resource
     *
     * @return the configuration XML
     */
     public static XmlDocument loadResource(URL url, String sDescr, ClassLoader loader)
         {
         return loadResourceInternal(url, sDescr, /*fWarnNoSchema*/true);
         }

    /**
    * Load the configuration from a file or resource.
    *
    * @param sName   the name of the file or resource
    * @param sDescr  a description of the resource being loaded (e.g.
    *                "cache configuration").  The description is only used in
    *                logging and error messages related to loading the resource
    *
    * @return the configuration XML
    */
    public static XmlDocument loadFileOrResource(String sName, String sDescr)
        {
        return loadResourceInternal(sName, sDescr, /*loader*/null, /*fFile*/true,
                /*fWarnNoSchema*/true);
        }

    /**
     * Load the configuration from a file or resource.
     *
     * @param sName          the name of the file or resource
     * @param sDescr         a description of the resource being loaded (e.g.
     *                       "cache configuration").  The description is only used in
     *                       logging and error messages related to loading the resource
     * @param loader         (optional) ClassLoader that should be used to load the
     *                       configuration resource
     *
     * @return the configuration XML
     */
     public static XmlDocument loadFileOrResource(String sName, String sDescr,
             ClassLoader loader)
         {
         return loadResourceInternal(sName, sDescr, loader, /*fFile*/true,
                 /*fWarnNoSchema*/true);
         }

    /**
    * Load the configuration from a file or resource.
    *
    * @param sName          the name of the file or resource
    * @param sDescr         a description of the resource being loaded (e.g.
    *                       "cache configuration").  The description is only used in
    *                       logging and error messages related to loading the resource
    * @param loader         (optional) ClassLoader that should be used to load the
    *                       configuration resource
    * @param fWarnNoSchema  display warning if schema is missing
    *
    * @return the configuration XML
    */
    public static XmlDocument loadFileOrResource(String sName, String sDescr,
            ClassLoader loader, boolean fWarnNoSchema)
        {
        return loadResourceInternal(sName, sDescr, loader, /*fFile*/true, fWarnNoSchema);
        }

    /**
    * Load the configuration from a file or resource.
    *
    * @param sName          the name of the file or resource
    * @param sDescr         a description of the resource being loaded (e.g.
    *                       "cache configuration").  The description is only used in
    *                       logging and error messages related to loading the resource
    * @param loader         (optional) ClassLoader that should be used to load the
    *                       configuration resource
    * @param fFile          true if the specified name could refer to a file
    * @param fWarnNoSchema  display warning if schema is missing
    *
    * @return the configuration XML
    */
    protected static XmlDocument loadResourceInternal(
        String sName, String sDescr, ClassLoader loader, boolean fFile,
            boolean fWarnNoSchema)
        {
        // default to something meaningful and generic
        sDescr = sDescr == null ? "configuration" : sDescr;

        URL url = fFile ? Resources.findFileOrResource(sName, loader) :
                Resources.findResource(sName, loader);

        if (url == null)
            {
            throw new RuntimeException("The " + sDescr + " is missing: \""
                    + sName + "\", loader=" + loader);
            }

        return loadResourceInternal(url, sDescr, fWarnNoSchema);
        }

    /**
    * Load the configuration from a URL.
    * <p>
    * Note: The default character set is used to load configurations provided in
    *       files; otherwise the character set "ISO-8859-1" is used.
    *
    * @param url            the file or resource URL
    * @param sDescr         a description of the resource being loaded (e.g.
    *                       "cache configuration").  The description is only used in
    *                       logging and error messages related to loading the resource
    * @param fWarnNoSchema  display warning if schema is missing
    *
    * @return the configuration XML
    */
    protected static XmlDocument loadResourceInternal(URL url, String sDescr,
            boolean fWarnNoSchema)
        {
        // default to something meaningful and generic
        sDescr = sDescr == null ? "configuration" : sDescr;

        if (url == null)
            {
            throw new IllegalArgumentException("The " + sDescr + " URL cannot be null");
            }

        try
            {
            XmlDocument xml = url.getProtocol().equals("file") ?
                XmlHelper.loadXml(url) :
                XmlHelper.loadXml(url, "ISO-8859-1");

            StringBuilder sb = new StringBuilder(256);
            sb.append("Loaded ").append(sDescr)
              .append(" from \"").append(url).append('"');

            if (fWarnNoSchema && getSchemaLocations(xml, null).isEmpty())
                {
                sb.append("; this document does not refer to any schema " +
                          "definition and has not been validated.");
                }
            CacheFactory.log(sb.toString(), CacheFactory.LOG_INFO);

            return xml;
            }
        catch (RuntimeException e)
            {
            throw ensureRuntimeException(e,
                    "Failed to load " + sDescr +": " + url);
            }
        }


    // ----- formatting helpers ---------------------------------------------

    /**
    * Validate the passed encoding.  Encodings are latin strings defined as:
    *
    *   [A-Za-z] ([A-Za-z0-9._] | '-')*
    *
    * @param sEncoding  the document encoding
    *
    * @return true if the encoding is valid, false otherwise
    */
    public static boolean isEncodingValid(String sEncoding)
        {
        if (sEncoding == null)
            {
            return false;
            }

        char[] ach = sEncoding.toCharArray();
        int    cch = ach.length;

        if (cch == 0)
            {
            return false;
            }

        char ch = ach[0];
        if (!(ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z'))
            {
            return false;
            }

        for (int of = 1; of < cch; ++of)
            {
            ch = ach[of];
            switch (ch)
                {
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
                case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
                case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
                case 'Y': case 'Z':

                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
                case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
                case 's': case 't': case 'u': case 'v': case 'w': case 'x':
                case 'y': case 'z':

                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':

                // other legal characters
                case '.':
                case '_':
                case '-':
                    break;

                default:
                    return false;
                }
            }

        return true;
        }

    /**
    * Validate the passed system identifier.
    *
    * @param sName  the system identifier of the XML document
    *
    * @return true if the identifier is valid, false otherwise
    */
    public static boolean isSystemIdentifierValid(String sName)
        {
        return true;
        }

    /**
    * Validate the passed public identifier.
    *
    * @param sName  the public identifier of the XML document
    *
    * @return true if the identifier is valid, false otherwise
    */
    public static boolean isPublicIdentifierValid(String sName)
        {
        // PubidLiteral ::= '"' PubidChar* '"' | "'" (PubidChar - "'")* "'"
        // PubidChar    ::= #x20 | #xD | #xA | [a-zA-Z0-9] | [-'()+,./:=?;!*#@$_%]
        char[] ach = sName.toCharArray();
        int    cch = ach.length;
        for (int of = 0; of < cch; ++of)
            {
            switch (ach[of])
                {
                case 0x20:
                case 0x0D:
                case 0x0A:

                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
                case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
                case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
                case 'Y': case 'Z':

                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
                case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
                case 's': case 't': case 'u': case 'v': case 'w': case 'x':
                case 'y': case 'z':

                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':

                case '-': case '\'':case '(': case ')': case '+':
                case ',': case '.': case '/': case ':': case '=':
                case '?': case ';': case '!': case '*': case '#':
                case '@': case '$': case '_': case '%':
                    break;

                default:
                    return false;
                }
            }

        return true;
        }

    /**
    * Validate the passed comment.  Comments may not contain "--". See the
    * XML specification 1.0 2ed section 2.5.
    *
    * @param sComment  the XML comment
    *
    * @return true if the comment is valid, false otherwise
    */
    public static boolean isCommentValid(String sComment)
        {
        return !sComment.contains("--");
        }

    /**
    * Validate the passed name.  Currently, this does not allow the
    * "CombiningChar" or "Extender" characters that are allowed by the XML
    * specification 1.0 2ed section 2.3 [4].
    *
    * @param sName  the XML name to validate
    *
    * @return true if the name is valid, false otherwise
    */
    public static boolean isNameValid(String sName)
        {
        if (sName == null)
            {
            return false;
            }

        char[] ach = sName.toCharArray();
        int    cch = ach.length;

        if (cch == 0)
            {
            return false;
            }

        char ch = ach[0];
        if (!(Character.isLetter(ch) || ch == '_' || ch == ':'))
            {
            return false;
            }

        for (int of = 1; of < cch; ++of)
            {
            ch = ach[of];
            switch (ch)
                {
                // inline latin uppercase/lowercase letters and digits
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
                case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
                case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
                case 'Y': case 'Z':

                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
                case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
                case 's': case 't': case 'u': case 'v': case 'w': case 'x':
                case 'y': case 'z':

                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':

                // other legal characters
                case '.':
                case '-':
                case '_':
                case ':':
                    break;

                default:
                    if (!(Character.isLetter(ch) || Character.isDigit(ch)))
                        {
                        return false;
                        }
                }
            }

        return true;
        }

    /**
    * Test if the specified character is XML whitespace.
    *
    * @param ch  a character
    *
    * @return true if the passed character is XML whitespace
    */
    public static boolean isWhitespace(char ch)
        {
        switch (ch)
            {
            case 0x09:
            case 0x0A:
            case 0x0D:
            case 0x20:
                return true;
            default:
                return false;
            }
        }

    /**
    * Trim XML whitespace.  See XML 1.0 2ed section 2.3.
    *
    * @param s  the original String
    *
    * @return the passed String minus any leading or trailing whitespace
    */
    public static String trim(String s)
        {
        char[] ach     = s.toCharArray();
        int    cch     = ach.length;
        int    ofStart = 0;
        int    ofEnd   = cch;

        while (ofStart < cch && isWhitespace(ach[ofStart]))
            {
            ++ofStart;
            }

        if (ofStart >= cch)
            {
            return "";
            }

        while (isWhitespace(ach[ofEnd - 1]))
            {
            --ofEnd;
            }

        return ofStart > 0 || ofEnd < cch ? s.substring(ofStart, ofEnd) : s;
        }

    /**
    * Trim leading XML whitespace.  See XML 1.0 2ed section 2.3.
    *
    * @param s  the original String
    *
    * @return the passed String minus any leading whitespace
    */
    public static String trimf(String s)
        {
        char[] ach     = s.toCharArray();
        int    cch     = ach.length;
        int    of      = 0;

        while (of < cch && isWhitespace(ach[of]))
            {
            ++of;
            }

        if (of >= cch)
            {
            return "";
            }

        if (of == 0)
            {
            return s;
            }

        return s.substring(of);
        }

    /**
    * Trim trailing XML whitespace.  See XML 1.0 2ed section 2.3.
    *
    * @param s  the original String
    *
    * @return the passed String minus any trailing whitespace
    */
    public static String trimb(String s)
        {
        char[] ach     = s.toCharArray();
        int    cch     = ach.length;
        int    of      = cch - 1;

        while (of >= 0 && isWhitespace(ach[of]))
            {
            --of;
            }

        if (of < 0)
            {
            return "";
            }

        if (of == cch - 1)
            {
            return s;
            }

        return s.substring(0, of + 1);
        }

    /**
    * Encode an attribute value so that it can be quoted and made part of a
    * valid and well formed XML document.
    *
    * @param sValue   the attribute value to encode
    * @param chQuote  the character that will be used to quote the attribute
    *
    * @return the attribute value in its encoded form (but not quoted)
    */
    public static String encodeAttribute(String sValue, char chQuote)
        {
        azzert(chQuote == '\'' || chQuote == '"', "Invalid quote character");

        char[] ach = sValue.toCharArray();
        int    cch = ach.length;

        // check for an empty attribute value
        if (cch == 0)
            {
            return sValue;
            }

        StringBuffer sb = null;
        int ofPrev = 0;
        for (int of = 0; of < cch; ++of)
            {
            char ch = ach[of];

            switch (ch)
                {
                // escape only the quote that is planned to be used
                case '\'':
                case '"':
                    if (ch != chQuote)
                        {
                        break;
                        }

                // control characters
                case 0x00:  case 0x01: case 0x02: case 0x03:
                case 0x04:  case 0x05: case 0x06: case 0x07:
                case 0x08:  case 0x09: case 0x0A: case 0x0B:
                case 0x0C:  case 0x0D: case 0x0E: case 0x0F:
                case 0x10:  case 0x11: case 0x12: case 0x13:
                case 0x14:  case 0x15: case 0x16: case 0x17:
                case 0x18:  case 0x19: case 0x1A: case 0x1B:
                case 0x1C:  case 0x1D: case 0x1E: case 0x1F:

                // characters that must be escaped
                // @see XML 1.0 2ed section 2.3[10]
                case '<':
                case '>':
                case '&':
                    {
                    if (sb == null)
                        {
                        // pre-allocate enough for several escapes
                        sb = new StringBuffer(cch + 16);
                        }

                    // transfer up to but not including the current offset
                    if (of > ofPrev)
                        {
                        sb.append(ach, ofPrev, of - ofPrev);
                        }

                    switch (ch)
                        {
                        case '>':
                            sb.append("&gt;");
                            break;
                        case '<':
                            sb.append("&lt;");
                            break;
                        case '&':
                            sb.append("&amp;");
                            break;
                        case '\'':
                            sb.append("&apos;");
                            break;
                        case '\"':
                            sb.append("&quot;");
                            break;
                        default:
                            // encode the current character
                            sb.append("&#x");
                            int n = ch;
                            if ((n & 0xF000) != 0)
                                {
                                sb.append(HEX[n >>> 12]);
                                }
                            if ((n & 0xFF00) != 0)
                                {
                                sb.append(HEX[n >>> 8 & 0xF]);
                                }
                            if ((n & 0xFFF0) != 0)
                                {
                                sb.append(HEX[n >>> 4 & 0xF]);
                                }
                            sb.append(HEX[n & 0xF]);
                            sb.append(';');
                            break;
                        }

                    // the next character in the String is now the next
                    // character to transfer/encode
                    ofPrev = of + 1;
                    }
                }
            }

        // there may be a portion of the string left that does not require
        // encoding
        if (sb != null && ofPrev < cch)
            {
            sb.append(ach, ofPrev, cch - ofPrev);
            }

        return sb == null ? sValue : sb.toString();
        }

    /**
    * Decode an attribute value that was quoted.
    *
    * @param sValue   the attribute value to decode
    *
    * @return the attribute value in its decoded form
    */
    public static String decodeAttribute(String sValue)
        {
        if (sValue.indexOf('&') == -1)
            {
            return sValue;
            }

        char[] ach = sValue.toCharArray();
        int    cch = ach.length;

        StringBuilder sb = new StringBuilder(cch);
        int ofPrev = 0;
        for (int of = 0; of < cch; ++of)
            {
            if (ach[of] == '&')
                {
                // scan up to ';'
                int ofSemi = of + 1;
                while (ofSemi < cch && ach[ofSemi] != ';')
                    {
                    ++ofSemi;
                    }
                if (ofSemi >= cch || ofSemi == of + 1)
                    {
                    throw new IllegalArgumentException("The XML attribute ("
                            + sValue + ") contains an unescaped '&'");
                    }

                // transfer up to but not including the current offset
                if (of > ofPrev)
                    {
                    sb.append(ach, ofPrev, of - ofPrev);
                    ofPrev = of;
                    }

                // convert the escaped sequence to a character, ignoring
                // potential entity refs
                if (ach[of+1] == '#')
                    {
                    boolean fHex = (ach[of+2] == 'x');
                    String sEsc = sValue.substring(of + (fHex ? 3 : 2), ofSemi);
                    try
                        {
                        if (sEsc.length() < 1)
                            {
                            throw new IllegalArgumentException("not a number");
                            }

                        int n = Integer.parseInt(sEsc, (fHex ? 16 : 10));
                        if (n < 0 || n > 0xFFFF)
                            {
                            throw new IllegalArgumentException("out of range");
                            }

                        sb.append((char) n);
                        }
                    catch (Exception e)
                        {
                        throw new IllegalArgumentException("The XML attribute ("
                                + sValue + ") contains an illegal escape ("
                                + (fHex ? "hex" : "decimal") + ' ' + sEsc + ')');
                        }
                    }
                else
                    {
                    String sEsc = sValue.substring(of + 1, ofSemi);
                    if (sEsc.equals("amp"))
                        {
                        sb.append('&');
                        }
                    else if (sEsc.equals("apos"))
                        {
                        sb.append('\'');
                        }
                    else if (sEsc.equals("gt"))
                        {
                        sb.append('>');
                        }
                    else if (sEsc.equals("lt"))
                        {
                        sb.append('<');
                        }
                    else if (sEsc.equals("quot"))
                        {
                        sb.append('\"');
                        }
                    else
                        {
                        // assume it is an entity ref etc.
                        continue;
                        }
                    }

                of = ofSemi;
                ofPrev = of + 1;
                }
            }

        if (ofPrev < cch)
            {
            sb.append(ach, ofPrev, cch - ofPrev);
            }

        return sb.toString();
        }

    /**
    * Encode an element's content value so that it can be made part of a
    * valid and well formed XML document.
    *
    * @param sValue  the content value to encode
    * @param fPreferBlockEscape  pass true to use the CDATA escape if two
    *        conditions are met:  that escaping is required, and that the
    *        value does not contain the string "]]&gt;"
    *
    * @return the attribute value in its encoded form (but not quoted)
    */
    public static String encodeContent(String sValue, boolean fPreferBlockEscape)
        {
        char[] ach = sValue.toCharArray();
        int    cch = ach.length;

        // check for an empty attribute value
        if (cch == 0)
            {
            return sValue;
            }

        if (fPreferBlockEscape)
            {
            // scan to see if any escape is necessary, and if so, use CDATA
            // if possible (content must not contain "]]>")
            boolean fUseCdataEscape = true;
            boolean fRequiresEscape = isWhitespace(ach[0]) || isWhitespace(ach[cch-1]);
            for (int of = 0; of < cch; ++of)
                {
                int nch = ach[of];
                switch (nch)
                    {
                    case '<':
                    case '&':
                        fRequiresEscape = true;
                        break;

                    case ']':
                        if (of + 2 < cch && ach[of+1] == ']' && ach[of+2] == '>')
                            {
                            fUseCdataEscape = false;
                            }
                        break;
                    }
                }

            if (!fRequiresEscape)
                {
                return sValue;
                }

            if (fUseCdataEscape)
                {
                return "<![CDATA[" + sValue + "]]>";
                }
            }

        StringBuilder sb = new StringBuilder(cch + 16);

        // encode leading whitespace
        int of = 0;
        leading: while (of < cch)
            {
            switch (ach[of])
                {
                case 0x09:
                    sb.append("&#x09;");
                    break;
                case 0x0A:
                    sb.append("&#x0A;");
                    break;
                case 0x0D:
                    sb.append("&#x0D;");
                    break;
                case 0x20:
                    sb.append("&#x20;");
                    break;
                default:
                    break leading;
                }
            ++of;
            }

        // figure out the extent of trailing whitespace
        int cchNonWhite = cch;
        while (cchNonWhite > of && isWhitespace(ach[cchNonWhite-1]))
            {
            --cchNonWhite;
            }

        // encode portion between leading and trailing whitespace
        int ofPrev = of;
        int cBrackets = 0;
        for (; of < cchNonWhite; ++of)
            {
            char ch = ach[of];

            switch (ch)
                {
                case ']':
                    ++cBrackets;
                    break;

                case '>':
                    if (cBrackets < 2)
                        {
                        cBrackets = 0;
                        break;
                        }

                case '<':
                case '&':
                    {
                    // transfer up to but not including the current offset
                    if (of > ofPrev)
                        {
                        sb.append(ach, ofPrev, of - ofPrev);
                        }

                    // escape the character
                    switch (ch)
                        {
                        case '>':
                            sb.append("&gt;");
                            break;
                        case '<':
                            sb.append("&lt;");
                            break;
                        case '&':
                            sb.append("&amp;");
                            break;
                        default:
                            azzert();
                        }

                    // the next character in the String is now the next
                    // character to transfer/encode
                    ofPrev = of + 1;

                    cBrackets = 0;
                    break;
                    }

                default:
                    cBrackets = 0;
                    break;
                }
            }

        // there may be a portion of the string left that does not require
        // encoding
        if (ofPrev < cchNonWhite)
            {
            sb.append(ach, ofPrev, cchNonWhite - ofPrev);
            }

        // encode trailing whitespace
        for (; of < cch; ++of)
            {
            switch (ach[of])
                {
                case 0x09:
                    sb.append("&#x09;");
                    break;
                case 0x0A:
                    sb.append("&#x0A;");
                    break;
                case 0x0D:
                    sb.append("&#x0D;");
                    break;
                case 0x20:
                    sb.append("&#x20;");
                    break;
                default:
                    azzert();
                }
            }

        return sb.toString();
        }

    /**
    * Decode an element's content value.
    *
    * @param sValue  the content value to decode
    *
    * @return the attribute value in its decoded form
    */
    public static String decodeContent(String sValue)
        {
        return decodeAttribute(sValue);
        }

    /**
    * Encode a System Identifier as per the XML 1.0 Specification
    * second edition, section 4.2.2.
    *
    * @param sUri  the URI to encode
    *
    * @return the encoded URI
    */
    public static String encodeUri(String sUri)
        {
        // From the XML 1.0 specification, section 4.2.2:
        //
        // URI references require encoding and escaping of certain
        // characters. The disallowed characters include all non-ASCII
        // characters, plus the excluded characters listed in Section
        // 2.4 of [IETF RFC 2396], except for the number sign (#) and
        // percent sign (%) characters and the square bracket characters
        // re-allowed in [IETF RFC 2732]. Disallowed characters must be
        // escaped as follows:
        //
        // Each disallowed character is converted to UTF-8 [IETF RFC 2279]
        // as one or more bytes.
        //
        // Any octets corresponding to a disallowed character are escaped
        // with the URI escaping mechanism (that is, converted to %HH,
        // where HH is the hexadecimal notation of the byte value).
        //
        // The original character is replaced by the resulting character
        // sequence.

        // determine if escaping is necessary
        char[]  ach  = sUri.toCharArray();
        int     cch  = ach.length;

        boolean fEsc = false;
        Scan: for (int of = 0; of < cch; ++of)
            {
            char ch = ach[of];
            switch (ch)
                {
                case '<':
                case '>':
                case '"':
                case '{':
                case '}':
                case '|':
                case '\\':
                case '^':
                case '`':
                case '%':
                case '\'':
                case ' ':
                    fEsc = true;
                    break Scan;

                default:
                    if (ch <= 0x1F || ch >= 0x7F)
                        {
                        fEsc = true;
                        break Scan;
                        }
                    break;
                }
            }

        if (!fEsc)
            {
            return sUri;
            }

        ByteArrayOutputStream stream = new ByteArrayOutputStream(cch + 32);
        try
            {
            new DataOutputStream(stream).writeUTF(sUri);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        // convert the UTF octets from bytes to chars
        ach = stream.toString(0).toCharArray();
        cch = ach.length;

        StringBuilder sb = new StringBuilder(cch + 32);

        // scan for characters to escape
        // the UTF has a 2-byte header (length-encoding); discard that
        int ofPrev = 2;
        for (int ofCur = ofPrev; ofCur < cch; ++ofCur)
            {
            char ch = ach[ofCur];
            switch (ch)
                {
                default:
                    if (ch > 0x1F && ch < 0x7F)
                        {
                        break;
                        }
                    // fall through
                case '<':
                case '>':
                case '"':
                case '{':
                case '}':
                case '|':
                case '\\':
                case '^':
                case '`':
                case '%':
                case '\'':
                case ' ':
                    {
                    // copy up to this point
                    if (ofCur > ofPrev)
                        {
                        sb.append(ach, ofPrev, ofCur - ofPrev);
                        }

                    // encode the character
                    sb.append('%')
                      .append(toHex(ch));

                    // processed up to the following character
                    ofPrev = ofCur + 1;
                    }
                }
            }

        // copy the remainder of the string
        if (sb != null && ofPrev < cch)
            {
            sb.append(ach, ofPrev, cch - ofPrev);
            }

        return sb.toString();
        }

    /**
    * Decode a System Identifier as per the XML 1.0 Specification 2nd ed
    * section 4.2.2.
    *
    * @param sUri  the URI to decode
    *
    * @return the decoded URI
    */
    public static String decodeUri(String sUri)
        {
        String sOrig = sUri;

        // scan for characters to unescape
        if (sUri.indexOf('%') != -1)
            {
            char[] ach = sUri.toCharArray();
            int    cch = ach.length;
            StringBuilder sb = new StringBuilder(cch + 16);
            int ofPrev = 0;
            for (int of = 0; of < cch; ++of)
                {
                if (ach[of] == '%')
                    {
                    if (of + 2 >= cch)
                        {
                        throw new IllegalArgumentException("The URI ("
                                + sOrig + ") contains an unescaped '%'");
                        }

                    // transfer up to but not including the current offset
                    if (of > ofPrev)
                        {
                        sb.append(ach, ofPrev, of - ofPrev);
                        }

                    // convert the escaped sequence to a character
                    try
                        {
                        int n = Integer.parseInt(sUri.substring(of + 1, of + 3), 16);
                        if (n < 0 || n > 0xFF)
                            {
                            throw new IllegalArgumentException("out of range");
                            }

                        sb.append((char) n);
                        }
                    catch (Exception e)
                        {
                        throw new IllegalArgumentException("The URI ("
                                + sOrig + ") contains an illegal escape");
                        }

                    of += 2;
                    ofPrev = of + 1;
                    }
                }

            if (ofPrev < cch)
                {
                sb.append(ach, ofPrev, cch - ofPrev);
                }

            sUri = sb.toString();
            }

        // build length-encoded UTF
        int    cch = sUri.length();
        int    cb  = cch + 2;
        byte[] ab  = new byte[cb];
	    ab[0] = (byte) ((cch >>> 8) & 0xFF);
	    ab[1] = (byte) ((cch      ) & 0xFF);
        sUri.getBytes(0, cch, ab, 2);

        // read as String
        try
            {
            return new DataInputStream(new ByteArrayInputStream(ab)).readUTF();
            }
        catch (IOException e)
            {
            throw new IllegalArgumentException("Invalid URI: " + sUri);
            }
        }

    /**
    * XML quote the passed string.
    *
    * @param s  the string to quote
    *
    * @return the quoted string
    */
    public static String quote(String s)
        {
        return '\'' + encodeAttribute(s, '\'') + '\'';
        }


    // ----- XmlElement helpers ---------------------------------------------

    /**
    * Get the '/'-delimited path of the passed element starting from the
    * root element.
    *
    * @param xml  an XML element
    *
    * @return the path to the passed element in "absolute" format
    */
    public static String getAbsolutePath(XmlElement xml)
        {
        azzert(xml != null, "Null element");

        StringBuilder sb = new StringBuilder();
        do
            {
            sb.insert(0, "/" + xml.getName());

            xml = xml.getParent();
            }
        while (xml != null);

        return sb.toString();
        }

    /**
    * Check whether or not this element or any of its children elements
    * have any content such as values or attributes.
    *
    * @param xml  an XmlElement
    *
    * @return true iff the element itself and all of its children have
    *         neither values nor attributes
    */
    public static boolean isEmpty(XmlElement xml)
        {
        if (!xml.isEmpty())
            {
            return false;
            }

        Map mapAttr = xml.getAttributeMap();
        if (!mapAttr.isEmpty())
            {
            return false;
            }

        List listEl = xml.getElementList();
        if (!listEl.isEmpty())
            {
            return false;
            }

        for (Iterator iter = listEl.iterator(); iter.hasNext();)
            {
            XmlElement xmlEl = (XmlElement) iter.next();
            if (!isEmpty(xmlEl))
                {
                return false;
                }
            }
        return true;
        }

    /**
    * Get a child element for the specified element.
    *
    * If multiple child elements exist that have the specified name, then
    * the behavior of this method is undefined, and it is permitted to return
    * any one of the matching elements, to return null, or to throw an
    * arbitrary runtime exception.
    *
    * @param xml    an XML element
    * @param sName  the name of the desired child element
    *
    * @return the specified element as an object implementing XmlElement, or
    *         null if the specified child element does not exist
    */
    public static XmlElement getElement(XmlElement xml, String sName)
        {
        azzert(xml != null && sName != null && isNameValid(sName), "Null element or invalid name");

        List list = xml.getElementList();
        if (list.isEmpty())
            {
            return null;
            }

        for (Iterator iter = list.iterator(); iter.hasNext(); )
            {
            xml = (XmlElement) iter.next();
            if (xml.getName().equals(sName))
                {
                return xml;
                }
            }

        return null;
        }

    /**
    * Return true iff the specified element has a child element of the specified
    * name.
    *
    * @param xml    an XML element
    * @param sName  the name of the child element to test for
    *
    * @return true iff the child element exists
    */
    public static boolean hasElement(XmlElement xml, String sName)
        {
        azzert(xml != null && sName != null && isNameValid(sName), "Null element or invalid name");

        List list = xml.getElementList();

        for (Iterator iter = list.iterator(); iter.hasNext(); )
            {
            xml = (XmlElement) iter.next();
            if (xml.getName().equals(sName))
                {
                return true;
                }
            }

        return false;
        }

    /**
    * Find a child element of the specified element with the specified '/'-delimited path.
    * <p>
    * The path format is based on file-system paths (not XPath).
    * <ul>
    *   <li>  Leading '/' to specify root
    *   <li>  Use of '/' as a path delimiter
    *   <li>  Use of '..' to specify parent
    * </ul>
    * While the path expressions look like XPath, the semantics <strong>are not the same</strong> as XPath.
    * eg: in findElement "/" means the root element, after which the root element does not need to be specified,
    * however in XPath, the root element name still needs to be specified after the "/".
    * <p>
    * If multiple child elements exist that have the specified name, then the behavior of this method is undefined,
    * and it is permitted to return any one of the matching elements, to return null, or to throw an arbitrary runtime
    * exception.
    *
    * @param xml    an XML element
    * @param sPath  the path to follow to find the desired XML element
    *
    * @return the child element with the specified path or
    *         null if such a child element does not exist
    */
    public static XmlElement findElement(XmlElement xml, String sPath)
        {
        azzert(xml != null && sPath != null, "Null element or path");

        if (sPath.startsWith("/"))
            {
            xml = xml.getRoot();
            }

        StringTokenizer tokens = new StringTokenizer(sPath, "/");
        while (xml != null && tokens.hasMoreTokens())
            {
            String sName = tokens.nextToken();

            if (sName.equals(".."))
                {
                xml = xml.getParent();
                if (xml == null)
                    {
                    throw new IllegalArgumentException("Invalid path " + sPath);
                    }
                }
            else
                {
                xml = xml.getElement(sName);
                }
            }

        return xml;
        }

    /**
    * Find a child element of the specified element with the specified '/'-delimited path and specified value.
    * <p>
    * The path format is based on file-system paths (not XPath).
    * <ul>
    *   <li>  Leading '/' to specify root
    *   <li>  Use of '/' as a path delimiter
    *   <li>  Use of '..' to specify parent
    * </ul>
    * While the path expressions look like XPath, the semantics <strong>are not the same</strong> as XPath.
    * eg: in findElement "/" means the root element, after which the root element does not need to be specified,
    * however in XPath, the root element name still needs to be specified after the "/".
    * <p>
    * If multiple child elements exist that have the specified name and value, then this method returns any one of
    * the matching elements
    *
    * @param xml     an XML element
    * @param sPath   the path to follow to find the desired XML element
    * @param oValue  the value to match
    *
    * @return the child element with the specified path and value or
    *         null if the such a child element does not exist
    */
    public static XmlElement findElement(XmlElement xml, String sPath, Object oValue)
        {
        azzert(xml != null && sPath != null, "Null element or path");

        while (sPath.startsWith("/"))
            {
            xml   = xml.getRoot();
            sPath = sPath.substring(1);
            }

        String sName;
        int    ofNext = sPath.indexOf("/");
        if (ofNext == -1)
            {
            sName = sPath;
            sPath = "";
            }
        else
            {
            sName = sPath.substring(0, ofNext);
            sPath = sPath.substring(ofNext + 1);

            while (sPath.startsWith("/"))
                {
                sPath = sPath.substring(1);
                }
            }

        if (sPath.length() == 0)
            {
            if (sName.equals(".."))
                {
                xml = xml.getParent();
                return xml != null && equals(xml.getValue(), oValue) ? xml : null;
               }
            else
                {
                for (Iterator iter = xml.getElements(sName); iter.hasNext();)
                    {
                    xml = (XmlElement) iter.next();

                    if (equals(xml.getValue(), oValue))
                        {
                        return xml;
                        }
                    }
                return null;
                }
            }
        else
            {
            if (sName.equals(".."))
                {
                xml = xml.getParent();
                return xml == null ? null : findElement(xml, sPath, oValue);
               }
            else
                {
                for (Iterator iter = xml.getElements(sName); iter.hasNext();)
                    {
                    xml = findElement((XmlElement) iter.next(), sPath, oValue);

                    if (xml != null)
                        {
                        return xml;
                        }
                    }
                return null;
                }
            }
        }

    /**
    * Ensure that a child element exists.
    *
    * If any part of the path does not exist create new child
    * elements to match the path.
    *
    * @param xml    and XmlElement
    * @param sPath  element path
    *
    * @return the existing or new XmlElement object
    *
    * @throws IllegalArgumentException  if the name is null or if any part
    *         of the path is not a legal XML tag name
    * @throws UnsupportedOperationException if any element in the path
    *         is immutable or otherwise can not add a child element
    *
    * @see #findElement(XmlElement, String)
    */
    public static XmlElement ensureElement(XmlElement xml, String sPath)
        {
        azzert(xml != null && sPath != null, "Null element or path");

        if (sPath.startsWith("/"))
            {
            xml = xml.getRoot();
            }

        StringTokenizer tokens = new StringTokenizer(sPath, "/");
        while (tokens.hasMoreTokens())
            {
            String sName = tokens.nextToken();

            if (sName.equals(".."))
                {
                xml = xml.getParent();
                if (xml == null)
                    {
                    throw new IllegalArgumentException("Invalid path " + sPath);
                    }
                }
            else
                {
                XmlElement child = xml.getElement(sName);
                xml = child == null ? xml.addElement(sName) : child;
                }
            }

        return xml;
        }

    /**
    * Add the elements from the iterator to the passed XML.
    *
    * @param xml   an XmlElement object to add to
    * @param iter  an Iterator of zero or more XmlElement objects to add
    */
    public static void addElements(XmlElement xml, Iterator iter)
        {
        List list = xml.getElementList();
        while (iter.hasNext())
            {
            list.add(iter.next());
            }
        }

    /**
    * Remove all immediate child elements with the given name
    *
    * @param xml    an XmlElement
    * @param sName  child element name
    *
    * @return the number of removed child elements
    *
    * @throws UnsupportedOperationException if the element is immutable
    *         or otherwise cannot remove a child element
    */
    public static int removeElement(XmlElement xml, String sName)
        {
        int cnt = 0;
        for (Iterator iter = xml.getElementList().iterator(); iter.hasNext();)
            {
            xml = (XmlElement) iter.next();

            if (xml.getName().equals(sName))
                {
                iter.remove();
                cnt++;
                }
            }
        return cnt;
        }

    /**
    * Remove all empty descendant elements.
    *
    * @param xml  the parent XmlElement
    *
    * @return the input XmlElement
    *
    * @throws UnsupportedOperationException if the element is immutable
    *         or otherwise cannot remove a child element
    */
    public static XmlElement removeEmptyElements(XmlElement xml)
        {
        for (Iterator<XmlElement> iter =
            ((List<XmlElement>) xml.getElementList()).iterator(); iter.hasNext();)
            {
            XmlElement xmlChild = removeEmptyElements(iter.next());

            if (xmlChild.isEmpty() && xmlChild.getElementList().isEmpty()
                && xmlChild.getAttributeMap().isEmpty())
                {
                iter.remove();
                }
            }
        return xml;
        }

    /**
    * Replace a child element with the same name as the specified element.
    * If the child element does not exist the specified element is just added.
    *
    * @param xmlParent   parent XmlElement
    * @param xmlReplace  element to replace with
    *
    * @return true if matching child element has been found and replaced;
    *         false otherwise
    *
    * @throws UnsupportedOperationException if the parent element is immutable
    *         or otherwise cannot remove a child element
    */
    public static boolean replaceElement(XmlElement xmlParent, XmlElement xmlReplace)
        {
        List list = xmlParent.getElementList();
        for (Iterator iter = list.iterator(); iter.hasNext();)
            {
            XmlElement xml = (XmlElement) iter.next();

            if (xml.getName().equals(xmlReplace.getName()))
                {
                list.set(list.indexOf(xml), xmlReplace.clone());
                return true;
                }
            }

        list.add(xmlReplace.clone());
        return false;
        }

    /**
    * Override the values of the specified base element with values from
    * the specified override element.
    * <p>
    * The values are only overridden if there is an exact match between the
    * element paths and all attribute values.  Empty override values are ignored.
    * Override elements that do not match any of the base elements are just
    * copied over.  No ambiguity is allowed.
    * <p>
    * For example, if the base element has more then one child with the same
    * name and attributes then the override is not allowed.
    *
    * @param xmlBase      base XmlElement
    * @param xmlOverride  override XmlElement
    *
    * @throws UnsupportedOperationException if the base element is immutable
    *         or there is ambiguity between the override and base elements
    */
    public static void overrideElement(XmlElement xmlBase, XmlElement xmlOverride)
        {
        overrideElement(xmlBase, xmlOverride, null);
        }
    /**
    * Override the values of the specified base element with values from
    * the specified override element.
    * <p>
    * The values are only overridden if there is an exact match between the
    * element paths and an attribute value for the specified attribute name.
    * Empty override values are ignored.
    * Override elements that do not match any of the base elements are just
    * copied over.  No ambiguity is allowed.
    * <p>
    * For example, if the base element has more then one child with the same
    * name and the specified attribute's value then the override is not allowed.
    * <p>
    * As of Coherence 12.2.1, the only exception from the above rule is a
    * scenario when a parent override element is a homogeneous sequence of
    * identically named simple child elements with no attributes. In that case,
    * all the corresponding child elements from the base are removed and replaced
    * with the override content.
    *
    * @param xmlBase      base XmlElement
    * @param xmlOverride  override XmlElement
    * @param sIdAttrName  attribute name that serves as an identifier allowing
    *                     to match elements with the same name; if not specified
    *                     all attributes have to match for an override
    *
    * @throws UnsupportedOperationException if the base element is immutable
    *         or there is ambiguity between the override and base elements
    */
    public static void overrideElement(XmlElement xmlBase, XmlElement xmlOverride,
            String sIdAttrName)
        {
        for (Iterator iterOver = xmlOverride.getElementList().iterator(); iterOver.hasNext();)
            {
            XmlElement xmlOver = (XmlElement) iterOver.next();
            if (isEmpty(xmlOver))
                {
                continue;
                }

            String sName   = xmlOver.getName();
            Object oAttrId = sIdAttrName == null ?
                xmlOver.getAttributeMap() : xmlOver.getAttribute(sIdAttrName);

            // ensure uniqueness
            for (Iterator iter = xmlOverride.getElements(sName); iter.hasNext();)
                {
                XmlElement xmlTest = (XmlElement) iter.next();
                if (xmlTest != xmlOver)
                    {
                    Object oAttrTest = sIdAttrName == null ?
                        xmlTest.getAttributeMap() : xmlTest.getAttribute(sIdAttrName);
                    if (equals(oAttrTest, oAttrId))
                        {
                        throw new UnsupportedOperationException(
                            "Override element is not unique:\n" + xmlOver);
                        }
                    }
                }

            // find matching base element
            XmlElement xmlMatch = null;
            for (Iterator iterBase = xmlBase.getElements(sName); iterBase.hasNext();)
                {
                XmlElement xmlTest = (XmlElement) iterBase.next();

                Object oAttrTest = sIdAttrName == null ?
                    xmlTest.getAttributeMap() : xmlTest.getAttribute(sIdAttrName);
                if (equals(oAttrTest, oAttrId))
                    {
                    if (xmlMatch == null)
                        {
                        xmlMatch = xmlTest;
                        }
                    else
                        {
                        throw new UnsupportedOperationException(
                            "Override element is ambiguous:\n" + xmlOver);
                        }
                    }
                }

            if (xmlMatch == null)
                {
                // no match; append to the base
                xmlBase.getElementList().add(xmlOver.clone());
                }
            else
                {
                String sValue       = xmlOver.getString();
                List   listChildren = xmlOver.getElementList();
                if (listChildren.isEmpty() && !sValue.isEmpty())
                    {
                    // terminal non-empty value - drop all existing children
                    xmlMatch.getElementList().clear();
                    xmlMatch.setString(sValue);
                    }
                else if (sValue.isEmpty() && !listChildren.isEmpty())
                    {
                    // container element - drop the existing value
                    xmlMatch.setString("");

                    if (isSimpleSequence(listChildren))
                        {
                        // replace all corresponding children with the override list
                        String sChild = ((XmlElement) listChildren.get(0)).getName();
                        for (Iterator iter = xmlMatch.getElements(sChild); iter.hasNext();)
                            {
                            iter.next();
                            iter.remove();
                            }
                        xmlMatch.getElementList().addAll(listChildren);
                        continue;
                        }
                    }

                // merge override children
                overrideElement(xmlMatch, xmlOver, sIdAttrName);
                }
            }
        }

    /**
    * Replace the values of the XmlElement and all its children that contain
    * the specified attribute with the values returned by the
    * <tt>Config.getProperty()</tt> call.
    * <p>
    * This method iterates the specified XmlElement tree and for each element
    * that contains the attribute with the specified name replaces its value
    * with the value of the corresponding system property (if exists).
    *
    * @param xml                 the XmlElement to process
    * @param sPropertyAttribute  the name of the attribute that supplies the
    *                            corresponding system property name
    */
    public static void replaceSystemProperties(XmlElement xml, String sPropertyAttribute)
        {
        XmlValue attr = xml.getAttribute(sPropertyAttribute);
        if (attr != null)
            {
            // remove the attribute
            xml.setAttribute(sPropertyAttribute, null);

            // set the element's value from the specified system property
            try
                {
                String sValue = Config.getProperty(attr.getString());
                if (sValue != null)
                    {
                    xml.setString(sValue);
                    }
                }
            catch (Exception e) {}
            }

        SystemPropertyPreprocessor.processValueMacro(xml);

        // iterate for each contained element
        for (Iterator iter = xml.getElementList().iterator(); iter.hasNext();)
            {
            replaceSystemProperties((XmlElement) iter.next(), sPropertyAttribute);
            }
        }


    // ----- Namespace support helpers --------------------------------------

    /**
    * Retrieve a listing of schema locations
    * (schemaLocation/noNamespaceSchemaLocation) URL's referenced
    * in XmlElement and all of its children.
    *
    * @param xml      the XmlElement to process
    * @param sPrefix  prefix of schema instances
    *
    * @return List of strings representing the schema location URL's
    */
    public static List<String> getSchemaLocations(XmlElement xml, String sPrefix)
        {
        List<String> listURLs = new ArrayList<String>();

        // Need to determine the prefix used for the schema and then
        // prepend it to the schemeLocation and noNamespaceSchemaLocation
        // attribute. This is null on first call and has a value on
        // recursive calls, optimization.
        if (sPrefix == null)
            {
            sPrefix = getNamespacePrefix(xml,
                     XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            }

        if (sPrefix != null)
            {
            XmlValue xmlLocation = xml.getAttribute(sPrefix + ":schemaLocation");
            if (xmlLocation == null)
                {
                xmlLocation = xml.getAttribute(sPrefix + ":noNamespaceSchemaLocation");
                if (xmlLocation != null)
                    {
                    listURLs.add(xmlLocation.getString());
                    }
                }
            else
                {
                // The format of the schemaLocation is a pair of URLs
                // "URL1 URl2" where URL1 is the xsd namespace and URL2 is
                // a hint to the location of the xsd. There can be a list of
                // URL pairs.  We will load the URL reference specified in
                // the URL location (URL2)
                String[] sURLs = xmlLocation.getString().split("\\s+");
                for (int i = 1; i < sURLs.length; i = i + 2)
                    {
                    listURLs.add(sURLs[i]);
                    }
                }

            // potentially each element can have a schemeLocation specified.
            for (Iterator iter = xml.getElementList().iterator(); iter.hasNext(); )
                {
                List<String> listChildURLs = getSchemaLocations(
                        (XmlElement) iter.next(), sPrefix);
                if (!listChildURLs.isEmpty())
                    {
                    listURLs.addAll(listChildURLs);
                    }
                }
            }

        return listURLs;
        }

    /**
    * Retrieve the Namespace URI for a given prefix in a context of the
    * specified XmlElement.
    *
    * @param xml      the XmlElement
    * @param sPrefix  the Namespace prefix
    *
    * @return the Namespace URI corresponding to the prefix
    */
    public static String getNamespaceUri(XmlElement xml, String sPrefix)
        {
        String sAttrName = "xmlns:" + sPrefix;

        while (xml != null)
            {
            XmlValue attrXmlns = xml.getAttribute(sAttrName);
            if (attrXmlns != null)
                {
                return attrXmlns.getString();
                }
            xml = xml.getParent();
            }
        return null;
        }

    /**
    * Retrieve the Namespace prefix for a given URI in a context of the
    * specified XmlElement.
    *
    * @param xml   the XmlElement
    * @param sUri  the Namespace URI
    *
    * @return the Namespace prefix corresponding to the URI
    */
    public static String getNamespacePrefix(XmlElement xml, String sUri)
        {
        while (xml != null)
            {
            for (Iterator iter = xml.getAttributeMap().entrySet().iterator(); iter.hasNext();)
                {
                Map.Entry entry = (Map.Entry) iter.next();

                if (sUri.equals(((XmlValue) entry.getValue()).getString()))
                    {
                    String sAttr = (String) entry.getKey();
                    if (sAttr.startsWith("xmlns:"))
                        {
                        return sAttr.substring(6); // "xmlns:".length()
                        }
                    }
                }
            xml = xml.getParent();
            }
        return null;
        }

    /**
    * Ensure the existence of the Namespace declaration attribute in a context
    * of the specified XmlElement.
    *
    * @param xml      the XmlElement
    * @param sPrefix  the Namespace prefix
    * @param sUri     the Namespace URI
    */
    public static void ensureNamespace(XmlElement xml, String sPrefix, String sUri)
        {
        String sNmsUri = getNamespaceUri(xml, sPrefix);
        if (sNmsUri == null)
            {
            xml.addAttribute("xmlns:" + sPrefix).setString(sUri);
            }
        else if (!sNmsUri.equals(sUri))
            {
            throw new IllegalStateException("Namespace conflict: prefix=" + sPrefix +
                ", current URI=" + sNmsUri + ", new URI=" + sUri);
            }
        }

    /**
    * Return a universal XML element name.
    *
    * @param sLocal   the local XML element name
    * @param sPrefix  the Namespace prefix
    *
    * @return the universal XML element name.
    *
    * @see <a href="http://www.jclark.com/xml/xmlns.htm">XML Namespaces</a>
    */
    public static String getUniversalName(String sLocal, String sPrefix)
        {
        return sPrefix == null || sLocal == null || sLocal.length() == 0 ?
            sLocal : sPrefix + ':' + sLocal;
        }

    /**
    * Check whether or not a universal (composite) name matches to the specified
    * local name and Namespace URI.
    *
    * @param xml    the (context) XmlElement
    * @param sName  the universal name
    * @param sLocal the local xml name
    * @param sUri   the Namespace URI
    *
    * @return true if the specified element  matches to the specified
    *         local name and the specified Namespace URI.
    */
    public static boolean isNameMatch(XmlElement xml, String sName, String sLocal, String sUri)
        {
        if (sUri == null)
            {
            return sName.equals(sLocal);
            }
        else
            {
            String sSuffix = ':' + sLocal;

            if (sName.endsWith(sSuffix))
                {
                String sPrefix = sName.substring(0, sName.length() - sSuffix.length());
                return sUri.equals(getNamespaceUri(xml, sPrefix));
                }
            else
                {
                // allow for "default" match
                return sName.equals(sLocal);
                }
            }
        }

    /**
    * Check whether or not an element matches to the specified local name
    * and Namespace URI.
    *
    * @param xml    the XmlElement
    * @param sLocal the local xml name
    * @param sUri   the Namespace URI
    *
    * @return true if the specified element  matches to the specified
    *         local name and the specified Namespace URI.
    */
    public static boolean isElementMatch(XmlElement xml, String sLocal, String sUri)
        {
        return isNameMatch(xml, xml.getName(), sLocal, sUri);
        }

    /**
    * Get a child element of the specified XmlElement that matches
    * to the specified local name and the specified Namespace URI.
    *
    * @param xml    the parent XmlElement
    * @param sLocal the local xml name
    * @param sUri   the Namespace URI
    *
    * @return an element that matches to the specified local name
    *         and the specified Namespace URI.
    */
    public static XmlElement getElement(XmlElement xml, String sLocal, String sUri)
        {
        if (sUri == null)
            {
            return xml.getElement(sLocal);
            }
        else
            {
            for (Iterator iter = xml.getElementList().iterator(); iter.hasNext();)
                {
                XmlElement el = (XmlElement) iter.next();

                if (isElementMatch(el, sLocal, sUri))
                    {
                    return el;
                    }
                }
            return null;
            }
        }

    /**
    * Check whether or not the specified list contains only simple elements
    * with the same names.
    *
    * @param listXml  the list of XmlElements
    *
    * @return true if the specified list contains only simple elements with
    *              the same name
    */
    protected static boolean isSimpleSequence(List<XmlElement> listXml)
        {
        String sName = null;

        for (XmlElement xmlTest: listXml)
            {
            if (!xmlTest.getAttributeMap().isEmpty()
             || !xmlTest.getElementList().isEmpty())
                {
                return false;
                }

            if (sName == null)
                {
                sName = xmlTest.getName();
                }
            else if (!sName.equals(xmlTest.getName()))
                {
                return false;
                }
            }
        return true;
        }

    /**
    * Get an attribute of the specified XmlElement that matches
    * to the specified local name and the specified Namespace URI.
    *
    * @param xml    the XmlElement
    * @param sLocal the local attribute name
    * @param sUri   the Namespace URI
    *
    * @return an XmlValue that matches to the specified local name
    *         and the specified Namespace URI.
    */
    public static XmlValue getAttribute(XmlElement xml, String sLocal, String sUri)
        {
        if (sUri == null)
            {
            return xml.getAttribute(sLocal);
            }
        else
            {
            for (Iterator iter = xml.getAttributeMap().entrySet().iterator(); iter.hasNext();)
                {
                Map.Entry entry = (Map.Entry) iter.next();
                String    sAttr = (String) entry.getKey();

                if (isNameMatch(xml, sAttr, sLocal, sUri))
                    {
                    return (XmlValue) entry.getValue();
                    }
                }
            return null;
            }
        }

    /**
    * Get an iterator of child elements of the specified XmlElement that
    * match to the specified local name and the specified Namespace URI.
    *
    * @param xml    the parent XmlElement
    * @param sLocal the local xml name
    * @param sUri   the Namespace URI
    *
    * @return an iterator containing all matching child elements.
    */
    public static Iterator getElements(XmlElement xml, final String sLocal, final String sUri)
        {
        if (sUri == null)
            {
            return xml.getElements(sLocal);
            }
        else
            {
            Filter filter = new Filter()
                {
                public boolean evaluate(Object o)
                    {
                    return o instanceof XmlElement &&
                           isElementMatch((XmlElement) o, sLocal, sUri);
                    }
                };
            return new FilterEnumerator(xml.getElementList().iterator(), filter);
            }
        }

    /**
    * For the specified XmlElement purge the Namespace declarations that are
    * declared somewhere up the xml tree.
    *
    * @param xml  the XmlElement
    */
    public static void purgeNamespace(XmlElement xml)
        {
        XmlElement xmlParent = xml.getParent();
        if (xmlParent == null)
            {
            return;
            }

        for (Iterator iter = xml.getAttributeMap().entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String sAttr = (String) entry.getKey();
            if (sAttr.startsWith("xmlns:"))
                {
                String sPrefix = sAttr.substring(6); // "xmlns:".length()
                String sUri    = ((XmlValue) entry.getValue()).getString();

                if (sUri.equals(getNamespaceUri(xmlParent, sPrefix)))
                    {
                    iter.remove();
                    }
                }
            }
         }

    /**
    * For the children elements of the specified XmlElement purge the repetitive
    * Namespace declarations.
    *
    * @param xml  the XmlElement
    */
    public static void purgeChildrenNamespace(XmlElement xml)
        {
        for (Iterator iter = xml.getElementList().iterator(); iter.hasNext();)
            {
            purgeNamespace((XmlElement) iter.next());
            }
        }


    // ----- misc helpers ---------------------------------------------------

    /**
    * Parse the specified "init-params" element of the following structure:
    * <pre>
    * &lt;!ELEMENT init-params (init-param*)&gt;
    * &lt;!ELEMENT init-param ((param-name | param-type), param-value, description?)&gt;
    * </pre>
    * into an object array.
    * <p>
    * For the purpose of this method only the parameters that have the "param-type"
    * element specified are processed. The following types are supported:
    * <ul>
    * <li> string   (a.k.a. java.lang.String)
    * <li> boolean  (a.k.a. java.lang.Boolean)
    * <li> int      (a.k.a. java.lang.Integer)
    * <li> long     (a.k.a. java.lang.Long)
    * <li> double   (a.k.a. java.lang.Double)
    * <li> decimal  (a.k.a. java.math.BigDecimal)
    * <li> file     (a.k.a. java.io.File)
    * <li> date     (a.k.a. java.sql.Date)
    * <li> time     (a.k.a. java.sql.Time)
    * <li> datetime (a.k.a. java.sql.Timestamp)
    * <li> xml      (a.k.a. com.tangosol.run.xml.XmlElement)
    * </ul>
    * For any other [explicitly specified] types the corresponding "init-param" XmlElement
    * itself is placed into the returned array.
    *
    * @param xmlParams  the "init-params" XmlElement to parse
    *
    * @return an array of parameters
    */
    public static Object[] parseInitParams(XmlElement xmlParams)
        {
        return parseInitParams(xmlParams, null);
        }

    /**
    * Parse the String value of the child XmlElement with the given name as a
    * time in milliseconds. If the specified child XmlElement does not exist or
    * is empty, the specified default value is returned.
    *
    * @param xml       the parent XmlElement
    * @param sName     the name of the child XmlElement
    * @param cDefault  the default value
    *
    * @return the time (in milliseconds) represented by the specified child
    *         XmlElement
    */
    public static long parseTime(XmlElement xml, String sName, long cDefault)
        {
        if (xml == null)
            {
            return cDefault;
            }

        String sTime = xml.getSafeElement(sName).getString().trim();
        if (sTime.length() == 0)
            {
            return cDefault;
            }

        try
            {
            return Base.parseTime(sTime);
            }
        catch (RuntimeException e)
            {
            throw Base.ensureRuntimeException(e, "illegal \"" + sName + "\" value: " + sTime);
            }
        }

    /**
    * An interface that describes a callback to resolve a substitutable
    * parameter value.
    */
    public interface ParameterResolver
        {
        /**
        * Resolve the passed substitutable parameter.
        *
        * @param sType   the value of the "param-type" element
        * @param sValue  the value of the "param-value" element, which is
        *                enclosed by curly braces, indicating its
        *                substitutability
        *
        * @return the object value to use or the {@link #UNRESOLVED} constant
        */
        public Object resolveParameter(String sType, String sValue);

        /**
        * A constant that indicates that the parameter cannot be resolved.
        */
        public static final Object UNRESOLVED = new Object();
        }

    /**
    * Parse the specified "init-params" element of the following structure:
    * <pre>
    * &lt;!ELEMENT init-params (init-param*)&gt;
    * &lt;!ELEMENT init-param ((param-name | param-type), param-value, description?)&gt;
    * </pre>
    * into an object array.
    * <p>
    * For the purpose of this method only the parameters that have the "param-type"
    * element specified are processed. The following types are supported:
    * <ul>
    * <li> string   (a.k.a. java.lang.String)
    * <li> boolean  (a.k.a. java.lang.Boolean)
    * <li> int      (a.k.a. java.lang.Integer)
    * <li> long     (a.k.a. java.lang.Long)
    * <li> double   (a.k.a. java.lang.Double)
    * <li> decimal  (a.k.a. java.math.BigDecimal)
    * <li> file     (a.k.a. java.io.File)
    * <li> date     (a.k.a. java.sql.Date)
    * <li> time     (a.k.a. java.sql.Time)
    * <li> datetime (a.k.a. java.sql.Timestamp)
    * <li> xml      (a.k.a. com.tangosol.run.xml.XmlElement)
    * </ul>
    * For any other [explicitly specified] types the corresponding
    * "init-param" XmlElement itself is placed into the returned array.
    *
    * @param xmlParams  the "init-params" XmlElement to parse
    * @param resolver   a ParameterResolver to resolve "{macro}" values (optional)
    *
    * @return an array of parameters
    */
    public static Object[] parseInitParams(XmlElement xmlParams, ParameterResolver resolver)
        {
        List listParam = new ArrayList();
        for (Iterator iter = xmlParams.getElements("init-param"); iter.hasNext();)
            {
            XmlElement xmlParam = (XmlElement) iter.next();
            String     sType    = xmlParam.getSafeElement("param-type" ).getString();
            XmlElement xmlValue = xmlParam.getSafeElement("param-value");
            String     sValue   = xmlValue.getString();

            // resolver has a priority if the type is a "{macro}"
            // or the value contains a "{macro}"
            if (resolver != null &&
                  ((  sType.indexOf('{') == 0
                  && sType.lastIndexOf('}') == sType.length() - 1)
                ||
                  (sValue.indexOf('{') >= 0
                  && sValue.indexOf('{') < sValue.lastIndexOf('}'))))
                {
                Object oResolved = resolver.resolveParameter(sType, sValue);
                if (oResolved != ParameterResolver.UNRESOLVED)
                    {
                    listParam.add(oResolved);
                    continue;
                    }
                }

            // process well-known types
            if (sType.length() == 0)
                {
                throw new RuntimeException("missing parameter type:\n" + xmlParam);
                }
            else if (sType.equalsIgnoreCase("string") || sType.equals("java.lang.String"))
                {
                listParam.add(sValue);
                }
            else if (sType.equalsIgnoreCase("int") || sType.equals("java.lang.Integer"))
                {
                listParam.add(Integer.valueOf(xmlValue.getInt()));
                }
            else if (sType.equalsIgnoreCase("long") || sType.equals("java.lang.Long"))
                {
                listParam.add(Long.valueOf(xmlValue.getLong()));
                }
            else if (sType.equalsIgnoreCase("boolean") || sType.equals("java.lang.Boolean"))
                {
                listParam.add(Boolean.valueOf(xmlValue.getBoolean()));
                }
            else if (sType.equalsIgnoreCase("double") || sType.equals("java.lang.Double"))
                {
                listParam.add(new Double(xmlValue.getDouble()));
                }
            else if (sType.equalsIgnoreCase("float") || sType.equals("java.lang.Float"))
                {
                listParam.add(new Float(xmlValue.getDouble()));
                }
            else if (sType.equalsIgnoreCase("decimal") || sType.equals("java.math.BigDecimal"))
                {
                listParam.add(xmlValue.getDecimal());
                }
            else if (sType.equalsIgnoreCase("file") || sType.equals("java.io.File"))
                {
                listParam.add(new File(xmlValue.getString()));
                }
            else if (sType.equalsIgnoreCase("date") || sType.equals("java.sql.Date"))
                {
                listParam.add(xmlValue.getDate());
                }
            else if (sType.equalsIgnoreCase("time") || sType.equals("java.sql.Time"))
                {
                listParam.add(xmlValue.getTime());
                }
            else if (sType.equalsIgnoreCase("datetime") || sType.equals("java.sql.Timestamp"))
                {
                listParam.add(xmlValue.getDateTime());
                }
            else if (sType.equalsIgnoreCase("xml") || sType.equals("com.tangosol.run.xml.XmlElement"))
                {
                listParam.add(xmlValue.clone());
                }
            else
                {
                // the type is unknown; pass the XmlElement itself as a parameter
                listParam.add(xmlParam.clone());
                }
            }
        return listParam.toArray();
        }

    /**
    * Transform the specified "init-params" element of the following structure:
    * <pre>
    * &lt;!ELEMENT init-params (init-param*)&gt;
    * &lt;!ELEMENT init-param ((param-name | param-type), param-value, description?)&gt;
    * </pre>
    * into an XML element composed of the corresponding names. For example, the
    * the "init-params" element of the following structure:
    * <pre>
    *   &lt;init-param&gt;
    *      &lt;param-name&gt;NameOne&lt;/param-name&gt;
    *      &lt;param-value&gt;ValueOne&lt;/param-value&gt;
    *   &lt;/init-param&gt;
    *   &lt;init-param&gt;
    *      &lt;param-name&gt;NameTwo&lt;/param-name&gt;
    *      &lt;param-value&gt;ValueTwo&lt;/param-value&gt;
    *   &lt;/init-param&gt;
    * </pre>
    * will transform into
    * <pre>
    *   &lt;NameOne&gt;ValueOne&lt;/NameOne&gt;
    *   &lt;NameTwo&gt;ValueTwo&lt;/NameTwo&gt;
    * </pre>
    * For the purpose of this method only the parameters that have the "param-name"
    * element specified are processed.
    *
    * @param xmlParent  the XML element to insert the transformed elements into
    * @param xmlParams  the "init-params" XmlElement to parse
    *
    * @return the resulting XmlElement (xmlParent)
    */
    public static XmlElement transformInitParams(XmlElement xmlParent, XmlElement xmlParams)
        {
        for (Iterator iter = xmlParams.getElements("init-param"); iter.hasNext();)
            {
            XmlElement xmlParam = (XmlElement) iter.next();
            String     sName    = xmlParam.getSafeElement("param-name" ).getString();
            String     sValue   = xmlParam.getSafeElement("param-value").getString();

            if (sName.length() != 0)
                {
                xmlParent.ensureElement(sName).setString(sValue);
                }
            }

        return xmlParent;
        }

    /**
    * Encode the supplied xmlConfig XmlElement as a series of init-param
    * elements.  This operation is the inverse of transformInitParams.
    *
    * @param xmlParent  the element in which to add the init-param elements
    * @param xmlConfig  the element to encode from
    *
    * @return the resulting XmlElement (xmlParent)
    */
    public static XmlElement encodeInitParams(XmlElement xmlParent, XmlElement xmlConfig)
        {
        for (Iterator iter = xmlConfig.getElementList().iterator(); iter.hasNext(); )
            {
            XmlElement xmlElem  = (XmlElement) iter.next();
            XmlElement xmlParam = xmlParent.addElement("init-param");
            xmlParam.addElement("param-name").setString(xmlElem.getName());
            xmlParam.addElement("param-value").setString(xmlElem.getString());
            }

        return xmlParent;
        }

    /**
     * Parse the specified "init-params" elements and return the array
     * of the param types.
     *
     * @param xmlParams  the "init-params" XmlElement to parse
     *
     * @return an array of parameter types
     */
    public static String[] parseParamTypes(XmlElement xmlParams)
        {
        List<String> listParam = new ArrayList<>();
        for (Iterator iter = xmlParams.getElements("init-param"); iter.hasNext(); )
            {
            XmlElement xmlParam   = (XmlElement) iter.next();
            String     sParamType = xmlParam.getSafeElement("param-type").getString();

            sParamType = sParamType == null || sParamType.trim().equals("") ? null : sParamType;

            listParam.add(sParamType);
            }
        return listParam.toArray(new String[listParam.size()]);
        }

    /**
    * Check whether or not the specified configuration defines an instance of a
    * class. The specified XmlElement should be of the same structure as used in
    * the {@link #createInstance(XmlElement, ClassLoader, ParameterResolver)
    * createInstance()} method.
    *
    * @param xmlClass  the XML element that contains the instantiation info
    *
    * @return true iff there is no class configuration information available
    */
    public static boolean isInstanceConfigEmpty(XmlElement xmlClass)
        {
        return isEmpty(xmlClass.getSafeElement("instance")) &&
               isEmpty(xmlClass.getSafeElement("class-name")) &&
               isEmpty(xmlClass.getSafeElement("class-factory-name"));
        }

    /**
    * Create an instance of the class configured using an XmlElement of the
    * following structure:
    * <pre>
    *   &lt;!ELEMENT ... (class-name | (class-factory-name, method-name), init-params?&gt;
    *   &lt;!ELEMENT init-params (init-param*)&gt;
    *   &lt;!ELEMENT init-param ((param-name | param-type), param-value, description?)&gt;
    * </pre>
    *
    * As of Coherence 3.6 the supplied element may specify all of the above
    * elements within an &lt;instance&gt; element.
    *
    * @param xml       the XML element that contains the instantiation info
    * @param loader    a ClassLoader that should be used to load necessary
    *                  classes (optional)
    * @param resolver  a ParameterResolver to resolve "{macro}" values (optional)
    *
    * @return an object instantiated or obtained based on the class configuration
    */
    public static Object createInstance(XmlElement xml, ClassLoader loader,
            ParameterResolver resolver)
        {
        return createInstance(xml, loader, resolver, /*clzAssignable*/ null);
        }

    /**
    * Create an instance of the class configured using an XmlElement of the
    * following structure:
    * <pre>
    *   &lt;!ELEMENT ... (class-name | (class-factory-name, method-name), init-params?&gt;
    *   &lt;!ELEMENT init-params (init-param*)&gt;
    *   &lt;!ELEMENT init-param ((param-name | param-type), param-value, description?)&gt;
    * </pre>
    *
    * As of Coherence 3.6 the supplied element may also be of the following format:
    * <pre>
    * &lt;!ELEMENT instance&gt;
    * </pre>
    * where the "instance" format is the same as above.
    *
    * @param xml            the XML element that contains the instantiation info
    * @param loader         a ClassLoader that should be used to load necessary
    *                       classes (optional)
    * @param resolver       a ParameterResolver to resolve "{macro}" values (optional)
    * @param clzAssignable  if non-null, this method will validate that
    *                       the Class is assignable from the loaded Class
    *
    * @return an object instantiated or obtained based on the class configuration
    */
    public static Object createInstance(XmlElement xml, ClassLoader loader,
            ParameterResolver resolver, Class clzAssignable)
        {
        XmlElement xmlInstance = xml.getSafeElement("instance");
        if (xmlInstance.getElementList().isEmpty())
            {
            // pre 3.6 style, no outer instance element
            xmlInstance = xml;
            }

        String   sClass  = xmlInstance.getSafeElement("class-name").getString();
        String   sMethod = null;
        Class    clz     = null;

        if (sClass.length() == 0)
            {
            sClass  = xmlInstance.getSafeElement("class-factory-name").getString();
            sMethod = xmlInstance.getSafeElement("method-name").getString();
            if (sClass.length() == 0)
                {
                throw new IllegalArgumentException("The configuration element \""
                        + xmlInstance.getName()
                        + "\" does not specify a \"class-name\" or \"class-factory-name\"\n" + xml);
                }
            else if (sMethod.length() == 0)
                {
                throw new IllegalArgumentException("The configuration element \""
                        + xmlInstance.getName()
                        + "\" specifies a \"class-factory-name\", but no \"method-name\"\n" + xml);
                }
            }

        XmlElement xmlParams = xmlInstance.getElement("init-params");
        Object[]   aoParam   = ClassHelper.VOID;
        if (xmlParams != null)
            {
            try
                {
                aoParam   = XmlHelper.parseInitParams(xmlParams, resolver);
                xmlParams = null; // params have been consumed
                }
            catch (RuntimeException e) {}
            }

        Object oInstance;
        try
            {
            clz       = ExternalizableHelper.loadClass(sClass, loader, null);
            oInstance = sMethod == null
                    ? ClassHelper.newInstance(clz, aoParam)
                    : ClassHelper.invokeStatic(clz, sMethod, aoParam);
            }
        catch (Throwable e)
            {
            String sMsg;

            if (clz == null)
                {
                sMsg = "Unable to load class \"" + sClass + "\" using " + loader;
                }
            else
                {
                StringBuffer sbArgs = new StringBuffer();

                if (aoParam != null)
                    {
                    for (int i = 0, c = aoParam.length; i < c; i++)
                        {
                        sbArgs.append(ClassHelper.getSimpleName(aoParam[i].getClass()));

                        if (i < c - 1)
                            {
                            sbArgs.append(',');
                            }
                        }
                    }

                if (sMethod == null)
                    {
                    sMsg = "Missing or inaccessible constructor \"" + sClass
                        + "(" + sbArgs + ")\"";
                    }
                else
                    {
                    sMsg = "Failed to instantiate class using factory method \""
                        + sClass + "." + sMethod + "(" + sbArgs + ")\"";
                    }
                }

            throw ensureRuntimeException(e, sMsg + "\n" + xml);
            }

        // validate the object
        if (oInstance == null)
            {
            throw new IllegalArgumentException("Configuration element \""
                    + xml.getName() + "\" produced a null object");
            }
        else if (clzAssignable != null && !clzAssignable.isInstance(oInstance))
            {
            throw new IllegalArgumentException("The instance of class \""
                    + oInstance.getClass().getName()
                    + "\" produced from configuration element \""
                    + xml.getName()
                    + "\" is not an instance of \""
                    + clzAssignable.getName()
                    + '"');
            }

        // configure the object
        if (oInstance instanceof XmlConfigurable && xmlParams != null)
            {
            ((XmlConfigurable) oInstance).setConfig(XmlHelper
                    .transformInitParams(new SimpleElement("config"),
                    xmlParams));
            }

        return oInstance;
        }


    // ----- Object method helpers ------------------------------------------

    /**
    * Provide a hash value for the XML element and all of its contained
    * information.  The hash value is defined as a xor of the following:
    * <ul>
    * <li> the hashCode from the element's value (i.e. super.hashCode())
    * <li> the hashCode from each attribute name
    * <li> the hashCode from each attribute value
    * <li> the hashCode from each sub-element
    * </ul>
    *
    * @param xml  the XML element
    *
    * @return the hash value for the XML element
    */
    public static int hashElement(XmlElement xml)
        {
        // start with the hashCode() from the element's value
        int n = hashValue(xml);

        for (Iterator iter = xml.getAttributeMap().entrySet().iterator(); iter.hasNext(); )
            {
            // entry.hashCode() is a xor of the key and value, which is
            // the attribute name and value
            n ^= iter.next().hashCode();
            }

        for (Iterator iter = xml.getElementList().iterator(); iter.hasNext(); )
            {
            // xor in the hashCode() of each sub-element
            n ^= iter.next().hashCode();
            }

        return n;
        }

    /**
    * Provide a hash value for the XML value.  The hash value is defined
    * as one of the following:
    * <ol>
    * <li> 0 if getValue() returns null
    * <li> otherwise the hash value is the hashCode() of the string
    *      representation of the value
    * </ol>
    * @param val  the XML value
    *
    * @return the hash value for the XML value
    */
    public static int hashValue(XmlValue val)
        {
        Object o = val.getValue();

        if (o == null)
            {
            return 0;
            }

        String s = o instanceof String ? (String) o
                                       : (String) convert(o, XmlValue.TYPE_STRING);

        return s.hashCode();
        }

    /**
    * Compare one XML element with another XML element for equality.
    *
    * @param xml1  a non-null XmlElement object
    * @param xml2  a non-null XmlElement object
    *
    * @return true if the elements are equal, false otherwise
    */
    public static boolean equalsElement(XmlElement xml1, XmlElement xml2)
        {
        azzert(xml1 != null && xml2 != null, "Null element");

        // name
        if (!equals(xml1.getName(), xml2.getName()))
            {
            return false;
            }

        // comment
        if (!equals(xml1.getComment(), xml2.getComment()))
            {
            return false;
            }

        // value
        if (!equalsValue(xml1, xml2))
            {
            return false;
            }

        // attributes
        if (!equals(xml1.getAttributeMap(), xml2.getAttributeMap()))
            {
            return false;
            }

        // children
        if (!equals(xml1.getElementList(), xml2.getElementList()))
            {
            return false;
            }

        return true;
        }

    /**
    * Compare one XML value with another XML value for equality.
    *
    * @param val1  a non-null XmlValue object
    * @param val2  a non-null XmlValue object
    *
    * @return true if the values are equal, false otherwise
    */
    public static boolean equalsValue(XmlValue val1, XmlValue val2)
        {
        azzert(val1 != null && val2 != null, "Null value");

        boolean fEmpty1 = val1.isEmpty();
        boolean fEmpty2 = val2.isEmpty();
        if (fEmpty1 || fEmpty2)
            {
            return fEmpty1 && fEmpty2;
            }

        Object o1 = val1.getValue();
        Object o2 = val2.getValue();
        azzert(o1 != null && o2 != null);

        if (o1.getClass() == o2.getClass())
            {
            return o1.equals(o2);
            }

        return convert(o1, XmlValue.TYPE_STRING).equals(convert(o2, XmlValue.TYPE_STRING));
        }

    /**
    * Return the XML of an XmlSerializable object as a String.
    *
    * @param xml  an object that can serialize itself into XML
    *
    * @return a String description of the object
    */
    public static String toString(XmlSerializable xml)
        {
        Writer      writer  = new CharArrayWriter();
        PrintWriter out     = new PrintWriter(writer);

        xml.toXml().writeXml(out, true);

        out.flush();
        out.close();

        return writer.toString();
        }


    // ----- type conversions -----------------------------------------------

    /**
    * Convert the passed Object to the specified type.
    *
    * @param o      the object value or null
    * @param nType  the enumerated type to convert to
    *
    * @return an object of the specified type
    */
    public static Object convert(Object o, int nType)
        {
        if (o == null)
            {
            return null;
            }

        switch (nType)
            {
            case XmlValue.TYPE_BOOLEAN:
                {
                if (o instanceof Boolean)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null && s.length() > 0)
                    {
                    switch (trim(s).charAt(0))
                        {
                        case 'T':   // TRUE or True
                        case 't':   // true
                        case 'Y':   // YES or Yes
                        case 'y':   // yes or y
                            return Boolean.TRUE;

                        case '1':   // integer representation of true
                            if (s.length() == 1)
                                {
                                return Boolean.TRUE;
                                }
                            break;

                        case 'F':   // FALSE or False
                        case 'f':   // false
                        case 'N':   // NO or No
                        case 'n':   // no
                            return Boolean.FALSE;

                        case '0':   // integer representation of false
                            if (s.length() == 1)
                                {
                                return Boolean.FALSE;
                                }
                            break;
                        }
                    }

                return null;
                }

            case XmlValue.TYPE_INT:
                {
                if (o instanceof Integer)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null && s.length() > 0)
                    {
                    return Integer.valueOf(trim(s));
                    }

                return null;
                }

            case XmlValue.TYPE_LONG:
                {
                if (o instanceof Long)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null && s.length() > 0)
                    {
                    return Long.valueOf(trim(s));
                    }

                return null;
                }

            case XmlValue.TYPE_DOUBLE:
                {
                if (o instanceof Double)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null && s.length() > 0)
                    {
                    return Double.valueOf(trim(s));
                    }

                return null;
                }

            case XmlValue.TYPE_DECIMAL:
                {
                if (o instanceof BigDecimal)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null && s.length() > 0)
                    {
                    return new BigDecimal(trim(s));
                    }

                return null;
                }

            case XmlValue.TYPE_STRING:
                {
                if (o instanceof String)
                    {
                    return o;
                    }

                if (o instanceof Binary)
                    {
                    Binary bin = (Binary) o;
                    try
                        {
                        CharArrayWriter writer = new CharArrayWriter((int) (bin.length() * 1.35 + 2));
                        OutputStream    stream = new Base64OutputStream(writer);
                        bin.writeTo(stream);
                        stream.close();
                        return writer.toString();
                        }
                    catch (IOException e)
                        {
                        throw ensureRuntimeException(e);
                        }
                    }

                return o.toString();
                }

            case XmlValue.TYPE_BINARY:
                {
                if (o instanceof Binary)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null)
                    {
                    if (s.length() == 0)
                        {
                        return Binary.NO_BINARY;
                        }

                    try
                        {
                        return new Binary(Base64InputStream.decode(s.toCharArray()));
                        }
                    catch (RuntimeException e) {}
                    }

                return null;
                }

            case XmlValue.TYPE_DATE:
                {
                if (o instanceof Date)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null && s.length() > 0)
                    {
                    return Date.valueOf(trim(s));
                    }

                return null;
                }

            case XmlValue.TYPE_TIME:
                {
                if (o instanceof Time)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null && s.length() > 0)
                    {
                    return Time.valueOf(trim(s));
                    }

                return null;
                }

            case XmlValue.TYPE_DATETIME:
                {
                if (o instanceof Timestamp)
                    {
                    return o;
                    }

                String s = (String) convert(o, XmlValue.TYPE_STRING);
                if (s != null && s.length() > 0)
                    {
                    s = trim(s);
                    int ofDelim = s.indexOf('T');
                    if (ofDelim >= 0)
                        {
                        s = s.substring(0, ofDelim) + ' ' + s.substring(ofDelim + 1);
                        }
                    return Timestamp.valueOf(s);
                    }

                return null;
                }

            default:
                azzert();
            }

        return o;
        }


    // ----- constants  -----------------------------------------------------

    /**
    * Hexadecimal digits.
    */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    }
