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
import com.tangosol.dev.assembler.Jsr;
import com.tangosol.dev.assembler.Goto;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Avar;
import com.tangosol.dev.assembler.Astore;
import com.tangosol.dev.assembler.Aload;
import com.tangosol.dev.assembler.Athrow;
import com.tangosol.dev.assembler.Try;
import com.tangosol.dev.assembler.Catch;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.DeltaSet;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;


/**
* This class implements the try..catch..finally constructs in a Java script.
*
* @version 1.00, 09/16/98
* @author  Cameron Purdy
*/
public class TryStatement extends GuardedStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Java try statement.
    *
    * @param outer  the enclosing Java statement
    * @param token  the try keyword
    */
    public TryStatement(Statement outer, Token token)
        {
        super(outer, token);
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
        Statement     stmtBlock     = getInnerStatement();
        CatchClause   stmtCatch     = this.stmtCatch;
        FinallyClause stmtFinally   = this.stmtFinally;
        Map           mapTryThrows  = mapThrown;

        if (stmtCatch != null)
            {
            // if there is both a catch and a finally clause, rearrange the
            // "try .. catch .. finally" as "try {try .. catch} finally"
            // (this appears to be what JAVAC does in the JDK 1.2)
            // create a new try statement to nest within this try statement
            if (stmtFinally != null)
                {
                TryStatement stmtNested = new TryStatement(this, getStartToken());

                // get the last token from the last catch clause
                Statement cur = stmtCatch, prev = null;
                while (cur != null)
                    {
                    prev = cur;
                    cur  = cur.getNextStatement();
                    }
                stmtNested.setEndToken(prev.getEndToken());

                // donate this try statement's block and catch clauses to the
                // nested try statement
                stmtNested.setInnerStatement(stmtBlock);
                stmtNested.setCatchClause   (stmtCatch);

                // fix up the "back pointers"
                stmtBlock.setOuterStatement(stmtNested);
                stmtCatch.setOuterStatement(stmtNested);

                // release this try statement's references to the donated
                // statements
                this.setInnerStatement(stmtBlock = stmtNested);
                this.setCatchClause   (stmtCatch = null      );
                }
            // if there are catch clauses, create a temporary map of thrown
            // exceptions to determine what is thrown within the try itself,
            // then use that map to validate the exception types caught by
            // the catch clauses
            else
                {
                mapTryThrows = new HashMap();
                }
            }

        // pre-compile guarded section
        DualSet setUBlockVars = new DualSet(setUVars);
        stmtBlock.precompile(ctx, setUBlockVars, setFVars, mapTryThrows, errlist);
        Set setUAssigned = setUBlockVars.getRemoved();

        DualSet setFBlockVars = new DualSet(setFVars);
        Set     setFAssigned  = new HashSet();

        // pre-compile catch clauses
        while (stmtCatch != null)
            {
            setUBlockVars.reset();
            stmtCatch.precompile(ctx, setUBlockVars, setFBlockVars, mapThrown, errlist);
            if (!setUAssigned.isEmpty())
                {
                setUAssigned.retainAll(setUBlockVars.getRemoved());
                }
            if (!setFBlockVars.getAdded().isEmpty())
                {
                setFAssigned.addAll(setFBlockVars.getAdded());
                }

            // JLS 14.19: Unreachable Statements
            // A catch block C is reachable iff both of the following are true:
            //  - Some expression or throw statement in the try block is
            //    reachable and can throw an exception whose type is assignable
            //    to the parameter of the catch clause C. (An expression is
            //    considered reachable iff the innermost statement containing
            //    it is reachable.)
            //  - There is no earlier catch block A in the try statement such
            //    that the type of Cs parameter is the same as or a subclass
            //    of the type of As parameter.
            stmtCatch.getDeclaration().getTypeExpression().checkCaughtException(
                    ctx, stmtCatch.getExceptionType(), mapTryThrows, errlist);

            stmtCatch = (CatchClause) stmtCatch.getNextStatement();
            }

        // pre-compile finally clause
        if (stmtFinally != null)
            {
            setUBlockVars.reset();
            stmtFinally.precompile(ctx, setUBlockVars, setFVars, mapThrown, errlist);

            // JLS 16.2.15:  V is definitely assigned after a try statement
            // iff one of the following is true:
            //  - ...
            //  - The try statement has a finally block and V is definitely
            //    assigned after the finally block.
            Set setAssignedFinally = setUBlockVars.getRemoved();
            if (!setAssignedFinally.isEmpty())
                {
                setUVars.removeAll(setAssignedFinally);
                }
            }

        // JLS 16.2.15:  V is definitely assigned after a try statement
        // iff one of the following is true:
        //  - V is definitely assigned after the try block and V is
        //    definitely assigned after every catch block in the try
        //    statement.
        //  - ...
        if (!setUAssigned.isEmpty())
            {
            setUVars.removeAll(setUAssigned);
            }

        // likewise for potentially assigned (not definitely unassigned)
        // final variables
        if (!setFAssigned.isEmpty())
            {
            setFAssigned.retainAll(getBlock().getVariables());
            if (!setFAssigned.isEmpty())
                {
                setFVars.addAll(setFAssigned);
                }
            }

        // merge in any uncaught exceptions
        if (mapTryThrows != mapThrown)
            {
            Iterator iter = mapTryThrows.keySet().iterator();
            while (iter.hasNext())
                {
                DataType dt = (DataType) iter.next();
                if (mapThrown.containsKey(dt))
                    {
                    // key is data type, value is a set of expressions that
                    // throw that particular data type
                    ((Set) mapThrown.get(dt)).addAll((Set) mapTryThrows.get(dt));
                    }
                else
                    {
                    // copy the entry
                    mapThrown.put(dt, mapTryThrows.get(dt));
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
        boolean       fCompletes  = fReached;
        Statement     stmtGuarded = getInnerStatement();
        CatchClause   stmtCatch   = this.stmtCatch;
        FinallyClause stmtFinally = this.stmtFinally;

        // try..catch:
        //
        //          try
        //          [block]
        //          catch       t[catch 1], lbl[catch 1]
        //          catch       t[catch 2], lbl[catch 2]
        //          ...
        //          goto exit
        //          [catch 1]
        //          goto exit
        //          [catch 2]
        //          goto exit
        //          ...
        //  exit:
        //
        // try..finally:
        //
        //          try
        //          [block]
        //          catch       null, handler
        //          jsr         finally
        //          goto        exit
        //  handler:
        //          begin
        //          avar        #temp
        //          astore      #temp
        //          jsr         finally
        //          aload       #temp
        //          athrow
        //          end
        //          [finally]
        //  exit:
        //
        // try..catch..finally:
        // it is not possible to have a try..catch..finally because the
        // implementation of precompile() re-arranges it to be a try..catch
        // nested within a try..finally in order to match JAVAC's output.

        // JLS 14.19: Unreachable Statements
        //
        // A try statement can complete normally iff both of the following
        // are true:
        //  - The try block can complete normally or any catch block can
        //    complete normally.
        //  - If the try statement has a finally block, then the finally
        //    block can complete normally.
        //
        // The try block is reachable iff the try statement is reachable.
        //
        // A catch block C is reachable iff both of the following are true:
        //  - Some expression or throw statement in the try block is
        //    reachable and can throw an exception whose type is assignable
        //    to the parameter of the catch clause C. (An expression is
        //    considered reachable iff the innermost statement containing
        //    it is reachable.)
        //  - There is no earlier catch block A in the try statement such
        //    that the type of Cs parameter is the same as or a subclass
        //    of the type of As parameter.
        //
        // If a finally block is present, it is reachable iff the try
        // statement is reach-able.

        if (stmtCatch != null)
            {
            // Since this statement is a try..catch (no finally clause), the
            // completion rule can be re-written as:
            //
            // The try block is reachable iff the try statement is reachable.
            //
            // A try..finally statement can complete normally iff both of the
            // following are true:
            //  - The try block can complete normally or any catch block can
            //    complete normally.
            //
            // Note that the catch blocks are considered reachable iff the
            // try is reachable, as verified in the precompile() step.

            Try   opTry     = new Try();        // the start of the try block

            // guarded section
            code.add(opTry);
            fCompletes &= stmtGuarded.compile(ctx, code, fReached, errlist);
            for (CatchClause stmt = stmtCatch; stmt != null; stmt = (CatchClause) stmt.getNextStatement())
                {
                code.add(new Catch(opTry, stmt.getExceptionClass(), stmt.getStartLabel()));
                }

            // normal completion of the guarded section
            Label lblExit = getEndLabel();
            code.add(new Goto(lblExit));

            // catch bodies
            for (CatchClause stmt = stmtCatch; stmt != null; stmt = (CatchClause) stmt.getNextStatement())
                {
                // assume each catch is reachable; the check is actually
                // performed during precompile() since it is based on the
                // types of the exceptions thrown in the try block
                fCompletes |= stmt.compile(ctx, code, fReached, errlist);
                code.add(new Goto(lblExit));
                }
            }
        else
            {
            // Since this statement is a try..finally (no catch clauses), the
            // completion rule can be re-written as:
            //
            // The try block is reachable iff the try statement is reachable.
            //
            // A try..finally statement can complete normally iff both of the
            // following are true:
            //  - The try block can complete normally.
            //  - The finally block can complete normally.

            Try   opTry     = new Try();        // the start of the try block
            Label lblGuard  = new Label();      // the exception handler
            Avar  varExcept = new Avar();       // temporary to hold the exception
            Label lblUnwind = getUnwindLabel(); // the finally body

            // guarded section
            code.add(opTry);
            fCompletes &= stmtGuarded.compile(ctx, code, fReached, errlist);
            code.add(new Catch(opTry, null, lblGuard));

            // finally invocation (normal completion of the guarded section)
            code.add(new Jsr(lblUnwind));
            code.add(new Goto(getEndLabel()));

            // exception handler
            code.add(lblGuard);
            code.add(new Begin());
            code.add(varExcept);
            code.add(new Astore(varExcept));
            code.add(new Jsr(lblUnwind));
            code.add(new Aload(varExcept));
            code.add(new Athrow());
            code.add(new End());

            // finally body
            fCompletes &= stmtFinally.compile(ctx, code, fReached, errlist);
            }

        return fCompletes;
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

        out(sIndent + "  Guarded:");
        getInnerStatement().print(sIndent + "    ");

        if (stmtCatch != null)
            {
            out(sIndent + "  Catch clauses:");
            stmtCatch.printList(sIndent + "    ");
            }

        if (stmtFinally != null)
            {
            out(sIndent + "  Finally clause:");
            stmtFinally.print(sIndent + "    ");
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the first catch clause.
    *
    * @return  the first catch clause (or null if none)
    */
    public CatchClause getCatchClause()
        {
        return stmtCatch;
        }

    /**
    * Set the first catch clause.
    *
    * @param stmtCatch  the first catch clause
    */
    protected void setCatchClause(CatchClause stmtCatch)
        {
        this.stmtCatch = stmtCatch;
        }

    /**
    * Get the finally clause.
    *
    * @return  the finally clause (or null if none)
    */
    public FinallyClause getFinallyClause()
        {
        return stmtFinally;
        }

    /**
    * Set the finally clause.
    *
    * @param stmtFinally  the finally clause
    */
    protected void setFinallyClause(FinallyClause stmtFinally)
        {
        this.stmtFinally = stmtFinally;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "TryStatement";

    /**
    * The linked list of catch clauses (or null).
    */
    private CatchClause   stmtCatch;

    /**
    * The finally clause (or null).
    */
    private FinallyClause stmtFinally;
    }
