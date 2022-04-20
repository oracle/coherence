/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.oracle.coherence.common.base.Timeout;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author jk 2015.03.05
 */
public class StatementExecutorTest
    {
    @Test
    public void shouldTimeoutStatementExecution()
            throws Exception
        {
        final ExecutionContext ctx       = new ExecutionContext();
        final Reader           reader    = new StringReader("select * from foo");
        CoherenceQueryLanguage lang      = new CoherenceQueryLanguage();
        CoherenceQueryLanguage langSpy   = spy(lang);
        StatementStub          statement = new StatementStub();

        ctx.setTimeout(new Duration(500, Duration.Magnitude.MILLI));
        ctx.setCoherenceQueryLanguage(langSpy);
        ctx.setWriter(new PrintWriter(System.out));

        doReturn(statement).when(langSpy).prepareStatement(any(NodeTerm.class), same(ctx), anyList(),
                 any(ParameterResolver.class));

        StatementExecutor executor = new StatementExecutor();

        executor.execute(reader, ctx);

        Assert.assertThat(statement.m_nTimeout, is(lessThanOrEqualTo(500L)));
        }

    /**
     * An implementation of {@link Statement} that capture
     * the value of {@link Timeout#remainingTimeoutMillis()}
     * in its {@link #execute(ExecutionContext)} method.
     */
    public class StatementStub
            implements Statement
        {
        @Override
        public synchronized StatementResult execute(ExecutionContext ctx)
            {
            m_nTimeout = Timeout.remainingTimeoutMillis();

            return StatementResult.NULL_RESULT;
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return null;
            }

        protected long m_nTimeout;
        }
    }
