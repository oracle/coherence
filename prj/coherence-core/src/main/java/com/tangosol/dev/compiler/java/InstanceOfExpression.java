/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Instanceof;
import com.tangosol.dev.assembler.ClassConstant;
import com.tangosol.dev.assembler.Ifnonnull;
import com.tangosol.dev.assembler.Goto;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Iconst;
import com.tangosol.dev.assembler.IntConstant;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the class castability test (instanceof) relational operator.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class InstanceOfExpression extends RelationalExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a InstanceOfExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public InstanceOfExpression(Expression left, Token operator, Expression right)
        {
        super(left, operator, right);
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
        // get the sub-expressions
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        // pre-compile the sub-expressions
        left  = (Expression) left .precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        right = (Expression) right.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // left and right sub-expressions must be reference types
        // (right sub-expression cannot be the null type)
        if (left.checkReference(true, errlist) & right.checkReferenceType(false, errlist))
            {
            // must be legal to attempt a cast of the left to the right type
            left.checkCastable(ctx, right.getType(), errlist);
            }

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

        // results in a boolean
        setType(BOOLEAN);

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
        if (!ctx.isDebug() && isConstant())
            {
            return super.compile(ctx, code, fReached, errlist);
            }

        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        left.compile(ctx, code, fReached, errlist);

        DataType type = right.getType();
        if (!ctx.isDebug() && left.checkAssignable(ctx, type, null))
            {
            // cast is guaranteed to succeed (identity conversion or widening
            // reference conversion) so instanceof will succeed if the value
            // of the left expression is non-null
            Label lblTrue = new Label();
            Label lblExit = new Label();

            code.add(new Ifnonnull(lblTrue));
            code.add(new Iconst(FALSE));
            code.add(new Goto(lblExit));
            code.add(lblTrue);
            code.add(new Iconst(TRUE));
            code.add(lblExit);
            }
        else
            {
            // use context to get the class constant in case
            // the data type we are dealing with is actually
            // optimized out (e.g. "is child discarded")
            code.add(new Instanceof((ClassConstant)
                    ctx.getTypeInfo(type).getConstant()));
            }

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
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        // "null instanceof T" always equals false
        return left instanceof NullExpression ||
                (left.isConstant() && left.getValue() == null);
        }

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        return Boolean.FALSE;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "InstanceOfExpression";

    /**
    * Boolean data type.
    */
    private static final DataType BOOLEAN = DataType.BOOLEAN;
    }
