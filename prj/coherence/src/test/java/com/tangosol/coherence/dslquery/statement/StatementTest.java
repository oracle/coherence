/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;

import org.junit.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.11
 */
public class StatementTest
    {
    @Test
    public void shouldNotBeValidCacheIfValidateFlagTrueAndNoCacheActive()
            throws Exception
        {
        ExecutionContext ctx                    = mock(ExecutionContext.class);
        ConfigurableCacheFactory  ccf           = mock(ConfigurableCacheFactory.class);
        AbstractStatement query                 = new QueryStub();
        when(ctx.getCacheFactory()).thenReturn(ccf);
        when(ccf.isCacheActive("test",null)).thenReturn(false);
        try
            {
            query.assertCacheName("test", ctx);
            fail("Expected AssertionError");
            }
        catch (AssertionError e)
            {
            assertThat(e.getMessage(), is("cache 'test' does not exist!"));
            }
        }
 

    @Test
    public void shouldBeValidCache()
            throws Exception
        {
        ExecutionContext ctx                    = mock(ExecutionContext.class);
        ConfigurableCacheFactory  ccf           = mock(ConfigurableCacheFactory.class);
        AbstractStatement query                 = new QueryStub();
        when(ctx.getCacheFactory()).thenReturn(ccf);
        when(ccf.isCacheActive("test",null)).thenReturn(true);
        query.assertCacheName("test", ctx);
        }

    @Test
    public void shouldDoNothingOnSanityCheck()
            throws Exception
        {
        AbstractStatement query   = new QueryStub();
        ConfigurableCacheFactory  ccf   = mock(ConfigurableCacheFactory.class);
        ExecutionContext  context       = mock(ExecutionContext.class);

        when(context.getCacheFactory()).thenReturn(ccf);

        query.sanityCheck(context);

        verifyNoMoreInteractions(ccf);
        }

    public static class QueryStub
            extends AbstractStatement
        {
        @Override
        public com.tangosol.coherence.dslquery.StatementResult execute(ExecutionContext ctx)
            {
            return null;
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            }
        }
    }
