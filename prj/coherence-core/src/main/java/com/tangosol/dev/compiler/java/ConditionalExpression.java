/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Goto;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Ifeq;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;


/**
* Implements the Conditional (? :) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class ConditionalExpression extends Expression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a ConditionalExpression.
    *
    * @param test       the test expression
    * @param operator   the conditional operator
    * @param iftrue     the true expression
    * @param separator  the colon separating the true expression from the false
    * @param iffalse    the false expression
    */
    public ConditionalExpression(Expression test, Token operator, Expression iftrue, Token separator, Expression iffalse)
        {
        super(test.getBlock(), test.getStartToken(), iffalse.getEndToken());

        this.test      = test;
        this.operator  = operator;
        this.iftrue    = iftrue;
        this.separator = separator;
        this.iffalse   = iffalse;
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
        Expression test    = this.test;
        Expression iftrue  = this.iftrue;
        Expression iffalse = this.iffalse;

        // JLS 16.1.11 The Boolean Operator ?:
        // Suppose that b and c are boolean-valued expressions.
        //  1)  V is definitely assigned after a ? b : c when true iff both
        //      of the following are true:
        //      1)  V is definitely assigned before b or V is definitely
        //          assigned after b when true.
        //      2)  V is definitely assigned before c or V is definitely
        //          assigned after c when true.
        //  2)  V is definitely assigned after a ? b : c when false iff both
        //      of the following are true:
        //      1)  V is definitely assigned before b or V is definitely
        //          assigned after b when false.
        //      2)  V is definitely assigned before c or V is definitely
        //          assigned after c when false.
        //  3)  V is definitely assigned before a iff V is definitely
        //      assigned before a ? b : c.
        //  4)  V is definitely assigned before b iff V is definitely
        //      assigned after a when true.
        //  5)  V is definitely assigned before c iff V is definitely
        //      assigned after a when false.
        //
        // JLS 16.1.12 The Conditional Operator ?:
        // Suppose that b and c are expressions that are not boolean-valued.
        //  1)  V is definitely assigned after a ? b : c iff both of the
        //      following are true:
        //      1)  V is definitely assigned after b.
        //      2)  V is definitely assigned after c.
        //  2)  V is definitely assigned before a iff V is definitely
        //      assigned before a ? b : c.
        //  3)  V is definitely assigned before b iff V is definitely
        //      assigned after a when true.
        //  4)  V is definitely assigned before c iff V is definitely
        //      assigned after a when false.
        //
        // Translation for definite assignment:
        //  1)  Precompile the test
        //  2)  Precompile the if-true expression using the when-true
        //      variables from the test precompilation
        //  3)  Precompile the if-false expression using the when-false
        //      variables from the test precompilation
        //  4)  Intersect the assigned variables from the if-true and
        //      if-false expressions; those are the variables assigned by
        //      the expression (in addition to those assigned by the test)
        //  5)  For if-true and if-false expressions that are boolean typed,
        //      determine the results both for "when true" and "when false"
        //
        // Translation for definite un-assignment (final vars):
        //  1)  Precompile the test
        //  2)  Precompile the if-true expression using the when-true
        //      variables from the test precompilation
        //  3)  Precompile the if-false expression using the when-false
        //      variables from the test precompilation
        //  4)  Union the potentially assigned variables from the if-true
        //      and if-false expressions; those are the variables potentially
        //      assigned by the expression (in addition to those potentially
        //      assigned by the test)
        //  5)  For if-true and if-false expressions that are boolean typed,
        //      determine the results both for "when true" and "when false"

        // pre-compile the test
        this.test = test = (Expression) test.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        test.checkBoolean(errlist);

        // pre-compile the if-true sub-expression
        DualSet setUTrue = new DualSet(setUVars.getTrueSet());
        DualSet setFTrue = new DualSet(setFVars.getTrueSet());
        this.iftrue = iftrue = (Expression) iftrue.precompile(ctx, setUTrue, setFVars, mapThrown, errlist);

        // pre-compile the if-false sub-expression
        DualSet setUFalse = new DualSet(setUVars.getFalseSet());
        DualSet setFFalse = new DualSet(setFVars.getFalseSet());
        this.iffalse = iffalse = (Expression) iffalse.precompile(ctx, setUTrue, setFVars, mapThrown, errlist);

        // determine type of expression
        DataType dtTrue   = iftrue .getType();
        DataType dtFalse  = iffalse.getType();
        DataType dtResult = UNKNOWN;

        // sub-expressions may both be boolean
        if (dtTrue == BOOLEAN)
            {
            // definite assignment
            if (setUTrue.isModified() && setUFalse.isModified())
                {
                Set setLeftTrue   = setUTrue .getTrueSet ().getRemoved();
                Set setLeftFalse  = setUTrue .getFalseSet().getRemoved();
                Set setRightTrue  = setUFalse.getTrueSet ().getRemoved();
                Set setRightFalse = setUFalse.getFalseSet().getRemoved();

                Set setAssigned = new HashSet(setLeftTrue);
                setAssigned.retainAll(setRightTrue);
                setUVars.getTrueSet().removeAll(setAssigned);

                setAssigned = new HashSet(setLeftFalse);
                setAssigned.retainAll(setRightFalse);
                setUVars.getFalseSet().removeAll(setAssigned);
                }

            // definite un-assignment (potential assignment)
            if (setFTrue.isModified() && setFFalse.isModified())
                {
                Set setLeftTrue   = setFTrue .getTrueSet ().getAdded();
                Set setLeftFalse  = setFTrue .getFalseSet().getAdded();
                Set setRightTrue  = setFFalse.getTrueSet ().getAdded();
                Set setRightFalse = setFFalse.getFalseSet().getAdded();

                Set setAssigned = new HashSet(setLeftTrue);
                setAssigned.addAll(setRightTrue);
                setUVars.getTrueSet().addAll(setAssigned);

                setAssigned = new HashSet(setLeftFalse);
                setAssigned.addAll(setRightFalse);
                setUVars.getFalseSet().addAll(setAssigned);
                }

            iffalse.checkBoolean(errlist);
            dtResult = BOOLEAN;
            }
        else
            {
            // definite assignment/definite unassignment:
            // when true/when false processing is only for boolean types;
            // just merge/commit the changes (which in effect intersects the
            // variables definitely assigned by if-true and if-false)
            setUTrue .resolve();
            setUFalse.resolve();
            setFTrue .resolve();
            setFFalse.resolve();

            // sub-expressions may both be reference types
            if (dtTrue.isReference())
                {
                final DataType NULL   = this.NULL;

                // JLS 15.2 Conditional Operator ?:
                //  - If the second and third operands have the same type
                //    (which may be the null type), then that is the type
                //    of the conditional expression.
                //  - If one of the second and third operands is of the
                //    null type and the type of the other is a reference
                //    type, then the type of the conditional expression
                //    is that reference type.
                //  - If the second and third operands are of different
                //    reference types, then it must be possible to
                //    convert one of the types to the other type (call
                //    this latter type T) by assignment conversion
                //    (5.2); the type of the conditional expression is
                //    T. It is a compile-time error if neither type is
                //    assignment compatible with the other type.
                if (dtTrue == dtFalse || !iffalse.checkReference(errlist))
                    {
                    dtResult = dtTrue;
                    }
                else if (dtTrue == NULL)
                    {
                    dtResult = dtFalse;
                    }
                else if (dtFalse == NULL)
                    {
                    dtResult = dtTrue;
                    }
                else if (iffalse.checkAssignable(ctx, dtTrue, null))
                    {
                    iffalse  = iffalse.convertAssignable(ctx, dtTrue);
                    dtResult = dtTrue;
                    }
                else if (iftrue.checkAssignable(ctx, dtFalse, errlist))
                    {
                    iftrue   = iftrue.convertAssignable(ctx, dtFalse);
                    dtResult = dtFalse;
                    }
                else
                    {
                    // error
                    dtResult = NULL;
                    }
                }
            // otherwise they must both be numeric
            else if (iftrue.checkNumeric(errlist) & iffalse.checkNumeric(errlist))
                {
                final DataType BYTE  = this.BYTE;
                final DataType CHAR  = this.CHAR;
                final DataType SHORT = this.SHORT;
                final DataType INT   = this.INT;

                // JLS 15.2 Conditional Operator ?:
                //  - If the second and third operands have the same type
                //    (which may be the null type), then that is the type
                //    of the conditional expression.
                //  - If one of the operands is of type byte and the other
                //    is of type short, then the type of the conditional
                //    expression is short.
                //  - If one of the operands is of type T where T is byte,
                //    short, or char, and the other operand is a constant
                //    expression of type int whose value is representable
                //    in type T, then the type of the conditional expression
                //    is T.
                //  - Otherwise, binary numeric promotion (5.6.2) is applied
                //    to the operand types, and the type of the conditional
                //    expression is the promoted type of the second and third
                //    operands.

                if (dtTrue == dtFalse)
                    {
                    dtResult = dtTrue;
                    }
                else if ((dtTrue  == BYTE || dtTrue  == SHORT)
                      && (dtFalse == BYTE || dtFalse == SHORT))
                    {
                    dtResult = SHORT;
                    }
                else if ((dtTrue == BYTE || dtTrue == SHORT || dtTrue == CHAR)
                        && dtFalse == INT && iffalse.isConstant()
                        && iffalse.checkAssignable(ctx, dtTrue, null))
                    {
                    iffalse  = iffalse.convertAssignable(ctx, dtTrue);
                    dtResult = dtTrue;
                    }
                else if ((dtFalse == BYTE || dtFalse == SHORT || dtFalse == CHAR)
                        && dtTrue == INT && iftrue.isConstant()
                        && iftrue.checkAssignable(ctx, dtFalse, null))
                    {
                    iftrue   = iftrue.convertAssignable(ctx, dtFalse);
                    dtResult = dtFalse;
                    }
                else
                    {
                    // binary numeric promotion
                    iftrue   = iftrue .promoteNumeric(iffalse);
                    iffalse  = iffalse.promoteNumeric(iftrue );
                    dtResult = iftrue.getType();
                    }
                }
            }

        // store expressions
        this.test    = test;
        this.iftrue  = iftrue;
        this.iffalse = iffalse;

        // store type
        setType(dtResult);

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
            // Expression has compiler implementation for constant values
            return super.compile(ctx, code, fReached, errlist);
            }

        // get the sub-expressions
        Expression test    = this.test;
        Expression iftrue  = this.iftrue;
        Expression iffalse = this.iffalse;

        // compilation of "<test> ? <iftrue> : <iffalse>":
        //
        //          [expr]
        //          ifeq    else
        //          [iftrue]
        //          goto    exit
        //  else:
        //          [iffalse]
        //  exit:

        if (test instanceof BooleanExpression || !ctx.isDebug() && test.isConstant())
            {
            Expression result = ((Boolean) test.getValue()).booleanValue() ? iftrue : iffalse;
            result.compile(ctx, code, fReached, errlist);
            }
        else
            {
            Label lblElse = new Label();
            Label lblExit = new Label();

            test.compile(ctx, code, fReached, errlist);
            code.add(new Ifeq(lblElse));
            iftrue.compile(ctx, code, fReached, errlist);
            code.add(new Goto(lblExit));
            code.add(lblElse);
            iffalse.compile(ctx, code, fReached, errlist);
            code.add(lblExit);
            }

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
        // constant iff test is constant boolean and result is constant
        Expression test = this.test;
        if (test.getType() == BOOLEAN && test.isConstant())
            {
            return (((Boolean) test.getValue()).booleanValue() ? iftrue : iffalse).isConstant();
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
        return (((Boolean) test.getValue()).booleanValue() ? iftrue : iffalse).getValue();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the conditional test.
    *
    * @return the conditional test
    */
    public Expression getTest()
        {
        return test;
        }

    /**
    * Set the conditional test.
    *
    * @param test  the new conditional test
    */
    protected void setTest(Expression test)
        {
        this.test = test;
        }

    /**
    * Get the expression which is evaluated if the test evaluates to true.
    *
    * @return the "true" expression
    */
    public Expression getTrueExpression()
        {
        return iftrue;
        }

    /**
    * Set the expression which is evaluated if the test evaluates to true.
    *
    * @param iftrue  the new "true" expression
    */
    public void setTrueExpression(Expression iftrue)
        {
        this.iftrue = iftrue;
        }

    /**
    * Get the expression which is evaluated if the test evaluates to false.
    *
    * @return the "false" expression
    */
    public Expression getFalseExpression()
        {
        return iffalse;
        }

    /**
    * Set the expression which is evaluated if the test evaluates to false.
    *
    * @param iffalse  the new "false" expression
    */
    public void setFalseExpression(Expression iffalse)
        {
        this.iffalse = iffalse;
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

        out(sIndent + "  test:");
        test.print(sIndent + "    ");

        out(sIndent + "  value if true:");
        iftrue.print(sIndent + "    ");

        out(sIndent + "  value if false:");
        iffalse.print(sIndent + "    ");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ConditionalExpression";

    /**
    * Unknown data type (caused by compiler error).
    */
    private static final DataType UNKNOWN = DataType.UNKNOWN;

    /**
    * Null data type.
    */
    private static final DataType NULL = DataType.NULL;

    /**
    * Boolean data type.
    */
    private static final DataType BOOLEAN = DataType.BOOLEAN;

    /**
    * Byte data type.
    */
    private static final DataType BYTE = DataType.BYTE;

    /**
    * Char data type.
    */
    private static final DataType CHAR = DataType.CHAR;

    /**
    * Short data type.
    */
    private static final DataType SHORT = DataType.SHORT;

    /**
    * Int data type.
    */
    private static final DataType INT = DataType.INT;

    /**
    * The test expression.
    */
    private Expression test;

    /**
    * The conditional operator.
    */
    private Token operator;

    /**
    * The true expression.
    */
    private Expression iftrue;

    /**
    * The colon separator.
    */
    private Token separator;

    /**
    * The false expression.
    */
    private Expression iffalse;
    }
