/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.Iconst;
import com.tangosol.dev.assembler.IntConstant;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Ifne;
import com.tangosol.dev.assembler.Goto;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the logical and (&&) operator.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class AndExpression extends LogicalExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a AndExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public AndExpression(Expression left, Token operator, Expression right)
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

        // JLS 16.1.3 The Boolean Operator &&
        //  - V is definitely assigned after a && b when true iff V is
        //    definitely assigned after a when true or V is definitely
        //    assigned after b when true.
        //  - V is definitely assigned after a && b when false iff V is
        //    definitely assigned after a when false and V is definitely
        //    assigned after b when false.
        //  - V is definitely assigned before a iff V is definitely assigned
        //    before a && b.
        //  - V is definitely assigned before b iff V is definitely assigned
        //    after a when true.
        //
        // Translation for definite assignment:
        //  1.  The false result of the left precompile is the false result
        //      for the expression
        //  2.  Take the true result of the left precompile and pass it to
        //      the right precompile
        //  3.  The true result of the right precompile is the true result
        //      for the expression
        //
        // Translation for definite unassignment (final vars):
        //  1.  Take the true result of the left precompile and pass it to
        //      the right precompile
        //  2.  The union of the false result of the left and right
        //      precompiles is the false result for the expression
        //  3.  The true result of the right precompile is the true result
        //      for the expression

        // pre-compile the left sub-expression
        left  = (Expression) left .precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // use the true result from the left as the input to the right
        DualSet setUTrue = new DualSet(setUVars.getTrueSet());
        DualSet setFTrue = new DualSet(setFVars.getTrueSet());

        // pre-compile the right sub-expression
        right = (Expression) right.precompile(ctx, setUTrue, setFTrue, mapThrown, errlist);

        // definite assignment:  discard the false result from the right
        // sub-expression
        if (setUTrue.isModified())
            {
            if (!setUTrue.isSingle())
                {
                setUTrue.getFalseSet().clear();
                }
            setUTrue.resolve();
            }
        if (!setUVars.isModified())
            {
            setUVars.merge();
            }

        // definite un-assignment:
        if (setFVars.isModified() || setFTrue.isModified())
            {
            // the union of the false result of the left and right
            // precompiles is the false result for the expression
            Set setFRightFalse = (setFTrue.isSingle() ? (Set) setFTrue : setFTrue.getFalseSet());
            setFVars.getFalseSet().addAll(setFRightFalse);

            // the true result of the right precompile is the true result
            // for the expression
            if (!setFTrue.isSingle())
                {
                setFTrue.getFalseSet().clear();
                }
            setFTrue.resolve();
            }
        if (!setFVars.isModified())
            {
            setFVars.merge();
            }

        // sub-expressions must be boolean
        left .checkBoolean(errlist);
        right.checkBoolean(errlist);

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

        // the result type is boolean
        setType(DataType.BOOLEAN);

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
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        if (!ctx.isDebug() && isConstant())
            {
            boolean fVal = ((Boolean) getValue()).booleanValue();
            code.add(new Iconst(fVal ? TRUE : FALSE));

            // if right side of expression is not constant, then log an
            // error that it is not reachable
            if (fReached && !right.isConstant())
                {
                right.notReached(errlist);
                }
            }
        else
            {
            // Code generation for the logical AND is as follows:
            //
            //  lbl_Entry:                      ; stack is considered empty
            //              [left]              ; compile left expression here
            //              ifne    lbl_Right   ; must eval right if left==true
            //              iconst  0           ; otherwise left==false
            //              goto    lbl_Exit    ; so exit conditional eval
            //  lbl_Right:
            //              [right]             ; compile right expression here
            //  lbl_Exit:                       ; stack contains word value 0 or 1

            Label lblRight = new Label();
            Label lblExit  = new Label();

            left.compile(ctx, code, fReached, errlist);
            code.add(new Ifne(lblRight));
            code.add(new Iconst(FALSE));
            code.add(new Goto(lblExit));
            code.add(lblRight);
            right.compile(ctx, code, fReached, errlist);
            code.add(lblExit);
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
        // (x && y) is constant iff:
        //  1)  x is constant with value of false, or
        //  2)  x is constant and y is constant
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        if (left.isConstant())
            {
            boolean fLeft = ((Boolean) left.getValue()).booleanValue();
            return (fLeft == false) || right.isConstant();
            }

        return false;
        }

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        // (x && y) is false iff:
        //  1)  x is false, or
        //  2)  y is false
        Expression left = getLeftExpression();
        if (((Boolean) left.getValue()).booleanValue() == false)
            {
            return Boolean.FALSE;
            }

        Expression right = getRightExpression();
        if (((Boolean) right.getValue()).booleanValue() == false)
            {
            return Boolean.FALSE;
            }

        return Boolean.TRUE;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "AndExpression";

    /**
    * The assembly constant int value of boolean true.
    */
    private static final IntConstant TRUE  = Constants.CONSTANT_ICONST_1;

    /**
    * The assembly constant int value of boolean false.
    */
    private static final IntConstant FALSE = Constants.CONSTANT_ICONST_0;
    }
