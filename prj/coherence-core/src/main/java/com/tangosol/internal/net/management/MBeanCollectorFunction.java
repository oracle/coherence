/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;

import com.tangosol.net.management.MBeanAccessor;
import com.tangosol.net.management.MBeanHelper.QueryExpFilter;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;
import com.tangosol.util.SetMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.RegexFilter;

import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemoteCollectors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularDataSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import java.util.regex.PatternSyntaxException;

import java.util.stream.Collector;

/**
 * MBeanCollectorFunction is {@link Function} that provided with an {@link
 * MBeanServer} will return a Map of MBean attributes with each attribute being
 * aggregated or listed. The type of aggregation to be performed is based on
 * the collector associated to each attribute. The collector is derived based
 * on precedence order as specified in the {@link CacheMBeanAttribute} and {@link
 * ServiceMBeanAttribute} docs.
 *
 * @author hr  2016.09.28
 * @since 12.2.1.4.0
 */
public class MBeanCollectorFunction
    implements Remote.Function<MBeanServer, Map<String, Object>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an MBeanCollectorFunction function.
     *
     * @param sLocator    either a regex to be applied against nodeids or a role name
     * @param sAttribute  the attribute to return
     * @param sCollector  the collector to use instead of the default
     * @param query       the MBean Object query to use
     */
    public MBeanCollectorFunction(String sLocator,
                                  String sAttribute,
                                  String sCollector,
                                  MBeanAccessor.QueryBuilder.ParsedQuery query)
        {
        f_sLocator   = sLocator;
        f_sAttribute = sAttribute;
        f_sCollector = sCollector;
        f_query      = query;
        }

    @Override
    public Map<String, Object> apply(MBeanServer mbs)
        {
        boolean  fAllAttributes = isOmitted(f_sAttribute);
        QueryExp query          = createQuery();
        String   sObjectQuery   = f_query.getQuery();

        // populate a map of attribute names to a collection of ObjectNames
        IdentityHashMap<Set<String>, Collection<ObjectName>> mapObjectNames = new IdentityHashMap<>(2);

        ObjectName objName = null;
        try
            {
            objName = new ObjectName(sObjectQuery);
            }
        catch(MalformedObjectNameException e)
            {
            throw new IllegalArgumentException("Malformed ObjectName: '" + objName + "', Query: '" + query + '\'', e);
            }

        Collection<ObjectName> colNames;
        try
            {
            colNames = mbs.queryNames(objName, query);
            }
        catch (RuntimeException e)
            {
            throw new IllegalArgumentException("Illegal query; ObjectName: '" + objName + "', Query: '" + query + '\'', e);
            }

        Iterator<ObjectName> iterNames = colNames.iterator();
        if (iterNames.hasNext())
            {
            try
                {
                MBeanInfo            info       = mbs.getMBeanInfo(iterNames.next());
                MBeanAttributeInfo[] aAttribute = info.getAttributes();

                Set<String> setAttributeName = new HashSet<>(aAttribute.length);
                for (MBeanAttributeInfo attribute : aAttribute)
                    {
                    setAttributeName.add(attribute.getName());
                    }
                mapObjectNames.put(setAttributeName, colNames);
                }
            catch (InstanceNotFoundException ex)
                {
                // ignore when MBean unregistered between query and request for its info/attributes
                Logger.log("MBeanCollector#apply(objName=" + objName +
                           "): ignoring InstanceNotFoundException: " + ex.getMessage(), Base.LOG_QUIET);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        // validate the requested attribute exists on the MBean being queried
        if (!fAllAttributes)
            {
            for (Iterator<Set<String>> iter = mapObjectNames.keySet().iterator(); iter.hasNext(); )
                {
                Set<String> setAttributeName = iter.next();

                // mutating the key of a hash container is generally erroneous
                // but identity based maps are an exception
                setAttributeName.retainAll(Collections.singletonList(f_sAttribute));
                if (setAttributeName.isEmpty())
                    {
                    iter.remove();
                    }
                }

            if (mapObjectNames.isEmpty())
                {
                throw Base.ensureRuntimeException(new AttributeNotFoundException(
                    "Attribute \"" + f_sAttribute + "\" cannot be found"));
                }
            }

        // create a collector capable of aggregating each MBean attribute
        Collector<MBeanSnapshot, Map<String, Object>, Map<String, Object>> collector =
            createCollector(objName, f_sCollector, mbs);

        BiConsumer<Map<String, Object>, MBeanSnapshot> accumulator = collector.accumulator();

        Map<String, Object> mapResult = collector.supplier().get();
        MBeanSnapshot       snapshot  = new MBeanSnapshot();

        for (Map.Entry<Set<String>, Collection<ObjectName>> entry : mapObjectNames.entrySet())
            {
            Set<String> setAttributeNames = entry.getKey();

            for (ObjectName objectName : entry.getValue())
                {
                Map<String, Object> mapAttributes = new SetMap<>(setAttributeNames,
                    sAttributeName ->
                    {
                    try
                        {
                        return mbs.getAttribute(objectName, sAttributeName);
                        }
                    catch (InstanceNotFoundException ex)
                        {
                        // ignore when MBean unregistered between query and request for its attributes
                        Logger.log("MBeanCollectorFunction#apply(objName=" + objectName + ",attribute=" + sAttributeName + "): ignoring InstanceNotFoundException: " + ex.getMessage(), Base.LOG_QUIET);
                        return null;
                        }
                    catch (Exception e)
                        {
                        throw Base.ensureRuntimeException(e);
                        }
                    });

                accumulator.accept(mapResult, snapshot.reset(objectName, mapAttributes));
                }
            }

        mapResult = collector.finisher().apply(mapResult);

        return mapResult;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a {@link QueryExp} based on the 'locator' this Function was
     * constructed with.
     * <p>
     * Note: the locator can be a regular expression based on member ids or
     *       role names.
     *
     * @return a QueryExp based on the 'locator'
     */
    protected QueryExp createQuery()
        {
        String sNodeIdRegex = f_sLocator;

        if (isOmitted(sNodeIdRegex))
            {
            return new QueryExpFilter(AlwaysFilter.INSTANCE());
            }

        try
            {
            if (sNodeIdRegex.matches(".*[A-Za-z]+.*")) // role-name based locator
                {
                // role based locator
                StringBuilder sb         = new StringBuilder();
                String        sDelim     = "";
                Set<Member>   setMembers = CacheFactory.ensureCluster().getMemberSet();
                for (Member member : setMembers)
                    {
                    if (member.getRoleName().matches(sNodeIdRegex))
                        {
                        sb.append(sDelim).append(member.getId());
                        sDelim = "|";
                        }
                    }
                sNodeIdRegex = sb.toString();
                }
            }
        catch (PatternSyntaxException e)
            {
            throw new IllegalArgumentException("Invalid Regular Expression: '" + sNodeIdRegex + "'", e);
            }

        return new QueryExpFilter(sNodeIdRegex.isEmpty()
            ? NeverFilter.INSTANCE()
            : new RegexFilter<>(new ReflectionExtractor<ObjectName, String>("getKeyProperty",
            new Object[]{"nodeId"}), sNodeIdRegex));
        }


    // ----- static helpers -------------------------------------------------

    /**
     * Return a {@link Collector} to be used to process various MBeans.
     *
     * @param objName     the ObjectName of the MBean(s)
     * @param sCollector  the collector to use
     *
     * @param <T> the Collectors supplied, accumulated and finished type
     *
     * @return a {@link Collector} to be used to process various MBeans
     */
    protected static <T extends Map<String, Object>> Collector<MBeanSnapshot, T, T>
    createCollector(ObjectName  objName,
                    String      sCollector,
                    MBeanServer mbs)
        {
        if (sCollector == null || sCollector.isEmpty())
            {
            Supplier<Collector>   supplier = RemoteCollectors::toList;
            Class<? extends Enum> clzEnum;

            String sType = objName.getKeyProperty("type");
            if (sType == null)
                {
                return new MultiAttributeCollector(supplier, null, mbs);
                }

            switch (sType)
                {
                case "Cache":
                    clzEnum = CacheMBeanAttribute.class;
                    break;
                case "Service":
                    clzEnum = ServiceMBeanAttribute.class;
                    break;
                default:
                    supplier = null;
                    clzEnum = null;
                }
            return new MultiAttributeCollector(supplier, clzEnum, mbs);
            }
        return new MultiAttributeCollector(() -> createAttributeCollector(sCollector));
        }

    /**
     * Return a {@link Collector} based on the provided collector name.
     *
     * @param sCollector  the name of the Collector
     *
     * @return a Collector based on the provided collector name
     */
    public static Collector createAttributeCollector(String sCollector)
        {
        switch (sCollector.toLowerCase())
            {
            case "all":
                return RemoteCollectors.summarizingLong(TO_LONG_FUNCTION);
            case "avg":
                return RemoteCollectors.averagingLong(TO_LONG_FUNCTION);
            case "count":
                return RemoteCollectors.counting();
            case "list":
                return RemoteCollectors.toList();
            case "max":
                return RemoteCollectors.maxBy(COMPARABLE_COMPARATOR);
            case "min":
                return RemoteCollectors.minBy(COMPARABLE_COMPARATOR);
            case "set":
                return RemoteCollectors.toSet();
            case "sum":
                return RemoteCollectors.summingLong(TO_LONG_FUNCTION);
            case "intsummary":
                return RemoteCollectors.summarizingInt(TO_INT_FUNCTION);
            case "longsummary":
                return RemoteCollectors.summarizingLong(TO_LONG_FUNCTION);
            case "doublesummary":
                return RemoteCollectors.summarizingDouble(TO_DOUBLE_FUNCTION);
            default:
                throw new IllegalArgumentException(
                    "The specified collector is not recognized: " + sCollector);
            }
        }

    /**
     * Return true if the provided string represents a wildcard / 'all' request.
     *
     * @param s  the string to be interrogated
     *
     * @return true if the provided string represents a wildcard / 'all' request
     */
    protected static boolean isOmitted(String s)
        {
        return s == null || "*".equals(s) || "all".equals(s);
        }

    // ----- inner class: MBeanSnapshot -------------------------------------

    /**
     * A holder for an MBean's ObjectName and its data.
     */
    protected static class MBeanSnapshot
        {
        // ----- helpers ----------------------------------------------------

        /**
         * Reset the state of this MBeanSnapshot based on the provided ObjectName
         * and Map of attribute data.
         *
         * @param objName        the object name
         * @param mapAttributes  the attribute values
         *
         * @return this MBeanSnapshot
         */
        protected MBeanSnapshot reset(ObjectName objName, Map<String, Object> mapAttributes)
            {
            m_objName = objName;
            m_mapAttributes = mapAttributes;

            return this;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the attribute values for this MBean.
         *
         * @return the attribute values for this MBean
         */
        protected Map<String, Object> getAttributes()
            {
            return m_mapAttributes;
            }

        /**
         * Return the ObjectName this MBean represents.
         *
         * @return the ObjectName this MBean represents
         */
        protected ObjectName getObjectName()
            {
            return m_objName;
            }

        // ----- data members -----------------------------------------------

        /**
         * The ObjectName for this MBean.
         */
        protected ObjectName m_objName;

        /**
         * The attribute values for this MBean.
         */
        protected Map<String, Object> m_mapAttributes;
        }

    // ----- inner class: CachedCollector -----------------------------------

    /**
     * A CachedCollector avoids multiple calls to the collector methods;
     * {@link Collector#supplier() supplier}, {@link Collector#accumulator()
     * accumulator}, {@link Collector#combiner() combiner} or {@link Collector#
     * finisher finisher}.
     *
     * @param <T> the type of input elements to the reduction operation
     * @param <A> the mutable accumulation type of the reduction operation (often
     *            hidden as an implementation detail)
     * @param <R> the result type of the reduction operation
     */
    public static class CachedCollector<T, A, R>
        implements Collector<T, A, R>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a CachedCollector based on the provided Collector.
         *
         * @param collector  a Collector to delegate to
         */
        public CachedCollector(Collector<T, A, R> collector)
            {
            Objects.requireNonNull(collector);

            f_collector = collector;
            }

        @Override
        public Supplier<A> supplier()
            {
            if (m_supplier == null)
                {
                m_supplier = f_collector.supplier();
                }
            return m_supplier;
            }

        @Override
        public BiConsumer<A, T> accumulator()
            {
            if (m_accumulator == null)
                {
                m_accumulator = f_collector.accumulator();
                }
            return m_accumulator;
            }

        @Override
        public BinaryOperator<A> combiner()
            {
            if (m_combiner == null)
                {
                m_combiner = f_collector.combiner();
                }
            return m_combiner;
            }

        @Override
        public Function<A, R> finisher()
            {
            if (m_finisher == null)
                {
                m_finisher = f_collector.finisher();
                }
            return m_finisher;
            }

        @Override
        public Set<Characteristics> characteristics()
            {
            return f_collector.characteristics();
            }

        // ----- data members -----------------------------------------------

        /**
         * The Collector to delegate to.
         */
        protected final Collector<T, A, R> f_collector;

        /**
         * The cached Supplier.
         */
        protected Supplier<A> m_supplier;

        /**
         * The cached accumulator.
         */
        protected BiConsumer<A, T> m_accumulator;

        /**
         * The cached combiner.
         */
        protected BinaryOperator<A> m_combiner;

        /**
         * The cache finisher.
         */
        protected Function<A, R> m_finisher;
        }

    // ----- inner class: MultiAttributeCollector ---------------------------

    /**
     * A {@link Collector} implementation that operates against a
     * Map&lt;String,Object&gt;. Each entry in the Map represents an attribute
     * and its value. Each attribute has its own collector to use and can be
     * specified at three levels:
     * <ol>
     *     <li>Request - 'collector' matrix parameter is specified and takes
     *                   precedence</li>
     *     <li>Attribute - each attribute may specify the appropriate collector
     *                     to use within the respective Enum definition</li>
     *     <li>Default - if none of the above a list collector is used</li>
     * </ol>
     * This Collector will ensure the appropriate Collector is used to aggregate
     * each attribute in the MBean.
     *
     * @param <T> the type being operated against
     */
    protected static class MultiAttributeCollector<T extends Map<String, Object>>
        extends AbstractMultiCollector
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a MultiAttributeCollector.
         *
         * @param supplierCollector  a {@link Supplier} that provides a Collector
         *                           to use for all attributes being aggregated
         */
        protected MultiAttributeCollector(Supplier<Collector<Object, Object, Object>> supplierCollector)
            {
            this(supplierCollector, null, null);
            }

        /**
         * Construct a MultiAttributeCollector.
         *
         * @param clzEnum            an Enum Class that represents the MBean
         * @param supplierCollector  a {@link Supplier} that provides a Collector
         *                           to use if undefined or {@code fUseDefault == true}
         */
        protected MultiAttributeCollector(Supplier<Collector<Object, Object, Object>> supplierCollector,
                                          Class<? extends Enum> clzEnum, MBeanServer server)
            {
            f_clzEnum           = clzEnum;
            f_supplierCollector = supplierCollector;
            f_server            = server;
            }

        // ----- Collector interface ----------------------------------------

        @Override
        public BiConsumer<T, MBeanSnapshot> accumulator()
            {
            return (mapResult, snapshot) ->
            {
            for (Map.Entry<String, Object> entry : snapshot.getAttributes().entrySet())
                {
                String sAttribute = entry.getKey();
                Object oValue     = ATTRIBUTE_NULLIFIER.apply(entry.getValue());

                if (oValue == null)
                    {
                    continue;
                    }

                Collector<Object, Object, Object> collector =
                    (Collector<Object, Object, Object>) f_mapCollector.get(sAttribute);
                if (collector == null)
                    {
                    collector = findCollector(snapshot.getObjectName(), sAttribute, oValue);
                    if (collector == null)
                        {
                        continue;
                        }

                    f_mapCollector.put(sAttribute, new CachedCollector<>(collector));

                    mapResult.put(sAttribute, collector.supplier().get());
                    }

                collector.accumulator().accept(mapResult.get(sAttribute), oValue);
                }
            };
            }

        @Override
        public Set<Characteristics> characteristics()
            {
            return null;
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Return the derived Collector to use for the given attribute name.
         *
         * @param objName          the ObjectName this attribute is bound to
         * @param sAttribute       the attribute name
         * @param oAttributeValue  the attribute value
         *
         * @return the derived Collector to use for the given attribute name
         */
        protected Collector findCollector(ObjectName objName, String sAttribute, Object oAttributeValue)
            {
            MBeanAttribute enumAttr = getMBeanAttribute(sAttribute);
            if (enumAttr == null)
                {
                try
                    {
                    MBeanInfo            info       = f_server == null ? null : f_server.getMBeanInfo(objName);
                    MBeanAttributeInfo[] aAttribute = info == null ? new MBeanAttributeInfo[0] : info.getAttributes();

                    for (MBeanAttributeInfo attrInfo : aAttribute)
                        {
                        if (sAttribute.equals(attrInfo.getName()))
                            {
                            String sCollector = (String) attrInfo.getDescriptor().getFieldValue(COLLECTOR_DESCRIPTOR);

                            Collector collector = sCollector == null || sCollector.isEmpty()
                                ? createTypeBasedCollector(oAttributeValue)
                                : createAttributeCollector(sCollector);

                            if (collector != null)
                                {
                                return collector;
                                }
                            }
                        }
                    return f_supplierCollector == null ? null : f_supplierCollector.get();
                    }
                catch (InstanceNotFoundException ex)
                    {
                    // ignore when MBean unregistered between query and request for MBean info/attributes
                    Logger.log("MBeanCollectorFunction#findCollector(objName=" + objName + "): ignoring InstanceNotFoundException: " + ex.getMessage(), Base.LOG_QUIET);
                    return null;
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                }

            return enumAttr.isVisible() ? enumAttr.collector() : null;
            }

        /**
         * Return a looked up MBeanAttribute(enum) or null.
         *
         * @param sName  the name of the MBeanAttribute
         *
         * @return a looked up MBeanAttribute or null
         */
        protected MBeanAttribute getMBeanAttribute(String sName)
            {
            try
                {
                return f_clzEnum == null ? null : (MBeanAttribute) Enum.valueOf(f_clzEnum, sName);
                }
            catch (Throwable ignore) {}

            return null;
            }

        // ----- data members -----------------------------------------------

        /**
         * The Enum class that represents the MBean attributes.
         */
        protected final Class<? extends Enum> f_clzEnum;

        /**
         * A Supplier of the default Collector to use.
         */
        protected final Supplier<Collector<Object, Object, Object>> f_supplierCollector;

        /**
         * The MBeanServer.
         */
        protected final MBeanServer f_server;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Return a Collector that can operate against known types (integers, longs,
     * doubles, floats, CompositeData and TabularDataSupport).
     *
     * @param oValue  the value to create a Collector based on
     *
     * @return a Collector that can operate against the given type
     */
    public static Collector createTypeBasedCollector(Object oValue)
        {
        if (oValue instanceof Integer)
            {
            return createAttributeCollector("intsummary");
            }
        if (oValue instanceof Long)
            {
            return createAttributeCollector("longsummary");
            }
        if (oValue instanceof Double)
            {
            return createAttributeCollector("doublesummary");
            }
        if (oValue instanceof Float)
            {
            return createAttributeCollector("doublesummary");
            }
        if (oValue instanceof CompositeData)
            {
            return new CompositeDataCollector();
            }
        if(oValue instanceof TabularDataSupport)
            {
            return new TabularDataCollector();
            }
        if (oValue instanceof String)
            {
            return createAttributeCollector("list");
            }
        if (oValue instanceof Boolean)
            {
            return createAttributeCollector("list");
            }
        return null;
        }

    /**
     * The separator between service name and cache name.
     */
    protected static final char SERVICE_CACHE_SEPARATOR = '!';

    /**
     * A {@link Comparator} that delegates to Comparable implementations.
     */
    protected static final Remote.Comparator<Object> COMPARABLE_COMPARATOR = (o1, o2) ->
    {
    if (!(o1 instanceof Comparable))
        {
        throw new IllegalArgumentException("Comparable type required for collector");
        }
    return ((Comparable) o1).compareTo(o2);
    };

    /**
     * A {@link ToLongFunction} that expects with a Number or a String that can
     * be parsed to a Long.
     */
    protected static final ValueExtractor<Object, Long> TO_LONG_FUNCTION = o ->
        o instanceof Number
            ? ((Number) o).longValue()
            : Long.parseLong(String.valueOf(o));

    /**
     * A {@link ToIntFunction} that expects with a Number or a String that can
     * be parsed to an Integer.
     */
    protected static final ValueExtractor<Object, Integer> TO_INT_FUNCTION = o ->
        o instanceof Number
            ? ((Number) o).intValue()
            : Integer.parseInt(String.valueOf(o));

    /**
     * A {@link ToDoubleFunction} that expects with a Number or a String that can
     * be parsed to an Integer.
     */
    protected static final ValueExtractor<Object, Double> TO_DOUBLE_FUNCTION = o ->
        o instanceof Number
            ? ((Number) o).doubleValue()
            : Double.parseDouble(String.valueOf(o));

    /**
     * The supported MBean types.
     */
    protected static final Map<String, String> MBEAN_TYPES = Collections.unmodifiableMap(new HashMap<String, String>()
        {{
        put("cache", Registry.CACHE_TYPE);
        put("storage", Registry.STORAGE_MANAGER_TYPE);
        put("service", Registry.SERVICE_TYPE);
        put("view", Registry.VIEW_TYPE);
        //put("cluster", Registry.CLUSTER_TYPE);
        //put("management", Registry.MANAGEMENT_TYPE);
        //put("node", Registry.NODE_TYPE);
        //put("p2p", Registry.POINT_TO_POINT_TYPE);
        //put("connectionmanager", Registry.CONNECTION_MANAGER_TYPE);
        }});

    /**
     * The key used in the MBean attribute descriptor to speciy the Collector
     * for an attribute.
     */
    protected static final String COLLECTOR_DESCRIPTOR = "rest.collector";

    // ----- data members ---------------------------------------------------

    /**
     * A regular expression to be applied against nodeids or a role names
     */
    protected final String f_sLocator;

    /**
     * The attribute to return.
     */
    protected final String f_sAttribute;

    /**
     * The collector to use.
     */
    protected final String f_sCollector;

    /**
     * The object query to use.
     */
    protected final MBeanAccessor.QueryBuilder.ParsedQuery f_query;
    }
