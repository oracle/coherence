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

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;


/**
* This class implements Java unary expressions.  Unary expressions consist
* of an operator and a sub-expression.  For example, the unary minus "-458"
* or the unary post-increment "i++".
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public abstract class UnaryExpression extends Expression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a UnaryExpression.
    *
    * @param operator  the operator token
    * @param expr      the sub-expression
    */
    protected UnaryExpression(Token operator, Expression expr)
        {
        // assumption is that the operator token is the first token
        super(expr.getBlock(), operator);
        setEndToken(expr.getEndToken());

        this.operator = operator;
        this.expr     = expr;
        }


    // ----- Element methods ------------------------------------------------

    /**
    * Print the element information.
    *
    * @param sIndent
    */
    public void print(String sIndent)
        {
        out(sIndent + toString());

        out(sIndent + "  Operator:  " + operator.toString());

        out(sIndent + "  Sub-expression:");
        expr.print(sIndent + "    ");
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the unary operator.
    *
    * @return the unary operator
    */
    public Token getOperator()
        {
        return operator;
        }

    /**
    * Get the sub-expression.
    *
    * @return the sub-expression of this unary expression
    */
    public Expression getExpression()
        {
        return expr;
        }

    /**
    * Replace the sub-expression.
    *
    * @param expr  the new sub-expression
    */
    protected void setExpression(Expression expr)
        {
        this.expr = expr;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "UnaryExpression";

    /**
    * The unary operator.
    */
    private Token operator;

    /**
    * The sub-expression.
    */
    private Expression expr;
    }
