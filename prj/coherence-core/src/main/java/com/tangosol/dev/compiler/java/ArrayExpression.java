/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Op;
import com.tangosol.dev.assembler.Iconst;
import com.tangosol.dev.assembler.Dup;
import com.tangosol.dev.assembler.Znewarray;
import com.tangosol.dev.assembler.Bnewarray;
import com.tangosol.dev.assembler.Cnewarray;
import com.tangosol.dev.assembler.Snewarray;
import com.tangosol.dev.assembler.Inewarray;
import com.tangosol.dev.assembler.Lnewarray;
import com.tangosol.dev.assembler.Fnewarray;
import com.tangosol.dev.assembler.Dnewarray;
import com.tangosol.dev.assembler.Anewarray;
import com.tangosol.dev.assembler.Bastore;
import com.tangosol.dev.assembler.Castore;
import com.tangosol.dev.assembler.Sastore;
import com.tangosol.dev.assembler.Iastore;
import com.tangosol.dev.assembler.Lastore;
import com.tangosol.dev.assembler.Fastore;
import com.tangosol.dev.assembler.Dastore;
import com.tangosol.dev.assembler.Aastore;
import com.tangosol.dev.assembler.ClassConstant;


import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the array ({}) expression used in array initializers and array
* allocation expressions.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class ArrayExpression extends Expression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a ArrayExpression.
    *
    * @param block     the containing block
    * @param tokFirst  the first token ({)
    * @param tokLast   the last token (})
    * @param aexpr     an array of expressions
    */
    public ArrayExpression(Block block, Token tokFirst, Token tokLast, Expression[] aexpr)
        {
        super(block, tokFirst, tokLast);

        this.aexpr = aexpr;
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
        // unlike other expressions, the array expression does not determine
        // its own type; by this point, the type should have been supplied
        if (getType() == UNKNOWN)
            {
            logError(ERROR, ARRAY_UNEXPECTED, null, errlist);
            return this;
            }

        DataType dtElement = getElementType();
        char     chType    = dtElement.getTypeString().charAt(0);

        Expression[] aexpr = this.aexpr;
        int          cexpr = aexpr.length;

        for (int i = 0; i < cexpr; ++i)
            {
            Expression expr = aexpr[i];

            if (expr instanceof ArrayExpression)
                {
                if (dtElement.isArray())
                    {
                    expr.setType(dtElement);
                    }
                else
                    {
                    logError(ERROR, BAD_INITIALIZER, new String[] {dtElement.toString()}, errlist);
                    continue;
                    }
                }

            expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
            if (expr.checkAssignable(ctx, dtElement, errlist))
                {
                expr = expr.convertAssignable(ctx, dtElement);
                }
            aexpr[i] = expr;
            }

        // store expressions
        this.aexpr = aexpr;

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
        // compilation for "{e0, e1, ..., en}":
        //
        //      iconst n
        //      ?newarray
        //
        // for element e at index i:
        //
        //      dup
        //      iconst i
        //      [e]
        //      ?astore

        DataType dtElement = getElementType();
        char     chType    = dtElement.getTypeString().charAt(0);

        Expression[] aexpr = this.aexpr;
        int          cexpr = aexpr.length;

        // size of array to create
        code.add(new Iconst(cexpr));

        // array creation
        Op opNew = null;
        switch (chType)
            {
            case 'Z':
                opNew = new Znewarray();
                break;
            case 'B':
                opNew = new Bnewarray();
                break;
            case 'C':
                opNew = new Cnewarray();
                break;
            case 'S':
                opNew = new Snewarray();
                break;
            case 'I':
                opNew = new Inewarray();
                break;
            case 'J':
                opNew = new Lnewarray();
                break;
            case 'F':
                opNew = new Fnewarray();
                break;
            case 'D':
                opNew = new Dnewarray();
                break;
            case 'N':
            case 'L':
            case 'R':
            case '[':
                opNew = new Anewarray(dtElement.getClassConstant());
                break;
            default:
                throw new IllegalStateException();
            }
        code.add(opNew);

        boolean fDebug = ctx.isDebug();
        for (int i = 0; i < cexpr; ++i)
            {
            Expression expr   = aexpr[i];
            boolean    fNoOpt = fDebug || !expr.isConstant();

            Op opStore = null;
            switch (chType)
                {
                case 'Z':
                    if (fNoOpt || ((Boolean) expr.getValue()).booleanValue() != false)
                        {
                        opStore = new Bastore();
                        }
                    break;
                case 'B':
                    if (fNoOpt || ((Number) expr.getValue()).byteValue() != (byte) 0)
                        {
                        opStore = new Bastore();
                        }
                    break;
                case 'C':
                    if (fNoOpt || (char) ((Number) expr.getValue()).intValue() != '\0')
                        {
                        opStore = new Castore();
                        }
                    break;
                case 'S':
                    if (fNoOpt || ((Number) expr.getValue()).shortValue() != (short) 0)
                        {
                        opStore = new Sastore();
                        }
                    break;
                case 'I':
                    if (fNoOpt || ((Number) expr.getValue()).intValue() != 0)
                        {
                        opStore = new Iastore();
                        }
                    break;
                case 'J':
                    if (fNoOpt || ((Number) expr.getValue()).longValue() != 0L)
                        {
                        opStore = new Lastore();
                        }
                    break;
                case 'F':
                    if (fNoOpt || ((Number) expr.getValue()).floatValue() != 0.0F)
                        {
                        opStore = new Fastore();
                        }
                    break;
                case 'D':
                    if (fNoOpt || ((Number) expr.getValue()).doubleValue() != 0.0)
                        {
                        opStore = new Dastore();
                        }
                    break;
                case 'N':
                case 'L':
                case 'R':
                case '[':
                    if (fNoOpt || expr.getValue() != null)
                        {
                        opStore = new Aastore();
                        }
                    break;
                default:
                    throw new IllegalStateException();
                }

            // set element <expr> value at index <i>
            if (opStore != null)
                {
                code.add(new Dup());
                code.add(new Iconst(i));
                expr.compile(ctx, code, fReached, errlist);
                code.add(opStore);
                }
            }

        return fReached;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the contents of the array expression.
    *
    * @return the array of expressions
    */
    public Expression[] getElements()
        {
        return aexpr;
        }

    /**
    * Determine the type of the contents of the array.
    *
    * @return the declared type of the array elements
    */
    public DataType getElementType()
        {
        return getType().getElementType();
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine if the expression has a constant value.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        // constant iff all expressions are constant
        Expression[] aexpr = this.aexpr;
        int          cexpr = aexpr.length;
        for (int i = 0; i < cexpr; ++i)
            {
            if (!aexpr[i].isConstant())
                {
                return false;
                }
            }

        return true;
        }

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        Expression[] aexpr = this.aexpr;
        int          cexpr = aexpr.length;

        Object[] aval = new Object[cexpr];
        for (int i = 0; i < cexpr; ++i)
            {
            aval[i] = aexpr[i].getValue();
            }

        return aval;
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

        Expression[] aexpr = this.aexpr;
        int          cexpr = aexpr.length;
        for (int i = 0; i < cexpr; ++i)
            {
            out(sIndent + "  [" + i + "]");
            aexpr[i].print(sIndent + "    ");
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ArrayExpression";

    /**
    * Unknown data type.
    */
    private static final DataType UNKNOWN = DataType.UNKNOWN;

    /**
    * Null data type.
    */
    private static final DataType NULL = DataType.NULL;

    /**
    * The array of expressions.
    */
    private Expression[] aexpr;
    }
