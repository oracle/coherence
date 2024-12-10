/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;



/**
* The pre-increment (++) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class PreIncExpression extends IncExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a PreIncExpression.
    *
    * @param operator  the operator token
    * @param expr      the sub-expression
    */
    public PreIncExpression(Token operator, Expression expr)
        {
        super(operator, expr);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "PreIncExpression";
    }
