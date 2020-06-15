/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.dev.compiler.Token;
import com.tangosol.dev.compiler.CompilerErrorInfo;
import com.tangosol.dev.compiler.CompilerException;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;


/**
* Represents an XML language token.
*
* @version 1.00, 07/16/01
* @author Cameron Purdy
*/
public class XmlToken
        extends Base
        implements Token
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Constructs an XML token without position/length information.  This is
    * used internally to construct the pre-defined tokens which can later
    * be cloned to produce instances of those tokens.
    *
    * @param nCategory the category of the token
    * @param nSubCategory the subcategory of the token (or NONE)
    * @param nTokenID the enumerated token ID (or NONE)
    * @param oValue the value of the token (or null)
    * @param sText the text of the token
    */
    public XmlToken(int nCategory, int nSubCategory, int nTokenID, Object oValue, String sText)
        {
        this(nCategory, nSubCategory, nTokenID, oValue, sText, -1, -1, -1);
        }

    /**
    * Constructs an XML token based on an existing java token; this is like
    * cloning a token, but allows the caller to provide position/length
    * information.
    *
    * @param that the token to clone from
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public XmlToken(XmlToken that, int iLine, int ofInLine, int cchToken)
        {
        this(that.m_cat, that.m_subcat, that.m_id, that.m_value, that.m_text, iLine, ofInLine, cchToken);
        }

    /**
    * Constructs an XML token.
    *
    * @param nCategory the category of the token
    * @param nSubCategory the subcategory of the token (or NONE)
    * @param nTokenID the enumerated token ID (or NONE)
    * @param oValue the value of the token (or null)
    * @param sText the text of the token
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public XmlToken(int nCategory, int nSubCategory, int nTokenID, Object oValue, String sText, int iLine, int ofInLine, int cchToken)
        {
        m_cat     = nCategory;
        m_subcat  = nSubCategory;
        m_id      = nTokenID;
        m_value   = oValue;
        m_text    = sText;
        m_line    = iLine;
        m_offset  = ofInLine;
        m_length  = cchToken;
        }


    // ----- token interface ------------------------------------------------

    /**
    * Determines the category that a token belongs to.  Java has five token
    * categories, which are defined in this abstract class.
    *
    * @return the category of the token
    */
    public int getCategory()
        {
        return m_cat;
        }

    /**
    * Determines the sub-category that a token belongs to.  In Java, only
    * the literal tokens have sub-categories.
    *
    * @return the sub-category of the token or 0 if sub-categorization is
    *         not applicable to this token
    */
    public int getSubCategory()
        {
        return m_subcat;
        }

    /**
    * Determines the unique identifier of a predefined token.  In Java,
    * the keywords, operators, separators, boolean and null literals are
    * the only pre-defined tokens.
    *
    * @return the token identifier, or 0 if the token does not have an ID
    */
    public int getID()
        {
        return m_id;
        }

    /**
    * Provides the value of the token if applicable.  This is used for
    * for Java literals.
    *
    * @return the value of the token or null if the token has no value
    */
    public Object getValue()
        {
        return m_value;
        }

    /**
    * Determines the line of the script which contains the token.  The
    * line number is zero-based and corresponds to the line in which the
    * first character of the token was encountered.
    *
    * @return the token's line number
    */
    public int getLine()
        {
        return m_line;
        }

    /**
    * Determines the offset of the token within the line of the script.
    * The offset is zero-based and specifies the location of the first
    * character of the token.
    *
    * @return the token's offset within the line
    */
    public int getOffset()
        {
        return m_offset;
        }

    /**
    * Determines the length, in characters, of the token.  The length of the
    * token is calculated as the difference between the offset of the first
    * character following the token and the offset of the first character of
    * the token itself.
    *
    * @return the length of the token
    */
    public int getLength()
        {
        return m_length;
        }

    /**
    * Adjusts the location (line/offset) of the token relative to it current
    * location.
    *
    * @param dLine
    * @param dOffset
    */
    public void adjust(int dLine, int dOffset)
        {
        m_line += dLine;
        if (m_line < 0)
            {
            m_line = 0;
            }

        m_offset += dOffset;
        if (m_offset < 0)
            {
            m_offset = 0;
            }
        }

    /**
    * Provides a string representation of the token as it would appear in
    * a script.  In other words, this method reverse-engineers the token.
    *
    * @return the Java token as it would appear in a script
    */
    public String getText()
        {
        return m_text;
        }

    /**
    * Provides a human-readable description of the token, including any
    * category and sub-category information.
    *
    * @return a human-readable description of the token
    */
    public String toString()
        {
        StringBuilder sb = new StringBuilder("(");

        // add the category (always applicable)
        sb.append(CATEGORY[getCategory()]).append(':');

        // location/length info
        sb.append(getText()).append(" on Line:").append(getLine()).append(" at Offset:")
                .append(getOffset()).append(")");

        // add the sub-category (if applicable)
        if (getSubCategory() != NONE)
            {
            sb.append(" ").append("sub-token:").append(SUBCATEGORY[getCategory()][getSubCategory()]);
            }

        // add the token's description of itself
        return sb.toString();
        }


    // ----- error logging --------------------------------------------------

    /**
    * Logs the passed error in the error list.
    *
    * @param nSeverity  severity of the error as defined by ErrorList.Constants
    * @param sCode      error code, as defined by the class logging the error
    * @param asParams   replaceable parameters for the error message
    * @param errlist    the error list
    *
    * @exception CompilerException If the error list overflows.
    */
    public void logError(int nSeverity, String sCode, String[] asParams, ErrorList errlist)
            throws CompilerException
        {
        if (errlist != null)
            {
            try
                {
                errlist.add(new CompilerErrorInfo(nSeverity, sCode,
                        XmlTokenizer.RESOURCES, asParams,
                        getLine(), getOffset(), getLength()));
                }
            catch (ErrorList.OverflowException e)
                {
                throw new CompilerException();
                }
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Return a token description based on a token ID.
    *
    * @param id  the token id
    *
    * @return  the token description
    */
    public static String getDescription(int id)
        {
        return DESC[id];
        }


    // ----- categories/sub-categories/token ids ----------------------------

    // categories
    /**
    * names
    */
    public static final int  CAT_NAME       = 1;
    /**
    * literal data
    */
    public static final int  CAT_LITERAL    = 2;
    /**
    * separators
    */
    public static final int  CAT_SEPARATOR  = 3;
    /**
    * DTD stuff is ignored
    */
    public static final int  CAT_DTDSTUFF   = 4;

    // sub-categories
    /**
    * literal data: processing instruction data
    */
    public static final int  LIT_PI         = 1;
    /**
    * literal data: comment data
    */
    public static final int  LIT_COMMENT    = 2;
    /**
    * literal data: quoted data (e.g. attribute values)
    */
    public static final int  LIT_QUOTED     = 3;
    /**
    * literal data: character data (e.g. element values)
    */
    public static final int  LIT_CHARDATA   = 4;

    // token ids
    public static final int  NONE           =  0; // pseudo (e.g. EOF)
    public static final int  NAME           =  1;
    public static final int  LITERAL        =  2;
    public static final int  CHARDATA       =  3;
    public static final int  CHARDATA_RAW   =  4;
    public static final int  COMMENT        =  5;
    public static final int  PI_START       =  6; // "<?"
    public static final int  PI_STOP        =  7; // "?>"
    public static final int  DOCTYPE_START  =  8; // "<!DOCTYPE"
    public static final int  ELEMENT_START  =  9; // "<"
    public static final int  ELEMENT_STOP   = 10; // ">"
    public static final int  ENDTAG_START   = 11; // "</"
    public static final int  EMPTY_STOP     = 12; // "/>"
    public static final int  COMMENT_START  = 13; // "<!--"
    public static final int  COMMENT_STOP   = 14; // "-->"
    public static final int  EQUALS         = 15; // "="
    public static final int  DTD_DECL_START = 16; // "["
    public static final int  DTD_DECL_STOP  = 17; // "]"
    public static final int  DTD_ELEMENT    = 18; // "<!ELEMENT"
    public static final int  DTD_ATTLIST    = 19; // "<!ATTLIST"
    public static final int  DTD_ENTITY     = 20; // "<!ENTITY"
    public static final int  DTD_NOTATION   = 21; // "<!NOTATION"
    public static final int  DTD_DECLSEP    = 22; // e.g. "%name;"


    // ----- category/sub-category/token descriptions -----------------------

    /**
    * Token category descriptions.  This array is subscripted by the
    * enumeration of Java token categories.
    */
    protected static final String[] CATEGORY =
        {
        null,
        "Name",
        "Literal",
        "Separator",
        "DTD Stuff",
        };

    /**
    * Token sub-category descriptions.  This two-dimensional array is
    * subscripted by a combination of Java token category and sub-category.
    */
    protected static final String[][] SUBCATEGORY =
        {
        null,
        null,
        new String[]
            {
            null,
            "PI",
            "Comment",
            "Quoted",
            "Character Data",
            },
        null,
        null,
        };

    // descriptions of each token id
    protected static final String[] DESC =
        {
        "none",             // NONE
        "name",             //
        "literal",          //
        "character data",   //
        "CDATA",            //
        "comment",          //
        "<?",               // PI_START
        "?>",               // PI_STOP
        "<!DOCTYPE",        // DOCTYPE_START
        "<",                // ELEMENT_START
        ">",                // ELEMENT_STOP
        "</",               // ENDTAG_START
        "/>",               // EMPTY_STOP
        "<!--",             // COMMENT_START
        "-->",              // COMMENT_STOP
        "=",                // EQUALS
        "[",                // DTD_DECL_START
        "]",                // DTD_DECL_STOP
        "<!ELEMENT",        // DTD_ELEMENT
        "<!ATTLIST",        // DTD_ATTLIST
        "<!ENTITY",         // DTD_ENTITY
        "<!NOTATION",       // DTD_NOTATION
        "%name;",           // DTD_DECLSEP
        };


    // ----- "abstract" tokens ----------------------------------------------

    public static final XmlToken TOK_PI_START       = new XmlToken(CAT_SEPARATOR, NONE, PI_START      , null, DESC[PI_START      ]);
    public static final XmlToken TOK_PI_STOP        = new XmlToken(CAT_SEPARATOR, NONE, PI_STOP       , null, DESC[PI_STOP       ]);
    public static final XmlToken TOK_DOCTYPE_START  = new XmlToken(CAT_SEPARATOR, NONE, DOCTYPE_START , null, DESC[DOCTYPE_START ]);
    public static final XmlToken TOK_ELEMENT_START  = new XmlToken(CAT_SEPARATOR, NONE, ELEMENT_START , null, DESC[ELEMENT_START ]);
    public static final XmlToken TOK_ELEMENT_STOP   = new XmlToken(CAT_SEPARATOR, NONE, ELEMENT_STOP  , null, DESC[ELEMENT_STOP  ]);
    public static final XmlToken TOK_ENDTAG_START   = new XmlToken(CAT_SEPARATOR, NONE, ENDTAG_START  , null, DESC[ENDTAG_START  ]);
    public static final XmlToken TOK_EMPTY_STOP     = new XmlToken(CAT_SEPARATOR, NONE, EMPTY_STOP    , null, DESC[EMPTY_STOP    ]);
    public static final XmlToken TOK_COMMENT_START  = new XmlToken(CAT_SEPARATOR, NONE, COMMENT_START , null, DESC[COMMENT_START ]);
    public static final XmlToken TOK_COMMENT_STOP   = new XmlToken(CAT_SEPARATOR, NONE, COMMENT_STOP  , null, DESC[COMMENT_STOP  ]);
    public static final XmlToken TOK_EQUALS         = new XmlToken(CAT_SEPARATOR, NONE, EQUALS        , null, DESC[EQUALS        ]);
    public static final XmlToken TOK_DTD_DECL_START = new XmlToken(CAT_SEPARATOR, NONE, DTD_DECL_START, null, DESC[DTD_DECL_START]);
    public static final XmlToken TOK_DTD_DECL_STOP  = new XmlToken(CAT_SEPARATOR, NONE, DTD_DECL_STOP , null, DESC[DTD_DECL_STOP ]);
    public static final XmlToken TOK_DTD_ELEMENT    = new XmlToken(CAT_DTDSTUFF , NONE, DTD_ELEMENT   , null, DESC[DTD_ELEMENT   ]);
    public static final XmlToken TOK_DTD_ATTLIST    = new XmlToken(CAT_DTDSTUFF , NONE, DTD_ATTLIST   , null, DESC[DTD_ATTLIST   ]);
    public static final XmlToken TOK_DTD_ENTITY     = new XmlToken(CAT_DTDSTUFF , NONE, DTD_ENTITY    , null, DESC[DTD_ENTITY    ]);
    public static final XmlToken TOK_DTD_NOTATION   = new XmlToken(CAT_DTDSTUFF , NONE, DTD_NOTATION  , null, DESC[DTD_NOTATION  ]);


    // ----- data members ---------------------------------------------------

    private int    m_cat;
    private int    m_subcat;
    private int    m_id;

    private Object m_value;
    private String m_text;

    private int    m_line;
    private int    m_offset;
    private int    m_length;
    }
