/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.compiler.PackageInfo;
import com.tangosol.dev.compiler.TypeInfo;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* This class implements a Java script TypeExpression.
*
* @version 1.00, 09/16/98
* @author  Cameron Purdy
*/
public class TypeExpression extends Expression implements TokenConstants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a TypeExpression.
    *
    * @param block    the block (scope) within which this element exists
    * @param tokType  the simple type (boolean, byte, char, short, etc.)
    */
    public TypeExpression(Block block, Token tokType)
        {
        super(block, tokType);
        setEndToken(tokType);

        DataType type;
        switch (tokType.getID())
            {
            case KEY_BOOLEAN:
                type = DataType.BOOLEAN;
                break;

            case KEY_BYTE:
                type = DataType.BYTE;
                break;

            case KEY_CHAR:
                type = DataType.CHAR;
                break;

            case KEY_SHORT:
                type = DataType.SHORT;
                break;

            case KEY_INT:
                type = DataType.INT;
                break;

            case KEY_LONG:
                type = DataType.LONG;
                break;

            case KEY_FLOAT:
                type = DataType.FLOAT;
                break;

            case KEY_DOUBLE:
                type = DataType.DOUBLE;
                break;

            default:
                throw new IllegalStateException();
            }
        setType(type);
        }

    /**
    * Construct a TypeExpression from a NameExpression.
    *
    * @param expr  the name expression
    */
    public TypeExpression(NameExpression expr)
        {
        super(expr.getBlock(), expr.getStartToken());
        setEndToken(expr.getEndToken());

        exprName = expr;
        }

    /**
    * Construct a TypeExpression from a DataType.  This constructor is used
    * by CastAssignExpression and SuperExpression.
    *
    * @param block  the block (scope) within which this element exists
    * @param token  the token starting this expression
    * @param type   the string containing the unresolved type name
    */
    protected TypeExpression(Block block, Token token, DataType type)
        {
        super(block, token);

        setType(type);
        }

    /**
    * Construct a TypeExpression.  Used by derived expressions.
    *
    * @param block     the block (scope) within which this element exists
    * @param tokStart  the first token
    * @param tokEnd    the last token
    */
    protected TypeExpression(Block block, Token tokStart, Token tokEnd)
        {
        super(block, tokStart);

        if (tokStart != tokEnd && tokEnd != null)
            {
            setEndToken(tokEnd);
            }
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
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Element precompile(Context ctx, DualSet setUVars, DualSet setFVars, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        TypeInfo type = null;
        DataType dt   = getType();

        if (dt == UNKNOWN)
            {
            // bind type from name
            NameExpression exprName = this.exprName;
            if (!exprName.isQualified())
                {
                // JLS 6.5.4.1 Simple Type Names
                // If a type name consists of a single Identifier, then the
                // identifier must occur in the scope of a declaration of a
                // type with this name, or a compile-time error occurs. It
                // is possible that the identifier occurs within the scope
                // of more than one type with that name, in which case the
                // type denoted by the name is determined as follows:
                //  1)  If a type with that name is declared in the current
                //      compilation unit (7.3), either by a single-type-
                //      import declaration (7.5.1) or by a declaration of
                //      a class or interface type (7.6), then the simple
                //      type name denotes that type.
                //  2)  Otherwise, if a type with that name is declared in
                //      another compilation unit (7.3) of the package (7.1)
                //      containing the identifier, then the identifier
                //      denotes that type. Note that, in systems that store
                //      compilation units in a file system, such a
                //      compilation unit must have a file name that is the
                //      name of the type (7.6).
                //  3)  Otherwise, if a type of that name is declared by
                //      exactly one type-import-on-demand declaration
                //      (7.5.2) of the compilation unit containing the
                //      identifier, then the simple type name denotes that
                //      type.
                //  4)  Otherwise, if a type of that name is declared by
                //      more than one type-import-on-demand declaration of
                //      the compilation unit, then the name is ambiguous as
                //      a type name; a compile-time error occurs.
                //  5)  Otherwise, the name is undefined as a type name; a
                //      compile-time error occurs.
                //
                // Note:  Items 3 and 4 above are not applicable due to the
                //        purposeful omission of type-import-on-demand
                //        statements from the Java scripting language
                // Note:  Allowance is made for a simple name referring to
                //        a packageless type

                // JLS-2ed 6.5.5.1 Simple Type Names
                //  1)  If the simple type name occurs within the scope of a
                //      visible local class declaration (14.3) with that
                //      name, then the simple type name denotes that local
                //      class type.
                //  2)  Otherwise, if the simple type name occurs within the
                //      scope of exactly one visible member type (8.5, 9.5),
                //      then the simple type name denotes that member type.
                //  3)  Otherwise, if the simple type name occurs within the
                //      scope of more than one visible member type, then the
                //      name is ambiguous as a type name; a compile-time
                //      error occurs.
                //  4)  Otherwise, if a type with that name is declared in
                //      the current compilation unit (7.3), either by a
                //      single-type-import declaration (7.5.1) or by a
                //      declaration of a class or interface type (7.6), then
                //      the simple type name denotes that type.
                //  5)  Otherwise, if a type with that name is declared in
                //      another compilation unit (7.3) of the package (7.1)
                //      containing the identifier, then the identifier
                //      denotes that type.
                //  6)  Otherwise, if a type of that name is declared by
                //      exactly one type-import-on-demand declaration (7.5.2)
                //      of the compilation unit containing the identifier,
                //      then the simple type name denotes that type.
                //  7)  Otherwise, if a type of that name is declared by more
                //      than one type-import-on-demand declaration of the
                //      compilation unit, then the name is ambiguous as a
                //      type name; a compile-time error occurs.
                //  8)  Otherwise, the name is undefined as a type name; a
                //      compile-time error occurs.
                //
                // Note:  This is the same list except that three rules were
                //        inserted at the front of the name resolution rules.
                // Note:  Item 1 is not applicable; we do not support local
                //        classes.

                Token  tokName = exprName.getToken(0);
                String sName   = tokName.getText();

                // TODO JLS-2ed mods

                do
                    {
                    // check if the name imports a type
                    dt = ctx.getImport(sName);
                    if (dt == null)
                        {
                        dt = UNKNOWN;
                        }
                    else
                        {
                        type = ctx.getTypeInfo(dt);
                        break;
                        }

                    // check if the name is the current type
                    type = ctx.getMethodInfo().getTypeInfo();
                    if (type.getName().equals(sName))
                        {
                        break;
                        }

                    // check if the name exists in the current package
                    type = type.getPackageInfo().getTypeInfo(sName);
                    }
                while (false);
                }

            if (type == null && dt == UNKNOWN)
                {
                do
                    {
                    // fully qualified and packageless type names
                    type = ctx.getTypeInfo(exprName.getName());
                    if (type != null)
                        {
                        break;
                        }

                    // the rest of the name lookups are relative to an
                    // import or to the current context, and unqualified
                    // names are already processed
                    if (!exprName.isQualified())
                        {
                        break;
                        }

                    String sName     = exprName.getName();
                    int    ofDot     = sName.indexOf('.');
                    String sBaseName = sName.substring(0, ofDot);
                    String sRelName  = sName.substring(ofDot);

                    // check if the name imports a type
                    DataType dtImport = ctx.getImport(sBaseName);
                    if (dtImport != null && dtImport.isComponent())
                        {
                        type = ctx.getTypeInfo(dtImport.getComponentName() + sRelName);
                        if (type != null)
                            {
                            break;
                            }
                        }

                    // check if the name is relative to this package
                    DataType dtThis    = ctx.getMethodInfo().getTypeInfo().getDataType();
                    String   sThisType = dtThis.isComponent() ? dtThis.getComponentName() : dtThis.getClassName();
                    String   sThisPkg  = sThisType.substring(0, sThisType.indexOf('.'));
                    type = ctx.getTypeInfo(sThisPkg + '.' + sName);
                    }
                while (false);
                }

            if (type == null)
                {
                if (dt != UNKNOWN)
                    {
                    // the type was imported but cannot be found
                    exprName.logError(ERROR, IMPORT_NOT_FOUND, new String[]
                            {dt.isComponent() ? dt.getComponentName()
                                              : dt.getClassName()}, errlist);
                    }
                else
                    {
                    int c = exprName.getTokenCount() - 1;
                    PackageInfo pkg;
                    if (c > 0)
                        {
                        // determine what is wrong with the type name
                        pkg = ctx.getPackageInfo("");
                        for (int i = 0; i < c; ++i)
                            {
                            Token tokName = exprName.getToken(i);
                            pkg = pkg.getPackageInfo(tokName.getText());
                            if (pkg == null)
                                {
                                tokName.logError(ERROR, PKG_NOT_FOUND, new String[]
                                        {tokName.getText()}, errlist);
                                break;
                                }
                            }
                        }
                    else
                        {
                        pkg = ctx.getMethodInfo().getTypeInfo().getPackageInfo();
                        }

                    if (pkg != null)
                        {
                        String sPkg = "";
                        while (pkg != null)
                            {
                            sPkg = "." + pkg.getName() + sPkg;
                            pkg = pkg.getPackageInfo();
                            }

                        Token tokName = exprName.getToken(c);
                        tokName.logError(ERROR, TYPE_NOT_FOUND, new String[]
                                {tokName.getText(), sPkg.substring(1)}, errlist);
                        }
                    }

                dt = UNKNOWN;
                }
            else
                {
                // verify that the specified type is accessible
                dt = type.getDataType();
                if (!type.isAccessible())
                    {
                    exprName.logError(ERROR, TYPE_NO_ACCESS, new String[]
                            {dt.isComponent() ? dt.getComponentName()
                                              : dt.getClassName()}, errlist);
                    dt = UNKNOWN;
                    }
                }

            setType(dt);
            }

        // register runtime dependency
        addDependency(ctx, dt);

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
    protected boolean compileImpl(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        // TypeExpressions are not compilable
        throw new IllegalStateException();
        }

    /**
    * Register a dependency.
    *
    * @param ctx       compiler context
    * @param dt        data type to register dependency against
    * @param fRuntime  true for a runtime dependency, false for a compile dependency
    */
    protected void addDependency(Context ctx, DataType dt)
            throws CompilerException
        {
        if (dt.isArray())
            {
            dt = dt.getBaseElementType();
            }

        TypeInfo type = null;
        if (dt.isComponent())
            {
            type = ctx.getTypeInfo(dt.getComponentName());
            }
        else if (dt.isClass())
            {
            type = ctx.getTypeInfo(dt.getClassName());
            }

        if (type != null)
            {
            type.addDependency(true, getStartLine(), getStartOffset(), getEndLine(), getEndOffset());
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the type name.
    *
    * @return the type name if available
    */
    protected NameExpression getNameExpression()
        {
        return exprName;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the element information as a string.
    *
    * @return a human-readable description of the element
    */
    public String toString()
        {
        String s = super.toString();
        DataType type = getType();
        if (type != null && (exprName == null || type != UNKNOWN))
            {
            s += " type=" + type.toString();
            }
        else if (exprName != null)
            {
            s += " name=" + exprName.getName();
            }
        return s;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "TypeExpression";

    /**
    * Unknown data type.
    */
    private static final DataType UNKNOWN = DataType.UNKNOWN;

    /**
    * The name of the data type (if the type has not been resolved).
    */
    private NameExpression exprName;
    }
