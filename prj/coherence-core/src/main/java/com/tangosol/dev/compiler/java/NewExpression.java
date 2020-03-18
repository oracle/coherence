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

import java.util.Set;
import java.util.Map;


/**
* Implements the class and array allocation (new) expressions.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public abstract class NewExpression extends Expression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a NewExpression.
    *
    * @param block     the containing block
    * @param tokFirst  the first token (new)
    * @param tokLast   the last token
    * @param exprType  the type expression
    */
    protected NewExpression(Block block, Token tokFirst, Token tokLast, TypeExpression exprType)
        {
        super(block, tokFirst);
        setEndToken(tokLast);

        this.exprType = exprType;
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
        exprType = (TypeExpression) exprType.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        return this;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the type expression of the new expression.
    *
    * @return the type expression
    */
    public TypeExpression getTypeExpression()
        {
        return exprType;
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

        out(sIndent + "  type:");
        exprType.print(sIndent + "    ");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "NewExpression";

    /**
    * The type expression.
    */
    private TypeExpression exprType;
    }
