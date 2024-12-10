/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Goto;
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
*   WhileStatement:
*       while ( Expression ) Statement
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class WhileStatement extends ConditionalStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an "do/while" statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    public WhileStatement(Statement stmt, Token token)
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

        // pre-compile the test
        // JLS 16.2.8:  V is definitely assigned before e iff V is
        // definitely assigned before the while statement.
        precompileTest(ctx, setUVars, setFVars, mapThrown, errlist);

        // pre-compile the body
        // JLS 16.2.8:  V is definitely assigned before S iff V is
        // definitely assigned after e when true.
        DualSet setUTrueVars = new DualSet(setUVars.getTrueSet());
        DualSet setFTrueVars = new DualSet(setFVars.getTrueSet());
        getInnerStatement().precompile(ctx, setUTrueVars, setFTrueVars, mapThrown, errlist);

        // JLS 16.2.8:  V is definitely assigned after while (e) S iff V
        // is definitely assigned after e when false and V is definitely
        // assigned before every break statement that may exit the while
        // statement.
        setUTrueVars.addAll(getBreakUVars());
        setUTrueVars.resolve();
        setUVars.merge();

        // likewise for the final variables ("definite unassignment")
        setFTrueVars.addAll(getBreakFVars());
        Set setFAssigned = setFTrueVars.getAdded();
        setFTrueVars.resolve();
        setFVars.merge();

        if (!setFVars.equals(setFInitAssigned))
            {
            // it is an error for a final variable declared outside of the
            // while statement to be assigned within the while statement
            for (Iterator iter = setFVars.iterator(); iter.hasNext(); )
                {
                Variable var = (Variable) iter.next();
                if (!setFInitAssigned.contains(var))
                    {
                    logError(ERROR, FINAL_IN_LOOP, new String[] {var.getName()}, errlist);
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
        // compilation of "while (<expr>) <stmt>":
        //
        //  start:
        //          goto test
        //  again:
        //          [stmt]
        //  test:                   // i.e. loop continuation point
        //          [expr]
        //          ifne    again
        //  end:
        //
        // compilation of "while (true) <stmt>":
        //
        //  start:
        //  again:
        //          [stmt]
        //  test:
        //          goto    again
        //  end:

        boolean    fEndless = false;
        Label      lblAgain = new Label();
        Statement  stmt     = getInnerStatement();
        Label      lblTest  = getContinuationLabel();
        Expression exprTest = getTest();

        if (exprTest instanceof BooleanExpression || !ctx.isDebug() && exprTest.isConstant())
            {
            if (((Boolean) exprTest.getValue()).booleanValue())
                {
                // "while (true)" or equivalent
                fEndless = true;
                }
            else
                {
                if (fReached)
                    {
                    // JLS 14.19:  The contained statement is reachable iff
                    // the while statement is reachable and the condition
                    // expression is not a constant expression whose value is
                    // false.
                    stmt.notReached(errlist);
                    }

                // since "while (false)" or equivalent is optimized out entirely,
                // the while statement can complete if reached
                return fReached;
                }
            }

        // do the test first; for the compilation rationale, see the Java
        // VM Specification 7.5 "Compiling for the Virtual Machine"
        if (!fEndless)
            {
            code.add(new Goto(lblTest));
            }

        // compile the body
        code.add(lblAgain);
        stmt.compile(ctx, code, fReached, errlist);

        // compile the test (or optimize it out for "while (true)")
        code.add(lblTest);
        if (fEndless)
            {
            code.add(new Goto(lblAgain));
            }
        else
            {
            if (ctx.isDebug())
                {
                // Must the line number correctly for the test expression
                code.setLine(exprTest.getStartToken().getLine() + 1);
                }
            exprTest.compile(ctx, code, fReached, errlist);
            code.add(new Ifne(lblAgain));
            }

        // JLS 14.19: A while statement can complete normally iff at
        // least one of the following is true:
        //  - The while statement is reachable and the condition
        //    expression is not a constant expression with value true.
        //  - There is a reachable break statement that exits the
        //    while statement.
        // (Note:  Completion via a break statement is implemented by the
        // Statement.compile() method, which wraps this method.)
        return fReached && !fEndless;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "WhileStatement";
    }
