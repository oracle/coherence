/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicBoolean;
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
 * Base class for {@link AtomicBoolean} tests.
 *
 * @author Aleks Seovic  2020.12.04
 * @since 21.12
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public abstract class AtomicBooleanTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AtomicBoolean value();

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
        assertThat(value().get(), is(false));
        }

    @Test
    @Order(2)
    void testSet()
        {
        value().set(true);
        assertThat(value().get(), is(true));
        }

    @Test
    @Order(3)
    void testGetAndSet()
        {
        assertThat(value().getAndSet(false), is(true));
        }

    @Test
    @Order(4)
    void testCompareAndSet()
        {
        assertThat(value().compareAndSet(true, false), is(false));
        assertThat(value().compareAndSet(false, true), is(true));
        }

    @Test
    @Order(5)
    void testCompareAndExchange()
        {
        assertThat(value().compareAndExchange(false, true), is(true));
        assertThat(value().compareAndExchange(true, false), is(true));
        assertThat(value().get(), is(false));
        }

    @Test
    @Order(6)
    void testToString()
        {
        assertThat(value().toString(), is("false"));
        }
    }
