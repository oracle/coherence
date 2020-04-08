/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Op;
import com.tangosol.dev.assembler.OpDeclare;
import com.tangosol.dev.assembler.OpStore;
import com.tangosol.dev.assembler.Dup;
import com.tangosol.dev.assembler.Dup2;
import com.tangosol.dev.assembler.Dup_x1;
import com.tangosol.dev.assembler.Dup2_x1;
import com.tangosol.dev.assembler.Dup_x2;
import com.tangosol.dev.assembler.Dup2_x2;
import com.tangosol.dev.assembler.Ivar;
import com.tangosol.dev.assembler.Lvar;
import com.tangosol.dev.assembler.Fvar;
import com.tangosol.dev.assembler.Dvar;
import com.tangosol.dev.assembler.Avar;
import com.tangosol.dev.assembler.Istore;
import com.tangosol.dev.assembler.Lstore;
import com.tangosol.dev.assembler.Fstore;
import com.tangosol.dev.assembler.Dstore;
import com.tangosol.dev.assembler.Astore;
import com.tangosol.dev.assembler.Bastore;
import com.tangosol.dev.assembler.Castore;
import com.tangosol.dev.assembler.Sastore;
import com.tangosol.dev.assembler.Iastore;
import com.tangosol.dev.assembler.Lastore;
import com.tangosol.dev.assembler.Fastore;
import com.tangosol.dev.assembler.Dastore;
import com.tangosol.dev.assembler.Aastore;
import com.tangosol.dev.assembler.Putfield;
import com.tangosol.dev.assembler.Putstatic;
import com.tangosol.dev.assembler.FieldConstant;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the Assign (=) expression.
*
* There is one very special case of the Assign expression, and that occurs
* during declaration.  Consider the statement:
* <pre><code>
*   int x = 1;
*
*   1)  Since this is a declaration, the variable x is registered.
*   2)  The literal 1 is evaluated.
*   3)  The assignment to x is made.
*
* Note:  Eventual optimization for x=x+expr and x=x-expr should go here
* </code></pre>
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class AssignExpression extends BinaryExpression implements TokenConstants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a AssignExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public AssignExpression(Expression left, Token operator, Expression right)
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
        return precompile(ctx, setUVars, setFVars, mapThrown, errlist, false);
        }

    /**
    * Perform semantic checks, parse tree re-organization, name binding,
    * and optimizations.
    *
    * @param ctx        the compiler context
    * @param setUVars   the set of potentially unassigned variables
    * @param setFVars   the set of potentially assigned final variables
    * @param mapThrown  the set of potentially thrown checked exceptions
    * @param errlist    the error list
    * @param fLeftDone  true if the left side is already pre-compiled
    *
    * @return the resulting language element (typically this)
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Element precompile(Context ctx, DualSet setUVars, DualSet setFVars, Map mapThrown, ErrorList errlist, boolean fLeftDone)
            throws CompilerException
        {
        // get the sub-expressions
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        // the right side is used as a value, as in any other binary
        // expression; the left side must be one of:
        //
        //  - array access expression
        //  - field access expression
        //  - local variable expression
        //
        // the parsing step does not differentiate between unqualified field
        // accessors and local variable names, so the left side (before pre-
        // compilation) could be one of:
        //
        //  - array access expression
        //  - field access expression
        //  - name expression
        //
        // (the name could be "this" or "super", which is illegal here)

        // pre-compile the left sub-expression
        if (!fLeftDone)
            {
            left.checkAssignable(errlist);
            left = (Expression) left.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
            }

        // pre-compile the right sub-expression
        right = (Expression) right.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // check if the type is assignment compatible with the variable
        if (right.checkAssignable(ctx, left.getType(), errlist))
            {
            right = right.convertAssignable(ctx, left.getType());
            }

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

        // type is determined by the left expression
        setType(left.getType());

        // update definite assignment information
        if (left instanceof VariableExpression)
            {
            Variable var = ((VariableExpression) left).getVariable();
            setUVars.remove(var);
            if (var.isFinal())
                {
                setFVars.add(var);
                }
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
        // assignment compilation for "left=right"
        //
        // compilation for variable assignment:
        //  [right]
        //  dup?        // dup or dup2, omitted if value discarded
        //  ?store
        //
        // compilation for array assignment, left being "array[index]":
        //  [array]
        //  [index]
        //  [right]
        //  dup?_x2     // dup_x2 or dup2_x2, omitted if value discarded
        //  ?astore
        //
        // compilation for static assignment, left being "ref.ident":
        //  [right]
        //  dup?        // dup or dup2, omitted if value discarded
        //  putstatic ?
        //
        // compilation for field assignment, left being "ref.ident":
        //  [ref]
        //  [right]
        //  dup?_x1     // dup_x1 or dup2_x1, omitted if value discarded
        //  putfield ?

        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        if (left instanceof VariableExpression)
            {
            Variable var    = ((VariableExpression) left).getVariable();
            char     chType = var.getType().getTypeString().charAt(0);

            // compile the right side
            right.compile(ctx, code, fReached, errlist);

            // if the value is not discarded, dup it
            if (!isDiscarded())
                {
                switch (chType)
                    {
                    default:
                        code.add(new Dup());
                        break;
                    case 'J':
                    case 'D':
                        code.add(new Dup2());
                        break;
                    }
                }

            // store in variable
            OpDeclare opVar   = var.getOp();
            OpStore   opStore = null;
            switch (chType)
                {
                case 'Z':
                case 'B':
                case 'C':
                case 'S':
                case 'I':
                    opStore = new Istore((Ivar) opVar);
                    break;

                case 'J':
                    opStore = new Lstore((Lvar) opVar);
                    break;

                case 'F':
                    opStore = new Fstore((Fvar) opVar);
                    break;

                case 'D':
                    opStore = new Dstore((Dvar) opVar);
                    break;

                case 'N':
                case 'L':
                case 'R':
                case '[':
                    opStore = new Astore((Avar) opVar);
                    break;

                default:
                    throw new IllegalStateException();
                }
            code.add(opStore);
            }
        else if (left instanceof ArrayAccessExpression)
            {
            ArrayAccessExpression exprElement = (ArrayAccessExpression) left;
            char chType = exprElement.getType().getTypeString().charAt(0);

            // compile array reference and element index
            exprElement.getArray().compile(ctx, code, fReached, errlist);
            exprElement.getIndex().compile(ctx, code, fReached, errlist);

            // compile expression to store
            right.compile(ctx, code, fReached, errlist);

            // if the value is not discarded, dup it and place it below the
            // array reference in the stack
            if (!isDiscarded())
                {
                switch (chType)
                    {
                    default:
                        code.add(new Dup_x2());
                        break;
                    case 'J':
                    case 'D':
                        code.add(new Dup2_x2());
                        break;
                    }
                }

            // array store
            Op opStore = null;
            switch (chType)
                {
                case 'Z':
                case 'B':
                    opStore = new Bastore();
                    break;

                case 'C':
                    opStore = new Castore();
                    break;

                case 'S':
                    opStore = new Sastore();
                    break;

                case 'I':
                    opStore = new Iastore();
                    break;

                case 'J':
                    opStore = new Lastore();
                    break;

                case 'F':
                    opStore = new Fastore();
                    break;

                case 'D':
                    opStore = new Dastore();
                    break;

                case 'N':
                case 'L':
                case 'R':
                case '[':
                    opStore = new Aastore();
                    break;

                default:
                    throw new IllegalStateException();
                }
            code.add(opStore);
            }
        else if (left instanceof FieldAccessExpression)
            {
            FieldAccessExpression exprField = (FieldAccessExpression) left;
            boolean fStatic = exprField.isStatic();
            char    chType  = exprField.getType().getTypeString().charAt(0);

            if (!fStatic)
                {
                // compile reference expression
                exprField.getExpression().compile(ctx, code, fReached, errlist);
                }


            // compile expression to store
            right.compile(ctx, code, fReached, errlist);

            // if the value is not discarded, dup it and place it below the
            // array reference in the stack
            if (!isDiscarded())
                {
                switch (chType)
                    {
                    default:
                        code.add(fStatic ? (Op) new Dup() : new Dup_x1());
                        break;
                    case 'J':
                    case 'D':
                        code.add(fStatic ? (Op) new Dup2() : new Dup2_x1());
                        break;
                    }
                }

            // store field
            FieldConstant field = exprField.getFieldConstant();
            code.add(fStatic ? (Op) new Putstatic(field) : new Putfield(field));
            }
        else
            {
            throw new IllegalStateException();
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
        // assignment expressions are never constant
        return false;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "AssignExpression";
    }
