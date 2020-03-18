/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Begin;
import com.tangosol.dev.assembler.Rvar;
import com.tangosol.dev.assembler.Rstore;
import com.tangosol.dev.assembler.Ret;
import com.tangosol.dev.assembler.End;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* This class implements a finally clause in the Java try statement.
*
*   Finally:
*       finally Block
*
* @version 1.00, 09/15/98
* @author  Cameron Purdy
*/
public class FinallyClause extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a finally clause.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the finally token
    */
    public FinallyClause(Statement stmt, Token token)
        {
        super(stmt, token);

        ((TryStatement) stmt).setUnwindLabel(getStartLabel());
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
        // pre-compile the finally block
        getFinallyBlock().precompile(ctx, setUVars, setFVars, mapThrown, errlist);

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
        boolean fCompletes = fReached;

        // Compilation for finally:
        //
        //      begin
        //      rvar    retaddr
        //      rstore  retaddr
        //      [block]
        //      ret     retaddr
        //      end

        Rvar retaddr = new Rvar();

        code.add(new Begin());
        code.add(retaddr);
        code.add(new Rstore(retaddr));
        fCompletes &= getFinallyBlock().compileImpl(ctx, code, fReached, errlist);
        code.add(new Ret(retaddr));
        code.add(new End());

        // JLS 14.19: (See comment in StatementBlock, which is the type of
        // statement which will be contained within a FinallyClause.)
        return fCompletes;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the finally block.
    *
    * @return  the statement which is the finally block
    */
    public Statement getFinallyBlock()
        {
        return getInnerStatement();
        }

    /**
    * Set the finally block.
    *
    * @param stmt  the statement which is the finally block
    */
    protected void setFinallyBlock(Statement stmt)
        {
        setInnerStatement(stmt);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "FinallyClause";
    }
