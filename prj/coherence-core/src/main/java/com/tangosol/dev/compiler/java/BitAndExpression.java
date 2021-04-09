/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Iand;
import com.tangosol.dev.assembler.Land;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the bitwise and (&) operator.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class BitAndExpression extends BitwiseExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a BitAndExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public BitAndExpression(Expression left, Token operator, Expression right)
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

        // JLS 16.1.6 The Boolean Operator &
        //  1)  V is definitely assigned after a & b when true iff V is
        //      definitely assigned after a when true or V is definitely
        //      assigned after b when true.
        //  2)  V is definitely assigned after a & b when false iff at least
        //      one of the following is true:
        //      1)  V is definitely assigned after b. (Note that if V is
        //          definitely assigned after a, it follows that V is
        //          definitely assigned after b.)
        //      2)  V is definitely assigned after a when false and V is
        //          definitely assigned after b when false.
        //  3)  V is definitely assigned before a iff V is definitely
        //      assigned before a & b.
        //  4)  V is definitely assigned before b iff V is definitely
        //      assigned after a.
        //
        // Translation for definite assignment:
        //  1)  Evaluate the left expression, and keep those results
        //  2)  Pass a copy of those results to the right expression
        //  3)  Union the true results from the left with the true
        //      results of the right to get the final true results
        //  4)  Intersect the false results from the left with the false
        //      results of the right to get the final false results
        //
        // Explanation:
        // In order for the result to be true, both left and right must be
        // true.  That means if any variables were assigned by the left when
        // true or by the right when true, then those variables are assigned
        // when the entire expression is true.  Likewise, in order for the
        // result to be false, at least one of the two (left or right) must
        // be false, but it cannot be assumed that both were false.
        // Therefore for something to be definitely assigned when the result
        // of the expression is false, it is necessary to prove that the
        // variable was assigned when the first was false AND when the second
        // was false.
        //
        // Translation for definite un-assignment (final vars):
        //  1)  Evaluate the left expression, and keep those results
        //  2)  Pass a copy of those results to the right expression
        //  3)  The false results are a union of potentially assigned
        //      variables from the left and right expressions (true or false)
        //  4)  The true results are a union of potentially assigned
        //      variables from the left (when true) and right (when true)
        //      expressions
        //
        // Explanation:
        // Similar to definite assignment, both expressions will be
        // evaluated, so if it is possible that the left expression assigned
        // a final variable, then the right must not assign it.  Since both
        // must be true for the result to be true, the variable is
        // potentially assigned if either expression (when true) potentially
        // assigns it.  Conversely, if the variable is only potentially
        // assigned by an expression when false, then it is not potentially
        // assigned when the entire expression is true.  Since either
        // expression can be false to make the entire expression false, and
        // the other can then be either true or false, it is obvious that
        // if either expression potentially assigns the variable (when either
        // true or false) then the variable is potentially assigned when the
        // entire expression is false.

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
                // union the true results from the left with the true
                // results of the right to get the final true results
                Set setRemovedTrue = setULeft.getTrueSet().getRemoved();
                setRemovedTrue.addAll(setURight.getTrueSet().getRemoved());
                setUVars.getTrueSet().removeAll(setRemovedTrue);

                // intersect the false results from the left with the false
                // results of the right to get the final false results
                Set setRemovedFalse = setULeft.getFalseSet().getRemoved();
                setRemovedFalse.retainAll(setURight.getFalseSet().getRemoved());
                setUVars.getFalseSet().removeAll(setRemovedFalse);
                }

            // definite unassignment (potential assignment of final vars)
            if (setFLeft.isModified())
                {
                // add left when true and when false to result when false
                setFVars.getFalseSet().addAll(setFLeft             .getAdded());
                // add left when true to result when true
                setFVars.getTrueSet ().addAll(setFLeft.getTrueSet().getAdded());
                }
            if (setFRight.isModified())
                {
                // add right when true and when false to result when false
                setFVars.getFalseSet().addAll(setFRight             .getAdded());
                // add right when true to result when true
                setFVars.getTrueSet ().addAll(setFRight.getTrueSet().getAdded());
                }

            // add right when true to left when true
            setType(BOOLEAN);
            }
        else if (left.checkIntegral(errlist) & right.checkIntegral(errlist))
            {
            // when true/when false processing only for boolean bitand;
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
            code.add(new Land());
            }
        else
            {
            code.add(new Iand());
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
            oVal = ((Boolean) oLeft).booleanValue() & ((Boolean) oRight).booleanValue();
            }
        else if (dt == LONG)
            {
            oVal = ((Number) oLeft).longValue() & ((Number) oRight).longValue();
            }
        else // int
            {
            oVal = ((Number) oLeft).intValue() & ((Number) oRight).intValue();
            }

        return oVal;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BitAndExpression";
    }
