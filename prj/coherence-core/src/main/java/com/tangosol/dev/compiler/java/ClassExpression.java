/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.ClassConstant;
import com.tangosol.dev.assembler.SignatureConstant;
import com.tangosol.dev.assembler.FieldConstant;
import com.tangosol.dev.assembler.MethodConstant;
import com.tangosol.dev.assembler.Getstatic;
import com.tangosol.dev.assembler.Invokestatic;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* The .class expression.
*
* @version 1.00, 01/25/99
* @author  Cameron Purdy
*/
public class ClassExpression extends UnaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a ClassExpression.
    *
    * @param tokDot   the operator token
    * @param expr     the type expression
    * @param tokName  the keyword "class")
    */
    public ClassExpression(Token tokDot, TypeExpression expr, Token tokName)
        {
        super(tokDot, expr);

        setStartToken(expr.getStartToken());
        setEndToken(tokName);
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
        Expression expr = getExpression();

        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        setExpression(expr);

        setType(CLZ);
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
        DataType dt = getExpression().getType();

        if (dt.isPrimitive())
            {
            FieldConstant constant;
            switch (dt.getTypeString().charAt(0))
                {
                case 'Z':
                    constant = BOOLEAN_TYPE;
                    break;
                case 'B':
                    constant = BYTE_TYPE;
                    break;
                case 'C':
                    constant = CHAR_TYPE;
                    break;
                case 'S':
                    constant = SHORT_TYPE;
                    break;
                case 'I':
                    constant = INT_TYPE;
                    break;
                case 'J':
                    constant = LONG_TYPE;
                    break;
                case 'F':
                    constant = FLOAT_TYPE;
                    break;
                case 'D':
                    constant = DOUBLE_TYPE;
                    break;
                default:
                    throw new IllegalStateException();
                }
            code.add(new Getstatic(constant));
            }
        else
            {
            MethodConstant constant = ctx.getClassForName(dt);
            code.add(new Invokestatic(constant));
            }

        return fReached;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ClassExpression";

    /**
    * The Java Class class (java/lang/Class).
    */
    private static final DataType CLZ = DataType.getClassType(Class.class);

    /**
    * The signature for the static fields containing Class instances for
    * primitive types.
    */
    private static final SignatureConstant SIG = new SignatureConstant("TYPE", CLZ.getJVMSignature());

    /**
    * The field constant for "boolean.class".
    */
    private static final FieldConstant BOOLEAN_TYPE = new FieldConstant(
            new ClassConstant(Boolean  .class.getName()), SIG);
    /**
    * The field constant for "byte.class".
    */
    private static final FieldConstant BYTE_TYPE    = new FieldConstant(
            new ClassConstant(Byte     .class.getName()), SIG);
    /**
    * The field constant for "char.class".
    */
    private static final FieldConstant CHAR_TYPE    = new FieldConstant(
            new ClassConstant(Character.class.getName()), SIG);
    /**
    * The field constant for "short.class".
    */
    private static final FieldConstant SHORT_TYPE   = new FieldConstant(
            new ClassConstant(Short    .class.getName()), SIG);
    /**
    * The field constant for "int.class".
    */
    private static final FieldConstant INT_TYPE     = new FieldConstant(
            new ClassConstant(Integer  .class.getName()), SIG);
    /**
    * The field constant for "long.class".
    */
    private static final FieldConstant LONG_TYPE    = new FieldConstant(
            new ClassConstant(Long     .class.getName()), SIG);
    /**
    * The field constant for "float.class".
    */
    private static final FieldConstant FLOAT_TYPE   = new FieldConstant(
            new ClassConstant(Float    .class.getName()), SIG);
    /**
    * The field constant for "double.class".
    */
    private static final FieldConstant DOUBLE_TYPE  = new FieldConstant(
            new ClassConstant(Double   .class.getName()), SIG);
    }
