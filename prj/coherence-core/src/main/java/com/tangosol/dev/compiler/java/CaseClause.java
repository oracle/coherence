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
* This class implements the "case <constant-value> :" clause of a switch
* statement.
*
*   SwitchLabel:
*       case ConstantExpression :
*       default :
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class CaseClause extends TargetStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a case clause.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the "case" token
    */
    public CaseClause(Statement stmt, Token token)
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
        Expression expr = getTest();

        // pre-compile the test
        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // the test must be constant
        if (expr.checkConstant(errlist))
            {
            // the test must match (be assignable to) the type of the switch
            if (expr.checkIntegral(errlist))
                {
                DataType dt = ((SwitchStatement) getOuterStatement()).getTest().getType();
                if (expr.checkAssignable(ctx, dt, errlist))
                    {
                    expr = expr.convertAssignable(ctx, DataType.INT);
                    }
                }
            }

        // store the test
        setTest(expr);

        // this will require a label
        getStartLabel();

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
        return true;
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

    /**
    * Determine if the test expression has a constant value.
    *
    * @return true if the test expression results in a constant value
    */
    public boolean isConstant()
        {
        return test.isConstant();
        }

    /**
    * Get the case value.
    *
    * @return  the int case value
    */
    protected int getValue()
        {
        return ((Number) getTest().getValue()).intValue();
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

        out(sIndent + "  Value:");
        test.print(sIndent + "    ");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "CaseClause";

    /**
    * The conditional expression.
    */
    private Expression test;
    }
