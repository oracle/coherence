/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Athrow;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.DeltaSet;

import java.util.Set;
import java.util.Map;


/**
* This class implements the throw statement.
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class ThrowStatement extends ExitStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a throw statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    public ThrowStatement(Statement stmt, Token token)
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
        // precompile the thrown expression
        Expression value = getExpression();
        value = (Expression) value.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        setExpression(value);

        // check and log the specified exception
        value.checkThrownException(ctx, value.getType(), mapThrown, errlist);

        // JLS 16.2.11:  By convention, we say that V is definitely assigned
        // after any break, continue, return, or throw statement.
        setUVars.clear();
        setFVars.clear();

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
        Expression value = getExpression();

        // note:  throw does not have to directly execute the finally
        //        clauses; they are executed naturally at runtime

        value.compile(ctx, code, fReached, errlist);
        code.add(new Athrow());

        // JLS 14.19: A break, continue, return, or throw statement cannot
        // complete normally.
        return false;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ThrowStatement";
    }
