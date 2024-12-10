/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Jsr;
import com.tangosol.dev.assembler.Label;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* This class represents the various branching statements:  Break and
* Continue.  Several other statements, such as SwitchStatement, could be
* considered branching statements, but do not derive from this class
* because of other, more important attributes.
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public abstract class BranchStatement extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a branch statement.
    *
    * @param stmt      the statement within which this element exists
    * @param tokFirst  the first token of the statement
    * @param tokLabel  the optional branch label
    */
    protected BranchStatement(Statement stmt, Token tokFirst, Token tokLabel)
        {
        super(stmt, tokFirst);

        this.tokLabel = tokLabel;
        }


    // ----- code generation ------------------------------------------------

    /**
    * Compile "finally" clause invocations.
    *
    * @param code  the assembler code attribute to compile to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void compileFinallyClauses(CodeAttribute code)
            throws CompilerException
        {
        // check for finally clauses that must be executed
        Statement target = getTargetStatement();
        Statement stmt   = getOuterStatement();
        Statement prev   = null;
        while (stmt != target)
            {
            // check if the statement is a guarded statement
            // (it is possible that the branch is in the finally, in which
            // case the guarded statement is already unwound)
            if (stmt instanceof GuardedStatement && !(prev instanceof FinallyClause))
                {
                Label lblUnwind = ((GuardedStatement) stmt).getUnwindLabel();
                if (lblUnwind != null)
                    {
                    code.add(new Jsr(lblUnwind));
                    }
                }

            prev = stmt;
            stmt = stmt.getOuterStatement();
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the branch label token.
    *
    * @return  the label token
    */
    public Token getLabelToken()
        {
        return tokLabel;
        }

    /**
    * Get the branch label name.
    *
    * @return  the label name
    */
    public String getLabelName()
        {
        return tokLabel == null ? null : tokLabel.getText();
        }

    /**
    * Get the branched-to statement.
    *
    * @return  the Statement to branch to
    */
    public Statement getTargetStatement()
        {
        return stmtTarget;
        }

    /**
    * Set the branched-to statement.
    *
    * @param stmt  the Statement to branch to
    */
    protected void setTargetStatement(Statement stmt)
        {
        this.stmtTarget = stmt;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the element information as a string.
    *
    * @return a human-readable description of the element
    */
    public String toString()
        {
        String s = super.toString();
        if (getLabelName() != null)
            {
            s += " label=" + getLabelName();
            }
        return s;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BranchStatement";

    /**
    * The target label.
    */
    private Token tokLabel;

    /**
    * The target statement
    */
    private Statement stmtTarget;
    }
