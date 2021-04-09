/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Drem;
import com.tangosol.dev.assembler.Frem;
import com.tangosol.dev.assembler.Irem;
import com.tangosol.dev.assembler.Lrem;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.util.ErrorList;


/**
* Implements the modulo (%) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class ModuloExpression extends MultiplicativeExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a ModuloExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public ModuloExpression(Expression left, Token operator, Expression right)
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
            // constant int or long modulo by zero is a compile time error
            if (getType().isIntegral()
                    && ((Number) getRightExpression().getValue()).longValue() == 0)
                {
                notValidDivisor(errlist);
                // fall through
                }
            else
                {
                return super.compile(ctx, code, fReached, errlist);
                }
            }

        // Code generation for the operator '%' is as follows:
        //
        //              [left]
        //              [right]
        //              ?rem

        getLeftExpression ().compile(ctx, code, fReached, errlist);
        getRightExpression().compile(ctx, code, fReached, errlist);

        switch (getType().getTypeString().charAt(0))
            {
            case 'I':
                code.add(new Irem());
                break;

            case 'J':
                code.add(new Lrem());
                break;

            case 'F':
                code.add(new Frem());
                break;

            case 'D':
                code.add(new Drem());
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
                return nbrLeft.intValue() % nbrRight.intValue();

            case 'J':
                return nbrLeft.longValue() % nbrRight.longValue();

            case 'F':
                return nbrLeft.floatValue() % nbrRight.floatValue();

            case 'D':
                return nbrLeft.doubleValue() % nbrRight.doubleValue();

            default:
                throw new IllegalStateException();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ModuloExpression";
    }
