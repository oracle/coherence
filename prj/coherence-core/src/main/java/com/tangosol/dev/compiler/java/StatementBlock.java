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

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* This class implements a block of code in a Java script.  A block is
* typically associated with a local variable scope, i.e. {...}, although
* there is also a root block which is associated with the script as a whole
* (as if the script were enclosed by curlies).
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class StatementBlock extends Block
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct the initial (outermost) Java code block.
    */
    public StatementBlock()
        {
        // pretend there is a curly brace at the beginning of the script
        super(null, new Token(Token.TOK_LCURLYBRACE, 0, 0, 0));
        }

    /**
    * Construct a Java code block.
    *
    * @param outer  the enclosing Java statement
    * @param token  the token starting the block (which is typically a
    *               left curly brace except for the "for" construct)
    */
    public StatementBlock(Statement outer, Token token)
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
        // statement blocks have their own variable scope
        DualSet setUBlockVars = new DualSet(setUVars);
        DualSet setFBlockVars = new DualSet(setFVars);

        // pre-compile the set of inner statements
        Statement stmt = getInnerStatement();
        while (stmt != null)
            {
            stmt.precompile(ctx, setUBlockVars, setFBlockVars, mapThrown, errlist);
            stmt = stmt.getNextStatement();
            }

        // JLS 16.2.2:  V is definitely assigned after a nonempty block iff
        // it is definitely assigned after the last statement in the block.
        Set setAssigned = setUBlockVars.getRemoved();
        if (!setAssigned.isEmpty())
            {
            // it is not possible for a statement block to cause variables
            // from its outer block to become unassigned (i.e. nothing is
            // added to setUVars) but it is possible that some have been
            // assigned (i.e. need to be removed)
            setUVars.removeAll(setAssigned);
            }

        // no new final variables have been declared for the outer block,
        // but it is possible that final variables that were previously
        // declared for the outer block have since been potentially assigned
        if (getBlock() != null)
            {
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
        boolean fCompletes = fReached;

        code.add(new Begin());

        // compile the set of inner statements
        Statement stmt = getInnerStatement();
        while (stmt != null)
            {
            // check if the last statement was reached but did not complete
            // (which means this statement will not be reached)
            if (fReached && !fCompletes)
                {
                stmt.notReached(errlist);
                fReached = false;
                }

            // compile the statement
            fCompletes = stmt.compile(ctx, code, fReached, errlist);

            stmt = stmt.getNextStatement();
            }

        code.add(new End());

        // JLS 14.19: An empty block that is not a switch block can complete
        // normally iff it is reachable. A nonempty block that is not a
        // switch block can complete normally iff the last statement in it
        // can complete normally. The first statement in a nonempty block
        // that is not a switch block is reachable iff the block is
        // reachable.  Every other statement S in a nonempty block that is
        // not a switch block is reachable iff the statement preceding S can
        // complete normally.
        return fCompletes;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "StatementBlock";
    }
