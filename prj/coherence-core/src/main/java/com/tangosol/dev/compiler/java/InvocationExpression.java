/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.FieldConstant;
import com.tangosol.dev.assembler.MethodConstant;
import com.tangosol.dev.assembler.InterfaceConstant;
import com.tangosol.dev.assembler.Op;
import com.tangosol.dev.assembler.Pop;
import com.tangosol.dev.assembler.Pop2;
import com.tangosol.dev.assembler.Baload;
import com.tangosol.dev.assembler.Caload;
import com.tangosol.dev.assembler.Saload;
import com.tangosol.dev.assembler.Iaload;
import com.tangosol.dev.assembler.Laload;
import com.tangosol.dev.assembler.Faload;
import com.tangosol.dev.assembler.Daload;
import com.tangosol.dev.assembler.Aaload;
import com.tangosol.dev.assembler.Bastore;
import com.tangosol.dev.assembler.Castore;
import com.tangosol.dev.assembler.Sastore;
import com.tangosol.dev.assembler.Iastore;
import com.tangosol.dev.assembler.Lastore;
import com.tangosol.dev.assembler.Fastore;
import com.tangosol.dev.assembler.Dastore;
import com.tangosol.dev.assembler.Aastore;
import com.tangosol.dev.assembler.Getfield;
import com.tangosol.dev.assembler.Getstatic;
import com.tangosol.dev.assembler.Putfield;
import com.tangosol.dev.assembler.Putstatic;
import com.tangosol.dev.assembler.Invokeinterface;
import com.tangosol.dev.assembler.Invokestatic;
import com.tangosol.dev.assembler.Invokespecial;
import com.tangosol.dev.assembler.Invokevirtual;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.compiler.TypeInfo;
import com.tangosol.dev.compiler.FieldInfo;
import com.tangosol.dev.compiler.MethodInfo;

import com.tangosol.dev.component.Behavior;
import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.ChainedEnumerator;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Hashtable;


