/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.ClassConstant;
import com.tangosol.dev.assembler.New;
import com.tangosol.dev.assembler.Dup;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.compiler.TypeInfo;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the class instance allocation (new) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class NewClassExpression extends NewExpression implements Constants, TokenConstants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a NewClassExpression.
    *
    * @param block      the containing block
    * @param tokNew     the first token (new)
    * @param exprType   the type expression
    * @param tokLParen  the left parenthesis
    * @param params     the parameter expressions
    * @param tokRParen  the right parenthesis
    */
    public NewClassExpression(Block block, Token tokNew, TypeExpression exprType, Token tokLParen, Expression[] params, Token tokRParen)
        {
        super(block, tokNew, tokRParen, exprType);

        this.m_tokLParen    = tokLParen;
        this.m_aexprParams  = params;
        this.m_tokRParen    = tokRParen;
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
        // get the type being instantiated
        super.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        TypeExpression exprType = getTypeExpression();

        DataType dtClass = exprType.getType();
        if (dtClass.isClass() || dtClass.isComponent())
            {
            TypeInfo info = ctx.getTypeInfo(dtClass);
            if (info == null)
                {
                String sFull = dtClass.toString();
                int    ofDot = sFull.lastIndexOf('.');
                String sPkg  = (ofDot < 0 ? "" : sFull.substring(0, ofDot));
                String sName = (ofDot < 0 ? sFull : sFull.substring(ofDot + 1));
                logError(ERROR, TYPE_NOT_FOUND, new String[] {sName, sPkg}, errlist);
                }
            else if (info.isAbstract())
                {
                logError(ERROR, ABSTRACT_TYPE, new String[] {dtClass.toString()}, errlist);
                }
            // 2000.04.18  cp  allow static child to be new'd
            else if (info.isStatic() && info.getParentInfo() == null)
                {
                logError(ERROR, STATIC_TYPE, new String[] {dtClass.toString()}, errlist);
                }
            else
                {
                // store type to instantiate
                m_typeinfo = info;

                // determine the constructor to use
                Token tokName = new Token(IDENTIFIER, NONE, IDENT, null, CONSTRUCTOR_NAME,
                        exprType.getEndLine(), exprType.getEndOffset(), 0);
                InvocationExpression exprInit = new InvocationExpression(exprType, tokName,
                        m_tokLParen, m_aexprParams, m_tokRParen);
                m_exprInit = exprInit = (InvocationExpression) exprInit.precompile(
                        ctx, setUVars, setFVars, mapThrown, errlist);
                }

            setType(dtClass);
            }

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
        // code for "new <class>(<p0>, ..., <pn>)"
        //          new [class]
        //          dup                         // if not discarded
        //          [p0]
        //          ...
        //          [pn]
        //          invokespecial  "<init>"

        // instantiate the class
        code.add(new New((ClassConstant) m_typeinfo.getConstant()));

        // if the reference is not discarded, dup it (since the initializer
        // will use one reference)
        if (!isDiscarded())
            {
            code.add(new Dup());
            }

        // invoke the initializer
        m_exprInit.compile(ctx, code, fReached, errlist);

        return fReached;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the constructor parameters.
    *
    * @return  the constructor parameters as an array of expressions
    */
    public Expression[] getParameters()
        {
        return m_aexprParams;
        }


    // ----- Element methods ------------------------------------------------

    /**
    * Print the element information.
    *
    * @param sIndent
    */
    public void print(String sIndent)
        {
        super.print(sIndent);

        out(sIndent + "  parameters:");
        Expression[] aexpr = m_aexprParams;
        int          cexpr = aexpr.length;
        for (int i = 0; i < cexpr; ++i)
            {
            aexpr[i].print(sIndent + "    ");
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "NewClassExpression";

    /**
    * The left parenthesis.
    */
    private Token m_tokLParen;

    /**
    * The array of parameters.
    */
    private Expression[] m_aexprParams;

    /**
    * The right parenthesis.
    */
    private Token m_tokRParen;

    /**
    * The type to instantiate.
    */
    private TypeInfo m_typeinfo;

    /**
    * The selected constructor.
    */
    private InvocationExpression m_exprInit;
    }
