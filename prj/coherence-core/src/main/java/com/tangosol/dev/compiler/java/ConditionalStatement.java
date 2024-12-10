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

import java.util.Set;
import java.util.Map;


/**
* This class represents the various conditional statements except for the
* ForStatement; (the ForStatement derives from Block since it can declare
* variables).
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public abstract class ConditionalStatement extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a conditional statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    protected ConditionalStatement(Statement stmt, Token token)
        {
        super(stmt, token);
        }


    // ----- code generation ------------------------------------------------

    /**
    * Perform semantic checks, parse tree re-organization, name binding,
    * and optimizations.
    *
    * @param ctx        the compiler context
    * @param setUVars   the set of potentially unassigned variables
    * @param setFVars   the set of potentially assigned final variables
    * @param mapThrown  the set of potentially thrown checked exceptions
    * @param errlist    the error list
    *
    * @return  the resulting test expression
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Expression precompileTest(Context ctx, DualSet setUVars, DualSet setFVars, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        Expression expr = getTest();

        // pre-compile the test
        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // the test must be boolean
        expr.checkBoolean(errlist);

        // store the test
        setTest(expr);

        return expr;
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

        out(sIndent + "  Test:");
        test.print(sIndent + "    ");

        if (getInnerStatement() != null)
            {
            out(sIndent + "  Inner Statements:");
            getInnerStatement().printList(sIndent + "    ");
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the test expression.
    *
    * @return  the test expression
    */
    public Expression getTest()
        {
        return test;
        }

    /**
    * Set the test expression.
    *
    * @param test  the test expression
    */
    protected void setTest(Expression test)
        {
        this.test = test;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ConditionalStatement";

    /**
    * The conditional expression.
    */
    private Expression test;
    }
