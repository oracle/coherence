/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Dsub;
import com.tangosol.dev.assembler.Fsub;
import com.tangosol.dev.assembler.Isub;
import com.tangosol.dev.assembler.Lsub;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.util.ErrorList;

import java.util.Map;


/**
* Implements the subtraction operator.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class SubtractExpression extends AdditiveExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a SubtractExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public SubtractExpression(Expression left, Token operator, Expression right)
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

        // both sub-expressions must be numeric
        if (left.checkNumeric(errlist) & right.checkNumeric(errlist))
            {
            // binary numeric promotion
            left  = left .promoteNumeric(right);
            right = right.promoteNumeric(left );
            }

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

        // result type is the type of left and right (now identical)
        setType(left.getType());

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

        // Code generation for the operator '-' is as follows:
        //
        //              [left]
        //              [right]
        //              ?sub

        getLeftExpression ().compile(ctx, code, fReached, errlist);
        getRightExpression().compile(ctx, code, fReached, errlist);

        switch (getType().getTypeString().charAt(0))
            {
            case 'I':
                code.add(new Isub());
                break;

            case 'J':
                code.add(new Lsub());
                break;

            case 'F':
                code.add(new Fsub());
                break;

            case 'D':
                code.add(new Dsub());
                break;

            default:
                throw new IllegalStateException();
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
        boolean fVal;

        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        Number nbrLeft  = (Number) left .getValue();
        Number nbrRight = (Number) right.getValue();

        switch (left.getType().getTypeString().charAt(0))
            {
            case 'I':
                return nbrLeft.intValue() - nbrRight.intValue();

            case 'J':
                return nbrLeft.longValue() - nbrRight.longValue();

            case 'F':
                return nbrLeft.floatValue() - nbrRight.floatValue();

            case 'D':
                return nbrLeft.doubleValue() - nbrRight.doubleValue();

            default:
                throw new IllegalStateException();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "SubtractExpression";
    }
