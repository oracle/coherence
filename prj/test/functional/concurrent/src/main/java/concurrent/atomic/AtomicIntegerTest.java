/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicInteger;
import concurrent.ConcurrentHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Base class for {@link AtomicInteger} tests.
 *
 * @author Aleks Seovic  2020.12.04
 * @since 21.12
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public abstract class AtomicIntegerTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AtomicInteger value();

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
        assertThat(value().get(), is(0));
        }

    @Test
    @Order(2)
    void testSet()
        {
        value().set(5);
        assertThat(value().get(), is(5));
        }

    @Test
    @Order(3)
    void testGetAndSet()
        {
        assertThat(value().getAndSet(3), is(5));
        }

    @Test
    @Order(4)
    void testCompareAndSet()
        {
        assertThat(value().compareAndSet(2, 4), is(false));
        assertThat(value().compareAndSet(3, 4), is(true));
        }

    @Test
    @Order(5)
    void testGetAndIncrement()
        {
        assertThat(value().getAndIncrement(), is(4));
        assertThat(value().get(), is(5));
        }

    @Test
    @Order(6)
    void testGetAndDecrement()
        {
        assertThat(value().getAndDecrement(), is(5));
        assertThat(value().get(), is(4));
        }

    @Test
    @Order(7)
    void testGetAndAdd()
        {
        assertThat(value().getAndAdd(2), is(4));
        assertThat(value().get(), is(6));
        }

    @Test
    @Order(8)
    void testIncrementAndGet()
        {
        assertThat(value().incrementAndGet(), is(7));
        }

    @Test
    @Order(9)
    void testDecrementAndGet()
        {
        assertThat(value().decrementAndGet(), is(6));
        }

    @Test
    @Order(10)
    void testAddAndGet()
        {
        assertThat(value().addAndGet(2), is(8));
        }

    @Test
    @Order(11)
    void testGetAndUpdate()
        {
        assertThat(value().getAndUpdate(v -> v * 2), is(8));
        assertThat(value().get(), is(16));
        }

    @Test
    @Order(12)
    void testUpdateAndGet()
        {
        assertThat(value().updateAndGet(v -> v / 2), is(8));
        }

    @Test
    @Order(13)
    void testGetAndAccumulate()
        {
        assertThat(value().getAndAccumulate(3, (v, x) -> v * x), is(8));
        assertThat(value().get(), is(24));
        }

    @Test
    @Order(14)
    void testAccumulateAndGet()
        {
        assertThat(value().accumulateAndGet(3, (v, x) -> v / x), is(8));
        }

    @Test
    @Order(15)
    void testCompareAndExchange()
        {
        assertThat(value().compareAndExchange(3, 1), is(8));
        assertThat(value().compareAndExchange(8, 1), is(8));
        assertThat(value().get(), is(1));
        }

    @Test
    @Order(16)
    void testPrimitiveValues()
        {
        assertThat(value().intValue(), is(1));
        assertThat(value().longValue(), is(1L));
        assertThat(value().floatValue(), is(1f));
        assertThat(value().doubleValue(), is(1d));
        assertThat(value().shortValue(), is((short) 1));
        assertThat(value().byteValue(), is((byte) 1));
        }

    @Test
    @Order(17)
    void testToString()
        {
        assertThat(value().toString(), is("1"));
        }
    }
