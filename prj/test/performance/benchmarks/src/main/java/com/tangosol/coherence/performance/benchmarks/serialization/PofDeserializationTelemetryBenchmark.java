/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.serialization;

import com.tangosol.io.SerializationRole;

import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.net.Member;

import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.NotificationManager;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HealthCheck;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Reproduces the registered-user-type telemetry cost in POF deserialization.
 *
 * <p>Each operation deserializes a realistic order graph containing eight
 * distinct registered user types and 61 registered user-type occurrences:
 * the root order, a customer, two addresses, an audit record, sixteen line
 * items with distinct product and money objects, and eight attribute values.
 * The serialized binary and {@link SimplePofContext} are shared safely by all
 * benchmark threads, matching concurrent cache-worker deserialization of the
 * same POF shape.</p>
 *
 * <p>The setup also installs a minimal management registry and performs one
 * complete deserialization before measurement. This registers every metric
 * and MBean tuple up front, ensuring the benchmark measures the steady-state
 * {@code allowed/registered-type} path rather than one-time registration.</p>
 *
 * <p>Run the default throughput measurement with:</p>
 *
 * <pre>{@code
 * java -jar target/benchmarks.jar PofDeserializationTelemetryBenchmark
 * }</pre>
 *
 * <p>Run sampled latency, including p99, with:</p>
 *
 * <pre>{@code
 * java -jar target/benchmarks.jar PofDeserializationTelemetryBenchmark \
 *     -bm sample -tu us
 * }</pre>
 *
 * @author Aleks Seovic  2026.09.01
 * @since 26.10
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class PofDeserializationTelemetryBenchmark
    {
    // ----- benchmark methods ---------------------------------------------

    /**
     * Measure the uncontended cost of deserializing the payload.
     *
     * @param state        the shared payload state
     * @param threadState  the serialization-role state
     *
     * @return the deserialized payload
     */
    @Benchmark
    @Threads(1)
    public OrderPayload deserializeOneThread(BenchmarkState state, ThreadState threadState)
        {
        return ExternalizableHelper.fromBinary(state.m_binary, state.m_context);
        }

    /**
     * Measure contention comparable to the customer POF cache-worker count.
     *
     * @param state        the shared payload state
     * @param threadState  the serialization-role state
     *
     * @return the deserialized payload
     */
    @Benchmark
    @Threads(8)
    public OrderPayload deserializeEightThreads(BenchmarkState state, ThreadState threadState)
        {
        return ExternalizableHelper.fromBinary(state.m_binary, state.m_context);
        }

    /**
     * Measure the high-contention behavior of the telemetry counter path.
     *
     * @param state        the shared payload state
     * @param threadState  the serialization-role state
     *
     * @return the deserialized payload
     */
    @Benchmark
    @Threads(32)
    public OrderPayload deserializeThirtyTwoThreads(BenchmarkState state, ThreadState threadState)
        {
        return ExternalizableHelper.fromBinary(state.m_binary, state.m_context);
        }

    // ----- inner class: BenchmarkState ------------------------------------

    /**
     * State shared by all benchmark workers.
     */
    @State(Scope.Benchmark)
    public static class BenchmarkState
        {
        /**
         * Create the POF context, payload, serialized binary, and warm
         * telemetry counters.
         */
        @Setup(Level.Trial)
        public void setup()
            {
            m_context = new SimplePofContext();
            register(m_context, TYPE_ORDER, OrderPayload.class);
            register(m_context, TYPE_CUSTOMER, Customer.class);
            register(m_context, TYPE_ADDRESS, Address.class);
            register(m_context, TYPE_LINE_ITEM, LineItem.class);
            register(m_context, TYPE_PRODUCT, Product.class);
            register(m_context, TYPE_MONEY, Money.class);
            register(m_context, TYPE_ATTRIBUTE, AttributeValue.class);
            register(m_context, TYPE_AUDIT, AuditInfo.class);

            OrderPayload payload = createPayload();
            m_binary = ExternalizableHelper.toBinary(payload, m_context);

            SerializationTelemetry.resetForTesting();
            SerializationTelemetry.register(new BenchmarkRegistry());

            try (SerializationRole.Scope ignored = SerializationRole.setAndClose(SerializationRole.CLUSTER))
                {
                OrderPayload copy = ExternalizableHelper.fromBinary(m_binary, m_context);
                if (copy.m_lineItems.size() != LINE_ITEM_COUNT || copy.m_attributes.size() != ATTRIBUTE_COUNT)
                    {
                    throw new IllegalStateException("POF payload did not round-trip");
                    }

                long cChecks = SerializationTelemetry.snapshot()
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().contains("coh.serialization.pof_check"))
                        .filter(entry -> entry.getKey().contains("result=allowed"))
                        .filter(entry -> entry.getKey().contains("reason=registered-type"))
                        .filter(entry -> entry.getKey().contains("route=CLUSTER"))
                        .mapToLong(Map.Entry::getValue)
                        .sum();

                if (cChecks != POF_USER_TYPE_CHECKS_PER_OPERATION)
                    {
                    throw new IllegalStateException("Expected " + POF_USER_TYPE_CHECKS_PER_OPERATION
                            + " registered POF type checks but observed " + cChecks);
                    }
                }
            }

        private static void register(SimplePofContext context, int nType, Class<?> clz)
            {
            context.registerUserType(nType, clz, new PortableObjectSerializer(nType));
            }

        static OrderPayload createPayload()
            {
            Customer customer = new Customer("C-100042", "Ada", "Lovelace", "ada@example.test", 17);
            Address shipping = new Address("12 Analytical Engine Way", "London", "Greater London",
                    "SW1A 1AA", "GB");
            Address billing = new Address("7 Difference Lane", "Oxford", "Oxfordshire",
                    "OX1 1AA", "GB");

            List<LineItem> lineItems = new ArrayList<>(LINE_ITEM_COUNT);
            for (int i = 0; i < LINE_ITEM_COUNT; i++)
                {
                Product product = new Product("SKU-" + (10000 + i), "precision-component-" + i,
                        "analytical-machinery", 1_000_000L + i);
                Money price = new Money(12_500L + i * 137L, "GBP");
                lineItems.add(new LineItem("LINE-" + i, product, i % 5 + 1, price));
                }

            Map<String, AttributeValue> attributes = new HashMap<>();
            for (int i = 0; i < ATTRIBUTE_COUNT; i++)
                {
                attributes.put("attribute-" + i,
                        new AttributeValue("value-with-enough-content-" + i, 10_000L + i));
                }

            AuditInfo audit = new AuditInfo(1_788_278_400_000L, "checkout-service",
                    List.of("created", "priced", "validated", "submitted"));

            return new OrderPayload("ORDER-20260901-0042", customer, shipping, billing,
                    lineItems, attributes, audit);
            }

        private SimplePofContext m_context;

        private Binary m_binary;
        }

    // ----- inner class: ThreadState ---------------------------------------

    /**
     * Establish the same cluster serialization role used by cache workers.
     */
    @State(Scope.Thread)
    public static class ThreadState
        {
        /**
         * Set the serialization route once for each benchmark worker.
         */
        @Setup(Level.Trial)
        public void setup()
            {
            m_scope = SerializationRole.setAndClose(SerializationRole.CLUSTER);
            }

        /**
         * Restore the worker thread's previous serialization route.
         */
        @TearDown(Level.Trial)
        public void tearDown()
            {
            m_scope.close();
            }

        private SerializationRole.Scope m_scope;
        }

    // ----- inner class: OrderPayload --------------------------------------

    /**
     * Root benchmark payload.
     */
    public static class OrderPayload
            implements PortableObject
        {
        /**
         * Construct an empty payload for POF.
         */
        public OrderPayload()
            {
            }

        private OrderPayload(String sOrderId, Customer customer, Address shipping, Address billing,
                             List<LineItem> lineItems, Map<String, AttributeValue> attributes, AuditInfo audit)
            {
            m_sOrderId   = sOrderId;
            m_customer  = customer;
            m_shipping  = shipping;
            m_billing   = billing;
            m_lineItems = lineItems;
            m_attributes = attributes;
            m_audit     = audit;
            }

        @Override
        @SuppressWarnings("unchecked")
        public void readExternal(PofReader in) throws IOException
            {
            m_sOrderId   = in.readString(0);
            m_customer  = (Customer) in.readObject(1);
            m_shipping  = (Address) in.readObject(2);
            m_billing   = (Address) in.readObject(3);
            m_lineItems = in.readCollection(4, new ArrayList<>());
            m_attributes = in.readMap(5, new HashMap<>());
            m_audit     = (AuditInfo) in.readObject(6);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sOrderId);
            out.writeObject(1, m_customer);
            out.writeObject(2, m_shipping);
            out.writeObject(3, m_billing);
            out.writeCollection(4, m_lineItems);
            out.writeMap(5, m_attributes);
            out.writeObject(6, m_audit);
            }

        private String m_sOrderId;
        private Customer m_customer;
        private Address m_shipping;
        private Address m_billing;
        private List<LineItem> m_lineItems;
        private Map<String, AttributeValue> m_attributes;
        private AuditInfo m_audit;
        }

    // ----- inner class: Customer ------------------------------------------

    /**
     * Customer nested user type.
     */
    public static class Customer
            implements PortableObject
        {
        public Customer()
            {
            }

        private Customer(String sId, String sFirstName, String sLastName, String sEmail, int nLoyaltyLevel)
            {
            m_sId           = sId;
            m_sFirstName    = sFirstName;
            m_sLastName     = sLastName;
            m_sEmail        = sEmail;
            m_nLoyaltyLevel = nLoyaltyLevel;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sId           = in.readString(0);
            m_sFirstName    = in.readString(1);
            m_sLastName     = in.readString(2);
            m_sEmail        = in.readString(3);
            m_nLoyaltyLevel = in.readInt(4);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sId);
            out.writeString(1, m_sFirstName);
            out.writeString(2, m_sLastName);
            out.writeString(3, m_sEmail);
            out.writeInt(4, m_nLoyaltyLevel);
            }

        private String m_sId;
        private String m_sFirstName;
        private String m_sLastName;
        private String m_sEmail;
        private int m_nLoyaltyLevel;
        }

    // ----- inner class: Address -------------------------------------------

    /**
     * Address nested user type.
     */
    public static class Address
            implements PortableObject
        {
        public Address()
            {
            }

        private Address(String sLine1, String sCity, String sRegion, String sPostalCode, String sCountry)
            {
            m_sLine1      = sLine1;
            m_sCity       = sCity;
            m_sRegion     = sRegion;
            m_sPostalCode = sPostalCode;
            m_sCountry    = sCountry;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sLine1      = in.readString(0);
            m_sCity       = in.readString(1);
            m_sRegion     = in.readString(2);
            m_sPostalCode = in.readString(3);
            m_sCountry    = in.readString(4);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sLine1);
            out.writeString(1, m_sCity);
            out.writeString(2, m_sRegion);
            out.writeString(3, m_sPostalCode);
            out.writeString(4, m_sCountry);
            }

        private String m_sLine1;
        private String m_sCity;
        private String m_sRegion;
        private String m_sPostalCode;
        private String m_sCountry;
        }

    // ----- inner class: LineItem ------------------------------------------

    /**
     * Line-item nested user type.
     */
    public static class LineItem
            implements PortableObject
        {
        public LineItem()
            {
            }

        private LineItem(String sLineId, Product product, int cQuantity, Money price)
            {
            m_sLineId  = sLineId;
            m_product  = product;
            m_cQuantity = cQuantity;
            m_price    = price;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sLineId  = in.readString(0);
            m_product  = (Product) in.readObject(1);
            m_cQuantity = in.readInt(2);
            m_price    = (Money) in.readObject(3);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sLineId);
            out.writeObject(1, m_product);
            out.writeInt(2, m_cQuantity);
            out.writeObject(3, m_price);
            }

        private String m_sLineId;
        private Product m_product;
        private int m_cQuantity;
        private Money m_price;
        }

    // ----- inner class: Product -------------------------------------------

    /**
     * Product nested user type.
     */
    public static class Product
            implements PortableObject
        {
        public Product()
            {
            }

        private Product(String sSku, String sName, String sCategory, long nCatalogVersion)
            {
            m_sSku           = sSku;
            m_sName          = sName;
            m_sCategory      = sCategory;
            m_nCatalogVersion = nCatalogVersion;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sSku           = in.readString(0);
            m_sName          = in.readString(1);
            m_sCategory      = in.readString(2);
            m_nCatalogVersion = in.readLong(3);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sSku);
            out.writeString(1, m_sName);
            out.writeString(2, m_sCategory);
            out.writeLong(3, m_nCatalogVersion);
            }

        private String m_sSku;
        private String m_sName;
        private String m_sCategory;
        private long m_nCatalogVersion;
        }

    // ----- inner class: Money ---------------------------------------------

    /**
     * Money nested user type.
     */
    public static class Money
            implements PortableObject
        {
        public Money()
            {
            }

        private Money(long cMinorUnits, String sCurrency)
            {
            m_cMinorUnits = cMinorUnits;
            m_sCurrency   = sCurrency;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_cMinorUnits = in.readLong(0);
            m_sCurrency   = in.readString(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeLong(0, m_cMinorUnits);
            out.writeString(1, m_sCurrency);
            }

        private long m_cMinorUnits;
        private String m_sCurrency;
        }

    // ----- inner class: AttributeValue ------------------------------------

    /**
     * Attribute-value nested user type.
     */
    public static class AttributeValue
            implements PortableObject
        {
        public AttributeValue()
            {
            }

        private AttributeValue(String sValue, long nVersion)
            {
            m_sValue  = sValue;
            m_nVersion = nVersion;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sValue  = in.readString(0);
            m_nVersion = in.readLong(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sValue);
            out.writeLong(1, m_nVersion);
            }

        private String m_sValue;
        private long m_nVersion;
        }

    // ----- inner class: AuditInfo -----------------------------------------

    /**
     * Audit nested user type.
     */
    public static class AuditInfo
            implements PortableObject
        {
        public AuditInfo()
            {
            }

        private AuditInfo(long lTimestamp, String sActor, List<String> events)
            {
            m_lTimestamp = lTimestamp;
            m_sActor     = sActor;
            m_events     = events;
            }

        @Override
        @SuppressWarnings("unchecked")
        public void readExternal(PofReader in) throws IOException
            {
            m_lTimestamp = in.readLong(0);
            m_sActor     = in.readString(1);
            m_events     = in.readCollection(2, new ArrayList<>());
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeLong(0, m_lTimestamp);
            out.writeString(1, m_sActor);
            out.writeCollection(2, m_events);
            }

        private long m_lTimestamp;
        private String m_sActor;
        private List<String> m_events;
        }

    // ----- inner class: BenchmarkRegistry ---------------------------------

    /**
     * Minimal in-memory management registry used to exercise the exact MBean
     * counter path without starting a Coherence cluster.
     */
    private static class BenchmarkRegistry
            implements Registry
        {
        @Override
        public String getDomainName()
            {
            return "Coherence";
            }

        @Override
        public String ensureGlobalName(String sName)
            {
            return sName + ",nodeId=1";
            }

        @Override
        public String ensureGlobalName(String sName, Member member)
            {
            return ensureGlobalName(sName);
            }

        @Override
        public boolean isRegistered(String sName)
            {
            return m_mapBeans.containsKey(sName);
            }

        @Override
        public void register(String sName, Object bean)
            {
            m_mapBeans.put(sName, bean);
            }

        @Override
        public void unregister(String sName)
            {
            m_mapBeans.remove(sName);
            }

        @Override
        public NotificationManager getNotificationManager()
            {
            return null;
            }

        @Override
        public MBeanServerProxy getMBeanServerProxy()
            {
            return null;
            }

        @Override
        public boolean isExtendedMBeanName()
            {
            return false;
            }

        @Override
        public void register(HealthCheck healthCheck)
            {
            }

        @Override
        public void unregister(HealthCheck healthCheck)
            {
            }

        @Override
        public Collection<HealthCheck> getHealthChecks()
            {
            return Collections.emptyList();
            }

        @Override
        public boolean allHealthChecksReady()
            {
            return true;
            }

        @Override
        public boolean allHealthChecksLive()
            {
            return true;
            }

        @Override
        public boolean allHealthChecksStarted()
            {
            return true;
            }

        @Override
        public boolean allHealthChecksSafe()
            {
            return true;
            }

        private final Map<String, Object> m_mapBeans = new ConcurrentHashMap<>();
        }

    // ----- constants ------------------------------------------------------

    private static final int TYPE_ORDER = 1000;
    private static final int TYPE_CUSTOMER = 1001;
    private static final int TYPE_ADDRESS = 1002;
    private static final int TYPE_LINE_ITEM = 1003;
    private static final int TYPE_PRODUCT = 1004;
    private static final int TYPE_MONEY = 1005;
    private static final int TYPE_ATTRIBUTE = 1006;
    private static final int TYPE_AUDIT = 1007;

    private static final int LINE_ITEM_COUNT = 16;
    private static final int ATTRIBUTE_COUNT = 8;

    private static final int POF_USER_TYPE_CHECKS_PER_OPERATION =
            5 + LINE_ITEM_COUNT * 3 + ATTRIBUTE_COUNT;
    }
