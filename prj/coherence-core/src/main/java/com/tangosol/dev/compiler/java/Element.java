/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;

import com.tangosol.dev.compiler.CompilerErrorInfo;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;
import java.util.Iterator;


/**
* This class implements a Java script language element.
*
* @version 1.00, 09/14/98
* @author  Cameron Purdy
*/
public abstract class Element
        extends Base
        implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a language element.
    *
    * @param block  the block (scope) within which this element exists
    * @param token  the first or only token making up the language element
    */
    protected Element(Block block, Token token)
        {
        this.block    = block;
        this.tokFirst = token;
        }

    /**
    * Construct a language element.
    *
    * @param block     the block (scope) within which this element exists
    * @param tokFirst  the first or only token making up the language element
    * @param tokLast   the last token making up the language element
    */
    protected Element(Block block, Token tokFirst, Token tokLast)
        {
        this.block    = block;
        this.tokFirst = tokFirst;
        this.tokLast  = tokLast;
        }


    // ----- code generation ------------------------------------------------

    /**
    * Perform semantic checks, parse tree re-organization, name binding,
    * and optimizations.
    *
    * @param ctx        the compiler context
    * @param setUVars   the set of potentially unassigned variables
    * @param setFVars   the set of potentially assigned final variables
    * @param mapThrown  the set of potentially thrown checked exceptions
    * @param errlist    the error list
    *
    * @return the resulting language element (typically this)
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Element precompile(Context ctx, DualSet setUVars, DualSet setFVars, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        return this;
        }

    /**
    * Perform final optimizations and code generation.
    *
    * @param ctx       the compiler context
    * @param code      the assembler code attribute to compile to
    * @param fReached  true if this language element is reached (JLS 14.19)
    * @param errlist   the error list to log errors to
    *
    * @return true if the element can complete normally (JLS 14.1)
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean compile(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        return true;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the block which contains this element.
    *
    * @return the containing block
    */
    public Block getBlock()
        {
        return block;
        }

    /**
    * Get the first token which comprises the language element.
    *
    * @return the first token which comprises the language element
    */
    public Token getStartToken()
        {
        return tokFirst;
        }

    /**
    * Set the first token which comprises the language element.
    *
    * @param tokFirst  the first token which comprises the language element
    */
    protected void setStartToken(Token tokFirst)
        {
        this.tokFirst = tokFirst;
        }

    /**
    * Get the line number which the language element begins on.
    *
    * @return the starting line number
    */
    public int getStartLine()
        {
        return getStartToken().getLine();
        }

    /**
    * Get the offset within the line where the language element begins.
    *
    * @return the starting offset
    */
    public int getStartOffset()
        {
        return getStartToken().getOffset();
        }

    /**
    * Get the last token which comprises the language element.
    *
    * @return the last token which comprises the language element
    */
    public Token getEndToken()
        {
        return tokLast == null ? tokFirst : tokLast;
        }

    /**
    * Set the last token which comprises the language element.
    *
    * @param tokLast  the last token which comprises the language element
    */
    protected void setEndToken(Token tokLast)
        {
        this.tokLast = tokLast;
        }

    /**
    * Get the line number which the language element ends on.
    *
    * @return the ending line number
    */
    public int getEndLine()
        {
        return getEndToken().getLine();
        }

    /**
    * Get the offset within the line where the language element ends.
    *
    * @return the ending offset
    */
    public int getEndOffset()
        {
        Token token = getEndToken();
        return token.getOffset() + token.getLength();
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
        logError(nSeverity, sCode, asParams, errlist,
                getStartLine(), getStartOffset(), getEndLine(), getEndOffset());
        }

    /**
    * Logs the passed error in the error list.
    *
    * @param nSeverity  severity of the error as defined by ErrorList.Constants
    * @param sCode      error code, as defined by the class logging the error
    * @param asParams   replaceable parameters for the error message
    * @param errlist    the error list
    * @param iStart
    * @param ofStart
    * @param iEnd
    * @param ofEnd
    *
    * @exception CompilerException If the error list overflows.
    */
    protected static void logError(int nSeverity, String sCode, String[] asParams, ErrorList errlist, int iStart, int ofStart, int iEnd, int ofEnd)
            throws CompilerException
        {
        if (errlist != null)
            {
            try
                {
                errlist.add(new CompilerErrorInfo(nSeverity, sCode, RESOURCES, asParams,
                        iStart, ofStart, iEnd, ofEnd));
                }
            catch (ErrorList.OverflowException e)
                {
                throw new CompilerException();
                }
            }
        }


    // ----- debug methods --------------------------------------------------

    /**
    * Print the element information.
    */
    public void print()
        {
        out();
        }

    /**
    * Print the element information.
    *
    * @param sIndent
    */
    public void print(String sIndent)
        {
        out(sIndent + toString());
        }

    /**
    * Print the set of variables.
    *
    * @param set  the set of variables
    */
    public void printVars(Set set)
        {
        for (Iterator iter = set.iterator(); iter.hasNext(); )
            {
            Variable var = (Variable) iter.next();
            out(var.getType().toString() + " " + var.getName());
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the element information as a string.
    *
    * @return a human-readable description of the element
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append('(');
        if (getStartToken() == null)
            {
            sb.append("?, ?, ");
            }
        else
            {
            sb.append(getStartLine())
              .append(", ")
              .append(getStartOffset())
              .append(", ");
            }

        if (getStartToken() == null)
            {
            sb.append("?, ?");
            }
        else
            {
            sb.append(getEndLine())
              .append(", ")
              .append(getEndOffset());
            }
        sb.append(") ");

        String sClass = getClass().getName();
        int of = sClass.lastIndexOf('.');
        if (of < 0)
            {
            of = 0;
            }
        sClass = sClass.substring(of + 1, sClass.length());
        sb.append(sClass);

        return sb.toString();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The block of code that the element is within.
    */
    private Block block;

    /**
    * The first token comprising the language element.
    */
    private Token tokFirst;

    /**
    * The last token comprising the language element.
    */
    private Token tokLast;
    }
