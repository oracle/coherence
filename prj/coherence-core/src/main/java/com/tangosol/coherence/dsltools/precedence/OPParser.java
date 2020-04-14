/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.base.NestedBaseTokens;
import com.tangosol.coherence.dsltools.base.BaseTokenStream;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import java.io.Reader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
* OPParser is the parser class for the Top Down Operator Presidence Parsing
* framework.  Top-down Operator Precedence Parsing is a technique invented by
* Vaughan Pratt in the 1970's.
*
* The fundamental idea is to associate semantics with tokens embodied in
* objects rather than using grammar rules embodied in procedures.  The
* token's job is to transform pieces of syntax into Abstract Syntax Trees (AST).
* Each token has a "binding power" that determines operator precedence and
* two functions, "nud" (null denotation) and "led" (left denotation).
* The "nud" function is typically used to process tokens that have a role of
*  "literal" or "variable".  The "led" function is used for tokens that are
* typically in the role of an operator such as "+" or "*".   The parsing
* process is driven by a very simple algorithm embodied in a method named
* "expression" which takes a right binding power as argument. The expression
* method starts by processing the first token (usually a literal or a
* variable) by invoking its "nud" method.  The expression method then loops
* fetching the next token and building an AST by dispatching to the current
* token's "led" methods so long as the passed right binding power is less
* than the next token's left binding power.  These "led" methods may eat as
* many tokens as is warranted by their semantics and they may make recursive
* calls back to the parsers expression method using their binding power as
* the right binding power argument.
*
* For example, to parse the expression "1 + 2", the parser's expression
* method would call "nud" on the token for the literal "1". The "nud" of a
* literal simply returns an atomic term. Then the parser checks if the right
* binding power  is less than the left binding power of the next token and
* if true, it calls "led" on that next token. In this case the right binding
* power is 0 and the binding power of the "+" operator token is 50. So the
* parser calls "led" on the "+" operator token passing itself and the left
* AST node term ("1").
*
* The "led" implementation of the "+" operator recursively calls the parser's
* expression method with a right binding power of 50. The expression calls
* "nud" on the next token, the literal token "2" which returns an atomic term
* for "2". The loop in expression is avoided because the next token is a
* special token used to represent the end of stream. It has a left binding
* power of -1 which is lower than any possible right binding power.
* Therefore, the parser returns the atomic term "2". The caller
* ("+" operator "led") returns an AST term for the "+" operator that has
* left and right terms. In this case the left term is the atomic "1" and
* the right is the atomic "2".
*
* Had we been parsing a slightly more complicated example such as "1 + 2 * 3"
* when the "led" for "+" recursively called expression with its binding power
* of 50 as the right binding power then the three tokens "2", "*", and "3"
* will be consumed because "*" has a left binding power of 60 which lets
* expression's loop calls the "led" of "*" ultimately returning an AST that
* represents "*(2,3) "to be used as the right side term for "+".
*
* @see OPToken
*
* @author djl  2009.03.14
*/
public class OPParser
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new OPParser that parses the given expression in the
    * language defined by the given TokenTable.
    * @param sExper        string expression to parse
    * @param table         the TokenTable that defines the language to parse
    * @param setOperators  the operators this parser will use
    */
     public OPParser(String sExper, TokenTable table, Set<CharSequence> setOperators)
        {
        this(new StringReader(sExper), table, setOperators);
        }

    /**
    * Construct a new OPParser that parses the given Reader in the
    * language defined by the given TokenTable.
    * @param reader        Reader with the chars to parse
    * @param table         the TokenTable that defines the language to parse
    * @param setOperators  the operators this parser will use
    */
     public OPParser(Reader reader, TokenTable table, Set<CharSequence> setOperators)
        {
        m_scanner = new OPScanner(table, setOperators);
        m_scanner.scan(reader);
        }

    /**
    * Construct a new OPParser initialized to gthe given Scanner
    *
    * @param scanner   OPScanner that is already initialized
    */
    public OPParser(OPScanner scanner)
        {
        m_scanner = scanner;
        m_scanner.reset();
        }


    // ----- Parser API -----------------------------------------------------

    /**
    * Obtain the OPScanner that backs this parser
    *
    * @return the OPScanner that acts as a token source
    */
    public OPScanner getScanner()
        {
        return m_scanner;
        }

    /**
    * Parse the next expression into an Abstract Syntax Tree.
    *
    * @param <T>  the term type
    *
    * @return an ASTNode that represents the structured meaning of the
    *         expression being parsed
    */
     public <T extends Term> T parse()
        {
        return (T) expression(0);
        }

    /**
    * Parse the next expression into an Abstract Syntax Tree using the
    * given right binding power. Parsing will continue so long as the right
    * binding power is less than the current tokens left binding power.
    * This is the central algorithm of Vaughan Pratt's Top Down Operator
    * Precedence Parser.
    *
    * @param nRightBindingPower  defines how strong token must bind to the
    *                            left before halting parsing
    *
    * @return an ASTNode that represents the structured meaning of the
    *         expression being parsed
    */
    public Term expression(int nRightBindingPower)
        {
        OPScanner scanner = m_scanner;
        OPToken   t       = m_scanner.getCurrent();
        if (t == null)
            {
            return AtomicTerm.createNull();
            }
        scanner.next();
        Term left = t.nud(this);
        while (((t = scanner.getCurrent()) != null) &&
                nRightBindingPower < t.leftBindingPower())
            {
            scanner.next();
            left = t.led(this, left);
            }
        return left;
        }


    // ----- Utility List Gathering API -------------------------------------

    /**
    * Parse a comma separated sequence of expressions upto the given end
    * marker. Return an array of ASTNodes
    *
    * @param sEndMarker  defines the symbol that ends the sequence
    *
    * @return an ASTNode array that represents the sequence
    */
    public Term[] nodeList(String sEndMarker)
        {
        return nodeList(sEndMarker, false);
        }


    /**
    * Parse a comma separated sequence of expressions upto the given end
    * marker. Given flag controll if end of stream overides the end marker.
    *  Return an array of ASTNodes
    *
    * @param sEndMarker  defines the symbol that ends the sequence
    * @param fEndStreamAllowed flag to overide testing for sEndMarker if
    *                          at the end of stream
    *
    * @return an ASTNode array that represents the sequence
    */
    public Term[] nodeList(String sEndMarker, boolean fEndStreamAllowed)
        {
        List      lst     = new ArrayList();
        OPScanner scanner = m_scanner;

        while (!scanner.isEndOfStatement() && !scanner.matches(sEndMarker))
            {
            Term t = expression(0);
            lst.add(t);
            if (fEndStreamAllowed && scanner.isEndOfStatement())
                {
                return (Term[]) lst.toArray(new Term[lst.size()]);
                }
            if (scanner.matches(sEndMarker))
                {
                break;
                }
            scanner.advance(",");
            }
        if (!scanner.matches(sEndMarker))
            {
            throw new OPException("Unfullfilled expectation \"" +
                    sEndMarker + "\" not found!");
            }
        return (Term[]) lst.toArray(new Term[lst.size()]);
        }

    /**
    * Parse a comma separated sequence of expressions to the of the tokens.
    * Return an array of ASTNodes
    *
    * @return an ASTNode array that represents the sequence
    */
    public Term[] nodeList()
        {
        List      lst     = new ArrayList();
        OPScanner scanner = m_scanner;

        while (!scanner.isEndOfStatement())
            {
            Term t = expression(0);
            lst.add(t);
            if (scanner.isEndOfStatement())
                {
                break;
                }
            if (!scanner.advanceWhenMatching(","))
                {
                break;
                }
            }
        return (Term[]) lst.toArray(new Term[lst.size()]);
        }

    /**
    * Build an array of ASTNodes by processing the this tokens nest as
    * a comma separated list.
    * of BaseTokens.
    *
    * @param nest the nest of BaseTokens to process
    *
    * @return an array of AstNodes
    */
    public Term[] readNestedCommaSeparatedList(NestedBaseTokens nest)
        {
        OPScanner scanner = m_scanner;
        ArrayList lst     = new ArrayList();

        scanner.pushStream(new BaseTokenStream(nest));
        while (!scanner.isEndOfStatement())
            {
            Term t = expression(0);
            lst.add(t);
            if (getScanner().isEndOfStatement())
                {
                break;
                }
            getScanner().advance(",");
            }

        getScanner().popStream();
        return (Term[]) lst.toArray(new Term[lst.size()]);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The OPScanner used as the stream of tokens
    */
    OPScanner m_scanner;
    }