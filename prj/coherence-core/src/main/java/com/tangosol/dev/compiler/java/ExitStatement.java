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
* This class represents the statements which can exit a method:  throw and
* return.
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public abstract class ExitStatement extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an exit statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    protected ExitStatement(Statement stmt, Token token)
        {
        super(stmt, token);
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

        if (expr != null)
            {
            out(sIndent + "  Value:");
            expr.print(sIndent + "    ");
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the exit value.
    *
    * @return  the value expression of the exit statement
    */
    public Expression getExpression()
        {
        return expr;
        }

    /**
    * Set the exit value.
    *
    * @param expr  the value expression of the exit statement
    */
    protected void setExpression(Expression expr)
        {
        this.expr = expr;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ExitStatement";

    /**
    * The return or exception value.
    */
    private Expression expr;
    }
