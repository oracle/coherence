/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.precedence.OPToken;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * SQLBackupOPToken is used for parsing and specifying the AST
 * used for backing up a cache.
 * <p>
 * Syntax:
 * <p>
 * BACKUP CACHE 'cache-name' [TO] [FILE] 'filename'
 *
 * @author djl  2009.09.10
 */
public class SQLBackupOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLBackupOPToken with the given parameters.
     *
     * @param id string identifier for this token
     */
    public SQLBackupOPToken(String id)
        {
        super(id, OPToken.IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        Term      termTable;
        Term      termFile;
        OPScanner scanner = parser.getScanner();

        if (scanner.advanceWhenMatching("cache"))
            {
            termTable = Terms.newTerm("from", AtomicTerm.createString(scanner.getCurrentAsStringWithAdvance()));
            scanner.advanceWhenMatching("to");
            scanner.advanceWhenMatching("file");
            termFile = Terms.newTerm("file", AtomicTerm.createString(scanner.getCurrentAsStringWithAdvance()));

            return Terms.newTerm(FUNCTOR, termTable, termFile);
            }
        else
            {
            return super.nud(parser);
            }

        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlBackupCacheNode";
    }
