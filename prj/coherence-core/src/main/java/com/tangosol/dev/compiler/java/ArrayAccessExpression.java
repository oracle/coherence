/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Op;
import com.tangosol.dev.assembler.Baload;
import com.tangosol.dev.assembler.Caload;
import com.tangosol.dev.assembler.Saload;
import com.tangosol.dev.assembler.Iaload;
import com.tangosol.dev.assembler.Laload;
import com.tangosol.dev.assembler.Faload;
import com.tangosol.dev.assembler.Daload;
import com.tangosol.dev.assembler.Aaload;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* The array access ([]) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class ArrayAccessExpression extends UnaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a ArrayAccessExpression.
    *
    * @param operator  the operator token
    * @param expr      the array expression
    * @param index     the array element index expression
    */
    public ArrayAccessExpression(Token operator, Expression expr, Expression index, Token tokLast)
        {
        super(operator, expr);

        setStartToken(expr.getStartToken());
        setEndToken(tokLast);

        this.index = index;
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
        Expression exprArray = getArray();
        Expression exprIndex = getIndex();

        exprArray = (Expression) exprArray.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        exprIndex = (Expression) exprIndex.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // type of sub-expression must be array
        // index must be int (or promotable to int)
        if (exprArray.checkArray(errlist) && exprIndex.checkAssignable(ctx, INT, errlist))
            {
            exprIndex = exprIndex.promoteNumeric();
            setType(exprArray.getType().getElementType());
            }

        setArray(exprArray);
        setIndex(exprIndex);

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
        // this is the compilation for array access, not storing into an
        // array; that is handled by AssignExpression

        if (!ctx.isDebug() && isConstant() && !(getValue() instanceof Object[]))
            {
            // Expression has compiler implementation for constant values
            return super.compile(ctx, code, fReached, errlist);
            }

        // compilation for "<expr>[<index>]":
        //
        //      [expr]
        //      [index]
        //      ?aload

        getArray().compile(ctx, code, fReached, errlist);
        getIndex().compile(ctx, code, fReached, errlist);

        Op opLoad = null;
        switch (getType().getTypeString().charAt(0))
            {
            case 'Z':
            case 'B':
                opLoad = new Baload();
                break;
            case 'C':
                opLoad = new Caload();
                break;
            case 'S':
                opLoad = new Saload();
                break;
            case 'I':
                opLoad = new Iaload();
                break;
            case 'J':
                opLoad = new Laload();
                break;
            case 'F':
                opLoad = new Faload();
                break;
            case 'D':
                opLoad = new Daload();
                break;
            case '[':
            case 'N':
            case 'L':
            case 'R':
                opLoad = new Aaload();
                break;
            default:
                throw new IllegalStateException();
            }
        code.add(opLoad);

        return fReached;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the array reference expression.
    *
    * @return  the array reference expression
    */
    public Expression getArray()
        {
        return getExpression();
        }

    /**
    * Set the array reference expression.
    *
    * @param expr  the array reference expression
    */
    protected void setArray(Expression expr)
        {
        setExpression(expr);
        }

    /**
    * Get the array index expression.
    *
    * @return  the array index expression
    */
    public Expression getIndex()
        {
        return index;
        }

    /**
    * Set the array index expression.
    *
    * @param expr  the array index expression
    */
    protected void setIndex(Expression expr)
        {
        index = expr;
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine if the expression has a constant value.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        if (getArray().isConstant() && getIndex().isConstant())
            {
            Object[] a = (Object[]) getArray().getValue();
            int      i = ((Number) getIndex().getValue()).intValue();
            return a != null && i >= 0 && i < a.length;
            }

        return false;
        }

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        Object[] a = (Object[]) getArray().getValue();
        int      i = ((Number) getIndex().getValue()).intValue();

        return a[i];
        }

    /**
    * Check that the expression is assignable (a "variable").  This call
    * may occur before pre-compilation.
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if the expression is a variable
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkAssignable(ErrorList errlist)
            throws CompilerException
        {
        return true;
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

        out(sIndent + "  Index:");
        index.print(sIndent + "    ");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ArrayAccessExpression";

    /**
    * Int data type.
    */
    private static final DataType INT = DataType.INT;

    /**
    * The array index expression.
    */
    private Expression index;
    }
