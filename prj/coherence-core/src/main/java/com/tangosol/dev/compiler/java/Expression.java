/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.Op;
import com.tangosol.dev.assembler.Iconst;
import com.tangosol.dev.assembler.Lconst;
import com.tangosol.dev.assembler.Fconst;
import com.tangosol.dev.assembler.Dconst;
import com.tangosol.dev.assembler.Aconst;
import com.tangosol.dev.assembler.IntConstant;
import com.tangosol.dev.assembler.MethodConstant;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.compiler.TypeInfo;
import com.tangosol.dev.compiler.MethodInfo;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.SortedEnumerator;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Enumeration;


/**
* This class implements a Java script expression.
*
* An expression has several attributes:
*
*   1.  All expressions have a data type.  Expressions can have the void
*       data type, which means that the expressions cannot be used as the
*       input to (cannot be subexpressions of) other expressions.
*
*       The data type of an expression is always determinable at compilation
*       time.  Furthermore, due to the conaining expression (or statement)
*       the data type may require coersion.  For example, numeric promotion,
*       implied type casts, and method invocations without use of the return
*       value (coerced to void).
*
*   2.  Expressions can have a determinable value at compilation.  These
*       expressions are said to be constant.  Expressions used in certain
*       constructs, such as case values, must be constant.  The value of
*       an expression, if determinable during compilation, reflects the
*       data type of the expression.  A void expression cannot have a value.
*
*       As a result, an expression must be able to answer two questions:
*
*       1.  Is there a known (constant) value?
*       2.  If so, what is the value?
*
*   3.  Expressions can be variables.  The use of the term "variable" in
*       this case does not denote a "local variable of a method", rather
*       it refers to an expression that can be assigned a value.  This
*       includes, but is not limited to, a local variable, a class or
*       instance field, and a JavaBean property.
*
*   4.  Expressions can throw exceptions.  Some of these exceptions are
*       defined explicitly by the Java Language Specification, for example
*       the ClassCastException.  The exceptions defined by the specification
*       are either errors (derive from java.lang.Error) or runtime exceptions
*       (derive from java.lang.RuntimeException).  Additionally, expressions
*       can have exceptions that result from method invocations either
*       implicitly or explicitly performed by the expression.
*
*       There are two categories (branches of the Java class hierarchy) of
*       exceptions that are ignored.  These two branches are the afore-
*       mentioned java.lang.Error and java.lang.RuntimeException.  The Java
*       Language Specification (with the JAVAC compiler implementation for
*       proof) states and/or implies the following two things:
*
*           It must be assumed that any statement, including the empty
*           statement and a block containing no statements, can throw any
*           exception from either one of these categories.  (See JAVAC.)
*
*           These categories reflect exceptions that need not be checked
*           nor declared.  (See 11.2.1 and 11.2.2.)
*
*   5.  Expressions can imply dependencies.  These dependencies may be
*       related to members of the class within which the containing method
*       exists, or other classes.  The dependencies (at a Java level) are:
*
*       1.  Class  - the new keyword
*       2.  Field  - either static or instance
*       3.  Method - either static or instance
*
*   6.  Expressions may not be reachable.  This is similar in concept to the
*       unreachable statement checking enforced by Java compilers, but is not
*       considered an error by a Java compiler.  Consider the expression:
*
*           (true || foo())
*
*       In which foo() is never executed and should, if possible, be
*       discarded as if the expression read:
*
*           (true)
*
* @version 1.00, 09/16/98
* @author  Cameron Purdy
*/
public abstract class Expression extends Element
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an Expression.
    *
    * @param block     the block (scope) within which this element exists
    * @param tokFirst  the first or only token making up the language element
    */
    protected Expression(Block block, Token tokFirst)
        {
        super(block, tokFirst);
        }

    /**
    * Construct an Expression.
    *
    * @param block     the block (scope) within which this element exists
    * @param tokFirst  the first or only token making up the language element
    * @param tokLast   the last token making up the language element
    */
    protected Expression(Block block, Token tokFirst, Token tokLast)
        {
        super(block, tokFirst, tokLast);
        }


    // ----- code generation ------------------------------------------------

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
        // compilation logic for constants only!
        DataType dt   = getType();
        Object   oVal = getValue();
        Op       op   = null;

        if (dt.isPrimitive())
            {
            switch (dt.getTypeString().charAt(0))
                {
                case 'Z':
                    op = new Iconst(((Boolean) oVal).booleanValue() ? TRUE : FALSE);
                    break;

                case 'B':
                case 'C':
                case 'S':
                case 'I':
                    op = new Iconst(((Number) oVal).intValue());
                    break;

                case 'J':
                    op = new Lconst(((Long) oVal).longValue());
                    break;

                case 'F':
                    op = new Fconst(((Float) oVal).floatValue());
                    break;

                case 'D':
                    op = new Dconst(((Double) oVal).doubleValue());
                    break;

                default:
                    throw new IllegalStateException();
                }
            }
        else if (dt.isReference() && oVal == null)
            {
            op = new Aconst();
            }
        else if (dt == DataType.STRING)
            {
            op = new Aconst((String) oVal);
            }
        else
            {
            throw new IllegalStateException();
            }

        code.add(op);

        return fReached;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the type of the expression.
    * Note:  Before pre-compilation, an expression does not have a type.
    *
    * @return the type of the expression
    */
    public DataType getType()
        {
        return dt;
        }

    /**
    * Set the type of the expression.  This is done during pre-compilation.
    *
    * @param dt  the type of the expression
    */
    protected void setType(DataType dt)
        {
        this.dt = dt;
        }

    /**
    * Force the expression to be void.
    *
    * For most expressions, the resulting value cannot be discarded, since
    * most expressions cannot be used as a void construct such as an
    * expression statement.
    *
    * @param fDiscard  true to force the expression to be void
    */
    public void setDiscarded(boolean fDiscard)
        {
        this.fDiscard = fDiscard;
        }

    /**
    * Determine if the expression should discard its value.
    *
    * @return  true if the expression is forced to be void
    */
    public boolean isDiscarded()
        {
        return fDiscard;
        }

    /**
    * Determine if the expression has a constant value.
    * Note:  This question is only valid after the expression is pre-compiled.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
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
        throw new IllegalStateException();
        }


    // -----

    /**
    * Determine if the first data type can be cast to the second data type.
    *
    * From the JLS 5.5:
    *
    * Casting conversion is applied to the operand of a cast operator
    * (15.15): the type of the operand expression must be converted to the
    * type explicitly named by the cast operator. Casting contexts allow the
    * use of an identity conversion (5.1.1), a widening primitive conversion
    * (5.1.2), a narrowing primitive conversion (5.1.3), a widening
    * reference conversion (5.1.4), or a narrowing reference conversion
    * (5.1.5). Thus casting conversions are more inclusive than assignment
    * or method invocation conversions: a cast can do any permitted
    * conversion other than a string conversion.
    *
    * @param ctx      the compiler context
    * @param dtThis   the type to cast from
    * @param dtThat   the type to cast to
    * @param errlist  the error list to log errors to
    *
    * @return true if a cast to the specified type is valid
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected static boolean isCastable(Context ctx, DataType dtThis, DataType dtThat, ErrorList errlist)
            throws CompilerException
        {
        // JLS 5.1.1 Identity Conversions:  A conversion from a type to that
        // same type is permitted for any type.
        if (dtThis == dtThat)
            {
            return true;
            }

        if (dtThis.isPrimitive())
            {
            // with both narrowing and widening primitive conversions
            // supported, any numeric type can be cast to any other numeric
            // type; the only other case where this type is primitive is if
            // it is boolean, and boolean can only be cast to boolean, and
            // identity conversions have already been checked
            if (dtThis.isNumeric() && dtThat.isNumeric())
                {
                return true;
                }
            }
        else if (dtThis == NULL)
            {
            // JLS 5.1.4:  Widening Reference Conversions
            // The following conversions are called the widening reference
            // conversions:
            //  - From the null type to any class type, interface type or
            //    array type.
            // ...
            if (dtThat.isReference())
                {
                return true;
                }
            }
        else if (dtThis != VOID) // reference type
            {
            // check widening reference conversions
            if (isAssignable(ctx, dtThis, dtThat, errlist))
                {
                return true;
                }

            // JLS 5.1.5 Narrowing Reference Conversions
            //    - From any array type SC[] to any array type TC[], provided
            //      that SC and TC are reference types and there is a
            //      narrowing conversion from SC to TC.
            while (dtThis.isArray() && dtThat.isArray())
                {
                dtThis = dtThis.getElementType();
                dtThat = dtThat.getElementType();
                }
            if (dtThis.isArray() || !dtThis.isReference() || !dtThat.isReference())
                {
                return false;
                }

            // JLS 5.1.5 Narrowing Reference Conversions
            // Casting from java.lang.Object
            //  1)  From any class type S to any class type T, provided that
            //      S is a superclass of T.  (An important special case is
            //      that there is a narrowing conversion from the class type
            //      Object to any other class type.)
            //  2)  From any class type S to any interface type K, provided
            //      that S is not final and does not implement K. (An
            //      important special case is that there is a narrowing
            //      conversion from the class type Object to any interface
            //      type.)
            //  3)  From type Object to any array type.
            //  4)  From type Object to any interface type.
            if (dtThis == DataType.OBJECT)
                {
                return true;
                }
            else if (dtThat.isArray())
                {
                // only Object can be cast to an array
                return false;
                }

            // load the type information
            TypeInfo infoThis = ctx.getTypeInfo(dtThis);
            TypeInfo infoThat = ctx.getTypeInfo(dtThat);
            if (infoThis == null || infoThat == null)
                {
                return false;
                }

            if (infoThis.isInterface())
                {
                if (infoThat.isInterface())
                    {
                    // JLS 5.1.5 Narrowing Reference Conversions
                    // Interface-to-interface conversion:
                    // - From any interface type J to any interface type K,
                    //   provided that J is not a sub-interface of K and
                    //   there is no method name m such that J and K both
                    //   declare a method named m with the same [parameter]
                    //   signature but different return types.
                    // (Note:  if J is a sub-interface of K, it is a widening
                    // conversion)

                    // first check if the interface being cast to is a
                    // sub-interface, in which case there cannot be conflicts
                    // in the return types of declared methods
                    if (infoThat.isInterface(infoThis.getDataType()))
                        {
                        return true;
                        }

                    // "zipper" search for identical method names
                    Enumeration enmrThisMethods = new SortedEnumerator(infoThis.methodNames());
                    Enumeration enmrThatMethods = new SortedEnumerator(infoThat.methodNames());
                    if (!enmrThisMethods.hasMoreElements() ||
                        !enmrThatMethods.hasMoreElements())
                        {
                        return true;
                        }
                    String sThisMethod = (String) enmrThisMethods.nextElement();
                    String sThatMethod = (String) enmrThatMethods.nextElement();
                    while (true)
                        {
                        int nCmp = sThisMethod.compareTo(sThatMethod);
                        if (nCmp == 0)
                            {
                            // names match; find methods on "this" and "that"
                            // declared with the same parameter types
                            Enumeration enmrParams = infoThis.paramTypes(sThisMethod, -1);
                            while (enmrParams.hasMoreElements())
                                {
                                DataType[] adtParams  = (DataType[]) enmrParams.nextElement();
                                MethodInfo methodThat = infoThat.getMethodInfo(sThisMethod, adtParams);
                                if (methodThat != null)
                                    {
                                    // both "this" and "that" have a method
                                    // declared with the same parameter types
                                    // so the return value must match
                                    MethodInfo methodThis = infoThis.getMethodInfo(sThisMethod, adtParams);
                                    if (methodThis.getDataType() != methodThat.getDataType())
                                        {
                                        return false;
                                        }
                                    }
                                }

                            // gg: 2002.3.26 - move the "zipper" forward
                            if (!enmrThisMethods.hasMoreElements() ||
                                !enmrThatMethods.hasMoreElements())
                                {
                                return true;
                                }
                            sThisMethod = (String) enmrThisMethods.nextElement();
                            sThatMethod = (String) enmrThatMethods.nextElement();
                            }
                        else if (nCmp < 0)
                            {
                            if (!enmrThisMethods.hasMoreElements())
                                {
                                return true;
                                }
                            sThisMethod = (String) enmrThisMethods.nextElement();
                            }
                        else
                            {
                            if (!enmrThatMethods.hasMoreElements())
                                {
                                return true;
                                }
                            sThatMethod = (String) enmrThatMethods.nextElement();
                            }
                        }
                    }
                else // infoThat is a class
                    {
                    // JLS 5.1.5 Narrowing Reference Conversions
                    // Interface-to-class conversion
                    //  1)  From any interface type J to any class type T
                    //      that is not final.
                    //  2)  From any interface type J to any class type T
                    //      that is final, provided that T implements J.
                    return !infoThat.isFinal() || infoThat.isInterface(infoThis.getDataType());
                    }
                }
            else // infoThis is a class
                {
                if (infoThat.isInterface())
                    {
                    // JLS 5.1.5 Narrowing Reference Conversions
                    // - From any class type S to any interface type K, provided
                    //   that S is not final and does not implement K.
                    // (Note:  if S implements K, it is a widening conversion)
                    return !infoThis.isFinal();
                    }
                else // infoThat is a class
                    {
                    // JLS 5.1.5 Narrowing Reference Conversions
                    // - From any class type S to any class type T, provided that
                    //   S is a superclass of T.
                    while (infoThat != null)
                        {
                        if (infoThis.getDataType() == infoThat.getDataType())
                            {
                            return true;
                            }

                        infoThat = infoThat.getSuperInfo();
                        }
                    }
                }
            }

        return false;
        }

    /**
    * Determine if the first data type can be converted to the second data
    * type without casting.
    * <pre>
    * From the JLS 5.5:
    *
    * Assignment conversion occurs when the value of an expression is
    * assigned (15.25) to a variable: the type of the expression must be
    * converted to the type of the variable. Assignment contexts allow the
    * use of an identity conversion (5.1.1), a widening primitive conversion
    * (5.1.2), or a widening reference conversion (5.1.4). In addition, a
    * narrowing primitive conversion may be used if all of the following
    * conditions are satisfied:
    *   1)  The expression is a constant expression of type int.
    *   2)  The type of the variable is byte, short, or char.
    *   3)  The value of the expression (which is known at compile time,
    *       because it is a constant expression) is representable in the
    *       type of the variable.
    * </pre>
    * @param ctx      the compiler context
    * @param dtThis   the type to convert
    * @param dtThat   the parameter type to convert to
    * @param errlist  the error list to log errors to
    *
    * @return true if a cast to the specified type is valid
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected static boolean isAssignable(Context ctx, DataType dtThis, DataType dtThat, ErrorList errlist)
            throws CompilerException
        {
        // JLS 5.1.1 Identity Conversions:  A conversion from a type to that
        // same type is permitted for any type.
        if (dtThis == dtThat)
            {
            return true;
            }

        if (dtThis == UNKNOWN || dtThat == UNKNOWN)
            {
            // goal:  minimize reported compiler errors
            return true;
            }

        if (dtThis == VOID || dtThat == VOID)
            {
            return false;
            }

        if (dtThis.isPrimitive())
            {
            // JLS 5.1.2 Widening Primitive Conversions:  The following 19
            // specific conversions on primitive types are called the
            // widening primitive conversions:
            switch (dtThis.getTypeString().charAt(0))
                {
                // JLS 5.1.2:  byte to short, int, long, float, or double
                case 'B':
                    if (dtThat == SHORT)
                        {
                        return true;
                        }
                    // fall through

                // JLS 5.1.2:  short to int, long, float, or double
                // JLS 5.1.2:  char to int, long, float, or double
                case 'S':
                case 'C':
                    if (dtThat == INT)
                        {
                        return true;
                        }
                    // fall through

                // JLS 5.1.2:  int to long, float, or double
                case 'I':
                    if (dtThat == LONG)
                        {
                        return true;
                        }
                    // fall through

                // JLS 5.1.2:  long to float or double
                case 'J':
                    if (dtThat == FLOAT)
                        {
                        return true;
                        }
                    // fall through

                // JLS 5.1.2:  float to double
                case 'F':
                    if (dtThat == DOUBLE)
                        {
                        return true;
                        }

                case 'D':
                    // no widening numeric conversion left
                    break;

                // JLS 5.1.1 Identity Conversions:  The only permitted
                // conversion that involves the type boolean is the identity
                // conversion from boolean to boolean.
                case 'Z':
                    break;

                default:
                    throw new IllegalStateException();
                }
            }
        else if (dtThis == NULL)
            {
            // JLS 5.1.4:  Widening Reference Conversions
            // The following conversions are called the widening reference
            // conversions:
            //  - From the null type to any class type, interface type or
            //    array type.
            // ...
            if (dtThat.isReference())
                {
                return true;
                }
            }
        else // reference type
            {
            // JLS 5.1.4:  Widening Reference Conversions
            // The following conversions are called the widening reference
            // conversions:
            //  - From any array type to type Object.
            //  - From any array type to type Cloneable.
            //  - From any array type SC[] to any array type TC[], provided
            //    that SC and TC are reference types and there is a widening
            //    conversion from SC to TC.
            // ...
            while (dtThis.isArray() && dtThat.isArray())
                {
                dtThis = dtThis.getElementType();
                dtThat = dtThat.getElementType();
                }

            if (dtThis.isArray())
                {
                if (dtThat == OBJECT || dtThat == CLONEABLE)
                    {
                    return true;
                    }
                }
            // this type is not an array, and nothing else is assignable to
            // an array, so we are left with just interface and class types
            else if (!dtThat.isArray())
                {
                // JLS 5.1.4:  Widening Reference Conversions
                // The following conversions are called the widening
                // reference conversions:
                // ...
                //  - From any class type S to any class type T, provided
                //    that S is a subclass of T.  (An important special case
                //    is that there is a widening conversion to the class
                //    type Object from any other class type.)
                //  - From any interface type to type Object.
                if (dtThat == OBJECT)
                    {
                    return true;
                    }

                // JLS 5.1.4:  Widening Reference Conversions
                // The following conversions are called the widening
                // reference conversions:
                // ...
                //  - From any class type S to any interface type K, provided
                //    that S implements K.
                //  - From any interface type J to any interface type K,
                //    provided that J is a sub-interface of K.
                TypeInfo infoThis = ctx.getTypeInfo(dtThis);
                if (infoThis == null)
                    {
                    return false;
                    }
                if (infoThis.isInterface(dtThat))
                    {
                    return true;
                    }

                // JLS 5.1.4:  Widening Reference Conversions
                // The following conversions are called the widening
                // reference conversions:
                // ...
                //  - From any class type S to any class type T, provided
                //    that S is a subclass of T.
                while (infoThis != null)
                    {
                    if (infoThis.getDataType() == dtThat)
                        {
                        return true;
                        }

                    infoThis = infoThis.getSuperInfo();
                    }
                }
            }

        return false;
        }

    /**
    * Check that the first data type is comparable to the second data type.
    *
    * From the JLS 15.20.3 Reference Equality Operators == and !=:
    *
    *   A compile-time error occurs if it is impossible to convert the type
    *   of either operand to the type of the other by a casting conversion
    *
    * @param ctx      the compiler context
    * @param dtThis   the first type
    * @param dtThat   the second type
    * @param errlist  the error list to log errors to
    *
    * @return true if the two types are comparable
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected static boolean isComparable(Context ctx, DataType dtThis, DataType dtThat, ErrorList errlist)
            throws CompilerException
        {
        return isCastable(ctx, dtThis, dtThat, errlist)
            || isCastable(ctx, dtThat, dtThis, errlist);
        }


    // ----- promotions and conversions

    /**
    * Perform numeric promotion on the expression.
    *
    * @return the promoted numeric expression to use instead of this
    *         expression, or in most cases, this expression itself
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Expression promoteNumeric()
            throws CompilerException
        {
        DataType dt = getType();
        switch (dt.getTypeString().charAt(0))
            {
            case 'B':
            case 'C':
            case 'S':
                return new CastExpression(this, INT);

            case 'I':
            case 'J':
            case 'F':
            case 'D':
            case 'U':   // unknown type
                return this;

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Perform binary numeric promotion on the expression.
    *
    * @return the promoted numeric expression to use instead of this
    *         expression, or in most cases, this expression itself
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Expression promoteNumeric(Expression exprOther)
            throws CompilerException
        {
        DataType dtThis = this.getType();
        DataType dtThat = exprOther.getType();

        int nThis;
        switch (dtThis.getTypeString().charAt(0))
            {
            case 'B':
            case 'C':
            case 'S':
                nThis = 0;  // must be promoted to at least int
                break;
            case 'I':
                nThis = 1;
                break;
            case 'J':
                nThis = 2;
                break;
            case 'F':
                nThis = 3;
                break;
            case 'D':
                nThis = 4;
                break;

            case 'U':   // unknown type
                return this;

            default:
                throw new IllegalStateException();
            }

        int nThat;
        switch (dtThat.getTypeString().charAt(0))
            {
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                nThat = 1;
                break;
            case 'J':
                nThat = 2;
                break;
            case 'F':
                nThat = 3;
                break;
            case 'D':
                nThat = 4;
                break;

            case 'U':   // unknown type
                return this;

            default:
                throw new IllegalStateException();
            }

        if (nThis >= nThat)
            {
            return this;
            }

        switch (nThat)
            {
            case 1:
                return new CastExpression(this, INT);
            case 2:
                return new CastExpression(this, LONG);
            case 3:
                return new CastExpression(this, FLOAT);
            case 4:
                return new CastExpression(this, DOUBLE);
            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Some types are considered assignable although they require conversion.
    *
    * @param ctx     the compilation context
    * @param dtThat  the type to assign to
    *
    * @return the resulting expression (this if not conversion is required)
    */
    protected Expression convertAssignable(Context ctx, DataType dtThat)
        {
        DataType dtThis = getType();
        if (dtThis != dtThat && dtThis.isPrimitive() && dtThat.isPrimitive())
            {
            return new CastExpression(this, dtThat);
            }

        return this;
        }

    // ----- checks

    /**
    * Check that the expression results in a value.
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if the expression results in a value
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkValue(ErrorList errlist)
            throws CompilerException
        {
        // type expressions and void expressions do not have a value
        boolean fValue = !(this instanceof TypeExpression) && getType() != VOID;

        if (!fValue)
            {
            notValue(errlist);
            }

        return fValue;
        }

    /**
    * Check that the expression is constant.
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if constant
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkConstant(ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        boolean fConst = isConstant();

        if (!fConst && getType() != UNKNOWN)
            {
            notConstant(errlist);
            }

        return fConst;
        }

    /**
    * Check that the type of the expression is numeric.
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if numeric
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkNumeric(ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        boolean fNumeric = getType().isNumeric();

        if (!fNumeric && getType() != UNKNOWN)
            {
            notNumeric(errlist);
            }

        return fNumeric;
        }

    /**
    * Check that the type of the expression is integral.
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if integral
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkIntegral(ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        boolean fIntegral = getType().isIntegral();

        if (!fIntegral && getType() != UNKNOWN)
            {
            notIntegral(errlist);
            }

        return fIntegral;
        }

    /**
    * Check that the type of the expression is boolean.
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if boolean
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkBoolean(ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        boolean fBoolean = (getType() == DataType.BOOLEAN);

        if (!fBoolean && getType() != UNKNOWN)
            {
            notBoolean(errlist);
            }

        return fBoolean;
        }

    /**
    * Check that the type of the expression is a reference type.
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if a reference type
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkReference(ErrorList errlist)
            throws CompilerException
        {
        return checkReference(true, errlist);
        }

    /**
    * Check that the type of the expression is a reference type.
    *
    * @param fNull    true if the null type is allowed
    * @param errlist  the error list to log errors to
    *
    * @return true if a reference type
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkReference(boolean fNull, ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        return checkReferenceType(fNull, errlist);
        }

    /**
    * Check that the type of the expression is a reference type.
    *
    * @param fNull    true if the null type is allowed
    * @param errlist  the error list to log errors to
    *
    * @return true if a reference type
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkReferenceType(boolean fNull, ErrorList errlist)
            throws CompilerException
        {
        boolean fRef = getType().isReference();

        if (!fRef && getType() != UNKNOWN)
            {
            notReference(errlist);
            }
        else if (!fNull && getType() == DataType.NULL)
            {
            logError(ERROR, NULL_ILLEGAL, null, errlist);
            }

        return fRef;
        }

    /**
    * Check that the type of the expression is an array type.
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if an array type
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkArray(ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        return checkArray(true, errlist);
        }

    /**
    * Check that the type of the expression is a array type.
    *
    * @param fNull    true if the null type is allowed
    * @param errlist  the error list to log errors to
    *
    * @return true if an array type
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkArray(boolean fNull, ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        boolean fArray = getType().isArray();

        if (!fArray)
            {
            if (getType() == DataType.NULL)
                {
                if (fNull)
                    {
                    fArray = true;
                    }
                else
                    {
                    logError(ERROR, NULL_ILLEGAL, null, errlist);
                    }
                }
            else if (getType() != UNKNOWN)
                {
                notArray(errlist);
                }
            }

        return fArray;
        }

    /**
    * Check that this expression can be cast to the specified data type.
    *
    * @param ctx      the compiler context
    * @param type     the type to cast to
    * @param errlist  the error list to log errors to
    *
    * @return true if a cast to the specified type is valid
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkCastable(Context ctx, DataType type, ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        DataType dtThis = getType();
        DataType dtThat = type;

        boolean fCastable = isCastable(ctx, dtThis, dtThat, errlist);

        if (!fCastable && dtThis != UNKNOWN)
            {
            notCastable(dtThat, errlist);
            }

        return fCastable;
        }

    /**
    * Check that this expression can be converted to the specified data type
    * for purposes of being passed as a parameter.  The JLS refers to this
    * as "Method Invocation Conversion".
    *
    * @param ctx      the compiler context
    * @param type     the type to convert to
    * @param errlist  the error list to log errors to
    *
    * @return true if a cast to the specified type is valid
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkInvocationAssignable(Context ctx, DataType type, ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        DataType dtThis = getType();
        DataType dtThat = type;

        boolean fAssignable = isAssignable(ctx, dtThis, dtThat, errlist);
        if (!fAssignable && dtThis != UNKNOWN)
            {
            notAssignable(dtThat, errlist);
            }

        return fAssignable;
        }

    /**
    * Check that this expression can be converted to the specified data type
    * without casting.
    *
    * @param ctx      the compiler context
    * @param type     the type to convert to
    * @param errlist  the error list to log errors to
    *
    * @return true if a cast to the specified type is valid
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkAssignable(Context ctx, DataType type, ErrorList errlist)
            throws CompilerException
        {
        if (!checkValue(errlist))
            {
            return false;
            }

        DataType dtThis = getType();
        DataType dtThat = type;

        boolean fAssignable = isAssignable(ctx, dtThis, dtThat, errlist);
        if (!fAssignable)
            {
            // JLS 5.1.2:  In addition, a narrowing primitive conversion may
            // be used if all of the following conditions are satisfied:
            // - The expression is a constant expression of type int.
            // - The type of the variable is byte, short, or char.
            // - The value of the expression (which is known at compile time,
            //   because it is a constant expression) is representable in the
            //   type of the variable.
            if (dtThis == INT && isConstant() && (dtThat == BYTE || dtThat == SHORT || dtThat == CHAR))
                {
                int n = ((Number) getValue()).intValue();
                if (dtThat == BYTE)
                    {
                    if (n >= Byte.MIN_VALUE && n <= Byte.MAX_VALUE)
                        {
                        fAssignable = true;
                        }
                    }
                else if (dtThat == SHORT)
                    {
                    if (n >= Short.MIN_VALUE && n <= Short.MAX_VALUE)
                        {
                        fAssignable = true;
                        }
                    }
                else // dtThat == CHAR
                    {
                    if (n >= Character.MIN_VALUE && n <= Character.MAX_VALUE)
                        {
                        fAssignable = true;
                        }
                    }
                }

            if (!fAssignable && dtThis != UNKNOWN)
                {
                notAssignable(dtThat, errlist);
                }
            }

        return fAssignable;
        }

    /**
    * Check that this reference expression is comparable to the specified
    * reference expression.
    *
    * @param ctx      the compiler context
    * @param that     the other expression
    * @param errlist  the error list to log errors to
    *
    * @return true if this expression can be compared to the other expression
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkComparable(Context ctx, Expression that, ErrorList errlist)
            throws CompilerException
        {
        if (!(this.checkValue(errlist) && that.checkValue(errlist)))
            {
            return false;
            }

        DataType dtThis = this.getType();
        DataType dtThat = that.getType();

        boolean fComparable = isComparable(ctx, dtThis, dtThat, errlist);
        if (!fComparable && dtThis != UNKNOWN)
            {
            notComparable(dtThat, errlist);
            }

        return fComparable;
        }

    /**
    * Check that the expression is theoritecally assignable (a "variable").
    *
    * @param errlist  the error list to log errors to
    *
    * @return true if the expression is a "variable" (according to JLS)
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean checkAssignable(ErrorList errlist)
            throws CompilerException
        {
        // must be over-ridden by the implementation of variable expression,
        // array accessor, and field accessor
        if (getType() != UNKNOWN)
            {
            notAssignable(errlist);
            }
        return false;
        }

    /**
    * Verify that the data type is an exception and record that it is thrown
    * by this expression (or that this expression is thrown, in the case of
    * a throw statement).
    *
    * @param ctx        the compiler context (used to check data types)
    * @param dt         the data type of the exception
    * @param mapThrown  the map of thrown exceptions
    * @param errlist    the error list to log errors to
    *
    * @return true if the exception can be thrown
    */
    protected boolean checkThrownException(Context ctx, DataType dt, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        if (dt == UNKNOWN)
            {
            return true;
            }

        // load the exception type information
        TypeInfo info = ctx.getTypeInfo(dt);
        if (info == null)
            {
            notType(dt, errlist);
            return false;
            }

        // make sure it is throwable
        if (!info.isThrowable())
            {
            notThrowable(dt, errlist);
            return false;
            }

        // determine if it is an error or run-time exception (not checked)
        if (!isChecked(ctx, dt))
            {
            return true;
            }

        // structure of mapThrown:
        //  - key is data type
        //  - value is a set of expressions that throw that data type
        Set set = (Set) mapThrown.get(dt);
        if (set == null)
            {
            set = new HashSet();
            mapThrown.put(dt, set);
            }

        set.add(this);
        return true;
        }

    /**
    * Verify that the data type is an exception and verify that it can be
    * caught (which means it must be possible for it to have been thrown).
    *
    * @param ctx        the compiler context (used to check data types)
    * @param dt         the data type of the exception
    * @param mapThrown  the map of thrown exceptions
    * @param errlist    the error list to log errors to
    *
    * @return true if the exception can be caught
    */
    protected boolean checkCaughtException(Context ctx, DataType dt, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        if (dt == UNKNOWN)
            {
            return true;
            }

        // load the exception type information
        TypeInfo info = ctx.getTypeInfo(dt);
        if (info == null)
            {
            notType(dt, errlist);
            return false;
            }

        // make sure it is throwable
        if (!info.isThrowable())
            {
            notThrowable(dt, errlist);
            return false;
            }

        // determine if it is an error or run-time exception (not checked)
        if (!isChecked(ctx, dt))
            {
            return true;
            }

        boolean fThrown = catchException(ctx, dt, mapThrown);
        if (!fThrown)
            {
            notCatchable(dt, errlist);
            }

        return fThrown;
        }

    /**
    * Catch the specified exception, removing all instances of it and its
    * sub-classes from the known exception list.
    *
    * @param ctx        the compiler context (used to check data types)
    * @param dt         the data type of the exception
    * @param mapThrown  the map of thrown exceptions
    *
    * @return true if the exception was thrown
    */
    protected static boolean catchException(Context ctx, DataType dt, Map mapThrown)
        {
        // structure of mapThrown:
        //  - key is data type
        //  - value is a set of expressions that throw that data type
        boolean fThrown = false;
        for (Iterator iter = mapThrown.keySet().iterator(); iter.hasNext(); )
            {
            DataType dtThrown   = (DataType) iter.next();
            TypeInfo infoThrown = ctx.getTypeInfo(dtThrown);
            if (infoThrown == null)
                {
                throw new IllegalStateException("No type info for " + dtThrown);
                }

            do
                {
                if (dt == dtThrown)
                    {
                    // exception was thrown and is now caught
                    iter.remove();
                    fThrown = true;
                    break;
                    }

                infoThrown = infoThrown.getSuperInfo();
                dtThrown   = infoThrown.getDataType();
                }
            while (infoThrown.isThrowable());
            }

        if (!fThrown)
            {
            // certain types are always assumed to be catchable
            if (   dt == DataType.THROWABLE
                || dt == DataType.EXCEPTION
                || !isChecked(ctx, dt))
                {
                fThrown = true;
                }
            }

        // 2000.04.28  cp  if a try block throws exception E1 and a catch block
        // catches exception E2, the compiler would error that E2 was not thrown
        // by the try block, even if E2 inherited from E1.
        if (!fThrown)
            {
            // check if the exception is a sub-class of one of the thrown exceptions
            DataType dtCaught   = dt;
            TypeInfo infoCaught = ctx.getTypeInfo(dtCaught);
            if (infoCaught == null)
                {
                throw new IllegalStateException("No type info for " + dtCaught);
                }

            do
                {
                if (mapThrown.containsKey(dtCaught))
                    {
                    // a super-class of the exception is thrown
                    fThrown = true;
                    break;
                    }

                infoCaught = infoCaught.getSuperInfo();
                dtCaught   = infoCaught.getDataType();
                }
            while (infoCaught.isThrowable());
            }

        return fThrown;
        }

    /**
    * Determine if the specified type is a checked exception.  Check
    * exceptions are all classes deriving from Throwable that do not
    * derive from either Error or RuntimeException.
    *
    * @param ctx  the compiler context
    * @param dt   the data type deriving from Throwable
    *
    * @return true if the type is a checked exception
    */
    protected static boolean isChecked(Context ctx, DataType dt)
        {
        final DataType THROWABLE = DataType.THROWABLE;
        final DataType ERROR     = DataType.ERROR;
        final DataType RUNTIME   = DataType.RUNTIME;

        TypeInfo info = ctx.getTypeInfo(dt);
        while (dt != THROWABLE)
            {
            if (dt == ERROR || dt == RUNTIME)
                {
                return false;
                }

            info = info.getSuperInfo();
            dt   = info.getDataType();
            }

        return true;
        }


    // ----- error logging

    /**
    * Error:  Expression does not result in a value.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notValue(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_VALUE, null, errlist);
        }

    /**
    * Error:  Expression is not constant.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notConstant(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_CONSTANT, null, errlist);
        }

    /**
    * Error:  Expression is not a primitive numeric type.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notNumeric(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_NUMERIC, null, errlist);
        }

    /**
    * Error:  Expression is not a primitive integral numeric type.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notIntegral(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_INTEGRAL, null, errlist);
        }

    /**
    * Error:  Expression is not a valid integral numeric value.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notValidIntegral(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, ILLEGAL_INTEGRAL, null, errlist);
        }

    /**
    * Error:  Expression is not a valid integral numeric value for a
    * divisor.  (Better known as "division by zero".)
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notValidDivisor(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, ILLEGAL_DIVISOR, null, errlist);
        }

    /**
    * Error:  Expression is not the boolean type.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notBoolean(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_BOOLEAN, null, errlist);
        }

    /**
    * Error:  Expression is not a reference type.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notReference(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_REFERENCE, null, errlist);
        }

    /**
    * Error:  Expression is not an array type.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notArray(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_ARRAY, null, errlist);
        }

    /**
    * Error:  Expression is not a castable to the specified type.
    *
    * @param type     the type that the expression is not castable to
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notCastable(DataType type, ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_CASTABLE, new String[]
                {this.getType().toString(), type.toString()}, errlist);
        }

    /**
    * Error:  Expression is not comparable with another data type.
    *
    * @param type     the type that the expression is not comparable to
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notComparable(DataType type, ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_COMPARABLE, new String[]
                {this.getType().toString(), type.toString()}, errlist);
        }

    /**
    * Error:  Expression is not a variable.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notAssignable(ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_VARIABLE, null, errlist);
        }

    /**
    * Error:  Expression is not assignable to another data type.
    *
    * @param type     the type that the expression is not assignable to
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notAssignable(DataType type, ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_ASSIGNABLE, new String[]
                {this.getType().toString(), type.toString()}, errlist);
        }

    /**
    * Error:  Expression is not reachable.
    *
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notReached(ErrorList errlist)
            throws CompilerException
        {
        // it is not considered an error for an expression to be unreached
        }

    /**
    * Error:  DataType is not throwable.
    *
    * @param type     the exception type
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notThrowable(DataType type, ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_THROWABLE, new String[]
                {type.toString()}, errlist);
        }

    /**
    * Error:  DataType is not catchable.
    *
    * @param type     the exception type
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notCatchable(DataType type, ErrorList errlist)
            throws CompilerException
        {
        logError(ERROR, NOT_CATCHABLE, new String[]
                {type.toString()}, errlist);
        }

    /**
    * Error:  DataType is not a data type.
    *
    * @param type     the type
    * @param errlist  the error list to log errors to
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected void notType(DataType type, ErrorList errlist)
            throws CompilerException
        {
        String sFull = type.toString();
        int    ofDot = sFull.lastIndexOf('.');
        String sPkg  = (ofDot < 0 ? "" : sFull.substring(0, ofDot));
        String sName = (ofDot < 0 ? sFull : sFull.substring(ofDot + 1));
        logError(ERROR, TYPE_NOT_FOUND, new String[] {sName, sPkg}, errlist);
        }


    // ----- support for AddExpression (String concatenation) ---------------

    /**
    * Find a static method to convert an expression to a String.
    *
    * @return a static method reference that will convert the specified
    *         type (on the stack) to a String (on the stack)
    */
    protected MethodConstant getConvertMethod()
        {
        switch (getType().getTypeString().charAt(0))
            {
            case 'Z':
                return BOOLEAN_TOSTRING;

            case 'C':
                return CHAR_TOSTRING;

            case 'B':
            case 'S':
            case 'I':
                return INT_TOSTRING;

            case 'J':
                return LONG_TOSTRING;

            case 'F':
                return FLOAT_TOSTRING;

            case 'D':
                return DOUBLE_TOSTRING;

            case 'N':
            case 'L':
            case 'R':
            case '[':
                return OBJECT_TOSTRING;

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Find a method on StringBuffer to append an expression.
    *
    * @return a StringBuffer virtual method reference that will append the
    *         specified value (which exists on the stack)
    */
    protected MethodConstant getAppendMethod()
        {
        switch (getType().getTypeString().charAt(0))
            {
            case 'Z':
                return BOOLEAN_APPEND;

            case 'C':
                return CHAR_APPEND;

            case 'B':
            case 'S':
            case 'I':
                return INT_APPEND;

            case 'J':
                return LONG_APPEND;

            case 'F':
                return FLOAT_APPEND;

            case 'D':
                return DOUBLE_APPEND;

            case 'N':
            case 'L':
            case 'R':
            case '[':
                return (getType() == DataType.STRING ? STRING_APPEND : OBJECT_APPEND);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    *
    */
    public String getStringValue()
        {
        DataType dt   = getType();
        Object   oVal = getValue();

        switch (dt.getTypeString().charAt(0))
            {
            case 'Z':
                return String.valueOf(((Boolean) oVal).booleanValue());

            case 'C':
                return String.valueOf((char) (((Number) oVal).intValue()));

            case 'B':
            case 'S':
            case 'I':
                return String.valueOf(((Number) oVal).intValue());

            case 'J':
                return String.valueOf(((Number) oVal).longValue());

            case 'F':
                return String.valueOf(((Number) oVal).floatValue());

            case 'D':
                return String.valueOf(((Number) oVal).doubleValue());

            case 'N':
            case 'L':
            case 'R':
            case '[':
                return String.valueOf(oVal);

            default:
                throw new IllegalStateException();
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The assembly constant int value of boolean false.
    */
    private static final IntConstant FALSE = Constants.CONSTANT_ICONST_0;

    /**
    * The assembly constant int value of boolean true.
    */
    private static final IntConstant TRUE  = Constants.CONSTANT_ICONST_1;


    // ----- <type>_TOSTRING conversion methods

    /**
    * The internal name of the String class.
    */
    private static final String STRING = "java/lang/String";

    /**
    * The function name that converts to String.
    */
    private static final String TOSTRING = "valueOf";

    /**
    * The method which converts a boolean to a String (on the stack).
    */
    private static final MethodConstant BOOLEAN_TOSTRING = new MethodConstant(STRING, TOSTRING, "(Z)L" + STRING + ";");

    /**
    * The method which converts a boolean to a String (on the stack).
    */
    private static final MethodConstant CHAR_TOSTRING = new MethodConstant(STRING, TOSTRING, "(C)L" + STRING + ";");

    /**
    * The method which converts a boolean to a String (on the stack).
    */
    private static final MethodConstant INT_TOSTRING = new MethodConstant(STRING, TOSTRING, "(I)L" + STRING + ";");

    /**
    * The method which converts a boolean to a String (on the stack).
    */
    private static final MethodConstant LONG_TOSTRING = new MethodConstant(STRING, TOSTRING, "(J)L" + STRING + ";");

    /**
    * The method which converts a boolean to a String (on the stack).
    */
    private static final MethodConstant FLOAT_TOSTRING = new MethodConstant(STRING, TOSTRING, "(F)L" + STRING + ";");

    /**
    * The method which converts a boolean to a String (on the stack).
    */
    private static final MethodConstant DOUBLE_TOSTRING = new MethodConstant(STRING, TOSTRING, "(D)L" + STRING + ";");

    /**
    * The method which converts a boolean to a String (on the stack).
    */
    private static final MethodConstant OBJECT_TOSTRING = new MethodConstant(STRING, TOSTRING, "(Ljava/lang/Object;)L" + STRING + ";");


    // ----- <type>_APPEND StringBuffer methods

    /**
    * The internal name of the StringBuilder class.
    */
    private static final String STRINGBUILDER = "java/lang/StringBuilder";

    /**
    * The function name that converts to String.
    */
    private static final String APPEND = "append";

    /**
    * The method which appends a boolean to a StringBuffer.
    */
    private static final MethodConstant BOOLEAN_APPEND = new MethodConstant(STRINGBUILDER, APPEND, "(Z)L" + STRINGBUILDER + ";");

    /**
    * The method which appends a char to a StringBuffer.
    */
    private static final MethodConstant CHAR_APPEND = new MethodConstant(STRINGBUILDER, APPEND, "(C)L" + STRINGBUILDER + ";");

    /**
    * The method which appends a int to a StringBuffer.
    */
    private static final MethodConstant INT_APPEND = new MethodConstant(STRINGBUILDER, APPEND, "(I)L" + STRINGBUILDER + ";");

    /**
    * The method which appends a long to a StringBuffer.
    */
    private static final MethodConstant LONG_APPEND = new MethodConstant(STRINGBUILDER, APPEND, "(J)L" + STRINGBUILDER + ";");

    /**
    * The method which appends a float to a StringBuffer.
    */
    private static final MethodConstant FLOAT_APPEND = new MethodConstant(STRINGBUILDER, APPEND, "(F)L" + STRINGBUILDER + ";");

    /**
    * The method which appends a double to a StringBuffer.
    */
    private static final MethodConstant DOUBLE_APPEND = new MethodConstant(STRINGBUILDER, APPEND, "(D)L" + STRINGBUILDER + ";");

    /**
    * The method which appends a String to a StringBuffer.
    */
    private static final MethodConstant STRING_APPEND = new MethodConstant(STRINGBUILDER, APPEND, "(L" + STRING + ";)L" + STRINGBUILDER + ";");

    /**
    * The method which appends a Object to a StringBuffer.
    */
    private static final MethodConstant OBJECT_APPEND = new MethodConstant(STRINGBUILDER, APPEND, "(Ljava/lang/Object;)L" + STRINGBUILDER + ";");


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "Expression";

    private static final DataType UNKNOWN   = DataType.UNKNOWN;
    private static final DataType VOID      = DataType.VOID;
    private static final DataType BOOLEAN   = DataType.BOOLEAN;
    private static final DataType BYTE      = DataType.BYTE;
    private static final DataType CHAR      = DataType.CHAR;
    private static final DataType SHORT     = DataType.SHORT;
    private static final DataType INT       = DataType.INT;
    private static final DataType LONG      = DataType.LONG;
    private static final DataType FLOAT     = DataType.FLOAT;
    private static final DataType DOUBLE    = DataType.DOUBLE;
    private static final DataType NULL      = DataType.NULL;
    private static final DataType OBJECT    = DataType.OBJECT;
    private static final DataType CLONEABLE = DataType.CLONEABLE;

    /**
    * The expression data type.  An unknown type can result from:
    * <ol>
    * <li> A field that cannot be resolved;
    * <li> A method that cannot be resolved;
    * <li> A type that cannot be resolved;
    * <li> A sub-expression with an UNKNOWN type.
    * </ol>
    * Since anything causing an UNKNOWN type is reported, subsequent
    * errors as a result of the type being UNKNOWN should be suppressed.
    */
    private DataType dt = UNKNOWN;

    /**
    * Discard the expression's resulting value?
    */
    private boolean fDiscard;
    }
