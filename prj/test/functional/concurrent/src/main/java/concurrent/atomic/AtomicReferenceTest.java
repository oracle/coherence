/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicReference;
import concurrent.ConcurrentHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Base class for {@link AtomicReference} tests.
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public abstract class AtomicReferenceTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AtomicReference<String> value();

    // ----- test lifecycle -------------------------------------------------

    @AfterAll
    void cleanUp()
        {
        ConcurrentHelper.resetAtomics();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    @Order(1)
    void testGet()
        {
        assertThat(value().get(), nullValue());
        }

    @Test
    @Order(2)
    void testSet()
        {
        value().set("atomic");
        assertThat(value().get(), is("atomic"));
        }

    @Test
    @Order(3)
    void testGetAndSet()
        {
        assertThat(value().getAndSet("ATOMIC"), is("atomic"));
        }

    @Test
    @Order(4)
    void testCompareAndSet()
        {
        assertThat(value().compareAndSet("foo", "bar"), is(false));
        assertThat(value().compareAndSet("ATOMIC", "atomic"), is(true));
        }

    @Test
    @Order(5)
    void testGetAndUpdate()
        {
        assertThat(value().getAndUpdate(String::toUpperCase), is("atomic"));
        assertThat(value().get(), is("ATOMIC"));
        }

    @Test
    @Order(6)
    void testUpdateAndGet()
        {
        assertThat(value().updateAndGet(String::toLowerCase), is("atomic"));
        }

    @Test
    @Order(7)
    void testGetAndAccumulate()
        {
        assertThat(value().getAndAccumulate("-ref", (v, x) -> v + x), is("atomic"));
        assertThat(value().get(), is("atomic-ref"));
        }

    @Test
    @Order(8)
    void testAccumulateAndGet()
        {
        assertThat(value().accumulateAndGet("erence", (v, x) -> v + x), is("atomic-reference"));
        }

    @Test
    @Order(9)
    void testCompareAndExchange()
        {
        value().set("atomic");
        assertThat(value().compareAndExchange("foo", "bar"), is("atomic"));
        assertThat(value().get(), is("atomic"));
        assertThat(value().compareAndExchange("atomic", "ATOMIC"), is("atomic"));
        assertThat(value().get(), is("ATOMIC"));
        }

    @Test
    @Order(10)
    void testToString()
        {
        assertThat(value().toString(), is("ATOMIC"));
        }
    }
