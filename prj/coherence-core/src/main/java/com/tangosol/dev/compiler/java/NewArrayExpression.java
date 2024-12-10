/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Znewarray;
import com.tangosol.dev.assembler.Bnewarray;
import com.tangosol.dev.assembler.Cnewarray;
import com.tangosol.dev.assembler.Snewarray;
import com.tangosol.dev.assembler.Inewarray;
import com.tangosol.dev.assembler.Lnewarray;
import com.tangosol.dev.assembler.Fnewarray;
import com.tangosol.dev.assembler.Dnewarray;
import com.tangosol.dev.assembler.Anewarray;
import com.tangosol.dev.assembler.Multianewarray;
import com.tangosol.dev.assembler.ClassConstant;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the array allocation (new) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class NewArrayExpression extends NewExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a NewArrayExpression.
    *
    * @param block     the containing block
    * @param tokFirst  the first token (new)
    * @param tokLast   the last token
    * @param type      the type expression
    * @param dims      the dimension expressions and null dims
    * @param value     an ArrayExpression (or null)
    */
    public NewArrayExpression(Block block, Token tokFirst, Token tokLast, TypeExpression type, Expression[] dims, ArrayExpression value)
        {
        super(block, tokFirst, tokLast, type);

        this.aexprDims = dims;
        this.exprValue = value;
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
        // type expression is supplied by the super
        super.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        TypeExpression exprType = getTypeExpression();
        DataType       dtArray  = exprType.getType();

        // dimensions, the last n of which may be null
        Expression[] aDims    = this.aexprDims;
        int          cDims    = aDims.length;
        boolean      fDimExpr = false;
        for (int i = 0; i < cDims; ++i)
            {
            Expression expr = aDims[i];

            if (expr != null)
                {
                fDimExpr = true;

                expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
                if (expr.checkAssignable(ctx, INT, errlist))
                    {
                    expr = expr.promoteNumeric();
                    }

                aDims[i] = expr;
                }

            dtArray = dtArray.getArrayType();
            }

        // array initializer, if any
        ArrayExpression exprValue = this.exprValue;
        if (exprValue != null)
            {
            // it is an error if there are both dimension expressions and
            // an array initializer
            if (fDimExpr)
                {
                logError(ERROR, ARRAY_DIMENSIONS, null, errlist);
                }

            if (dtArray != UNKNOWN)
                {
                exprValue.setType(dtArray);
                this.exprValue = exprValue = (ArrayExpression) exprValue.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
                }
            }

        // store resulting type of array
        setType(dtArray);

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
        // compilation for "new <type> [<expr>]":
        //
        //      <expr>
        //      ?newarray <type>    // <type> omitted for primitive types
        //
        // compilation for "new <type> [<expr-1>]...[<expr-n>][]...[]"
        // with n dimension expressions and m additional dims where n+m>1:
        //
        //      [expr-1]
        //      ...
        //      [expr-n]
        //      multianewarray <type>, n
        //
        // compilation for "new <type> []...[] <initializer>":
        //
        //      [initializer]

        ArrayExpression exprValue = this.exprValue;
        if (exprValue != null)
            {
            return exprValue.compile(ctx, code, fReached, errlist);
            }

        Expression[] aDims = this.aexprDims;
        int          cDims = aDims.length;
        int          cExpr = 0;
        for (int i = 0; i < cDims; ++i)
            {
            Expression expr = aDims[i];
            if (expr == null)
                {
                break;
                }

            expr.compile(ctx, code, fReached, errlist);
            ++cExpr;
            }

        DataType dtArray = getType();
        if (cDims == 1)
            {
            switch (dtArray.getElementType().getTypeString().charAt(0))
                {
                case 'Z':
                    code.add(new Znewarray());
                    break;
                case 'B':
                    code.add(new Bnewarray());
                    break;
                case 'C':
                    code.add(new Cnewarray());
                    break;
                case 'S':
                    code.add(new Snewarray());
                    break;
                case 'I':
                    code.add(new Inewarray());
                    break;
                case 'J':
                    code.add(new Lnewarray());
                    break;
                case 'F':
                    code.add(new Fnewarray());
                    break;
                case 'D':
                    code.add(new Dnewarray());
                    break;
                case 'N':
                case 'L':
                case 'R':
                case '[':
                    code.add(new Anewarray(dtArray.getElementType().getClassConstant()));
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
        else
            {
            code.add(new Multianewarray(dtArray.getClassConstant(), cExpr));
            }

        return fReached;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the array dimensions (including null dims).
    *
    * @return  the array dimensions as an array of expressions
    */
    public Expression[] getDimensions()
        {
        return aexprDims;
        }

    /**
    * Get the array initializer, if any.
    *
    * @return  the array initializer or null
    */
    public ArrayExpression getInitializer()
        {
        return exprValue;
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine if the expression has a constant value.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        return exprValue != null && exprValue.isConstant();
        }

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        return exprValue.getValue();
        }


    // ----- Element methods ------------------------------------------------

    /**
    * Print the element information.
    *
    * @param sIndent
    */
    public void print(String sIndent)
        {
        super.print(sIndent);

        out(sIndent + "  dimensions:");
        Expression[] aexpr = aexprDims;
        int          cexpr = aexpr.length;
        for (int i = 0; i < cexpr; ++i)
            {
            if (aexpr[i] == null)
                {
                out(sIndent + "    <null>");
                }
            else
                {
                aexpr[i].print(sIndent + "    ");
                }
            }

        if (exprValue != null)
            {
            out(sIndent + "  value:");
            exprValue.print(sIndent + "    ");
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "NewArrayExpression";

    /**
    * Unknown data type.
    */
    private static final DataType UNKNOWN = DataType.UNKNOWN;

    /**
    * Int data type.
    */
    private static final DataType INT = DataType.INT;

    /**
    * The array of dimension expressions and null dims.
    */
    private Expression[] aexprDims;

    /**
    * The initializer.
    */
    private ArrayExpression exprValue;
    }
