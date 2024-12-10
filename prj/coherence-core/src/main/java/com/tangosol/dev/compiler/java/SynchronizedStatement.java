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
import com.tangosol.dev.assembler.Monitorenter;
import com.tangosol.dev.assembler.Monitorexit;
import com.tangosol.dev.assembler.Avar;
import com.tangosol.dev.assembler.Aload;
import com.tangosol.dev.assembler.Astore;
import com.tangosol.dev.assembler.Athrow;
import com.tangosol.dev.assembler.Rvar;
import com.tangosol.dev.assembler.Rstore;
import com.tangosol.dev.assembler.Ret;
import com.tangosol.dev.assembler.Try;
import com.tangosol.dev.assembler.Catch;
import com.tangosol.dev.assembler.Goto;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Dup;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* This class implements the synchronized keyword in a Java script.
*
* @version 1.00, 09/14/98
* @author  Cameron Purdy
*/
public class SynchronizedStatement extends GuardedStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a synchronized Java statement.
    *
    * @param outer  the enclosing Java statement
    * @param token  the synchronized keyword
    */
    public SynchronizedStatement(Statement outer, Token token)
        {
        super(outer, token);

        setUnwindLabel(new Label());
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
        Expression expr = monitor;
        // precompile the monitor expression
        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        // must be a reference type
        expr.checkReference(errlist);
        monitor = expr;

        // precompile the synchronized statement
        getInnerStatement().precompile(ctx, setUVars, setFVars, mapThrown, errlist);

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
    protected boolean compileImpl(Context ctx, CodeAttribute code,
                                  boolean fReached, ErrorList errlist)
        throws CompilerException
        {
        // The construct:
        //
        //  synchronized(expr) block
        //
        // used to be incorrectly compiled as:
        //
        //              begin
        //              avar        #monitor
        //              [expr]
        //              dup
        //              astore      #monitor
        //              monitorenter
        //              try
        //              [block]
        //              aload       #monitor
        //              catch       null, guard
        //              monitorexit
        //              goto        exit
        //  guard:      aload       #monitor
        //              monitorexit
        //              athrow
        //  unwind:     begin
        //              rvar        #addr
        //              rstore      #addr
        //              aload       #monitor
        //              monitorexit
        //              ret         #addr
        //              end
        //              end
        //  exit:
        //
        // is now correctly compiled as:
        //
        //               begin
        //               avar        #monitor
        //               [expr]
        //               dup
        //               astore      #monitor
        //               monitorenter
        //               try
        //               [block]
        //               aload       #monitor
        //               monitorexit
        //               catch       null, guard
        //               goto        exit
        //  guard:       try
        //               astore      #exception
        //               aload       #monitor
        //               monitorexit
        //               catch       null, guard
        //               aload       #exception
        //               athrow
        //  unwind:      begin
        //               rvar        #addr
        //               rstore      #addr
        //               try
        //               aload       #monitor
        //               monitorexit
        //               catch       null, guardunwind
        //               ret         #addr
        //               end
        //  guardunwind: try
        //               astore      #exception
        //               aload       #monitor
        //               monitorexit
        //               catch       null, guard
        //               aload       #exception
        //               athrow
        //               end
        //  exit:

        Expression expr = monitor;
        Statement  stmt = getInnerStatement();

        Avar  varMonitor = new Avar();
        Rvar  varRetAddr = new Rvar();

        Try   opTry      = new Try();
        Label lblGuard   = new Label();

        // evaluate monitor expression
        code.add(new Begin());
        code.add(varMonitor);
        expr.compile(ctx, code, fReached, errlist);
        code.add(new Dup());
        code.add(new Astore(varMonitor));

        // guarded (synchronized) section
        code.add(new Monitorenter());
        code.add(opTry);
        boolean fCompletes = stmt.compile(ctx, code, fReached, errlist);
        code.add(new Aload(varMonitor));
        code.add(new Monitorexit());

        // guard up to Monitorexit (exclusive)
        code.add(new Catch(opTry, null, lblGuard));

        // jump to exit
        code.add(new Goto(getEndLabel()));

        // exception handler
        Try   opTryForever = new Try();
        Avar  varException = new Avar();

        code.add(lblGuard);
        code.add(new Begin());
        code.add(varException);
        code.add(opTryForever);
        code.add(new Astore(varException));
        code.add(new Aload(varMonitor));
        code.add(new Monitorexit());

        // guard up to Monitorexit (exclusive)
        code.add(new Catch(opTryForever, null, lblGuard));

        code.add(new Aload(varException));
        code.add(new Athrow());
        code.add(new End());

        // "finally" clause for a synchronized block
        Try   opTryFinally   = new Try();
        Label lblGuardUnwind = new Label();

        code.add(getUnwindLabel());
        code.add(new Begin());
        code.add(varRetAddr);
        code.add(opTryFinally);
        code.add(new Rstore(varRetAddr));
        code.add(new Aload(varMonitor));
        code.add(new Monitorexit());
        code.add(new Catch(opTryFinally, null, lblGuardUnwind));
        code.add(new Ret(varRetAddr));

        // finally exception handler
        Try   opTryFinallyForever = new Try();
        Avar  varFinallyException = new Avar();

        code.add(lblGuardUnwind);
        code.add(new Begin());
        code.add(varFinallyException);
        code.add(opTryFinallyForever);
        code.add(new Astore(varFinallyException));
        code.add(new Aload(varMonitor));
        code.add(new Monitorexit());

        // guard up to Monitorexit (exclusive)
        code.add(new Catch(opTryFinallyForever, null, lblGuardUnwind));

        code.add(new Aload(varFinallyException));
        code.add(new Athrow());
        code.add(new End());  // guarded unwind block
        code.add(new End());  // unwind block
        code.add(new End());  // method

        // JLS 14.19: A synchronized statement can complete normally iff
        // the contained statement can complete normally. The contained
        // statement is reachable iff the synchronized statement is
        // reachable.
        return fCompletes;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the monitor expression.
    *
    * @return  the reference for the monitor statement
    */
    public Expression getExpression()
        {
        return monitor;
        }

    /**
    * Set the monitor expression.
    *
    * @param expr  the reference for the monitor statement
    */
    protected void setExpression(Expression expr)
        {
        this.monitor = expr;
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

        out(sIndent + "  Monitor:");
        monitor.print(sIndent + "    ");

        out(sIndent + "  Synchronized Block:");
        getInnerStatement().print(sIndent + "    ");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "SynchronizedStatement";

    /**
    * The expression being synchronized around.
    */
    private Expression monitor;
    }
