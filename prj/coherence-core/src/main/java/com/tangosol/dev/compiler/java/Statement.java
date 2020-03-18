/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Label;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.util.ErrorList;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;


/**
* This class implements a Java statement language element.
*
* The differences between the default element and a statement are:
*
*   1)  An element takes two sets of unassigned variables for its precompile;
*       statements only use the first ("if true") set.  (The other set is
*       used only by expressions.)  Due to this, statements have a
*   2)  An element can "swap itself out" during its precompile; statements
*       by convention do not.
*   3)  Statements do not implement the compile method; it is implemented
*       by the Statement class itself.  Instead, statements implement the
*       compileImpl method, which is wrapped by the compile method and
*       takes the same parameters (and has the same return value).
*
* @version 1.00, 09/15/98
* @author  Cameron Purdy
*/
public abstract class Statement
        extends Element
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token making up the language element
    */
    protected Statement(Statement outer, Token token)
        {
        super((outer == null || outer instanceof Block ? (Block) outer : outer.getBlock()), token);
        this.outer = outer;
        }


    // ----- code generation ------------------------------------------------

    /**
    * For all statements, the actual compilation is performed by the
    * compileImpl method.  The compile method (final on Statement) is
    * responsible only for the start and end labels and for handling
    * the "forced completion" flag.
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
    protected final boolean compile(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        if (labelStart != null)
            {
            code.add(labelStart);
            }

        if (ctx.isDebug())
            {
            code.setLine(getStartToken().getLine() + 1);
            }

        // compile the actual operation of this statement
        boolean fCompletes = compileImpl(ctx, code, fReached, errlist);

        if (labelEnd != null)
            {
            code.add(labelEnd);
            }

        // this statement may believe it completes, or some statement (e.g.
        // a break) within this statement may make this statement completable
        return fCompletes || isCompletable();
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
    protected abstract boolean compileImpl(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException;


    // ----- accessors ------------------------------------------------------

    /**
    * Get the statement that contains this statement.  All statements, except
    * for the main block, are contained by other statements.
    *
    * @return the statement that contains this statement
    */
    public Statement getOuterStatement()
        {
        return outer;
        }

    /**
    * Set the statement that contains this statement.
    *
    * @param outer  the statement that contains this statement
    */
    public void setOuterStatement(Statement outer)
        {
        this.outer = outer;
        }

    /**
    * Get the first inner statement, if any.
    *
    * @return the first inner statement
    */
    public Statement getInnerStatement()
        {
        return inner;
        }

    /**
    * Set the inner statement.
    *
    * @first the inner statement
    */
    protected void setInnerStatement(Statement inner)
        {
        this.inner = inner;
        }

    /**
    * Get the next statement, if any.
    *
    * @return the next statement (after this one)
    */
    public Statement getNextStatement()
        {
        return next;
        }

    /**
    * Set the next statement.
    *
    * @first the next statement
    */
    protected void setNextStatement(Statement next)
        {
        this.next = next;
        }

    /**
    * Get the assembly label for this statement.
    *
    * @return the assembly label for this statement
    */
    public Label getStartLabel()
        {
        if (labelStart == null)
            {
            labelStart = new Label();
            }
        return labelStart;
        }

    /**
    * Get the assembly label for the loop continuation point of this
    * statement.
    *
    * This is only applicable for the following:
    *
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @return the assembly label trailing this statement
    */
    public Label getContinuationLabel()
        {
        if (labelCont == null)
            {
            labelCont = new Label();
            }
        return labelCont;
        }

    /**
    * Get the assembly label for the next statement.
    *
    * @return the assembly label trailing this statement
    */
    public Label getEndLabel()
        {
        if (labelEnd == null)
            {
            labelEnd = new Label();
            }
        return labelEnd;
        }

    /**
    * Determine if this statement completes normally even if it appears
    * to complete abruptly.  (Value is only meaningful after the inner
    * statements have been pre-compiled.)
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   SwitchStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @return true if an inner statement causes this statement to complete
    *         normally
    */
    protected boolean isCompletable()
        {
        return fCompletable;
        }

    /**
    * Force this statement to complete normally.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   SwitchStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @param fCompletable  true if this statement completes normally as a
    *        result of an inner
    */
    protected void setCompletable(boolean fCompletable)
        {
        this.fCompletable = fCompletable;
        }

    /**
    * Add the passed unassigned variables from a BreakStatement to the
    * set of unassigned variables for this statement.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   SwitchStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @param setVars  the set of unassigned vars at the BreakStatement
    */
    protected void addBreakUVars(Set setVars)
        {
        getBreakUVars().addAll(setVars);
        }

    /**
    * Get the (union) set of all unassigned variables from each break.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   SwitchStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @return  a set of variables which is the union of all sets of
    *          unassigned variables for each break statement which
    *          completes this statement
    */
    protected Set getBreakUVars()
        {
        Set setVars = this.setBreakUVars;

        if (setVars == null)
            {
            this.setBreakUVars = setVars = new HashSet();
            }

        return setVars;
        }

    /**
    * Add the passed unassigned variables from a ContinueStatement to the
    * set of unassigned variables for this statement.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @param setVars  the set of unassigned vars at the ContinueStatement
    */
    protected void addContinueUVars(Set setVars)
        {
        getContinueUVars().addAll(setVars);
        }

    /**
    * Get the (union) set of all unassigned variables from each continue.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @return  a set of variables which is the union of all sets of
    *          unassigned variables for each Continue statement which
    *          completes this statement
    */
    protected Set getContinueUVars()
        {
        Set setVars = this.setContinueUVars;

        if (setVars == null)
            {
            this.setContinueUVars = setVars = new HashSet();
            }

        return setVars;
        }

    /**
    * Add the passed assigned final variables from a BreakStatement to the
    * set of assigned variables for this statement.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   SwitchStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @param setVars  the set of assigned final vars at the BreakStatement
    */
    protected void addBreakFVars(Set setVars)
        {
        getBreakFVars().addAll(setVars);
        }

    /**
    * Get the (union) set of all assigned final variables from each break.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   SwitchStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @return  a set of variables which is the union of all sets of
    *          assigned final variables for each break statement which
    *          completes this statement
    */
    protected Set getBreakFVars()
        {
        Set setVars = this.setBreakFVars;

        if (setVars == null)
            {
            this.setBreakFVars = setVars = new HashSet();
            }

        return setVars;
        }

    /**
    * Add the passed assigned final variables from a ContinueStatement to
    * the set of assigned final variables for this statement.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @param setVars  the set of assigned final vars at the ContinueStatement
    */
    protected void addContinueFVars(Set setVars)
        {
        getContinueFVars().addAll(setVars);
        }

    /**
    * Get the (union) set of all assigned final variables from each continue.
    *
    * This is only applicable for the following:
    *
    *   LabelStatement
    *   WhileStatement
    *   DoStatement
    *   ForStatement
    *
    * @return  a set of variables which is the union of all sets of
    *          assigned final variables for each Continue statement which
    *          completes this statement
    */
    protected Set getContinueFVars()
        {
        Set setVars = this.setContinueFVars;

        if (setVars == null)
            {
            this.setContinueFVars = setVars = new HashSet();
            }

        return setVars;
        }


    // ----- error logging --------------------------------------------------

    /**
    * Error:  Statement is not reachable.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notReached(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, STMT_NOT_REACHED, null, errlist);
        }


    // ----- Element methods ------------------------------------------------

    /**
    * Print the statement information.
    *
    * @param sIndent
    */
    public void print(String sIndent)
        {
        out(sIndent + toString());

        if (inner != null)
            {
            out(sIndent + "  Inner Statements:");
            inner.printList(sIndent + "    ");
            }
        }

    /**
    * Print this statement and the list of statements following this statement.
    *
    * @param sIndent
    */
    public void printList(String sIndent)
        {
        for (Statement stmt = this; stmt != null; stmt = stmt.next)
            {
            stmt.print(sIndent);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "Statement";

    /**
    * The statement within which this statement exists
    */
    private Statement outer;

    /**
    * The statement within which this statement exists
    */
    private Statement inner;

    /**
    * Supports a linked list of statements within a block.
    */
    private Statement next;

    /**
    * The location of this statement in the assembly.
    */
    private Label labelStart;

    /**
    * The location of the continuation point of this statement in the
    * assembly.
    */
    private Label labelCont;

    /**
    * The location of the next statement in the assembly.
    */
    private Label labelEnd;

    /**
    * LabelStatement, SwitchStatement, WhileStatement, DoStatement, and
    * ForStatement may complete normally due to a reachable break within
    * them.
    */
    private boolean fCompletable;

    /**
    * LabelStatement, SwitchStatement, WhileStatement, DoStatement, and
    * ForStatement collect all unassigned variables from each continue.
    */
    private Set setContinueUVars;

    /**
    * LabelStatement, SwitchStatement, WhileStatement, DoStatement, and
    * ForStatement collect all unassigned variables from each break.
    */
    private Set setBreakUVars;

    /**
    * LabelStatement, SwitchStatement, WhileStatement, DoStatement, and
    * ForStatement collect all assigned final variables from each continue.
    */
    private Set setContinueFVars;

    /**
    * LabelStatement, SwitchStatement, WhileStatement, DoStatement, and
    * ForStatement collect all assigned final variables from each break.
    */
    private Set setBreakFVars;
    }
