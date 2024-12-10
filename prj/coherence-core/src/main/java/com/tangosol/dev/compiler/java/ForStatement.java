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
* This class implements the for loop construct.
*
*   ForStatement:
*       for ( ForInit-opt ; Expression-opt ; ForUpdate-opt ) Statement
*   ForInit:
*       StatementExpressionList
*       LocalVariableDeclaration
*   ForUpdate:
*       StatementExpressionList
*   StatementExpressionList:
*       StatementExpression
*       StatementExpressionList , StatementExpression
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class ForStatement extends Block
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a for statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the "for" token
    */
    public ForStatement(Statement stmt, Token token)
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
        Statement  stmtInit   = getInit();
        Expression exprTest   = getTest();
        Statement  stmtUpdate = getUpdate();
        Statement  stmtBody   = getBody();

        // for statements have their own variable scope
        DualSet setUBlockVars = new DualSet(setUVars);
        DualSet setFBlockVars = new DualSet(setFVars);

        // JLS 16.2.10 for Statements
        //  - V is definitely assigned before the initialization part of the
        //    for statement iff V is definitely assigned before the for
        //    statement.
        // JLS 16.2.10.1 Initialization Part
        //  - If the initialization part of the for statement is a local
        //    variable declaration statement, the rules of 16.2.3 apply.
        //  - Otherwise, if the initialization part is empty, then V is
        //    definitely assigned after the initialization part iff V is
        //    definitely assigned before the initialization part.
        //  - Otherwise, three rules apply:
        //      - V is definitely assigned after the initialization part
        //        iff V is definitely assigned after the last expression
        //        statement in the initialization part.
        //      - V is definitely assigned before the first expression
        //        statement in the initialization part iff V is definitely
        //        assigned before the initialization part.
        //      - V is definitely assigned before an expression statement E
        //        other than the first in the initialization part iff V is
        //        definitely assigned after the expression statement
        //        immediately preceding E.
        while (stmtInit != null)
            {
            stmtInit.precompile(ctx, setUBlockVars, setFBlockVars, mapThrown, errlist);
            stmtInit = stmtInit.getNextStatement();
            }

        // determine what final variables were assigned by just the init
        // portion of the for statement
        Set setFInitAssigned = NullImplementation.getSet();
        if (!setFBlockVars.getAdded().isEmpty())
            {
            setFInitAssigned = new HashSet(setFBlockVars.getAdded());
            }

        // JLS 16.2.10 for Statements
        //  - V is definitely assigned before the condition part of the for
        //    statement iff V is definitely assigned after the initialization
        //    part of the for statement.
        if (exprTest != null)
            {
            exprTest = (Expression) exprTest.precompile(ctx, setUBlockVars, setFBlockVars, mapThrown, errlist);
            exprTest.checkBoolean(errlist);
            setTest(exprTest);
            }

        // JLS 16.2.10 for Statements
        //  - V is definitely assigned before the contained statement iff
        //    either of the following is true:
        //      - A condition expression is present and V is definitely
        //        assigned after the condition expression when true.
        //      - No condition expression is present and V is definitely
        //        assigned after the initialization part of the for
        //        statement.
        DualSet setUTrueVars = new DualSet(setUBlockVars.getTrueSet());
        DualSet setFTrueVars = new DualSet(setFBlockVars.getTrueSet());
        stmtBody.precompile(ctx, setUTrueVars, setFTrueVars, mapThrown, errlist);

        // JLS 16.2.10 for Statements
        //  - V is definitely assigned before the incrementation part of the
        //    for statement iff V is definitely assigned after the contained
        //    statement and V is definitely assigned before every continue
        //    statement that may exit the body of the for statement.
        // JLS 16.2.10.2 Incrementation Part
        //  - If the incrementation part of the for statement is empty, then
        //    V is definitely assigned after the incrementation part iff V is
        //    definitely assigned before the incrementation part.
        //  - Otherwise, three rules apply:
        //      - V is definitely assigned after the incrementation part iff
        //        V is definitely assigned after the last expression
        //        statement in the incrementation part.
        //      - V is definitely assigned before the first expression
        //        statement in the incrementation part iff V is definitely
        //        assigned before the incrementation part.
        //      - V is definitely assigned before an expression statement E
        //        other than the first in the incrementation part iff V is
        //        definitely assigned after the expression statement
        //        immediately preceding E.
        while (stmtUpdate != null)
            {
            stmtUpdate.precompile(ctx, setUTrueVars, setFTrueVars, mapThrown, errlist);
            stmtUpdate = stmtUpdate.getNextStatement();
            }

        // JLS 16.2.10 for Statements
        //  - V is definitely assigned after a for statement iff both of the
        //    following are true:
        //      - Either a condition expression is not present or V is
        //        definitely assigned after the condition expression when
        //        false.
        //      - V is definitely assigned before every break statement that
        //        may exit the for statement.
        setUTrueVars.addAll(getBreakUVars());
        setUTrueVars.resolve();
        setUBlockVars.merge();

        Set setAssigned = setUBlockVars.getRemoved();
        if (!setAssigned.isEmpty())
            {
            // it is not possible for a statement block to cause variables
            // from its outer block to become unassigned (i.e. nothing is
            // added to setUVars) but it is possible that some have been
            // assigned (i.e. need to be removed)
            setUVars.removeAll(setAssigned);
            }

        // likewise for the final variables ("definite unassignment")
        setFTrueVars.addAll(getBreakFVars());
        setFTrueVars.resolve();
        setFBlockVars.merge();

        // no new final variables have been declared for the outer block,
        // but it is possible that final variables that were previously
        // declared for the outer block have since been potentially assigned
        setAssigned = setFBlockVars.getAdded();
        if (!setAssigned.isEmpty())
            {
            // some of the assigned final variables may not be from the
            // outer variable scope
            setAssigned.retainAll(getBlock().getVariables());
            if (!setAssigned.isEmpty())
                {
                setFVars.addAll(setAssigned);
                }

            // it is an error for a final variable declared outside of the
            // for statement to be assigned within the for statement, except
            // by the init portion which is only executed once
            setAssigned.removeAll(setFInitAssigned);
            if (!setAssigned.isEmpty())
                {
                for (Iterator iter = setAssigned.iterator(); iter.hasNext(); )
                    {
                    Variable var = (Variable) iter.next();
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
        // compilation for "for (<init-opt>; <test>; <update-opt>) <stmt>":
        //
        //  start:
        //          begin
        //          [init-opt]
        //          goto test
        //  again:
        //          [stmt]
        //  lcp:                    // loop continuation point
        //          [update-opt]
        //  test:
        //          [test]
        //          ifne again
        //          end
        //  end:
        //
        // compilation for "for (<init-opt>; ; <update-opt>) <stmt>":
        // (also for <test> which evaluates to true)
        //
        //  start:
        //          begin
        //          [init-opt]
        //  again:                  // loop continuation point
        //          [stmt]
        //  lcp:                    // loop continuation point
        //          [update-opt]
        //  test:
        //          goto again
        //          end
        //  end:

        Statement  stmtInit   = getInit();
        Expression exprTest   = getTest();
        Statement  stmtUpdate = getUpdate();
        Statement  stmtBody   = getBody();

        // determine endless (e.g. "for(;true;)" or "for(;;)")
        boolean fEndless = false;
        if (exprTest == null)
            {
            fEndless = true;
            }
        else if (exprTest instanceof BooleanExpression || !ctx.isDebug() && exprTest.isConstant())
            {
            if (((Boolean) exprTest.getValue()).booleanValue())
                {
                // "for(;true;)" or equivalent
                fEndless = true;
                }
            else
                {
                // JLS 14.19:  The contained statement is reachable iff
                // the for statement is reachable and the condition
                // expression is not a constant expression whose value is
                // false.
                if (fReached)
                    {
                    stmtBody.notReached(errlist);
                    }

                // since "for (;false;)" or equivalent is optimized out
                // entirely, the for statement can complete if reached
                return fReached;
                }
            }

        code.add(new Begin());

        // figure out where the loop continuation label goes (see above)
        Label lblAgain    = new Label();
        Label lblContinue = getContinuationLabel();
        Label lblTest     = new Label();

        // compile for-init statement list (if it exists)
        while (stmtInit != null)
            {
            stmtInit.compile(ctx, code, fReached, errlist);
            stmtInit = stmtInit.getNextStatement();
            }

        // execute the test (if it exists) before the inner statement
        if (!fEndless)
            {
            code.add(new Goto(lblTest));
            }

        // repeat the for body
        code.add(lblAgain);

        // compile statement
        stmtBody.compile(ctx, code, fReached, errlist);

        // loop continuation point
        code.add(lblContinue);

        // compile update statement list (if it exists)
        while (stmtUpdate != null)
            {
            stmtUpdate.compile(ctx, code, fReached, errlist);
            stmtUpdate = stmtUpdate.getNextStatement();
            }

        // location of for-exit test
        code.add(lblTest);

        if (fEndless)
            {
            // no exit condition
            code.add(new Goto(lblAgain));
            }
        else
            {
            // test for-exit condition
            exprTest.compile(ctx, code, fReached, errlist);
            code.add(new Ifne(lblAgain));
            }

        code.add(new End());

        // JLS 14.19:  A for statement can complete normally iff at least
        // one of the following is true:
        //  - The for statement is reachable, there is a condition
        //    expression, and the condition expression is not a constant
        //    expression with value true.
        //  - There is a reachable break statement that exits the for
        //    statement.
        // (Note:  Completion via a break statement is implemented by the
        // Statement.compile() method, which wraps this method.)
        return fReached && !fEndless;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the init statement.
    *
    * @return  the init statement
    */
    public Statement getInit()
        {
        return init;
        }

    /**
    * Set the init statement.
    *
    * @param init  the init statement
    */
    protected void setInit(Statement init)
        {
        this.init = init;
        }

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
    * Get the update statement.
    *
    * @return  the update statement
    */
    public Statement getUpdate()
        {
        return update;
        }

    /**
    * Set the update statement.
    *
    * @param update  the update statement
    */
    protected void setUpdate(Statement update)
        {
        this.update = update;
        }

    /**
    * Get the body of the for statement.
    *
    * @return  the body of the for statement
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

        out(sIndent + "  Init:");
        if (init == null)
            {
            out(sIndent + "    <null>");
            }
        else
            {
            init.print(sIndent + "    ");
            }

        out(sIndent + "  Test:");
        if (test == null)
            {
            out(sIndent + "    <null>");
            }
        else
            {
            test.print(sIndent + "    ");
            }

        out(sIndent + "  Update:");
        if (update == null)
            {
            out(sIndent + "    <null>");
            }
        else
            {
            update.print(sIndent + "    ");
            }

        out(sIndent + "  Statement:");
        getInnerStatement().print(sIndent + "    ");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ForStatement";

    /**
    * The conditional expression.
    */
    private Expression test;

    /**
    * A linked list of ForInit statements.
    */
    private Statement init;

    /**
    * A linked list of ForUpdate statements.
    */
    private Statement update;
    }
