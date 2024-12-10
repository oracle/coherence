/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Ishr;
import com.tangosol.dev.assembler.L2i;
import com.tangosol.dev.assembler.Lshr;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.util.ErrorList;


/**
* Implements the right shift operator (>>).
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class RightShiftExpression extends ShiftExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a RightShiftExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public RightShiftExpression(Expression left, Token operator, Expression right)
        {
        super(left, operator, right);
        }


    // ----- code generation ------------------------------------------------

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

        // Code generation for the operator '>>' is as follows:
        //
        //              [left]
        //        (int) [right]
        //              ?shr

        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        left .compile(ctx, code, fReached, errlist);
        right.compile(ctx, code, fReached, errlist);

        if (right.getType() == LONG)
            {
            code.add(new L2i());
            }

        if (left.getType() == LONG)
            {
            code.add(new Lshr());
            }
        else
            {
            code.add(new Ishr());
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
                return Integer.valueOf(nbrLeft.intValue() >> nbrRight.intValue());

            case 'J':
                return Long.valueOf(nbrLeft.longValue() >> nbrRight.intValue());

            default:
                throw new IllegalStateException();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "RightShiftExpression";
    }
