/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.IntConstant;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Base class for equals (==) and not equals (!=) equality operators.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public abstract class EqualityExpression extends BinaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a EqualityExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    protected EqualityExpression(Expression left, Token operator, Expression right)
        {
        super(left, operator, right);
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        boolean fVal = compareEquality(left.getValue(), right.getValue(), left.getType());

        return (fVal ? Boolean.TRUE : Boolean.FALSE);
        }

    /**
    * Compare the passed values for equality.
    *
    * @param oLeft   the "left" value
    * @param oRight  the "right" value
    * @param dt      the data type of the values
    *
    * @return true if the two values are equal
    */
    public static boolean compareEquality(Object oLeft, Object oRight, DataType dt)
        {
        switch (dt.getTypeString().charAt(0))
            {
            case 'Z':
                return (((Boolean) oLeft).booleanValue() == ((Boolean) oRight).booleanValue());

            case 'I':
                return (((Number) oLeft).intValue() == ((Number) oRight).intValue());

            case 'J':
                return (((Number) oLeft).longValue() == ((Number) oRight).longValue());

            case 'F':
                return (((Number) oLeft).floatValue() == ((Number) oRight).floatValue());

            case 'D':
                return (((Number) oLeft).doubleValue() == ((Number) oRight).doubleValue());

            case 'N':
            case 'L':
            case 'R':
                // constant references types are the null type and strings
                // (note:  constant strings are from a unique pool, so use
                // String comparison method to compare, instead of ==)
                return (oLeft == null ? oRight == null : oLeft.equals(oRight));

            case '[':
                {
                if (oLeft == null)
                    {
                    return oRight == null;
                    }

                Object[] aoLeft  = (Object[]) oLeft;
                Object[] aoRight = (Object[]) oRight;
                int cLeft  = aoLeft .length;
                int cRight = aoRight.length;
                if (cLeft != cRight)
                    {
                    return false;
                    }

                DataType dtElement = dt.getElementType();
                for (int i = 0; i < cLeft; ++i)
                    {
                    if (!compareEquality(aoLeft[i], aoRight[i], dtElement))
                        {
                        return false;
                        }
                    }

                return true;
                }

            default:
                throw new IllegalStateException();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "EqualityExpression";

    /**
    * String data type.
    */
    protected static final DataType STRING = DataType.STRING;

    /**
    * Boolean data type.
    */
    protected static final DataType BOOLEAN = DataType.BOOLEAN;

    /**
    * The assembly constant int value of boolean true.
    */
    protected static final IntConstant TRUE  = Constants.CONSTANT_ICONST_1;

    /**
    * The assembly constant int value of boolean false.
    */
    protected static final IntConstant FALSE = Constants.CONSTANT_ICONST_0;
    }
