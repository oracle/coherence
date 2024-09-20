/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.oracle.coherence.grpc.messages.cache.v0.MapEventResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerResponse;

import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
import com.tangosol.io.Serializer;
import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.util.MapEvent;
import com.tangosol.util.ObservableMap;
import grpc.proxy.BaseGrpcIT;
import grpc.proxy.TestNamedCacheServiceProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class BaseVersionZeroGrpcIT
        extends BaseGrpcIT
    {
    /**
     * Create an instance of the {@link NamedCacheService} to use for testing.
     *
     * @return an instance of the {@link NamedCacheService} to use for testing
     */
    protected NamedCacheService createCacheService()
        {
        GrpcAcceptorController                controller = GrpcAcceptorController.discoverController();
        NamedCacheService.DefaultDependencies deps       = new NamedCacheService.DefaultDependencies(controller.getServerType());

        deps.setConfigurableCacheFactorySupplier(this::ensureCCF);
        Optional<TestNamedCacheServiceProvider> optional = TestNamedCacheServiceProvider.getProvider();
        assertThat("Cannot find a TestNamedCacheServiceProvider instance, are you running this test from the TCK module instead of from one of the specific Netty or Helidon test modules",
                optional.isPresent(), is(true));
        return optional.get().getService(deps);
        }

    protected <K, V> List<MapEvent<K, V>> toMapEvents(ObservableMap<K, V> map, List<MapListenerResponse> responses,
                                                      Serializer serializer)
        {
        return toMapEventsForFilterId(map, responses, serializer, -1);
        }

    protected <K, V> List<MapEvent<K, V>> toMapEventsForFilterId(ObservableMap<K, V> map,
                                                                 List<MapListenerResponse> responses,
                                                                 Serializer serializer, long filterId)
        {
        List<MapEvent<K, V>> events = new ArrayList<>();
        for (MapListenerResponse response : responses)
            {
            if (response.getResponseTypeCase() == MapListenerResponse.ResponseTypeCase.EVENT)
                {
                MapEventResponse event = response.getEvent();
                assertThat(event, is(notNullValue()));

                if (filterId <= 0 || event.getFilterIdsList().contains(filterId))
                    {
                    events.add(new MapEvent<>(map,
                                              event.getId(),
                                              BinaryHelper.fromByteString(event.getKey(), serializer),
                                              BinaryHelper.fromByteString(event.getOldValue(), serializer),
                                              BinaryHelper.fromByteString(event.getNewValue(), serializer)));
                    }
                }
            }
        return events;
        }

    /**
     * Convert a list of {@link Entry} instances into a {@link Map}.
     *
     * @param list       the list of {@link Entry} instances
     * @param serializer the serializer to use to deserialize the keys and values
     * @param <K>        the type of the entry key
     * @param <V>        the type of te entry value
     *
     * @return the {@link Map} of entries
     */
    protected <K, V> LinkedHashMap<K, V> toMap(List<Entry> list, Serializer serializer)
        {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (Entry entry : list)
            {
            map.put(BinaryHelper.fromByteString(entry.getKey(), serializer),
                    BinaryHelper.fromByteString(entry.getValue(), serializer));
            }
        return map;
        }

    /**
     * Convert a list of {@link Entry} instances into a {@link List} of {@link Map.Entry}
     * instances with deserialized keys and values.
     *
     * @param list       the list of {@link Entry} instances
     * @param serializer the serializer to use to deserialize the keys and values
     * @param <K>        the type of the entry key
     * @param <V>        the type of te entry value
     *
     * @return the {@link Map} of entries
     */
    protected <K, V> List<Map.Entry<K, V>> toList(List<Entry> list, Serializer serializer)
        {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (Entry entry : list)
            {
            map.put(BinaryHelper.fromByteString(entry.getKey(), serializer),
                    BinaryHelper.fromByteString(entry.getValue(), serializer));
            }
        return new ArrayList<>(map.entrySet());
        }
    }
