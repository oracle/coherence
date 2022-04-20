/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.prescedence;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;

import com.tangosol.coherence.dsltools.precedence.IdentifierOPToken;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.precedence.OPToken;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * @author jk 2014.08.06
 */
public class OPScannerTest
    {

    @Test
    public void shouldScanIdentifier() throws Exception
        {
        OPScanner scanner = new OPScanner(f_language.sqlTokenTable(), f_language.getOperators());
        String    sTokens = "foo";

        scanner.scan(sTokens);

        OPToken current = scanner.getCurrent();
        assertThat(current, is(instanceOf(IdentifierOPToken.class)));
        assertThat(current.getValue(), is("foo"));
        }

    @Test
    public void shouldBeAtEnd() throws Exception
        {
        OPScanner scanner = new OPScanner(f_language.sqlTokenTable(), f_language.getOperators());
        String    sTokens = "foo";

        scanner.scan(sTokens);
        scanner.advance();

        assertThat(scanner.isEnd(), is(true));
        }

    @Test
    public void shouldBeAtEndOfStatement() throws Exception
        {
        OPScanner scanner = new OPScanner(f_language.sqlTokenTable(), f_language.getOperators());
        String    sTokens = "foo;";

        scanner.scan(sTokens);

        scanner.advance();
        assertThat(scanner.isEndOfStatement(), is(true));

        scanner.advance();
        assertThat(scanner.isEndOfStatement(), is(true));
        }

    protected final CoherenceQueryLanguage f_language = new CoherenceQueryLanguage();
    }
