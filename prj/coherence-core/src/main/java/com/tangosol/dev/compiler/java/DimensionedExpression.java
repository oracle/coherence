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
* This class implements a Java script dimensioned type expression.
*
* @version 1.00, 10/09/98
* @author  Cameron Purdy
*/
public class DimensionedExpression extends TypeExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a dimensioned DimensionedExpression.
    *
    * @param expr        the type expression to be dimensioned
    * @param tokBracket  the right bracket
    */
    public DimensionedExpression(TypeExpression expr, Token tokBracket)
        {
        super(expr.getBlock(), expr.getStartToken(), tokBracket);

        element = expr;
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
        element = (TypeExpression) element.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        setType(element.getType().getArrayType());
        
        return this;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the inner type.
    *
    * @return the inner type
    */
    public TypeExpression getTypeExpression()
        {
        return element;
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

        if (element != null)
            {
            out(sIndent + "  Element Expression:");
            element.print(sIndent + "    ");
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the element information as a string.
    *
    * @return a human-readable description of the element
    */
    public String toString()
        {
        String sDims = "";
        TypeExpression expr = this;
        while (expr instanceof DimensionedExpression)
            {
            sDims += "[]";
            expr = ((DimensionedExpression) expr).element;
            }

        return super.toString() + sDims;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "DimensionedExpression";

    /**
    * If this is a dimensioned type expression, the element type is another
    * type expression.
    */
    private TypeExpression element;
    }
