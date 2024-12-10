/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.util.ErrorList;


/**
* This class implements all of the Java binary expressions.  Binary
* expressions are those that have a "left" operand, an operator, and a
* "right" operand.  To common examples are assignment (a=b) and addition
* (a+b).
*
* @version 1.00, 09/22/98
* @author  Cameron Purdy
*/
public abstract class BinaryExpression extends Expression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a BinaryExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    protected BinaryExpression(Expression left, Token operator, Expression right)
        {
        super(left.getBlock(), left.getStartToken(), right.getEndToken());

        this.left     = left;
        this.operator = operator;
        this.right    = right;
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine if the expression has a constant value.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        // most binary expressions are constant if both sub-expressions are
        // constant
        return left.isConstant() && right.isConstant();
        }


    // ----- Element methods ------------------------------------------------

    /**
    * Print the element information.
    *
    * @param sIndent
    */
    public void print(String sIndent)
        {
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        out(sIndent + toString());

        sIndent += "  ";

        out(sIndent + "Left:");
        left.print(sIndent + "  ");

        out(sIndent + "Operator:  " + operator.toString());

        out(sIndent + "Right:");
        right.print(sIndent + "  ");
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the "left" expression.
    *
    * @return the "left" expression
    */
    public Expression getLeftExpression()
        {
        return left;
        }

    /**
    * Set the "left" expression.
    *
    * @param left  the "left" expression
    */
    protected void setLeftExpression(Expression left)
        {
        this.left = left;
        }

    /**
    * Get the binary operator.
    *
    * @return the binary operator token
    */
    public Token getOperator()
        {
        return operator;
        }

    /**
    * Get the "right" expression.
    *
    * @return the "right" expression
    */
    public Expression getRightExpression()
        {
        return right;
        }

    /**
    * Set the "right" expression.
    *
    * @param right  the "right" expression
    */
    protected void setRightExpression(Expression right)
        {
        this.right = right;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BinaryExpression";

    /**
    * The first expression.
    */
    private Expression left;

    /**
    * The binary operator.
    */
    private Token operator;

    /**
    * The second expression.
    */
    private Expression right;
    }
