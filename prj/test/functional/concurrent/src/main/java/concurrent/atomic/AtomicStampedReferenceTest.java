/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicStampedReference;
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
 * Base class for {@link AtomicStampedReference} tests.
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public abstract class AtomicStampedReferenceTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AtomicStampedReference<String> value();

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
        int[] aMark = new int[1];
        assertThat(value().get(aMark), nullValue());
        assertThat(aMark[0], is(0));
        }

    @Test
    @Order(2)
    void testSet()
        {
        value().set("ATOMIC", 1);
        assertThat(value().getReference(), is("ATOMIC"));
        assertThat(value().getStamp(), is(1));
        }

    @Test
    @Order(3)
    void testCompareAndSet()
        {
        assertThat(value().compareAndSet("foo", "bar", 1, 2), is(false));
        assertThat(value().compareAndSet("ATOMIC", "atomic", 1, 2), is(true));
        assertThat(value().getReference(), is("atomic"));
        assertThat(value().getStamp(), is(2));
        }

    @Test
    @Order(4)
    void testAttemptStamp()
        {
        assertThat(value().attemptStamp("ATOMIC", 3), is(false));
        assertThat(value().attemptStamp("atomic", 3), is(true));
        }

    @Test
    @Order(5)
    void testToString()
        {
        assertThat(value().toString(), is("atomic (3)"));
        }
    }
