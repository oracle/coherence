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
* This class implements a Java statement which is composed of an
* expression.  This includes assignment, for example.
*
*   ExpressionStatement:
*       StatementExpression ;
*   StatementExpression:
*       Assignment
*       PreIncrementExpression
*       PreDecrementExpression
*       PostIncrementExpression
*       PostDecrementExpression
*       MethodInvocation
*       ClassInstanceCreationExpression
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class ExpressionStatement extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an expression statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    public ExpressionStatement(Statement stmt, Token token)
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
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Element precompile(Context ctx, DualSet setUVars, DualSet setFVars, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        Expression expr = this.expr;

        // pre-compile the expression
        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // the expression is responsible for discarding its value
        expr.setDiscarded(true);

        this.expr = expr;

        // JLS 16.2.5:  V is definitely assigned after an expression
        // statement e; iff it is definitely assigned after e.
        setUVars.merge();
        setFVars.merge();

        return this;
        }

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
    protected boolean compileImpl(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        // compile the expression
        expr.compile(ctx, code, fReached, errlist);

        // JLS 14.19: An expression statement can complete normally iff it
        // is reachable.
        return fReached;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the expression.
    *
    * @return the expression
    */
    public Expression getExpression()
        {
        return expr;
        }

    /**
    * Set the expression.
    *
    * @param expr  the expression
    */
    protected void setExpression(Expression expr)
        {
        this.expr = expr;
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

        out(sIndent + "  Expression:");
        expr.print(sIndent + "    ");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ExpressionStatement";

    /**
    * The expression.
    */
    private Expression expr;
    }
