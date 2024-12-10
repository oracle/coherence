/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Ior;
import com.tangosol.dev.assembler.Lor;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the bitwise or (|) operator.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class BitOrExpression extends BitwiseExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a BitOrExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public BitOrExpression(Expression left, Token operator, Expression right)
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

        // JLS 16.1.7 The Boolean Operator |
        //  1)  V is definitely assigned after a | b when true iff at least
        //      one of the following is true:
        //      1)  V is definitely assigned after b. (Note that if V is
        //          definitely assigned after a, it follows that V is
        //          definitely assigned after b.)
        //      2)  V is definitely assigned after a when true and V is
        //          definitely assigned after b when true.
        //  2)  V is definitely assigned after a | b when false iff V
        //      is definitely assigned after a when false or V is
        //      definitely assigned after b when false.
        //  3)  V is definitely assigned before a iff V is definitely
        //      assigned before a | b.
        //  4)  V is definitely assigned before b iff V is definitely
        //      assigned after a.
        //
        // Translation for definite assignment:
        //  1)  Evaluate the left expression, and keep those results
        //  2)  Pass a copy of those results to the right expression
        //  3)  Intersect the true results from the left with the true
        //      results of the right to get the final true results
        //  4)  Union the false results from the left with the false
        //      results of the right to get the final false results
        //
        // Translation for definite un-assignment (final vars):
        //  1)  Evaluate the left expression, and keep those results
        //  2)  Pass a copy of those results to the right expression
        //  3)  The false results are a union of potentially assigned
        //      variables from the left (when false) and right (when
        //      false) expressions
        //  4)  The true results are a union of potentially assigned
        //      variables from the left and right expressions (true or false)
        //
        // (See also the additional explanatory comments in BitAndExpression)

        // pre-compile the left sub-expression
        DualSet setULeft = new DualSet(setUVars);
        DualSet setFLeft = new DualSet(setFVars);
        left  = (Expression) left .precompile(ctx, setULeft, setFLeft, mapThrown, errlist);

        // pre-compile the right sub-expression
        DualSet setURight = new DualSet(setULeft);
        DualSet setFRight = new DualSet(setFLeft);
        right = (Expression) right.precompile(ctx, setURight, setFRight, mapThrown, errlist);

        // sub-expressions must both be boolean or must both be integral
        if (left.getType() == BOOLEAN && right.getType() == BOOLEAN)
            {
            // definite assignment
            if (setULeft.isModified() || setURight.isModified())
                {
                // intersect the true results from the left with the true
                // results of the right to get the final true results
                Set setRemovedTrue = setULeft.getTrueSet().getRemoved();
                setRemovedTrue.retainAll(setURight.getTrueSet().getRemoved());
                setUVars.getTrueSet().removeAll(setRemovedTrue);

                // union the false results from the left with the false
                // results of the right to get the final false results
                Set setRemovedFalse = setULeft.getFalseSet().getRemoved();
                setRemovedFalse.addAll(setURight.getFalseSet().getRemoved());
                setUVars.getFalseSet().removeAll(setRemovedFalse);
                }

            // definite unassignment (potential assignment of final vars)
            if (setFLeft.isModified())
                {
                // add left when true and when false to result when true
                setFVars.getTrueSet ().addAll(setFLeft              .getAdded());
                // add left when false to result when false
                setFVars.getFalseSet().addAll(setFLeft.getFalseSet().getAdded());
                }
            if (setFRight.isModified())
                {
                // add right when true and when false to result when true
                setFVars.getTrueSet ().addAll(setFRight              .getAdded());
                // add right when false to result when false
                setFVars.getFalseSet().addAll(setFRight.getFalseSet().getAdded());
                }

            // add right when true to left when true
            setType(BOOLEAN);
            }
        else if (left.checkIntegral(errlist) & right.checkIntegral(errlist))
            {
            // when true/when false processing only for boolean bitor;
            // just merge/commit the changes
            setURight.resolve();
            setFRight.resolve();
            setULeft .resolve();
            setFLeft .resolve();

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
            code.add(new Lor());
            }
        else
            {
            code.add(new Ior());
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
            oVal = ((Boolean) oLeft).booleanValue() | ((Boolean) oRight).booleanValue();
            }
        else if (dt == LONG)
            {
            oVal = ((Number) oLeft).longValue() | ((Number) oRight).longValue();
            }
        else // int
            {
            oVal = ((Number) oLeft).intValue() | ((Number) oRight).intValue();
            }

        return oVal;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BitOrExpression";
    }
