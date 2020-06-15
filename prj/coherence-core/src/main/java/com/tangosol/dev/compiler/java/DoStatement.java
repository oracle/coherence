/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Goto;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Ifne;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.NullImplementation;

import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.HashSet;


/**
* This class implements the do/while construct.
*
*   DoStatement:
*       do Statement while ( Expression ) ;
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class DoStatement extends ConditionalStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an "do/while" statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    public DoStatement(Statement stmt, Token token)
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
        // keep a list of initial potentially assigned finals
        Set setFInitAssigned = NullImplementation.getSet();
        if (!setFVars.isEmpty())
            {
            setFInitAssigned = new HashSet(setFVars);
            }

        // pre-compile the body
        // JLS 16.2.9:  V is definitely assigned before S iff V is
        // definitely assigned before the do statement.
        getInnerStatement().precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // pre-compile the test
        // JLS 16.2.9:  V is definitely assigned before e iff V is
        // definitely assigned after S and V is definitely assigned
        // before every continue statement that may exit the body of
        // the do statement.
        setUVars.addAll(getContinueUVars());
        setFVars.addAll(getContinueFVars());
        Expression exprTest = precompileTest(ctx, setUVars, setFVars, mapThrown, errlist);

        // JLS 16.2.9:  V is definitely assigned after do S while (e);
        // iff V is definitely assigned after e when false and V is
        // definitely assigned before every break statement that may
        // exit the do statement.
        setUVars.getTrueSet().clear();
        setUVars.merge();
        setUVars.addAll(getBreakUVars());

        // likewise for the final variables ("definite unassignment")
        setFVars.getTrueSet().clear();
        setFVars.merge();
        setFVars.addAll(getBreakFVars());

        // make sure labels are available
        getStartLabel();            // entry (repeat) label
        getContinuationLabel();     // continuation (test) label
        getEndLabel();              // exit label

        if (!setFVars.equals(setFInitAssigned))
            {
            // it is an error for a final variable declared outside of the
            // do statement to be assigned within the do statement, unless
            // the do statement is a "do..while (false)"
            if (!(exprTest.isConstant() && ((Boolean) exprTest.getValue()).booleanValue() == false))
                {
                for (Iterator iter = setFVars.iterator(); iter.hasNext(); )
                    {
                    Variable var = (Variable) iter.next();
                    if (!setFInitAssigned.contains(var))
                        {
                        logError(ERROR, FINAL_IN_LOOP, new String[] {var.getName()}, errlist);
                        }
                    }
                }
            }

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
        // compilation of "do <stmt> while (<expr>)":
        //
        //  start:
        //          [stmt]
        //  test:                   // i.e. loop continuation point
        //          [expr]
        //          ifne    start
        //  end:
        //
        // compilation of "do <stmt> while (true)":
        //
        //  start:
        //          [stmt]
        //  test:
        //          goto    start
        //  end:
        //
        // compilation of "do <stmt> while (false)":
        //
        //  start:
        //          [stmt]
        //  test:
        //  end:

        Label      lblStart = getStartLabel();
        Statement  stmt     = getInnerStatement();
        Label      lblTest  = getContinuationLabel();
        Expression exprTest = getTest();

        // compile the body of the do statement
        boolean fCompletes = stmt.compile(ctx, code, fReached, errlist);

        // add the "loop continuation point"
        code.add(lblTest);

        // check if the expression needs to be evaluated; optimize it out
        // if the expression is the literal true or false (or if the debug
        // flag is set and the constant boolean value can be determined)
        if (exprTest instanceof BooleanExpression || !ctx.isDebug() && exprTest.isConstant())
            {
            if (((Boolean) exprTest.getValue()).booleanValue())
                {
                // this is the "while (true)" scenario, which never completes
                // (this can only be over-ridden by an inner break statement)
                code.add(new Goto(lblStart));
                fCompletes = false;
                }
            }
        else
            {
            exprTest.compile(ctx, code, fReached, errlist);
            code.add(new Ifne(lblStart));
            }

        // JLS 14.19: A do statement can complete normally iff at least
        // one of the following is true:
        //  - The contained statement can complete normally and the
        //    condition expression is not a constant expression with
        //    value true.
        //  - There is a reachable break statement that exits the do
        //    statement.
        // The contained statement is reachable iff the do statement is
        // reachable.
        return fCompletes;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "DoStatement";
    }
