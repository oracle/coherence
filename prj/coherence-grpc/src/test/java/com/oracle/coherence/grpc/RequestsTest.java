/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.google.protobuf.ByteString;

import com.tangosol.net.cache.CacheMap;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2019.11.27
 */
class RequestsTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    void shouldCreateAddIndexRequest()
        {
        AddIndexRequest request = Requests.addIndex("test", "foo", "pof", BYTES_1);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is("test"));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getExtractor(),  is(BYTES_1));
        assertThat(request.getSorted(),     is(false));
        assertThat(request.getComparator(), is(ByteString.EMPTY));
        }

    @Test
    void shouldCreateAddIndexRequestWithDefaultScope()
        {
        AddIndexRequest request = Requests.addIndex(null, "foo", "pof", BYTES_1);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getExtractor(),  is(BYTES_1));
        assertThat(request.getSorted(),     is(false));
        assertThat(request.getComparator(), is(ByteString.EMPTY));
        }

    @Test
    void shouldCreateAddIndexRequestSorted()
        {
        AddIndexRequest request = Requests.addIndex("test", "foo", "pof", BYTES_1, true);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is("test"));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getExtractor(),  is(BYTES_1));
        assertThat(request.getSorted(),     is(true));
        assertThat(request.getComparator(), is(ByteString.EMPTY));
        }

    @Test
    void shouldCreateAddIndexRequestSortedWithDefaultScope()
        {
        AddIndexRequest request = Requests.addIndex(null, "foo", "pof", BYTES_1, true);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getExtractor(),  is(BYTES_1));
        assertThat(request.getSorted(),     is(true));
        assertThat(request.getComparator(), is(ByteString.EMPTY));
        }

    @Test
    void shouldCreateAddIndexRequestWithComparator()
        {
        AddIndexRequest request = Requests.addIndex("test", "foo", "pof", BYTES_1, true, BYTES_2);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is("test"));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getExtractor(),  is(BYTES_1));
        assertThat(request.getSorted(),     is(true));
        assertThat(request.getComparator(), is(BYTES_2));
        }

    @Test
    void shouldCreateAddIndexRequestWithComparatorWithDefaultScope()
        {
        AddIndexRequest request = Requests.addIndex(null, "foo", "pof", BYTES_1, true, BYTES_2);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getExtractor(),  is(BYTES_1));
        assertThat(request.getSorted(),     is(true));
        assertThat(request.getComparator(), is(BYTES_2));
        }

    @Test
    void shouldCreateAggregateRequestWithFilter()
        {
        AggregateRequest request = Requests.aggregate("test", "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is("test"));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getFilter(),     is(BYTES_1));
        assertThat(request.getAggregator(), is(BYTES_2));
        }

    @Test
    void shouldCreateAggregateRequestWithFilterWithDefaultScope()
        {
        AggregateRequest request = Requests.aggregate(null, "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getFilter(),     is(BYTES_1));
        assertThat(request.getAggregator(), is(BYTES_2));
        }

    @Test
    void shouldCreateAggregateRequestWithKeys()
        {
        List<ByteString> keys    = Arrays.asList(BYTES_3, BYTES_4, BYTES_5);
        AggregateRequest request = Requests.aggregate("test", "foo", "pof", keys, BYTES_2);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is("test"));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getKeysList(),   is(keys));
        assertThat(request.getAggregator(), is(BYTES_2));
        }

    @Test
    void shouldCreateAggregateRequestWithKeysWithDefaultScope()
        {
        List<ByteString> keys    = Arrays.asList(BYTES_3, BYTES_4, BYTES_5);
        AggregateRequest request = Requests.aggregate(null, "foo", "pof", keys, BYTES_2);

        assertThat(request,                 is(notNullValue()));
        assertThat(request.getScope(),      is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),      is("foo"));
        assertThat(request.getFormat(),     is("pof"));
        assertThat(request.getKeysList(),   is(keys));
        assertThat(request.getAggregator(), is(BYTES_2));
        }

    @Test
    void shouldCreateClearRequest()
        {
        ClearRequest request = Requests.clear("test", "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(),      is("test"));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateClearRequestWithDefaultScope()
        {
        ClearRequest request = Requests.clear(null, "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(),      is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateContainsEntryRequest()
        {
        ContainsEntryRequest request = Requests.containsEntry("test", "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        }

    @Test
    void shouldCreateContainsEntryRequestWithDefaultScope()
        {
        ContainsEntryRequest request = Requests.containsEntry(null, "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        }

    @Test
    void shouldCreateContainsKeyRequest()
        {
        ContainsKeyRequest request = Requests.containsKey("test", "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        }

    @Test
    void shouldCreateContainsKeyRequestWithDefaultScope()
        {
        ContainsKeyRequest request = Requests.containsKey(null, "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        }

    @Test
    void shouldCreateContainsValueRequest()
        {
        ContainsValueRequest request = Requests.containsValue("test", "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getValue(),  is(BYTES_1));
        }

    @Test
    void shouldCreateContainsValueRequestWithDefaultScope()
        {
        ContainsValueRequest request = Requests.containsValue(null, "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getValue(),  is(BYTES_1));
        }

    @Test
    void shouldCreateDestroyRequest()
        {
        DestroyRequest request = Requests.destroy("test", "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(), is("test"));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateDestroyRequestWithDefaultScope()
        {
        DestroyRequest request = Requests.destroy(null, "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(), is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateEntrySetRequest()
        {
        EntrySetRequest request = Requests.entrySet("test", "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getFilter(), is(BYTES_1));
        }

    @Test
    void shouldCreateEntrySetRequestWithDefaultScope()
        {
        EntrySetRequest request = Requests.entrySet(null, "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getFilter(), is(BYTES_1));
        }

    @Test
    void shouldCreateGetRequest()
        {
        GetRequest request = Requests.get("test", "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        }

    @Test
    void shouldCreateGetRequestWithDefaultScope()
        {
        GetRequest request = Requests.get(null, "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        }

    @Test
    void shouldCreateGetAllRequest()
        {
        List<ByteString> keys    = Arrays.asList(BYTES_3, BYTES_4, BYTES_5);
        GetAllRequest request = Requests.getAll("test", "foo", "pof", keys);

        assertThat(request,              is(notNullValue()));
        assertThat(request.getScope(),   is("test"));
        assertThat(request.getCache(),   is("foo"));
        assertThat(request.getFormat(),  is("pof"));
        assertThat(request.getKeyList(), is(keys));
        }

    @Test
    void shouldCreateGetAllRequestWithDefaultScope()
        {
        List<ByteString> keys    = Arrays.asList(BYTES_3, BYTES_4, BYTES_5);
        GetAllRequest request = Requests.getAll(null, "foo", "pof", keys);

        assertThat(request,              is(notNullValue()));
        assertThat(request.getScope(),   is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),   is("foo"));
        assertThat(request.getFormat(),  is("pof"));
        assertThat(request.getKeyList(), is(keys));
        }

    @Test
    void shouldCreateInvokeRequest()
        {
        InvokeRequest request = Requests.invoke("test", "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is("test"));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getKey(),       is(BYTES_1));
        assertThat(request.getProcessor(), is(BYTES_2));
        }

    @Test
    void shouldCreateInvokeRequestWithDefaultScope()
        {
        InvokeRequest request = Requests.invoke(null, "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getKey(),       is(BYTES_1));
        assertThat(request.getProcessor(), is(BYTES_2));
        }

    @Test
    void shouldCreateInvokeAllRequestWithFilter()
        {
        InvokeAllRequest request = Requests.invokeAll("test", "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is("test"));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getFilter(),    is(BYTES_1));
        assertThat(request.getKeysCount(), is(0));
        assertThat(request.getProcessor(), is(BYTES_2));
        }

    @Test
    void shouldCreateInvokeAllRequestWithFilterWithDefaultScope()
        {
        InvokeAllRequest request = Requests.invokeAll(null, "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getFilter(),    is(BYTES_1));
        assertThat(request.getKeysCount(), is(0));
        assertThat(request.getProcessor(), is(BYTES_2));
        }

    @Test
    void shouldCreateInvokeAllRequestWithKeys()
        {
        List<ByteString> keys    = Arrays.asList(BYTES_3, BYTES_4, BYTES_5);
        InvokeAllRequest request = Requests.invokeAll("test", "foo", "pof", keys, BYTES_2);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is("test"));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getFilter(),    is(ByteString.EMPTY));
        assertThat(request.getKeysList(),  is(keys));
        assertThat(request.getProcessor(), is(BYTES_2));
        }

    @Test
    void shouldCreateInvokeAllRequestWithKeysWithDefaultScope()
        {
        List<ByteString> keys    = Arrays.asList(BYTES_3, BYTES_4, BYTES_5);
        InvokeAllRequest request = Requests.invokeAll(null, "foo", "pof", keys, BYTES_2);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getFilter(),    is(ByteString.EMPTY));
        assertThat(request.getKeysList(),  is(keys));
        assertThat(request.getProcessor(), is(BYTES_2));
        }

    @Test
    void shouldCreateIsEmptyRequest()
        {
        IsEmptyRequest request = Requests.isEmpty("test", "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(), is("test"));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateIsEmptyRequestWithDefaultScope()
        {
        IsEmptyRequest request = Requests.isEmpty(null, "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(), is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateKeySetRequest()
        {
        KeySetRequest request = Requests.keySet("test", "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getFilter(), is(BYTES_1));
        }

    @Test
    void shouldCreateKeySetRequestWithDefaultScope()
        {
        KeySetRequest request = Requests.keySet(null, "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getFilter(), is(BYTES_1));
        }

    @Test
    void shouldCreatePageRequest()
        {
        PageRequest request = Requests.page("test", "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getCookie(), is(BYTES_1));
        }

    @Test
    void shouldCreatePageRequestWithDefaultScope()
        {
        PageRequest request = Requests.page(null, "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getCookie(), is(BYTES_1));
        }

    @Test
    void shouldCreatePutRequest()
        {
        PutRequest request = Requests.put("test", "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        assertThat(request.getTtl(),    is(CacheMap.EXPIRY_DEFAULT));
        }

    @Test
    void shouldCreatePutRequestWithDefaultScope()
        {
        PutRequest request = Requests.put(null, "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        assertThat(request.getTtl(),    is(CacheMap.EXPIRY_DEFAULT));
        }

    @Test
    void shouldCreatePutRequestWithExpiry()
        {
        PutRequest request = Requests.put("test", "foo", "pof", BYTES_1, BYTES_2, 19L);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        assertThat(request.getTtl(),    is(19L));
        }

    @Test
    void shouldCreatePutRequestWithExpiryWithDefaultScope()
        {
        PutRequest request = Requests.put(null, "foo", "pof", BYTES_1, BYTES_2, 19L);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        assertThat(request.getTtl(),    is(19L));
        }

    @Test
    void shouldCreatePutAllRequest()
        {
        List<Entry>   entries = Arrays.asList(Entry.newBuilder().setKey(BYTES_1).setValue(BYTES_2).build(),
                                             Entry.newBuilder().setKey(BYTES_3).setValue(BYTES_4).build());
        PutAllRequest request = Requests.putAll("test", "foo", "pof", entries);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is("test"));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getEntryList(), is(entries));
        }

    @Test
    void shouldCreatePutAllRequestWithDefaultScope()
        {
        List<Entry>   entries = Arrays.asList(Entry.newBuilder().setKey(BYTES_1).setValue(BYTES_2).build(),
                                              Entry.newBuilder().setKey(BYTES_3).setValue(BYTES_4).build());
        PutAllRequest request = Requests.putAll(null, "foo", "pof", entries);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getEntryList(), is(entries));
        }

    @Test
    void shouldCreatePutIfAbsentRequest()
        {
        PutIfAbsentRequest request = Requests.putIfAbsent("test", "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        assertThat(request.getTtl(),    is(CacheMap.EXPIRY_DEFAULT));
        }

    @Test
    void shouldCreatePutIfAbsentRequestWithDefaultScope()
        {
        PutIfAbsentRequest request = Requests.putIfAbsent(null, "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        assertThat(request.getTtl(),    is(CacheMap.EXPIRY_DEFAULT));
        }

    @Test
    void shouldCreateRemoveRequest()
        {
        RemoveRequest request = Requests.remove("test", "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        }

    @Test
    void shouldCreateRemoveRequestWithDefaultScope()
        {
        RemoveRequest request = Requests.remove(null, "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        }

    @Test
    void shouldCreateRemoveMappingRequest()
        {
        RemoveMappingRequest request = Requests.remove("test", "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        }

    @Test
    void shouldCreateRemoveMappingRequestWithDefaultScope()
        {
        RemoveMappingRequest request = Requests.remove(null, "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        }

    @Test
    void shouldCreateRemoveIndexRequest()
        {
        RemoveIndexRequest request = Requests.removeIndex("test", "foo", "pof", BYTES_1);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is("test"));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getExtractor(), is(BYTES_1));
        }

    @Test
    void shouldCreateRemoveIndexRequestWithDefaultScope()
        {
        RemoveIndexRequest request = Requests.removeIndex(null, "foo", "pof", BYTES_1);

        assertThat(request,                is(notNullValue()));
        assertThat(request.getScope(),     is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),     is("foo"));
        assertThat(request.getFormat(),    is("pof"));
        assertThat(request.getExtractor(), is(BYTES_1));
        }

    @Test
    void shouldCreateReplaceRequest()
        {
        ReplaceRequest request = Requests.replace("test", "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        }

    @Test
    void shouldCreateReplaceRequestWithDefaultScope()
        {
        ReplaceRequest request = Requests.replace(null, "foo", "pof", BYTES_1, BYTES_2);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getKey(),    is(BYTES_1));
        assertThat(request.getValue(),  is(BYTES_2));
        }

    @Test
    void shouldCreateReplaceMappingRequest()
        {
        ReplaceMappingRequest request = Requests.replace("test", "foo", "pof", BYTES_1, BYTES_2, BYTES_3);

        assertThat(request,                    is(notNullValue()));
        assertThat(request.getScope(),         is("test"));
        assertThat(request.getCache(),         is("foo"));
        assertThat(request.getFormat(),        is("pof"));
        assertThat(request.getKey(),           is(BYTES_1));
        assertThat(request.getPreviousValue(), is(BYTES_2));
        assertThat(request.getNewValue(),      is(BYTES_3));
        }

    @Test
    void shouldCreateReplaceMappingRequestWithDefaultScope()
        {
        ReplaceMappingRequest request = Requests.replace(null, "foo", "pof", BYTES_1, BYTES_2, BYTES_3);

        assertThat(request,                    is(notNullValue()));
        assertThat(request.getScope(),         is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),         is("foo"));
        assertThat(request.getFormat(),        is("pof"));
        assertThat(request.getKey(),           is(BYTES_1));
        assertThat(request.getPreviousValue(), is(BYTES_2));
        assertThat(request.getNewValue(),      is(BYTES_3));
        }

    @Test
    void shouldCreateSizeRequest()
        {
        SizeRequest request = Requests.size("test", "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(), is("test"));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateSizeRequestWithDefaultScope()
        {
        SizeRequest request = Requests.size(null, "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(), is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateTruncateRequest()
        {
        TruncateRequest request = Requests.truncate("test", "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(), is("test"));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateTruncateRequestWithDefaultScope()
        {
        TruncateRequest request = Requests.truncate(null, "foo");

        assertThat(request,            is(notNullValue()));
        assertThat(request.getScope(), is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(), is("foo"));
        }

    @Test
    void shouldCreateValuesRequest()
        {
        ValuesRequest request = Requests.values("test", "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is("test"));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getFilter(), is(BYTES_1));
        }

    @Test
    void shouldCreateValuesRequestWithDefaultScope()
        {
        ValuesRequest request = Requests.values(null, "foo", "pof", BYTES_1);

        assertThat(request,             is(notNullValue()));
        assertThat(request.getScope(),  is(Requests.DEFAULT_SCOPE));
        assertThat(request.getCache(),  is("foo"));
        assertThat(request.getFormat(), is("pof"));
        assertThat(request.getFilter(), is(BYTES_1));
        }

    // ----- constants ------------------------------------------------------

    private static final ByteString BYTES_1 = ByteString.copyFrom("one".getBytes());

    private static final ByteString BYTES_2 = ByteString.copyFrom("two".getBytes());

    private static final ByteString BYTES_3 = ByteString.copyFrom("three".getBytes());

    private static final ByteString BYTES_4 = ByteString.copyFrom("four".getBytes());

    private static final ByteString BYTES_5 = ByteString.copyFrom("five".getBytes());
    }
