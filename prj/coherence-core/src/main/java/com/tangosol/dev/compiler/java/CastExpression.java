/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.Checkcast;
import com.tangosol.dev.assembler.ClassConstant;
import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.D2f;
import com.tangosol.dev.assembler.D2i;
import com.tangosol.dev.assembler.D2l;
import com.tangosol.dev.assembler.F2d;
import com.tangosol.dev.assembler.F2i;
import com.tangosol.dev.assembler.F2l;
import com.tangosol.dev.assembler.I2b;
import com.tangosol.dev.assembler.I2c;
import com.tangosol.dev.assembler.I2d;
import com.tangosol.dev.assembler.I2f;
import com.tangosol.dev.assembler.I2l;
import com.tangosol.dev.assembler.I2s;
import com.tangosol.dev.assembler.L2d;
import com.tangosol.dev.assembler.L2f;
import com.tangosol.dev.assembler.L2i;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.component.DataType;
import com.tangosol.util.ErrorList;

import java.util.Map;


/**
* The cast () expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class CastExpression extends UnaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a CastExpression.
    *
    * @param operator  the operator token
    * @param expr      the sub-expression
    * @param exprType  the type cast to
    */
    public CastExpression(Token operator, Expression expr, TypeExpression exprType)
        {
        super(operator, expr);

        this.exprType = exprType;
        }

    /**
    * Construct a CastExpression.
    *
    * @param expr  the sub-expression
    * @param type  the type cast to
    */
    public CastExpression(Expression expr, DataType type)
        {
        super(expr.getStartToken(), expr);

        setType(type);
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
        // precompile the sub-expression (which will be cast)
        Expression expr = getExpression();
        setExpression(expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist));

        TypeExpression exprType = this.exprType;
        if (exprType != null)
            {
            // specified cast -- the cast must be checked

            // precompile the cast-to type
            this.exprType = exprType = (TypeExpression) exprType.precompile(
                    ctx, setUVars, setFVars, mapThrown, errlist);

            // check the cast
            DataType dt = exprType.getType();
            expr.checkCastable(ctx, dt, errlist);
            setType(dt);
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
    protected boolean compile(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        // the theory behind compiling a cast expression is simple, which
        // is to determine what the cast is supposed to do:
        //  - runtime primitive conversions (e.g. double to int)
        //  - runtime reference type check (JASM checkcast op)

        Expression expr     = getExpression();
        DataType   typeExpr = expr.getType();
        DataType   typeCast = this.getType();

        // compile the sub-expression
        expr.compile(ctx, code, fReached, errlist);

        switch (typeExpr.getTypeString().charAt(0))
            {
            case 'Z':
                // implied identity conversion
                // (boolean to boolean conversion is the only valid one)
                break;

            case 'B':
                switch (typeCast.getTypeString().charAt(0))
                    {
                    case 'C':
                        code.add(new I2c());
                        break;
                    case 'S':
                        code.add(new I2s());
                        break;
                    case 'J':
                        code.add(new I2l());
                        break;
                    case 'F':
                        code.add(new I2f());
                        break;
                    case 'D':
                        code.add(new I2d());
                        break;
                    }
                break;

            case 'C':
                switch (typeCast.getTypeString().charAt(0))
                    {
                    case 'B':
                        code.add(new I2b());
                        break;
                    case 'S':
                        code.add(new I2s());
                        break;
                    case 'J':
                        code.add(new I2l());
                        break;
                    case 'F':
                        code.add(new I2f());
                        break;
                    case 'D':
                        code.add(new I2d());
                        break;
                    }
                break;

            case 'S':
                switch (typeCast.getTypeString().charAt(0))
                    {
                    case 'B':
                        code.add(new I2b());
                        break;
                    case 'C':
                        code.add(new I2c());
                        break;
                    case 'J':
                        code.add(new I2l());
                        break;
                    case 'F':
                        code.add(new I2f());
                        break;
                    case 'D':
                        code.add(new I2d());
                        break;
                    }
                break;

            case 'I':
                switch (typeCast.getTypeString().charAt(0))
                    {
                    case 'B':
                        code.add(new I2b());
                        break;
                    case 'C':
                        code.add(new I2c());
                        break;
                    case 'S':
                        code.add(new I2s());
                        break;
                    case 'J':
                        code.add(new I2l());
                        break;
                    case 'F':
                        code.add(new I2f());
                        break;
                    case 'D':
                        code.add(new I2d());
                        break;
                    }
                break;

            case 'J':
                switch (typeCast.getTypeString().charAt(0))
                    {
                    case 'B':
                        code.add(new L2i());
                        code.add(new I2b());
                        break;
                    case 'C':
                        code.add(new L2i());
                        code.add(new I2c());
                        break;
                    case 'S':
                        code.add(new L2i());
                        code.add(new I2s());
                        break;
                    case 'I':
                        code.add(new L2i());
                        break;
                    case 'F':
                        code.add(new L2f());
                        break;
                    case 'D':
                        code.add(new L2d());
                        break;
                    }
                break;

            case 'F':
                switch (typeCast.getTypeString().charAt(0))
                    {
                    case 'B':
                        code.add(new F2i());
                        code.add(new I2b());
                        break;
                    case 'C':
                        code.add(new F2i());
                        code.add(new I2c());
                        break;
                    case 'S':
                        code.add(new F2i());
                        code.add(new I2s());
                        break;
                    case 'I':
                        code.add(new F2i());
                        break;
                    case 'J':
                        code.add(new F2l());
                        break;
                    case 'D':
                        code.add(new F2d());
                        break;
                    }
                break;

            case 'D':
                switch (typeCast.getTypeString().charAt(0))
                    {
                    case 'B':
                        code.add(new D2i());
                        code.add(new I2b());
                        break;
                    case 'C':
                        code.add(new D2i());
                        code.add(new I2c());
                        break;
                    case 'S':
                        code.add(new D2i());
                        code.add(new I2s());
                        break;
                    case 'I':
                        code.add(new D2i());
                        break;
                    case 'J':
                        code.add(new D2l());
                        break;
                    case 'F':
                        code.add(new D2f());
                        break;
                    }
                break;

            case 'N':
                // widening reference conversion
                // (null can be cast to any reference type)
                break;

            case 'L':
            case 'R':
            case '[':
                // determine if a runtime check is necessary
                // (a runtime check is necessary if the type of the
                // expression is not assignment compatible with the
                // type of the cast)
                if (!exprType.checkAssignable(ctx, typeCast, null))
                    {
                    // use context to get the class constant in case
                    // the data type we are dealing with is actually
                    // optimized out (e.g. "is child discarded")
                    code.add(new Checkcast((ClassConstant)
                            ctx.getTypeInfo(typeCast).getConstant()));
                    }
                break;

            default:
                throw new IllegalStateException();
            }

        return fReached;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the type expression for the cast.  The type expression is
    * null if the cast is an "implied" cast, for example by the op= operators
    * such as "+=".  The type expression is not null if the cast is specified
    * directly, for example "x=(int)y;".
    *
    * @return the type expression for the cast
    */
    public TypeExpression getTypeExpression()
        {
        return exprType;
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine if the expression has a constant value.
    * Note:  This question is only valid after the expression is pre-compiled.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        // constant numeric expressions
        return getExpression().isConstant() && getType().isNumeric();
        }

    /**
    * Determine the constant value of the expression.
    * Note:  This question is only valid in the compile step.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        Number number = (Number) getExpression().getValue();
        switch (getType().getTypeString().charAt(0))
            {
            case 'B':
                return number.byteValue();
            case 'C':
                return (int) (char) number.intValue();
            case 'S':
                return number.shortValue();
            case 'I':
                return number.intValue();
            case 'J':
                return number.longValue();
            case 'F':
                return number.floatValue();
            case 'D':
                return number.doubleValue();
            default:
                throw new IllegalStateException();
            }
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

        DataType type = getType();
        out(sIndent + "  Cast to:  " + (exprType == null ? type.toString() : ""));
        if (exprType != null)
            {
            exprType.print(sIndent + "    ");
            }

        out(sIndent + "  Sub-expression:");
        getExpression().print(sIndent + "    ");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "CastExpression";

    /**
    * The type expression being cast to.
    */
    private TypeExpression exprType;
    }
