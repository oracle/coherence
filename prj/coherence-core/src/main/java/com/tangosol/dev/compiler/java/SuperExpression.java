/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the reserved name "super".
*
* @version 1.00, 01/25/99
* @author  Cameron Purdy
*/
public class SuperExpression extends Expression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a SuperExpression.
    *
    * @param block    the containing block
    * @param tokThis  the "this" token
    */
    public SuperExpression(Block block, Token tokThis)
        {
        super(block, tokThis);
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
        Expression expr    = this;
        if (!m_fSuperAllowed)
            {
            logError(ERROR, SUPER_ILLEGAL, null, errlist);
            }
        else
            {
            // super class
            // 2001.05.02 cp  to resolve a problem in personalization, this
            //                was modified to use the data type containing
            //                the actual super method, not the data type of
            //                the super class, because the super class is not
            //                always available (e.g. when compiling from
            //                within a classloader)
            // DataType dtSuper = ctx.getMethodInfo().getTypeInfo().getSuperInfo().getDataType();
            DataType dtSuper = ctx.getSuperInfo().getTypeInfo().getDataType();

            if (ctx.getMethodInfo().isStatic())
                {
                // convert SuperExpression to a TypeExpression
                expr = new TypeExpression(getBlock(), getStartToken(), dtSuper);
                }
            else
                {
                // convert SuperExpression to a special ThisExpression
                Token tokSuper = getStartToken();
                Token tokThis  = new Token(Token.TOK_THIS,
                        tokSuper.getLine(), tokSuper.getOffset(), tokSuper.getLength());
                expr = new VariableExpression(getBlock(), tokThis);
                }

            expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
            expr.setType(dtSuper);
            }

        return expr;
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
        throw new IllegalStateException();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Specify that super is allowed to occur at this point in the parse tree.
    */
    protected void allowSuper()
        {
        m_fSuperAllowed = true;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "SuperExpression";

    /**
    * Super allowed.
    */
    private boolean m_fSuperAllowed;
    }
