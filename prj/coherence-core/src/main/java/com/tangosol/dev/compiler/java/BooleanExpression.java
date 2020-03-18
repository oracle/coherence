/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.Iconst;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements a boolean literal expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class BooleanExpression extends LiteralExpression implements TokenConstants, Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a BooleanExpression.
    *
    * @param block    the containing block
    * @param literal  the literal token
    */
    public BooleanExpression(Block block, Token literal)
        {
        super(block, literal);
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
    * @return the resulting language element (typically this)
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Element precompile(Context ctx, DualSet setUVars, DualSet setFVars, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        boolean fVal = ((Boolean) getEndToken().getValue()).booleanValue();

        // JLS 16.1.1 Boolean Constant Expressions
        // V is definitely assigned after any constant expression whose
        // value is true when false. V is definitely assigned after any
        // constant expression whose value is false when true.
        (fVal ? setUVars.getFalseSet() : setUVars.getTrueSet()).clear();
        (fVal ? setFVars.getFalseSet() : setFVars.getTrueSet()).clear();

        setType(DataType.BOOLEAN);

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
    protected boolean compile(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        boolean fVal = ((Boolean) getEndToken().getValue()).booleanValue();
        code.add(new Iconst(fVal ? CONSTANT_ICONST_1 : CONSTANT_ICONST_0));

        return fReached;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BooleanExpression";
    }
