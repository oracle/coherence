/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


import com.tangosol.dev.compiler.CompilerErrorInfo;
import com.tangosol.dev.compiler.CompilerException;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;

import java.util.Hashtable;


/**
* Represents a Java language token.
*
* @version 0.10, 11/21/96
* @version 0.50, 09/09/98
* @author 	Cameron Purdy
*/
public class Token
        extends Base
        implements com.tangosol.dev.compiler.Token, TokenConstants, Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Constructs a Java token without position/length information.  This is
    * used internally to construct the pre-defined tokens which can later
    * be cloned to produce instances of those tokens.
    *
    * @param nCategory the category of the token
    * @param nSubCategory the subcategory of the token (or TOK_NONE)
    * @param nTokenID the enumerated token ID (or TOK_NONE)
    * @param oValue the value of the token (or null)
    * @param sText the text of the token
    */
    public Token(int nCategory, int nSubCategory, int nTokenID, Object oValue, String sText)
        {
        this(nCategory, nSubCategory, nTokenID, oValue, sText, -1, -1, -1);
        }

    /**
    * Constructs a Java token based on an existing java token; this is like
    * cloning a token, but allows the caller to provide position/length
    * information.
    *
    * @param that the token to clone from
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public Token(Token that, int iLine, int ofInLine, int cchToken)
        {
        this(that.cat, that.subcat, that.id, that.value, that.text, iLine, ofInLine, cchToken);
        }

    /**
    * Constructs a Java token.
    *
    * @param nCategory the category of the token
    * @param nSubCategory the subcategory of the token (or TOK_NONE)
    * @param nTokenID the enumerated token ID (or TOK_NONE)
    * @param oValue the value of the token (or null)
    * @param sText the text of the token
    * @param iLine the line number of the token
    * @param ofInLine the offset of the token within the line
    * @param cchToken the length of the token
    */
    public Token(int nCategory, int nSubCategory, int nTokenID, Object oValue, String sText, int iLine, int ofInLine, int cchToken)
        {
        cat     = nCategory;
        subcat  = nSubCategory;
        id      = nTokenID;
        value   = oValue;
        text    = sText;
        line    = iLine;
        offset  = ofInLine;
        length  = cchToken;
        }


    // ----- token interface ------------------------------------------------

    /**
    * Determines the category that a token belongs to.  Java has five token
    * categories, which are defined in this abstract class.
    *
    * @return the category of the token
    *
    * @see TokenConstants#IDENTIFIER
    * @see TokenConstants#KEYWORD
    * @see TokenConstants#LITERAL
    * @see TokenConstants#SEPARATOR
    * @see TokenConstants#OPERATOR
    * @see TokenConstants#COMMENT
    */
    public int getCategory()
        {
        return cat;
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
        return subcat;
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
        return id;
        }

    /**
    * Provides the value of the token if applicable.  This is used for
    * for Java literals.
    *
    * @return the value of the token or null if the token has no value
    */
    public Object getValue()
        {
        return value;
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
        return line;
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
        return offset;
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
        return length;
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
        line += dLine;
        if (line < 0)
            {
            line = 0;
            }

        offset += dOffset;
        if (offset < 0)
            {
            offset = 0;
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
        return text;
        }

    /**
    * Provides a human-readable description of the token, including any
    * category and sub-category information.
    *
    * @return a human-readable description of the token
    */
    public String toString()
        {
        // location/length info
        StringBuffer sb = new StringBuffer("(" + getLine() + ',' + getOffset() + ',' + getLength() + ") ");

        // add the sub-category (if applicable)
        if (subcat != TOK_NONE)
            {
            sb.append(SUBCATEGORY[cat][subcat]).append(' ');
            }

        // add the category (always applicable)
        sb.append(CATEGORY[cat]).append(' ');

        // add the token's description of itself
        return sb.append(getText()).toString();
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
    protected void logError(int nSeverity, String sCode, String[] asParams, ErrorList errlist)
            throws CompilerException
        {
        if (errlist != null)
            {
            try
                {
                errlist.add(new CompilerErrorInfo(nSeverity, sCode, RESOURCES, asParams, line, offset, length));
                }
            catch (ErrorList.OverflowException e)
                {
                throw new CompilerException();
                }
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Determine if the passed string is a legal Java simple name.
    *
    * @param sName the string containing the name
    *
    * @return true if a legal name, false otherwise
    */
    public static boolean isSimpleNameLegal(String sName)
        {
        char[] ach = sName.toCharArray();
        int    cch = ach.length;

        // verify that the string is a lexically valid identifier
        if (cch < 1 || !Character.isJavaIdentifierStart(ach[0]))
            {
            return false;
            }

        for (int i = 1; i < cch; ++i)
            {
            if (!Character.isJavaIdentifierPart(ach[i]))
                {
                return false;
                }
            }

        // verify that the string is not a reserved word
        return (!RESERVED.containsKey(sName));
        }

    /**
    * Determine if the passed string is a legal dot-delimited identifier.
    *
    * @param sName the string containing the dot-delimited identifier
    *
    * @return true if a legal identifier, false otherwise
    */
    public static boolean isQualifiedNameLegal(String sName)
        {
        int cch = sName.length();
        if (cch < 1)
            {
            return false;
            }

        int ofStart = 0;
        while (ofStart < cch)
            {
            int ofEnd = sName.indexOf('.', ofStart);
            if (ofEnd == cch - 1)
                {
                // illegal:  ends with dot
                return false;
                }

            if (ofEnd < 0)
                {
                ofEnd = cch;
                }

            if (!isSimpleNameLegal(sName.substring(ofStart, ofEnd)))
                {
                return false;
                }

            ofStart = ofEnd + 1;
            }

        return true;
        }

    /**
    * Return a HashTable which contains all Java tokens that are reserved
    * words.  The reserved tokens are the keywords and literals that, were
    * they not reserved, could be used as identifiers.
    */
    public static Hashtable getReservedWords()
        {
        return (Hashtable) RESERVED.clone();
        }

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


    // ----- category/sub-category descriptions -----------------------------

    /**
    * Token category descriptions.  This array is subscripted by the
    * enumeration of Java token categories.
    */
    protected static final String[] CATEGORY =
        {
        null,
        "Identifier",
        "Keyword",
        "Literal",
        "Separator",
        "Operator",
        "Comment",
        };

    /**
    * Literal token sub-category descriptions.  This array is subscripted
    * by the enumeration of Java literal token sub-categories.
    */
    protected static final String[] LITERAL_SUBCATEGORY =
        {
        null,
        "Integer",
        "Integer",
        "Float",
        "Float",
        "Boolean",
        "Character",
        "String",
        "Null"
        };

    /**
    * Token sub-category descriptions.  This two-dimensional array is
    * subscripted by a combination of Java token category and sub-category.
    */
    protected static final String[][] SUBCATEGORY =
        {
        null,
        null,
        null,
        LITERAL_SUBCATEGORY,
        null,
        null
        };

    // descriptions of each token id
    protected static final String[] DESC =
        {
        "none",             // TOK_NONE
        "abstract",         // KEY_ABSTRACT
        "as",               // KEY_AS
        "boolean",          // KEY_BOOLEAN
        "break",            // KEY_BREAK
        "byte",             // KEY_BYTE
        "case",             // KEY_CASE
        "catch",            // KEY_CATCH
        "char",             // KEY_CHAR
        "class",            // KEY_CLASS
        "const",            // KEY_CONST
        "continue",         // KEY_CONTINUE
        "default",          // KEY_DEFAULT
        "do",               // KEY_DO
        "double",           // KEY_DOUBLE
        "else",             // KEY_ELSE
        "extends",          // KEY_EXTENDS
        "final",            // KEY_FINAL
        "finally",          // KEY_FINALLY
        "float",            // KEY_FLOAT
        "for",              // KEY_FOR
        "goto",             // KEY_GOTO
        "if",               // KEY_IF
        "implements",       // KEY_IMPLEMENTS
        "import",           // KEY_IMPORT
        "instanceof",       // KEY_INSTANCEOF
        "int",              // KEY_INT
        "interface",        // KEY_INTERFACE
        "long",             // KEY_LONG
        "native",           // KEY_NATIVE
        "new",              // KEY_NEW
        "package",          // KEY_PACKAGE
        "private",          // KEY_PRIVATE
        "protected",        // KEY_PROTECTED
        "public",           // KEY_PUBLIC
        "return",           // KEY_RETURN
        "short",            // KEY_SHORT
        "static",           // KEY_STATIC
        "strictfp",         // KEY_STRICTFP
        "super",            // KEY_SUPER
        "switch",           // KEY_SWITCH
        "synchronized",     // KEY_SYNCHRONIZED
        "this",             // KEY_THIS
        "throw",            // KEY_THROW
        "throws",           // KEY_THROWS
        "transient",        // KEY_TRANSIENT
        "try",              // KEY_TRY
        "void",             // KEY_VOID
        "volatile",         // KEY_VOLATILE
        "while",            // KEY_WHILE
        "(",                // SEP_LPARENTHESIS
        ")",                // SEP_RPARENTHESIS
        "{",                // SEP_LCURLYBRACE
        "}",                // SEP_RCURLYBRACE
        "[",                // SEP_LBRACKET
        "]",                // SEP_RBRACKET
        ";",                // SEP_SEMICOLON
        ",",                // SEP_COMMA
        ".",                // SEP_DOT
        "+",                // OP_ADD
        "-",                // OP_SUB
        "*",                // OP_MUL
        "/",                // OP_DIV
        "%",                // OP_REM
        "<<",               // OP_SHL
        ">>",               // OP_SHR
        ">>>",              // OP_USHR
        "&",                // OP_BITAND
        "|",                // OP_BITOR
        "^",                // OP_BITXOR
        "~",                // OP_BITNOT
        "=",                // OP_ASSIGN
        "+=",               // OP_ASSIGN_ADD
        "-=",               // OP_ASSIGN_SUB
        "*=",               // OP_ASSIGN_MUL
        "/=",               // OP_ASSIGN_DIV
        "%=",               // OP_ASSIGN_REM
        "<<=",              // OP_ASSIGN_SHL
        ">>=",              // OP_ASSIGN_SHR
        ">>>=",             // OP_ASSIGN_USHR
        "&=",               // OP_ASSIGN_BITAND
        "|=",               // OP_ASSIGN_BITOR
        "^=",               // OP_ASSIGN_BITXOR
        "==",               // OP_TEST_EQ
        "!=",               // OP_TEST_NE
        ">",                // OP_TEST_GT
        ">=",               // OP_TEST_GE
        "<",                // OP_TEST_LT
        "<=",               // OP_TEST_LE
        "&&",               // OP_LOGICAL_AND
        "||",               // OP_LOGICAL_OR
        "!",                // OP_LOGICAL_NOT
        "++",               // OP_INCREMENT
        "--",               // OP_DECREMENT
        "?",                // OP_CONDITIONAL
        ":",                // OP_COLON
        "null",             // LIT_NULL
        "true",             // LIT_TRUE
        "false",            // LIT_FALSE
        "char literal",     // LIT_CHAR
        "int literal",      // LIT_INT
        "long literal",     // LIT_LONG
        "float literal",    // LIT_FLOAT
        "double literal",   // LIT_DOUBLE
        "string literal",   // LIT_STRING
        "identifier",       // IDENT
        "single-line",      // CMT_SINGLELINE
        "traditional",      // CMT_TRADITIONAL
        "documentation",    // CMT_DOCUMENTATION
        };

    // ----- predefined Java tokens -----------------------------------------

    // these "abstract" tokens can be used to copy-construct new tokens

    public static final Token TOK_ABSTRACT      = new Token(KEYWORD  , TOK_NONE, KEY_ABSTRACT    , null, DESC[KEY_ABSTRACT    ]);
    public static final Token TOK_AS            = new Token(KEYWORD  , TOK_NONE, KEY_AS          , null, DESC[KEY_AS          ]);
    public static final Token TOK_BOOLEAN       = new Token(KEYWORD  , TOK_NONE, KEY_BOOLEAN     , null, DESC[KEY_BOOLEAN     ]);
    public static final Token TOK_BREAK         = new Token(KEYWORD  , TOK_NONE, KEY_BREAK       , null, DESC[KEY_BREAK       ]);
    public static final Token TOK_BYTE          = new Token(KEYWORD  , TOK_NONE, KEY_BYTE        , null, DESC[KEY_BYTE        ]);
    public static final Token TOK_CASE          = new Token(KEYWORD  , TOK_NONE, KEY_CASE        , null, DESC[KEY_CASE        ]);
    public static final Token TOK_CATCH         = new Token(KEYWORD  , TOK_NONE, KEY_CATCH       , null, DESC[KEY_CATCH       ]);
    public static final Token TOK_CHAR          = new Token(KEYWORD  , TOK_NONE, KEY_CHAR        , null, DESC[KEY_CHAR        ]);
    public static final Token TOK_CLASS         = new Token(KEYWORD  , TOK_NONE, KEY_CLASS       , null, DESC[KEY_CLASS       ]);
    public static final Token TOK_CONST         = new Token(KEYWORD  , TOK_NONE, KEY_CONST       , null, DESC[KEY_CONST       ]);
    public static final Token TOK_CONTINUE      = new Token(KEYWORD  , TOK_NONE, KEY_CONTINUE    , null, DESC[KEY_CONTINUE    ]);
    public static final Token TOK_DEFAULT       = new Token(KEYWORD  , TOK_NONE, KEY_DEFAULT     , null, DESC[KEY_DEFAULT     ]);
    public static final Token TOK_DO            = new Token(KEYWORD  , TOK_NONE, KEY_DO          , null, DESC[KEY_DO          ]);
    public static final Token TOK_DOUBLE        = new Token(KEYWORD  , TOK_NONE, KEY_DOUBLE      , null, DESC[KEY_DOUBLE      ]);
    public static final Token TOK_ELSE          = new Token(KEYWORD  , TOK_NONE, KEY_ELSE        , null, DESC[KEY_ELSE        ]);
    public static final Token TOK_EXTENDS       = new Token(KEYWORD  , TOK_NONE, KEY_EXTENDS     , null, DESC[KEY_EXTENDS     ]);
    public static final Token TOK_FINAL         = new Token(KEYWORD  , TOK_NONE, KEY_FINAL       , null, DESC[KEY_FINAL       ]);
    public static final Token TOK_FINALLY       = new Token(KEYWORD  , TOK_NONE, KEY_FINALLY     , null, DESC[KEY_FINALLY     ]);
    public static final Token TOK_FLOAT         = new Token(KEYWORD  , TOK_NONE, KEY_FLOAT       , null, DESC[KEY_FLOAT       ]);
    public static final Token TOK_FOR           = new Token(KEYWORD  , TOK_NONE, KEY_FOR         , null, DESC[KEY_FOR         ]);
    public static final Token TOK_GOTO          = new Token(KEYWORD  , TOK_NONE, KEY_GOTO        , null, DESC[KEY_GOTO        ]);
    public static final Token TOK_IF            = new Token(KEYWORD  , TOK_NONE, KEY_IF          , null, DESC[KEY_IF          ]);
    public static final Token TOK_IMPLEMENTS    = new Token(KEYWORD  , TOK_NONE, KEY_IMPLEMENTS  , null, DESC[KEY_IMPLEMENTS  ]);
    public static final Token TOK_IMPORT        = new Token(KEYWORD  , TOK_NONE, KEY_IMPORT      , null, DESC[KEY_IMPORT      ]);
    public static final Token TOK_INSTANCEOF    = new Token(KEYWORD  , TOK_NONE, KEY_INSTANCEOF  , null, DESC[KEY_INSTANCEOF  ]);
    public static final Token TOK_INT           = new Token(KEYWORD  , TOK_NONE, KEY_INT         , null, DESC[KEY_INT         ]);
    public static final Token TOK_INTERFACE     = new Token(KEYWORD  , TOK_NONE, KEY_INTERFACE   , null, DESC[KEY_INTERFACE   ]);
    public static final Token TOK_LONG          = new Token(KEYWORD  , TOK_NONE, KEY_LONG        , null, DESC[KEY_LONG        ]);
    public static final Token TOK_NATIVE        = new Token(KEYWORD  , TOK_NONE, KEY_NATIVE      , null, DESC[KEY_NATIVE      ]);
    public static final Token TOK_NEW           = new Token(KEYWORD  , TOK_NONE, KEY_NEW         , null, DESC[KEY_NEW         ]);
    public static final Token TOK_PACKAGE       = new Token(KEYWORD  , TOK_NONE, KEY_PACKAGE     , null, DESC[KEY_PACKAGE     ]);
    public static final Token TOK_PRIVATE       = new Token(KEYWORD  , TOK_NONE, KEY_PRIVATE     , null, DESC[KEY_PRIVATE     ]);
    public static final Token TOK_PROTECTED     = new Token(KEYWORD  , TOK_NONE, KEY_PROTECTED   , null, DESC[KEY_PROTECTED   ]);
    public static final Token TOK_PUBLIC        = new Token(KEYWORD  , TOK_NONE, KEY_PUBLIC      , null, DESC[KEY_PUBLIC      ]);
    public static final Token TOK_RETURN        = new Token(KEYWORD  , TOK_NONE, KEY_RETURN      , null, DESC[KEY_RETURN      ]);
    public static final Token TOK_SHORT         = new Token(KEYWORD  , TOK_NONE, KEY_SHORT       , null, DESC[KEY_SHORT       ]);
    public static final Token TOK_STATIC        = new Token(KEYWORD  , TOK_NONE, KEY_STATIC      , null, DESC[KEY_STATIC      ]);
    public static final Token TOK_STRICTFP      = new Token(KEYWORD  , TOK_NONE, KEY_STRICTFP    , null, DESC[KEY_STRICTFP    ]);
    public static final Token TOK_SUPER         = new Token(KEYWORD  , TOK_NONE, KEY_SUPER       , null, DESC[KEY_SUPER       ]);
    public static final Token TOK_SWITCH        = new Token(KEYWORD  , TOK_NONE, KEY_SWITCH      , null, DESC[KEY_SWITCH      ]);
    public static final Token TOK_SYNCHRONIZED  = new Token(KEYWORD  , TOK_NONE, KEY_SYNCHRONIZED, null, DESC[KEY_SYNCHRONIZED]);
    public static final Token TOK_THIS          = new Token(KEYWORD  , TOK_NONE, KEY_THIS        , null, DESC[KEY_THIS        ]);
    public static final Token TOK_THROW         = new Token(KEYWORD  , TOK_NONE, KEY_THROW       , null, DESC[KEY_THROW       ]);
    public static final Token TOK_THROWS        = new Token(KEYWORD  , TOK_NONE, KEY_THROWS      , null, DESC[KEY_THROWS      ]);
    public static final Token TOK_TRANSIENT     = new Token(KEYWORD  , TOK_NONE, KEY_TRANSIENT   , null, DESC[KEY_TRANSIENT   ]);
    public static final Token TOK_TRY           = new Token(KEYWORD  , TOK_NONE, KEY_TRY         , null, DESC[KEY_TRY         ]);
    public static final Token TOK_VOID          = new Token(KEYWORD  , TOK_NONE, KEY_VOID        , null, DESC[KEY_VOID        ]);
    public static final Token TOK_VOLATILE      = new Token(KEYWORD  , TOK_NONE, KEY_VOLATILE    , null, DESC[KEY_VOLATILE    ]);
    public static final Token TOK_WHILE         = new Token(KEYWORD  , TOK_NONE, KEY_WHILE       , null, DESC[KEY_WHILE       ]);
    public static final Token TOK_NULL          = new Token(LITERAL  , NULL    , LIT_NULL        , null, DESC[LIT_NULL        ]);
    public static final Token TOK_TRUE          = new Token(LITERAL  , BOOL    , LIT_TRUE ,Boolean.TRUE, DESC[LIT_TRUE        ]);
    public static final Token TOK_FALSE         = new Token(LITERAL  , BOOL    , LIT_FALSE,Boolean.FALSE,DESC[LIT_FALSE       ]);
    public static final Token TOK_LPARENTHESIS  = new Token(SEPARATOR, TOK_NONE, SEP_LPARENTHESIS, null, DESC[SEP_LPARENTHESIS]);
    public static final Token TOK_RPARENTHESIS  = new Token(SEPARATOR, TOK_NONE, SEP_RPARENTHESIS, null, DESC[SEP_RPARENTHESIS]);
    public static final Token TOK_LCURLYBRACE   = new Token(SEPARATOR, TOK_NONE, SEP_LCURLYBRACE , null, DESC[SEP_LCURLYBRACE ]);
    public static final Token TOK_RCURLYBRACE   = new Token(SEPARATOR, TOK_NONE, SEP_RCURLYBRACE , null, DESC[SEP_RCURLYBRACE ]);
    public static final Token TOK_LBRACKET      = new Token(SEPARATOR, TOK_NONE, SEP_LBRACKET    , null, DESC[SEP_LBRACKET    ]);
    public static final Token TOK_RBRACKET      = new Token(SEPARATOR, TOK_NONE, SEP_RBRACKET    , null, DESC[SEP_RBRACKET    ]);
    public static final Token TOK_SEMICOLON     = new Token(SEPARATOR, TOK_NONE, SEP_SEMICOLON   , null, DESC[SEP_SEMICOLON   ]);
    public static final Token TOK_COMMA         = new Token(SEPARATOR, TOK_NONE, SEP_COMMA       , null, DESC[SEP_COMMA       ]);
    public static final Token TOK_DOT           = new Token(SEPARATOR, TOK_NONE, SEP_DOT         , null, DESC[SEP_DOT         ]);
    public static final Token TOK_ADD           = new Token(OPERATOR , TOK_NONE, OP_ADD          , null, DESC[OP_ADD          ]);
    public static final Token TOK_SUB           = new Token(OPERATOR , TOK_NONE, OP_SUB          , null, DESC[OP_SUB          ]);
    public static final Token TOK_MUL           = new Token(OPERATOR , TOK_NONE, OP_MUL          , null, DESC[OP_MUL          ]);
    public static final Token TOK_DIV           = new Token(OPERATOR , TOK_NONE, OP_DIV          , null, DESC[OP_DIV          ]);
    public static final Token TOK_REM           = new Token(OPERATOR , TOK_NONE, OP_REM          , null, DESC[OP_REM          ]);
    public static final Token TOK_SHL           = new Token(OPERATOR , TOK_NONE, OP_SHL          , null, DESC[OP_SHL          ]);
    public static final Token TOK_SHR           = new Token(OPERATOR , TOK_NONE, OP_SHR          , null, DESC[OP_SHR          ]);
    public static final Token TOK_USHR          = new Token(OPERATOR , TOK_NONE, OP_USHR         , null, DESC[OP_USHR         ]);
    public static final Token TOK_BITAND        = new Token(OPERATOR , TOK_NONE, OP_BITAND       , null, DESC[OP_BITAND       ]);
    public static final Token TOK_BITOR         = new Token(OPERATOR , TOK_NONE, OP_BITOR        , null, DESC[OP_BITOR        ]);
    public static final Token TOK_BITXOR        = new Token(OPERATOR , TOK_NONE, OP_BITXOR       , null, DESC[OP_BITXOR       ]);
    public static final Token TOK_BITNOT        = new Token(OPERATOR , TOK_NONE, OP_BITNOT       , null, DESC[OP_BITNOT       ]);
    public static final Token TOK_ASSIGN        = new Token(OPERATOR , TOK_NONE, OP_ASSIGN       , null, DESC[OP_ASSIGN       ]);
    public static final Token TOK_ASSIGN_ADD    = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_ADD   , null, DESC[OP_ASSIGN_ADD   ]);
    public static final Token TOK_ASSIGN_SUB    = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_SUB   , null, DESC[OP_ASSIGN_SUB   ]);
    public static final Token TOK_ASSIGN_MUL    = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_MUL   , null, DESC[OP_ASSIGN_MUL   ]);
    public static final Token TOK_ASSIGN_DIV    = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_DIV   , null, DESC[OP_ASSIGN_DIV   ]);
    public static final Token TOK_ASSIGN_REM    = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_REM   , null, DESC[OP_ASSIGN_REM   ]);
    public static final Token TOK_ASSIGN_SHL    = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_SHL   , null, DESC[OP_ASSIGN_SHL   ]);
    public static final Token TOK_ASSIGN_SHR    = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_SHR   , null, DESC[OP_ASSIGN_SHR   ]);
    public static final Token TOK_ASSIGN_USHR   = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_USHR  , null, DESC[OP_ASSIGN_USHR  ]);
    public static final Token TOK_ASSIGN_BITAND = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_BITAND, null, DESC[OP_ASSIGN_BITAND]);
    public static final Token TOK_ASSIGN_BITOR  = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_BITOR , null, DESC[OP_ASSIGN_BITOR ]);
    public static final Token TOK_ASSIGN_BITXOR = new Token(OPERATOR , TOK_NONE, OP_ASSIGN_BITXOR, null, DESC[OP_ASSIGN_BITXOR]);
    public static final Token TOK_TEST_EQ       = new Token(OPERATOR , TOK_NONE, OP_TEST_EQ      , null, DESC[OP_TEST_EQ      ]);
    public static final Token TOK_TEST_NE       = new Token(OPERATOR , TOK_NONE, OP_TEST_NE      , null, DESC[OP_TEST_NE      ]);
    public static final Token TOK_TEST_GT       = new Token(OPERATOR , TOK_NONE, OP_TEST_GT      , null, DESC[OP_TEST_GT      ]);
    public static final Token TOK_TEST_GE       = new Token(OPERATOR , TOK_NONE, OP_TEST_GE      , null, DESC[OP_TEST_GE      ]);
    public static final Token TOK_TEST_LT       = new Token(OPERATOR , TOK_NONE, OP_TEST_LT      , null, DESC[OP_TEST_LT      ]);
    public static final Token TOK_TEST_LE       = new Token(OPERATOR , TOK_NONE, OP_TEST_LE      , null, DESC[OP_TEST_LE      ]);
    public static final Token TOK_LOGICAL_AND   = new Token(OPERATOR , TOK_NONE, OP_LOGICAL_AND  , null, DESC[OP_LOGICAL_AND  ]);
    public static final Token TOK_LOGICAL_OR    = new Token(OPERATOR , TOK_NONE, OP_LOGICAL_OR   , null, DESC[OP_LOGICAL_OR   ]);
    public static final Token TOK_LOGICAL_NOT   = new Token(OPERATOR , TOK_NONE, OP_LOGICAL_NOT  , null, DESC[OP_LOGICAL_NOT  ]);
    public static final Token TOK_INCREMENT     = new Token(OPERATOR , TOK_NONE, OP_INCREMENT    , null, DESC[OP_INCREMENT    ]);
    public static final Token TOK_DECREMENT     = new Token(OPERATOR , TOK_NONE, OP_DECREMENT    , null, DESC[OP_DECREMENT    ]);
    public static final Token TOK_CONDITIONAL   = new Token(OPERATOR , TOK_NONE, OP_CONDITIONAL  , null, DESC[OP_CONDITIONAL  ]);
    public static final Token TOK_COLON         = new Token(OPERATOR , TOK_NONE, OP_COLON        , null, DESC[OP_COLON        ]);

    static
        {
        RESERVED.put(TOK_ABSTRACT    .getText(), TOK_ABSTRACT);
        RESERVED.put(TOK_AS          .getText(), TOK_AS);
        RESERVED.put(TOK_BOOLEAN     .getText(), TOK_BOOLEAN);
        RESERVED.put(TOK_BREAK       .getText(), TOK_BREAK);
        RESERVED.put(TOK_BYTE        .getText(), TOK_BYTE);
        RESERVED.put(TOK_CASE        .getText(), TOK_CASE);
        RESERVED.put(TOK_CATCH       .getText(), TOK_CATCH);
        RESERVED.put(TOK_CHAR        .getText(), TOK_CHAR);
        RESERVED.put(TOK_CLASS       .getText(), TOK_CLASS);
        RESERVED.put(TOK_CONST       .getText(), TOK_CONST);
        RESERVED.put(TOK_CONTINUE    .getText(), TOK_CONTINUE);
        RESERVED.put(TOK_DEFAULT     .getText(), TOK_DEFAULT);
        RESERVED.put(TOK_DO          .getText(), TOK_DO);
        RESERVED.put(TOK_DOUBLE      .getText(), TOK_DOUBLE);
        RESERVED.put(TOK_ELSE        .getText(), TOK_ELSE);
        RESERVED.put(TOK_EXTENDS     .getText(), TOK_EXTENDS);
        RESERVED.put(TOK_FALSE       .getText(), TOK_FALSE);
        RESERVED.put(TOK_FINAL       .getText(), TOK_FINAL);
        RESERVED.put(TOK_FINALLY     .getText(), TOK_FINALLY);
        RESERVED.put(TOK_FLOAT       .getText(), TOK_FLOAT);
        RESERVED.put(TOK_FOR         .getText(), TOK_FOR);
        RESERVED.put(TOK_GOTO        .getText(), TOK_GOTO);
        RESERVED.put(TOK_IF          .getText(), TOK_IF);
        RESERVED.put(TOK_IMPLEMENTS  .getText(), TOK_IMPLEMENTS);
        RESERVED.put(TOK_IMPORT      .getText(), TOK_IMPORT);
        RESERVED.put(TOK_INSTANCEOF  .getText(), TOK_INSTANCEOF);
        RESERVED.put(TOK_INT         .getText(), TOK_INT);
        RESERVED.put(TOK_INTERFACE   .getText(), TOK_INTERFACE);
        RESERVED.put(TOK_LONG        .getText(), TOK_LONG);
        RESERVED.put(TOK_NATIVE      .getText(), TOK_NATIVE);
        RESERVED.put(TOK_NEW         .getText(), TOK_NEW);
        RESERVED.put(TOK_NULL        .getText(), TOK_NULL);
        RESERVED.put(TOK_PACKAGE     .getText(), TOK_PACKAGE);
        RESERVED.put(TOK_PRIVATE     .getText(), TOK_PRIVATE);
        RESERVED.put(TOK_PROTECTED   .getText(), TOK_PROTECTED);
        RESERVED.put(TOK_PUBLIC      .getText(), TOK_PUBLIC);
        RESERVED.put(TOK_RETURN      .getText(), TOK_RETURN);
        RESERVED.put(TOK_SHORT       .getText(), TOK_SHORT);
        RESERVED.put(TOK_STATIC      .getText(), TOK_STATIC);
        RESERVED.put(TOK_SUPER       .getText(), TOK_SUPER);
        RESERVED.put(TOK_SWITCH      .getText(), TOK_SWITCH);
        RESERVED.put(TOK_SYNCHRONIZED.getText(), TOK_SYNCHRONIZED);
        RESERVED.put(TOK_THIS        .getText(), TOK_THIS);
        RESERVED.put(TOK_THROW       .getText(), TOK_THROW);
        RESERVED.put(TOK_THROWS      .getText(), TOK_THROWS);
        RESERVED.put(TOK_TRANSIENT   .getText(), TOK_TRANSIENT);
        RESERVED.put(TOK_TRUE        .getText(), TOK_TRUE);
        RESERVED.put(TOK_TRY         .getText(), TOK_TRY);
        RESERVED.put(TOK_VOID        .getText(), TOK_VOID);
        RESERVED.put(TOK_VOLATILE    .getText(), TOK_VOLATILE);
        RESERVED.put(TOK_WHILE       .getText(), TOK_WHILE);
        }


    // ----- data members ---------------------------------------------------

    protected int    cat;
    protected int    subcat;
    protected int    id;

    protected Object value;
    protected String text;

    protected int    line;
    protected int    offset;
    protected int    length;
    }
