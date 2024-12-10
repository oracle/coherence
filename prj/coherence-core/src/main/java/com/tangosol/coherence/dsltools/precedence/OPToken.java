/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;
import com.tangosol.util.HashHelper;


/**
* OPToken is the root class for the Top Down Operator Precedence Parsing
* framework's tokens.  This framework was first done by Vaughan Pratt in 1973
* an is undergoing a bit of a renaissance with people that find the typical
* formal grammer tool a bit to heavyweight.
*
* The fundamental idea behind "Pratt Parsers" is that Tokens are objects that
* posses methods that allow them to make precedence decisions, match other
* tokens, and build abstract syntax trees (AST). The central issue of the
* precedence problem is that given an operand object between two operators,
* should the operand be bound to the left operator or the right operator?
* obj1 OP1 obj2 OP2 obj3 like: (1 + 2 * 3)
* Does obj2 bind to OP1 or to OP2? The technique we will use has Token
* objects "know" their precedence levels, and implement methods called "nud"
* (the null denotation in Pratt speak) and "led" (the left denotation).
* A nud method "should" have no interest in the tokens to the left while
* a "led" method does. A nud method is typically used on values such as
* variables and literals and by prefix operators like '-', or 'not'. A led
* method is used by infix operators and suffix operators. A token will often
* have both a nud method and a led method with the canonical example being '-'
* which is both a prefix operator and an infix operator.
* The heart of Pratt's technique is the "expression" function. It takes a
* right binding power that controls how aggressively that it should bind to
* tokens on its right. We also pass the parser to the token objects so that
* their functions may look at context and have access to the tokenizer.
*
* @see OPParser
*
* @author djl  2009.03.14
*/
public class OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new OPToken.
    */
     public OPToken()
        {
        }

    /**
    * Construct a new OPToken with the given parameters.
    *
    * @param sId  string representation of the literal
    */
     public OPToken(String sId)
        {
        m_sValue = sId;
        }

    /**
    * Construct a new OPToken with the given parameters.
    *
    * @param sId  string representation of the token
    * @param nBp  The binding precedence for this token
    */
    public OPToken(String sId, int nBp)
        {
        m_sValue        = sId;
        m_nBindingPower = nBp;
        }

    /**
    * Construct a new OPToken with the given parameters.
    *
    * @param sId       string representation of the token
    * @param sAstName  the type code for this literal token
    */
     public OPToken(String sId, String sAstName)
        {
        m_sValue      = sId;
        m_sLedASTName = sAstName;
        }

    /**
    * Construct a new OPToken with the given parameters.
    *
    * @param sId       string representation of the token
    * @param nBp       The binding precedence for this token
    * @param sAstName  the name for this tokens AST
    */
    public OPToken(String sId, int nBp, String sAstName)
        {
        m_sValue        = sId;
        m_nBindingPower = nBp;
        m_sLedASTName   = sAstName;
        }

    /**
    * Construct a new OPToken with the given parameters.
    *
    * @param sId          string representation of the token
    * @param nBp          The binding precedence for this token
    * @param sLedASTName  the name for this tokens AST
    * @param sNudASTName  the name for this tokens AST
    */
    public OPToken(String sId, int nBp, String sLedASTName,
            String sNudASTName)
        {
        m_sValue        = sId;
        m_nBindingPower = nBp;
        m_sLedASTName   = sLedASTName;
        m_sNudASTName   = sNudASTName;
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * Obtain the power that this token will bind to the left.
    *
    * @return the left binding power
    */
    public int leftBindingPower()
        {
        return m_nBindingPower;
        }

    /**
    * Process this token in the context of parser p with the null
    * denotation. A nud method typically will have no interest in the token
    * to the left. The processing results in an Abstract Syntax Tree Node
    * that captures the meaning
    *
    * @param parser the parser that is the context for parsing
    *
    * @return an AstNode
    */
    public Term nud(OPParser parser)
        {
        throw new OPException("Unexpected use of " + getId() +
            " in prefix position");
        }

    /**
    * Process this token and possibly the given leftNodein the context of
    * a parser with the left denotation. A led method typically will be
    * interested t in the token to the left. The processing results in an
    * Abstract Syntax Tree Node that captures the meaning
    *
    * @param parser    the parser that is the context for parsing
    * @param leftNode  an ast Term that the token is possibly interested in
    *
    * @return an AstNode
    */
    public Term led(OPParser parser, Term leftNode)
        {
        throw new OPException("Unexpected use of " + getId() +
            " in infix position");
        }


    // ----- AST Factory API ------------------------------------------------

    /**
    * Create an Abstract Syntax Node for the given arguments.  If the
    * astName argument is not null then use it for the functor and the
    * given functor argument become the first child Term.
    *
    * @param sAstName   classification functor or null
    * @param sFunctor   functor for ast node to be constructed
    *
    * @return a Term representing the AST
    */
    protected Term newAST(String sAstName, String sFunctor)
        {
        return sAstName == null ?
            AtomicTerm.createString(sFunctor) :
            Terms.newTerm(m_sLedASTName, AtomicTerm.createString(sFunctor));
        }

    /**
    * Create an Abstract Syntax Node for the given arguments.  If the
    * astName argument is not null then use it for the functor otherwise
    * just assume the Term t is good.
    *
    * @param sAstName   classification functor or null
    * @param term       an Term that is part of the ast
    *
    * @return a Term representing the AST
    */
    protected Term newAST(String sAstName, Term term)
        {
        return sAstName == null? term : Terms.newTerm(sAstName, term);
        }

    /**
    * Create an Abstract Syntax Node for the given arguments.  If the
    * astName argument is not null then use it for the functor and the
    * given functor argument become the first child Term.
    *
    * @param sAstName   classification functor or null
    * @param sFunctor   functor for ast node to be constructed
    * @param term       an Term that is part of the ast
    *
    * @return a Term representing the AST
    */
    protected Term newAST(String sAstName, String sFunctor, Term term)
        {
        return sAstName == null ?
            Terms.newTerm(sFunctor, term) :
            Terms.newTerm(sAstName, AtomicTerm.createString(sFunctor), term);
        }

    /**
    * Create an Abstract Syntax Node for the given arguments.  If the
    * astName argument is not null then use it for the functor and the
    * given functor argument become the first child Term.
    *
    * @param sAstName   classification functor or null
    * @param sFunctor   functor for ast node to be constructed
    * @param t1         an Term that is part of the ast
    * @param t2         an Term that is part of the ast
    *
    * @return a Term representing the AST
    */
    protected Term newAST(String sAstName, String sFunctor, Term t1, Term t2)
        {
        return sAstName == null ?
            Terms.newTerm(sFunctor, t1, t2) :
            Terms.newTerm(sAstName,
                    AtomicTerm.createString(sFunctor), t1, t2);
        }

    /**
    * Create an Abstract Syntax Node for the given arguments.  If the
    * astName argument is not null then use it for the functor and the
    * given functor argument become the first child Term.
    *
    * @param sAstName   classification functor or null
    * @param sFunctor   functor for ast node to be constructed
    * @param t1         an Term that is part of the ast
    * @param t2         an Term that is part of the ast
    * @param t3         an Term that is part of the ast
    *
    * @return a Term representing the AST
    */
    protected Term newAST(String sAstName, String sFunctor,
            Term t1, Term t2, Term t3)
        {
        return sAstName == null ?
            Terms.newTerm(sFunctor, t1, t2, t3) :
            Terms.newTerm(sAstName,
                AtomicTerm.createString(sFunctor), t1, t2, t3);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the string representation of this token.
    *
    * @return the string representation
    */
    public String getId()
        {
        return m_sValue;
        }

    /**
    * Set the string representation of this token to the given id.
    *
    * @param sId  the string representation for the token
    */
    public void setId(String sId)
        {
        m_sValue = sId;
        }

    /**
    * Get The binding precedence of this token.
    *
    * @return The binding precedence
    */
    public int getBindingPower()
        {
        return m_nBindingPower;
        }

    /**
    * Set The binding precedence that this token will use for binding to the right.
    *
    * @param nBp  the power power for this token
    */
    public void setBindingPower(int nBp)
        {
        m_nBindingPower = nBp;
        }

    /**
    * Get nud AST Name for this token
    *
    * @return the nud ast name for this token
    */
    public String getNudASTName()
        {
        return m_sNudASTName;
        }

    /**
    * Set the nud AST Name for this token to be the given string
    *
    * @param sAstName  the nud ast name for this token
    */
    public void setNudASTName(String sAstName)
        {
        m_sNudASTName = sAstName;
        }

    /**
    * Get led AST Name for this token
    *
    * @return the led ast name for this token
    */
    public String getLedASTName()
        {
        return m_sLedASTName;
        }

    /**
    * Set the led AST Name for this token to be the given string
    *
    * @param sAstName  the led ast name for this token
    */
    public void setLedASTName(String sAstName)
        {
        m_sLedASTName = sAstName;
         }

    /**
    * Get a string value that identifies this token
    *
    * @return the a string that identifies this token
    */
    public String getValue()
        {
        return m_sValue;
        }

    /**
    * Set the AST Name for this token to be the given string
    *
    * @param s  the ast name for this token
    */
    public void setValue(String s)
        {
        m_sValue = s;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this token.
    *
    * @return a String description of the token
    */
    public String toString()
        {
        return getClass().getName() + " " + getValue();
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        OPToken opToken = (OPToken) o;

        return m_sValue == null ? opToken.m_sValue == null
                : m_sValue.equals(opToken.m_sValue);
        }

    @Override
    public int hashCode()
        {
        return HashHelper.hash(m_sValue, 0);
        }

// ----- constants ------------------------------------------------------

    /**
     * The AST node name for a Binary Operator node.
     */
    public static String BINARY_OPERATOR_NODE = "binaryOperatorNode";

    /**
     * The AST node name for a Binding Node.
     */
    public static String BINDING_NODE = "bindingNode";

    /**
     * The AST node name for a Method Call Node.
     */
    public static String CALL_NODE = "callNode";

    /**
     * The AST node name for a De-referencing Node.
     */
    public static String DEREF_NODE = "derefNode";

    /**
     * The AST node name for a Field List Node.
     */
    public static String FIELD_LIST = "fieldList";

    /**
     * The AST node name for an Identifier Node.
     */
    public static String IDENTIFIER_NODE = "identifier";

    /**
     * The AST node name for a List of Values Node.
     */
    public static String LIST_NODE = "listNode";

    /**
     * The AST node name for a Literal Node.
     */
    public static String LITERAL_NODE = "literal";

    /**
     * The AST node name for a Unary Operator Node.
     */
    public static String UNARY_OPERATOR_NODE = "unaryOperatorNode";

    // ----- binding precedence constants -----------------------------------

    /** 
     * The binding precedence for keyword tokens 
     */
    public static final int PRECEDENCE_KEYWORD = -1;

    /** 
     * The binding precedence for identifier tokens 
     */
    public static final int PRECEDENCE_IDENTIFIER = 1;

    /**
     * The binding precedence for assignment operators assignment
     * such as = += -= *= /= %= &amp;= ^= |= &lt;&lt;= &gt;&gt;= &gt;&gt;&gt;=
     */
    public static final int PRECEDENCE_ASSIGNMENT = 20;

    /**
     * The binding precedence for logical tokens such as &amp;&amp;, ||, etc
     */
    public static final int PRECEDENCE_LOGICAL = 30;

    /**
     * The binding precedence for bitwise logical tokens such as &amp;, |, ^ etc
     */
    public static final int PRECEDENCE_LOGICAL_BITWISE = 35;

    /**
     * The binding precedence for relational operators such as ==, &lt;=, like, contains etc
     */
    public static final int PRECEDENCE_RELATIONAL = 40;

    /**
     * The binding precedence for bitwise operators such as &gt;&gt;&gt; &gt;&gt; and &lt;&lt;
     */
    public static final int PRECEDENCE_BITWISE = 45;

    /** The binding precedence for sum arithmetic, i.e. + and - */
    public static final int PRECEDENCE_SUM = 50;

    /**
     * The binding precedence for product arithmetic, multiplication, division, mod	* / %
     */
    public static final int PRECEDENCE_PRODUCT = 60;

    /** 
     * The binding precedence for exponent arithmetic, i.e. raising by a power 
     */
    public static final int PRECEDENCE_EXPONENT = 61;

    /**
     * The binding precedence for other unary operators: pre-increment, pre-decrement, plus, minus,
     * logical negation, bitwise complement, type cast	++expr --expr +expr -expr ! ~ (type)
     */
    public static final int PRECEDENCE_UNARY = 75;

    /**
     * The binding precedence for unary post operators such as post-increment and post-decrement
     * of the form expr++ or expr--
     */
    public static final int PRECEDENCE_UNARY_POST = 76;

    /**
     * The binding precedence for parentheses ( ) [ ]
     */
    public static final int PRECEDENCE_PARENTHESES = 80;

    // ----- data members ---------------------------------------------------

    /**
    * The string value of the literal.
    */
    protected String m_sValue;

    /**
    * A functor for building ast for led method.
    */
    protected String m_sLedASTName = null;

    /**
    * A functor for building ast for nud method.
    */
    protected String m_sNudASTName = null;

    /**
    * The power that this token binds, typically to the left.
    */
    protected int m_nBindingPower = -1;
    }
