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

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* This class implements the break statement.
*
*   BreakStatement:
*       break Identifier-opt ;
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class BreakStatement extends BranchStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a break statement.
    *
    * @param stmt      the statement within which this element exists
    * @param tokFirst  the first token of the statement
    * @param tokLabel  the optional branch label
    */
    public BreakStatement(Statement stmt, Token tokFirst, Token tokLabel)
        {
        super(stmt, tokFirst, tokLabel);
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
        String    sName = getLabelName();
        Statement stmt  = getOuterStatement();

        if (sName == null)
            {
            // find the first enclosing switch, while, do, or for statement
            while (stmt != null)
                {
                if (stmt instanceof SwitchStatement ||
                    stmt instanceof WhileStatement  ||
                    stmt instanceof DoStatement     ||
                    stmt instanceof ForStatement)
                    {
                    break;
                    }

                stmt = stmt.getOuterStatement();
                }

            if (stmt == null)
                {
                logError(ERROR, BREAK_TARGET, null, errlist);
                }
            }
        else
            {
            // find the enclosing label statement to break to
            while (stmt != null)
                {
                if (stmt instanceof LabelStatement &&
                    sName.equals(((LabelStatement) stmt).getName()))
                    {
                    break;
                    }

                stmt = stmt.getOuterStatement();
                }

            if (stmt == null)
                {
                getLabelToken().logError(ERROR, LABEL_MISSING, new String[] {sName}, errlist);
                }
            }

        if (stmt != null)
            {
            // we will need the loop "exit" label, so ask for it now
            stmt.getEndLabel();

            // register all unassigned variables with the target statement
            if (!setUVars.isEmpty())
                {
                stmt.addBreakUVars(setUVars);
                }

            // register all assigned final variables with the target statement
            if (!setFVars.isEmpty())
                {
                stmt.addBreakFVars(setFVars);
                }

            setTargetStatement(stmt);
            }

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
        // compilation for continue:
        //
        //          jsr     lblFinally[0]       ; innermost finally
        //          jsr     lblFinally[1]       ; next innermost
        //          ...                         ; and so on
        //          goto    lblEnd              ; exit the statement

        // invoke the finally clauses
        compileFinallyClauses(code);

        // jump to exit label of the target statement
        Statement target = getTargetStatement();
        code.add(new Goto(target.getEndLabel()));

        // JLS 14.13: A break statement with label Identifier attempts to
        // transfer control to the enclosing labeled statement (14.6) that
        // has the same Identifier as its label; this statement, which is
        // called the break target, then immediately completes normally.
        if (fReached)
            {
            target.setCompletable(true);
            }

        // JLS 14.19: A break, continue, return, or throw statement cannot
        // complete normally.
        return false;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BreakStatement";
    }
