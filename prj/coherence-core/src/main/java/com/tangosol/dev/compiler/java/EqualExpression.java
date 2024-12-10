/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.If_icmpeq;
import com.tangosol.dev.assembler.If_acmpeq;
import com.tangosol.dev.assembler.Ifnull;
import com.tangosol.dev.assembler.Lcmp;
import com.tangosol.dev.assembler.Fcmpl;
import com.tangosol.dev.assembler.Dcmpl;
import com.tangosol.dev.assembler.Ifeq;
import com.tangosol.dev.assembler.Goto;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Iconst;
import com.tangosol.dev.assembler.IntConstant;
import com.tangosol.dev.assembler.Ixor;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;


/**
* Implements the "is equal to" (==) expression
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class EqualExpression extends EqualityExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a EqualExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public EqualExpression(Expression left, Token operator, Expression right)
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

        // JLS 16.1.9 The Boolean Operator ==
        //  1)  V is definitely assigned after a == b when true iff at least
        //      one of the following is true:
        //      1)  V is definitely assigned after b.
        //      2)  V is definitely assigned after a when true and V is
        //          definitely assigned after b when false.
        //      3)  V is definitely assigned after a when false and V is
        //          definitely assigned after b when true.
        //  2)  V is definitely assigned after a == b when false iff at least
        //      one of the following is true:
        //      1)  V is definitely assigned after b.
        //      2)  V is definitely assigned after a when true and V is
        //          definitely assigned after b when true.
        //      3)  V is definitely assigned after a when false and V is
        //          definitely assigned after b when false.
        //  3)  V is definitely assigned before a iff V is definitely
        //      assigned before a == b.
        //  4)  V is definitely assigned before b iff V is definitely
        //      assigned after a.
        //
        // Explanation:  The opposite of !=.

        // pre-compile the left sub-expression
        DualSet setULeft = new DualSet(setUVars);
        left  = (Expression) left .precompile(ctx, setULeft, setFVars, mapThrown, errlist);

        // pre-compile the right sub-expression
        DualSet setURight = new DualSet(setULeft);
        right = (Expression) right.precompile(ctx, setURight, setFVars, mapThrown, errlist);

        // sub-expressions may both be boolean
        if (left.getType() == BOOLEAN)
            {
            // definite assignment
            if (setULeft.isModified() || setURight.isModified())
                {
                Set setLeftTrue   = setULeft .getTrueSet ().getRemoved();
                Set setLeftFalse  = setULeft .getFalseSet().getRemoved();
                Set setRightTrue  = setURight.getTrueSet ().getRemoved();
                Set setRightFalse = setURight.getFalseSet().getRemoved();

                Set setAssigned = new HashSet(setLeftTrue);
                setAssigned.retainAll(setRightFalse);
                setUVars.getTrueSet().removeAll(setAssigned);

                setAssigned = new HashSet(setLeftFalse);
                setAssigned.retainAll(setRightTrue);
                setUVars.getTrueSet().removeAll(setAssigned);

                setAssigned = new HashSet(setLeftTrue);
                setAssigned.retainAll(setRightTrue);
                setUVars.getFalseSet().removeAll(setAssigned);

                setAssigned = new HashSet(setLeftFalse);
                setAssigned.retainAll(setRightFalse);
                setUVars.getFalseSet().removeAll(setAssigned);
                }

            right.checkBoolean(errlist);
            }
        else
            {
            // when true/when false processing only for boolean types;
            // just merge/commit the changes
            setURight.resolve();
            setULeft .resolve();

            // sub-expressions may both be reference types
            if (left.getType().isReference())
                {
                if (right.checkReference(errlist))
                    {
                    left.checkComparable(ctx, right, errlist);
                    }
                }
            // otherwise they must both be numeric
            else if (left.checkNumeric(errlist) & right.checkNumeric(errlist))
                {
                // binary numeric promotion
                left  = left .promoteNumeric(right);
                right = right.promoteNumeric(left );
                }
            }

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

        // equality tests always result in a boolean
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

        // Code generation for the operator '==' is as follows:
        //
        // boolean with one expression (e.g. right) true:
        //              [left]
        //
        // boolean with one expression (e.g. right) false:
        //              [left]
        //              iconst      1
        //              ixor
        //
        // boolean otherwise:
        //              [left]
        //              [right]
        //              ixor
        //              iconst      1
        //              ixor
        //
        // int:
        //              [left]
        //              [right]
        //              if_icmpeq   lbl_True
        //              iconst      0
        //              goto        lbl_Exit
        //  lbl_True:   iconst      1
        //  lbl_Exit:
        //
        // long:
        //              [left]
        //              [right]
        //              lcmp
        //              ifeq        lbl_True
        //              iconst      0
        //              goto        lbl_Exit
        //  lbl_True:   iconst      1
        //  lbl_Exit:
        //
        // float:
        //              [left]
        //              [right]
        //              fcmpl
        //              ifeq        lbl_True
        //              iconst      0
        //              goto        lbl_Exit
        //  lbl_True:   iconst      1
        //  lbl_Exit:
        //
        //  double:
        //              [left]
        //              [right]
        //              dcmpl
        //              ifeq        lbl_True
        //              iconst      0
        //              goto        lbl_Exit
        //  lbl_True:   iconst      1
        //  lbl_Exit:
        //
        // reference with one expression (e.g. right) null:
        //              [left]
        //              ifnull      lbl_True
        //              iconst      0
        //              goto        lbl_Exit
        //  lbl_True:   iconst      1
        //  lbl_Exit:
        //
        // reference otherwise:
        //              [left]
        //              [right]
        //              if_acmpeq   lbl_True
        //              iconst      0
        //              goto        lbl_Exit
        //  lbl_True:   iconst      1
        //  lbl_Exit:

        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        DataType dt = left.getType();
        if (dt.isNumeric())
            {
            Label lblTrue = new Label();
            Label lblExit = new Label();

            left .compile(ctx, code, fReached, errlist);
            right.compile(ctx, code, fReached, errlist);

            switch (dt.getTypeString().charAt(0))
                {
                case 'I':
                    code.add(new If_icmpeq(lblTrue));
                    break;

                case 'J':
                    code.add(new Lcmp());
                    code.add(new Ifeq(lblTrue));
                    break;

                case 'F':
                    code.add(new Fcmpl());
                    code.add(new Ifeq(lblTrue));
                    break;

                case 'D':
                    code.add(new Dcmpl());
                    code.add(new Ifeq(lblTrue));
                    break;

                default:
                    throw new IllegalStateException();
                }

            code.add(new Iconst(FALSE));
            code.add(new Goto(lblExit));
            code.add(lblTrue);
            code.add(new Iconst(TRUE));
            code.add(lblExit);
            }
        else if (dt == BOOLEAN)
            {
            // check if only one of the two binary expressions needs to be
            // evaluated
            boolean    fUnary = false;
            Expression expr   = null;
            boolean    fValue = false;
            if (left instanceof BooleanExpression || (!ctx.isDebug() && left.isConstant()))
                {
                // "true==expr" or "false==expr"
                fUnary = true;
                expr   = right;
                fValue = ((Boolean) left.getValue()).booleanValue();
                }
            else if (right instanceof BooleanExpression || (!ctx.isDebug() && right.isConstant()))
                {
                // "expr==true" or "expr==false"
                fUnary = true;
                expr   = left;
                fValue = ((Boolean) right.getValue()).booleanValue();
                }

            if (fUnary)
                {
                expr.compile(ctx, code, fReached, errlist);
                if (fValue == false)
                    {
                    code.add(new Iconst(TRUE));
                    code.add(new Ixor());
                    }
                }
            else
                {
                left .compile(ctx, code, fReached, errlist);
                right.compile(ctx, code, fReached, errlist);
                code.add(new Ixor());
                code.add(new Iconst(TRUE));
                code.add(new Ixor());
                }
            }
        else // reference
            {
            Label lblTrue = new Label();
            Label lblExit = new Label();

            if (left instanceof NullExpression ||
                    (!ctx.isDebug() && left.isConstant() && left.getValue() == null))
                {
                right.compile(ctx, code, fReached, errlist);
                code.add(new Ifnull(lblTrue));
                }
            else if (right instanceof NullExpression ||
                    (!ctx.isDebug() && right.isConstant() && right.getValue() == null))
                {
                left.compile(ctx, code, fReached, errlist);
                code.add(new Ifnull(lblTrue));
                }
            else
                {
                left .compile(ctx, code, fReached, errlist);
                right.compile(ctx, code, fReached, errlist);
                code.add(new If_acmpeq(lblTrue));
                }

            code.add(new Iconst(FALSE));
            code.add(new Goto(lblExit));
            code.add(lblTrue);
            code.add(new Iconst(TRUE));
            code.add(lblExit);
            }

        // normal completion possible if reachable
        return fReached;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "EqualExpression";
    }
