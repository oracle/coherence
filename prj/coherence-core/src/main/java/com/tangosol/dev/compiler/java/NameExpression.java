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
import com.tangosol.dev.compiler.FieldInfo;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;


/**
* Implements a simple or qualified name expression.  This class is basically
* the "ambiguous name" referred to in the Java Language Specification.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class NameExpression extends Expression implements TokenConstants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a NameExpression.
    *
    * @param block    the containing block
    * @param tokName  the simple name
    */
    public NameExpression(Block block, Token tokName)
        {
        super(block, tokName);
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
        Expression expr       = this;
        boolean    fQualified = isQualified();
        Block      block      = getBlock();
        Token      tokName    = getStartToken();
        String     sName      = tokName.getText();

        if (fQualified)
            {
            // JLS 6.5.2 Reclassification of Contextually Ambiguous Names
            // If the AmbiguousName is a qualified name, consisting of a
            // name, a ".", and an Identifier, then the name to the left
            // of the "." is first reclassified, for it is itself an
            // AmbiguousName. There is then a choice:
            //  1)  If the name to the left of the "." is reclassified as
            //      a PackageName, then there is a further choice:
            //      1)  If there is a package whose name is the name to
            //          the left of the "." and that package contains a
            //          declaration of a type whose name is the same as the
            //          Identifier, then this AmbiguousName is reclassified
            //          as a TypeName.
            //      2)  Otherwise, this AmbiguousName is reclassified as a
            //          PackageName. A later step determines whether or not
            //          a package of that name actually exists.
            //  2)  If the name to the left of the "." is reclassified as a
            //      TypeName, then this AmbiguousName is reclassified as an
            //      ExpressionName.
            //  3)  If the name to the left of the "." is reclassified as an
            //      ExpressionName, then this AmbiguousName is reclassified
            //      as an ExpressionName.
            //
            // Translation:
            //  1)  Process each portion of the name, left to right.
            //  2)  As soon as some portion of the name is known to be a
            //      field or variable, then all subsequent portions of the
            //      name are considered to be field expressions.
            //  3)  Otherwise, if the name resolves to a class type, then
            //      all subsequent portions of the name are considered to
            //      be field expressions.
            //  4)  Otherwise, if the name resolves to a component type,
            //      and subsequent portions exist, then it cannot be
            //      determined yet whether those portions of the name
            //      thus far processed represent a type or a package.

            List listNames = this.listNames;
            int  cNames    = listNames.size();
            int  iName     = 0;

            // check if the very first part of the name is a variable or
            // field, which means that the rest are field expressions
            Variable var;
            FieldInfo field;
            if ((var = block.getVariable(sName)) != null)
                {
                VariableExpression exprVar = new VariableExpression(block, tokName);
                exprVar.setAssignee(fAssignee);
                expr = exprVar;

                // next portion of the name is a field accessor
                iName = 1;
                }
            else if ((field = ctx.getMethodInfo().getTypeInfo().getFieldInfo(sName)) != null
                    && !field.isViaAccessor())
                {
                // a field accessor (implied "this" or this class name);
                Token tokThis  = new Token(Token.TOK_THIS,
                        tokName.getLine(), tokName.getOffset(), 0);
                Token tokDot = new Token(Token.TOK_DOT,
                        tokName.getLine(), tokName.getOffset(), 0);
                expr = new FieldAccessExpression(tokDot,
                        new ThisExpression(block, tokThis), tokName);

                // next portion of the name is a field accessor
                iName = 1;
                }
            else
                {
                // the first n portions of the name identify a type; the type
                // extent (n) is determined when:
                //  1)  the type name identifies a class type
                //  2)  the next portion is a field
                // an error occurs when:
                //  1)  all portions have been processed and a type has not been
                //      identified (i.e. the name refers to a package)

                // verify that the first portion of the name is a type or a
                // package; otherwise assume the first portion was supposed
                // to be a variable name
                PackageInfo pkg      = null;
                TypeInfo    type     = null;

                // build a list of names that represents just the type
                // portion(s) of the name
                List listType = new ArrayList();

                // check if the name imports a type
                DataType dt = ctx.getImport(sName);
                if (dt != null)
                    {
                    // name is imported; look up the type info
                    type = ctx.getTypeInfo(dt);
                    if (type == null)
                        {
                        // the type was imported but cannot be found
                        tokName.logError(ERROR, IMPORT_NOT_FOUND, new String[]
                                {dt.isComponent() ? dt.getComponentName()
                                                  : dt.getClassName()}, errlist);
                        }
                    }
                // check:
                //  1)  if the name is a type in this package
                //  2)  if the name is a type in the root package (i.e. the
                //      simple name is fully qualified)
                //  3)  if the name is a package name
                else if ((type = ctx.getMethodInfo().getTypeInfo().getPackageInfo()
                                    .getTypeInfo   (sName)) == null
                      && (type = ctx.getTypeInfo   (sName)) == null
                      && (pkg  = ctx.getPackageInfo(sName)) == null)
                    {
                    // assume it was supposed to be a variable name
                    logError(ERROR, VAR_NOT_FOUND, new String[] {sName}, errlist);
                    }

                // if the name found either (or both) a type or a package,
                // then determine at which point (if any) the name refers
                // to a field
                if (pkg != null || type != null)
                    {
                    if (type != null)
                        {
                        // check if the type is also a package
                        pkg = type.getPackageInfo().getPackageInfo(sName);
                        }

                    while (true)
                        {
                        // add the name portion as a part of the type
                        listType.add(tokName);

                        // advance to next portion of the name
                        ++iName;
                        tokName = (Token) listNames.get(iName);
                        sName   = tokName.getText();

                        // check if name is a field
                        if (type != null && (field = type.getFieldInfo(sName)) != null
                                && !field.isViaAccessor())
                            {
                            // store portion(s) of name that is type as the
                            // new name (the rest of the name becomes field
                            // access expressions)
                            this.listNames = listType;
                            expr = new TypeExpression(this);
                            break;
                            }

                        // check if name is still a package and/or a type
                        if (pkg == null)
                            {
                            // the current name cannot specify a type
                            // because the previous name didn't specify
                            // a package
                            type = null;
                            }
                        else
                            {
                            type = pkg.getTypeInfo   (sName);
                            pkg  = pkg.getPackageInfo(sName);
                            }

                        if (pkg == null && type == null)
                            {
                            // the portion of the name processed so far
                            // is neither a package nor a type so the
                            // name can be neither a type nor a field
                            break;
                            }

                        if (iName == cNames - 1)
                            {
                            if (type == null)
                                {
                                // out of name portions and the name is not a
                                // type (strangely enough, it is a package)
                                break;
                                }
                            else
                                {
                                // the entire name expression is a type
                                iName = cNames;
                                expr  = new TypeExpression(this);
                                break;
                                }
                            }
                        }

                    if (expr == this)
                        {
                        // assemble name of type or package that was verified
                        // to exist
                        StringBuffer sb = new StringBuffer();
                        int c = listType.size();
                        for (int i = 0; i < c; ++i)
                            {
                            if (i > 0)
                                {
                                sb.append('.');
                                }
                            sb.append(((Token) listType.get(i)).getText());
                            }
                        String sTypeOrPkg = sb.toString();

                        if (ctx.getTypeInfo(sTypeOrPkg) != null)
                            {
                            logError(ERROR, FIELD_NOT_FOUND, new String[]
                                    {sName, sTypeOrPkg}, errlist);
                            }
                        else
                            {
                            logError(ERROR, TYPE_NOT_FOUND, new String[]
                                    {sName, sTypeOrPkg}, errlist);
                            }
                        }
                    }
                }

            if (expr != this)
                {
                // convert the remainder of the name to field expressions
                for (; iName < cNames; ++iName)
                    {
                    // the field name
                    tokName = (Token) listNames.get(iName);

                    // fake the dot (.field)
                    Token tokDot = new Token(Token.TOK_DOT,
                            tokName.getLine(), tokName.getOffset(), 0);

                    expr = new FieldAccessExpression(tokDot, expr, tokName);
                    }
                }
            }
        else
            {
            // JLS 6.5.2 Reclassification of Contextually Ambiguous Names
            // If the AmbiguousName is a simple name, consisting of a single
            // Identifier:
            //  1)  If the Identifier appears within the scope (6.3) of a
            //      local variable declaration (14.3) or parameter
            //      declaration (8.4.1, 8.6.1, 14.18) with that name, then
            //      the AmbiguousName is reclassified as an ExpressionName.
            //  2)  Otherwise, consider the class or interface C within whose
            //      declaration the Identifier occurs. If C has one or more
            //      fields with that name, which may be either declared
            //      within it or inherited, then the AmbiguousName is
            //      reclassified as an ExpressionName.
            //  3)  Otherwise, if a type of that name is declared in the
            //      compilation unit (7.3) containing the Identifier, either
            //      by a single-type-import declaration (7.5.1) or by a
            //      class or interface type declaration (7.6), then the
            //      AmbiguousName is reclassified as a TypeName.
            //  4)  Otherwise, if a type of that name is declared in another
            //      compilation unit (7.3) of the package (7.1) of the
            //      compilation unit containing the Identifier, then the
            //      AmbiguousName is reclassified as a TypeName.
            //  5)  Otherwise, if a type of that name is declared by exactly
            //      one type-import-on-demand declaration (7.5.2) of the
            //      compilation unit containing the Identifier, then the
            //      AmbiguousName is reclassified as a TypeName.
            //  6)  Otherwise, if a type of that name is declared by more
            //      than one type-import-on-demand declaration of the
            //      compilation unit containing the Identifier, then a
            //      compile-time error results.
            //  7)  Otherwise, the AmbiguousName is reclassified as a
            //      PackageName. A later step determines whether or not a
            //      package of that name actually exists.
            //
            // Note:  Rule 5 is not applicable since type-import-on-demand
            //        declarations are not supported.
            // Note:  Rule 6 is not applicable for the same reason.
            // Note:  Rule 7 is not applicable because there are no
            //        constructs within which a package name would be legal.
            //        In other words, a NameExpression, by its context, must
            //        always resolve to a type or a variable/value.

            // check if the name is a variable
            Variable  var;
            FieldInfo field;
            if ((var = block.getVariable(sName)) != null)
                {
                VariableExpression exprVar = new VariableExpression(block, tokName);
                exprVar.setAssignee(fAssignee);
                expr = exprVar;
                }
            // check if the name is a field
            else if ((field = ctx.getMethodInfo().getTypeInfo().getFieldInfo(sName)) != null
                    && !field.isViaAccessor())
                {
                // a field accessor (implied "this" or this class name);
                // replace with a field access expression against an
                // implied this
                Token tokThis  = new Token(Token.TOK_THIS,
                        tokName.getLine(), tokName.getOffset(), 0);
                Token tokDot = new Token(Token.TOK_DOT,
                        tokName.getLine(), tokName.getOffset(), 0);
                expr = new FieldAccessExpression(tokDot,
                        new ThisExpression(block, tokThis), tokName);
                }
            // check if the name imports a type
            else if (ctx.getImport(sName) != null)
                {
                expr = new TypeExpression(this);
                }
            // check if the name is a type in this package
            else if (ctx.getMethodInfo().getTypeInfo().getPackageInfo().getTypeInfo(sName) != null)
                {
                expr = new TypeExpression(this);
                }
            // check if the name is a type in the root package (i.e. the
            // simple name is fully qualified)
            else if (ctx.getTypeInfo(sName) != null)
                {
                expr = new TypeExpression(this);
                }
            // assume it was supposed to be a variable name
            else
                {
                logError(ERROR, VAR_NOT_FOUND, new String[] {sName}, errlist);
                }
            }

        if (expr != this)
            {
            expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);
            }

        return expr;
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
        throw new IllegalStateException();
        }


    // ----- Expression methods ---------------------------------------------

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


    // ----- accessors ------------------------------------------------------

    /**
    * Get the name.
    *
    * @return  the name
    */
    public String getName()
        {
        String sName;

        if (isQualified())
            {
            StringBuffer sb = new StringBuffer();
            List listNames = this.listNames;
            int c = listNames.size();
            for (int i = 0; i < c; ++i)
                {
                if (i > 0)
                    {
                    sb.append('.');
                    }
                sb.append(((Token) listNames.get(i)).getText());
                }
            sName = sb.toString();
            }
        else
            {
            sName = getStartToken().getText();
            }

        return sName;
        }

    /**
    * Determine how many parts are in the name expression.  One part is
    * in a simple name expression; more than one part is in a qualified
    * name expression.
    *
    * @return  the number of parts in the name expression
    */
    public int getTokenCount()
        {
        return listNames == null ? 1 : listNames.size();
        }

    /**
    * Look up the i-th name in the name expression.
    *
    * @param i  zero-based index
    *
    * @return  the token holding the i-th portion of the name expression
    */
    public Token getToken(int i)
        {
        return listNames == null ? getStartToken() : (Token) listNames.get(i);
        }

    /**
    * Determine if the name is qualified.
    *
    * @return  true if the name is qualified
    */
    public boolean isQualified()
        {
        List listNames = this.listNames;
        return listNames != null && listNames.size() > 1;
        }

    /**
    * Add a name to the name expression.  For example, adding "b" to "a"
    * results in "a.b".
    *
    * @param tokName  the name token to add
    */
    protected void addName(Token tokName)
        {
        List listNames = this.listNames;

        if (listNames == null)
            {
            this.listNames = listNames = new ArrayList();
            listNames.add(getStartToken());
            }

        // update end token
        setEndToken(tokName);

        // add to list
        listNames.add(tokName);
        }

    /**
    * Remove a name from the name expression.  For example, removing from
    * "a.b" returns "b" and results in "a".
    *
    * @return the token for the last simple part of the name
    */
    protected Token removeName()
        {
        List listNames = this.listNames;
        if (listNames != null)
            {
            int iName = listNames.size() - 1;

            // update end token (if any tokens will remain)
            if (iName >= 1)
                {
                setEndToken((Token) listNames.get(iName-1));
                }

            // remove from list and return
            return (Token) listNames.remove(iName);
            }
        else
            {
            return getStartToken();
            }
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


    // ----- Object methods -------------------------------------------------

    /**
    * Format the element information as a string.
    *
    * @return a human-readable description of the element
    */
    public String toString()
        {
        return super.toString() + " " + getName();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "NameExpression";

    /**
    * Qualified names are stored as an array of tokens.
    */
    private List listNames;

    /**
    * If this name expression is used as a "left-hand-side" of an assignment
    * operation.
    */
    private boolean fAssignee;
    }
