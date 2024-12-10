/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.tangosol.internal.net.management.CacheMBeanAttribute;
import com.tangosol.internal.net.management.MBeanCollectorFunction;
import com.tangosol.internal.net.management.ServiceMBeanAttribute;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder.ParsedQuery;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import com.tangosol.util.WrapperException;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.function.Remote;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

/**
 * MBeanAccessor provides a means to access JMX MBeans via the Coherence
 * Management Server (a single MBeanServer with an aggregated view of MBeans
 * for the entire cluster).
 * <p>
 * This API provides a means to query MBeans, get and update MBean attributes,
 * and invoke MBean operations. Ultimately this allows a client to use Coherence's
 * transport to communicate with the MBeanServer to interrogate MBeans.
 *
 * @author sr/hr
 * @since 12.2.1.4.0
 *
 * @see <a href="http://www.oracle.com/pls/topic/lookup?ctx=fmwlatest&id=COHMG5498">
 * Introduction to Oracle Coherence Management</a>
 */
public class MBeanAccessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default Constructor.
     */
    public MBeanAccessor()
        {
        this(CacheFactory.getCluster().getManagement().getMBeanServerProxy());
        }

    /**
     * Create an instance of MBeanAccessor.
     *
     * @param mBeanServerProxy  the {@link MBeanServerProxy} to be used
     */
    public MBeanAccessor(MBeanServerProxy mBeanServerProxy)
        {
        f_mbeanServerProxy = mBeanServerProxy;
        }

    // ----- MBeanAccessor methods ------------------------------------------

    /**
     * Return all the attributes that match the query expressed by the provided
     * {@link QueryBuilder}.
     * <p>
     * Note: the return type is keyed by the full ObjectName registered with
     *       the MBeanServer and a Map of attribute name to value
     *
     * @param query  the {@link ParsedQuery} to be used to determine applicable
     *               MBeans
     *
     * @return a Map keyed by the full ObjectName registered with the MBeanServer
     *         and a Map of attribute name to value
     */
    public Map<String, Map<String, Object>> getAttributes(ParsedQuery query)
        {
        return getAttributes(query, null, true);
        }

    /**
     * Return all the attributes that match the query expressed by the provided
     * {@link QueryBuilder}.
     * <p>
     * Note: the return type is keyed by the full ObjectName registered with
     *       the MBeanServer and a Map of attribute name to value
     *
     * @param query  the {@link ParsedQuery} to be used to determine applicable
     *               MBeans
     *
     * @param filter server side filter for what attributes to return
     *
     * @param fAddStorageMBeanAttributes add storage MBean attributes to CacheMBean tier="back" attributes.
     *
     * @return a Map keyed by the full ObjectName registered with the MBeanServer
     *         and a Map of attribute name to value
     */
    public Map<String, Map<String, Object>> getAttributes(ParsedQuery query, Filter<MBeanAttributeInfo> filter,
                                                          boolean fAddStorageMBeanAttributes)
        {
        return f_mbeanServerProxy.execute(new GetAttributes(query, filter, fAddStorageMBeanAttributes));
        }

    /**
     * Invoke a JMX MBean operation on one or many MBeans determined by the
     * provided {@link QueryBuilder}.
     *
     * @param query           the {@link ParsedQuery} to be used to determine
     *                        applicable MBeans
     * @param sOperationName  the name of the operation
     * @param aoArguments     the arguments of the operation
     * @param asSignature     the signature of the operation
     *
     * @return a Map keyed by the full ObjectName registered with the MBeanServer
     *         and the value returned by invoking the operation
     */
    public Map<String, Object> invoke(ParsedQuery query,
                                      String      sOperationName,
                                      Object[]    aoArguments,
                                      String[]    asSignature)
        {
        return f_mbeanServerProxy.execute(
                new Invoke(query, sOperationName, aoArguments, asSignature));
        }

    /**
     * Update all JMX MBeans determined by evaluating the provided {@link QueryBuilder}.
     * All attributes in the provided map will be updated on the corresponding
     * MBean(s) iff the attribute is present.
     * <p>
     * A Set of ObjectNames corresponding to the updated MBeans is returned.
     * These MBeans have been updated with the matching attributes provided to
     * this update method. Attributes not present on the targeted MBean are
     * silently ignored unless there are no attributes that match and therefore
     * will be absent from the returned set of ObjectNames.
     *
     * @param query          the {@link ParsedQuery} to be used to determine
     *                       applicable MBeans
     * @param mapAttributes  the attributes to update
     *
     * @return a map keyed by ObjectNames updated with the map of attributes
     *         that were updated (based on the attributes present on the MBean)
     */
    public Map<String, Map<String, Object>> update(ParsedQuery query, Map<String, Object> mapAttributes)
        {
        return f_mbeanServerProxy.execute(new SetAttributes(query, mapAttributes));
        }

    /**
     * Return a list of MBean ObjectNames determined by the provided {@link
     * QueryBuilder}.
     *
     * @param query  the {@link ParsedQuery} to be used to determine applicable
     *               MBeans
     *
     * @return the Set of MBean ObjectNames
     */
    public Set<String> queryKeys(ParsedQuery query)
        {
        return f_mbeanServerProxy.queryNames(query.getQuery(), query.getObjectNameFilter());
        }

    // TODO: clean up this doc based on previous management resource description
    /**
     * Process the request for MBean attributes.
     * <p>
     * The requested MBean solely depends on the 'type' input. This may be one
     * of the values specified in the {@link MBeanCollectorFunction#MBEAN_TYPES
     * mbean types} constant.
     * <p>
     * The processing can be modified in three ways:
     * <ol>
     *   <li>reducing the data (querying)</li>
     *   <li>choosing the collector implementation (aggregating)</li>
     *   <li>select the attributes to return</li>
     * </ol>
     * The data can be reduced by specifying a context sensitive name. This name
     * is either the cache name, service name or a concatenation of service name
     * and cache name delimited by '!'.
     * In addition, either a regular expression evaluated against nodeIds or
     * specifying a role name is supported to reduce the MBeans to interrogate.
     * <p>
     * If the data is not reduced to a single MBean some attribute level aggregation
     * is applied to the MBeans. By default this aggregation is a list, however
     * each attribute may specify its own default as defined in the {@link
     * ServiceMBeanAttribute service} or {@link CacheMBeanAttribute cache}
     * enums.
     * <p>
     * Either all attributes are returned or a single attribute based upon the
     * absence or presence, respectively, of the attribute in the URI.
     *
     * @param query       the {@link QueryBuilder} to be used to generate MBean query
     * @param sLocator    either a regex to be applied against nodeids or a role name
     * @param sAttribute  the attribute to return
     * @param sCollector  the collector to use instead of the default
     *
     * @return a Map of attribute name to attribute value; the attribute value
     *         may be the result of an aggregation based upon the request
     */
    public Map<String, Object> aggregate(ParsedQuery query,
                                         String      sLocator,
                                         String      sAttribute,
                                         String      sCollector)
        {
        return f_mbeanServerProxy.execute(
                new MBeanCollectorFunction(sLocator, sAttribute, sCollector, query));
        }

    // ----- inner class: QueryBuilder --------------------------------------

    /**
     * The Query Builder for generating Coherence MBean queries.
     */
    public static class QueryBuilder
            implements java.io.Serializable
        {
        // ----- QueryBuilder methods ---------------------------------------

        /**
         * Set the base MBean query.
         *
         * @param sBaseQuery  the base query
         *
         * @return this {@link QueryBuilder} instance
         */
        public QueryBuilder withBaseQuery(String sBaseQuery)
            {
            m_sBaseQuery = sBaseQuery;
            return this;
            }

        /**
         * Set the cluster name to be used in the query.
         *
         * @param sCluster  the cluster name
         *
         * @return this {@link QueryBuilder} instance
         */
        public QueryBuilder withCluster(String sCluster)
            {
            m_sCluster = sCluster;
            return this;
            }

        /**
         * Set the service name to be used in the query.
         *
         * @param sService  the service name
         *
         * @return this {@link QueryBuilder} instance
         */
        public QueryBuilder withService(String sService)
            {
            m_sService = sService;
            return this;
            }

        /**
         * Set the member key to be used in the query.
         *
         * @param sMemberKey  the member key
         *
         * @return this {@link QueryBuilder} instance
         */
        public QueryBuilder withMember(String sMemberKey)
            {
            m_sMemberKey = sMemberKey;
            return this;
            }

        /**
         * Set the MBean domain name to be used on the query.
         *
         * @param sMBeanDomainName  the MBean domain name
         *
         * @return this {@link QueryBuilder} instance
         */
        public QueryBuilder withMBeanDomainName(String sMBeanDomainName)
            {
            m_sMBeanDomainName = sMBeanDomainName;
            return this;
            }

        /**
         *
         *
         * @param sKey       the key to apply the predicate against
         * @param predicate  the predicate to test the value against
         *
         * @return this {@link QueryBuilder} instance
         */
        public QueryBuilder withFilter(String sKey, Filter<String> predicate)
            {
            Objects.requireNonNull(sKey);
            Objects.requireNonNull(predicate);

            ensureMapFilters().put(sKey, predicate);
            return this;
            }

        /**
         * Ensure the generated query does not include a wild card (exact).
         *
         * @return this {@link QueryBuilder} instance
         */
        public QueryBuilder exact()
            {
            return exact(true);
            }

        /**
         * Ensure the generated query does not include a wild card (exact).
         *
         * @param fExact  whether the generated query should include a wild card
         *
         * @return this {@link QueryBuilder} instance
         */
        public QueryBuilder exact(boolean fExact)
            {
            m_fExact = fExact;
            return this;
            }

        /**
         * Build and return the MBean query.
         *
         * @return the complete MBean query
         */
        public ParsedQuery build()
            {
            String        sBaseQuery   = m_sBaseQuery;
            String        sClusterName = m_sCluster;
            String        sDomainName  = m_sMBeanDomainName;
            String        sMemberKey   = m_sMemberKey;
            String        sService     = m_sService;
            StringBuilder sbQuery      = new StringBuilder(sBaseQuery);

            if (sClusterName != null)
                {
                sbQuery.append(",").append(Registry.KEY_CLUSTER).append(sClusterName);
                }

            int ofDomain = sBaseQuery.indexOf(':');
            int ofEquals = sBaseQuery.indexOf('=');

            // prepend the MBean domain name if provided and not present in the base query

            // Note: ':' may be present in the value of the key/value pairs
            //       in the ObjectName and should not be considered
            if (sDomainName != null && (ofDomain <= 0 || ofDomain > ofEquals))
                {
                sbQuery.insert(0, sDomainName);
                }

            if (sMemberKey != null)
                {
                if (sMemberKey.matches("\\d+"))
                    {
                    sbQuery.append(",").append(NODE_ID).append("=").append(sMemberKey);
                    }
                else
                    {
                    sbQuery.append(",").append(MEMBER).append("=").append(sMemberKey);
                    }
                }

            if (sService != null)
                {
                sbQuery.append(",").append(SERVICE).append("=").append(sService);
                }

            // we cannot get a complete query name, for example when extended MBean is used
            // or in WLS where more key properties are added to the ObjectName
            // so we always search.
            if (!m_fExact)
                {
                sbQuery.append(",*");
                }

            try
                {
                Map<String, Filter<String>> mapFilters = m_mapFilters;
                if (mapFilters != null)
                    {
                    mapFilters = Collections.unmodifiableMap(new HashMap<>(mapFilters));
                    }
                return new ParsedQuery(
                        MBeanHelper.quoteCanonical(sbQuery.toString()),
                        mapFilters);
                }
            catch (MalformedObjectNameException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        /**
         * The Coherence cluster name to be used for the query.
         *
         * @return the Coherence clutser name
         */
        public String getCluster()
            {
            return m_sCluster;
            }

        /**
         * The MBean Domain to be used for the query.
         *
         * @return the MBean domain name
         */
        public String getMBeanDomain()
            {
            return m_sMBeanDomainName;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return this.getClass().getSimpleName()
                + "{member=" + m_sMemberKey
                + ", service=" + m_sService
                + ", cluster=" + m_sCluster
                + ", exact=" + m_fExact
                + ", domain=" + m_sMBeanDomainName
                + ", query=" + m_sBaseQuery
                + ", filtermap=" + m_mapFilters
                + '}';
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Ensure a Map is created to store {@link Filter}s to apply against
         * values of the key/value pairs within the ObjectName.
         *
         * @return a Map of key to Filter
         */
        protected Map<String, Filter<String>> ensureMapFilters()
            {
            Map<String, Filter<String>> map = m_mapFilters;
            if (map == null)
                {
                // the common path is to have a single predicate (domainPartition)
                map = m_mapFilters = new ConcurrentHashMap<>(1);
                }
            return map;
            }

        // ----- inner class: ParsedQuery -----------------------------------

        /**
         * A ParsedQuery represents the result of a call to {@link QueryBuilder#build()}.
         * A ParsedQuery instance is required by many methods of the {@link MBeanAccessor}.
         */
        public static class ParsedQuery
            implements Serializable
            {
            // ----- constructors -------------------------------------------

            /**
             * Construct a ParsedQuery instance.
             */
            protected ParsedQuery()
                {
                }

            /**
             * Construct a ParsedQuery based on the given query expression and
             * the map of key to Filter used to test against the corresponding
             * key/value within the ObjectName.
             *
             * @param sQuery      the query to reduce the ObjectNames to consider
             * @param mapFilters  a map of Filters to further reduce the MBeans
             */
            protected ParsedQuery(String sQuery, Map<String, Filter<String>> mapFilters)
                {
                m_sQuery     = sQuery;
                m_mapFilters = mapFilters;
                }

            // ----- ParsedQuery methods ------------------------------------

            /**
             * Return the query to reduce the ObjectNames to consider.
             *
             * @return the query to reduce the ObjectNames to consider
             */
            public String getQuery()
                {
                return m_sQuery;
                }

            /**
             * Return a map of key to Filter used to test against the corresponding
             * key/value within the ObjectName.
             *
             * @return a map of Filters to further reduce the MBeans
             */
            public Map<String, Filter<String>> getMapFilters()
                {
                return m_mapFilters;
                }

            /**
             * Return a Filter that can be applied against an ObjectName and
             * ensures all the Filters within the {@link #getMapFilters() map}
             * of filters test successfully (logical AND).
             *
             * @return a Filter that can be applied against an ObjectName representing
             *         this query
             */
            public Filter<ObjectName> getObjectNameFilter()
                {
                return instantiateObjectNameFilter(m_mapFilters);
                }

            private static Filter<ObjectName> instantiateObjectNameFilter(Map<String, Filter<String>> mapFilters)
                {
                if (mapFilters == null)
                    {
                    return AlwaysFilter.INSTANCE();
                    }

                return objectName ->
                    {
                    for (Map.Entry<String, Filter<String>> entry : mapFilters.entrySet())
                        {
                        if (!entry.getValue().evaluate(objectName.getKeyProperty(entry.getKey())))
                            {
                            return false;
                            }
                        }
                    return true;
                    };
                }

            // ----- constants ----------------------------------------------

            static final private long serialVersionUID = -1;

            // ----- data members -------------------------------------------

            /**
             * A map of ObjectName key to Filter.
             */
            protected Map<String, Filter<String>> m_mapFilters;

            /**
             * A query that can be passed to the MBeanServer to reduce the MBeans.
             */
            protected String m_sQuery;
            }

        // ----- data members -----------------------------------------------

        /**
         * The member key to be used in the query.
         */
        protected String m_sMemberKey;

        /**
         * The service name to be used in the query.
         */
        protected String m_sService;

        /**
         * The cluster name to be used in the query.
         */
        protected String m_sCluster;

        /**
         * Whether to construct a query without a wildcard suffix (exact) or
         * with.
         */
        protected boolean m_fExact = false;

        /**
         * The MBean domain name to be used in the query.
         */
        protected String m_sMBeanDomainName;

        /**
         * The base MBean query to be used in the query.
         */
        protected String m_sBaseQuery;

        /**
         * A map of ObjectName key to Filter.
         */
        protected Map<String, Filter<String>> m_mapFilters;

        // ----- constants ----------------------------------------------

        static final private long serialVersionUID = -1;
        }

    // ----- inner class: SetAttributes--------------------------------------

    /**
     * The Remote.Function for the {@link javax.management.MBeanServerConnection}.setAttributes
     * method.
     */
    public static class SetAttributes
            implements Remote.Function<MBeanServer, Map<String,  Map<String, Object>>>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor.
         */
        public SetAttributes()
            {
            }

        /**
         * Construct a SetAttributes object.
         *
         * @param query          the MBean query builder
         * @param mapAttributes  the map of attributes to be updated
         */
        public SetAttributes(ParsedQuery query, Map<String, Object> mapAttributes)
            {
            m_query         = query;
            m_mapAttributes = mapAttributes;
            }

        // ----- Remote.Function interface ----------------------------------

        @Override
        public Map<String,  Map<String, Object>> apply(MBeanServer mBeanServer)
            {
            try
                {
                AttributeList attrList = getAttributeList(m_mapAttributes);
                ParsedQuery   query    = m_query;

                Map<String, Map<String, Object>> mapUpdatedMBeans = new HashMap<>();

                if (!attrList.isEmpty())
                    {
                    Set<ObjectName> setObjectNames = mBeanServer.queryNames(new ObjectName(query.getQuery()),
                            new MBeanHelper.QueryExpFilter(query.getObjectNameFilter()));


                    for (ObjectName oObjectName : setObjectNames)
                        {
                        try
                            {
                            AttributeList       attrResponseList     = mBeanServer.setAttributes(oObjectName, attrList);
                            Map<String, Object> mapUpdatedAttributes = new HashMap<>();

                            if (attrResponseList != null && !attrResponseList.isEmpty())
                                {
                                for (Attribute attr : attrResponseList.asList())
                                    {
                                    mapUpdatedAttributes.put(attr.getName(), attr.getValue());
                                    }
                                }
                            mapUpdatedMBeans.put(oObjectName.toString(), mapUpdatedAttributes);
                            }
                        catch (InstanceNotFoundException e)
                            {
                            // ignore when MBean unregistered between query and request to set its attributes
                           Logger.log("MBeanAccessor$SetAttributes#apply(objName=" + oObjectName +
                                      "): ignoring InstanceNotFoundException: " + e.getMessage(), Base.LOG_QUIET);
                            }
                        }
                    }
                return mapUpdatedMBeans;
                }
            catch (RuntimeMBeanException e)
                {
                if (e.getTargetException() instanceof SecurityException)
                    {
                    throw e.getTargetException();
                    }
                throw e;
                }
            catch (Exception e)
                {
                throw new RuntimeException(e);
                }
            }

        // ----- SetAttributes methods --------------------------------------

        /**
         * Convert the entity map to an {@link AttributeList}
         *
         * @param mapEntity  the input entity map
         *
         * @return the {@link AttributeList}
         */
        protected AttributeList getAttributeList(Map<String, Object> mapEntity)
            {
            return new AttributeList(mapEntity.entrySet()
                    .stream().map(e -> new Attribute(e.getKey(), e.getValue()))
                    .collect(Collectors.toList()));
            }

        // ----- constants ----------------------------------------------

        static final private long serialVersionUID = -1;

        // ----- data members ---------------------------------------------------

        /**
         * The ParsedQuery to be used to query the MBeans.
         */
        protected ParsedQuery m_query;

        /**
         * The AttributeList to be updated.
         */
        protected Map<String, Object> m_mapAttributes;
        }

    // ----- inner class: Invoke --------------------------------------------

    /**
     * The Remote.Function for the {@link javax.management.MBeanServerConnection}.invoke
     * method.
     */
    public static class Invoke
            implements Remote.Function<MBeanServer, Map<String, Object>>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor.
         */
        public Invoke()
            {
            }

        /**
         * Create an Invoke object.
         *
         * @param query           the MBean query
         * @param sOperationName  the name of the operation to be invoked
         * @param aoArguments     the arguments to the operation
         * @param asSignature     the signature of the operation
         */
        public Invoke(ParsedQuery query, String sOperationName, Object[] aoArguments, String[] asSignature)
            {
            m_query           = query;
            m_sOperationName  = sOperationName;
            m_arguments       = aoArguments;
            m_signature       = asSignature;
            }

        // ----- Remote.Function interface ----------------------------------

        @Override
        public Map<String, Object> apply(MBeanServer mBeanServer)
            {
            try
                {
                ParsedQuery         query     = m_query;
                Map<String, Object> mapMBeans = new HashMap<>();

                Set<ObjectName> setObjectNames = mBeanServer.queryNames(new ObjectName(query.getQuery()),
                        new MBeanHelper.QueryExpFilter(query.getObjectNameFilter()));

                for (ObjectName oObjectName : setObjectNames)
                    {
                    String sObjectName = oObjectName.toString();
                    Object result = null;
                    try
                        {
                        result = invokeOperation(mBeanServer, sObjectName);
                        }
                    catch (InstanceNotFoundException e)
                        {
                        // ignore; assume MBean unregistered between query and invoke
                        Logger.log("MBeanAccessor$Invoke#apply(mbeanServer=" + mBeanServer +
                                         ", objName=" + sObjectName +
                                         "): ignoring InstanceNotFoundException: " + e.getMessage(), Base.LOG_QUIET);

                        continue;
                        }
                    catch (WrapperException e)
                        {
                        if (e.getCause() instanceof InstanceNotFoundException)
                            {
                            // ignore; assume MBean unregistered between query and invoke
                            CacheFactory.log("MBeanAccessor$Invoke#apply(mbeanServer=" + mBeanServer +
                                    ", objName=" + sObjectName +
                                    "): ignoring InstanceNotFoundException: " + e.getMessage(), Base.LOG_QUIET);

                            continue;
                            }
                        throw e;
                        }
                    mapMBeans.put(sObjectName, result);
                    }
                return mapMBeans;
                }
            catch (SecurityException e)
                {
                // ensure a SecurityException is not wrapped in RuntimeException
                throw e;
                }
            catch (Exception e)
                {
                throw new RuntimeException(e);
                }
            }

        // ----- Invoke methods ---------------------------------------------

        /**
         * Invoke the operation on the MBean.
         *
         * @param mBeanServer  the MBeanServer to be used
         * @param sObjectName  the ObjectName of the MBean
         *
         * @return the response of the operation
         */
        protected Object invokeOperation(MBeanServer mBeanServer, String sObjectName)
            throws InstanceNotFoundException
            {
            try
                {
                return mBeanServer.invoke(new ObjectName(sObjectName), m_sOperationName, m_arguments, m_signature);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e, "invoke operation " + m_sOperationName + " to MBean " + sObjectName + " failed with an exception");
                }
            }

        // ----- constants ----------------------------------------------

        static final private long serialVersionUID = -1;

        // ----- data members ---------------------------------------------------

        /**
         * The query used to reduce the MBeans.
         */
        protected ParsedQuery m_query;

        /**
         * The the name of the operation to be invoked.
         */
        protected String m_sOperationName;

        /**
         * The arguments to the operation.
         */
        protected Object[] m_arguments;

        /**
         * The signature of the operation.
         */
        protected String[] m_signature;
        }

    // ----- inner class: GetAttributes -------------------------------------

    /**
     * The Remote.Function for the {@link javax.management.MBeanServerConnection}.getAttributes
     * method.
     */
    public static class GetAttributes
            implements Remote.Function<MBeanServer, Map<String, Map<String, Object>>>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor.
         */
        public GetAttributes()
            {
            }

        /**
         * Create a GetAttributes object.
         *
         * @param query  the MBean query
         */
        public GetAttributes(ParsedQuery query)
            {
            this(query, null, true);
            }

        /**
         * Create a GetAttributes object.
         *
         * @param query  the MBean query
         * @param filter server side filter for attributes to return
         * @param fAddStorageMBeanAttributes when true, append Storage MBean attributes
         *                                   to a back tier cache mbean.
         */
        public GetAttributes(ParsedQuery query, Filter<MBeanAttributeInfo> filter, boolean fAddStorageMBeanAttributes)
            {
            m_query                      = query;
            m_filter                     = filter;
            m_fAddStorageMBeanAttributes = fAddStorageMBeanAttributes;
            }

        // ----- Remote.Function interface ----------------------------------

        @Override
        public Map<String, Map<String, Object>> apply(MBeanServer mBeanServer)
            {
            try
                {
                ParsedQuery                      query     = m_query;
                Map<String, Map<String, Object>> mapMBeans = new HashMap<>();

                Set<ObjectName> setObjectNames = mBeanServer.queryNames(new ObjectName(query.getQuery()),
                        new MBeanHelper.QueryExpFilter(query.getObjectNameFilter()));

                for (ObjectName oObjectName : setObjectNames)
                    {
                    String sObjectName = oObjectName.toString();
                    Map<String, Object> mapAttributes = new HashMap<>();

                    try
                        {
                        addMBeanAttributes(oObjectName, mapAttributes, mBeanServer, m_filter);
                        }
                    catch (InstanceNotFoundException e)
                        {
                        // ignore when MBean unregistered between query and request for its attributes
                        Logger.log("MBeanAccessor$GetAttributes#apply(objName=" + sObjectName +
                                         "): ignoring InstanceNotFoundException: " + e.getMessage(), Base.LOG_QUIET);
                        }

                    // in case of CacheMBean, if it is a back tier cache, then append the StorageManageMBean
                    // attributes also to the response map
                    ObjectName objName = new ObjectName(sObjectName);
                    if (m_fAddStorageMBeanAttributes && objName.getKeyProperty(TYPE).equals("Cache") &&
                            objName.getKeyProperty(TIER).equals("back"))
                        {
                        String sCacheName   = objName.getKeyProperty(NAME);
                        String sServiceName = objName.getKeyProperty(SERVICE);
                        String sNodeId      = objName.getKeyProperty(NODE_ID);
                        String sBaseQuery   = STORAGE_MANAGER_QUERY + sCacheName;

                        QueryBuilder bldrStorageQuery = new QueryBuilder().withBaseQuery(sBaseQuery)
                                .withMBeanDomainName(objName.getDomain())
                                .withService(sServiceName)
                                .withMember(sNodeId);

                        String sCluster = objName.getKeyProperty(CLUSTER);
                        if (sCluster != null)
                            {
                            bldrStorageQuery.withCluster(sCluster);
                            }

                        ParsedQuery queryStorage = bldrStorageQuery.build();

                        setObjectNames = mBeanServer.queryNames(new ObjectName(queryStorage.getQuery()),
                                new MBeanHelper.QueryExpFilter(queryStorage.getObjectNameFilter()));

                        if (setObjectNames.size() == 1)
                            {
                            addMBeanAttributes(setObjectNames.iterator().next(), mapAttributes, mBeanServer, m_filter);
                            }
                        }

                    mapMBeans.put(sObjectName, mapAttributes);
                    }
                return mapMBeans;
                }
            catch (Exception e)
                {
                throw new RuntimeException(e);
                }
            }

        // ----- GetAttributes methods --------------------------------------

        /**
         * Add attributes of an MBean to a Map.
         *
         * @param oObjName       the attributes filter
         * @param mapAttributes  the response map
         * @param mBeanServer    the MBeanServer
         * @param filter         the MBeanAttributeInfo filter
         *
         * @throws Exception if an error occurs
         */
        protected void addMBeanAttributes(ObjectName          oObjName,
                                          Map<String, Object> mapAttributes,
                                          MBeanServer         mBeanServer,
                                          Filter<MBeanAttributeInfo> filter) throws Exception
            {
            MBeanInfo info = mBeanServer.getMBeanInfo(oObjName);
            String[] arrAttributes = Arrays.stream(info.getAttributes())
                    .filter(attrInfo -> filter == null ? true : filter.evaluate(attrInfo))
                    .map(MBeanAttributeInfo::getName)
                    .collect(Collectors.toSet()).toArray(new String[0]);

            AttributeList attrList = mBeanServer.getAttributes(oObjName, arrAttributes);
            if (attrList != null && !attrList.isEmpty())
                {
                for (Attribute attribute : attrList.asList())
                    {
                    mapAttributes.put(attribute.getName(), attribute.getValue());
                    }

                }
            }

        // ----- constants ----------------------------------------------

        static final private long serialVersionUID = -1;

        // ----- data members ---------------------------------------------------

        /**
         * The query used to reduce the MBeans.
         */
        protected ParsedQuery                m_query;

        /**
         * MBean Attribute filter.
         */
        protected Filter<MBeanAttributeInfo> m_filter = null;

        /**
         * Provide a way to disable management over rest assumption that
         * we want storage mbean attributes when processing a back tier cache.
         */
        protected boolean                    m_fAddStorageMBeanAttributes;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link MBeanServerProxy} to be used for MBean operations.
     */
    protected final MBeanServerProxy f_mbeanServerProxy;

    // ----- static constants -----------------------------------------------

    /**
     * The "name" key in the ObjectName.
     */
    public static final String NAME     = "name";

    /**
     * The "type" key in the ObjectName.
     */
    public static final String TYPE     = "type";

    /**
     * The "service" key in the ObjectName.
     */
    public static final String SERVICE  = Registry.KEY_SERVICE.substring(0, Registry.KEY_SERVICE.indexOf('='));

    /**
     * The "tier" key in the ObjectName.
     */
    public static final String TIER     = "tier";

    /**
     * The "nodeId" key in the ObjectName.
     */
    public static final String NODE_ID  = Registry.KEY_NODE_ID.substring(0, Registry.KEY_NODE_ID.indexOf('='));

    /**
     * The "member" key in the ObjectName.
     */
    public static final String MEMBER   = Registry.KEY_MEMBER.substring(0, Registry.KEY_MEMBER.indexOf('='));

    /**
     * The "cluster" key in the ObjectName.
     */
    public static final String CLUSTER   = Registry.KEY_CLUSTER.substring(0, Registry.KEY_CLUSTER.indexOf('='));

    /**
     * MBean query to filter out StorageManager MBean of a specific cache and service, running on a specific node.
     */
    public static final String STORAGE_MANAGER_QUERY = ":" + Registry.STORAGE_MANAGER_TYPE + ",cache=";
    }
