/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.cache.configuration.CompleteConfiguration;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

/**
 * Unit tests for a {@link javax.cache.configuration.Configuration}.
 *
 * @author Brian Oliver
 * @author Yannis Cosmadopoulos
 * @author Joe Fialli
 * @since 1.0
 */
public class CoherenceConfigurationTests
    {
    /**
     * Method description
     *
     * @param <K>  key type
     * @param <V>  value type
     *
     * @return  a configuration
     */
    public <K, V> CompleteConfiguration<K, V> getCacheConfiguration()
        {
        // TODO: we should run this test with all of the CompleteConfiguration implementations we support
        return new PartitionedCacheConfiguration<K, V>();
        }

    /**
     * Method description
     */
    @Test
    public void checkDefaults()
        {
        CompleteConfiguration<?, ?> config = getCacheConfiguration();

        assertFalse(config.isReadThrough());
        assertFalse(config.isWriteThrough());
        assertFalse(config.isStatisticsEnabled());
        assertTrue(config.isStoreByValue());

        ExpiryPolicy expiryPolicy = config.getExpiryPolicyFactory().create();

        assertEquals(Duration.ETERNAL, expiryPolicy.getExpiryForCreation());
        assertNull(expiryPolicy.getExpiryForAccess());
        assertNull(expiryPolicy.getExpiryForUpdate());
        }

    /**
     * Method description
     */
    @Test
    public void notSameButClone()
        {
        CompleteConfiguration<String, String> config1 = getCacheConfiguration();
        CompleteConfiguration<String, String> config2 = getCacheConfiguration();

        assertNotSame(config1, config2);
        assertEquals(config1, config2);
        }

    /**
     * Method description
     */
    @Test
    public void notSame()
        {
        CompleteConfiguration<?, ?> config1 = getCacheConfiguration();
        CompleteConfiguration<?, ?> config2 = getCacheConfiguration();

        assertNotSame(config1, config2);
        }

    /**
     * Method description
     */
    @Test
    public void equals()
        {
        CompleteConfiguration<?, ?> config1 = getCacheConfiguration();
        CompleteConfiguration<?, ?> config2 = getCacheConfiguration();

        assertEquals(config1, config2);
        }

    /**
     * Method description
     */
    @Test
    public void DurationEquals()
        {
        Duration duration1 = new Duration(TimeUnit.DAYS, 2);
        Duration duration2 = new Duration(TimeUnit.DAYS, 2);

        assertEquals(duration1, duration2);
        }

    /**
     * Method description
     */
    @Test
    public void durationNotEqualsAmount()
        {
        Duration duration1 = new Duration(TimeUnit.DAYS, 2);
        Duration duration2 = new Duration(TimeUnit.DAYS, 3);

        assertFalse(duration1.equals(duration2));
        assertFalse(duration1.hashCode() == duration2.hashCode());
        }

    /**
     * Method description
     */
    @Test
    public void durationNotEqualsUnit()
        {
        Duration duration1 = new Duration(TimeUnit.DAYS, 2);
        Duration duration2 = new Duration(TimeUnit.MINUTES, 2);

        assertFalse(duration1.equals(duration2));
        assertFalse(duration1.hashCode() == duration2.hashCode());

        }

    /**
     * Checks that equals() is semantically meaningful.
     *
     * Also verifies the second requirement in the contract of hashcode:
     * * <li>If two objects are equal according to the <tt>equals(Object)</tt>
     *     method, then calling the <code>hashCode</code> method on each of
     *     the two objects must produce the same integer result.
     */
    @Test
    public void durationEqualsWhenSemanticallyEqualsButExpressedDifferentUnits()
        {
        Duration duration1 = new Duration(TimeUnit.SECONDS, 120);
        Duration duration2 = new Duration(TimeUnit.MINUTES, 2);

        assertEquals(duration1, duration2);
        assertEquals(duration1.hashCode(), duration2.hashCode());
        }

    /**
     * Method description
     */
    @Test
    public void durationEqualsWhenSemanticallyEqualsButExpressedDifferentUnitsHashCode()
        {
        Duration duration1 = new Duration(TimeUnit.SECONDS, 120);
        Duration duration2 = new Duration(TimeUnit.MINUTES, 2);

        assertEquals(duration1, duration2);
        assertEquals(duration1.hashCode(), duration2.hashCode());
        }

    /**
     * Method description
     */
    @Test
    public void durationNotEqualsUnitEquals()
        {
        long     time      = 2;
        Duration duration1 = new Duration(TimeUnit.HOURS, 2);

        time *= 60;

        Duration duration2 = new Duration(TimeUnit.MINUTES, 120);

        assertEquals(duration1, duration2);
        time      *= 60;
        duration2 = new Duration(TimeUnit.SECONDS, time);
        assertEquals(duration1, duration2);
        time      *= 1000;
        duration2 = new Duration(TimeUnit.MILLISECONDS, time);
        assertEquals(duration1, duration2);
        }

    /**
     * Method description
     */
    @Test
    public void DurationExceptions()
        {
        try
            {
            new Duration(null, 2);
            }
        catch (NullPointerException e)
            {
            // expected
            }

        try
            {
            new Duration(TimeUnit.MINUTES, 0);
            }
        catch (NullPointerException e)
            {
            // expected
            }

        try
            {
            new Duration(TimeUnit.MICROSECONDS, 10);
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }

        try
            {
            new Duration(TimeUnit.MILLISECONDS, -10);
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }
    }
