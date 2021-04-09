/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Ixor;
import com.tangosol.dev.assembler.Lxor;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;

/**
* Implements the bitwise exclusive or (^) operator.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class BitXorExpression extends BitwiseExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a BitXorExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public BitXorExpression(Expression left, Token operator, Expression right)
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

        // JLS 16.1.8 The Boolean Operator ^
        //  1)  V is definitely assigned after a ^ b when true iff at least
        //      one of the following is true:
        //      1)  V is definitely assigned after b.
        //      2)  V is definitely assigned after a when true and V is
        //          definitely assigned after b when true.
        //      3)  V is definitely assigned after a when false and V is
        //          definitely assigned after b when false.
        //  2)  V is definitely assigned after a ^ b when false iff at least
        //      one of the following is true:
        //      1)  V is definitely assigned after b.
        //      2)  V is definitely assigned after a when true and V is
        //          definitely assigned after b when false.
        //      3)  V is definitely assigned after a when false and V is
        //          definitely assigned after b when true.
        //  3)  V is definitely assigned before a iff V is definitely
        //      assigned before a ^ b.
        //  4)  V is definitely assigned before b iff V is definitely
        //      assigned after a.
        //
        // Explanation for definite assignment:
        // The result of a^b is true if a and b are different; if a and b are
        // the same then the result is false.  So if the result is true, that
        // means that either a or b was true and either a or b was false, so
        // a variable is assigned (when a^b is true) if the variable is
        // assigned when a is true AND when b is true (since we don't know
        // which one will be true) or when a is false AND when b is false
        // (again, since we don't know which one will be false).  The
        // opposite is true for when a^b is false, since one must be true and
        // one must be false, so the variable is assigned by a^b if it is
        // assigned when a is true AND when b is false or when a is false AND
        // when b is true.
        //
        // Explanation for definite un-assignment (final vars):
        // It is impossible to determine if either a or b were true or false
        // regardless of the result, so treat this like any other binary
        // expression.

        // pre-compile the left sub-expression
        DualSet setULeft = new DualSet(setUVars);
        left  = (Expression) left .precompile(ctx, setULeft, setFVars, mapThrown, errlist);

        // pre-compile the right sub-expression
        DualSet setURight = new DualSet(setULeft);
        right = (Expression) right.precompile(ctx, setURight, setFVars, mapThrown, errlist);

        // sub-expressions must both be boolean or must both be integral
        if (left.getType() == BOOLEAN && right.getType() == BOOLEAN)
            {
            // definite assignment
            if (setULeft.isModified() || setURight.isModified())
                {
                Set setLeftTrue   = setULeft .getTrueSet ().getRemoved();
                Set setLeftFalse  = setULeft .getFalseSet().getRemoved();
                Set setRightTrue  = setURight.getTrueSet ().getRemoved();
                Set setRightFalse = setURight.getFalseSet().getRemoved();

                Set setAssigned = new HashSet(setLeftTrue);
                setAssigned.retainAll(setRightTrue);
                setUVars.getTrueSet().removeAll(setAssigned);

                setAssigned = new HashSet(setLeftFalse);
                setAssigned.retainAll(setRightFalse);
                setUVars.getTrueSet().removeAll(setAssigned);

                setAssigned = new HashSet(setLeftTrue);
                setAssigned.retainAll(setRightFalse);
                setUVars.getFalseSet().removeAll(setAssigned);

                setAssigned = new HashSet(setLeftFalse);
                setAssigned.retainAll(setRightTrue);
                setUVars.getFalseSet().removeAll(setAssigned);
                }

            // add right when true to left when true
            setType(BOOLEAN);
            }
        else if (left.checkIntegral(errlist) & right.checkIntegral(errlist))
            {
            // when true/when false processing only for boolean bitand;
            // just merge/commit the changes
            setURight.resolve();
            setULeft .resolve();

            // binary numeric promotion
            left  = left .promoteNumeric(right);
            right = right.promoteNumeric(left );
            setType(left.getType());
            }
        else
            {
            // invalid types; pretend to be int
            setType(INT);
            }

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

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
            // Expression has compiler implementation for constant values
            return super.compile(ctx, code, fReached, errlist);
            }

        // compile the sub-expressions
        getLeftExpression() .compile(ctx, code, fReached, errlist);
        getRightExpression().compile(ctx, code, fReached, errlist);

        // type is either word (boolean, int) or dword (long)
        if (getType() == DataType.LONG)
            {
            code.add(new Lxor());
            }
        else
            {
            code.add(new Ixor());
            }

        // normal completion possible if reachable
        return fReached;
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        Object oLeft  = getLeftExpression ().getValue();
        Object oRight = getRightExpression().getValue();
        Object oVal;

        DataType dt = getType();
        if (dt == BOOLEAN)
            {
            oVal = ((Boolean) oLeft).booleanValue() ^ ((Boolean) oRight).booleanValue();
            }
        else if (dt == LONG)
            {
            oVal = ((Number) oLeft).longValue() ^ ((Number) oRight).longValue();
            }
        else // int
            {
            oVal = ((Number) oLeft).intValue() ^ ((Number) oRight).intValue();
            }

        return oVal;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BitXorExpression";
    }
