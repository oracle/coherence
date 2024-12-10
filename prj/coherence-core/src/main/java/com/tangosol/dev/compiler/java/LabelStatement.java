/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.DeltaSet;

import java.util.Set;
import java.util.Map;


/**
* This class implements a Java statement label by pretending the label is
* itself a statement.  This easily facilitates multiple labels per actual
* statement which is one possible ambiguity of the LabeledStatement grammar.
*
*   LabeledStatement:
*       Identifier : Statement
*
* @version 1.00, 09/15/98
* @author  Cameron Purdy
*/
public class LabelStatement extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an empty statement.
    *
    * @param stmt      the statement within which this element exists
    * @param tokName   the identifier (the label name)
    * @param tokColon  the colon
    */
    public LabelStatement(Statement stmt, Token tokName, Token tokColon)
        {
        super(stmt, tokName);
        setEndToken(tokColon);

        sName = tokName.getText();
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
        // verify that no containing label statement has the same name
        Statement stmt = getOuterStatement();
        while (stmt != null)
            {
            if (stmt instanceof LabelStatement &&
                    sName.equals(((LabelStatement) stmt).sName))
                {
                // A statement labeled by an identifier must not appear
                // anywhere within another statement labeled by the same
                // identifier, or a compile-time error will occur. Two
                // statements can be labeled by the same identifier only
                // if neither statement contains the other.
                getStartToken().logError(ERROR, LABEL_DUPLICATE, new String[] {sName}, errlist);
                break;
                }

            stmt = stmt.getOuterStatement();
            }

        // pre-compile the contained statement
        getInnerStatement().precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // JLS 16.2.4:  V is definitely assigned after a labeled statement
        // L:S (where L is a label) iff V is definitely assigned after S and
        // V is definitely assigned before every break statement that may
        // exit the labeled statement L:S.
        // (getBreakUVars() returns the union of all unassigned variables
        // from each break; setUVars contains all unassigned variables at the
        // end of normal completion of the inner statement)
        setUVars.addAll(getBreakUVars());

        // do the same for final variables that may have been assigned; just
        // filter out any final variables that were declared in an inner
        // scope (by intersecting with all variables declared at this scope)
        Set setAssigned = getBreakFVars();
        if (!setAssigned.isEmpty())
            {
            setAssigned.retainAll(getBlock().getVariables());
            setFVars.addAll(setAssigned);
            }

        // make sure assembly labels exist
        getStartLabel();
        getEndLabel();

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
        return getInnerStatement().compile(ctx, code, fReached, errlist);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the label name.
    *
    * @return the label name
    */
    public String getName()
        {
        return sName;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the element information as a string.
    *
    * @return a human-readable description of the element
    */
    public String toString()
        {
        return super.toString() + " " + sName;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "LabelStatement";

    /**
    * The label name.
    */
    private String sName;
    }
