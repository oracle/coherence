/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.IntConstant;
import com.tangosol.dev.assembler.Iconst;
import com.tangosol.dev.assembler.Ixor;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* The logical not (!) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class NotExpression extends UnaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a NotExpression.
    *
    * @param operator  the operator token
    * @param expr      the sub-expression
    */
    public NotExpression(Token operator, Expression expr)
        {
        super(operator, expr);
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
        // get the sub-expression
        Expression expr = getExpression();

        // pre-compile the sub-expression
        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // sub-expression must be boolean
        expr.checkBoolean(errlist);

        // store the sub-expression
        setExpression(expr);

        // the result type is the type of the sub-expression
        setType(DataType.BOOLEAN);

        // JLS 16.1.5 The Boolean Operator !
        //  - V is definitely assigned after !a when true iff V is
        //    definitely assigned after a when false.
        //  - V is definitely assigned after !a when false iff V is
        //    definitely assigned after a when true.
        setUVars.negate();
        setFVars.negate();

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
        Expression expr = getExpression();

        if (!ctx.isDebug() && expr.isConstant())
            {
            // get the constant value of the sub-expression
            boolean fVal = ((Boolean) expr.getValue()).booleanValue();
            code.add(new Iconst(fVal ? FALSE : TRUE));
            }
        else
            {
            expr.compile(ctx, code, fReached, errlist);
            code.add(new Iconst(TRUE));
            code.add(new Ixor());
            }

        // normal completion possible if reachable
        return fReached;
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine if the expression has a constant value.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        return getExpression().isConstant();
        }

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        Expression expr = getExpression();
        boolean    fVal = ((Boolean) expr.getValue()).booleanValue();
        return (fVal ? Boolean.FALSE : Boolean.TRUE);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "NotExpression";

    /**
    * The assembly constant int value of boolean false.
    */
    private static final IntConstant FALSE = Constants.CONSTANT_ICONST_0;

    /**
    * The assembly constant int value of boolean true.
    */
    private static final IntConstant TRUE  = Constants.CONSTANT_ICONST_1;
    }
