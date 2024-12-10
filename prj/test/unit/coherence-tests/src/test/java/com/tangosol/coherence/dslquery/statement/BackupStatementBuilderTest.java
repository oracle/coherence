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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.10
 */
public class BackupStatementBuilderTest
    {
    @Test
    public void shouldRealizeQuery()
            throws Exception
        {
        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create("sqlBackupCacheNode(from('test'),file('data.ser'))");
        BackupStatementBuilder builder = BackupStatementBuilder.INSTANCE;

        BackupStatementBuilder.BackupStatement query = builder.realize(context, term, null, null);

        assertThat(query.f_sCache, is("test"));
        assertThat(query.f_sFile, is("data.ser"));
        }

    @Test
    public void shouldThrowExceptionIfFileIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed for backing up cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlBackupCacheNode(from('test'),file(''))");

        BackupStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfFileIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed for backing up cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlBackupCacheNode(from('test'),file())");

        BackupStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfFileIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed for backing up cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlBackupCacheNode(from('test'))");

        BackupStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for backing up cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlBackupCacheNode(from(''),file('data.ser'))");

        BackupStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for backing up cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlBackupCacheNode(from(),file('data.ser'))");

        BackupStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for backing up cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlBackupCacheNode(file('data.ser'))");

        BackupStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldPerformBackupOfNonDistributedCache()
            throws Exception
        {
        File             backupFile   = temporaryFolder.newFile("backup1.dat");
        String           cacheName    = "test";
        String           fileName     = backupFile.getAbsolutePath();
        Session          session      = mock(Session.class);
        CacheService     cacheService = mock(CacheService.class);
        NamedCache       cache        = new WrapperNamedCache(new HashMap(), "test", cacheService);
        ExecutionContext context      = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq("test"), any(TypeAssertion.class), any(NamedMap.Option.class))).thenReturn(cache);

        cache.put("Key-1", "value-1");

        BackupStatementBuilder.BackupStatement statement = new BackupStatementBuilder.BackupStatement(cacheName, fileName);

        statement.execute(context);

        cache.clear();

        DataInput in = new DataInputStream(new FileInputStream(backupFile));

        MapBackupHelper.readMap(in, cache, 1, null);
        assertThat(cache.size(), is(1));
        }

    @Test
    public void shouldAssertCacheExistsInSanityCheck()
            throws Exception
        {
        File             backupFile = temporaryFolder.newFile("backup2.dat");
        String           cacheName  = "test";
        String           fileName   = backupFile.getAbsolutePath();
        ExecutionContext context    = mock(ExecutionContext.class);

        BackupStatementBuilder.BackupStatement query = new BackupStatementBuilder.BackupStatement(cacheName, fileName)
            {
            @Override
            protected void assertCacheName(String sName, ExecutionContext context)
                {
                }
            };

        BackupStatementBuilder.BackupStatement spyQuery = spy(query);

        spyQuery.sanityCheck(context);

        verify(spyQuery).assertCacheName(cacheName, context);
        }

    @Test
    public void shouldPerformBackupOfDistributedCache()
            throws Exception
        {
        File                     backupFile   = temporaryFolder.newFile("backup2.dat");
        String                   cacheName    = "test";
        String                   fileName     = backupFile.getAbsolutePath();
        Session                  session      = mock(Session.class);
        DistributedCacheService  cacheService = mock(DistributedCacheService.class);
        NamedCache               cache        = new WrapperNamedCache(new HashMap(), "test", cacheService);
        ExecutionContext         context      = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq("test"), any(TypeAssertion.class), any(NamedMap.Option.class))).thenReturn(cache);
        when(cacheService.getPartitionCount()).thenReturn(13);

        cache.put("Key-11", "value-11");

        BackupStatementBuilder.BackupStatement statement
                = new BackupStatementBuilder.BackupStatement(cacheName, fileName);

        statement.execute(context);

        cache.clear();

        DataInput in = new DataInputStream(new FileInputStream(backupFile));

        MapBackupHelper.readMap(in, cache, 1, null);
        assertThat(cache.size(), is(1));
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
