/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package tracing;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListenerSupport;

import io.opentracing.Scope;
import io.opentracing.Span;

import io.opentracing.util.GlobalTracer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertThat;

/**
 * Base class for cache operation+tracing.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
@SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
public abstract class AbstractCacheOperationsTracingTests
        extends AbstractTracingTest
    {
    // ----- AbstractCacheOperationsTracingTest methods ---------------------

    /**
     * Return the {@link NamedCache cache} under test.
     *
     * @return the {@link NamedCache} under test
     */
    protected abstract NamedCache getNamedCache();

    /**
     * Return the {@code OpenTracing} {@link Scope} for the test.
     *
     * @param sOpName  the name of the operation for the scope
     *
     * @return the {@code OpenTracing} {@link Scope} for the test
     */
    protected abstract Scope startTestScope(String sOpName);

    // ----- methods from AbstractTracingTest -------------------------------

    @Override
    protected void runTest(AbstractTracingTest.TestBody testBody)
        {
        runTest(testBody, getDefaultProperties(), getCacheConfigPath(), "tracing-enabled.xml");
        }

    // ----- test methods ---------------------------------------------------

    @Override
    @Ignore
    @Test
    public void shouldBeDisabledByDefault()
        {
        }

    /**
     * Ensure {@code get} operations produce tracing {@link Span spans}.
     *
     * @param validator  the validator callback that validate the results
     */
    public void testGetTracing(Validator validator)
        {
        runTest(() ->
                runCacheOperation(() ->
                    {
                    NamedCache cache = getNamedCache();
                    assertThat(cache, is(notNullValue()));
                    cache.get("a");
                    }, "GET", validator));
        }

    /**
     * Ensure {@code put} operations produce tracing {@link Span spans}.
     *
     * @param validator  the validator callback that validate the results
     */
    public void testPutTracing(Validator validator)
        {
        runTest(() ->
                runCacheOperation(() ->
                    {
                    NamedCache cache = getNamedCache();
                    assertThat(cache, is(notNullValue()));

                    final AtomicBoolean entryInserted = new AtomicBoolean();
                    final AtomicBoolean entryUpdated  = new AtomicBoolean();
                    final AtomicBoolean entryDeleted  = new AtomicBoolean();
                    cache.addMapListener(new MapListenerSupport.SynchronousListener()
                        {
                        @Override
                        public void entryInserted(MapEvent evt)
                            {
                            entryInserted.compareAndSet(false, true);
                            }

                        @Override
                        public void entryUpdated(MapEvent evt)
                            {
                            entryUpdated.compareAndSet(false, true);
                            }

                        @Override
                        public void entryDeleted(MapEvent evt)
                            {
                            entryDeleted.compareAndSet(false, true);
                            }
                        });

                    cache.put("a", "1");

                    assertThat("entryInserted not invoked",        entryInserted.get(), is(true));
                    assertThat("entryUpdated incorrectly invoked", entryUpdated.get(),  is(false));
                    assertThat("entryDeleted incorrectly invoked", entryDeleted.get(),  is(false));
                    }, "PUT", validator));
        }

    /**
     * Ensure {@code remove} operations produce tracing {@link Span spans}.
     *
     * @param validator  the validator callback that validate the results
     */
    public void testRemoveTracing(Validator validator)
        {
        runTest(() ->
            runCacheOperation(() ->
                {
                NamedCache cache = getNamedCache();
                assertThat(cache, is(notNullValue()));

                cache.put("a", "1"); // not too interested in this event

                final AtomicBoolean entryInserted = new AtomicBoolean();
                final AtomicBoolean entryUpdated  = new AtomicBoolean();
                final AtomicBoolean entryDeleted  = new AtomicBoolean();
                cache.addMapListener(new MapListenerSupport.SynchronousListener()
                    {
                    @Override
                    public void entryInserted(MapEvent evt)
                        {
                        entryInserted.compareAndSet(false, true);
                        }

                    @Override
                    public void entryUpdated(MapEvent evt)
                        {
                        entryUpdated.compareAndSet(false, true);
                        }

                    @Override
                    public void entryDeleted(MapEvent evt)
                        {
                        entryDeleted.compareAndSet(false, true);
                        }
                    });

                cache.remove("a");

                assertThat("entryInserted incorrectly invoked", entryInserted.get(), is(false));
                assertThat("entryUpdated incorrectly invoked",  entryUpdated.get(),  is(false));
                assertThat("entryDeleted not invoked",          entryDeleted.get(),  is(true));
                }, "PUT", validator));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Run the {@code cacheOperation} and invoke the provided {@link Validator} to confirm the results of
     * the operation.
     *
     * @param cacheOperation  cache test logic
     * @param sOpName         the name of the operation (e.g., {@code GET} or {@code PUT}
     * @param validator       the {@link Validator} to validate the results of the tracing
     *
     * @throws Exception if an error occurs during the test
     */
    protected void runCacheOperation(TestBody cacheOperation, String sOpName, Validator validator) throws Exception
        {
        Scope scopeTest = startTestScope(sOpName);
        Span  span      = GlobalTracer.get().activeSpan();
        try
            {
            cacheOperation.run();
            Blocking.sleep(250);
            }
        finally
            {
            scopeTest.close();
            span.finish();
            }

        validator.validate();
        }

    // ----- inner class: Validator -----------------------------------------

    /**
     * {@link FunctionalInterface} to pass generic validation functions to other test functions.
     */
    @FunctionalInterface
    protected interface Validator
        {
        /**
         * Perform validation.
         *
         * @throws RuntimeException if an error occurs
         */
        void validate();
        }
    }
