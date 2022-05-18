/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.*;

import java.util.concurrent.TimeUnit;

import javax.cache.*;

import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;

/**
 * Class description
 *
 * @version        Enter version here..., 13/05/20
 * @author         Enter your name here...
 */
public class ConfigurationDurationTests
    {
    /**
     * Method description
     */
    @Test
    public void EternalDurationToString()
        {
        System.out.println("ETERNAL=" + Duration.ETERNAL);
        }

    /**
     * Method description
     */
    @Test
    public void EternalEquals()
        {
        Duration.ETERNAL.equals(Duration.ETERNAL);
        }

    /**
     * Method description
     */
    @Test
    public void EternalEqualsTake2()
        {
        Duration E = new Duration(Long.MAX_VALUE, 0);

        assert(E.isEternal());
        assert(E.equals(Duration.ETERNAL));
        }

    /**
     * Method description
     */
    @Test
    public void EternalEqualsTake3()
        {
        Duration E = EternalExpiryPolicy.factoryOf().create().getExpiryForCreation();

        assert(E != null);
        assert(E.isEternal());
        assert(E.equals(Duration.ETERNAL));
        }

    /**
     * Method description
     */
    @Test
    public void EternalEqualsTake4()
        {
        Duration E = new Duration(Long.MAX_VALUE, 0);

        assert(E.isEternal());
        assert(E.equals(Duration.ETERNAL));
        }

    /**
     * Duration.hashCode was throwing NPE in past.  Just be sure it is called and does not anymore.
     */
    @Test
    public void EternalHashCode()
        {
        // assertion is nonsense.  just trying to avoid findbug complaints that hashCode() result is not being used.
        assert(Duration.ETERNAL.hashCode() != -2);
        }

    /**
     * Method description
     */
    @Test
    public void ZeroDurationToString()
        {
        System.out.println("ZERO=" + Duration.ZERO);

        Duration isNull = null;

        System.out.println("null=" + isNull);
        }

    /**
     * Method description
     */
    @Test
    public void NullDurationToString()
        {
        Duration isNull = null;

        System.out.println("null=" + isNull);
        }

    private Duration pofTest(Duration d)
        {
        if (d == null)
            {
            return null;
            }

        Binary   bin     = ExternalizableHelper.toBinary(d);
        Duration binFrom = (Duration) ExternalizableHelper.fromBinary(bin);

        // if (!binFrom.isEternal()) {
        assert(d.equals(binFrom));

        // }
        return binFrom;
        }

    /**
     * Method description
     */
    @Test
    public void durationPofTest()
        {
        Duration result;

        result = pofTest(Duration.ZERO);
        assert(!result.isEternal());
        assert(result.isZero());

        Duration source = new Duration(TimeUnit.SECONDS, 3);

        result = pofTest(source);
        assert(source.equals(result));
        assert(!source.isEternal());
        assert(!result.isZero());

        result = pofTest(Duration.ETERNAL);
        assert(result.isEternal());
        assert(!result.isZero());
        }
    }
