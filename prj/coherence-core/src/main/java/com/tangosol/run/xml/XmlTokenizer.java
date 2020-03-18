/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.CompilerErrorInfo;
import com.tangosol.dev.compiler.ParsePosition;
import com.tangosol.dev.compiler.Script;
import com.tangosol.dev.compiler.Token;
import com.tangosol.dev.compiler.Tokenizer;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Dequeue;
import com.tangosol.util.ErrorList;
import com.tangosol.util.Resources;

import java.io.EOFException;
import java.io.IOException;

import java.util.NoSuchElementException;


/**
* Converts a character stream (Script object) into XML tokens.
*
* @version 1.00, 07/16/01
* @author Cameron Purdy
*/
public class XmlTokenizer
        extends Base
        implements Tokenizer, ErrorList.Constants
    {
    // ----- constructors  --------------------------------------------------

    /**
    * (Default) Constructs an XML Tokenizer.
    */
    public XmlTokenizer()
        {
        }

    /**
    * Constructs an XML Tokenizer.
    *
    * @param s       the string which contains the XML script to tokenize
    * @param errlist the error list which the Tokenizer will log to
    */
    public XmlTokenizer(String s, ErrorList errlist)
            throws CompilerException
        {
        this(new XmlScript(s), errlist);
        }


    /**
    * Constructs a Tokenizer.
    *
    * @param script  the script object which contains the XML script
    * @param errlist the error list which the Tokenizer will log to
    */
    public XmlTokenizer(Script script, ErrorList errlist)
            throws CompilerException
        {
        setScript(script, errlist);
        }


    // ----- Tokenizer interface ------------------------------------------------

    /**
    * Initializes the Tokenizer.  This method must be called exactly
    * one time to initialize the Tokenizer object.
    *
    * @param script   the Script to tokenize
    * @param errlist  the ErrorList to log errors to
    *
    * @exception NoSuchElementException If the tokens are exhausted
    * @exception CompilerException If a lexical error occurs that should stop
    *            the compilation process
    */
    public void setScript(Script script, ErrorList errlist)
            throws CompilerException
        {
        // assert:  this method must be called only one time
        if (m_script != null)
            {
            throw new IllegalStateException();
            }

        m_errlist  = errlist;
        m_script   = script;
        m_dq       = new Dequeue();

        // eat up to the next token
        eatFluff();
        }

    /**
    * Checks for more tokens in the script.
    *
    * @return true if tokenizing of the script is incomplete.
    */
    public boolean hasMoreTokens()
        {
        // first check for any tokens that were "put back"
        if (m_dq.hasMoreElements())
            {
            return true;
            }

        // check if there is more to parse
        return m_script.hasMoreChars();
        }

    /**
    * Eats and returns the next token from the script.
    *
    * @return the next token
    *
    * @exception NoSuchElementException If the tokens are exhausted
    * @exception CompilerException If a lexical error occurs that should stop
    *            the compilation process
    */
    public Token nextToken()
            throws CompilerException
        {
        Token tok;

        if (m_dq.hasMoreElements())
            {
            tok = (Token) m_dq.nextElement();
            }
        else if (m_script.hasMoreChars())
            {
            tok = eatToken();
            eatFluff();
            }
        else
            {
            throw new NoSuchElementException();
            }

        return tok;
        }

    /**
    * Regurgitates the last eaten token so that the next call to nextToken
    * will return the same token that was returned by the most recent call
    * to nextToken.  (This method can be called more than once to regurgitate
    * multiple tokens.)
    *
    * @exception NoSuchElementException an attempt to back up past the
    *            beginning of the script -or- the dequeue was not constructed
    *            large enough to hold the number of entries that have been
    *            put back.
    */
    public void putBackToken(com.tangosol.dev.compiler.Token tok)
        {
        m_dq.putBackElement(tok);
        }

    /**
    * Returns an object that can be used to restore the current position
    * in the script.  This method is similar to the mark method of the
    * Java stream classes, but by returning an object that identifies the
    * position, multiple positions can be saved and later returned to.
    *
    * @return an object which identifies the current position within the
    *         script
    */
    public ParsePosition savePosition()
        {
        Position pos = new Position();

        pos.toker     = this;
        pos.dq        = (Dequeue) m_dq.clone();
        pos.scriptpos = m_script.savePosition();

        return pos;
        }

    /**
    * Restores the current parsing position that was returned from
    * the savePosition method.
    *
    * @param parsepos The return value from a previous call to savePosition
    */
    public void restorePosition(ParsePosition parsepos)
        {
        Position pos = (Position) parsepos;
        if (pos.toker != this)
            {
            throw new IllegalArgumentException("Unknown ParsePosition object!");
            }

        m_script.restorePosition(pos.scriptpos);
        m_dq = pos.dq;
        }


    // ----- script parsing -------------------------------------------------

    /**
    * Eats whitespace and comments until the next XML token is encountered
    * or the end of the string is reached, whichever comes first.
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected void eatFluff()
            throws CompilerException
        {
        Script script = m_script;
        try
            {
            while (script.hasMoreChars())
                {
                char ch = script.nextChar();
                switch (ch)
                    {
                    // XML 1.0 spec 2nd ed section 2.3:
                    // S ::= (#x20 | #x9 | #xD | #xA)+
                    case 0x20:
                    case 0x09:
                    case 0x0D:
                    case 0x0A:
                        break;

                    // a token has been encountered
                    default:
                        script.putBackChar();
                        return;
                    }
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null,
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Eats the largest token possible, returning a Token interface which
    * describes the token.
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatToken()
            throws CompilerException
        {
        switch (m_nCtx)
            {
            case CTX_OUTSIDE:
                return eatOutside();

            case CTX_XMLDECL:
                return eatXmlDecl();

            case CTX_DOCTYPE:
                return eatDocType();

            case CTX_COMMENT:
            case CTX_DTD_COMMENT:
                return eatComment();

            case CTX_PI:
                return eatPi();

            case CTX_ELEMENT:
                return eatElement();

            case CTX_DTD:
                return eatDtd();

            default:
                throw internalError();
            }
        }

    /**
    * Eat a token from the "outside of markup" context.
    *
    * @return the next Token
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatOutside()
            throws CompilerException
        {
        Script script   = m_script;
        int    iLine    = script.getLine();
        int    ofInLine = script.getOffset();

        try
            {
            if (script.nextChar() == '<')
                {
                // could be "<?", "<!--", "<!DOCTYPE", "<!CDATA", or just "<"
                switch (script.nextChar())
                    {
                    case '/':
                        m_nCtx = CTX_ELEMENT;
                        return new XmlToken(XmlToken.TOK_ENDTAG_START, iLine, ofInLine, script.getOffset() - ofInLine);

                    case '?':
                        m_nCtx = CTX_PI;
                        return new XmlToken(XmlToken.TOK_PI_START, iLine, ofInLine, script.getOffset() - ofInLine);

                    case '!':
                        // match either "--", "[CDATA" or "DOCTYPE"
                        switch(script.nextChar())
                            {
                            case '-':
                                match('-');
                                m_nCtx = CTX_COMMENT;
                                return new XmlToken(XmlToken.TOK_COMMENT_START, iLine, ofInLine, script.getOffset() - ofInLine);

                            case '[':
                                match("CDATA[");
                                ParsePosition pos = script.savePosition();
                                scan("]]>");
                                String sData = script.subScript(pos).toString();
                                script.nextChar(); // ]
                                script.nextChar(); // ]
                                script.nextChar(); // >
                                return new XmlToken(XmlToken.CAT_LITERAL, XmlToken.LIT_CHARDATA,
                                        XmlToken.CHARDATA_RAW, sData, sData, iLine,
                                        ofInLine, script.getOffset() - ofInLine);

                            case 'D':
                                script.putBackChar();
                                match("DOCTYPE");
                                m_nCtx = CTX_DOCTYPE;
                                return new XmlToken(XmlToken.TOK_DOCTYPE_START, iLine, ofInLine, script.getOffset() - ofInLine);

                            default:
                                // pretend we were looking for a comment
                                // (easier than duplicating the error reporting)
                                script.putBackChar();
                                match('-'); // will throw CompilerException
                                throw internalError();
                            }

                    default:
                        // assume it is an element
                        script.putBackChar();
                        m_nCtx = CTX_ELEMENT;
                        return new XmlToken(XmlToken.TOK_ELEMENT_START, iLine, ofInLine, script.getOffset() - ofInLine);
                    }
                }
            else
                {
                // character data - scan up to '<'
                script.putBackChar();
                ParsePosition pos = script.savePosition();
                while (script.nextChar() != '<')
                    {
                    }
                script.putBackChar();

                String sData = script.subScript(pos).toString();
                return new XmlToken(XmlToken.CAT_LITERAL, XmlToken.LIT_CHARDATA,
                        XmlToken.CHARDATA, sData, sData, iLine,
                        ofInLine, script.getOffset() - ofInLine);
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Eat a token from the XML declaration section.
    *
    * @return the next Token
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatXmlDecl()
            throws CompilerException
        {
        // use element-context parsing
        return eatElement();
        }

    /**
    * Eat a token from the DOCTYPE section.
    *
    * @return the next Token
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatDocType()
            throws CompilerException
        {
        // check for embedded DTD
        Script       script   = m_script;
        int          iLine    = script.getLine();
        int          ofInLine = script.getOffset();
        try
            {
            if (script.nextChar() == '[')
                {
                m_nCtx = CTX_DTD;
                return new XmlToken(XmlToken.TOK_DTD_DECL_START,
                        iLine, ofInLine, script.getOffset() - ofInLine);
                }
            script.putBackChar();
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }

        // use element-context parsing
        return eatElement();
        }

    /**
    * Eat a token from the DOCTYPE's embedded DTD section.
    *
    * @return the next Token
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatDtd()
            throws CompilerException
        {
        Script script   = m_script;
        int    iLine    = script.getLine();
        int    ofInLine = script.getOffset();

        try
            {
            char ch = script.nextChar();
            switch (ch)
                {
                case '<':
                    {
                    match('!');

                    if (peek('-'))
                        {
                        match('-');
                        m_nCtx = CTX_DTD_COMMENT;
                        return new XmlToken(XmlToken.TOK_COMMENT_START, iLine, ofInLine, script.getOffset() - ofInLine);
                        }

                    Token tokName = eatName();
                    String sName = tokName.getText();
                    scan('>');
                    match('>');

                    if (sName.equals("ELEMENT"))
                        {
                        return new XmlToken(XmlToken.TOK_DTD_ELEMENT,
                            iLine, ofInLine, script.getOffset() - ofInLine);
                        }
                    else if (sName.equals("ATTLIST"))
                        {
                        return new XmlToken(XmlToken.TOK_DTD_ATTLIST,
                            iLine, ofInLine, script.getOffset() - ofInLine);
                        }
                    else if (sName.equals("ENTITY"))
                        {
                        return new XmlToken(XmlToken.TOK_DTD_ENTITY,
                            iLine, ofInLine, script.getOffset() - ofInLine);
                        }
                    else if (sName.equals("NOTATION"))
                        {
                        return new XmlToken(XmlToken.TOK_DTD_NOTATION,
                            iLine, ofInLine, script.getOffset() - ofInLine);
                        }
                    else
                        {
                        logError(ERROR, ERR_XML_FORMAT, null, iLine, ofInLine, 0);
                        throw new CompilerException();
                        }
                    }

                case '%':
                    {
                    // %name;
                    Token tokName = eatName();
                    String sName = tokName.getText();
                    match(';');
                    return new XmlToken(XmlToken.CAT_DTDSTUFF, XmlToken.NONE,
                            XmlToken.DTD_DECLSEP, sName, sName, iLine,
                            ofInLine, script.getOffset() - ofInLine);
                    }

                default:
                    script.putBackChar();
                    match(']');
                    m_nCtx = CTX_DOCTYPE;
                    return new XmlToken(XmlToken.TOK_DTD_DECL_STOP,
                            iLine, ofInLine, script.getOffset() - ofInLine);
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Eat a token from a comment.
    *
    * @return the next Token
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatComment()
            throws CompilerException
        {
        // read until "-->" found
        Script        script   = m_script;
        ParsePosition pos      = script.savePosition();
        int           iLine    = script.getLine();
        int           ofInLine = script.getOffset();
        boolean       fFirst   = true;
        try
            {
            while (true)
                {
                switch (script.nextChar())
                    {
                    case '-':
                        if (script.nextChar() == '-')
                            {
                            if (script.nextChar() == '>')
                                {
                                if (fFirst)
                                    {
                                    // return the end-of-commment
                                    m_nCtx = (m_nCtx == CTX_DTD_COMMENT ? CTX_DTD : CTX_OUTSIDE);
                                    return new XmlToken(XmlToken.TOK_COMMENT_STOP,
                                            iLine, ofInLine, script.getOffset() - ofInLine);
                                    }
                                else
                                    {
                                    // restore the end of comment
                                    script.putBackChar();
                                    script.putBackChar();
                                    script.putBackChar();

                                    String sComment = script.subScript(pos).toString();
                                    return new XmlToken(XmlToken.CAT_LITERAL, XmlToken.LIT_COMMENT,
                                            XmlToken.COMMENT, null, sComment, iLine,
                                            ofInLine, script.getOffset() - ofInLine);
                                    }
                                }
                            }

                    case '\r':
                    case '\n':
                        {
                        // restore newline character
                        script.putBackChar();

                        String sComment = script.subScript(pos).toString();
                        return new XmlToken(XmlToken.CAT_LITERAL, XmlToken.LIT_COMMENT,
                                XmlToken.COMMENT, null, sComment, iLine,
                                ofInLine, script.getOffset() - ofInLine);
                        }
                    }

                fFirst = false;
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Eat a token from a processing instruction (PI).
    *
    * @return the next Token
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatPi()
            throws CompilerException
        {
        Token tokName = eatName();
        if (tokName.getText().equals("xml"))
            {
            // switch over to XML-specific PI
            m_nCtx = CTX_XMLDECL;
            }
        else
            {
            eatFluff();

            // eat body
            Script        script   = m_script;
            ParsePosition pos      = script.savePosition();
            int           iLine    = script.getLine();
            int           ofInLine = script.getOffset();
            scan("?>");
            String sBody = script.subScript(pos).toString();
            if (sBody.length() > 0)
                {
                // queue body
                m_dq.addElement(new XmlToken(XmlToken.CAT_LITERAL,
                        XmlToken.LIT_CHARDATA, XmlToken.CHARDATA_RAW,
                        null, sBody, iLine, ofInLine,
                        script.getOffset() - ofInLine));
                }

            // queue closing separator
            iLine    = script.getLine();
            ofInLine = script.getOffset();
            try
                {
                script.nextChar();
                script.nextChar();
                }
            catch (IOException e)
                {
                throw internalError();
                }
            m_dq.addElement(new XmlToken(XmlToken.TOK_PI_STOP,
                    iLine, ofInLine, script.getOffset() - ofInLine));

            m_nCtx = CTX_OUTSIDE;
            }

        return tokName;
        }

    /**
    * Eat a token from an element.
    *
    * @return the next Token
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatElement()
            throws CompilerException
        {
        Script script   = m_script;
        int    iLine    = script.getLine();
        int    ofInLine = script.getOffset();

        try
            {
            char ch = script.nextChar();
            switch (ch)
                {
                // "?>" is supported here because PI and XMLDECL delegate
                // to this method for parsing
                case '?':
                    match('>');
                    m_nCtx = CTX_OUTSIDE;
                    return new XmlToken(XmlToken.TOK_PI_STOP,
                            iLine, ofInLine, script.getOffset() - ofInLine);

                case '=':
                    return new XmlToken(XmlToken.TOK_EQUALS,
                            iLine, ofInLine, script.getOffset() - ofInLine);

                case '\'':
                case '\"':
                    {
                    // scan for closing quote (ch)
                    ParsePosition pos = script.savePosition();
                    scan(ch);
                    String sLit = script.subScript(pos).toString();
                    match(ch);
                    return new XmlToken(XmlToken.CAT_LITERAL,
                        XmlToken.LIT_QUOTED, XmlToken.LITERAL,
                        null, sLit, iLine, ofInLine,
                        script.getOffset() - ofInLine);
                    }

                case '/':
                    match('>');
                    m_nCtx = CTX_OUTSIDE;
                    return new XmlToken(XmlToken.TOK_EMPTY_STOP,
                            iLine, ofInLine, script.getOffset() - ofInLine);

                case '>':
                    m_nCtx = CTX_OUTSIDE;
                    return new XmlToken(XmlToken.TOK_ELEMENT_STOP,
                            iLine, ofInLine, script.getOffset() - ofInLine);

                default:
                    script.putBackChar();
                    return eatName();
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Eat a name token.
    *
    * @return the next Token
    *
    * @exception CompilerException  If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatName()
            throws CompilerException
        {
        Script script   = m_script;
        int    iLine    = script.getLine();
        int    ofInLine = script.getOffset();

        try
            {
            StringBuffer sb = new StringBuffer();

            char ch = script.nextChar();
            if (!isNameStartChar(ch))
                {
                int ofNext = script.getOffset();
                script.putBackChar();
                logError(ERROR, ERR_XML_FORMAT, null,
                        iLine, ofInLine, ofNext - ofInLine);
                throw new CompilerException();
                }
            sb.append(ch);

            while (isNameChar(ch = script.nextChar()))
                {
                sb.append(ch);
                }
            script.putBackChar();

            String sName = sb.toString();
            return new XmlToken(XmlToken.CAT_NAME, XmlToken.NONE,
                    XmlToken.NAME, null, sName, iLine,
                    ofInLine, script.getOffset() - ofInLine);
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Helper: Is the passed character a starting character for an XML name?
    */
    protected static boolean isNameStartChar(char ch)
        {
        // close enough (checking for the most probable first)
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
            ch == '_' || ch == ':' || Character.isUnicodeIdentifierStart(ch);
        }

    /**
    * Helper: Is the passed character a valid character in an XML name?
    */
    protected static boolean isNameChar(char ch)
        {
        // close enough (checking for the most probable first)
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
            ch == '_' || ch == ':' || ch == '-' || ch == '.' || Character.isUnicodeIdentifierPart(ch);
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Peek for the specified character in the data being streamed from the
    * Script object. Advance past if found. Otherwise return false.
    *
    * @param ch  the character to peek
    *
    * @return true if peek found it, otherwise false
    *
    * @exception CompilerException
    */
    protected boolean peek(char ch)
            throws CompilerException
        {
        Script script = m_script;
        try
            {
            char chActual = script.nextChar();
            if (ch == chActual)
                {
                return true;
                }
            else
                {
                script.putBackChar();
                return false;
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }


    /**
    * Match the specified character in the data being streamed from the
    * Script object.
    *
    * @param ch  the character to match
    *
    * @exception CompilerException
    */
    protected void match(char ch)
            throws CompilerException
        {
        Script script = m_script;
        try
            {
            char chActual = script.nextChar();
            if (ch != chActual)
                {
                int ofNext = script.getOffset();
                script.putBackChar();
                logError(ERROR, ERR_CHAR_EXPECTED, new String[]
                        {toCharEscape(ch), toCharEscape(chActual)},
                        script.getLine(), script.getOffset(),
                        ofNext - script.getOffset());
                script.nextChar();
                throw new CompilerException();
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Match the specified String in the data being streamed from the
    * Script object.
    *
    * @param s  the String to match
    *
    * @exception CompilerException
    */
    protected void match(String s)
            throws CompilerException
        {
        char[] ach    = s.toCharArray();
        int    cch    = ach.length;
        Script script = m_script;
        try
            {
            for (int of = 0; of < cch; ++of)
                {
                char chActual = script.nextChar();
                if (ach[of] != chActual)
                    {
                    int ofNext = script.getOffset();
                    script.putBackChar();
                    logError(ERROR, ERR_CHAR_EXPECTED, new String[]
                            {toCharEscape(ach[of]), toCharEscape(chActual)},
                            script.getLine(), script.getOffset(),
                            ofNext - script.getOffset());
                    script.nextChar();
                    throw new CompilerException();
                    }
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Match one whitespace character.
    *
    * @exception CompilerException
    */
    protected void matchWhitespace()
            throws CompilerException
        {
        try
            {
            char ch = m_script.nextChar();
            switch (ch)
                {
                // XML 1.0 spec 2nd ed section 2.3:
                // S ::= (#x20 | #x9 | #xD | #xA)+
                case 0x20:
                case 0x09:
                case 0x0D:
                case 0x0A:
                    return;

                default:
                    // easy way to log an error for no whitespace
                    m_script.putBackChar();
                    match(' ');
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null,
                    m_script.getLine(), m_script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    m_script.getLine(), m_script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Scan until the specified String is encountered.
    *
    * @param ch  the character to scan for
    *
    * @exception CompilerException
    */
    protected void scan(char ch)
            throws CompilerException
        {
        Script script = m_script;
        try
            {
            while (true)
                {
                if (script.nextChar() == ch)
                    {
                    script.putBackChar();
                    return;
                    }
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Scan until the specified String is encountered.
    *
    * @param s  the String to scan for
    *
    * @exception CompilerException
    */
    protected void scan(String s)
            throws CompilerException
        {
        char[] ach      = s.toCharArray();
        int    cch      = ach.length;
        int    chSentry = ach[0];
        Script script   = m_script;
        try
            {
            next: while (true)
                {
                if (script.nextChar() == chSentry)
                    {
                    // determine if the entire search string has been found
                    boolean fFound  = true;
                    int     cTested = 1;
                    for (int of = 1; of < cch; ++of)
                        {
                        fFound = script.nextChar() == ach[of];
                        ++cTested;
                        if (!fFound)
                            {
                            break;
                            }
                        }

                    // restore the search string if it were found; otherwise
                    // restore everything but the sentry character (to avoid
                    // finding the same one again)
                    for (int i = 0, c = fFound ? cTested : cTested - 1; i < c; ++i)
                        {
                        script.putBackChar();
                        }

                    if (fFound)
                        {
                        break next;
                        }
                    }
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, script.getLine(),
                    script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    script.getLine(), script.getOffset(), 0);
            throw new CompilerException();
            }
        }


    // ----- error handling -------------------------------------------------

    /**
    * Logs an internal error and throws a CompilerException.
    *
    * @exception CompilerException Thrown unconditionally
    */
    protected CompilerException internalError()
            throws CompilerException
        {
        logError(FATAL, ERR_INTERNAL, null, m_script.getLine(), m_script.getOffset(), 0);
        throw new CompilerException();
        }

    /**
    * Logs the passed error in the error list.
    *
    * @param nSeverity Severity of the error as defined by ErrorList.Constants
    * @param sCode Error code, as defined by the class logging the error
    * @param asParams Replaceable parameters for the error message
    * @param iLine Line number where the error was detected
    * @param ofInLine Offset of the error within the line
    * @param cchText Length of the text which caused the error
    *
    * @exception CompilerException If the error list overflows.
    */
    protected void logError(int nSeverity, String sCode, String[] asParams, int iLine, int ofInLine, int cchText)
            throws CompilerException
        {
        try
            {
            m_errlist.add(new CompilerErrorInfo(nSeverity, sCode, RESOURCES,
                    asParams, iLine, ofInLine, cchText));
            }
        catch (ErrorList.OverflowException e)
            {
            throw new CompilerException();
            }
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Stores all information required to later restore the current position in
    * the script.
    */
    class Position
            implements ParsePosition
        {
        Tokenizer     toker;
        Dequeue       dq;
        ParsePosition scriptpos;
        }


    // ----- error codes ----------------------------------------------------

    public static final String ERR_INTERNAL         = "XT-001";
    public static final String ERR_UNEXPECTED_EOF   = "XT-002";
    public static final String ERR_UNEXPECTED_IO    = "XT-003";
    public static final String ERR_XML_FORMAT       = "XT-004";
    public static final String ERR_CHAR_EXPECTED    = "XT-005";

    /**
    * The package resources.
    */
    public static final Resources RESOURCES =
            ClassHelper.getPackageResources("com.tangosol.run.xml.");


    // ----- constants ------------------------------------------------------

    private static final int CTX_OUTSIDE    = 0;
    private static final int CTX_XMLDECL    = 1;
    private static final int CTX_DOCTYPE    = 2;
    private static final int CTX_COMMENT    = 3;
    private static final int CTX_PI         = 4;
    private static final int CTX_ELEMENT    = 5;
    private static final int CTX_CHARDATA   = 6;
    private static final int CTX_DTD        = 7;
    private static final int CTX_DTD_COMMENT= 8;


    // ----- data members ---------------------------------------------------

    /**
    * Context-sensitive parsing indicator.
    */
    private int         m_nCtx;

    /**
    * The script to parse.
    */
    private Script      m_script;

    /**
    * The "put back" queue.
    */
    private Dequeue     m_dq;

    /**
    * The error list to log to.
    */
    private ErrorList   m_errlist;
    }
