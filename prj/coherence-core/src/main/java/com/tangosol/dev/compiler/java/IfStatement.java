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
import com.tangosol.dev.assembler.Ifeq;
import com.tangosol.dev.assembler.Ifne;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.DeltaSet;

import java.util.Set;
import java.util.Map;


/**
* This class implements the if/then and if/then/else constructs.
*
*   IfThenStatement:
*       if ( Expression ) Statement
*   IfThenElseStatement:
*       if ( Expression ) StatementNoShortIf else Statement
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class IfStatement extends ConditionalStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an "if/then" or "if/then/else" statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    public IfStatement(Statement stmt, Token token)
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
        // pre-compile the test
        Expression exprTest = precompileTest(ctx, setUVars, setFVars, mapThrown, errlist);

        // pre-compile the "then"
        Statement stmtThen = getThenStatement();
        DualSet   setUThen = new DualSet(setUVars.getTrueSet());
        DualSet   setFThen = new DualSet(setFVars.getTrueSet());
        stmtThen.precompile(ctx, setUThen, setFThen, mapThrown, errlist);
        setUThen.resolve();
        setFThen.resolve();

        // pre-compile the "else"
        Statement stmtElse = getElseStatement();
        if (stmtElse != null)
            {
            DualSet setUElse = new DualSet(setUVars.getFalseSet());
            DualSet setFElse = new DualSet(setFVars.getFalseSet());
            stmtElse.precompile(ctx, setUElse, setFElse, mapThrown, errlist);
            setUElse.resolve();
            setFElse.resolve();
            }

        // JLS 16.2.6:  V is definitely assigned after if (e) S iff V is
        // definitely assigned after S and V is definitely assigned after e
        // when false.  V is definitely assigned before e iff V is definitely
        // assigned before if (e) S else T. V is definitely assigned before S
        // iff V is definitely assigned after e when true. V is definitely
        // assigned before T iff V is definitely assigned after e when false.
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
        Expression exprTest = getTest();
        Statement  stmtThen = getThenStatement();
        Statement  stmtElse = getElseStatement();

        if (exprTest instanceof BooleanExpression || !ctx.isDebug() && exprTest.isConstant())
            {
            // JLS 14.19: An optimizing compiler may realize that the
            // [inner statement] will never be executed and may choose to
            // omit the code for that statement from the generated class
            // file, but [that statement] is not regarded as unreachable
            // in the technical sense specified here.
            if (!((Boolean) exprTest.getValue()).booleanValue())
                {
                stmtThen = stmtElse;
                }
            if (stmtThen != null)
                {
                stmtThen.compile(ctx, code, fReached, errlist);
                }
            // this if statement being reachable infers the compiled block/statement
            // (then or else) is reachable
            return fReached;
            }

        // compilation of "if (<expr>) <stmt>":
        //
        //          [expr]
        //          ifeq    else    // or "ifne" if "then" replaced with "else"
        //          [stmt]
        //  else:
        //
        // compilation of "if (<expr>) <stmt> else <stmt>":
        //
        //          [expr]
        //          ifeq    else
        //          [stmt]          // then
        //          goto    exit
        //  else:
        //          [stmt]
        //  exit:

        Label lblElse = new Label();
        Label lblExit = getEndLabel();

        // conditional test
        exprTest.compile(ctx, code, fReached, errlist);

        code.add(new Ifeq(lblElse));

        // "then" portion
        boolean fCompletes = stmtThen.compile(ctx, code, fReached, errlist);

        // "else" portion
        if (stmtElse == null)
            {
            code.add(lblElse);

            // JLS 14.19: An if-then statement can complete normally iff it
            // is reachable.  The then statement is reachable iff the if-then
            // statement is reachable.
            fCompletes = fReached;
            }
        else
            {
            code.add(new Goto(lblExit));
            code.add(lblElse);

            // JLS 14.19: An if-then-else statement can complete normally
            // iff the then statement can complete normally or the else
            // statement can complete normally.  The then-statement is
            // reachable iff the if-then-else statement is reachable. The
            // else-statement is reachable iff the if-then-else statement
            // is reachable.
            fCompletes |= stmtElse.compile(ctx, code, fReached, errlist);
            }

        return fCompletes;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the "then" statement.
    *
    * @return  the statement to execute if the test is true
    */
    public Statement getThenStatement()
        {
        return getInnerStatement();
        }

    /**
    * Set the "then" statement.
    *
    * @param stmt  the statement to execute if the test is true
    */
    protected void setThenStatement(Statement stmt)
        {
        setInnerStatement(stmt);

        // the "then" has the last token if there is no else
        if (stmtElse == null)
            {
            setEndToken(stmt.getEndToken());
            }
        }

    /**
    * Get the "else" statement.
    *
    * @return  the statement to execute if the test is false, or null if no
    *          else statement is present
    */
    public Statement getElseStatement()
        {
        return stmtElse;
        }

    /**
    * Set the "else" statement.
    *
    * @param stmt  the statement to execute if the test is false
    */
    protected void setElseStatement(Statement stmt)
        {
        this.stmtElse = stmt;

        // the "else" has the last token
        setEndToken(stmt.getEndToken());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "IfStatement";

    /**
    * The else clause.
    */
    private Statement stmtElse;
    }
