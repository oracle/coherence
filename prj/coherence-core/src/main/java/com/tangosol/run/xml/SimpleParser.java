/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.SyntaxException;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
* This class uses the XmlTokenizer to produce an XmlDocument from XML text.
*
* @version 1.00, 2001.07.16
* @author  Cameron Purdy
*/
public class SimpleParser
        extends Base
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an XML SimpleParser.  If the XML contains an XSD reference,
    * the parser will validate using the provided XSD.
    */
    public SimpleParser()
        {
        this(true);
        }

    /**
    * Construct an XML SimpleParser.
    *
    * @param fValidate  if true, validate XML if it contains
    *                   an XSD reference
    */
    public SimpleParser(boolean fValidate)
        {
        // hidden system property to disable all validation
        boolean fDisable = Config.getBoolean("coherence.xml.validation.disable");
        if (fDisable && fValidate)
            {
            Logger.info("XML validation disabled");
            }
        m_fValidate = !fDisable && fValidate;
        }

    /**
    * Internal initialization.
    */
    protected void init()
        {
        m_toker = null;
        m_token = null;
        }


    // ----- public interface -----------------------------------------------

    /**
    * Parse the specified String into an XmlDocument object.
    *
    * @param sXml the String to parse
    *
    * @return an XmlDocument object
    *
    * @throws IOException  if I/O error occurs
    */
    public XmlDocument parseXml(String sXml)
            throws IOException
        {
        return parseXml(sXml, null);
        }

    /**
    * Parse the specified Reader into an XmlDocument object.
    *
    * @param reader  the Reader object
    *
    * @return an XmlDocument object
    *
    * @throws IOException  if I/O error occurs
    */
    public XmlDocument parseXml(Reader reader)
            throws IOException
        {
        return parseXml(read(reader), null);
        }

    /**
    * Parse the specified InputStream into an XmlDocument object.
    *
    * @param stream  the InputStream object
    *
    * @return an XmlDocument object
    *
    * @throws IOException  if I/O error occurs
    */
    public XmlDocument parseXml(InputStream stream)
            throws IOException
        {
        XmlDocument doc;
        InputStream in = new ByteArrayInputStream(read(stream));

        try
            {
            in.mark(0);
            try
                {
                // try to parse with UTF-8 encoding
                doc = parseXml(in, XML_DEFAULT_ENCODING);
                }
            catch (UnsupportedEncodingException e)
                {
                // in the unlikely event that UTF-8 isn't supported, try
                // the default (platform specific) encoding
                in.reset();
                doc = parseXml(new InputStreamReader(in));
                }

            String sEncoding = doc.getEncoding();
            if (sEncoding != null && !sEncoding.equalsIgnoreCase(XML_DEFAULT_ENCODING))
                {
                in.reset();
                try
                    {
                    // reparse document with specified encoding
                    doc = parseXml(in, sEncoding);
                    }
                catch (UnsupportedEncodingException e)
                    {
                    // proceeding with parsed document
                    Logger.warn("Could not parse XML with encoding " + sEncoding);
                    }
                }
            }
        finally
            {
            in.close();
            }

        return doc;
        }

    /**
    * Parse the specified InputStream into an XmlDocument object using
    * the specified charset.
    *
    * @param stream    the InputStream object
    * @param sCharset  the charset name
    *
    * @return an XmlDocument object
    *
    * @throws IOException  if I/O error occurs
    */
    public XmlDocument parseXml(InputStream stream, String sCharset)
            throws IOException
        {
        return parseXml(new InputStreamReader(skipBOM(stream), sCharset));
        }

    /**
    * Parse the passed script.
    *
    * @param sXml  the script to parse (as a string)
    * @param xml   the XML document object to parse into
    *
    * @return the XmlDocument object
    *
    * @throws IOException  if I/O error occurs
    */
    public synchronized XmlDocument parseXml(String sXml, XmlDocument xml)
            throws IOException
        {
        azzert(sXml != null);

        if (xml == null)
            {
            xml = instantiateDocument();
            }

        init();
        ErrorList errlist = new ErrorList();

        try
            {
            m_toker = new XmlTokenizer(sXml, errlist);
            m_token = next();
            parseDocument(xml);
            }
        catch (CompilerException e)
            {
            throw ensureRuntimeException(e, "Exception occurred during parsing: " + e.getMessage());
            }
        catch (Exception e)
            {
            String s = "Exception occurred during parsing: " + e.getMessage();
            if (!errlist.isEmpty())
                {
                s += "\nLogged errors:\n" + errlist;
                }
            throw new IOException(s, e);
            }

        if (m_fValidate)
            {
            try
                {
                // attempt to validate the XML if a schema Location
                // is specified, using sax parser
                new SaxParser().validateXsd(sXml, xml);
                }
            catch (Exception e)
                {
                String s = "Exception occurred during schema validation: \n"
                        + e.getMessage();
                throw new IOException(s, e);
                }
            }

        return xml;
        }


    // ----- factory methods ------------------------------------------------

    /**
    * Factory method to instantiate an XmlDocument implementation.
    *
    * @return an object implementing XmlDocument
    */
    protected XmlDocument instantiateDocument()
        {
        return new SimpleDocument();
        }


    // ----- construction ---------------------------------------------------

    /**
    * Unit test.
    *
    * @param asArgs  the string array arguments
    */
    public static void main(String[] asArgs)
        {
        try
            {
            String sFile = "TestXml.xml";
            try
                {
                sFile = asArgs[0];
                }
            catch (Exception e)
                {
                }

            File file = new File(sFile);
            azzert(file.exists() && file.canRead());

            out();
            out("Original Document:");
            String sXml = new String(read(file));
            out(sXml);

            out();
            out("Parsing ...");
            XmlDocument xml = new SimpleParser().parseXml(sXml);

            out();
            out("Parsed Document:");
            sXml = xml.toString();
            out(sXml);

            out();
            out("Parsing the Parsed Document...");
            XmlDocument xml2 = new SimpleParser().parseXml(sXml);

            out();
            out("Re-parsed Document:");
            String sXml2 = xml2.toString();
            out(sXml2);

            out();
            out("Comparing:");
            trace(xml.equals(xml2));
            trace(sXml.equals(sXml2));
            }
        catch (Exception e)
            {
            out("Exception occurred in test: " + e);
            out(e);
            }
        }


    // ----- implementation -------------------------------------------------

    /**
    * Factory method to instantiate an XmlDocument implementation.
    *
    * @param xml  a blank XmlDocument
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parseDocument(XmlDocument xml)
            throws CompilerException
        {
        // document    ::= prolog element Misc*
        // prolog      ::= XMLDecl? Misc* (doctypedecl Misc*)?
        // XMLDecl     ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
        // doctypedecl ::= '<!DOCTYPE' S Name (S ExternalID)? S? ('[' (markupdecl | DeclSep)* ']' S?)? '>'
        // Misc        ::= Comment | PI | S
        // Comment     ::= '<!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
        // PI          ::= '<?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>'

        // check for "<?xml"
        if (peek(XmlToken.PI_START) != null)
            {
            parsePi(xml, true);
            }

        // check for comments / other PIs
        parseMisc(xml);

        // check for "<!DOCTYPE"
        if (peek(XmlToken.DOCTYPE_START) != null)
            {
            parseDoctype(xml);
            }

        // check for comments / other PIs
        parseMisc(xml);

        // root element is required
        match(XmlToken.ELEMENT_START);
        if (xml.getName() != null)
            {
            match(xml.getName());
            }
        else
            {
            String sName = match(XmlToken.NAME).getText();
            xml.setName(sName);
            }
        parseElement(xml);

        // check for comments / other PIs
        parseMisc(xml);
        }

    /**
    * Parse &lt;?xml.
    *
    * @param xml  the XML element
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parsePi(XmlElement xml)
            throws CompilerException
        {
        parsePi(xml, false);
        }

    /**
    * Parse &lt;?xml.
    *
    * @param xml              the XML element
    * @param fXmlDeclAllowed  whether XML declaration is allowed
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parsePi(XmlElement xml, boolean fXmlDeclAllowed)
            throws CompilerException
        {
        XmlToken tokName = match(XmlToken.NAME);
        if (tokName.getText().equals("xml"))
            {
            if (fXmlDeclAllowed)
                {
                parseXmlDecl((XmlDocument) xml);
                }
            else
                {
                throw new SyntaxException(
                    "XML declaration can only appear at the beginning of a document");
                }
            }
        else
            {
            // ignore all other PIs
            while (current().getID() != XmlToken.PI_STOP)
                {
                }
            }
        }

    /**
    * Parse XML declaration.
    *
    * @param xml  the XML document
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parseXmlDecl(XmlDocument xml)
            throws CompilerException
        {
        // XMLDecl      ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
        // VersionInfo  ::= S 'version' Eq ("'" VersionNum "'" | '"' VersionNum '"')
        // VersionNum   ::= ([a-zA-Z0-9_.:] | '-')+
        // EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' | "'" EncName "'" )
        // EncName      ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
        // SDDecl       ::= S 'standalone' Eq (("'" ('yes' | 'no') "'") | ('"' ('yes' | 'no') '"'))

        if (peek("version") != null)
            {
            // version is assumed to be "1.0"
            match(XmlToken.EQUALS);
            match(XmlToken.LITERAL);
            }
        else
            {
            throw new SyntaxException("The version value is"
                + " is missing from the XML declaration");
            }

        if (peek("encoding") != null)
            {
            match(XmlToken.EQUALS);
            String sValue = match(XmlToken.LITERAL).getText();
            if (!XmlHelper.isEncodingValid(sValue))
                {
                throw new SyntaxException("The encoding value in"
                    + " the XML declaration is illegal (" + sValue + ")");
                }
            xml.setEncoding(sValue);
            }

        if (peek("standalone") != null)
            {
            // standalone is discarded
            match(XmlToken.EQUALS);
            String sValue = match(XmlToken.LITERAL).getText();
            if (!(sValue.equals("yes") || sValue.equals("no")))
                {
                throw new SyntaxException("The standalone value in"
                    + " the XML declaration must be 'yes' or 'no'");
                }
            }

        match(XmlToken.PI_STOP);
        }

    /**
    * Parse doc type.
    *
    * @param xml  the XML document
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parseDoctype(XmlDocument xml)
            throws CompilerException
        {
        // doctypedecl ::= '<!DOCTYPE' S Name (S ExternalID)? S? ('[' (markupdecl | DeclSep)* ']' S?)? '>'
        // ExternalID  ::= 'SYSTEM' S SystemLiteral
        //                 'PUBLIC' S PubidLiteral S SystemLiteral

        // root element name
        xml.setName(match(XmlToken.NAME).getText());

        // ExternalID (optional): public identifier
        boolean fPublic = (peek("PUBLIC") != null);
        if (fPublic)
            {
            String sName = match(XmlToken.LITERAL).getText();
            sName = XmlHelper.decodeAttribute(sName);
            if (!XmlHelper.isPublicIdentifierValid(sName))
                {
                throw new SyntaxException("The public identifier in"
                    + " the XML DOCTYPE is invalid (" + sName + ")");
                }
            xml.setDtdName(sName);
            }

        // ExternalID (optional): system identifier
        if (fPublic || peek("SYSTEM") != null)
            {
            String sUri = match(XmlToken.LITERAL).getText();
            sUri = XmlHelper.decodeUri(XmlHelper.decodeAttribute(sUri));
            if (!XmlHelper.isSystemIdentifierValid(sUri))
                {
                throw new SyntaxException("The system identifier in"
                    + " the XML DOCTYPE is invalid (" + sUri + ")");
                }
            xml.setDtdUri(sUri);
            }

        // ignore inline markup decl
        if (peek(XmlToken.DTD_DECL_START) != null)
            {
            while (current().getID() != XmlToken.DTD_DECL_STOP)
                {
                }
            }

        match(XmlToken.ELEMENT_STOP);
        }

    /**
    *
    * Note: '&lt;' and element name have already been parsed
    *
    * @param xml  the XML element
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parseElement(XmlElement xml)
            throws CompilerException
        {
        // element      ::= EmptyElemTag
        //                  STag content ETag
        // EmptyElemTag ::= '<' Name (S Attribute)* S? '/>'
        // STag         ::= '<' Name (S Attribute)* S? '>'
        // Attribute    ::= Name Eq AttValue
        // ETag         ::= '</' Name S? '>'

        // parse attributes
        while (true)
            {
            XmlToken token = peek(XmlToken.NAME);
            if (token == null)
                {
                break;
                }
            String sAttr = token.getText();
            if (!XmlHelper.isNameValid(sAttr))
                {
                throw new SyntaxException("Illegal attribute name: " + sAttr);
                }
            match(XmlToken.EQUALS);
            String sValue = match(XmlToken.LITERAL).getText();
            sValue = XmlHelper.decodeAttribute(sValue);
            xml.addAttribute(sAttr).setString(sValue);
            }

        // check if this were an empty element
        if (peek(XmlToken.EMPTY_STOP) != null)
            {
            return;
            }

        // this element is the "content" type (not empty)
        match(XmlToken.ELEMENT_STOP);

        String sValue = null;
        while (true)
            {
            XmlToken token = current();
            switch (token.getID())
                {
                case XmlToken.COMMENT_START:
                    parseComment(xml);
                    break;

                case XmlToken.PI_START:
                    parsePi(xml);
                    break;

                case XmlToken.CHARDATA:
                    {
                    String sChunk = (String) token.getValue();
                    sChunk = XmlHelper.trim(sChunk);
                    sChunk = XmlHelper.decodeContent(sChunk);
                    if (sChunk.length() > 0)
                        {
                        if (sValue == null)
                            {
                            sValue = sChunk;
                            }
                        else
                            {
                            sValue += sChunk;
                            }
                        }
                    }
                    break;

                case XmlToken.CHARDATA_RAW:
                    {
                    String sChunk = (String) token.getValue();
                    if (sValue == null)
                        {
                        sValue = sChunk;
                        }
                    else
                        {
                        sValue += sChunk;
                        }
                    }
                    break;

                case XmlToken.ELEMENT_START:
                    String sName = match(XmlToken.NAME).getText();
                    parseElement(xml.addElement(sName));
                    break;

                case XmlToken.ENDTAG_START:
                    if (sValue != null)
                        {
                        xml.setString(sValue);
                        }
                    match(xml.getName());
                    match(XmlToken.ELEMENT_STOP);
                    return;
                }
            }
        }

    /**
    * Parse comments / other PIs.
    *
    * @param xml  the XML element
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parseMisc(XmlElement xml)
            throws CompilerException
        {
        // Misc        ::= Comment | PI | S
        // Comment     ::= '<!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
        // PI          ::= '<?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>'
        while (true)
            {
            if (!hasCurrent())
                {
                return;
                }

            if (peek(XmlToken.COMMENT_START) != null)
                {
                parseComment(xml, true);
                continue;
                }

            if (peek(XmlToken.PI_START) != null)
                {
                parsePi(xml);
                continue;
                }

            return;
            }
        }

    /**
    * Parse comments.
    *
    * @param xml  the XML element
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parseComment(XmlElement xml)
            throws CompilerException
        {
        parseComment(xml, false);
        }

    /**
    * Parse comments.
    *
    * @param xml          the XML element
    * @param fIsDocument  whether the passed in XmlElement is an XmlDocument
    *
    * @throws CompilerException  if compiler error occurs
    */
    protected void parseComment(XmlElement xml, boolean fIsDocument)
            throws CompilerException
        {
        StringBuilder sb       = new StringBuilder();
        boolean      fFirst    = true;
        int          cchIndent = 0;
        int          cDeferredBlanks = 0;
        while (peek(XmlToken.COMMENT_STOP) == null)
            {
            String sComment = match(XmlToken.COMMENT).getText();
            char[] ach      = sComment.toCharArray();
            int    cch      = ach.length;

            if (fFirst)
                {
                cchIndent = 0;
                scan: for (int of = 0; of < cch; ++of)
                    {
                    switch (ach[of])
                        {
                        case 0x20:
                        case 0x09:
                            ++cchIndent;
                            break;
                        default:
                            fFirst = false;
                            break scan;
                        }
                    }
                }

            // unindent comment
            int ofStart = 0;
            if (cchIndent > 0)
                {
                scan: for (int of = 0; of < cch && of < cchIndent; ++of)
                    {
                    switch (ach[of])
                        {
                        case 0x20:
                        case 0x09:
                            ++ofStart;
                            break;
                        default:
                            break scan;
                        }
                    }
                }

            // trim off whitespace from end of comment
            scan: for (int of = cch - 1; of >= ofStart; --of)
                {
                switch (ach[of])
                    {
                    case 0x20:
                    case 0x09:
                        --cch;
                        break;
                    default:
                        break scan;
                    }
                }

            if (sb.length() > 0)
                {
                ++cDeferredBlanks;
                }

            if (ofStart < cch)
                {
                for (int i = 0; i < cDeferredBlanks; ++i)
                    {
                    sb.append('\n');
                    }
                cDeferredBlanks = 0;

                sb.append(ach, ofStart, cch);
                }
            }

        if (sb.length() > 0)
            {
            if (fIsDocument)
                {
                XmlDocument doc = (XmlDocument) xml;
                String sComment = doc.getDocumentComment();
                if (sComment == null || sComment.length() == 0)
                    {
                    sComment = sb.toString();
                    }
                else
                    {
                    sComment = sComment + '\n' + sb.toString();
                    }
                doc.setDocumentComment(sComment);
                }
            else
                {
                String sComment = xml.getComment();
                if (sComment == null || sComment.length() == 0)
                    {
                    sComment = sb.toString();
                    }
                else
                    {
                    sComment = sComment + '\n' + sb.toString();
                    }
                xml.setComment(sComment);
                }
            }
        }


    // ----- parsing helpers ------------------------------------------------

    /**
    * Determine if there is a current token.
    *
    * @return true if there is a current token
    */
    protected boolean hasCurrent()
        {
        return m_token != null;
        }

    /**
    * Returns the current token and advances to the next token.
    *
    * @return the current token
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected XmlToken current()
            throws CompilerException
        {
        XmlToken current = m_token;
        next();
        return current;
        }

    /**
    * Determine if there is a next token.
    *
    * @return true if there is a next token
    */
    protected boolean hasNext()
        {
        return m_toker.hasMoreTokens();
        }

    /**
    * Advances to and returns the next token.
    *
    * @return the next token
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected XmlToken next()
            throws CompilerException
        {
        XmlTokenizer toker = m_toker;
        if (toker.hasMoreTokens())
            {
            return m_token = (XmlToken) toker.nextToken();
            }

        if (m_token == null)
            {
            throw new CompilerException("Invalid root element");
            }

        return m_token = null;
        }

    /**
    * Verifies that the current token matches the passed token id and, if so,
    * advances to the next token.  Otherwise, a syntax exception is thrown.
    *
    * @param id the token id to match
    *
    * @return the current token
    *
    * @exception SyntaxException    thrown if the token does not match
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected XmlToken match(int id)
            throws CompilerException
        {
        if (m_token.getID() != id)
            {
            throw new SyntaxException("looking for id=" + id
                + ", found id=" + m_token.getID() + '(' + m_token + ')');
            }
        return current();
        }

    /**
    * Verifies that the current token is a name token whose name matches
    * the passed String and, if so, advances to the next token.  Otherwise,
    * a syntax exception is thrown.
    *
    * @param sName  the name token text to match
    *
    * @return the matched token
    *
    * @exception SyntaxException    thrown if the token does not match
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected XmlToken match(String sName)
            throws CompilerException
        {
        XmlToken token = peek(sName);
        if (token == null)
            {
            throw new SyntaxException("looking for name token=" + sName
                + ", found token=" + m_token + "... It is possible that " + sName
                    + " is missing a closing tag");
            }
        return token;
        }

    /**
    * Tests if the current token matches the passed token id and, if so,
    * advances to the next token.
    *
    * @param id the token id to peek for
    *
    * @return the current token, if matched, or null
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected XmlToken peek(int id)
            throws CompilerException
        {
        return (m_token.getID() == id ? current() : null);
        }

    /**
    * Tests if the current token matches the passed token category and
    * sub-category.  If so, it returns the current token and advances
    * to the next token.
    *
    * @param cat     the category to peek for
    * @param subcat  the sub-category to peek for
    *
    * @return the current token, if matched, or null
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected XmlToken peek(int cat, int subcat)
            throws CompilerException
        {
        XmlToken token = m_token;
        return (token.getCategory() == cat && token.getSubCategory() == subcat ? current() : null);
        }

    /**
    * Tests if the current token is a name that matches the passed String
    * and, if so, advances to the next token.
    *
    * @param sName  the name token text to peek for
    *
    * @return id the current token, if matched, or null
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected XmlToken peek(String sName)
            throws CompilerException
        {
        XmlToken token = m_token;
        return (token.getID() == XmlToken.NAME && token.getText().equals(sName) ? current() : null);
        }

    /**
    * Marks the current position and returns it as a token.
    *
    * @return the current token
    */
    protected XmlToken mark()
        {
        return m_token;
        }

    /**
    * Read the provided {@link InputStream} to determine if the stream starts
    * with a UTF-8 BOM (http://www.unicode.org/faq/utf_bom.html#BOM). If the
    * BOM is present, advance the stream to skip it.
    * <p>
    * This is a workaround for the inability of the Java UTF-8 encoding to
    * recognize the UTF-8 BOM (http://bugs.sun.com/view_bug.do?bug_id=4508058).
    *
    * @param in  InputStream to check for BOM
    *
    * @return an  InputStream with the UTF-8 BOM skipped
    *
    * @throws IOException  if I/O error occurs
    */
    protected InputStream skipBOM(InputStream in)
            throws IOException
        {
        // make sure we have a stream that supports mark/reset
        if (!in.markSupported())
            {
            in = new BufferedInputStream(in);
            }

        // mark the beginning of the stream so that we can reset if necessary
        in.mark(UTF_8_BOM.length);

        // attempt to read the BOM
        boolean fBOM;
        int     cb = 0;
        do
            {
            int n = in.read();
            if (n == -1)
                {
                // EOF
                fBOM = false;
                }
            else
                {
                fBOM = ((byte) n) == UTF_8_BOM[cb++];
                }
            }
        while (fBOM && cb < UTF_8_BOM.length);

        // if the UTF-8 BOM is not found, reset the stream
        if (!fBOM)
            {
            in.reset();
            }

        return in;
        }


    // ----- data members ---------------------------------------------------

    /**
    * If true, validate XML if it contains an XSD reference
    */
    protected final boolean m_fValidate;

    /**
    * The lexical tokenizer.
    */
    protected XmlTokenizer m_toker;

    /**
    * The "current" token being evaluated.
    */
    protected XmlToken m_token;

    /**
    *  The default encoding of an XML document if no encoding declaration is present.
    */
    private static final String XML_DEFAULT_ENCODING = "UTF-8";

    /**
    * UTF-8 BOM (See http://www.unicode.org/faq/utf_bom.html#BOM).
    */
    private static final byte[] UTF_8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    }
