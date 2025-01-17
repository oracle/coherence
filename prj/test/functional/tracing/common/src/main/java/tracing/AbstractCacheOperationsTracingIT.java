/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tracing;

import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListenerSupport;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Base class for cache operation+tracing.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
@SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
public abstract class AbstractCacheOperationsTracingIT
        extends AbstractTracingIT
    {
    // ----- AbstractCacheOperationsTracingTest methods ---------------------

    /**
     * Return the {@link NamedCache cache} under test.
     *
     * @return the {@link NamedCache} under test
     */
    protected abstract NamedCache getNamedCache();

    // ----- methods from AbstractTracingTest -------------------------------

    @Override
    protected void runTest(AbstractTracingIT.TestBody testBody)
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
     * Ensure {@code get} operations produce tracing {@code Span spans}.
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
     * Ensure {@code put} operations produce tracing {@code Span spans}.
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

                    cache.put("a", "1");
                    }, "PUT", validator));
        }

    /**
     * Ensure {@code remove} operations produce tracing {@code Span spans}.
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

                cache.remove("a");
                }, "REMOVE", validator));
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
    protected abstract void runCacheOperation(TestBody cacheOperation, String sOpName, Validator validator)
            throws Exception;

    // ----- inner class: Validator -----------------------------------------

    /**
     * {@link FunctionalInterface} to pass generic validation functions to other test functions.
     */
    @FunctionalInterface
    public interface Validator
        {
        /**
         * Perform validation.
         *
         * @throws RuntimeException if an error occurs
         */
        void validate();
        }
    }
