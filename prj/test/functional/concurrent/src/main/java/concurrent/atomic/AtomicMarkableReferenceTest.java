/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicMarkableReference;
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
 * Base class for {@link AtomicMarkableReference} tests.
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public abstract class AtomicMarkableReferenceTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AtomicMarkableReference<String> value();

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
        boolean[] aMark = new boolean[1];
        assertThat(value().get(aMark), nullValue());
        assertThat(aMark[0], is(false));
        }

    @Test
    @Order(2)
    void testSet()
        {
        value().set("ATOMIC", true);
        assertThat(value().getReference(), is("ATOMIC"));
        assertThat(value().isMarked(), is(true));
        }

    @Test
    @Order(3)
    void testCompareAndSet()
        {
        assertThat(value().compareAndSet("foo", "bar", true, false), is(false));
        assertThat(value().compareAndSet("ATOMIC", "atomic", true, false), is(true));
        assertThat(value().getReference(), is("atomic"));
        assertThat(value().isMarked(), is(false));
        }

    @Test
    @Order(4)
    void testAttemptMark()
        {
        assertThat(value().attemptMark("ATOMIC", true), is(false));
        assertThat(value().attemptMark("atomic", true), is(true));
        }

    @Test
    @Order(5)
    void testToString()
        {
        assertThat(value().toString(), is("atomic (true)"));
        }
    }
