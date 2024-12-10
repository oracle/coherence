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
* Implements a literal expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public abstract class LiteralExpression extends Expression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a LiteralExpression.
    *
    * @param block    the containing block
    * @param literal  the literal token
    */
    protected LiteralExpression(Block block, Token literal)
        {
        super(block, literal);

        // literals are single-token expressions
        setEndToken(literal);
        }

    /**
    * Construct a LiteralExpression.  This is used by int and long for
    * negative values.
    *
    * @param block    the containing block
    * @param literal  the literal token
    */
    protected LiteralExpression(Block block, Token tokFirst, Token literal)
        {
        super(block, tokFirst, literal);
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine if the expression has a constant value.  Literal expressions
    * are constant.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        return true;
        }

    /**
    * Determine the constant value of the expression.  For a literal
    * expression, the constant value is the value of the literal token.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        // use the end token since the start token may get set to a minus
        return getEndToken().getValue();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the element information as a string.
    *
    * @return a human-readable description of the element
    */
    public String toString()
        {
        // use the end token since the start token may get set to a minus
        return super.toString() + " " + getEndToken().getText();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "LiteralExpression";
    }
