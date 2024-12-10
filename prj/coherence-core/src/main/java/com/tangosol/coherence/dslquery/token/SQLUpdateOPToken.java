/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * SQLUpdateOPToken is used for parsing and specifying the AST
 * used for an update statement.
 * <p>
 * Syntax:
 * <p>
 * UPDATE 'cache-name' [[AS] alias]
 * SET update-statement {, update-statement}
 * [WHERE conditional-expression]
 *
 * @author djl  2009.09.10
 */
public class SQLUpdateOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLUpdateOPToken with the given parameters.
     *
     * @param id string identifier for this token
     */
    public SQLUpdateOPToken(String id)
        {
        super(id, IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        Term      setlist;
        Term      where;
        Term      alias;
        OPScanner s      = parser.getScanner();
        String    s1     = s.getCurrentAsString();
        String[]  s2and3 = s.peekNext2AsStrings();

        if (s2and3 == null
                ||!(s2and3[0].equalsIgnoreCase("set") || s2and3[0].equalsIgnoreCase("as")
                    || s2and3[1].equalsIgnoreCase("set")))
            {
            return super.nud(parser);
            }

        Term table = Terms.newTerm("from", AtomicTerm.createString(s1));

        s.next();    // eat the cache name

        if (s2and3[0].equalsIgnoreCase("as"))
            {
            alias = Terms.newTerm("alias", AtomicTerm.createString(s2and3[1]));
            s.next();
            s.next();
            }
        else if (s2and3[1].equalsIgnoreCase("set"))
            {
            alias = Terms.newTerm("alias", AtomicTerm.createString(s2and3[0]));
            s.next();
            }
        else
            {
            // no alias and thank goodness for a double peek!
            alias = Terms.newTerm("alias");
            }

        s.advanceWhenMatching("set");
        setlist = Terms.newTerm("setList", parser.nodeList("where", true));

        if (s.advanceWhenMatching("where"))
            {
            where = Terms.newTerm("whereClause", parser.expression(0));
            }
        else
            {
            where = Terms.newTerm("whereClause");
            }

        return Terms.newTerm(FUNCTOR, table, setlist, alias, where);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlUpdateNode";
    }
