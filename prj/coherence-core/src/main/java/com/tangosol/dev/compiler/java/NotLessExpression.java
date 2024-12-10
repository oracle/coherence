/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.If_icmpge;
import com.tangosol.dev.assembler.Lcmp;
import com.tangosol.dev.assembler.Fcmpl;
import com.tangosol.dev.assembler.Dcmpl;
import com.tangosol.dev.assembler.Ifge;
import com.tangosol.dev.assembler.Goto;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Iconst;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the greater than or equals to (>=) relational operator.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class NotLessExpression extends RelationalExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a NotLessExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public NotLessExpression(Expression left, Token operator, Expression right)
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
        else
            {
            // Code generation for the operator '>=' is as follows:
            //
            //              [left]
            //              [right]
            //              [test]
            //              iconst      0
            //              goto        lbl_Exit
            //  lbl_True:   iconst      1
            //  lbl_Exit:
            //
            // The test portion is compiled by this expression based on type:
            //
            //  int:        if_icmpge   lbl_True
            //
            //  long:       lcmp
            //              ifge        lbl_True
            //
            //  float:      fcmpl
            //              ifge        lbl_True
            //
            //  double:     dcmpl
            //              ifge        lbl_True

            Expression left  = getLeftExpression();
            Expression right = getRightExpression();

            Label lblTrue = new Label();
            Label lblExit = new Label();

            left .compile(ctx, code, fReached, errlist);
            right.compile(ctx, code, fReached, errlist);

            switch (left.getType().getTypeString().charAt(0))
                {
                case 'I':
                    code.add(new If_icmpge(lblTrue));
                    break;

                case 'J':
                    code.add(new Lcmp());
                    code.add(new Ifge(lblTrue));
                    break;

                case 'F':
                    code.add(new Fcmpl());
                    code.add(new Ifge(lblTrue));
                    break;

                case 'D':
                    code.add(new Dcmpl());
                    code.add(new Ifge(lblTrue));
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
                fVal = (nbrLeft.intValue() >= nbrRight.intValue());
                break;

            case 'J':
                fVal = (nbrLeft.longValue() >= nbrRight.longValue());
                break;

            case 'F':
                fVal = (nbrLeft.floatValue() >= nbrRight.floatValue());
                break;

            case 'D':
                fVal = (nbrLeft.doubleValue() >= nbrRight.doubleValue());
                break;

            default:
                throw new IllegalStateException();
            }

        return (fVal ? Boolean.TRUE : Boolean.FALSE);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "NotLessExpression";
    }
