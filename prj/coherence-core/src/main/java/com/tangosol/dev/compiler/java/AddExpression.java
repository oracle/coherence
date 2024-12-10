/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.ClassConstant;
import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.Dadd;
import com.tangosol.dev.assembler.Dup;
import com.tangosol.dev.assembler.Fadd;
import com.tangosol.dev.assembler.Iadd;
import com.tangosol.dev.assembler.Invokespecial;
import com.tangosol.dev.assembler.Invokestatic;
import com.tangosol.dev.assembler.Invokevirtual;
import com.tangosol.dev.assembler.Ladd;
import com.tangosol.dev.assembler.MethodConstant;
import com.tangosol.dev.assembler.New;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.component.DataType;
import com.tangosol.util.ErrorList;

import java.util.Map;


/**
* Implements the addition operator.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class AddExpression extends AdditiveExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a AddExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public AddExpression(Expression left, Token operator, Expression right)
        {
        super(left, operator, right);
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
        // get the sub-expressions
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        // pre-compile the sub-expressions
        left  = (Expression) left .precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        right = (Expression) right.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        if (left.getType() != UNKNOWN && right.getType() != UNKNOWN)
            {
            // if one sub-expression is String, then use String concatenation
            if (left.getType() == STRING || right.getType() == STRING)
                {
                setType(STRING);
                }
            // otherwise both sub-expressions must be numeric
            else if (left.checkNumeric(errlist) & right.checkNumeric(errlist))
                {
                // binary numeric promotion
                left  = left .promoteNumeric(right);
                right = right.promoteNumeric(left );

                // result type is the type of left and right (now identical)
                setType(left.getType());
                }
            }

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

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
        if (!ctx.isDebug() && isConstant())
            {
            return super.compile(ctx, code, fReached, errlist);
            }

        if (getType() == STRING)
            {
            compileConcatenation(ctx, code, fReached, errlist);
            code.add(new Invokevirtual(STRINGBUFFER_TOSTRING));
            return fReached;
            }

        // Code generation for the operator '+' is as follows:
        //
        //              [left]
        //              [right]
        //              ?add

        getLeftExpression ().compile(ctx, code, fReached, errlist);
        getRightExpression().compile(ctx, code, fReached, errlist);

        switch (getType().getTypeString().charAt(0))
            {
            case 'I':
                code.add(new Iadd());
                break;

            case 'J':
                code.add(new Ladd());
                break;

            case 'F':
                code.add(new Fadd());
                break;

            case 'D':
                code.add(new Dadd());
                break;

            default:
                throw new IllegalStateException();
            }

        // normal completion possible if reachable
        return fReached;
        }

    /**
    * Build code for string concatenation using the StringBuffer class.
    *
    *  The first part of the string expression
    *  (i.e. the left-most add with string result) must:
    *
    *   new java/lang/StringBuffer
    *   dup
    *   (String) [left]
    *   invokespecial java/lang/StringBuffer.<init>(Ljava/lang/String;)V
    *
    *  ... thus leaving an instance of StringBuffer on the stack.
    *
    *  As control returns back up the chain of add expressions, they:
    *
    *   [right]
    *   invokevirtual java/lang/StringBuffer.append(T)Ljava/lang/StringBuffer;
    *
    *  ... where T is the type of the [right] expression, one of:
    *
    *   boolean
    *   char
    *   int
    *   long
    *   float
    *   double
    *   reference
    *
    *  This compileConcatenation method always leaves an instance of
    *  StringBuffer on the stack.
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
    protected boolean compileConcatenation(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        // if the left side is a string concatenation
        if (left instanceof AddExpression && left.getType() == STRING
                // and its value cannot be determined at compile time because
                // either the debug flag is set or it is not a constant
                && (ctx.isDebug() || !left.isConstant()))
            {
            // before this call, stack is considered empty
            ((AddExpression) left).compileConcatenation(ctx, code, fReached, errlist);
            }
        else
            {
            // this creates the StringBuffer containing the start of the
            // concatenated String expression
            code.add(new New(CLZ_STRINGBUFFER));
            code.add(new Dup());
            left.compile(ctx, code, fReached, errlist);
            // non-null constant string values do not need conversion
            if (ctx.isDebug()
                    || left.getType() != STRING
                    || !left.isConstant()
                    || left.getValue() == null)
                {
                code.add(new Invokestatic(left.getConvertMethod()));
                }
            code.add(new Invokespecial(INIT_STRINGBUFFER));
            }

        // at this point, the stack has a StringBuffer instance on it
        right.compile(ctx, code, fReached, errlist);
        code.add(new Invokevirtual(right.getAppendMethod()));

        // at this point, the stack still has a StringBuffer instance on it
        return fReached;
        }


    // ----- Expression methods ---------------------------------------------

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        boolean fVal;

        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        Object oLeft  = left .getValue();
        Object oRight = right.getValue();

        switch (getType().getTypeString().charAt(0))
            {
            case 'I':
                return ((Number) oLeft).intValue() + ((Number) oRight).intValue();

            case 'J':
                return ((Number) oLeft).longValue() + ((Number) oRight).longValue();

            case 'F':
                return ((Number) oLeft).floatValue() + ((Number) oRight).floatValue();

            case 'D':
                return ((Number) oLeft).doubleValue() + ((Number) oRight).doubleValue();

            case 'L':
                return left.getStringValue() + right.getStringValue();

            default:
                throw new IllegalStateException();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "AddExpression";

    /**
    * String data type.
    */
    private static final DataType STRING = DataType.STRING;

    /**
    * Unknown data type.
    */
    private static final DataType UNKNOWN = DataType.UNKNOWN;

    /**
    * The internal name of the StringBuilder class.
    */
    private static final String STRINGBUILDER = "java/lang/StringBuilder";

    /**
    * The StringBuffer class, used for string concatenation.
    */
    private static final ClassConstant CLZ_STRINGBUFFER = new ClassConstant(STRINGBUILDER);

    /**
    * The StringBuffer String-based constructor.
    */
    private static final MethodConstant INIT_STRINGBUFFER = new MethodConstant(STRINGBUILDER, Constants.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V");

    /**
    * The StringBuffer to String conversion.
    */
    private static final MethodConstant STRINGBUFFER_TOSTRING = new MethodConstant(STRINGBUILDER, "toString", "()Ljava/lang/String;");
    }
