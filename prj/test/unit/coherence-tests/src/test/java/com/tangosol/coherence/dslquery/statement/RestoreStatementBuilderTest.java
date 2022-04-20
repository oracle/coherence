/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.internal.util.MapBackupHelper;

import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.cache.WrapperNamedCache;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.10
 */
public class RestoreStatementBuilderTest
    {
    @Test
    public void shouldRealizeQuery()
            throws Exception
        {
        ExecutionContext        context = mock(ExecutionContext.class);
        NodeTerm                term    = (NodeTerm) Terms.create("sqlRestoreCacheNode(from('test'),file('data.ser'))");

        RestoreStatementBuilder builder = RestoreStatementBuilder.INSTANCE;

        RestoreStatementBuilder.RestoreStatement query = builder.realize(context, term, null, null);

        assertThat(query.f_sCacheName, is("test"));
        assertThat(query.f_sFile, is("data.ser"));
        }

    @Test
    public void shouldThrowExceptionIfFileIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed to restore cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlRestoreCacheNode(from('test'),file(''))");

        RestoreStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfFileIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed to restore cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlRestoreCacheNode(from('test'),file())");

        RestoreStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfFileIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed to restore cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlRestoreCacheNode(from('test'))");

        RestoreStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed to restore cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlRestoreCacheNode(from(''),file('data.ser'))");

        RestoreStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed to restore cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlRestoreCacheNode(from(),file('data.ser'))");

        RestoreStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed to restore cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlRestoreCacheNode(file('data.ser'))");

        RestoreStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldPerformRestoreOfNonDistributedCache()
            throws Exception
        {
        File             file         = temporaryFolder.newFile("Restore1.dat");
        String           cacheName    = "test";
        String           fileName     = file.getAbsolutePath();
        Session          session      = mock(Session.class);
        CacheService     cacheService = mock(CacheService.class);
        NamedCache       cache        = new WrapperNamedCache(new HashMap(), "test", cacheService);
        ExecutionContext context      = mock(ExecutionContext.class);
        DataOutput       dataOutput   = new DataOutputStream(new FileOutputStream(file));

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq("test"), any(TypeAssertion.class), any(NamedMap.Option.class))).thenReturn(cache);

        cache.put("Key-1", "value-1");

        MapBackupHelper.writeMap(dataOutput, cache);
        cache.clear();

        RestoreStatementBuilder.RestoreStatement statement
                = new RestoreStatementBuilder.RestoreStatement(cacheName, fileName);

        statement.execute(context);

        assertThat(cache.size(), is(1));
        assertThat((String) cache.get("Key-1"), is("value-1"));
        }

    @Test
    public void shouldPerformRestoreOfDistributedCache()
            throws Exception
        {
        File                     file         = temporaryFolder.newFile("Restore2.dat");
        String                   cacheName    = "test";
        String                   fileName     = file.getAbsolutePath();
        Session                  session      = mock(Session.class);
        DistributedCacheService  cacheService = mock(DistributedCacheService.class);
        NamedCache               cache        = new WrapperNamedCache(new HashMap(), "test", cacheService);
        ExecutionContext         context      = mock(ExecutionContext.class);
        DataOutput               dataOutput   = new DataOutputStream(new FileOutputStream(file));

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq("test"), any(TypeAssertion.class), any(NamedMap.Option.class))).thenReturn(cache);
        when(cacheService.getPartitionCount()).thenReturn(13);

        cache.put("Key-1", "value-1");

        MapBackupHelper.writeMap(dataOutput, cache);
        cache.clear();

        RestoreStatementBuilder.RestoreStatement statement
                = new RestoreStatementBuilder.RestoreStatement(cacheName, fileName);

        statement.execute(context);
        assertThat(cache.size(), is(1));
        assertThat((String) cache.get("Key-1"), is("value-1"));
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * JUnit rule to use to create and destroy temporary folders for tests
     * that use files. This should ensure all the test file junk is cleaned up.
     */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    }
