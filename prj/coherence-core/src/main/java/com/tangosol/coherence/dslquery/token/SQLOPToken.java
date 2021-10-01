/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token;

import com.tangosol.coherence.dsltools.precedence.IdentifierOPToken;
import com.tangosol.coherence.dsltools.precedence.OPException;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

/**
 * SQLOPToken provides useful convenience methods for subclasses.
 *
 * @author djl  2010.05.04
 */
public class SQLOPToken
        extends IdentifierOPToken

    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLOPToken with the given parameters.
     *
     * @param id  string identifier for this token
     */
    public SQLOPToken(String id)
        {
        this(id, IDENTIFIER_NODE);
        }

    /**
     * Construct a new SQLOPToken with the given parameters.
     *
     * @param id          string identifier for this token
     * @param sNudASTName the ast name to use for constructing an ast
     */
    public SQLOPToken(String id, String sNudASTName)
        {
        super(id, sNudASTName);
        }

    // ----- Utility API ----------------------------------------------------

    /**
     * Check to see if there is an alias and create a Term to hold the alias
     * identifier if one exists
     *
     * @param p                      The current Parser
     * @param expectedNextKeywords   The next keyword to expect
     * @return the alias Term
     */
    protected Term checkAlias(OPParser p, String... expectedNextKeywords)
        {
        OPScanner s     = p.getScanner();
        NodeTerm  alias = new NodeTerm("alias");
        String    s1    = s.getCurrentAsString();
        String    s2    = s.peekNextAsString();

        if (s1 != null)
            {
            if (s1.equalsIgnoreCase("as"))
                {
                if (s2 == null || containsIgnoreCase(s2, expectedNextKeywords))
                    {
                    throw new OPException("Unfullfilled expectation, alias not found");
                    }

                alias.withChild(AtomicTerm.createString(s2));
                s.advance();
                s.advance();
                }
            else if (!containsIgnoreCase(s1, expectedNextKeywords))
        	    {
                alias.withChild(AtomicTerm.createString(s1));
                s.advance();
        	    }
            }

        return alias;
        }

    private boolean containsIgnoreCase(String s, String[] list)
        {
        for (String test : list)
            {
            if (test.equalsIgnoreCase(s))
                {
                return true;
                }
            }

        return false;
        }
    }
