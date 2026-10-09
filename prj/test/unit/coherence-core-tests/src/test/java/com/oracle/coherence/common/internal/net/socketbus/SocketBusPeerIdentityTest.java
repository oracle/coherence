/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.socketbus;

import com.oracle.coherence.common.base.Collector;

import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.SingleBufferSequence;

import com.oracle.coherence.common.net.TcpSocketProvider;

import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;

import com.oracle.coherence.common.net.exabus.util.SimpleDepot;
import com.oracle.coherence.common.net.exabus.util.SimpleEvent;
import com.oracle.coherence.common.net.exabus.util.UrlEndPoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.nio.ByteBuffer;

import java.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests protocol v6 MessageBus peer identity.
 *
 * @author Patrick Fry  2026.07.27
 * @since 26.10
 */
public class SocketBusPeerIdentityTest
    {
    @AfterEach
    public void closeBuses()
        {
        for (TestSocketMessageBus bus : f_listBus)
            {
            try
                {
                bus.close();
                }
            catch (RuntimeException ignored)
                {
                }
            }
        f_listBus.clear();
        }

    @Test
    public void shouldKeepV6PeersWithTheSameAdvertisedEndpointDistinct()
            throws Exception
        {
        TestSocketMessageBus busReceiver = createBus(6);
        TestSocketMessageBus busPeerOne  = createBus(6);
        TestSocketMessageBus busPeerTwo  = createBus(6);
        String               sAdvertised = "tmb://127.0.0.1:65530";

        busPeerOne.setAdvertisedEndPoint(sAdvertised);
        busPeerTwo.setAdvertisedEndPoint(sAdvertised);

        TestCollector collectorReceiver = open(busReceiver);
        TestCollector collectorPeerOne  = open(busPeerOne);
        TestCollector collectorPeerTwo  = open(busPeerTwo);

        EndPoint pointReceiver = busReceiver.getLocalEndPoint();
        busPeerOne.connect(pointReceiver);
        busPeerTwo.connect(pointReceiver);

        Event eventConnectOne = collectorReceiver.await(Event.Type.CONNECT);
        Event eventConnectTwo = collectorReceiver.await(Event.Type.CONNECT);
        try
            {
            EndPoint pointPeerOne = eventConnectOne.getEndPoint().equals(busPeerOne.getBoundEndPoint())
                    ? eventConnectOne.getEndPoint()
                    : eventConnectTwo.getEndPoint();
            EndPoint pointPeerTwo = pointPeerOne == eventConnectOne.getEndPoint()
                    ? eventConnectTwo.getEndPoint()
                    : eventConnectOne.getEndPoint();

            assertNotEquals(pointPeerOne, pointPeerTwo);
            assertEquals(Set.of(busPeerOne.getBoundEndPoint(), busPeerTwo.getBoundEndPoint()),
                    Set.of(pointPeerOne, pointPeerTwo));

            send(busPeerOne, pointReceiver, 1);
            send(busPeerTwo, pointReceiver, 2);
            assertEquals(Set.of(1, 2), Set.of(
                    read(collectorReceiver.await(Event.Type.MESSAGE)),
                    read(collectorReceiver.await(Event.Type.MESSAGE))));

            send(busReceiver, pointPeerOne, 3);
            send(busReceiver, pointPeerTwo, 4);
            assertEquals(3, read(collectorPeerOne.await(Event.Type.MESSAGE)));
            assertEquals(4, read(collectorPeerTwo.await(Event.Type.MESSAGE)));

            busReceiver.release(pointPeerOne);
            assertEndPoint(pointPeerOne, collectorReceiver.await(Event.Type.RELEASE));
            await(() -> busReceiver.getPeerEndPoints().size() == 1);

            send(busPeerTwo, pointReceiver, 5);
            assertEquals(5, read(collectorReceiver.await(Event.Type.MESSAGE)));
            }
        finally
            {
            eventConnectOne.dispose();
            eventConnectTwo.dispose();
            }
        }

    @Test
    public void shouldAdvertiseAStableBusIdentity()
            throws IOException
        {
        TestSocketMessageBus bus = createBus(6, -1);
        UrlEndPoint          point = (UrlEndPoint) bus.getLocalEndPoint();

        assertNotNull(point.getPeerIdentity());
        assertEquals(point.getPeerIdentity(),
                ((UrlEndPoint) bus.resolve(point.getCanonicalName())).getPeerIdentity());
        assertEquals(point.getTransportName(),
                ((UrlEndPoint) bus.resolve(point.getTransportName())).getCanonicalName());
        assertEquals(point.getTransportName() + "?bus-id=" + point.getPeerIdentity(),
                point.getCanonicalName());
        }

    @Test
    public void shouldLeavePeerIdentityDisabledByDefault()
            throws IOException
        {
        SocketBusDriver driver = new SocketBusDriver(new SocketBusDriver.DefaultDependencies()
                .setMessageBusProtocol("tmb")
                .setMemoryBusProtocol("trb")
                .setSocketProvider(TcpSocketProvider.INSTANCE)
                .setSocketReconnectDelayMillis(10L)
                .setSocketReconnectLimit(-1));
        driver.setDepot(SimpleDepot.getInstance());

        TestSocketMessageBus bus = createBus(driver, -1);
        String sName = "tmb://127.0.0.1:30100?bus-id=blue&";
        UrlEndPoint point = driver.resolveSocketEndPoint(sName);

        assertEquals(5, bus.getMaximumVersion());
        assertNull(((UrlEndPoint) bus.getLocalEndPoint()).getPeerIdentity());
        assertFalse(bus.getLocalEndPoint().getCanonicalName().contains("bus-id="));
        assertNull(point.getPeerIdentity());
        assertEquals(sName, point.getCanonicalName());
        assertEquals(sName, point.getTransportName());
        assertEquals("bus-id=blue&", point.getQueryString());
        }

    @Test
    public void shouldKeepOrdinaryDriversOnV5WhenFormerSwitchIsSet()
            throws IOException
        {
        String sProperty = "coherence.messagebus.peerIdentity.enabled";
        String sPrevious = System.getProperty(sProperty);
        try
            {
            System.setProperty(sProperty, "true");

            SocketBusDriver.DefaultDependencies deps = new SocketBusDriver.DefaultDependencies()
                    .setMessageBusProtocol("tmb")
                    .setMemoryBusProtocol("trb")
                    .setSocketProvider(TcpSocketProvider.INSTANCE)
                    .setSocketReconnectLimit(-1);
            SocketBusDriver driverEnabled = new SocketBusDriver(deps,
                    SocketBusDriver.PeerIdentityMode.ENABLED);
            SocketBusDriver driverDisabled = new SocketBusDriver(deps);
            driverEnabled.setDepot(SimpleDepot.getInstance());
            driverDisabled.setDepot(SimpleDepot.getInstance());

            TestSocketMessageBus busEnabled  = createBus(driverEnabled, -1);
            TestSocketMessageBus busDisabled = createBus(driverDisabled, -1);

            assertEquals(SocketBusDriver.PeerIdentityMode.ENABLED, driverEnabled.getPeerIdentityMode());
            assertEquals(6, busEnabled.getMaximumVersion());
            assertNotNull(((UrlEndPoint) busEnabled.getLocalEndPoint()).getPeerIdentity());
            assertEquals(SocketBusDriver.PeerIdentityMode.DISABLED, driverDisabled.getPeerIdentityMode());
            assertEquals(5, busDisabled.getMaximumVersion());
            assertNull(((UrlEndPoint) busDisabled.getLocalEndPoint()).getPeerIdentity());

            // the stock Depot used by ordinary MessageBus consumers builds
            // every socket driver through the same one-argument constructor
            Map<String, com.oracle.coherence.common.net.exabus.spi.Driver> mapDrivers =
                    new SimpleDepot.DefaultDependencies().getDrivers();
            assertTrue(mapDrivers.values().stream().anyMatch(SocketBusDriver.class::isInstance));
            assertTrue(mapDrivers.values().stream()
                    .filter(SocketBusDriver.class::isInstance)
                    .map(SocketBusDriver.class::cast)
                    .allMatch(driver -> driver.getPeerIdentityMode() == SocketBusDriver.PeerIdentityMode.DISABLED));
            }
        finally
            {
            if (sPrevious == null)
                {
                System.clearProperty(sProperty);
                }
            else
                {
                System.setProperty(sProperty, sPrevious);
                }
            }
        }

    @Test
    public void shouldRoundTripIdentifiedAliases()
        {
        SocketBusDriver driver = createDriver(-1);
        SimpleDepot depot = new SimpleDepot(new SimpleDepot.DefaultDependencies()
                .setDrivers(Map.of("socket", driver)));
        UUID            uuid   = UUID.randomUUID();
        UrlEndPoint pointOne = new UrlEndPoint("tmb://127.0.0.1:30101",
                TcpSocketProvider.INSTANCE, driver.getDependencies().getSocketAddressHasher(), uuid);
        UrlEndPoint pointTwo = new UrlEndPoint("tmb://127.0.0.1:30102?foo=bar&",
                TcpSocketProvider.INSTANCE, driver.getDependencies().getSocketAddressHasher(), uuid);
        UrlEndPoint pointOneResolved = driver.resolveSocketEndPoint(pointOne.getCanonicalName());
        UrlEndPoint pointTwoResolved = driver.resolveSocketEndPoint(pointTwo.getCanonicalName());
        UrlEndPoint pointUnidentified = driver.resolveSocketEndPoint(pointOne.getTransportName());
        EndPoint    pointDepotResolved = depot
                .resolveEndPoint(pointTwo.getCanonicalName());
        UrlEndPoint pointDefaultResolved = (UrlEndPoint) SimpleDepot.getInstance()
                .resolveEndPoint(pointTwo.getCanonicalName());

        assertEquals(pointOne, pointTwo);
        assertEquals(pointOne.hashCode(), pointTwo.hashCode());
        assertEquals(pointOne, pointOneResolved);
        assertEquals(pointTwo, pointTwoResolved);
        assertEquals(pointTwo, pointDepotResolved);
        assertEquals(uuid, pointOneResolved.getPeerIdentity());
        assertEquals("foo=bar&", pointTwoResolved.getQueryString());
        assertEquals("tmb://127.0.0.1:30102?foo=bar&", pointTwoResolved.getTransportName());
        assertNotEquals(pointOne, pointUnidentified);
        assertNotEquals(pointUnidentified, pointOne);
        assertNull(pointDefaultResolved.getPeerIdentity());
        assertEquals("foo=bar&&bus-id=" + uuid, pointDefaultResolved.getQueryString());
        assertThrows(IllegalArgumentException.class,
                () -> driver.resolveSocketEndPoint("tmb://127.0.0.1:30103?bus-id=invalid"));
        }

    @Test
    public void shouldPreserveBusIdAsAnOrdinaryQueryForGenericUrlEndPoints()
        {
        SocketBusDriver driver = createDriver(-1, SocketBusDriver.PeerIdentityMode.DISABLED);
        String          sName  = "custom://127.0.0.1:30103?bus-id=blue&";
        UrlEndPoint point = new UrlEndPoint(sName, TcpSocketProvider.INSTANCE,
                driver.getDependencies().getSocketAddressHasher());

        assertNull(point.getPeerIdentity());
        assertEquals(sName, point.getCanonicalName());
        assertEquals(sName, point.getTransportName());
        assertEquals("bus-id=blue&", point.getQueryString());
        }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    public void shouldFallBackToEndpointIdentityWithALegacyPeer(int nLegacyVersion)
            throws Exception
        {
        TestSocketMessageBus busV6     = createBus(6, -1);
        TestSocketMessageBus busLegacy = createBus(
                SocketBusDriver.PeerIdentityMode.DISABLED, nLegacyVersion, -1);
        TestCollector        collectorV6 = open(busV6);
        TestCollector        collectorLegacy = open(busLegacy);

        busLegacy.advertiseTransportOnly();

        EndPoint pointV6     = busLegacy.resolve(busV6.getBoundEndPoint().getTransportName());
        EndPoint pointLegacy = busLegacy.getLocalEndPoint();
        busLegacy.connect(pointV6);

        Event eventConnect = collectorV6.await(Event.Type.CONNECT);
        try
            {
            EndPoint pointPeer = eventConnect.getEndPoint();
            await(() -> busV6.getProtocolVersion(pointPeer) == nLegacyVersion
                    && busLegacy.getProtocolVersion(pointV6) == nLegacyVersion);

            assertEquals(pointLegacy, pointPeer);
            assertNull(((UrlEndPoint) pointPeer).getPeerIdentity());

            send(busLegacy, pointV6, nLegacyVersion);
            send(busV6, pointPeer, 6);
            assertEquals(nLegacyVersion, read(collectorV6.await(Event.Type.MESSAGE)));
            assertEquals(6, read(collectorLegacy.await(Event.Type.MESSAGE)));
            }
        finally
            {
            eventConnect.dispose();
            }
        }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    public void shouldFallBackToEndpointIdentityWhenV6PeerInitiates(int nLegacyVersion)
            throws Exception
        {
        TestSocketMessageBus busV6     = createBus(6, -1);
        TestSocketMessageBus busLegacy = createBus(
                SocketBusDriver.PeerIdentityMode.DISABLED, nLegacyVersion, -1);
        TestCollector collectorV6     = open(busV6);
        TestCollector collectorLegacy = open(busLegacy);

        EndPoint pointLegacy    = busV6.resolve(busLegacy.getLocalEndPoint().getCanonicalName());
        EndPoint pointV6Legacy  = busLegacy.resolve(busV6.getBoundEndPoint().getCanonicalName());
        busV6.connect(pointLegacy);

        Event eventConnect = collectorLegacy.await(Event.Type.CONNECT);
        try
            {
            EndPoint pointPeer = eventConnect.getEndPoint();
            await(() -> busV6.getProtocolVersion(pointLegacy) == nLegacyVersion
                    && busLegacy.getProtocolVersion(pointPeer) == nLegacyVersion);

            assertEquals(pointV6Legacy, pointPeer);
            assertNull(((UrlEndPoint) pointPeer).getPeerIdentity());

            send(busV6, pointLegacy, 6);
            send(busLegacy, pointPeer, nLegacyVersion);
            assertEquals(6, read(collectorLegacy.await(Event.Type.MESSAGE)));
            assertEquals(nLegacyVersion, read(collectorV6.await(Event.Type.MESSAGE)));
            }
        finally
            {
            eventConnect.dispose();
            }
        }

    @RepeatedTest(20)
    public void shouldPreserveLogicalHandlesDuringSameEndpointSimultaneousConnect()
            throws Exception
        {
        TestSocketMessageBus busOne = createBus(6, -1);
        TestSocketMessageBus busTwo = createBus(6, -1);
        EndPoint pointOne = busOne.getBoundEndPoint();
        EndPoint pointTwo = busTwo.getBoundEndPoint();
        String   sAdvertised = "tmb://127.0.0.1:65530";

        busOne.setAdvertisedEndPoint(sAdvertised);
        busTwo.setAdvertisedEndPoint(sAdvertised);

        TestCollector collectorOne = open(busOne);
        TestCollector collectorTwo = open(busTwo);

        connectSimultaneously(busOne, pointTwo, busTwo, pointOne);

        assertEndPoint(pointTwo, collectorOne.await(Event.Type.CONNECT));
        assertEndPoint(pointOne, collectorTwo.await(Event.Type.CONNECT));
        await(() -> busOne.getProtocolVersion(pointTwo) == 6
                && busTwo.getProtocolVersion(pointOne) == 6);
        assertEquals(Set.of(pointTwo), busOne.getPeerEndPoints());
        assertEquals(Set.of(pointOne), busTwo.getPeerEndPoints());

        send(busOne, pointTwo, 11);
        send(busTwo, pointOne, 12);
        assertEquals(11, read(collectorTwo.await(Event.Type.MESSAGE)));
        assertEquals(12, read(collectorOne.await(Event.Type.MESSAGE)));

        collectorOne.assertNoEvent(Event.Type.DISCONNECT, 50L);
        collectorTwo.assertNoEvent(Event.Type.DISCONNECT, 50L);
        collectorOne.assertNoEvent(Event.Type.RELEASE, 50L);
        collectorTwo.assertNoEvent(Event.Type.RELEASE, 50L);
        collectorOne.assertLifecycle(Event.Type.CONNECT);
        collectorTwo.assertLifecycle(Event.Type.CONNECT);
        }

    @Test
    public void shouldRetainLifecycleEvidenceWhileAwaitingMessages()
            throws Exception
        {
        for (Event.Type type : List.of(Event.Type.DISCONNECT, Event.Type.RELEASE))
            {
            TestCollector collector = new TestCollector();
            collector.add(new SimpleEvent(Event.Type.CONNECT, null));
            collector.await(Event.Type.CONNECT).dispose();
            collector.add(new SimpleEvent(type, null));
            collector.add(new SimpleEvent(Event.Type.MESSAGE, null));
            collector.await(Event.Type.MESSAGE).dispose();

            assertThrows(AssertionError.class, () -> collector.assertLifecycle(Event.Type.CONNECT));
            }
        }

    @Test
    public void shouldNotHideReleaseEvidenceWhileCheckingForDisconnect()
            throws Exception
        {
        TestCollector collector = new TestCollector();
        collector.add(new SimpleEvent(Event.Type.CONNECT, null));
        collector.await(Event.Type.CONNECT).dispose();
        collector.add(new SimpleEvent(Event.Type.RELEASE, null));
        collector.assertNoEvent(Event.Type.DISCONNECT, 1L);
        collector.assertNoEvent(Event.Type.RELEASE, 1L);

        assertThrows(AssertionError.class, () -> collector.assertLifecycle(Event.Type.CONNECT));
        }

    @Test
    public void shouldTreatAnIdentifiedContactAliasAsTheSamePeer()
            throws Exception
        {
        TestSocketMessageBus busClient = createBus(6, -1);
        TestSocketMessageBus busServer = createBus(6, -1);
        TestCollector collectorClient = open(busClient);
        TestCollector collectorServer = open(busServer);
        UrlEndPoint pointServer = busServer.getBoundEndPoint();
        UrlEndPoint pointAlias = new UrlEndPoint("tmb://127.0.0.1:65529",
                TcpSocketProvider.INSTANCE,
                busClient.getDriver().getDependencies().getSocketAddressHasher(),
                pointServer.getPeerIdentity());

        busClient.connect(pointServer);
        assertEndPoint(pointServer, collectorClient.await(Event.Type.CONNECT));
        assertEndPoint(busClient.getBoundEndPoint(), collectorServer.await(Event.Type.CONNECT));
        await(() -> busClient.getProtocolVersion(pointServer) == 6);

        busClient.connect(pointAlias);
        collectorClient.assertNoEvent(Event.Type.CONNECT, 100L);
        assertEquals(Set.of(pointServer), busClient.getPeerEndPoints());

        send(busClient, pointAlias, 21);
        assertEquals(21, read(collectorServer.await(Event.Type.MESSAGE)));
        }

    @Test
    public void shouldRejectInitialConnectionRoutedToADifferentBusIdentity()
            throws Exception
        {
        TestSocketMessageBus busClient   = createBus(6, -1);
        TestSocketMessageBus busExpected = createBus(6, -1);
        TestSocketMessageBus busActual   = createBus(6, -1);
        TestCollector        collectorClient = open(busClient);

        open(busExpected);
        open(busActual);

        try (TcpProxy proxy = new TcpProxy(busActual.getBoundEndPoint()))
            {
            UrlEndPoint pointExpected = busExpected.getBoundEndPoint();
            UrlEndPoint pointProxied  = new UrlEndPoint(proxy.getEndPoint(),
                    TcpSocketProvider.INSTANCE,
                    busClient.getDriver().getDependencies().getSocketAddressHasher(),
                    pointExpected.getPeerIdentity());

            busClient.connect(pointProxied);

            Event eventDisconnect = collectorClient.await(Event.Type.DISCONNECT);
            try
                {
                assertEquals(pointProxied, eventDisconnect.getEndPoint());
                assertFalse(busClient.isReady(pointProxied));
                }
            finally
                {
                eventDisconnect.dispose();
                }
            }
        }

    @Test
    public void shouldMigrateThroughAnAdvertisedContactAlias()
            throws Exception
        {
        TestSocketMessageBus busReceiver = createBus(6, 3);
        TestSocketMessageBus busPeer     = createBus(6, 3);

        try (TcpProxy proxy = new TcpProxy(busPeer.getBoundEndPoint()))
            {
            busPeer.setAdvertisedEndPoint(proxy.getEndPoint());

            TestCollector collectorReceiver = open(busReceiver);
            TestCollector collectorPeer     = open(busPeer);
            EndPoint      pointReceiver     = busReceiver.getBoundEndPoint();
            busPeer.connect(pointReceiver);

            Event eventConnect = collectorReceiver.await(Event.Type.CONNECT);
            try
                {
                EndPoint pointPeer = eventConnect.getEndPoint();
                await(() -> busReceiver.isReady(pointPeer));

                busReceiver.migrate(pointPeer);
                await(() -> busReceiver.getMigrationCount(pointPeer) > 0
                        && busReceiver.isReady(pointPeer));

                send(busReceiver, pointPeer, 31);
                assertEquals(31, read(collectorPeer.await(Event.Type.MESSAGE)));
                }
            finally
                {
                eventConnect.dispose();
                }
            }
        }

    @Test
    public void shouldRejectMigrationToADifferentBusIdentity()
            throws Exception
        {
        TestSocketMessageBus busReceiver = createBus(6, 1);
        TestSocketMessageBus busPeerOne  = createBus(6, -1);
        TestSocketMessageBus busPeerTwo  = createBus(6, -1);

        try (TcpProxy proxy = new TcpProxy(busPeerOne.getBoundEndPoint()))
            {
            busPeerOne.setAdvertisedEndPoint(proxy.getEndPoint());

            TestCollector collectorReceiver = open(busReceiver);
            open(busPeerOne);
            open(busPeerTwo);
            busPeerOne.connect(busReceiver.getBoundEndPoint());

            Event eventConnect = collectorReceiver.await(Event.Type.CONNECT);
            try
                {
                EndPoint pointPeer = eventConnect.getEndPoint();
                await(() -> busReceiver.isReady(pointPeer));

                proxy.routeTo(busPeerTwo.getBoundEndPoint());
                busReceiver.migrate(pointPeer);

                Event eventDisconnect = collectorReceiver.await(Event.Type.DISCONNECT);
                try
                    {
                    assertEquals(pointPeer, eventDisconnect.getEndPoint());
                    }
                finally
                    {
                    eventDisconnect.dispose();
                    }
                }
            finally
                {
                eventConnect.dispose();
                }
            }
        }

    private TestSocketMessageBus createBus(int nMaximumVersion)
            throws IOException
        {
        return createBus(nMaximumVersion, -1);
        }

    private TestSocketMessageBus createBus(int nMaximumVersion, int cReconnect)
            throws IOException
        {
        return createBus(SocketBusDriver.PeerIdentityMode.ENABLED, nMaximumVersion, cReconnect);
        }

    private TestSocketMessageBus createBus(SocketBusDriver.PeerIdentityMode mode,
            int nMaximumVersion, int cReconnect)
            throws IOException
        {
        return createBus(createDriver(cReconnect, mode), nMaximumVersion);
        }

    private TestSocketMessageBus createBus(SocketBusDriver driver, int nMaximumVersion)
            throws IOException
        {
        TestSocketMessageBus bus = new TestSocketMessageBus(driver,
                driver.resolveSocketEndPoint("tmb://127.0.0.1:0"), nMaximumVersion);
        f_listBus.add(bus);
        return bus;
        }

    private SocketBusDriver createDriver(int cReconnect)
        {
        return createDriver(cReconnect, SocketBusDriver.PeerIdentityMode.ENABLED);
        }

    private SocketBusDriver createDriver(int cReconnect, SocketBusDriver.PeerIdentityMode mode)
        {
        SocketBusDriver driver = new SocketBusDriver(new SocketBusDriver.DefaultDependencies()
                .setMessageBusProtocol("tmb")
                .setMemoryBusProtocol("trb")
                .setSocketProvider(TcpSocketProvider.INSTANCE)
                .setSocketReconnectDelayMillis(10L)
                .setSocketReconnectLimit(cReconnect), mode);
        driver.setDepot(SimpleDepot.getInstance());
        return driver;
        }

    private TestCollector open(TestSocketMessageBus bus)
        {
        TestCollector collector = new TestCollector();
        bus.setEventCollector(collector);
        bus.open();
        return collector;
        }

    private void connectSimultaneously(TestSocketMessageBus busOne, EndPoint pointTwo,
            TestSocketMessageBus busTwo, EndPoint pointOne)
            throws InterruptedException
        {
        CountDownLatch latchStart = new CountDownLatch(1);
        Thread threadOne = new Thread(() ->
            {
            await(latchStart);
            busOne.connect(pointTwo);
            }, "SocketBusPeerIdentityTestConnectOne");
        Thread threadTwo = new Thread(() ->
            {
            await(latchStart);
            busTwo.connect(pointOne);
            }, "SocketBusPeerIdentityTestConnectTwo");

        threadOne.start();
        threadTwo.start();
        latchStart.countDown();
        threadOne.join(TIMEOUT.toMillis());
        threadTwo.join(TIMEOUT.toMillis());
        assertFalse(threadOne.isAlive());
        assertFalse(threadTwo.isAlive());
        }

    private static void send(TestSocketMessageBus bus, EndPoint peer, int nValue)
        {
        BufferSequence sequence = new SingleBufferSequence(null,
                ByteBuffer.wrap(new byte[] {(byte) nValue}));
        bus.send(peer, sequence, sequence);
        bus.flush();
        }

    private static int read(Event event)
        {
        try
            {
            BufferSequence sequence = (BufferSequence) event.getContent();
            return sequence.getBuffer(0).get() & 0xFF;
            }
        finally
            {
            event.dispose();
            }
        }

    private static void assertEndPoint(EndPoint pointExpected, Event event)
        {
        try
            {
            assertEquals(pointExpected, event.getEndPoint());
            }
        finally
            {
            event.dispose();
            }
        }

    private static void await(BooleanSupplier condition)
            throws InterruptedException
        {
        long ldtStop = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean())
            {
            if (System.nanoTime() >= ldtStop)
                {
                throw new AssertionError("condition was not satisfied within " + TIMEOUT);
                }
            Thread.sleep(10L);
            }
        }

    private static void await(CountDownLatch latch)
        {
        try
            {
            latch.await();
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
            }
        }

    private static class TestCollector
            implements Collector<Event>
        {
        @Override
        public void add(Event event)
            {
            Event.Type type = event.getType();
            if (type == Event.Type.CONNECT || type == Event.Type.DISCONNECT || type == Event.Type.RELEASE)
                {
                f_listLifecycle.add(type);
                }
            if (event.getType() == Event.Type.RECEIPT)
                {
                ((BufferSequence) event.getContent()).dispose();
                event.dispose();
                }
            else
                {
                f_queue.add(event);
                }
            }

        @Override
        public void flush()
            {
            }

        Event await(Event.Type type)
                throws InterruptedException
            {
            long ldtStop = System.nanoTime() + TIMEOUT.toNanos();
            while (true)
                {
                long  cNanos = ldtStop - System.nanoTime();
                Event event  = f_queue.poll(Math.max(1L, cNanos), TimeUnit.NANOSECONDS);
                if (event == null)
                    {
                    throw new AssertionError("event " + type + " was not received within " + TIMEOUT);
                    }
                if (event.getType() == type)
                    {
                    return event;
                    }
                event.dispose();
                }
            }

        void assertNoEvent(Event.Type type, long cMillis)
                throws InterruptedException
            {
            long ldtStop = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(cMillis);
            while (true)
                {
                long cNanos = ldtStop - System.nanoTime();
                if (cNanos <= 0L)
                    {
                    return;
                    }

                Event event = f_queue.poll(cNanos, TimeUnit.NANOSECONDS);
                if (event == null)
                    {
                    return;
                    }
                try
                    {
                    assertNotEquals(type, event.getType(),
                            "unexpected " + type + " for " + event.getEndPoint());
                    }
                finally
                    {
                    event.dispose();
                    }
                }
            }

        void assertLifecycle(Event.Type... types)
            {
            assertEquals(List.of(types), f_listLifecycle, "unexpected connection lifecycle");
            }

        private final BlockingQueue<Event> f_queue = new LinkedBlockingQueue<>();

        private final List<Event.Type> f_listLifecycle = new CopyOnWriteArrayList<>();
        }

    private static class TestSocketMessageBus
            extends SocketMessageBus
        {
        TestSocketMessageBus(SocketBusDriver driver, UrlEndPoint pointLocal, int nMaximumVersion)
                throws IOException
            {
            super(driver, pointLocal);
            f_pointBound      = (UrlEndPoint) getLocalEndPoint();
            f_nMaximumVersion = nMaximumVersion;
            }

        @Override
        protected short getMaximumProtocolVersion()
            {
            short nMaximum = super.getMaximumProtocolVersion();
            return f_nMaximumVersion < 0
                    ? nMaximum
                    : (short) Math.min(f_nMaximumVersion, nMaximum);
            }

        void setAdvertisedEndPoint(String sEndPoint)
            {
            m_pointLocal = f_driver.resolveSocketEndPoint(sEndPoint);
            }

        void advertiseTransportOnly()
            {
            m_pointLocal = f_driver.resolveSocketEndPoint(f_pointBound.getTransportName());
            }

        UrlEndPoint getBoundEndPoint()
            {
            return f_pointBound;
            }

        SocketBusDriver getDriver()
            {
            return f_driver;
            }

        EndPoint resolve(String sEndPoint)
            {
            return f_driver.resolveEndPoint(sEndPoint);
            }

        int getProtocolVersion(EndPoint peer)
            {
            return ensureConnection(peer).getProtocolVersion();
            }

        int getMaximumVersion()
            {
            return getMaximumProtocolVersion();
            }

        boolean isReady(EndPoint peer)
            {
            return ensureConnection(peer).getReadyTransportGeneration() >= 0L;
            }

        int getMigrationCount(EndPoint peer)
            {
            return ensureConnection(peer).m_cMigrations;
            }

        void migrate(EndPoint peer)
            {
            Connection connection = ensureConnection(peer);
            long       lGeneration = connection.getReadyTransportGeneration();
            assertTrue(lGeneration >= 0L);
            connection.migrate(lGeneration, new IOException("test migration"));
            }

        private final UrlEndPoint f_pointBound;
        private final int f_nMaximumVersion;
        }

    private static class TcpProxy
            implements AutoCloseable
        {
        TcpProxy(UrlEndPoint pointBackend)
                throws IOException
            {
            f_addressBackend = (InetSocketAddress) pointBackend.getAddress();
            f_socketServer.bind(new InetSocketAddress("127.0.0.1", 0));
            f_executor.submit(this::accept);
            }

        String getEndPoint()
            {
            return "tmb://127.0.0.1:" + f_socketServer.getLocalPort();
            }

        void routeTo(UrlEndPoint pointBackend)
            {
            f_addressBackend = (InetSocketAddress) pointBackend.getAddress();
            }

        @Override
        public void close()
            {
            try
                {
                f_socketServer.close();
                }
            catch (IOException ignored)
                {
                }
            for (Socket socket : f_listSocket)
                {
                close(socket);
                }
            f_executor.shutdownNow();
            }

        private void accept()
            {
            while (!f_socketServer.isClosed())
                {
                try
                    {
                    Socket socketClient  = f_socketServer.accept();
                    Socket socketBackend = new Socket();
                    socketBackend.connect(f_addressBackend);
                    f_listSocket.add(socketClient);
                    f_listSocket.add(socketBackend);
                    f_executor.submit(() -> copy(socketClient, socketBackend));
                    f_executor.submit(() -> copy(socketBackend, socketClient));
                    }
                catch (IOException e)
                    {
                    if (!f_socketServer.isClosed())
                        {
                        throw new RuntimeException(e);
                        }
                    }
                }
            }

        private static void copy(Socket socketFrom, Socket socketTo)
            {
            try
                {
                InputStream  input  = socketFrom.getInputStream();
                OutputStream output = socketTo.getOutputStream();
                input.transferTo(output);
                }
            catch (IOException ignored)
                {
                }
            finally
                {
                close(socketFrom);
                close(socketTo);
                }
            }

        private static void close(Socket socket)
            {
            try
                {
                socket.close();
                }
            catch (IOException ignored)
                {
                }
            }

        private final ServerSocket f_socketServer = new ServerSocket();
        private volatile InetSocketAddress f_addressBackend;
        private final List<Socket> f_listSocket = new CopyOnWriteArrayList<>();
        private final ExecutorService f_executor = Executors.newCachedThreadPool(runnable ->
            {
            Thread thread = new Thread(runnable, "SocketBusPeerIdentityTestProxy");
            thread.setDaemon(true);
            return thread;
            });
        }

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final List<TestSocketMessageBus> f_listBus = new ArrayList<>();
    }
