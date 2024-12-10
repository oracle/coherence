/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.Statement;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dsltools.precedence.OPException;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import org.junit.Test;

import static com.tangosol.coherence.dslquery.TermMatcher.matchingTerm;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.03.06
 */
public class SetTimeoutStatementBuilderTest
    {
    // ----- SetTimeoutStatementBuilder tests --------------------------------

    @Test
    public void shouldRealizeStatementWithNoMagnitudeSpecified()
            throws Exception
        {
        ExecutionContext           ctx       = mock(ExecutionContext.class);
        NodeTerm                   term      = (NodeTerm) Terms.create("setTimeout('1234')");
        SetTimeoutStatementBuilder builder   = new SetTimeoutStatementBuilder();
        Statement                  statement = builder.realize(ctx, term, null, null);
        Duration                   expected  = new Duration(1234, Duration.Magnitude.MILLI);

        assertThat(statement, is(instanceOf(SetTimeoutStatementBuilder.SetTimeoutStatement.class)));
        assertThat(((SetTimeoutStatementBuilder.SetTimeoutStatement) statement).f_durationTimeout, is(expected));
        }

    @Test
    public void shouldRealizeStatementWithMagnitudeSpecified()
            throws Exception
        {
        ExecutionContext           ctx       = mock(ExecutionContext.class);
        NodeTerm                   term      = (NodeTerm) Terms.create("setTimeout('10m')");
        SetTimeoutStatementBuilder builder   = new SetTimeoutStatementBuilder();
        Statement                  statement = builder.realize(ctx, term, null, null);
        Duration                   expected  = new Duration(10, Duration.Magnitude.MINUTE);

        assertThat(statement, is(instanceOf(SetTimeoutStatementBuilder.SetTimeoutStatement.class)));
        assertThat(((SetTimeoutStatementBuilder.SetTimeoutStatement) statement).f_durationTimeout, is(expected));
        }

    @Test(expected = CohQLException.class)
    public void shouldFailToRealizeStatementForNullTimeout()
            throws Exception
        {
        ExecutionContext          ctx     = mock(ExecutionContext.class);
        NodeTerm                  term    = (NodeTerm) Terms.newTerm("setTimeout", AtomicTerm.createNull());
        SetTimeoutStatementBuilder builder = new SetTimeoutStatementBuilder();

        builder.realize(ctx, term, null, null);
        }

    @Test(expected = CohQLException.class)
    public void shouldFailToRealizeStatementForBlankTimeout()
            throws Exception
        {
        ExecutionContext          ctx     = mock(ExecutionContext.class);
        NodeTerm                  term    = (NodeTerm) Terms.create("setTimeout('')");
        SetTimeoutStatementBuilder builder = new SetTimeoutStatementBuilder();

        builder.realize(ctx, term, null, null);
        }

    @Test(expected = CohQLException.class)
    public void shouldFailToRealizeStatementWithInvalidTimeout()
            throws Exception
        {
        ExecutionContext          ctx     = mock(ExecutionContext.class);
        NodeTerm                  term    = (NodeTerm) Terms.create("setTimeout('12xxx')");
        SetTimeoutStatementBuilder builder = new SetTimeoutStatementBuilder();

        builder.realize(ctx, term, null, null);
        }

    @Test
    public void shouldReturnCorrectOPToken()
            throws Exception
        {
        SetTimeoutStatementBuilder builder = new SetTimeoutStatementBuilder();

        assertThat(builder.instantiateOpToken(), is(instanceOf(SetTimeoutStatementBuilder.SetTimeoutOPToken.class)));
        }

    // ----- SetTimeoutOPToken tests -----------------------------------------

    @Test
    public void shouldParseStatement()
            throws Exception
        {
        OPParser                                          parser  = mock(OPParser.class);
        OPScanner                                         scanner = mock(OPScanner.class);
        SetTimeoutStatementBuilder                        builder = new SetTimeoutStatementBuilder();
        AbstractQueryPlusStatementBuilder.AbstractOPToken token   = builder.instantiateOpToken();

        when(parser.getScanner()).thenReturn(scanner);
        when(scanner.isEndOfStatement()).thenReturn(false, false, false, true);
        when(scanner.getCurrentAsStringWithAdvance()).thenReturn("10", "m");

        Term term = token.nud(parser);

        assertThat(term, is(matchingTerm("setTimeout('10 m')")));
        }

    @Test(expected = OPException.class)
    public void shouldFailToParseStatement()
            throws Exception
        {
        OPParser                                          parser  = mock(OPParser.class);
        OPScanner                                         scanner = mock(OPScanner.class);
        SetTimeoutStatementBuilder                        builder = new SetTimeoutStatementBuilder();
        AbstractQueryPlusStatementBuilder.AbstractOPToken token   = builder.instantiateOpToken();

        when(parser.getScanner()).thenReturn(scanner);
        when(scanner.isEndOfStatement()).thenReturn(true);

        token.nud(parser);
        }

    // ----- SetTimeoutStatement tests ---------------------------------------

    @Test
    public void shouldSetTimeout()
            throws Exception
        {
        SetTimeoutStatementBuilder builder         = new SetTimeoutStatementBuilder();
        NodeTerm                   term            = (NodeTerm) Terms.create("setTimeout('30 s')");
        Statement                  statement       = builder.realize(null, term, null, null);
        ExecutionContext           ctx             = mock(ExecutionContext.class);

        StatementResult result = statement.execute(ctx);

        assertThat(result, is(notNullValue()));
        assertThat(result.getResult(), is((Object) "CohQL statement timeout set to 30s"));
        }
    }
