/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Return;

import com.tangosol.dev.compiler.CompilerErrorInfo;
import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.SyntaxException;
import com.tangosol.dev.compiler.Context;
import com.tangosol.dev.compiler.TypeInfo;
import com.tangosol.dev.compiler.MethodInfo;
import com.tangosol.dev.compiler.ParamInfo;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;
import com.tangosol.util.NullImplementation;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.HashMap;


/**
* This class implements the Java script compiler.
*
*   1.  The Java script is passed as a string.  The first step is to create
*       a character stream (using the Script interface) which understands
*       Unicode escape sequences; this is done within the Tokenizer by using
*       the UnicodeScript class.
*
*   2.  The second step is to lexically analyze and parse the Java script.
*       This is done by the Tokenizer class.
*
*   3.  The third step is to parse the tokens into a parse tree.  This is
*       done by this class (Compiler) using the Statement and Expression
*       classes.
*
*   4.  The fourth step is to semantically analyze the parse tree.  This is
*       handled by the various statements and expressions within the parse
*       tree.  This step is referred to as "precompile".
*
*   5.  The last step is to generate the Java byte codes necessary for each
*       statement and expression.  This step is referred to as "compile".
*
* The following is the hierarchy of language elements.  Note that only the
* leaf elements are non-abstract:
*
*   Element
*       Statement
*           EmptyStatement
*           DeclarationStatement
*           ExpressionStatement
*           ConditionalStatement (note:  does not include "for")
*               IfStatement
*               DoStatement
*               WhileStatement
*           Block
*               StatementBlock
*               ForStatement
*               CatchClause
*               SwitchStatement
*           TargetStatement
*               LabelStatement
*               CaseClause
*               DefaultClause
*           GuardedStatement
*               TryStatement
*               SynchronizedStatement
*           FinallyClause
*           BranchStatement
*               BreakStatement
*               ContinueStatement
*           ExitStatement
*               ReturnStatement
*               ThrowStatement
*       Expression
*           NameExpression
*           TypeExpression
*               DimensionedExpression
*           LiteralExpression
*               NullExpression
*               BooleanExpression
*               CharExpression
*               IntExpression
*               LongExpression
*               FloatExpression
*               DoubleExpression
*               StringExpression
*           ArrayExpression
*           VariableExpression
*           ConditionalExpression
*           NewExpression
*               NewClassExpression
*               NewArrayExpression
*           UnaryExpression
*               IncExpression
*                   PreIncExpression
*                   PostIncExpression
*                   PreDecExpression
*                   PostDecExpression
*               PlusExpression
*               MinusExpression
*               NotExpression
*               BitNotExpression
*               CastExpression
*               ArrayAccessExpression
*               FieldAccessExpression
*               InvocationExpression
*           BinaryExpression
*               AssignExpression
*                   CastAssignExpression
*               LogicalExpression
*                   AndExpression
*                   OrExpression
*               BitwiseExpression
*                   BitAndExpression
*                   BitOrExpression
*                   BitXorExpression
*               EqualityExpression
*                   EqualExpression
*                   NotEqualExpression
*               RelationalExpression
*                   LessExpression
*                   NotLessExpression
*                   GreaterExpression
*                   NotGreaterExpression
*                   InstanceOfExpression
*               ShiftExpression
*                   LeftShiftExpression
*                   RightShiftExpression
*                   UnsignedShiftExpression
*               AdditiveExpression
*                   AddExpression
*                   SubtractExpression
*               MultiplicativeExpression
*                   MultiplyExpression
*                   DivideExpression
*                   ModuloExpression
*
* @version 1.00, 09/14/98
* @author  Cameron Purdy
*/
public class Compiler
        extends Base
        implements com.tangosol.dev.compiler.Compiler, Constants, TokenConstants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Java compiler.  A public default constructor is required.
    */
    public Compiler()
        {
        }


    // ----- compiler interface ---------------------------------------------

    /**
    * Compile the passed script.
    *
    * @param ctx      the compiler context
    * @param sScript  the script to compile (as a string)
    * @param errlist  the error list to log to
    *
    * @exception CompilerException  thrown if the compilation of this script
    *            fails
    */
    public void compile(Context ctx, String sScript, ErrorList errlist)
            throws CompilerException
        {
        // parameters are required
        if (ctx == null || sScript == null || errlist == null)
            {
            throw new IllegalArgumentException(CLASS + ".compile:  "
                    + "Parameters required!");
            }

        // register parameter names/types
        Block block = new StatementBlock();

        // for instance methods, there is an implied final parameter "this"
        MethodInfo method = ctx.getMethodInfo();
        azzert(method != null, "Failed to retrieve the context method");

        if (DEBUG)
            {
            out();
            printMethodInfo(method);
            }

        if (!method.isStatic())
            {
            block.addStatement(createParameterDeclaration(block, true,
                    method.getTypeInfo().getDataType(), "this"));
            }

        int cParams = method.getParamCount();
        for (int i = 0; i < cParams; ++i)
            {
            ParamInfo param = method.getParamInfo(i);
            block.addStatement(createParameterDeclaration(block,
                param.isFinal(), param.getDataType(), param.getName()));
            }

        // store the information used by parsing/code generation
        CodeAttribute code = ctx.getCode();

        this.errlist = errlist;
        this.toker   = new Tokenizer(sScript, code.getLine(), errlist);
        this.token   = next();

        // parse the script
        parseCompilationUnit(block);

        if (DEBUG)
            {
            block.print();
            }

        if (errlist.isSevere())
            {
            throw new CompilerException();
            }

        // check the imports
        checkImports(ctx);
        if (errlist.isSevere())
            {
            throw new CompilerException();
            }

        // check the parse tree
        DualSet setUVars  = new DualSet(NullImplementation.getSet());
        DualSet setFVars  = new DualSet(NullImplementation.getSet());
        HashMap mapThrown = new HashMap();
        block.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        if (!mapThrown.isEmpty())
            {
            for (Enumeration enmr = method.exceptionTypes(); enmr.hasMoreElements(); )
                {
                Expression.catchException(ctx, (DataType) enmr.nextElement(), mapThrown);
                if (mapThrown.isEmpty())
                    {
                    break;
                    }
                }
            }

        // uncaught/undeclared exceptions
        if (!mapThrown.isEmpty())
            {
            for (Iterator iterThrown = mapThrown.entrySet().iterator(); iterThrown.hasNext(); )
                {
                // map key is the data type of the exception; map value is a
                // set of expressions that throw the exception
                Entry    entry    = (Entry) iterThrown.next();
                DataType dtThrown = (DataType) entry.getKey();

                for (Iterator iterExpr = ((Set) entry.getValue()).iterator(); iterExpr.hasNext(); )
                    {
                    Expression expr = (Expression) iterExpr.next();
                    expr.logError(ERROR, EXCEPT_UNCAUGHT,
                            new String[] {dtThrown.getClassName()}, errlist);
                    }
                }
            }

        // check for errors from the pre-compile pass
        if (errlist.isSevere())
            {
            throw new CompilerException();
            }

        // generate code
        if (block.compile(ctx, code, true, errlist))
            {
            // the main block completes; if the method is void, then add
            // the implied return, otherwise it is an error
            if (method.getDataType() == DataType.VOID)
                {
                code.add(new Return());
                }
            else
                {
                logError(ERROR, RETURN_MISSING, null,
                        block.getEndLine(), block.getEndOffset(), 0);
                }
            }

        if (DEBUG)
            {
            code.print();
            out();
            }

        // check for errors from the compile pass
        if (errlist.isSevere())
            {
            throw new CompilerException();
            }
        }


    // ----- script parsing -------------------------------------------------

    /**
    * Parse the script.
    *
    *   Goal:
    *       CompilationUnit
    *   CompilationUnit:
    *       ImportDeclarations-opt BlockStatements-opt
    */
    protected void parseCompilationUnit(Block block)
            throws CompilerException
        {
        parseImportDeclarations();
        parseBlockStatements(block);

        // after parsing the block, the remaining token should be the pretend
        // closing curly brace for the block
        if (token != null && token.id == SEP_RCURLYBRACE && token.length == 0)
            {
            // the block was started with a corresponding pretend open curly
            block.setEndToken(token);
            }
        else
            {
            // log error - tokens remaining, probably missing {
            logError(ERROR, UNBALANCED_BRACE, null, token.getLine(), token.getOffset(), 0);
            }
        }

    /**
    * Parse the "import" declarations and register each imported class under
    * its short name.
    */
    protected void parseImportDeclarations()
            throws CompilerException
        {
        while (peek(KEY_IMPORT) != null)
            {
            try
                {
                Token  tokName;  // name token
                String sFull;    // fully qualified name

                // parse name "n.n.n"
                StringBuffer sb = new StringBuffer();
                boolean fFirst = true;
                do
                    {
                    if (fFirst)
                        {
                        fFirst = false;
                        }
                    else
                        {
                        sb.append('.');
                        }

                    tokName = match(IDENT);
                    sb.append(tokName.getText());
                    }
                while (peek(SEP_DOT) != null);
                sFull = sb.toString();

                // check for optional alias
                if (peek(KEY_AS) != null)
                    {
                    tokName = match(IDENT);
                    }

                match(SEP_SEMICOLON);

                // register the import name
                tblImports.put(tokName, sFull);
                }
            catch (SyntaxException e)
                {
Expurgate:      while (true)
                    {
                    switch (token.id)
                        {
                        // end of an import or statement
                        case SEP_SEMICOLON:
                            next();
                        // start of an import
                        case KEY_IMPORT:
                        // start of a statement
                        case KEY_BREAK:
                        case KEY_CASE:
                        case KEY_CONTINUE:
                        case KEY_DEFAULT:
                        case KEY_DO:
                        case KEY_FINAL:
                        case KEY_FOR:
                        case KEY_IF:
                        case KEY_RETURN:
                        case KEY_SWITCH:
                        case KEY_SYNCHRONIZED:
                        case KEY_THROW:
                        case KEY_TRY:
                        case KEY_WHILE:
                        // could be end of script - definitely not part of
                        // the imports!
                        case SEP_RCURLYBRACE:
                            break Expurgate;

                        default:
                            next();
                        }
                    }
                }
            }
        }


    // ----- statement parsing ----------------------------------------------

    /**
    * Parse a statement block.
    *
    *   Block:
    *       { BlockStatements-opt }
    */
    protected Block parseBlock(Statement outer)
            throws CompilerException
        {
        Block block = new StatementBlock(outer, token);

        if (token.id == SEP_LCURLYBRACE)
            {
            // { BlockStatements-opt }
            match(SEP_LCURLYBRACE);
            parseBlockStatements(block);
            block.setEndToken(match(SEP_RCURLYBRACE));
            }
        else
            {
            // open curly expected; someone probably just forgot their curlies
            logError(ERROR, TOKEN_EXPECTED, new String[] {"{"},
                    token.getLine(), token.getOffset(), 0);

            // question:  if the statement parsing fails, would it be
            // better to handle a syntax error here or to let the caller
            // deal with it?  for now, we'll assume the latter
            block.addStatement(parseStatement(block));
            }

        return block;
        }

    /**
    * Parse a sequence of statements.
    *
    *   BlockStatements:
    *       BlockStatement
    *       BlockStatements BlockStatement
    */
    protected void parseBlockStatements(Block block)
            throws CompilerException
        {
        while (token.id != SEP_RCURLYBRACE)
            {
            try
                {
                block.addStatement(parseStatement(block));
                }
            catch (SyntaxException e)
                {
                // an error occurred parsing the statement ... skip it
                expurgateStatement();
                }
            }
        }

    /**
    * Parse a statement.
    *
    * @param outer  contains the statement being parsed
    */
    protected Statement parseStatement(Statement outer)
            throws CompilerException
        {
        // determine the containing block
        Block block = (outer instanceof Block ? (Block) outer : outer.getBlock());

        switch (token.id)
            {
            //  EmptyStatement:
            //      ;
            case SEP_SEMICOLON:
                return new EmptyStatement(outer, current());

            //  Block:
            //      { BlockStatements-opt }
            case SEP_LCURLYBRACE:
                return parseBlock(outer);

            //  TryStatement:
            //      try Block Catches
            //      try Block Catches-opt Finally
            //  Catches:
            //      CatchClause
            //      Catches CatchClause
            //  CatchClause:
            //      catch ( FormalParameter ) Block
            //  Finally:
            //      finally Block
            case KEY_TRY:
                {
                TryStatement stmt = new TryStatement(outer, current());
                stmt.setInnerStatement(parseBlock(stmt));

                CatchClause last = null;
                boolean fNoClauses = true;
                while (token.id == KEY_CATCH)
                    {
                    CatchClause clause = new CatchClause(stmt, current());
                    match(SEP_LPARENTHESIS);
                    // note that the CatchClause itself is the Block (and
                    // that the exception variable is treated as a parameter)
                    DeclarationStatement stmtDecl = parseDeclaration(clause);
                    stmtDecl.setParameter(true);
                    clause.addStatement(stmtDecl);
                    match(SEP_RPARENTHESIS);
                    clause.addStatement(parseBlock(clause));
                    if (fNoClauses)
                        {
                        stmt.setCatchClause(clause);
                        }
                    else
                        {
                        last.setNextStatement(clause);
                        }
                    last       = clause;
                    fNoClauses = false;
                    }

                if (token.id == KEY_FINALLY)
                    {
                    FinallyClause clause = new FinallyClause(stmt, current());
                    clause.setInnerStatement(parseBlock(clause));
                    stmt.setFinallyClause(clause);
                    fNoClauses = false;
                    }

                if (fNoClauses)
                    {
                    // no deadly parsing errors have occurred, but parts of
                    // the try statement are missing
                    // TODO log error
                    }

                return stmt;
                }

            //  SynchronizedStatement:
            //      synchronized ( Expression ) Block
            case KEY_SYNCHRONIZED:
                {
                SynchronizedStatement stmt = new SynchronizedStatement(outer, current());
                match(SEP_LPARENTHESIS);
                stmt.setExpression(parseExpression(block));
                match(SEP_RPARENTHESIS);
                Statement inner = parseBlock(stmt);
                stmt.setInnerStatement(inner);
                stmt.setEndToken(inner.getEndToken());
                return stmt;
                }

            //  DoStatement:
            //      do Statement while ( Expression ) ;
            case KEY_DO:
                {
                DoStatement stmt = new DoStatement(outer, current());
                stmt.setInnerStatement(parseStatement(stmt));
                match(KEY_WHILE);
                match(SEP_LPARENTHESIS);
                stmt.setTest(parseExpression(block));
                match(SEP_RPARENTHESIS);
                stmt.setEndToken(match(SEP_SEMICOLON));
                return stmt;
                }

            //  WhileStatement:
            //      while ( Expression ) Statement
            case KEY_WHILE:
                {
                WhileStatement stmt = new WhileStatement(outer, current());
                match(SEP_LPARENTHESIS);
                stmt.setTest(parseExpression(block));
                match(SEP_RPARENTHESIS);
                stmt.setInnerStatement(parseStatement(stmt));
                return stmt;
                }

            //  ForStatement:
            //      for ( ForInit-opt ; Expression-opt ; ForUpdate-opt ) Statement
            //  ForInit:
            //      StatementExpressionList
            //      LocalVariableDeclaration
            //  ForUpdate:
            //      StatementExpressionList
            //  StatementExpressionList:
            //      StatementExpression
            //      StatementExpressionList , StatementExpression
            case KEY_FOR:
                {
                ForStatement stmt = new ForStatement(outer, current());
                match(SEP_LPARENTHESIS);
                if (token.id != SEP_SEMICOLON)
                    {
                    stmt.setInit(parseStatementList(stmt, true));
                    }
                match(SEP_SEMICOLON);
                if (token.id != SEP_SEMICOLON)
                    {
                    // note that the ForStatement itself is the Block
                    stmt.setTest(parseExpression(stmt));
                    }
                match(SEP_SEMICOLON);
                if (token.id != SEP_RPARENTHESIS)
                    {
                    stmt.setUpdate(parseStatementList(stmt, false));
                    }
                match(SEP_RPARENTHESIS);
                stmt.setInnerStatement(parseStatement(stmt));
                return stmt;
                }

            //  IfThenStatement:
            //      if ( Expression ) Statement
            //  IfThenElseStatement:
            //      if ( Expression ) StatementNoShortIf else Statement
            case KEY_IF:
                {
                IfStatement stmt = new IfStatement(outer, current());
                match(SEP_LPARENTHESIS);
                stmt.setTest(parseExpression(block));
                match(SEP_RPARENTHESIS);
                stmt.setThenStatement(parseStatement(stmt));
                // although the LALR(1) grammar goes into great detail about
                // the "NoShortIf" constructs, recursive decent parsing only
                // cares whether or not an else exists at this point
                if (peek(KEY_ELSE) != null)
                    {
                    stmt.setElseStatement(parseStatement(stmt));
                    }
                return stmt;
                }

            //  ReturnStatement:
            //      return Expression-opt ;
            case KEY_RETURN:
                {
                ReturnStatement stmt = new ReturnStatement(outer, current());
                if (token.id != SEP_SEMICOLON)
                    {
                    stmt.setExpression(parseExpression(block));
                    }
                stmt.setEndToken(match(SEP_SEMICOLON));
                return stmt;
                }

            //  ThrowStatement:
            //      throw Expression ;
            case KEY_THROW:
                {
                ThrowStatement stmt = new ThrowStatement(outer, current());
                stmt.setExpression(parseExpression(block));
                stmt.setEndToken(match(SEP_SEMICOLON));
                return stmt;
                }

            //  SwitchStatement:
            //      switch ( Expression ) SwitchBlock
            //  SwitchBlock:
            //      { SwitchBlockStatementGroups-opt SwitchLabels-opt }
            //  SwitchBlockStatementGroups:
            //      SwitchBlockStatementGroup
            //      SwitchBlockStatementGroups SwitchBlockStatementGroup
            //  SwitchBlockStatementGroup:
            //      SwitchLabels BlockStatements
            //  SwitchLabels:
            //      SwitchLabel
            //      SwitchLabels SwitchLabel
            //  SwitchLabel:
            //      case ConstantExpression :
            //      default :
            case KEY_SWITCH:
                {
                SwitchStatement stmt = new SwitchStatement(outer, current());
                match(SEP_LPARENTHESIS);
                stmt.setTest(parseExpression(block));
                match(SEP_RPARENTHESIS);
                match(SEP_LCURLYBRACE);
SwitchBlock:    while (true)
                    {
                    try
                        {
                        switch (token.id)
                            {
                            case KEY_CASE:
                                {
                                CaseClause clause = new CaseClause(stmt, current());
                                // note that SwitchStatement itself is the block
                                clause.setTest(parseExpression(stmt));
                                clause.setEndToken(match(OP_COLON));
                                stmt.addStatement(clause);
                                }
                                break;

                            case KEY_DEFAULT:
                                {
                                DefaultClause clause = new DefaultClause(stmt, current());
                                clause.setEndToken(match(OP_COLON));
                                stmt.addStatement(clause);
                                }
                                break;

                            default:
                                stmt.addStatement(parseStatement(stmt));
                                break;

                            case SEP_RCURLYBRACE:
                                break SwitchBlock;
                            }
                        }
                    catch (SyntaxException e)
                        {
                        expurgateStatement();
                        }
                    }
                stmt.setEndToken(current());  // right curly
                return stmt;
                }

            //  BreakStatement:
            //      break Identifier-opt ;
            case KEY_BREAK:
                {
                BreakStatement stmt = new BreakStatement(outer, current(), peek(IDENT));
                stmt.setEndToken(match(SEP_SEMICOLON));
                return stmt;
                }

            //  ContinueStatement:
            //      continue Identifier-opt ;
            case KEY_CONTINUE:
                {
                ContinueStatement stmt = new ContinueStatement(outer, current(), peek(IDENT));
                stmt.setEndToken(match(SEP_SEMICOLON));
                return stmt;
                }

            //  VariableDeclaration:
            //      Modifiers-opt Type VariableDeclarators
            case KEY_FINAL:
            case KEY_BOOLEAN:
            case KEY_BYTE:
            case KEY_CHAR:
            case KEY_SHORT:
            case KEY_INT:
            case KEY_LONG:
            case KEY_FLOAT:
            case KEY_DOUBLE:
                {
                Statement stmt = parseDeclaration(outer);
                stmt.setEndToken(match(SEP_SEMICOLON));
                return stmt;
                }

            //  LabeledStatement:
            //      Identifier : Statement
            //  VariableDeclaration:
            //      Modifiers-opt Type VariableDeclarators
            //  ExpressionStatement:
            //      StatementExpression ;
            case IDENT:
                {
                Expression expr = parseExpression(block);
                switch (token.id)
                    {
                    case IDENT:
                        {
                        // expr must be the type in a variable declaration
                        Statement stmt = parseDeclaration(outer, null, toTypeExpression(expr));
                        stmt.setEndToken(match(SEP_SEMICOLON));
                        return stmt;
                        }

                    case OP_COLON:
                        {
                        // expr must be a label, which is a simple identifier
                        Token tokLabel = expr.getStartToken();
                        if (tokLabel == expr.getEndToken() && tokLabel.getCategory() == IDENTIFIER)
                            {
                            LabelStatement stmt = new LabelStatement(outer, tokLabel, match(OP_COLON));
                            stmt.setInnerStatement(parseStatement(stmt));
                            return stmt;
                            }
                        // assume it was supposed to be a statement expression
                        // (so fall through)
                        }

                    default:
                        {
                        // expr must be a statement expression
                        // turn it into a ExpressionStatement
                        Statement stmt = createExpressionStatement(outer, expr);
                        stmt.setEndToken(match(SEP_SEMICOLON));
                        return stmt;
                        }
                    }
                }

            //  ExpressionStatement:
            //      StatementExpression ;
            case KEY_NEW:
            case KEY_THIS:
            case KEY_SUPER:
            case SEP_LPARENTHESIS:
            case OP_INCREMENT:
            case OP_DECREMENT:
                {
                Expression expr = parseExpression(block);
                Statement  stmt = createExpressionStatement(outer, expr);
                stmt.setEndToken(match(SEP_SEMICOLON));
                return stmt;
                }

            // unexpected statement continuations
            case KEY_ELSE:
                logError(ERROR, ELSE_NO_IF, new String[] {token.getText()}, token);
                throw new SyntaxException();

            case KEY_CATCH:
                logError(ERROR, CATCH_NO_TRY, new String[] {token.getText()}, token);
                throw new SyntaxException();

            case KEY_FINALLY:
                logError(ERROR, FINALLY_NO_TRY, new String[] {token.getText()}, token);
                throw new SyntaxException();

            case KEY_CASE:
            case KEY_DEFAULT:
                logError(ERROR, LABEL_NO_SWITCH, new String[] {token.getText()}, token);
                throw new SyntaxException();

            // unexpected separator; probably recoverable
            case SEP_DOT:
            case SEP_COMMA:
            case SEP_RPARENTHESIS:
            case SEP_LBRACKET:
            case SEP_RBRACKET:

            // unexpected keyword; probably recoverable
            case KEY_INSTANCEOF:

            // unexpected operator; probably recoverable
            case OP_ADD:
            case OP_SUB:
            case OP_MUL:
            case OP_DIV:
            case OP_REM:
            case OP_SHL:
            case OP_SHR:
            case OP_USHR:
            case OP_BITAND:
            case OP_BITOR:
            case OP_BITXOR:
            case OP_BITNOT:
            case OP_ASSIGN:
            case OP_ASSIGN_ADD:
            case OP_ASSIGN_SUB:
            case OP_ASSIGN_MUL:
            case OP_ASSIGN_DIV:
            case OP_ASSIGN_REM:
            case OP_ASSIGN_SHL:
            case OP_ASSIGN_SHR:
            case OP_ASSIGN_USHR:
            case OP_ASSIGN_BITAND:
            case OP_ASSIGN_BITOR:
            case OP_ASSIGN_BITXOR:
            case OP_TEST_EQ:
            case OP_TEST_NE:
            case OP_TEST_GT:
            case OP_TEST_GE:
            case OP_TEST_LT:
            case OP_TEST_LE:
            case OP_LOGICAL_AND:
            case OP_LOGICAL_OR:
            case OP_LOGICAL_NOT:
            case OP_CONDITIONAL:    // identifier missing?
            case OP_COLON:          // label missing?

            // unexpected literal value; probably recoverable
            case LIT_NULL:
            case LIT_TRUE:
            case LIT_FALSE:
            case LIT_CHAR:
            case LIT_INT:
            case LIT_LONG:
            case LIT_FLOAT:
            case LIT_DOUBLE:
            case LIT_STRING:
                logError(ERROR, TOKEN_UNEXPECTED, new String[] {token.getText()}, token);
                throw new SyntaxException();

            // totally unexpected tokens ... assume unrecoverable
            case SEP_RCURLYBRACE:
            case KEY_IMPORT:
                logError(ERROR, TOKEN_PANIC, new String[] {token.getText()}, token);
                throw new CompilerException();

            // illegal keywords - not used in Java script language
            case KEY_ABSTRACT:
            case KEY_CLASS:
            case KEY_EXTENDS:
            case KEY_IMPLEMENTS:
            case KEY_INTERFACE:
            case KEY_NATIVE:
            case KEY_PACKAGE:
            case KEY_PRIVATE:
            case KEY_PROTECTED:
            case KEY_PUBLIC:
            case KEY_STATIC:
            case KEY_THROWS:
            case KEY_TRANSIENT:
            case KEY_VOID:
            case KEY_VOLATILE:
                // unsupported keyword in a statement
                logError(ERROR, TOKEN_UNSUPP, new String[] {token.getText()}, token);
                throw new CompilerException();

            // illegal keywords - not used in Java
            case KEY_CONST:
            case KEY_GOTO:
                logError(ERROR, TOKEN_ILLEGAL, new String[] {token.getText()}, token);
                throw new CompilerException();

            default:
                logError(ERROR, TOKEN_UNKNOWN, new String[] {token.getText()}, token);
                throw new CompilerException();
            }
        }

    /**
    * Parse a statement expression list.  Additional statements are linked
    * after the first (via the Statement.next field).  This is used only
    * within the "for" statement.
    *
    *   ForInit:
    *       StatementExpressionList
    *       LocalVariableDeclaration
    *   ForUpdate:
    *       StatementExpressionList
    *   StatementExpressionList:
    *       StatementExpression
    *       StatementExpressionList , StatementExpression
    *
    * @param outer     contains the statement being parsed
    * @param fDeclare  true if the statement expression list may be a
    *                  variable declaration
    *
    * @return the parsed statement(s)
    */
    protected Statement parseStatementList(Statement outer, boolean fDeclare)
            throws CompilerException
        {
        Block block = (outer instanceof Block ? (Block) outer : outer.getBlock());

        // check for a variable declaration
        Token      tokFinal = (fDeclare ? peek(KEY_FINAL) : null);
        Expression expr     = parseExpression(block);
        if (fDeclare && (tokFinal != null || token.id == IDENT))
            {
            return parseDeclaration(outer, tokFinal, toTypeExpression(expr));
            }

        // build the StatementExpressionList
        ExpressionStatement stmt = createExpressionStatement(outer, expr);
        ExpressionStatement last = stmt;
        while (peek(SEP_COMMA) != null)
            {
            ExpressionStatement next = createExpressionStatement(outer, parseExpression(block));
            last.setNextStatement(next);
            last = next;
            }
        return stmt;
        }

    /**
    * Create an ExpressionStatement from the StatementExpression.  If the
    * expression is not an expression statement, this method throws a syntax
    * exception.
    *
    *   ExpressionStatement:
    *       StatementExpression ;
    *   StatementExpression:
    *       Assignment
    *       PreIncrementExpression
    *       PreDecrementExpression
    *       PostIncrementExpression
    *       PostDecrementExpression
    *       MethodInvocation
    *       ClassInstanceCreationExpression
    *
    * @param outer  contains the expression statement
    * @param expr   the statement expression
    *
    * @return the new statement
    *
    * @exception CompilerException expression is not an ExpressionStatement
    */
    protected ExpressionStatement createExpressionStatement(Statement outer, Expression expr)
            throws CompilerException
        {
        if (expr instanceof AssignExpression     ||
            expr instanceof InvocationExpression ||
            expr instanceof NewClassExpression   ||
            expr instanceof PreIncExpression     ||
            expr instanceof PostIncExpression    ||
            expr instanceof PreDecExpression     ||
            expr instanceof PostDecExpression       )
            {
            ExpressionStatement stmt = new ExpressionStatement(outer, expr.getStartToken());
            stmt.setExpression(expr);
            return stmt;
            }

        expr.logError(ERROR, EXPR_NOT_STMT, null, errlist);
        throw new SyntaxException();
        }

    /**
    * Helper to parse a variable declaration statement.
    *
    * @param outer     contains the statement being parsed
    *
    * @return the variable declaration statement
    */
    protected DeclarationStatement parseDeclaration(Statement outer)
            throws CompilerException
        {
        Block block    = (outer instanceof Block ? (Block) outer : outer.getBlock());
        Token tokFinal = peek(KEY_FINAL);

        TypeExpression type;
        switch (token.id)
            {
            case KEY_BOOLEAN:
            case KEY_BYTE:
            case KEY_CHAR:
            case KEY_SHORT:
            case KEY_INT:
            case KEY_LONG:
            case KEY_FLOAT:
            case KEY_DOUBLE:
                type = parseDimensionedExpression(block, new TypeExpression(block, current()));
                break;

            default:
                type = toTypeExpression(parseExpression(block));
                break;
            }

        return parseDeclaration(outer, tokFinal, type);
        }

    /**
    * Parse a variable declaration statement.  The modifiers and data type
    * are already parsed.  This method does not eat the trailing semi-colon
    * in order to be usable from the statement list parsing used by the
    * "for" statement parsing.
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
    * @param outer     contains the statement being parsed
    * @param tokFinal  the "final" modifier token or null
    * @param type      the type expression
    *
    * @return the variable declaration statement
    */
    protected DeclarationStatement parseDeclaration(Statement outer, Token tokFinal, TypeExpression type)
            throws CompilerException
        {
        Block block    = (outer instanceof Block ? (Block) outer : outer.getBlock());
        Token tokFirst = (tokFinal == null ? type.getStartToken() : tokFinal);

        DeclarationStatement decl = new DeclarationStatement(outer, tokFirst);
        decl.setModifier(tokFinal);
        decl.setTypeExpression(type);

        Statement last = null;
        do
            {
            // the expressions may or may not be assignment expressions,
            // but at this point, assume they all are; this simplifies
            // parsing of things like:  int a, b=3, c=b+1, d, e=b*c;
            ExpressionStatement stmt = new ExpressionStatement(decl, token);
            stmt.setExpression(parseExpression(block));

            // add the variable declaration to the declaration statement
            if (last == null)
                {
                decl.setInnerStatement(stmt);
                }
            else
                {
                last.setNextStatement(stmt);
                }
            last = stmt;
            }
        while (peek(SEP_COMMA) != null);

        return decl;
        }

    /**
    * Used to declare parameters.
    *
    * @param fFinal
    * @param dtParam
    * @param sName
    *
    * @return a declaration statement
    */
    protected Statement createParameterDeclaration(Block block, boolean fFinal, DataType dtParam, String sName)
        {
        // pretend that at (0,0) there are a whole bunch of tokens that
        // magically define the variable
        Token tokFinal = (fFinal ? new Token(Token.TOK_FINAL, 0, 0, 0) : null);
        Token tokType  = null;
        Token tokLast  = null;
        Token tokIdent = new Token(IDENTIFIER, NONE, IDENT, null, sName, 0, 0, 0);

        DataType dt = dtParam;
        while (dt.isArray())
            {
            dt = dt.getElementType();

            if (tokLast != null)
                {
                tokLast = new Token(Token.TOK_RBRACKET, 0, 0, 0);
                }
            }

        if (dt.isPrimitive())
            {
            switch (dt.getJVMSignature().charAt(0))
                {
                case 'Z':
                    tokType = Token.TOK_BOOLEAN;
                    break;
                case 'B':
                    tokType = Token.TOK_BYTE;
                    break;
                case 'C':
                    tokType = Token.TOK_CHAR;
                    break;
                case 'S':
                    tokType = Token.TOK_SHORT;
                    break;
                case 'I':
                    tokType = Token.TOK_INT;
                    break;
                case 'J':
                    tokType = Token.TOK_LONG;
                    break;
                case 'F':
                    tokType = Token.TOK_FLOAT;
                    break;
                case 'D':
                    tokType = Token.TOK_DOUBLE;
                    break;
                }
            tokType = new Token(tokType, 0, 0, 0);
            }
        else
            {
            String sType = (dt.isComponent() ? dt.getComponentName() : dt.getClassName());
            int    of    = sType.indexOf('.');
            if (of >= 0)
                {
                if (tokLast != null)
                    {
                    tokLast = new Token(Token.IDENTIFIER, Token.NONE, Token.IDENT, null,
                        sType.substring(sType.lastIndexOf('.') + 1), 0, 0, 0);
                    }
                sType = sType.substring(0, of);
                }
            tokType = new Token(Token.IDENTIFIER, Token.NONE, Token.IDENT, null, sType, 0, 0, 0);
            }
        TypeExpression type = new TypeExpression(block, tokType, dtParam);
        type.setEndToken(tokLast == null ? tokType : tokLast);

        // create declaration
        DeclarationStatement decl = new DeclarationStatement(block, (tokFinal == null ? tokType : tokFinal));
        decl.setParameter(true);
        decl.setModifier(tokFinal);
        decl.setTypeExpression(type);

        // declare the parameter
        ExpressionStatement stmt = new ExpressionStatement(decl, tokIdent);
        stmt.setExpression(new NameExpression(block, tokIdent));
        decl.setInnerStatement(stmt);

        return decl;
        }

    /**
    * Parsing died in the middle of a statement.  Discard tokens until it
    * appears that the statement ends.
    */
    protected void expurgateStatement()
            throws CompilerException
        {
        while (true)
            {
            switch (token.id)
                {
                // 99% of the time, statements will end with semi-colon
                case SEP_SEMICOLON:
                    next();
                    return;

                // keywords which start a new statement
                case KEY_BREAK:
                case KEY_CONTINUE:
                case KEY_DO:
                case KEY_FINAL:
                case KEY_FOR:
                case KEY_IF:
                case KEY_RETURN:
                case KEY_SWITCH:
                case KEY_SYNCHRONIZED:
                case KEY_THROW:
                case KEY_TRY:
                case KEY_WHILE:
                    return;

                // parenthetically speaking ...
                case SEP_LCURLYBRACE:   // trailing statement or array initializer
                case SEP_LPARENTHESIS:  // who knows ... method invocation?
                case SEP_LBRACKET:      // array index?
                    expurgateParenthetical();
                    break;

                // end of a block (or last token)
                case SEP_RCURLYBRACE:
                    return;

                default:
                    next();
                    break;
                }
            }
        }

    /**
    * Discard tokens comprising a parenthetical expression.  When called, the
    * current token should be one that opens a parenthetical expression.  The
    * term "expression" does not refer to the language element Expression but
    * to some open/close pair of tokens and everything in between.
    */
    protected void expurgateParenthetical()
            throws CompilerException
        {
        int    idOpen  = token.id;
        int    idClose;
        String sClose;
        switch (idOpen)
            {
            default:
            case SEP_LCURLYBRACE:
                idClose = SEP_RCURLYBRACE;
                sClose  = "}";
                break;

            case SEP_LPARENTHESIS:
                idClose = SEP_RPARENTHESIS;
                sClose  = ")";
                break;

            case SEP_LBRACKET:
                idClose = SEP_RBRACKET;
                sClose  = "]";
                break;
            }

        int cUnmatched = 0;     // number of opens w/o closes
        do
            {
            int id = token.id;
            if (id == SEP_RCURLYBRACE && token.length == 0)
                {
                // this is the last token in the script
                // note:  the call to next() will bomb
                logError(ERROR, TOKEN_EXPECTED, new String[] {sClose},
                        token.getLine(), token.getOffset(), 0);
                }
            else if (id == idOpen)
                {
                ++cUnmatched;
                }
            else if (id == idClose)
                {
                --cUnmatched;
                }

            next();
            }
        while (cUnmatched > 0);
        }


    // ----- expression parsing ---------------------------------------------

    /**
    * Parse an expression.  The expression parsing uses a recursive descent
    * algorithm, which requires a different type of grammar than that found
    * in the Java Language Specification.  For example, the JLS will state:
    *
    *   ConstructList:
    *       Construct
    *       ConstructList Construct
    *
    * A recursive descent parser implicitly re-arranges this as:
    *
    *   ConstructList:
    *       Construct ConstructList-opt
    *
    * This method handles the top-most-level of expression parsing, which is
    * the assignment expression.
    *
    *   ConstantExpression:
    *       Expression
    *   Expression:
    *       AssignmentExpression
    *   AssignmentExpression:
    *       ConditionalExpression
    *       Assignment
    *   Assignment:
    *       LeftHandSide AssignmentOperator AssignmentExpression
    *   LeftHandSide:
    *       Name
    *       FieldAccess
    *       ArrayAccess
    *   AssignmentOperator: one of
    *       = *= /= %= += -= <<= >>= >>>= &= ^= |=
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseExpression(Block block)
            throws CompilerException
        {
        Expression exprLeft = parseConditionalExpression(block);
        Token      tokOp;
        Expression exprRight;

        switch (token.id)
            {
            case OP_ASSIGN:
                return new AssignExpression(exprLeft, current(), parseExpression(block));

            case OP_ASSIGN_ADD:
                tokOp     = current();
                exprRight = new AddExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_SUB:
                tokOp     = current();
                exprRight = new SubtractExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_MUL:
                tokOp     = current();
                exprRight = new MultiplyExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_DIV:
                tokOp     = current();
                exprRight = new DivideExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_REM:
                tokOp     = current();
                exprRight = new ModuloExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_SHL:
                tokOp     = current();
                exprRight = new LeftShiftExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_SHR:
                tokOp     = current();
                exprRight = new RightShiftExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_USHR:
                tokOp     = current();
                exprRight = new UnsignedShiftExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_BITAND:
                tokOp     = current();
                exprRight = new BitAndExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_BITOR:
                tokOp     = current();
                exprRight = new BitOrExpression(exprLeft, tokOp, parseExpression(block));
                break;
            case OP_ASSIGN_BITXOR:
                tokOp     = current();
                exprRight = new BitXorExpression(exprLeft, tokOp, parseExpression(block));
                break;

            default:
                return exprLeft;
            }

        return new CastAssignExpression(exprLeft, tokOp, exprRight);
        }

    /**
    * Parse a conditional expression.
    *
    *   ConditionalExpression:
    *       ConditionalOrExpression
    *       ConditionalOrExpression ? Expression : ConditionalExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseConditionalExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseOrExpression(block);
        if (token.id == OP_CONDITIONAL)
            {
            expr = new ConditionalExpression(expr, current(), parseExpression(block),
                    match(OP_COLON), parseConditionalExpression(block));
            }
        return expr;
        }

    /**
    * Parse a logical or expression.
    *
    *   ConditionalOrExpression:
    *       ConditionalAndExpression
    *       ConditionalOrExpression || ConditionalAndExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseOrExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseAndExpression(block);
        while (token.id == OP_LOGICAL_OR)
            {
            expr = new OrExpression(expr, current(), parseAndExpression(block));
            }
        return expr;
        }

    /**
    * Parse a logical and expression.
    *
    *   ConditionalAndExpression:
    *       InclusiveOrExpression
    *       ConditionalAndExpression && InclusiveOrExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseAndExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseBitOrExpression(block);
        while (token.id == OP_LOGICAL_AND)
            {
            expr = new AndExpression(expr, current(), parseBitOrExpression(block));
            }
        return expr;
        }

    /**
    * Parse a bitwise or expression.
    *
    *   InclusiveOrExpression:
    *       ExclusiveOrExpression
    *       InclusiveOrExpression | ExclusiveOrExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseBitOrExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseBitXorExpression(block);
        while (token.id == OP_BITOR)
            {
            expr = new BitOrExpression(expr, current(), parseBitXorExpression(block));
            }
        return expr;
        }

    /**
    * Parse a bitwise exclusive or expression.
    *
    *   ExclusiveOrExpression:
    *       AndExpression
    *       ExclusiveOrExpression ^ AndExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseBitXorExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseBitAndExpression(block);
        while (token.id == OP_BITXOR)
            {
            expr = new BitXorExpression(expr, current(), parseBitAndExpression(block));
            }
        return expr;
        }

    /**
    * Parse a bitwise and expression.
    *
    *   AndExpression:
    *       EqualityExpression
    *       AndExpression & EqualityExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseBitAndExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseEqualityExpression(block);
        while (token.id == OP_BITAND)
            {
            expr = new BitAndExpression(expr, current(), parseEqualityExpression(block));
            }
        return expr;
        }

    /**
    * Parse an equality test expression.
    *
    *   EqualityExpression:
    *       RelationalExpression
    *       EqualityExpression == RelationalExpression
    *       EqualityExpression != RelationalExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseEqualityExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseRelationalExpression(block);
        while (true)
            {
            switch (token.id)
                {
                case OP_TEST_EQ:
                    expr = new EqualExpression(expr, current(), parseRelationalExpression(block));
                    break;
                case OP_TEST_NE:
                    expr = new NotEqualExpression(expr, current(), parseRelationalExpression(block));
                    break;
                default:
                    return expr;
                }
            }
        }

    /**
    * Parse a relational test expression.
    *
    *   RelationalExpression:
    *       ShiftExpression
    *       RelationalExpression < ShiftExpression
    *       RelationalExpression > ShiftExpression
    *       RelationalExpression <= ShiftExpression
    *       RelationalExpression >= ShiftExpression
    *       RelationalExpression instanceof ReferenceType
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseRelationalExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseShiftExpression(block);
        while (true)
            {
            switch (token.id)
                {
                case OP_TEST_LT:
                    expr = new LessExpression(expr, current(), parseShiftExpression(block));
                    break;
                case OP_TEST_GE:
                    expr = new NotLessExpression(expr, current(), parseShiftExpression(block));
                    break;
                case OP_TEST_GT:
                    expr = new GreaterExpression(expr, current(), parseShiftExpression(block));
                    break;
                case OP_TEST_LE:
                    expr = new NotGreaterExpression(expr, current(), parseShiftExpression(block));
                    break;
                case KEY_INSTANCEOF:
                    expr = new InstanceOfExpression(expr, current(), parseShiftExpression(block));
                    break;
                default:
                    return expr;
                }
            }
        }

    /**
    * Parse a bitwise shift expression.
    *
    *   ShiftExpression:
    *       AdditiveExpression
    *       ShiftExpression << AdditiveExpression
    *       ShiftExpression >> AdditiveExpression
    *       ShiftExpression >>> AdditiveExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseShiftExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseAdditiveExpression(block);
        while (true)
            {
            switch (token.id)
                {
                case OP_SHL:
                    expr = new LeftShiftExpression(expr, current(), parseAdditiveExpression(block));
                    break;
                case OP_SHR:
                    expr = new RightShiftExpression(expr, current(), parseAdditiveExpression(block));
                    break;
                case OP_USHR:
                    expr = new UnsignedShiftExpression(expr, current(), parseAdditiveExpression(block));
                    break;
                default:
                    return expr;
                }
            }
        }

    /**
    * Parse an additive expression.
    *
    *   AdditiveExpression:
    *       MultiplicativeExpression
    *       AdditiveExpression + MultiplicativeExpression
    *       AdditiveExpression - MultiplicativeExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseAdditiveExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseMultiplicativeExpression(block);
        while (true)
            {
            switch (token.id)
                {
                case OP_ADD:
                    expr = new AddExpression(expr, current(), parseMultiplicativeExpression(block));
                    break;
                case OP_SUB:
                    expr = new SubtractExpression(expr, current(), parseMultiplicativeExpression(block));
                    break;
                default:
                    return expr;
                }
            }
        }

    /**
    * Parse a multiplicative expression.
    *
    *   MultiplicativeExpression:
    *       UnaryExpression
    *       MultiplicativeExpression * UnaryExpression
    *       MultiplicativeExpression / UnaryExpression
    *       MultiplicativeExpression % UnaryExpression
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseMultiplicativeExpression(Block block)
            throws CompilerException
        {
        Expression expr = parseUnaryExpression(block);
        while (true)
            {
            switch (token.id)
                {
                case OP_MUL:
                    expr = new MultiplyExpression(expr, current(), parseUnaryExpression(block));
                    break;
                case OP_DIV:
                    expr = new DivideExpression(expr, current(), parseUnaryExpression(block));
                    break;
                case OP_REM:
                    expr = new ModuloExpression(expr, current(), parseUnaryExpression(block));
                    break;
                default:
                    return expr;
                }
            }
        }

    /**
    * Parse a unary expression.
    *
    *   UnaryExpression:
    *       PreIncrementExpression
    *       PreDecrementExpression
    *       + UnaryExpression
    *       - UnaryExpression
    *       UnaryExpressionNotPlusMinus
    *   PreIncrementExpression:
    *       ++ UnaryExpression
    *   PreDecrementExpression:
    *       -- UnaryExpression
    *   UnaryExpressionNotPlusMinus:
    *       PostfixExpression
    *       ~ UnaryExpression
    *       ! UnaryExpression
    *       CastExpression
    *   CastExpression:
    *       ( PrimitiveType Dims-opt ) UnaryExpression
    *       ( Expression ) UnaryExpressionNotPlusMinus
    *       ( Name Dims ) UnaryExpressionNotPlusMinus
    *
    * This can be simplified to:
    *
    *   UnaryExpression(sign_allowed)
    *       ++ UnaryExpression(true)    // if sign_allowed
    *       -- UnaryExpression(true)    // if sign_allowed
    *       + UnaryExpression(true)     // if sign_allowed
    *       - UnaryExpression(true)     // if sign_allowed
    *       ~ UnaryExpression(true)
    *       ! UnaryExpression(true)
    *       ( PrimitiveType Dims-opt ) UnaryExpression(true)
    *       ( Expression ) UnaryExpression(false)
    *       ( Name Dims ) UnaryExpression(false)
    *       PostfixExpression
    *
    * The purpose for disallowing the pre-inc/dec and the leading sign in
    * some instances is to limit the tokens that could follow a cast.  In
    * other words, to be able to determine if the partial sequence:
    *
    *   (T) +
    *
    * ... is a cast to type T of a signed number or an addition to the
    * parenthesized expression T.  Since, in Java, the only signed types
    * and pre/post-inc/dec-remented types are primitives, and since all
    * primitive types are known, the use of T as a cast of a signed or
    * pre-inc/dec-remented value is determinable since T must be in the
    * set of keywords set forth as PrimitiveType; more specifically, for
    * semantic correctness, the type must be a NumericType:
    *
    *   PrimitiveType:
    *       NumericType
    *       boolean
    *   NumericType:
    *       IntegralType
    *       FloatingPointType
    *   IntegralType: one of
    *       byte short int long char
    *   FloatingPointType: one of
    *       float double
    *
    * For a finite automaton parser, this is discussed in 19.1.5.
    *
    * The PostfixExpression grammar:
    *
    *   PostfixExpression:
    *       Primary
    *       Name
    *       PostIncrementExpression
    *       PostDecrementExpression
    *   Primary:
    *       PrimaryNoNewArray
    *       ArrayCreationExpression
    *   Name:
    *       SimpleName
    *       QualifiedName
    *   SimpleName:
    *       Identifier
    *   QualifiedName:
    *       Name . Identifier
    *   PostIncrementExpression:
    *       PostfixExpression ++
    *   PostDecrementExpression:
    *       PostfixExpression --
    *
    *   PrimaryNoNewArray:
    *       Literal
    *       this
    *       ( Expression )
    *       ClassInstanceCreationExpression
    *       FieldAccess
    *       MethodInvocation
    *       ArrayAccess
    *   Literal:
    *       IntegerLiteral
    *       FloatingPointLiteral
    *       BooleanLiteral
    *       CharacterLiteral
    *       StringLiteral
    *       NullLiteral
    *   ClassInstanceCreationExpression:
    *       new ClassType ( ArgumentList-opt )
    *   ArgumentList:
    *       Expression
    *       ArgumentList , Expression
    *   FieldAccess:
    *       Primary . Identifier
    *       super . Identifier
    *   MethodInvocation:
    *       Name ( ArgumentList-opt )
    *       Primary . Identifier ( ArgumentList-opt )
    *       super . Identifier ( ArgumentList-opt )
    *   ArrayAccess:
    *       Name [ Expression ]
    *       PrimaryNoNewArray [ Expression ]
    *
    *   ArrayCreationExpression:
    *       new PrimitiveType DimExprs Dims-opt
    *       new ClassOrInterfaceType DimExprs Dims-opt
    *   DimExprs:
    *       DimExpr
    *       DimExprs DimExpr
    *   DimExpr:
    *       [ Expression ]
    *   Dims:
    *       [ ]
    *       Dims [ ]
    *
    * The JDK 1.1 added the ability to initialize an array within an
    * expression, modifying
    *
    * From "Changes [to the Java Language Specification] for Java 1.1":
    *
    *   You can initialize the contents of an array when you new it. For
    *   example, the following would be a flexible way to create an array
    *   of strings:
    *
    *       String[] martians = new String[] {"Gidney", "Cloyd"};
    *
    * As a result, the ArrayCreationExpression grammar is changed to:
    *
    *   ArrayCreationExpression:
    *       new PrimitiveType DimExprs Dims-opt ArrayInitializer-opt
    *       new PrimitiveType DimExprs-opt Dims ArrayInitializer
    *       new ClassOrInterfaceType DimExprs Dims-opt ArrayInitializer-opt
    *       new ClassOrInterfaceType DimExprs-opt Dims ArrayInitializer
    *   ArrayInitializer:
    *       { VariableInitializers-opt , -opt }
    *   VariableInitializers:
    *       VariableInitializer
    *       VariableInitializers , VariableInitializer
    *
    * The following basic constructs are included here for reference:
    *
    *   Type:
    *       PrimitiveType
    *       ReferenceType
    *   PrimitiveType:
    *       NumericType
    *       boolean
    *   NumericType:
    *       IntegralType
    *       FloatingPointType
    *   IntegralType: one of
    *       byte short int long char
    *   FloatingPointType: one of
    *       float double
    *   ReferenceType:
    *       ClassOrInterfaceType
    *       ArrayType
    *   ClassOrInterfaceType:
    *       Name
    *   ClassType:
    *       ClassOrInterfaceType
    *   InterfaceType:
    *       ClassOrInterfaceType
    *   ArrayType:
    *       PrimitiveType [ ]
    *       Name [ ]
    *       ArrayType [ ]
    *
    * From this grammar, we can determine the starting set of tokens for a
    * unary expression as follows:
    *
    * In cases where a sign is allowed:
    *
    *   +   Unary plus
    *   -   Unary minus
    *   ++  Pre-increment
    *   --  Pre-decrement
    *
    * In all cases:
    *
    *   ~           Bitwise not
    *   !           Logical not
    *   (           Cast or parenthesised expression
    *   new         Class or array creation
    *   this        Either the variable "this" or a field access/method invocation
    *   super       Field access/method invocation
    *   null
    *   true
    *   false
    *   Literal     A literal expression
    *   Identifier  A variable, field access, method invocation, array access
    *
    * @param block  the current block
    *
    * @return the parsed expression
    */
    protected Expression parseUnaryExpression(Block block)
            throws CompilerException
        {
        Expression expr;
        switch (token.id)
            {
            case KEY_BOOLEAN:
            case KEY_BYTE:
            case KEY_CHAR:
            case KEY_SHORT:
            case KEY_INT:
            case KEY_LONG:
            case KEY_FLOAT:
            case KEY_DOUBLE:
                {
                expr = parseNarrowingExpression(block, new TypeExpression(block, current()));
                if (expr instanceof TypeExpression)
                    {
                    // this is a type, not a unary expression, but it is
                    // possible that someone called parseExpression() blindly
                    return expr;
                    }
                }
                break;

            case SEP_LCURLYBRACE:
                // this is not a unary expression, but it is possible that
                // a variable declaration with assignment is being parsed
                return parseArrayInitializer(block);

            case OP_ADD:
                expr = new PlusExpression(current(), parseUnaryExpression(block));
                break;

            case OP_SUB:
                {
                // there is a very odd problem introduced by the Java
                // Language Specification relating to the unary minus:
                // it can be applied to a literal such as the out of
                // range integer 2147483648; the responsibility for
                // handling this is neither the tokenizer's nor the
                // semantic evaluators, so it must be in the parser
                Token   tokMinus = current();
                switch (token.id)
                    {
                    case LIT_INT:
                        expr = new IntExpression(block, tokMinus, current());
                        break;

                    case LIT_LONG:
                        expr = new LongExpression(block, tokMinus, current());
                        break;

                    default:
                        expr = new MinusExpression(tokMinus, parseUnaryExpression(block));
                        break;
                    }
                }
                break;

            case OP_INCREMENT:
                expr = new PreIncExpression(current(), parseUnaryExpression(block));
                break;

            case OP_DECREMENT:
                expr = new PreDecExpression(current(), parseUnaryExpression(block));
                break;

            case OP_BITNOT:
                expr = new BitNotExpression(current(), parseUnaryExpression(block));
                break;

            case OP_LOGICAL_NOT:
                expr = new NotExpression(current(), parseUnaryExpression(block));
                break;

            case SEP_LPARENTHESIS:
                {
                // parse the item in the parentheses
                Token tokParen = current();
                expr = parseExpression(block);
                match(SEP_RPARENTHESIS);

                // determine if it is a type cast
                boolean fCast = false;
                if (expr instanceof TypeExpression)
                    {
                    // ( PrimitiveType Dims-opt ) UnaryExpression
                    // ( Name Dims ) UnaryExpressionNotPlusMinus
                    fCast = true;
                    }
                else
                    {
                    switch (token.id)
                        {
                        case OP_BITNOT:
                        case OP_LOGICAL_NOT:
                        case SEP_LPARENTHESIS:
                        case KEY_NEW:
                        case KEY_THIS:
                        case KEY_SUPER:
                        case IDENT:
                        case LIT_NULL:
                        case LIT_TRUE:
                        case LIT_FALSE:
                        case LIT_CHAR:
                        case LIT_INT:
                        case LIT_LONG:
                        case LIT_FLOAT:
                        case LIT_DOUBLE:
                        case LIT_STRING:
                            // ( Expression ) UnaryExpressionNotPlusMinus
                            fCast = true;
                        }
                    }

                if (fCast)
                    {
                    expr = new CastExpression(tokParen, parseUnaryExpression(block),
                            toTypeExpression(expr));
                    }
                else
                    {
                    // ( Expression )
                    expr = parseNarrowingExpression(block, expr);
                    }
                }
                break;

            case KEY_NEW:
                //  ClassInstanceCreationExpression:
                //      new ClassType ( ArgumentList-opt )
                //  ArrayCreationExpression:
                //      new PrimitiveType DimExprs Dims-opt ArrayInitializer-opt
                //      new PrimitiveType DimExprs-opt Dims ArrayInitializer
                //      new ClassOrInterfaceType DimExprs Dims-opt ArrayInitializer-opt
                //      new ClassOrInterfaceType DimExprs-opt Dims ArrayInitializer
                //  ArrayInitializer:
                //      { VariableInitializers-opt , -opt }
                //  VariableInitializers:
                //      VariableInitializer
                //      VariableInitializers , VariableInitializer
                {
                Token          tokFirst = current();
                TypeExpression type;
                boolean        fArray;

                switch (token.id)
                    {
                    case KEY_BOOLEAN:
                    case KEY_BYTE:
                    case KEY_CHAR:
                    case KEY_SHORT:
                    case KEY_INT:
                    case KEY_LONG:
                    case KEY_FLOAT:
                    case KEY_DOUBLE:
                        // type is primitive; must be array allocation
                        type   = new TypeExpression(block, current());
                        fArray = true;
                        break;

                    default:
                        {
                        // the type is a name; may be array or class allocation
                        NameExpression name = new NameExpression(block, current());
                        while (peek(SEP_DOT) != null)
                            {
                            name.addName(match(IDENT));
                            }

                        type   = new TypeExpression(name);
                        fArray = (token.id == SEP_LBRACKET);
                        }
                        break;
                    }

                if (fArray)
                    {
                    // parse dim exprs then dims
                    ArrayList list    = new ArrayList();
                    boolean   fEmpty  = false;           // parsing dims (not dim exprs)
                    boolean   fInit   = true;            // array requires initializers
                    Token     tokLast;

                    match(SEP_LBRACKET);
                    do
                        {
                        if (fEmpty || token.id == SEP_RBRACKET)
                            {
                            list.add(null);
                            fEmpty = true;
                            }
                        else
                            {
                            list.add(parseExpression(block));
                            fInit = false;
                            }
                        tokLast = match(SEP_RBRACKET);
                        }
                    while (peek(SEP_LBRACKET) != null);
                    Expression[] aexpr = (Expression[]) list.toArray(new Expression[list.size()]);

                    // array initializers
                    ArrayExpression value = null;
                    if (fInit || token.id == SEP_LCURLYBRACE)
                        {
                        value   = parseArrayInitializer(block);
                        tokLast = value.getEndToken();
                        }

                    expr = new NewArrayExpression(block, tokFirst, tokLast, type, aexpr, value);
                    }
                else
                    {
                    Token        tokLParen = match(SEP_LPARENTHESIS);
                    Expression[] aexpr     = parseParameters(block);
                    Token        tokRParen = match(SEP_RPARENTHESIS);
                    expr = parseNarrowingExpression(block, new NewClassExpression(
                            block, tokFirst, type, tokLParen, aexpr, tokRParen));
                    }
                }
                break;

            case KEY_THIS:
                expr = parseNarrowingExpression(block, new ThisExpression(block, current()));
                break;

            case KEY_SUPER:
                expr = parseNarrowingExpression(block, new SuperExpression(block, current()));
                break;

            case IDENT:
                expr = parseNarrowingExpression(block, new NameExpression(block, current()));
                break;

            case LIT_NULL:
                expr = new NullExpression(block, current());
                break;

            case LIT_TRUE:
            case LIT_FALSE:
                expr = new BooleanExpression(block, current());
                break;

            case LIT_CHAR:
                expr = new CharExpression(block, current());
                break;

            case LIT_INT:
                {
                LiteralToken literal = (LiteralToken) current();
                if (literal.isOutOfRange())
                    {
                    logError(ERROR, INTEGRAL_RANGE, null, literal);
                    }
                expr = new IntExpression(block, literal);
                }
                break;

            case LIT_LONG:
                {
                LiteralToken literal = (LiteralToken) current();
                if (literal.isOutOfRange())
                    {
                    logError(ERROR, INTEGRAL_RANGE, null, literal);
                    }
                expr = new LongExpression(block, literal);
                }
                break;

            case LIT_FLOAT:
                expr = new FloatExpression(block, current());
                break;

            case LIT_DOUBLE:
                expr = new DoubleExpression(block, current());
                break;

            case LIT_STRING:
                expr = parseNarrowingExpression(block, new StringExpression(block, current()));
                break;

            default:
                if (token.getCategory() == KEYWORD)
                    {
                    logError(ERROR, KEYWORD_UNEXP, new String[] {token.getText()},
                        token.getLine(), token.getOffset(), 0);
                    }
                else
                    {
                    logError(ERROR, NOT_EXPRESSION, null, token.getLine(), token.getOffset(), 0);
                    }
                throw new SyntaxException();
            }

        // check for post-increment/decrement(s); see PostfixExpression
        while (true)
            {
            switch (token.id)
                {
                case OP_INCREMENT:
                    expr = new PostIncExpression(current(), expr);
                    break;
                case OP_DECREMENT:
                    expr = new PostDecExpression(current(), expr);
                    break;
                default:
                    return expr;
                }
            }
        }

    /**
    * Check for narrowing operators following the passed expression.
    * There are two narrowing operators:
    *
    *   1.  Field ('.')
    *
    *           FieldAccess:
    *               Primary . Identifier
    *               super . Identifier
    *
    *       (Since parsing does not determine the meaning of (semantic
    *       evaluation for) a name, this method also parses qualified names
    *       as if they were field access expressions.)
    *
    *          QualifiedName:
    *               Name . Identifier
    *
    *   2.  Method calls ('(')
    *
    *           MethodInvocation:
    *               Name ( ArgumentList-opt )
    *               Primary . Identifier ( ArgumentList-opt )
    *               super . Identifier ( ArgumentList-opt )
    *           ArgumentList:
    *               Expression
    *               ArgumentList , Expression
    *
    *   3.  Array subscript ('[')
    *
    *           DimExpr:
    *               [ Expression ]
    *
    * For example, after parsing the partial expression "x" from the
    * following input:
    *
    *   x[5][3].y.z[3].foo()[0]  (etc.)
    *
    * ... this method completes the parsing.
    *
    * @param block  the current block
    * @param expr   the expression which may be followed by a narrowing
    *               operator
    *
    * @return the parsed narrowing expression or the original expression if
    *         the expression is not narrowed by a field or array operator
    */
    protected Expression parseNarrowingExpression(Block block, Expression expr)
            throws CompilerException
        {
        while (true)
            {
            switch (token.id)
                {
                case SEP_DOT:
                    {
                    // parsing rules:
                    //  1)  if the token following the dot must is neither
                    //      an identifier nor the keyword "class", a parsing
                    //      error occurs
                    //  2)  otherwise, if the token is the keyword "class",
                    //      then the expression preceding the dot is
                    //      converted to a TypeExpression and the result is
                    //      a ClassExpression
                    //  3)  otherwise, if the expression preceding the dot
                    //      is a NameExpression, then the result is a
                    //      NameExpression
                    //  4)  otherwise, the result is a FieldAccessExpression

                    Token tokDot = current();
                    if (token.id == KEY_CLASS)
                        {
                        expr = new ClassExpression(tokDot, toTypeExpression(expr), current());
                        }
                    else
                        {
                        Token tokName = match(IDENT);
                        if (expr instanceof NameExpression)
                            {
                            ((NameExpression) expr).addName(tokName);
                            }
                        else
                            {
                            expr = new FieldAccessExpression(tokDot, expr, tokName);
                            }
                        }
                    }
                    break;

                case SEP_LPARENTHESIS:
                    {
                    // parsing rules:
                    //  1)  the method name is determined from the expression
                    //      preceding the opening parenthesis; the expression
                    //      must be either a field access expression or a
                    //      name expression or a parsing error occurs
                    //  2)  if the expression is a field access expression,
                    //      then it is replaced with an invocation expression
                    //  3)  if the expression is an unqualified name
                    //      expression, then it is replaced with an
                    //      invocation expression
                    //  4)  if the expression is a qualified name expression,
                    //      then the rightmost portion of the qualified name
                    //      is removed from the name expression and used to
                    //      create an invocation expression that is qualified
                    //      by the remainder of the name expression

                    Expression exprQual;
                    Token tokName;
                    if (expr instanceof FieldAccessExpression)
                        {
                        exprQual = ((FieldAccessExpression) expr).getExpression();
                        tokName  = expr.getEndToken();
                        }
                    else if (expr instanceof NameExpression)
                        {
                        NameExpression exprName = (NameExpression) expr;

                        if (exprName.isQualified())
                            {
                            exprQual = exprName;
                            }
                        else
                            {
                            // need an implicit (fake) "this" or this-class
                            Token tokFirst = exprName.getStartToken();
                            Token tokThis  = new Token(Token.TOK_THIS,
                                    tokFirst.getLine(), tokFirst.getOffset(), 0);
                            exprQual = new ThisExpression(block, tokThis);
                            }

                        tokName = exprName.removeName(); // could be last
                        }
                    else
                        {
                        expr.logError(ERROR, NOT_METHOD_NAME, null, errlist);
                        throw new SyntaxException();
                        }
                    expr = new InvocationExpression(exprQual, tokName, current(),
                            parseParameters(block), match(SEP_RPARENTHESIS));
                    }
                    break;

                case SEP_LBRACKET:
                    {
                    Token tokFirst = current();
                    if (token.id == SEP_RBRACKET)
                        {
                        // the expression is a type because dims encountered
                        TypeExpression type = toTypeExpression(expr);
                        type = new DimensionedExpression(type, match(SEP_RBRACKET));
                        type = parseDimensionedExpression(expr.getBlock(), type);
                        expr = type;
                        }
                    else
                        {
                        expr = new ArrayAccessExpression(tokFirst, expr, parseExpression(block), match(SEP_RBRACKET));
                        }
                    }
                    break;

                default:
                    return expr;
                }
            }
        }

    /**
    * Parse an array initialization.
    *
    *   VariableInitializer:
    *       Expression
    *       ArrayInitializer
    *   ArrayInitializer:
    *       { VariableInitializers-opt , -opt }
    *   VariableInitializers:
    *       VariableInitializer
    *       VariableInitializers , VariableInitializer
    *
    * @param block  the current block
    *
    * @return the array initialization expression
    */
    protected ArrayExpression parseArrayInitializer(Block block)
            throws CompilerException
        {
        Token tokFirst = match(SEP_LCURLYBRACE);

        Expression[] aexpr = NO_EXPRESSIONS;
        // note:  by the grammar it is possible for there to be no elements
        // but just one comma
        if (token.id != SEP_RCURLYBRACE && peek(SEP_COMMA) == null)
            {
            ArrayList list = new ArrayList();
            do
                {
                Expression expr;
                if (token.id == SEP_LCURLYBRACE)
                    {
                    expr = parseArrayInitializer(block);
                    }
                else
                    {
                    expr = parseExpression(block);
                    }
                list.add(expr);
                }
            while (peek(SEP_COMMA) != null && token.id != SEP_RCURLYBRACE);
            aexpr = (Expression[]) list.toArray(new Expression[list.size()]);
            }

        Token tokLast = match(SEP_RCURLYBRACE);

        return new ArrayExpression(block, tokFirst, tokLast, aexpr);
        }

    /**
    * Parse parameters.  (The left paren must already be eaten.  This method
    * does not have eat the right paren either.)
    *
    *   ArgumentList:
    *       Expression
    *       ArgumentList , Expression
    *
    * @param block  the current block
    *
    * @return an array of expressions
    */
    protected Expression[] parseParameters(Block block)
            throws CompilerException
        {
        Expression[] aexpr = NO_EXPRESSIONS;

        if (token.id != SEP_RPARENTHESIS)
            {
            ArrayList list = new ArrayList();
            do
                {
                list.add(parseExpression(block));
                }
            while (peek(SEP_COMMA) != null);
            aexpr = (Expression[]) list.toArray(new Expression[list.size()]);
            }

        return aexpr;
        }

    /**
    * Parses the Dims type-modifier used optionally in type casts and array
    * creation expressions.
    *
    *   Dims:
    *       [ ]
    *       Dims [ ]
    *
    * @param block  not used, but all expression parsing functions take it
    * @param expr   the type expression
    *
    * @return the passed type expression, modified to reflect any dims
    */
    protected TypeExpression parseDimensionedExpression(Block block, TypeExpression expr)
            throws CompilerException
        {
        while (peek(SEP_LBRACKET) != null)
            {
            expr = new DimensionedExpression(expr, match(SEP_RBRACKET));
            }
        return expr;
        }

    /**
    * Convert an expression to a type expression.
    *
    * @param expr  the expression which is assumed to be a type expression
    *
    * @return the type expression
    */
    protected TypeExpression toTypeExpression(Expression expr)
            throws CompilerException
        {
        if (expr instanceof TypeExpression)
            {
            return (TypeExpression) expr;
            }
        else if (expr instanceof NameExpression)
            {
            return new TypeExpression((NameExpression) expr);
            }
        else
            {
            // expression is not a type
            expr.logError(ERROR, NOT_TYPE_NAME, null, errlist);
            throw new SyntaxException();
            }
        }


    // ----- parsing helpers ------------------------------------------------

    /**
    * Returns the current token and advances to the next token.
    *
    * @return the current token
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected Token current()
            throws CompilerException
        {
        Token current = token;
        next();
        return current;
        }

    /**
    * Advances to and returns the next token.
    *
    * @return the next token
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected Token next()
            throws CompilerException
        {
        Tokenizer toker = this.toker;
        if (toker.hasMoreTokens())
            {
            return token = (Token) toker.nextToken();
            }

        // there is an imaginary closing curly after the script
        if (token != null && token.id == SEP_RCURLYBRACE && token.length == 0)
            {
            // the imaginary right curly was already returned ... now there
            // really are no more tokens
            logError(ERROR, UNEXPECTED_EOF, null, token);
            throw new CompilerException();
            }

        // this corresponds to the opening curly which is created by the
        // default constructor for the Block element)
        int iLine = toker.getLine();
        int of    = 0;
        if (token != null)
            {
            iLine = token.getLine();
            of    = token.getOffset() + token.getLength();
            }
        return token = new Token(Token.TOK_RCURLYBRACE, iLine, of, 0);
        }

    /**
    * Verifies that the current token matches the passed token id and, if so,
    * advances to the next token.  Otherwise, a syntax exception is thrown.
    *
    * @param id token id to match
    *
    * @return the current token
    *
    * @exception SyntaxException    thrown if the token does not match
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected Token match(int id)
            throws CompilerException
        {
        if (token.id != id)
            {
            logError(ERROR, TOKEN_EXPECTED, new String[] {Token.DESC[id]},
                    token.getLine(), token.getOffset(), 0);
            throw new SyntaxException();
            }
        return current();
        }

    /**
    * Tests if the current token matches the passed token id and, if so,
    * advances to the next token.
    *
    * @param id token id to peek for
    *
    * @return the current token, if matched, or null
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected Token peek(int id)
            throws CompilerException
        {
        return (token.id == id ? current() : null);
        }

    /**
    * Tests if the current token matches the passed token category and
    * sub-category.  If so, it returns the current token and advances
    * to the next token.
    *
    * @param cat     the category to peek for
    * @param subcat  the sub-category to peek for
    *
    * @return the current token, if matched, or null
    *
    * @exception CompilerException  potentially thrown by the tokenizer
    */
    protected Token peek(int cat, int subcat)
            throws CompilerException
        {
        Token token = this.token;
        return (token.cat == cat && token.subcat == subcat ? current() : null);
        }


    // ----- imported types -------------------------------------------------

    /**
    * Scan the imported type names and determine the DataType of each one.
    * Any illegal types are logged and the compiler continues, assuming that
    * they only have the members present in java.lang.Object.
    *
    * @param ctx  compiler context
    */
    protected void checkImports(Context ctx)
            throws CompilerException
        {
        final DataType DEFAULT = DataType.OBJECT;

        HashMap tblNames = tblImports;      // token name to full name map
        HashMap tblTypes = new HashMap();   // short name to type map

        for (Iterator iter = tblNames.entrySet().iterator(); iter.hasNext(); )
            {
            Entry    entry   = (Entry) iter.next();
            Token    tokName = (Token) entry.getKey();
            String   sFull   = (String) entry.getValue();
            String   sName   = tokName.getText();
            TypeInfo type    = ctx.getTypeInfo(sFull);
            DataType dt      = DEFAULT;
            if (type == null)
                {
                // unknown or invalid type in import
                logError(ERROR, IMPORT_NOT_FOUND, new String[] {sFull}, tokName);
                }
            else
                {
                dt = type.getDataType();
                }

            if (tblTypes.containsKey(sName))
                {
                // duplicate import short name
                logError(ERROR, IMPORT_DUPLICATE, new String[] {sName}, tokName);
                }
            tblTypes.put(sName, dt);

            ctx.addImport(sName, dt);
            }

        // replace the name-to-name lookup with a name-to-type lookup
        tblImports = tblTypes;
        }


    // ----- error logging --------------------------------------------------

    /**
    * Logs the passed error in the error list.
    *
    * @param nSeverity  severity of the error as defined by ErrorList.Constants
    * @param sCode      error code, as defined by the class logging the error
    * @param asParams   replaceable parameters for the error message
    * @param token      location where the error occurred
    *
    * @exception CompilerException If the error list overflows.
    */
    protected void logError(int nSeverity, String sCode, String[] asParams, Token token)
            throws CompilerException
        {
        logError(nSeverity, sCode, asParams,
                token.getLine(), token.getOffset(), token.getLength());
        }

    /**
    * Logs the passed error in the error list.
    *
    * @param nSeverity  severity of the error as defined by ErrorList.Constants
    * @param sCode      error code, as defined by the class logging the error
    * @param asParams   replaceable parameters for the error message
    * @param iLine      line in which the error was detected
    * @param ofInLine   offset of the error within the line
    * @param cchError   length of the detected error
    *
    * @exception CompilerException If the error list overflows.
    */
    protected void logError(int nSeverity, String sCode, String[] asParams, int iLine, int ofInLine, int cchError)
            throws CompilerException
        {
        if (errlist != null)
            {
            try
                {
                errlist.add(new CompilerErrorInfo(nSeverity, sCode, RESOURCES, asParams,
                        iLine, ofInLine, cchError));
                }
            catch (ErrorList.OverflowException e)
                {
                throw new CompilerException();
                }
            }
        }


    // ----- debug output ---------------------------------------------------

    /**
    * Print information about the passed method.
    *
    * @param method  the MethodInfo object to "dump"
    */
    public static void printMethodInfo(MethodInfo method)
        {
        StringBuffer sb = new StringBuffer();

        if (method.isPublic())
            {
            sb.append("public ");
            }
        if (method.isProtected())
            {
            sb.append("protected ");
            }
        if (method.isPackage())
            {
            sb.append("package ");
            }
        if (method.isPrivate())
            {
            sb.append("private ");
            }
        if (method.isAbstract())
            {
            sb.append("abstract ");
            }
        if (method.isStatic())
            {
            sb.append("static ");
            }
        if (method.isFinal())
            {
            sb.append("final ");
            }
        if (method.isAccessible())
            {
            sb.append("[accessible] ");
            }

        sb.append(method.getDataType())
          .append(' ')
          .append(method.getName())
          .append('(');

        int c = method.getParamCount();
        for (int i = 0; i < c; ++i)
            {
            ParamInfo param = method.getParamInfo(i);

            if (i > 0)
                {
                sb.append(", ");
                }

            if (param.isFinal())
                {
                sb.append("final ");
                }

            sb.append(param.getDataType())
              .append(' ')
              .append(param.getName());
            }

        sb.append(')');

        out(sb.toString());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "Compiler";

    /**
    * Empty set of parameters.
    */
    private static final Expression[] NO_EXPRESSIONS = new Expression[0];

    /**
    * Debug mode.
    */
    private static final boolean DEBUG = ("JAVA".equals(System.getProperty("DEBUG")));

    /**
    * The error list to log to.
    */
    protected ErrorList errlist;

    /**
    * The lexical tokenizer.
    */
    protected Tokenizer toker;

    /**
    * The "current" token being evaluated.
    */
    protected Token token;

    /**
    * Imports looked up by "short name".
    */
    protected HashMap tblImports = new HashMap();
    }
