/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.OpDeclare;
import com.tangosol.dev.assembler.OpLoad;
import com.tangosol.dev.assembler.Ivar;
import com.tangosol.dev.assembler.Iload;
import com.tangosol.dev.assembler.Lvar;
import com.tangosol.dev.assembler.Lload;
import com.tangosol.dev.assembler.Fvar;
import com.tangosol.dev.assembler.Fload;
import com.tangosol.dev.assembler.Dvar;
import com.tangosol.dev.assembler.Dload;
import com.tangosol.dev.assembler.Avar;
import com.tangosol.dev.assembler.Aload;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* A local variable.
*
* @version 1.00, 12/07/98
* @author  Cameron Purdy
*/
public class VariableExpression extends Expression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a VariableExpression.
    *
    * @param operator  the operator token
    * @param expr      the sub-expression
    */
    public VariableExpression(Block block, Token name)
        {
        super(block, name);
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
        Block    block = getBlock();
        String   sName = getStartToken().getText();
        Variable var   = block.getVariable(sName);

        if (isAssignee())
            {
            // assignment to a final can only occur if the variable
            // is definitely unassigned
            if (var.isFinal())
                {
                if (setFVars.contains(var))
                    {
                    logError(ERROR, FINAL_REASSIGN, new String[] {var.getName()}, errlist);
                    }
                }
            }
        else if (setUVars.contains(var))
            {
            // variable is potentially unassigned
            logError(ERROR, VAR_UNASSIGNED, new String[] {sName}, errlist);
            }

        setType(var.getType());

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
        if (!ctx.isDebug() && isConstant() && !(getValue() instanceof Object[]))
            {
            return super.compile(ctx, code, fReached, errlist);
            }

        // compilation for variable expression:
        //  ?load

        // load variable
        Variable  var    = getVariable();
        OpDeclare opVar  = var.getOp();
        OpLoad    opLoad = null;
        switch (getType().getTypeString().charAt(0))
            {
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                opLoad = new Iload((Ivar) opVar);
                break;

            case 'J':
                opLoad = new Lload((Lvar) opVar);
                break;

            case 'F':
                opLoad = new Fload((Fvar) opVar);
                break;

            case 'D':
                opLoad = new Dload((Dvar) opVar);
                break;

            case 'L':
            case 'R':
            case '[':
                opLoad = new Aload((Avar) opVar);
                break;

            default:
                throw new IllegalStateException();
            }
        code.add(opLoad);

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
        return getVariable().isConstant();
        }

    /**
    * Determine the constant value of the expression.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        return getVariable().getValue();
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
        setAssignee(true);
        return true;
        }


    // ----- accessors

    /**
    * Determine the variable name.
    *
    * @return the name
    */
    public String getName()
        {
        return getStartToken().getText();
        }

    /**
    * Determine the variable.
    *
    * @return the variable
    */
    public Variable getVariable()
        {
        return getBlock().getVariable(getName());
        }

    /**
    * Determine if this variable expression is an assignee.  In other words,
    * is this expression being used on the left-hand-side of an assignment.
    *
    * @return true if this variable expression is being assigned to
    */
    public boolean isAssignee()
        {
        return fAssignee;
        }

    /**
    * Specify that this variable expression is an assignee.
    *
    * @param fAssignee  if this variable expression is being assigned to
    */
    protected void setAssignee(boolean fAssignee)
        {
        this.fAssignee = fAssignee;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "VariableExpression";

    /**
    * If this variable expression is used as a "left-hand-side" of an
    * assignment operation.
    */
    private boolean fAssignee;
    }