/**
* Implements the method invocation () expression.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class InvocationExpression extends UnaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a InvocationExpression.
    *
    * @param expr      the type/reference that the method is invoked against
    * @param tokName   the method name
    * @param tokLeft   the left paren
    * @param params    the parameter expressions
    * @param tokRight  the right paren
    */
    public InvocationExpression(Expression expr, Token tokName, Token tokLeft, Expression[] params, Token tokRight)
        {
        super(tokLeft, expr);

        setStartToken(expr.getStartToken());
        setEndToken(tokRight);

        m_tokName     = tokName;
        m_aexprParams = params;
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
        Expression expr    = getExpression();
        Token      tokName = m_tokName;
        String     sName   = tokName.getText();

        // "super" means to use the "this" reference cast but to execute the
        // next implementation of the current method
        if (expr instanceof SuperExpression)
            {
            // verify super method name is same as this method name
            if (sName.equals(ctx.getMethodInfo().getName()))
                {
                // remember it is a super call
                m_fSuper = true;

                // tell super that it is allowed to be pre-compiled here
                ((SuperExpression) expr).allowSuper();
                }
            else
                {
                expr.logError(ERROR, NOT_SUPER_METHOD, null, errlist);

                // pretend the "super" was "this"
                Token tokThis  = new Token(Token.TOK_THIS,
                        tokName.getLine(), tokName.getOffset(), 0);
                expr = new ThisExpression(getBlock(), tokThis);
                }
            }

        // pre-compile the expression
        expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        setExpression(expr);

        boolean fSkip = false;
        boolean fRef  = !(expr instanceof TypeExpression);
        if (expr.getType() == UNKNOWN)
            {
            fSkip = true;
            }
        else if (expr instanceof SuperExpression)
            {
            throw new IllegalStateException();
            }
        else if (fRef)
            {
            fSkip = !expr.checkReference(false, errlist);
            }

        // pre-compile the parameters
        Expression[] aexpr = m_aexprParams;
        int          cexpr = aexpr.length;
        for (int i = 0; i < cexpr; ++i)
            {
            Expression param = (Expression) aexpr[i].precompile(
                    ctx, setUVars, setFVars, mapThrown, errlist);
            if (param.getType() == UNKNOWN || !param.checkValue(errlist))
                {
                fSkip = true;
                }
            aexpr[i] = param;
            }

        // find method (if the parameters all pre-compiled)
        MethodInfo method = null;
        if (!fSkip)
            {
            if (m_fSuper)
                {
                method = ctx.getSuperInfo();
                if (method == null)
                    {
                    expr.logError(ERROR, NO_SUPER_METHOD, new String[] {sName}, errlist);
                    }
                else if (method.getParamCount() != cexpr)
                    {
                    expr.logError(ERROR, SUPER_MISMATCH, new String[]
                            {""+cexpr, ""+method.getParamCount()}, errlist);
                    }
                else
                    {
                    // verify parameters are invocation assignable
                    for (int i = 0; i < cexpr; ++i)
                        {
                        aexpr[i].checkInvocationAssignable(ctx, method.
                                getParamInfo(i).getDataType(), errlist);
                        }
                    }
                }
            else
                {
                TypeInfo typeRef = ctx.getTypeInfo(expr.getType());
                if (typeRef == null)
                    {
                    throw new IllegalStateException("Missing type: " + expr.getType());
                    }
                TypeInfo typeObj    = ctx.getTypeInfo(OBJECT);
                boolean  fInterface = typeRef.isInterface();

                // check for an exact match
                DataType[] adtParam = new DataType[cexpr];
                for (int i = 0; i < cexpr; ++i)
                    {
                    adtParam[i] = aexpr[i].getType();
                    }
                method = typeRef.getMethodInfo(sName, adtParam);
                if (method == null && fInterface)
                    {
                    // all objects have the methods from Object
                    method = typeObj.getMethodInfo(sName, adtParam);
                    }
                if (method != null && !method.isAccessible())
                    {
                    method = null;
                    }

                if (method == null)
                    {
                    List        list = new ArrayList();
                    Enumeration enmr = typeRef.paramTypes(sName, cexpr);

                    // if the invocation is against an interface, merge in
                    // the methods from Object
                    if (fInterface)
                        {
                        Hashtable tbl = new Hashtable();
                        enmr = new ChainedEnumerator(enmr, typeObj.paramTypes(sName, cexpr));
                        while (enmr.hasMoreElements())
                            {
                            DataType adt[] = (DataType[]) enmr.nextElement();
                            tbl.put(Behavior.getSignature("", adt), adt);
                            }
                        enmr = tbl.elements();
                        }

NextMethod:         while (enmr.hasMoreElements())
                        {
                        DataType adt[] = (DataType[]) enmr.nextElement();

                        // check that the method is accessible
                        method = typeRef.getMethodInfo(sName, adt);
                        if (method == null)
                            {
                            // azzert(fInterface);
                            method = typeObj.getMethodInfo(sName, adt);
                            }
                        if (!method.isAccessible())
                            {
                            continue NextMethod;
                            }

                        // check that each argument can be assigned (via
                        // invocation conversion) to each parameter
                        for (int i = 0; i < cexpr; ++i)
                            {
                            if (!isAssignable(ctx, adtParam[i], adt[i], errlist))
                                {
                                continue NextMethod;
                                }
                            }

                        // add the method to the list of potential methods
                        list.add(method);
                        }

                    switch (list.size())
                        {
                        case 0:
                            logError(ERROR, METHOD_NOT_FOUND, new String[]
                                    {sName, expr.getType().toString()}, errlist);
                            method = null;
                            break;

                        case 1:
                            method = (MethodInfo) list.get(0);
                            break;

                        default:
                            // multiple methods are applicable and accessible;
                            // select the "best match" method
                            //
                            // JLS 15.11.2.2 Choose the Most Specific Method
                            // Let m be a name and suppose that there are two
                            // declarations of methods named m , each having
                            // n parameters. Suppose that one declaration
                            // appears within a class or interface T and that
                            // the types of the parameters are T1, ..., Tn;
                            // suppose moreover that the other declaration
                            // appears within a class or interface U and that
                            // the types of the parameters are U1, ..., Un.
                            // Then the method m declared in T is more
                            // specific than the method m declared in U if
                            // and only if both of the following are true:
                            //  1)  T can be converted to U by method
                            //      invocation conversion.
                            //  2)  Tj can be converted to Uj by method
                            //      invocation conversion, for all j from 1
                            //      to n.
NextT:                      for (enmr = new SimpleEnumerator(list.toArray());
                                    enmr.hasMoreElements(); )
                                {
                                MethodInfo methodT = (MethodInfo) enmr.nextElement();
                                if (!list.contains(methodT))
                                    {
                                    continue NextT;
                                    }

NextU:                          for (Iterator iter = list.iterator(); iter.hasNext(); )
                                    {
                                    MethodInfo methodU = (MethodInfo) iter.next();
                                    if (methodT != methodU)
                                        {
                                        // can T be converted to U?
                                        if (!isAssignable(ctx,
                                                methodT.getTypeInfo().getDataType(),
                                                methodU.getTypeInfo().getDataType(), errlist))
                                            {
                                            continue NextU;
                                            }

                                        // can Tj be converted to Uj
                                        for (int i = 0; i < cexpr; ++i)
                                            {
                                            if (!isAssignable(ctx,
                                                    methodT.getParamInfo(i).getDataType(),
                                                    methodU.getParamInfo(i).getDataType(), errlist))
                                                {
                                                continue NextU;
                                                }
                                            }

                                        // T is more specific than U; discard U
                                        iter.remove();
                                        }
                                    }
                                }

                            // make sure there is only one "best match"
                            if (list.size() == 1)
                                {
                                method = (MethodInfo) list.get(0);
                                }
                            else
                                {
                                method = null;
                                logError(ERROR, AMBIGUOUS_METHOD, new String[]
                                        {sName, expr.getType().toString()}, errlist);
                                }
                            break;
                        }
                    }
                }
            }

        if (method != null)
            {
            // check if instance method is being accessed without a reference
            if (!method.isStatic() && !fRef && !sName.equals(Constants.CONSTRUCTOR_NAME))
                {
                logError(ERROR, REF_REQUIRED, new String[] {sName}, errlist);
                }

            // verify method is accessible
            if (!method.isAccessible())
                {
                logError(ERROR, METHOD_NO_ACCESS, new String[]
                        {sName, expr.getType().toString()}, errlist);
                }

            // check exceptions from the method
            for (Enumeration enmr = method.exceptionTypes(); enmr.hasMoreElements(); )
                {
                checkThrownException(ctx, (DataType) enmr.nextElement(), mapThrown, errlist);
                }

            // the type of the invocation expression is the type of the method
            setType(method.getDataType());

            // apply assignment conversion to the parameters (for example,
            // passing an int value into a long parameter)
            for (int i = 0; i < cexpr; ++i)
                {
                aexpr[i] = aexpr[i].convertAssignable(ctx, method.getParamInfo(i).getDataType());
                }

            // register compile-time dependency on the method
            addDependency(method, false);
            }

        // store method information
        m_method = method;

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
        Expression   expr     = getExpression();
        boolean      fStatic  = isStatic();
        boolean      fOmitRef = false;
        MethodInfo   method   = m_method;
        Expression[] aexpr    = m_aexprParams;
        int          cexpr    = aexpr.length;
        String       sName    = m_tokName.getText();

        if (sName.equals(Constants.CONSTRUCTOR_NAME))
            {
            // an uninitialized reference will already be on the stack
            fOmitRef = true;
            }
        else if (fStatic)
            {
            if (expr.isConstant()
                    || expr instanceof TypeExpression
                    || expr instanceof VariableExpression
                    || expr instanceof ThisExpression)
                {
                fOmitRef = true;
                }
            }

        // compile reference
        if (!fOmitRef)
            {
            expr.compile(ctx, code, fReached, errlist);

            if (fStatic)
                {
                code.add(new Pop());
                }
            }

        if (method.isInlined() || !ctx.isDebug() && method.isInlineable())
            {
            // access the field directly
            FieldInfo field = method.getFieldInfo();

            // register compile-time dependency on the field
            addDependency(field, false);

            // determine if the field itself gets inlined
            if (field.isInlined() || !ctx.isDebug() && field.isInlineable())
                {
                if (!fOmitRef)
                    {
                    code.add(new Pop());
                    }

                if (field.getDataType() != VOID)
                    {
                    // compile constant accessor
                    super.compile(ctx, code, fReached, errlist);
                    }

                // compile indexed property accessor
                if (cexpr == 1 && aexpr[0].getType() == INT
                        && field.getDataType().isArray()
                        && field.getDataType().getElementType() == method.getDataType())
                    {
                    // compile index
                    aexpr[0].compile(ctx, code, fReached, errlist);

                    // extract array element
                    code.add(arrayLoad(getType()));
                    }
                else if (cexpr > 0)
                    {
                    // there are parameters but they are not used;
                    // evaluate and pop them if they might have side effects
                    for (int i = 0; i < cexpr; ++i)
                        {
                        Expression exprParam = aexpr[i];
                        if (!(exprParam.isConstant()
                                || exprParam instanceof TypeExpression
                                || exprParam instanceof VariableExpression
                                || exprParam instanceof ThisExpression))
                            {
                            exprParam.compile(ctx, code, fReached, errlist);
                            DataType dt = exprParam.getType();
                            code.add(dt == LONG || dt == DOUBLE ? (Op) new Pop2() : new Pop());
                            }
                        }
                    }
                }
            else
                {
                // register run-time dependency on the field
                addDependency(field, true);

                FieldConstant constant = (FieldConstant) field.getConstant();
                sName = method.getName();
                if (sName.startsWith("is") || sName.startsWith("get"))
                    {
                    code.add(fStatic ? (Op) new Getstatic(constant)
                                     : (Op) new Getfield (constant));

                    if (cexpr == 1 && aexpr[0].getType() == INT)
                        {
                        // getX(int) or isX(int)

                        // compile index
                        aexpr[0].compile(ctx, code, fReached, errlist);

                        // extract element
                        code.add(arrayLoad(getType()));
                        }
                    else if (cexpr != 0) // getX() or isX()
                        {
                        throw new IllegalStateException();
                        }
                    }
                else if (sName.startsWith("set"))
                    {
                    if (cexpr == 1)
                        {
                        // setX(T)

                        // compile value
                        aexpr[0].compile(ctx, code, fReached, errlist);

                        // store
                        code.add(fStatic ? (Op) new Putstatic(constant)
                                         : (Op) new Putfield (constant));
                        }
                    else if (cexpr == 2 && aexpr[0].getType() == INT)
                        {
                        // setX(int, T)

                        // load array
                        code.add(fStatic ? (Op) new Getstatic(constant)
                                         : (Op) new Getfield (constant));

                        // compile index
                        aexpr[0].compile(ctx, code, fReached, errlist);

                        // compile value
                        aexpr[1].compile(ctx, code, fReached, errlist);

                        // store element
                        code.add(arrayStore(aexpr[1].getType()));
                        }
                    else
                        {
                        throw new IllegalStateException();
                        }
                    }
                else
                    {
                    throw new IllegalStateException();
                    }
                }
            }
        else
            {
            // compile parameters
            for (int i = 0; i < cexpr; ++i)
                {
                aexpr[i].compile(ctx, code, fReached, errlist);
                }

            MethodConstant constant = (MethodConstant) method.getConstant();
            if (constant instanceof InterfaceConstant)
                {
                if (m_fSuper)
                    {
                    // TODO: for Java 8 we need to generate the following
                    //      code.add(new Invokespecial(constant));
                    // however, for that to work we need to increase to the
                    // class file version of 52.0, and to do that we need to
                    // generate the StackMapTableAttribute
                    expr.logError(ERROR, DEFAULT_UNSUPP, null, errlist);
                    }
                else if (method.isStatic())
                    {
                    // TODO: similarly to the above, for Java 8 we need to generate
                    //      code.add(new Invokestatic(constant));
                    expr.logError(ERROR, STATIC_UNSUPP, null, errlist);
                    }
                else
                    {
                    code.add(new Invokeinterface((InterfaceConstant) constant));
                    }
                }
            else if (fStatic)
                {
                code.add(new Invokestatic(constant));
                }
            else if (method.isPrivate() || m_fSuper ||
                    method.getName().equals(Constants.CONSTRUCTOR_NAME))
                {
                code.add(new Invokespecial(constant));
                }
            else
                {
                code.add(new Invokevirtual(constant));
                }

            // register run-time dependency on the method
            addDependency(method, true);
            }

        if (isDiscarded())
            {
            switch (getType().getTypeString().charAt(0))
                {
                default:
                    code.add(new Pop());
                    break;

                case 'D':
                case 'J':
                    code.add(new Pop2());
                    break;

                case 'V':
                    break;
                }
            }

        return fReached;
        }

    /**
    * Register a dependency.
    *
    * @param method
    * @param fRuntime  true for a runtime dependency, false for a compile dependency
    */
    protected void addDependency(MethodInfo method, boolean fRuntime)
            throws CompilerException
        {
        Token tokMethod = m_tokName;
        method.addDependency(fRuntime, tokMethod.getLine(), tokMethod.getOffset(),
                tokMethod.getLine(), tokMethod.getOffset() + tokMethod.getLength());
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
        Token tokField = m_tokName;
        field.addDependency(fRuntime, tokField.getLine(), tokField.getOffset(),
                tokField.getLine(), tokField.getOffset() + tokField.getLength());
        }

    /**
    * Helper to construct an op to load an element of an array.
    *
    * @param dt  the type of the element
    *
    * @return the op which will load the element (assuming the array
    *         reference and the integer index are on the stack)
    */
    private static Op arrayLoad(DataType dt)
        {
        Op opLoad;
        switch (dt.getTypeString().charAt(0))
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
            case 'N':
            case 'L':
            case 'R':
                opLoad = new Aaload();
                break;
            default:
                throw new IllegalStateException();
            }

        return opLoad;
        }

    /**
    * Helper to construct an op to store an element of an array.
    *
    * @param dt  the type of the element
    *
    * @return the op which will store the element (assuming the array
    *         reference, the integer index, and the value are on the stack)
    */
    private static Op arrayStore(DataType dt)
        {
        Op opStore;
        switch (dt.getTypeString().charAt(0))
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
                opStore = new Aastore();
                break;
            default:
                throw new IllegalStateException();
            }

        return opStore;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the method name.
    *
    * @return the method name
    */
    public String getName()
        {
        return m_tokName.getText();
        }

    /**
    * Get the method constant.  Valid only after pre-compilation
    *
    * @return the method constant for the method access expression
    */
    public MethodConstant getMethodConstant()
        {
        MethodInfo method = m_method;
        return method == null ? null : (MethodConstant) m_method.getConstant();
        }

    /**
    * Determine if the method is static.
    *
    * @return true if the method is static
    */
    public boolean isStatic()
        {
        MethodInfo method = m_method;
        return method != null && method.isStatic();
        }

    /**
    * Determine the constant value of the expression.
    * Note:  This question is only valid in the compile step.
    *
    * @return the constant value of the expression
    */
    public Object getValue()
        {
        return m_method.getFieldInfo().getValue();
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

        out(sIndent + "  parameters:");
        Expression[] aexpr = m_aexprParams;
        int          cexpr = aexpr.length;
        for (int i = 0; i < cexpr; ++i)
            {
            aexpr[i].print(sIndent + "    ");
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "InvocationExpression";

    private static final DataType VOID      = DataType.VOID;
    private static final DataType INT       = DataType.INT;
    private static final DataType LONG      = DataType.LONG;
    private static final DataType DOUBLE    = DataType.DOUBLE;
    private static final DataType OBJECT    = DataType.OBJECT;
    private static final DataType UNKNOWN   = DataType.UNKNOWN;


    /**
    * The method name.
    */
    private Token m_tokName;

    /**
    * The array of parameters.
    */
    private Expression[] m_aexprParams;

    /**
    * The selected method info.
    */
    private MethodInfo m_method;

    /**
    * Is it a super method call?
    */
    private boolean m_fSuper;
    }
