/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.net.RequestIncompleteException;

import java.util.Collections;
import java.util.Iterator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2019.11.29
 * @since 20.06
 */
@SuppressWarnings({"unchecked", "rawtypes", "MismatchedQueryAndUpdateOfCollection"})
class RemoteCollectionTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    void shouldHandleExecutionExceptionCallingSize()
        {
        AsyncNamedCacheClient client     = mock(AsyncNamedCacheClient.class);
        RemoteCollection      collection = new RemoteCollectionStub(client);

        when(client.size()).thenReturn(failedFuture());

        //noinspection ResultOfMethodCallIgnored
        RequestIncompleteException error = assertThrows(RequestIncompleteException.class, collection::size);
        assertThat(error.getCause(), is(instanceOf(ExecutionException.class)));
        assertThat(error.getCause().getCause(), is(sameInstance(ERROR)));
        }

    @Test
    void shouldHandleExecutionExceptionCallingIsEmpty()
        {
        AsyncNamedCacheClient client     = mock(AsyncNamedCacheClient.class);
        RemoteCollection      collection = new RemoteCollectionStub(client);

        when(client.isEmpty()).thenReturn(failedFuture());

        //noinspection ResultOfMethodCallIgnored
        RequestIncompleteException error = assertThrows(RequestIncompleteException.class, collection::isEmpty);
        assertThat(error.getCause(), is(instanceOf(ExecutionException.class)));
        assertThat(error.getCause().getCause(), is(sameInstance(ERROR)));
        }

    @Test
    void shouldHandleExecutionExceptionCallingClear()
        {
        AsyncNamedCacheClient client     = mock(AsyncNamedCacheClient.class);
        RemoteCollection      collection = new RemoteCollectionStub(client);

        when(client.clear()).thenReturn(failedFuture());

        RequestIncompleteException error = assertThrows(RequestIncompleteException.class, collection::clear);
        assertThat(error.getCause(), is(instanceOf(ExecutionException.class)));
        assertThat(error.getCause().getCause(), is(sameInstance(ERROR)));
        }

    @Test
    void shouldFailContainAllForNullKeys()
        {
        AsyncNamedCacheClient client     = mock(AsyncNamedCacheClient.class);
        RemoteCollection      collection = new RemoteCollectionStub(client);

        assertThrows(NullPointerException.class, () -> collection.containsAll(null));
        }

    @Test
    void shouldNotContainAllForEmptyKeys()
        {
        AsyncNamedCacheClient client     = mock(AsyncNamedCacheClient.class);
        RemoteCollection      collection = new RemoteCollectionStub(client);

        assertThat(collection.containsAll(Collections.emptyList()), is(true));
        }


    // ----- helper methods -------------------------------------------------

    private <T> CompletableFuture<T> failedFuture()
        {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ERROR);
        return future;
        }

    // ----- inner class RemoteCollectionStub -------------------------------


    @SuppressWarnings("NullableProblems")
    protected static class RemoteCollectionStub
            extends RemoteCollection
        {
        // ----- constructors -----------------------------------------------

        protected RemoteCollectionStub(AsyncNamedCacheClient client)
            {
            super(client);
            }

        // ----- RemoveCollection methods -----------------------------------

        @Override
        public boolean contains(Object o)
            {
            return false;
            }

        @SuppressWarnings("ConstantConditions")
        @Override
        public Iterator iterator()
            {
            return null;
            }

        @Override
        public Object[] toArray()
            {
            return new Object[0];
            }

        @Override
        public Object[] toArray(Object[] a)
            {
            return new Object[0];
            }

        @Override
        public boolean remove(Object o)
            {
            return false;
            }
        }

    // ----- constants ------------------------------------------------------

    protected static final RuntimeException ERROR = new RuntimeException("Computer says No!");
    }
