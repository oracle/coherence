/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.ByteString;

import com.tangosol.net.CacheService;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.PartitionSet;

import io.grpc.StatusRuntimeException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2019.11.28
 * @since 20.06
 */
class PagedQueryHelperTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldEncodePageRequestCookie()
        {
        int cCount = 257;
        int nPage  = 19;
        PartitionSet parts = new PartitionSet(cCount);
        parts.add(6);
        parts.add(20);
        parts.add(76);

        ByteString bytes = PagedQueryHelper.encodeCookie(parts, nPage);

        assertThat(bytes, is(notNullValue()));

        PartitionedService cacheService = mock(PartitionedService.class);
        when(cacheService.getPartitionCount()).thenReturn(cCount);

        Object[] decoded = PagedQueryHelper.decodeCookie(cacheService, bytes);

        assertThat(decoded, is(notNullValue()));
        assertThat(decoded.length, is(2));
        assertThat(decoded[0], is(parts));
        assertThat(decoded[1], is(nPage));
        }

    @Test
    public void shouldDecodeNullPageRequestCookie()
        {
        int          cCount = 257;
        PartitionSet parts  = new PartitionSet(cCount);
        parts.fill();

        PartitionedService cacheService = mock(PartitionedService.class);
        when(cacheService.getPartitionCount()).thenReturn(cCount);

        Object[] decoded = PagedQueryHelper.decodeCookie(cacheService, null);

        assertThat(decoded, is(notNullValue()));
        assertThat(decoded.length, is(2));
        assertThat(decoded[0], is(parts));
        assertThat(decoded[1], is(0));
        }


    @Test
    public void shouldNotDecodeNullPageRequestCookieForNonPartitionedService()
        {
        CacheService cacheService = mock(CacheService.class);

        assertThrows(StatusRuntimeException.class, () -> PagedQueryHelper.decodeCookie(cacheService, null));
        }

    @Test
    public void shouldDecodeEmptyPageRequestCookie()
        {
        int          cCount = 257;
        PartitionSet parts  = new PartitionSet(cCount);
        parts.fill();

        PartitionedService cacheService = mock(PartitionedService.class);
        when(cacheService.getPartitionCount()).thenReturn(cCount);

        Object[] decoded = PagedQueryHelper.decodeCookie(cacheService, ByteString.EMPTY);

        assertThat(decoded, is(notNullValue()));
        assertThat(decoded.length, is(2));
        assertThat(decoded[0], is(parts));
        assertThat(decoded[1], is(0));
        }

    @Test
    public void shouldNotDecodeEmptyPageRequestCookieForNonPartitionedService()
        {
        CacheService cacheService = mock(CacheService.class);

        assertThrows(StatusRuntimeException.class, () -> PagedQueryHelper.decodeCookie(cacheService, ByteString.EMPTY));
        }
    }
