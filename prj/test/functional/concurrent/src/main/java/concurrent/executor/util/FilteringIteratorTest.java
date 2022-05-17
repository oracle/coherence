/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor.util;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.util.FilteringIterator;
import com.tangosol.util.function.Remote.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Functional tests for {@link FilteringIterator}.
 *
 * @author bo
 * @since 21.12
 */
public class FilteringIteratorTest
    {
    /**
     * Ensure iteration doesn't occur on an empty collection.
     */
    @Test
    public void shouldNotIterateOverEmptyCollection()
        {
        assertThrows(NoSuchElementException.class, () ->
            {
            Iterator<String>          iterator = Collections.emptyIterator();
            FilteringIterator<String> filtered = new FilteringIterator<>(iterator, Predicates.AlwaysPredicate.get());

            assertThat(filtered.hasNext(), is(false));

            filtered.next();

            Assertions.fail("Should not acquire a value from the empty iterator");
            });
        }

    /**
     * Should filter a singleton collection.
     */
    @Test
    public void shouldFilterASingletonCollection()
        {
        assertThrows(NoSuchElementException.class, () ->
            {
            Iterator<String>          iterator = Collections.singletonList("Hello").iterator();
            FilteringIterator<String> filtered =
                    new FilteringIterator<>(iterator, (Predicate<String>) string -> string.contains("World"));

            assertThat(filtered.hasNext(), is(false));

            filtered.next();

            Assertions.fail("Should not acquire a value from the filtered iterator");
            });
        }

    /**
     * Should filter a multi-value collection.
     */
    @Test
    public void shouldFilterACollection()
        {
        Iterator<String>          iterator = Arrays.asList("Hello", "World", "Gudday", "Bonjour").iterator();
        FilteringIterator<String> filtered =
                new FilteringIterator<>(iterator, (Predicate<String>) string -> string.contains("a"));

        assertThat(filtered.hasNext(), is(true));

        String value = filtered.next();

        assertThat(value, is("Gudday"));

        assertThat(filtered.hasNext(), is(false));
        }
    }
