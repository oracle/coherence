/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;



/**
* The post-decrement (--) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class PostDecExpression extends IncExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a PostDecExpression.
    *
    * @param operator  the operator token
    * @param expr      the sub-expression
    */
    public PostDecExpression(Token operator, Expression expr)
        {
        super(operator, expr);

        // expression is first, operator is last
        setStartToken(expr.getStartToken());
        setEndToken(operator);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "PostDecExpression";
    }
