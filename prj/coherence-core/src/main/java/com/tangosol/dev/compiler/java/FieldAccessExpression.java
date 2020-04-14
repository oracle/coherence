/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.Arraylength;
import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.FieldConstant;
import com.tangosol.dev.assembler.Getfield;
import com.tangosol.dev.assembler.Getstatic;
import com.tangosol.dev.assembler.Pop;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.compiler.FieldInfo;
import com.tangosol.dev.compiler.TypeInfo;
import com.tangosol.dev.component.DataType;
import com.tangosol.util.ErrorList;

import java.util.Map;


/**
* The field access (.) expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class FieldAccessExpression extends UnaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a FieldAccessExpression.
    *
    * @param operator  the operator token
    * @param expr      the array expression
    * @param tokName   the field name (or the keyword "class")
    */
    public FieldAccessExpression(Token operator, Expression expr, Token tokName)
        {
        super(operator, expr);

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
        Expression expr  = getExpression();
        String     sName = getName();

        // determine:
        //  1)  field constant
        //  2)  static or instance field
        //  3)  whether to compile the left hand side (not necessary if the
        //      field is static and the left hand side does nothing)

        // "super" means the "this" reference cast to the type of the super
        // class, but since there is no field hiding with components, it is
        // treated the same as "this"
        if (expr instanceof SuperExpression)
            {
            // tell super that it is allowed to be pre-compiled here
            ((SuperExpression) expr).allowSuper();
            }

        // pre-compile the expression
        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        setExpression(expr);

        boolean fRef;
        if (expr instanceof TypeExpression)
            {
            fRef = false;
            }
        else if (expr instanceof SuperExpression)
            {
            throw new IllegalStateException();
            }
        else
            {
            expr.checkReference(false, errlist);
            fRef = true;
            }

        // type of the reference
        DataType  dtRef = expr.getType();
        if (dtRef == UNKNOWN)
            {
            return this;
            }

        // special handling for array types
        if (dtRef.isArray())
            {
            if (sName.equals("length"))
                {
                // arraylength op

                // must not be assigned to (since it ain't no field)
                if (isAssignee())
                    {
                    logError(ERROR, FINAL_REASSIGN, new String[] {sName}, errlist);
                    }

                setType(INT);
                return this;
                }

            dtRef = OBJECT;
            }

        // get reference type information
        TypeInfo  type  = ctx.getTypeInfo(dtRef);
        if (type == null)
            {
            String sPkg  = "";
            String sType = expr.getType().toString();
            int    ofDot = sType.lastIndexOf('.');
            if (ofDot >= 0)
                {
                sPkg  = sType.substring(0, ofDot);
                sType = sType.substring(ofDot + 1);
                }

            expr.logError(ERROR, TYPE_NOT_FOUND, new String[]
                    {sType, sPkg}, errlist);
            return this;
            }

        // the referenced field
        FieldInfo field = type.getFieldInfo(sName);
        if (field == null)
            {
            // no such field
            DataType dt = type.getDataType();
            logError(ERROR, FIELD_NOT_FOUND, new String[]
                    {sName, expr.getType().toString()}, errlist);
            return this;
            }

        // check if instance field is being accessed without a reference
        if (!field.isStatic() && !fRef)
            {
            logError(ERROR, REF_REQUIRED, new String[] {sName}, errlist);
            }

        // verify field is settable if necessary
        if (field.isFinal() && isAssignee())
            {
            // final fields cannot be set
            logError(ERROR, FINAL_REASSIGN, new String[] {sName}, errlist);
            }

        // verify field is accessible
        if (field.isViaAccessor() || !field.isAccessible())
            {
            logError(ERROR, FIELD_NO_ACCESS, new String[]
                    {sName, expr.getType().toString()}, errlist);
            }

        // register compile-time dependency on the field
        addDependency(field, false);

        setType(field.getDataType());
        m_field = field;
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
        Expression expr     = getExpression();
        boolean    fStatic  = isStatic();
        boolean    fOmitRef = false;

        if (fStatic)
            {
            if (expr.isConstant()
                    || expr instanceof TypeExpression
                    || expr instanceof VariableExpression
                    || expr instanceof ThisExpression)
                {
                fOmitRef = true;
                }
            }

        if (!fOmitRef)
            {
            expr.compile(ctx, code, fReached, errlist);

            if (fStatic)
                {
                code.add(new Pop());
                }
            }

        FieldInfo field = m_field;
        if (field == null)
            {
            // assertion:  must be length "prop" of array
            if (!getName().equals("length"))
                {
                throw new IllegalStateException();
                }

            code.add(new Arraylength());
            }
        else if (field.isInlined() || !ctx.isDebug() && field.isInlineable())
            {
            // constant
            super.compile(ctx, code, fReached, errlist);
            }
        else
            {
            FieldConstant constant = (FieldConstant) field.getConstant();
            if (fStatic)
                {
                code.add(new Getstatic(constant));
                }
            else
                {
                code.add(new Getfield(constant));
                }

            // register run-time dependency on the field
            addDependency(field, true);
            }

        return fReached;
        }

    /**
    * Register a dependency.
    *
    * @param field
    * @param fRuntime  true for a runtime dependency, false for a compile dependency
    */
    protected void addDependency(FieldInfo field, boolean fRuntime)
            throws CompilerException
        {
        Token tokField = getEndToken();
        field.addDependency(fRuntime, tokField.getLine(), tokField.getOffset(),
                tokField.getLine(), tokField.getOffset() + tokField.getLength());
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine if the expression has a constant value.
    * Note:  This question is only valid after the expression is pre-compiled.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        FieldInfo field = m_field;
        if (field != null)
            {
            return field.isInlineable();
            }

        if (getType() == INT && getName().equals("length"))
            {
            // array length calculable if array is constant
            Expression expr = getExpression();
            return expr.isConstant() && expr.getValue() != null;
            }

        return false;
        }

    /**
    * Determine the constant value of the expression.
    * Note:  This question is only valid in the compile step.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        FieldInfo field = m_field;
        if (field == null)
            {
            // array length
            return Integer.valueOf(((Object[]) getExpression().getValue()).length);
            }

        Object value = field.getValue();

        // in Java, char is a numeric type, but Character does not inherit
        // from Number, which the compiler assumes all numeric constant
        // values are stored as, so convert Characters to Integers
        if (value instanceof Character)
            {
            value = Integer.valueOf(((Character) value).charValue());
            }

        return value;
        }

    /**
    * Get the field name.
    *
    * @return the field name
    */
    public String getName()
        {
        return getEndToken().getText();
        }

    /**
    * Determine if the field is static.
    *
    * @return true if the field is static
    */
    public boolean isStatic()
        {
        FieldInfo field = m_field;
        return field != null && field.isStatic();
        }

    /**
    * Get the field constant.  Valid only after pre-compilation
    *
    * @return the field constant for the field access expression
    */
    public FieldConstant getFieldConstant()
        {
        FieldInfo field = m_field;
        return field == null ? null : (FieldConstant) m_field.getConstant();
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

    /**
    * Determine if this variable expression is an assignee.  In other words,
    * is this expression being used on the left-hand-side of an assignment.
    *
    * @return true if this variable expression is being assigned to
    */
    public boolean isAssignee()
        {
        return m_fAssignee;
        }

    /**
    * Specify that this variable expression is an assignee.
    *
    * @param fAssignee  if this variable expression is being assigned to
    */
    protected void setAssignee(boolean fAssignee)
        {
        m_fAssignee = fAssignee;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the element information as a string.
    *
    * @return a human-readable description of the element
    */
    public String toString()
        {
        return super.toString() + " field=" + getName();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "FieldAccessExpression";

    /**
    * Unknown data type.
    */
    private static final DataType UNKNOWN = DataType.UNKNOWN;

    /**
    * Object data type (java/lang/Object).
    */
    private static final DataType OBJECT = DataType.OBJECT;

    /**
    * Int data type.
    */
    private static final DataType INT = DataType.INT;

    /**
    * If this name expression is used as a "left-hand-side" of an assignment
    * operation.
    */
    private boolean m_fAssignee;

    /**
    * The referred to field.
    */
    private FieldInfo m_field;

    /**
    * If the sub-expression of this expression is optimized out.
    */
    private boolean m_fOmitRef;
    }
