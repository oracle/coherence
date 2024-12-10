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
import com.tangosol.dev.assembler.IntConstant;
import com.tangosol.dev.assembler.Ixor;
import com.tangosol.dev.assembler.Lconst;
import com.tangosol.dev.assembler.LongConstant;
import com.tangosol.dev.assembler.Lxor;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.component.DataType;
import com.tangosol.util.ErrorList;

import java.util.Map;


/**
* The bitwise not (~) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class BitNotExpression extends UnaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a BitNotExpression.
    *
    * @param operator  the operator token
    * @param expr      the sub-expression
    */
    public BitNotExpression(Token operator, Expression expr)
        {
        super(operator, expr);
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
        // get the sub-expression
        Expression expr = getExpression();

        // pre-compile the sub-expression
        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // sub-expression must be integral
        if (expr.checkIntegral(errlist))
            {
            // numeric promotion is applied to the sub-expression
            expr = expr.promoteNumeric();

            // store the sub-expression
            setExpression(expr);

            // the result type is the type of the sub-expression
            setType(expr.getType());
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
        Expression expr  = getExpression();
        boolean    fLong = (expr.getType() == DataType.LONG);

        if (!ctx.isDebug() && expr.isConstant())
            {
            if (fLong)
                {
                // get the constant value of the sub-expression
                long l = ((Number) expr.getValue()).longValue();
                code.add(new Lconst(~l));
                }
            else
                {
                int i = ((Number) expr.getValue()).intValue();
                code.add(new Iconst(~i));
                }
            }
        else
            {
            expr.compile(ctx, code, fReached, errlist);
            if (fLong)
                {
                code.add(new Lconst(LCONST_M1));
                code.add(new Lxor());
                }
            else
                {
                code.add(new Iconst(ICONST_M1));
                code.add(new Ixor());
                }
            }

        // normal completion possible if reachable
        return fReached;
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine if the expression has a constant value.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        return getExpression().isConstant();
        }

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        Expression expr = getExpression();
        boolean    fLong = (expr.getType() == DataType.LONG);

        if (fLong)
            {
            // get the constant value of the sub-expression
            long l = ((Long) expr.getValue()).longValue();
            return Long.valueOf(~l);
            }
        else
            {
            int i = ((Integer) expr.getValue()).intValue();
            return Integer.valueOf(~i);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BitNotExpression";

    /**
    * The integer -1 used to implement bitnot..
    */
    private static final IntConstant ICONST_M1 = Constants.CONSTANT_ICONST_M1;

    /**
    * The long -1 used to implement bitnot..
    */
    private static final LongConstant LCONST_M1 = new LongConstant(-1L);
    }
