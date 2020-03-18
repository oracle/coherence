/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.SyntaxException;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;


/**
* This class implements the Java script parser.  The purpose of this class
* is to extract method "scripts" from a .java file.
*
* @version 1.00, 2001.05.22
* @author  Cameron Purdy
*/
public class ScriptParser
        extends Base
        implements Constants, TokenConstants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Java ScriptParser.
    */
    public ScriptParser()
        {
        }

    /**
    * Internal initialization.
    */
    protected void init()
        {
        m_toker      = null;
        m_token      = null;
        m_asLines    = null;
        m_map        = new HashMap();
        m_mapParam   = new HashMap();
        m_sPackage   = null;
        m_sImports   = null;
        m_stackClass = new Stack();
        }


    // ----- public interface -----------------------------------------------

    /**
    * Parse the passed script.
    *
    * @param sScript  the script to parse (as a string)
    *
    * @return a map from method signatures to scripts (scripts may be null)
    */
    public synchronized Map parse(String sScript)
        {
        return parse(sScript, null);
        }

    /**
    * Parse the passed script.
    *
    * @param sScript   the script to parse (as a string)
    * @param mapParam  the map to store parameter names into
    *
    * @return a map from method signatures to scripts (scripts may be null)
    */
    public synchronized Map parse(String sScript, Map mapParam)
        {
        init();

        // 2002-07-22 cp - use passed map for storing parameters
        if (mapParam != null)
            {
            m_mapParam = mapParam;
            }

        // script is required
        if (sScript == null)
            {
            throw new IllegalArgumentException(CLASS + ".compile:  "
                    + "Script required!");
            }

        // parse script into lines
        sScript = replace(sScript, "\r\n", "\n");
        sScript = sScript.replace('\r', '\n');
        m_asLines = parseDelimitedString(sScript, '\n');

        // parse the script
        try
            {
            m_toker = new Tokenizer(sScript, new ErrorList(), OPTIONS);
            m_token = next();
            parseCompilationUnit();
            }
        catch (Exception e)
            {
            if (DEBUG)
                {
                err("An exception occurred parsing the .java file:");
                err(e);
                }
            }

        return m_map;
        }


    // ----- construction ---------------------------------------------------

    /**
    * Unit test.
    */
    public static void main(String[] asArgs)
        {
        try
            {
            String sClass = asArgs[0];
            ClassLoader loader = Class.forName(sClass).getClassLoader();
            if (loader == null)
                {
                loader = ClassLoader.getSystemClassLoader();
                }
            InputStream stream = loader.getResourceAsStream(sClass.replace('.', '/') + ".java");
            if (stream == null)
                {
                out("No source for: " + sClass);
                return;
                }

            byte[] ab = new byte[1000000];
            int    cb = read(stream, ab);
            stream.close();

            String sScript = new String(ab, 0, cb);
            Map map = new ScriptParser().parse(sScript);
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry = (Map.Entry) iter.next();
                out();
                out(entry.getKey());
                out("    {");
                out(indentString((String) entry.getValue(), "    "));
                out("    }");
                }
            }
        catch (Exception e)
            {
            out("Exception occurred in test: " + e);
            out(e);
            }
        }


    // ----- script parsing -------------------------------------------------

    /**
    * Parse the script.
    *
    *   Goal:
    *       CompilationUnit
    *   CompilationUnit:
    *       PackageDeclaration-opt ImportDeclarations-opt TypeDeclarations-opt
    */
    protected void parseCompilationUnit()
            throws CompilerException
        {
        String sPackage = parsePackageDeclaration();
        m_sPackage = sPackage.length() == 0 ? "" : sPackage + '.';

        String sImports = parseImportDeclarations();
        if (sImports.length() > 0)
            {
            m_sImports = sImports;
            }

        parseTypeDeclarations();
        }

    /**
    * Parse the "package" declaration.
    *
    * @return the qualified package name (or a zero length string if no package)
    */
    protected String parsePackageDeclaration()
            throws CompilerException
        {
        String sPackage = "";
        if (peek(KEY_PACKAGE) != null)
            {
            sPackage = parseQualifiedName();
            match(SEP_SEMICOLON);
            }
        return sPackage;
        }

    /**
    * Parse the "import" declarations.
    *
    * @return a String containing the import declarations (or a zero length
    *         string if there are none)
    */
    protected String parseImportDeclarations()
            throws CompilerException
        {
        Token tokMark = mark();
        while (peek(KEY_IMPORT) != null)
            {
            parseQualifiedName(true);
            match(SEP_SEMICOLON);
            }
        return snip(tokMark, mark());
        }

    /**
    * Parse "n.n.n".
    */
    protected String parseQualifiedName()
            throws CompilerException
        {
        return parseQualifiedName(false);
        }

    /**
    * Parse "n.n.n".
    */
    protected String parseQualifiedName(boolean fAllowStar)
            throws CompilerException
        {
        // parse name "n.n.n"
        StringBuffer sb = new StringBuffer();
        boolean fFirst = true;
        do
            {
            if (fFirst)
                {
                fFirst = false;
                }
            else
                {
                sb.append('.');
                }

            Token tokName = null;
            if (fAllowStar)
                {
                tokName = peek(OP_MUL);
                }
            if (tokName == null)
                {
                tokName = match(IDENT);
                }
            sb.append(tokName.getText());

            // "n.*" -- done after '*'
            if (tokName.id == OP_MUL)
                {
                break;
                }
            }
        while (peek(SEP_DOT) != null);
        return sb.toString();
        }

    /**
    * Parse zero or more class/interface declarations.
    *
    *   TypeDeclaration:
	*       ClassOrInterfaceDeclaration
	*       ;
    */
    protected void parseTypeDeclarations()
            throws CompilerException
        {
        while (hasCurrent())
            {
            if (peek(SEP_SEMICOLON) != null)
                {
                continue;
                }
            parseClassOrInterfaceDeclaration();
            }
        }

    /**
    *
    *   ClassOrInterfaceDeclaration:
	*       ModifiersOpt (ClassDeclaration | InterfaceDeclaration)
    *
    *   ClassDeclaration:
    *   	class Identifier [extends Type] [implements TypeList] ClassBody
    *
    *   InterfaceDeclaration:
    *   	interface Identifier [extends TypeList] InterfaceBody
    *
    *   TypeList:
    *       Type { , Type}
    */
    protected void parseClassOrInterfaceDeclaration()
            throws CompilerException
        {
        parseModifiers();

        switch (m_token.id)
            {
            case KEY_CLASS:
                {
                // class Identifier
                match(KEY_CLASS);
                Token tokName = match(IDENT);

                // [extends Type]
                if (peek(KEY_EXTENDS) != null)
                    {
                    parseQualifiedName();
                    }

                // [implements TypeList]
                if (peek(KEY_IMPLEMENTS) != null)
                    {
                    do
                        {
                        parseQualifiedName();
                        }
                    while (peek(SEP_COMMA) != null);
                    }

                // ClassBody
                parseClassOrInterfaceBody(tokName);
                }
                break;

            case KEY_INTERFACE:
                {
                // interface Identifier
                match(KEY_INTERFACE);
                Token tokName = match(IDENT);

                // [extends TypeList]
                if (peek(KEY_EXTENDS) != null)
                    {
                    do
                        {
                        parseQualifiedName();
                        }
                    while (peek(SEP_COMMA) != null);
                    }

                // InterfaceBody
                parseClassOrInterfaceBody(tokName);
                }
                break;

            default:
                throw new SyntaxException();
            }
        }

    /**
    * Parse zero or more class/interface declarations.
    *
    *   ModifiersOpt:
    *   	{ Modifier }
    *
    *   Modifier:
    *   	public
    *   	protected
    *   	private
    *   	static
    *   	abstract
    *   	final
    *   	native
    *   	synchronized
    *   	transient
    *   	volatile
    *   	strictfp
    */
    protected void parseModifiers()
            throws CompilerException
        {
        while (true)
            {
            switch (m_token.id)
                {
                case KEY_PUBLIC:
                case KEY_PROTECTED:
                case KEY_PRIVATE:
                case KEY_STATIC:
                case KEY_ABSTRACT:
                case KEY_FINAL:
                case KEY_NATIVE:
                case KEY_SYNCHRONIZED:
                case KEY_TRANSIENT:
                case KEY_VOLATILE:
                case KEY_STRICTFP:
                    next();
                    break;

                default:
                    return;
                }
            }
        }

    /**
    * Parse a class or interface body.
    *
    *   ClassBody:
    *   	{ {ClassBodyDeclaration} }
    *
    *   InterfaceBody:
    *   	{ {InterfaceBodyDeclaration} }
    *
    *   ClassBodyDeclaration:
    *   	;
    *   	[static] Block
    *   	ModifiersOpt MemberDecl
    *
    *   InterfaceBodyDeclaration:
    *   	;
    *   	ModifiersOpt InterfaceMemberDecl
    */
    protected void parseClassOrInterfaceBody(Token tokName)
            throws CompilerException
        {
        // push the class onto the stack of classes being parsed
        Stack  stack  = m_stackClass;
        stack.push(tokName.getText());

        try
            {
            // store the imports as a script
            if (m_sImports != null)
                {
                setScript(IMPORTS, m_sImports);
                }

            match(SEP_LCURLYBRACE);
nextMember: while (true)
                {
                switch (m_token.id)
                    {
                    case SEP_SEMICOLON:
                        next();
                        break;

                    case SEP_LCURLYBRACE:
                        parseStatic();
                        break;

                    case KEY_STATIC:
                        if (next().id == SEP_LCURLYBRACE)
                            {
                            parseStatic();
                            }
                        // fall through
                    case KEY_PUBLIC:
                    case KEY_PROTECTED:
                    case KEY_PRIVATE:
                    case KEY_ABSTRACT:
                    case KEY_FINAL:
                    case KEY_NATIVE:
                    case KEY_SYNCHRONIZED:
                    case KEY_TRANSIENT:
                    case KEY_VOLATILE:
                    case KEY_STRICTFP:
                        parseModifiers();
                        // fall through
                    case IDENT:
                    case KEY_VOID:
                    case KEY_BOOLEAN:
                    case KEY_BYTE:
                    case KEY_CHAR:
                    case KEY_SHORT:
                    case KEY_INT:
                    case KEY_LONG:
                    case KEY_FLOAT:
                    case KEY_DOUBLE:
                    case KEY_CLASS:
                    case KEY_INTERFACE:
                        parseMember();
                        break;

                    default:
                        break nextMember;
                    }
                }
            match(SEP_RCURLYBRACE);
            }
        finally
            {
            // pop class off the stack
            stack.pop();
            }
        }

    /**
    * Parse a static code section.
    */
    protected void parseStatic()
            throws CompilerException
        {
        String sScript = parseMethodBody();
        addStatic(sScript);
        }

    /**
    * Parse a class member.
    */
    protected void parseMember()
            throws CompilerException
        {
        switch (m_token.id)
            {
            case IDENT:
                // could be constructor (name of class)
                // could be return type (n or n.n.n)
                // could be field declaration (n or n.n.n)
                {
                Token tokType = current();

                // the next token is a left-paren if this is a constructor
                if (m_token.id == SEP_LPARENTHESIS)
                    {
                    parseMethodDeclaratorRest("<init>");
                    break;
                    }

                // eat through the rest of the qualified name
                // (keep only the short name of the type)
                while (peek(SEP_DOT) != null)
                    {
                    tokType = match(IDENT);
                    }

                // eat optional brackets
                while (peek(SEP_LBRACKET) != null)
                    {
                    match(SEP_RBRACKET);
                    }

                // up to the field/method name now
                Token tokName = match(IDENT);
                if (m_token.id == SEP_LPARENTHESIS)
                    {
                    parseMethodDeclaratorRest(tokName.getText());
                    }
                else
                    {
                    // it is a field declaration; expurgate
                    while (current().id != SEP_SEMICOLON)
                        {
                        }
                    }
                }
                break;

            case KEY_BOOLEAN:
            case KEY_BYTE:
            case KEY_CHAR:
            case KEY_SHORT:
            case KEY_INT:
            case KEY_LONG:
            case KEY_FLOAT:
            case KEY_DOUBLE:
                // could be return type
                // could be field declaration
                {
                Token tokType = current();

                // eat optional brackets
                while (peek(SEP_LBRACKET) != null)
                    {
                    match(SEP_RBRACKET);
                    }

                Token tokName = match(IDENT);
                if (m_token.id == SEP_LPARENTHESIS)
                    {
                    parseMethodDeclaratorRest(tokName.getText());
                    }
                else
                    {
                    // it is a field declaration; expurgate
                    while (current().id != SEP_SEMICOLON)
                        {
                        }
                    }
                }
                break;

            case KEY_VOID:
                // definitely a return type
                {
                Token tokType = current();
                Token tokName = match(IDENT);
                parseMethodDeclaratorRest(tokName.getText());
                }
                break;

            case KEY_CLASS:
            case KEY_INTERFACE:
                parseClassOrInterfaceDeclaration();
                break;
            }
        }

    /**
    * Parse a method body and return the script.
    */
    protected String parseMethodBody()
            throws CompilerException
        {
        match(SEP_LCURLYBRACE);

        Token tokStart     = mark();
        Token tokBeyondEnd;

        int cDepth = 1;
        ScriptBody: while (true)
            {
            Token tok = current();
            switch (tok.id)
                {
                case SEP_LCURLYBRACE:
                    ++cDepth;
                    break;

                case SEP_RCURLYBRACE:
                    if (--cDepth == 0)
                        {
                        tokBeyondEnd = tok;
                        break ScriptBody;
                        }
                    break;
                }
            }

        return snip(tokStart, tokBeyondEnd);
        }

    /**
    * Parse the remainder of the method declaration and the method body
    * if there is one.
    *
    *   MethodDeclaratorRest:
    *       FormalParameters BracketsOpt [throws QualifiedIdentifierList]
    * ->        ( MethodBody | ; )
    */
    protected void parseMethodDeclaratorRest(String sMethod)
            throws CompilerException
        {
        // 2002-07-22 cp - collect parameter names
        List listParam = new ArrayList();

        StringBuffer sb = new StringBuffer(sMethod);
        sb.append('(');

        // FormalParameters
        match(SEP_LPARENTHESIS);
        boolean fFirst = true;
        NextParam: while (true)
            {
            // optional "final"
            peek(KEY_FINAL);

            String sType;
            switch (m_token.id)
                {
                case IDENT:
                    // n or n.n.n
                    sType = current().getText();
                    while (peek(SEP_DOT) != null)
                        {
                        sType = current().getText();
                        }
                    break;

                case KEY_BOOLEAN:
                case KEY_BYTE:
                case KEY_CHAR:
                case KEY_SHORT:
                case KEY_INT:
                case KEY_LONG:
                case KEY_FLOAT:
                case KEY_DOUBLE:
                    sType = current().getText();
                    break;

                default:
                    break NextParam;
                }

            // eat optional brackets
            while (peek(SEP_LBRACKET) != null)
                {
                match(SEP_RBRACKET);
                sType += "[]";
                }

            // eat the name
            // 2002-07-22 cp - also store the name
            listParam.add(match(IDENT).getText());

            // eat optional brackets
            while (peek(SEP_LBRACKET) != null)
                {
                match(SEP_RBRACKET);
                sType += "[]";
                }

            // add the short parameter type to the script "key"
            int of = sType.lastIndexOf('$');
            if (of >= 0)
                {
                sType = sType.substring(of + 1);
                }
            if (fFirst)
                {
                fFirst = false;
                }
            else
                {
                sb.append(',');
                }
            sb.append(sType);

            // eat the optional (in the case of a trailing) comma
            peek(SEP_COMMA);
            }
        match(SEP_RPARENTHESIS);

        // finish building the script "key"
        sb.append(')');
        String sSig = sb.toString();

        // 2002-07-22 cp - assemble parameter names
        String[] asParam = (String[]) listParam.toArray(new String[listParam.size()]);
        setParams(sSig, asParam);

        // BracketsOpt
        while (peek(SEP_LBRACKET) != null)
            {
            match(SEP_RBRACKET);
            }

        // [throws QualifiedIdentifierList]
        if (peek(KEY_THROWS) != null)
            {
            while (m_token.id == IDENT)
                {
                parseQualifiedName();
                peek(SEP_COMMA);
                }
            }

        // ( MethodBody | ; )
        if (peek(SEP_SEMICOLON) == null)
            {
            String sScript = parseMethodBody();
            setScript(sSig, sScript);
            }
        }


    // ----- script collection helpers --------------------------------------

    /**
    * Determine the current name.
    */
    String getClassName()
        {
        Stack  stack  = m_stackClass;
        azzert(!stack.isEmpty());

        StringBuffer sb = new StringBuffer(m_sPackage);
        for (int i = 0, c = stack.size(); i < c; ++i)
            {
            if (i > 0)
                {
                sb.append('$');
                }
            sb.append((String) stack.get(i));
            }
        return sb.toString();
        }

    /**
    * Accumulate static block code for the class.
    */
    void addStatic(String sScript)
        {
        String sOrig = getScript(STATIC_INIT);
        if (sOrig == null)
            {
            setScript(STATIC_INIT, sScript);
            }
        else
            {
            setScript(STATIC_INIT, sOrig + '\n' + sScript);
            }
        }

    /**
    * Look up the specified script for the current class and return it.
    *
    * @return the script or null if there is none
    */
    String getScript(String sSig)
        {
        String sKey = getClassName() + '.' + sSig;
        return (String) m_map.get(sKey);
        }

    /**
    * Store the specified script.
    */
    void setScript(String sSig, String sScript)
        {
        String sKey = getClassName() + '.' + sSig;
        m_map.put(sKey, sScript);
        }

    /**
    * Look up the specified param names for the specified sig of the current class.
    *
    * @return the param names or null if there is none
    */
    String[] getParams(String sSig)
        {
        String sKey = getClassName() + '.' + sSig;
        return (String[]) m_mapParam.get(sKey);
        }

    /**
    * Store the specified param names.
    */
    void setParams(String sSig, String[] asParam)
        {
        String sKey = getClassName() + '.' + sSig;
        m_mapParam.put(sKey, asParam);
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
    protected Token current()
            throws CompilerException
        {
        Token current = m_token;
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
    protected Token next()
            throws CompilerException
        {
        Tokenizer toker = m_toker;
        if (toker.hasMoreTokens())
            {
            return m_token = (Token) toker.nextToken();
            }

        if (m_token == null)
            {
            throw new CompilerException();
            }

        return m_token = null;
        }

    /**
    * Verifies that the current token matches the passed token id and, if so,
    * advances to the next token.  Otherwise, a syntax exception is thrown.
    *
    * @param id  the token id to match
    *
    * @return the current token
    *
    * @exception SyntaxException    thrown if the token does not match
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected Token match(int id)
            throws CompilerException
        {
        if (m_token.id != id)
            {
            throw new SyntaxException();
            }
        return current();
        }

    /**
    * Tests if the current token matches the passed token id and, if so,
    * advances to the next token.
    *
    * @param id  the token id to peek for
    *
    * @return the current token, if matched, or null
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected Token peek(int id)
            throws CompilerException
        {
        return (m_token.id == id ? current() : null);
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
    protected Token peek(int cat, int subcat)
            throws CompilerException
        {
        Token token = m_token;
        return (token.cat == cat && token.subcat == subcat ? current() : null);
        }

    /**
    * Marks the current position and returns it as a token.
    *
    * @return the current token
    */
    protected Token mark()
        {
        return m_token;
        }

    /**
    * Obtains the source code between two marked tokens, incusive of the
    * start and exclusive of the end.
    *
    * @param tokStart      the first token of the script to "snip"
    * @param tokBeyondEnd  the (noninclusive) token that follows the script
    *                      to "snip"
    *
    * @return the source code starting at tokStart and ending before
    *         tokBeyondEnd
    */
    protected String snip(Token tokStart, Token tokBeyondEnd)
        {
        if (tokStart == tokBeyondEnd)
            {
            return "";
            }

        String[] asLines = m_asLines;
        int iStart  = tokStart.getLine();
        int ofStart = tokStart.getOffset();
        int iEnd    = tokBeyondEnd.getLine();
        int ofEnd   = tokBeyondEnd.getOffset(); // non-inclusive

        // check if the script is all contained within one line
        if (iStart == iEnd)
            {
            String sLine   = asLines[iStart];
            String sScript = sLine.substring(ofStart, ofEnd).trim();
            return sScript;
            }

        // collect lines of script
        List list = new ArrayList();
        if (ofStart > 0)
            {
            // include leading white space with the first line
            String sLine = asLines[iStart];
            while (ofStart > 0)
                {
                char ch = sLine.charAt(ofStart - 1);
                if (ch == ' ' || ch == '\t')
                    {
                    --ofStart;
                    }
                else
                    {
                    break;
                    }
                }
            }
        list.add(asLines[iStart].substring(ofStart));
        for (int i = iStart + 1; i < iEnd; ++i)
            {
            list.add(asLines[i]);
            }
        if (ofEnd > 0)
            {
            list.add(asLines[iEnd].substring(0, ofEnd));
            }

        // remove leading and trailing whitespace
        while (((String) list.get(0)).trim().length() == 0)
            {
            list.remove(0);
            }
        if (list.size() == 0)
            {
            return "";
            }
        while (((String) list.get(list.size()-1)).trim().length() == 0)
            {
            list.remove(list.size()-1);
            }

        // determine the minimum script indent
        int ofIndent = -1;
        for (int i = 0, c = list.size(); i < c; ++i)
            {
            String sLine = (String) list.get(i);
            if (sLine.trim().length() == 0)
                {
                list.set(i, "");
                }
            else
                {
                // Sun's java code uses tabs for eight spaces
                if (sLine.indexOf('\t') != -1)
                    {
                    sLine = replace(sLine, "\t", "        ");
                    list.set(i, sLine);
                    }

                // count leading spaces
                if (sLine.charAt(0) == ' ')
                    {
                    char[] ach     = sLine.toCharArray();
                    int    cSpaces = 0;
                    for (int of = 0, cch = ach.length; of < cch && ach[of] == ' '; ++of)
                        {
                        ++cSpaces;
                        }
                    if (ofIndent == -1 || cSpaces < ofIndent)
                        {
                        ofIndent = cSpaces;
                        }
                    }
                else
                    {
                    ofIndent = 0;
                    }
                }
            }

        // assemble the script
        StringBuffer sb = new StringBuffer();
        for (int i = 0, c = list.size(); i < c; ++i)
            {
            String sLine = (String) list.get(i);

            if (i > 0)
                {
                sb.append('\n');
                }

            if (sLine.length() > 0)
                {
                if (ofIndent > 0)
                    {
                    sLine = sLine.substring(ofIndent);
                    }

                sb.append(sLine);
                }
            }
        return sb.toString();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ScriptParser";

    /**
    * Debug mode.
    */
    private static final boolean DEBUG = ("JAVA.PARSER".equalsIgnoreCase(System.getProperty("DEBUG")));

    /**
    * Static initializer signature.
    */
    public static final String STATIC_INIT = "<clinit>()";

    /**
    * Imports signature.
    */
    public static final String IMPORTS = "_imports()";

    /**
    * The tokenizer options.
    */
    protected static final Properties OPTIONS = new Properties();
    static
        {
        OPTIONS.put(OPT_PARSE_COMMENTS  , "true" );
        OPTIONS.put(OPT_PARSE_EXTENSIONS, "false");
        }

    /**
    * The lexical tokenizer.
    */
    protected Tokenizer m_toker;

    /**
    * The "current" token being evaluated.
    */
    protected Token m_token;

    /**
    * The script parsed into lines.
    */
    protected String[] m_asLines;

    /**
    * The collected scripts, keyed by signature (string) with values of
    * scripts (string).
    */
    protected Map m_map;

    /**
    * The collected param names, keyed by signature (string) with values of
    * String[].
    */
    protected Map m_mapParam;

    /**
    * The package name (including a trailing dot).
    */
    protected String m_sPackage;

    /**
    * The imports "script".
    */
    protected String m_sImports;

    /**
    * The stack of class names.  The top most element is the class currently
    * being parsed.
    */
    protected Stack m_stackClass = new Stack();
    }
