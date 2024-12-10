/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.OpDeclare;
import com.tangosol.dev.assembler.Ivar;
import com.tangosol.dev.assembler.Lvar;
import com.tangosol.dev.assembler.Fvar;
import com.tangosol.dev.assembler.Dvar;
import com.tangosol.dev.assembler.Avar;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ArrayList;

/**
* This class implements a Java local variable declaration statement.
*
*   LocalVariableDeclarationStatement:
*       LocalVariableDeclaration ;
*   LocalVariableDeclaration: (modified for JDK 1.1 to allow "final")
*       Modifiers-opt Type VariableDeclarators
*   VariableDeclarators:
*       VariableDeclarator
*       VariableDeclarators , VariableDeclarator
*   VariableDeclarator:
*       VariableDeclaratorId
*       VariableDeclaratorId = VariableInitializer
*   VariableDeclaratorId:
*       Identifier
*       VariableDeclaratorId [ ]
*   VariableInitializer:
*       Expression
*       ArrayInitializer
*   ArrayInitializer:
*       { VariableInitializers-opt , -opt }
*   VariableInitializers:
*       VariableInitializer
*       VariableInitializers , VariableInitializer
*
* Before precompilation, the DeclarationStatement can provide:
*   - The type of the declaration as parsed
*   - Whether the declaration is a parameter
*   - Whether the declaration is final
*   - What declarators are present
*
* A declarator is an expression.  Considering various types of declarations,
* a declarator will always fall into one of the following categories:
*
* Category      Examples    Description
* ------------  ----------  -------------------------------------------------
* Name          i           A simple variable without an initializer
* Dim'd Expr    i[]         A dimensioned variable
* Assign Expr   i=0         A simple variable with an initializer
*               i[]={0,1}   A dimensioned variable with an initializer
*
* After pre-compilation, the DeclarationStatement can provide:
*   - What variable(s) are declared
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class DeclarationStatement extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a declaration statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    public DeclarationStatement(Statement stmt, Token token)
        {
        super(stmt, token);
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
        // JLS 16.2.3 Local Variable Declaration Statements
        //  - V is definitely assigned after a local variable declaration
        //    statement that contains no initializers iff it is definitely
        //    assigned before the local variable declaration statement.
        //  - V is definitely assigned after a local variable declaration
        //    statement that contains initializers iff either it is
        //    definitely assigned after the last initializer expression in
        //    the local variable declaration statement or the last
        //    initializer expression in the declaration is in the declarator
        //    that declares V.
        //  - V is definitely assigned before the first initializer
        //    expression iff it is definitely assigned before the local
        //    variable declaration statement.
        //  - V is definitely assigned before any other initializer
        //    expression e iff either it is definitely assigned after the
        //    initializer expression immediately preceding e in the local
        //    variable declaration statement or the initializer expression
        //    immediately preceding e in the local variable declaration
        //    statement is in the declarator that declares V.

        // pre-compile the type expression (to validate its type information)
        TypeExpression exprType = this.exprType;
        exprType = (TypeExpression) exprType.precompile(ctx, null, null, null, errlist);
        this.exprType = exprType;

        // variable declarations must be immediately contained by a block
        Block block = getBlock();
        if (block != getOuterStatement())
            {
            logError(ERROR, DECL_NOT_IMMED, null, errlist);
            }

        // get the type of the declaration statement (which may not be the
        // type of each declarator)
        DataType dtStmt = exprType.getType();
        boolean  fFinal = isFinal();
        boolean  fParam = isParameter();

        // process each declarator
        List listVars = new ArrayList();
        for (Iterator iter = getDeclarators(); iter.hasNext(); )
            {
            ExpressionStatement stmtDeclarator  = (ExpressionStatement) iter.next();
            AssignExpression    exprAssignment  = null;
            Expression          exprDeclarator  = (Expression) stmtDeclarator.getExpression();
            Expression          exprInitializer = null;
            DataType            dtDeclarator    = dtStmt;

            // unwrap any initializer
            if (exprDeclarator instanceof AssignExpression)
                {
                exprAssignment  = (AssignExpression) exprDeclarator;
                exprDeclarator  = exprAssignment.getLeftExpression();
                exprInitializer = exprAssignment.getRightExpression();
                }

            // unwrap any dims ("[]")
            boolean fDimensioned = false;
            while (exprDeclarator instanceof DimensionedExpression)
                {
                dtDeclarator   = dtDeclarator.getArrayType();
                exprDeclarator = ((DimensionedExpression) exprDeclarator).getTypeExpression();
                fDimensioned   = true;
                }

            if (fDimensioned && exprDeclarator instanceof TypeExpression)
                {
                // the name was accidently parsed as a type because it was
                // followed by brackets
                exprType = (TypeExpression) exprDeclarator;
                if (exprType.getNameExpression() != null)
                    {
                    exprDeclarator = exprType.getNameExpression();
                    }
                }

            // the remaining expression must be a simple name
            if (!(exprDeclarator instanceof NameExpression))
                {
                exprDeclarator.logError(ERROR, DECL_BAD_NAME, null, errlist);
                continue;
                }

            NameExpression exprName = (NameExpression) exprDeclarator;
            if (exprName.isQualified())
                {
                exprDeclarator.logError(ERROR, DECL_BAD_NAME, null, errlist);
                continue;
                }

            // create and register the variable
            String sName = exprName.getName();
            Variable var = new Variable(block, sName, dtDeclarator, fParam, fFinal);
            if (!block.registerVariable(var))
                {
                // this variable name hides another variable
                logError(ERROR, VAR_DUPLICATE, new String[] {var.getName()}, errlist);
                }

            // if it is a parameter, then it is assigned, otherwise ...
            if (fParam)
                {
                if (fFinal)
                    {
                    // potentially assigned (actually, all parameters are
                    // definitely assigned)
                    setFVars.add(var);
                    }
                }
            else
                {
                // local variables are potentially unassigned
                setUVars.add(var);
                }

            // process the declarator initializer
            if (exprAssignment != null)
                {
                // update the left side of the assign expression
                exprAssignment.setLeftExpression(exprName);

                // if an array initializer is used, its type is determined
                // by the variable declaration
                if (dtDeclarator.isArray() && exprInitializer instanceof ArrayExpression)
                    {
                    ((ArrayExpression) exprInitializer).setType(dtDeclarator);
                    }

                // pre-compile the assignment statement
                stmtDeclarator.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

                // final variables may have a determinable constant value
                if (fFinal)
                    {
                    Expression exprValue = ((AssignExpression)
                            stmtDeclarator.getExpression()).getRightExpression();
                    if (exprValue.isConstant())
                        {
                        var.setValue(exprValue.getValue());
                        }
                    }
                }

            listVars.add(var);
            }

        avar = (Variable[]) listVars.toArray(NO_VARS);

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
        // compilation for variable declaration:
        //  start:
        //          ?var
        //          [init-opt]
        //  end:
        for (Iterator iterDecl = getDeclarators(), iterVars = getVariables(); iterDecl.hasNext(); )
            {
            ExpressionStatement stmt = (ExpressionStatement) iterDecl.next();
            Variable            var  = (Variable           ) iterVars.next();

            String   sName = var.getName();
            DataType dt    = var.getType();

            OpDeclare op;
            switch (dt.getTypeString().charAt(0))
                {
                case 'Z':
                case 'B':
                case 'C':
                case 'S':
                case 'I':
                    op = new Ivar(sName, dt.getJVMSignature());
                    break;

                case 'J':
                    op = new Lvar(sName);
                    break;

                case 'F':
                    op = new Fvar(sName);
                    break;

                case 'D':
                    op = new Dvar(sName);
                    break;

                case 'L':
                case 'R':
                case '[':
                    op = new Avar(sName, dt.getJVMSignature());
                    break;

                default:
                    throw new IllegalStateException();
                }
            code.add(op);

            // store the declaring op with the variable as well; any
            // expression that uses the variable will need it
            var.setOp(op);

            if (stmt.getExpression() instanceof AssignExpression)
                {
                stmt.compile(ctx, code, fReached, errlist);
                }
            }

        // JLS 14.19: A local variable declaration statement can complete
        // normally iff it is reach-able.
        return fReached;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine if the declaration is for a parameter.
    *
    * @return true if this declaration statement declares a parameter
    */
    public boolean isParameter()
        {
        return fParam;
        }

    /**
    * Specify that the declaration is for a parameter.
    *
    * @param fParam  true if this declaration statement declares a parameter
    */
    protected void setParameter(boolean fParam)
        {
        this.fParam = fParam;
        }

    /**
    * Determine if the variable is declared as final.
    *
    * @return true if this declaration statement declares a final variable
    */
    public boolean isFinal()
        {
        // currently only modifier supported is final
        return tokMod != null; // && mod.getID() == TokenConstants.KEY_FINAL;
        }

    /**
    * Get the declaration modifier if any.
    *
    * @return the declaration modifier or null
    */
    public Token getModifier()
        {
        return tokMod;
        }

    /**
    * Specify the declaration modifier.
    *
    * @param tokMod  a modifier
    */
    protected void setModifier(Token tokMod)
        {
        this.tokMod = tokMod;
        }

    /**
    * Get the declaration type.
    *
    * @return the declaration type
    */
    public TypeExpression getTypeExpression()
        {
        return exprType;
        }

    /**
    * Set the declaration type.
    *
    * @param exprType  the declaration type
    */
    protected void setTypeExpression(TypeExpression exprType)
        {
        this.exprType = exprType;
        }

    /**
    * Determine if only one variable is being declared.
    *
    * @return true if only one variable is being declared
    */
    public boolean isSingleDeclaration()
        {
        // declarations are organized as inner expression statements
        return getInnerStatement().getNextStatement() == null;
        }

    /**
    * Determine if there is assignment as part of the declaration.
    *
    * @return true if there is assignment as part of the declaration
    */
    public boolean isAssignmentDeclaration()
        {
        ExpressionStatement stmt = (ExpressionStatement) getInnerStatement();
        while (stmt != null)
            {
            if (stmt.getExpression() instanceof AssignExpression)
                {
                return true;
                }

            stmt = (ExpressionStatement) stmt.getNextStatement();
            }

        return false;
        }

    /**
    * Enumerate the declarators of the local variable declaration statement.
    *
    * @return an iterator of declarator statements
    */
    protected Iterator getDeclarators()
        {
        return new Iterator()
            {
            /**
            * The next statement containing a declarator expression.
            */
            private Statement stmt = getInnerStatement();

            /**
            * @return true if there are more declarators
            */
            public boolean hasNext()
                {
                return stmt != null;
                }

            /**
            * @return the next declarator expression
            */
            public Object next()
                {
                Statement stmtRet = stmt;

                if (stmtRet == null)
                    {
                    throw new NoSuchElementException();
                    }

                stmt = stmtRet.getNextStatement();
                return stmtRet;
                }

            /**
            * Immutable iterator.
            */
            public void remove()
                {
                throw new UnsupportedOperationException();
                }
            };
        }

    /**
    * After pre-compilation, the declaration statement variables.
    *
    * @return an iterator of variables declared by the declaration statement
    */
    public Iterator getVariables()
        {
        return new Iterator()
            {
            /**
            * The next variable.
            */
            private int i = 0;

            /**
            * @return true if there are more variables
            */
            public boolean hasNext()
                {
                return i < avar.length;
                }

            /**
            * @return the next variable
            */
            public Object next()
                {
                try
                    {
                    return avar[i++];
                    }
                catch (RuntimeException e)
                    {
                    throw new NoSuchElementException();
                    }
                }

            /**
            * Immutable iterator.
            */
            public void remove()
                {
                throw new UnsupportedOperationException();
                }
            };
        }

    /**
    * The first variable.
    *
    * @return the first variable declared by the declaration statement
    */
    protected Variable getVariable()
        {
        return avar[0];
        }


    // ----- Element methods ------------------------------------------------

    /**
    * Print the statement information.
    *
    * @param sIndent
    */
    public void print(String sIndent)
        {
        out(sIndent + toString()
            + " (" + (tokMod != null ? tokMod.text + " " : "")
            + (fParam ? "parameter" : "local") + ")");

        if (exprType != null)
            {
            out(sIndent + "  Type:");
            exprType.print(sIndent + "    ");
            }

        Statement inner = getInnerStatement();
        if (inner != null)
            {
            out(sIndent + "  Declarations:");
            inner.printList(sIndent + "    ");
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "DeclarationStatement";

    /**
    * True if a parameter, false otherwise.
    */
    private boolean fParam;

    /**
    * The final modifier, if any.
    */
    private Token tokMod;

    /**
    * The type of the declaration.
    */
    private TypeExpression exprType;

    /**
    * An empty array of variables.
    */
    private static final Variable[] NO_VARS = new Variable[0];

    /**
    * The declared variables.
    */
    private Variable[] avar = NO_VARS;
    }
