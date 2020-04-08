/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Begin;
import com.tangosol.dev.assembler.End;
import com.tangosol.dev.assembler.Switch;
import com.tangosol.dev.assembler.Case;
import com.tangosol.dev.assembler.Label;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;


/**
* This class implements the switch/case construct.
*
*   SwitchStatement:
*       switch ( Expression ) SwitchBlock
*   SwitchBlock:
*       { SwitchBlockStatementGroups-opt SwitchLabels-opt }
*   SwitchBlockStatementGroups:
*       SwitchBlockStatementGroup
*       SwitchBlockStatementGroups SwitchBlockStatementGroup
*   SwitchBlockStatementGroup:
*       SwitchLabels BlockStatements
*   SwitchLabels:
*       SwitchLabel
*       SwitchLabels SwitchLabel
*   SwitchLabel:
*       case ConstantExpression :
*       default :
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class SwitchStatement extends Block
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a switch statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the "switch" token
    */
    public SwitchStatement(Statement stmt, Token token)
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
        Expression exprTest = getTest();
        Statement  stmtBody = getBody();

        // pre-compile the test (the switch expression)
        exprTest = (Expression) exprTest.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        exprTest.checkAssignable(ctx, DataType.INT, errlist);
        setTest(exprTest);

        // switch blocks have their own variable scope
        DualSet setUBlockVars = new DualSet(setUVars);
        DualSet setFBlockVars = new DualSet(setFVars);

        // collect case clauses
        Set setCases = new HashSet();

        // pre-compile the set of inner statements
        Statement stmt = stmtBody;
        while (stmt != null)
            {
            stmt.precompile(ctx, setUBlockVars, setFBlockVars, mapThrown, errlist);

            if (stmt instanceof CaseClause)
                {
                CaseClause clause = (CaseClause) stmt;
                if (clause.isConstant())
                    {
                    Integer I = Integer.valueOf(clause.getValue());
                    if (setCases.contains(I))
                        {
                        // repeated case value
                        logError(ERROR, CASE_DUPLICATE, new String[] {I.toString()}, errlist);
                        }
                    else
                        {
                        setCases.add(I);
                        }
                    }

                // JLS 16.2.7:  V is definitely assigned before a statement
                // or local variable declaration statement S in the switch
                // block iff at least one of the following is true:
                //  - V is definitely assigned after the switch expression.
                //  - S is not labeled by a case or default label and V is
                //    definitely assigned after the preceding statement.
                setUBlockVars.reset();
                }
            else if (stmt instanceof DefaultClause)
                {
                if (stmtDefault == null)
                    {
                    stmtDefault = (DefaultClause) stmt;
                    }
                else
                    {
                    // only one default per switch please
                    logError(ERROR, DFT_DUPLICATE, null, errlist);
                    }

                // JLS 16.2.7:  V is definitely assigned before a statement
                // or local variable declaration statement S in the switch
                // block iff at least one of the following is true:
                //  - V is definitely assigned after the switch expression.
                //  - S is not labeled by a case or default label and V is
                //    definitely assigned after the preceding statement.
                setUBlockVars.reset();
                }

            stmt = stmt.getNextStatement();
            }

        // JLS 16.2.7:  V is definitely assigned after a switch statement
        // iff all of the following are true:
        //  - Either the switch block is empty or V is definitely assigned
        //    after the last statement of the switch block.
        //  - V is definitely assigned before every break statement that may
        //    exit the switch statement.
        //  - V is definitely assigned after the switch expression or the
        //    switch block contains a default label.
        if (stmtDefault != null)
            {
            setUBlockVars.addAll(getBreakUVars());
            Set setAssigned = setUBlockVars.getRemoved();
            if (!setAssigned.isEmpty())
                {
                // it is not possible for a statement block to cause variables
                // from its outer block to become unassigned (i.e. nothing is
                // added to setUVars) but it is possible that some have been
                // assigned (i.e. need to be removed)
                setUVars.removeAll(setAssigned);
                }
            }

        // likewise for the final variables ("definite unassignment")
        setFBlockVars.addAll(getBreakFVars());
        Set setAssigned = setFBlockVars.getAdded();
        if (!setAssigned.isEmpty())
            {
            // some of the assigned final variables may not be from the
            // outer variable scope
            setAssigned.retainAll(getBlock().getVariables());
            if (!setAssigned.isEmpty())
                {
                setFVars.addAll(setAssigned);
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
        // compilation for "switch (<expr>) {<case> <stmt> ... <default> ...}":
        //
        //  start:
        //          [expr]
        //          Switch  default     // start label for default
        //          Case    [c], case   // start label for case
        //          ...
        //          begin
        //          [case]
        //          [stmt]
        //          [default]
        //          end
        //  end:
        //
        // if no default clause is present, the default label is the end
        // label of the switch statement

        Expression exprTest = getTest();
        Statement  stmtBody = getBody();

        boolean fSwitchReached = fReached;
        boolean fCompletes     = fReached;

        // compile test expression
        exprTest.compile(ctx, code, fReached, errlist);

        // switch/default
        Label lblDefault = (stmtDefault == null ? getEndLabel() : stmtDefault.getStartLabel());
        code.add(new Switch(lblDefault));

        // switch cases (branches)
        Statement stmt = stmtBody;
        while (stmt != null)
            {
            if (stmt instanceof CaseClause)
                {
                code.add(new Case(((CaseClause) stmt).getValue(), stmt.getStartLabel()));
                }

            stmt = stmt.getNextStatement();
            }

        // switch block
        code.add(new Begin());

        // compile the set of inner statements
        stmt     = getInnerStatement();
        fReached = false;
        while (stmt != null)
            {
            // JLS 14.19:  A statement in a switch block is reachable iff
            // its switch statement is reachable and at least one of the
            // following is true:
            //  - It bears a case or default label.
            //  - There is a statement preceding it in the switch block and
            //    that preceding statement can complete normally.
            if (stmt instanceof CaseClause || stmt instanceof DefaultClause)
                {
                fReached = fSwitchReached;
                }
            else if (fReached && !fCompletes)
                {
                stmt.notReached(errlist);
                fReached = false;
                }

            // compile the statement
            fCompletes = stmt.compile(ctx, code, fReached, errlist);

            stmt = stmt.getNextStatement();
            }

        code.add(new End());

        // JLS 14.19:  A switch statement can complete normally iff at least
        // one of the following is true:
        //  - The last statement in the switch block can complete normally.
        //  - The switch block is empty or contains only switch labels.
        //  - There is at least one switch label after the last switch block
        //    statement group.
        //  - There is a reachable break statement that exits the switch
        //    statement.
        // (Note:  Completion via a break statement is implemented by the
        // Statement.compile() method, which wraps this method.)
        return fCompletes || stmtDefault == null;
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
    * Get the body of the switch statement.
    *
    * @return  the body of the switch statement
    */
    public Statement getBody()
        {
        return getInnerStatement();
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


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "SwitchStatement";

    /**
    * The conditional expression.
    */
    private Expression test;

    /**
    * The default clause (determined by precompile).
    */
    private DefaultClause stmtDefault;
    }
