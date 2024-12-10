/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.util.Base;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A {@link SchemeMappingRegistry} provides a mechanism to manage a collection
 * of {@link ResourceMapping}s, together with the ability to search the registry for
 * said {@link ResourceMapping}s, possibly using wildcards.
 * <p>
 * {@link SchemeMappingRegistry}s are {@link Iterable}, the order of iteration
 * being that in which the {@link ResourceMapping}s where added to the said
 * {@link SchemeMappingRegistry}.
 * <p>
 * There is a separate namespace for {@link CacheMapping} and {@link TopicMapping}, allowing
 * for a cache and a topic with exactly same name.
 *
 * @author jk 2015.06.01
 * @since Coherence 14.1.1
 */
public class SchemeMappingRegistry
        implements ResourceMappingRegistry
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link SchemeMappingRegistry}.
     */
    public SchemeMappingRegistry()
        {
        f_mapMappings = new LinkedHashMap<>();
        }

    // ----- ResourceMappingRegistry methods ----------------------------------------

    @Override
    public Iterator<ResourceMapping> iterator()
        {
        return f_mapMappings.values().iterator();
        }

    @Override
    public void register(ResourceMapping mapping)
            throws IllegalArgumentException
        {
        String           sName     = mapping.getNamePattern();
        Class            classType = mapping instanceof CacheMapping ? CacheMapping.class : mapping instanceof TopicMapping ? TopicMapping.class : null;
        SchemeMappingKey key       = new SchemeMappingKey(classType, sName);

        if (classType == null)
            {
            throw new IllegalArgumentException("SchemeMappingRegistry.register: unknown class type: " + mapping.getClass().getCanonicalName());
            }

        if (f_mapMappings.containsKey(key))
            {
            String sElementName = mapping.getConfigElementName();
            throw new IllegalArgumentException(String.format(
                "Attempted to redefine an existing mapping for <%s>%s</%s>",
                sElementName, sName, sElementName));
            }
        else
            {
            f_mapMappings.put(key, mapping);
            }

        List<ResourceMapping> listMappings = mapping.getSubMappings();
        if (listMappings != null)
            {
            for (ResourceMapping mappingChild : listMappings)
                {
                register(mappingChild);
                }
            }
        }

    @Override
    public <M extends ResourceMapping> M findMapping(String sName, Class<M> type)
        {
        ResourceMapping mapping;

        // is there an exact match for the provided name?
        // ie: is the name explicitly defined as a mapping without wildcards?

        SchemeMappingKey key = new SchemeMappingKey(type, sName);
        if (f_mapMappings.containsKey(key))
            {
            mapping = f_mapMappings.get(key);
            }
        else
            {
            // attempt to find the most specific (ie: longest) wildcard defined
            // mapping that matches the name
            mapping = null;

            for (ResourceMapping mappingNext : f_mapMappings.values())
                {
                if (type.isAssignableFrom(mappingNext.getClass()) && mappingNext.isForName(sName))
                    {
                    if (mapping == null)
                        {
                        mapping = mappingNext;
                        }
                    else if (mappingNext.getNamePattern().length() > mapping.getNamePattern().length())
                        {
                        mapping = mappingNext;
                        }
                    }
                }
            }

        if (mapping == null)
            {
            return null;
            }

        //noinspection unchecked
        return (M) mapping;
        }

    @Override
    public int size()
        {
        return f_mapMappings.size();
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Key class for a scheme mapping.
     */
    static public class SchemeMappingKey
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link SchemeMappingKey}.
         *
         * @param clz    class of registered scheme mapping
         * @param sName  name of registered scheme mapping
         */
        public SchemeMappingKey(Class<? extends ResourceMapping> clz, String sName)
            {
            if (clz == null)
                {
                throw new NullPointerException("SchemeMapping class cannot be null");
                }

            if (sName == null)
                {
                throw new NullPointerException("SchemeMapping name cannot be null");
                }

            f_clz = clz;
            f_sName = sName;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the scheme mapping class.
         *
         * @return  the scheme mapping class
         */
        public Class<? extends ResourceMapping> getSchemeMappingClass()
            {
            return f_clz;
            }

        /**
         * Return the resource name.
         *
         * @return  the resource name
         */
        public String getName()
            {
            return f_sName;
            }

        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }

            if (o == null || !Base.equals(getClass(), o.getClass()))
                {
                return false;
                }

            SchemeMappingKey that = (SchemeMappingKey) o;

            return Base.equals(f_clz, that.f_clz) && Base.equals(f_sName, that.f_sName);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
            {
            int result = f_clz.hashCode();

            result = 31 * result + f_sName.hashCode();

            return result;
            }

        // ----- data members -----------------------------------------------

        /**
         * The scheme mapping class.
         */
        private final Class<? extends ResourceMapping> f_clz;

        /**
         * The scheme mapping name.
         */
        private final String f_sName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of {@link ResourceMapping}s.
     */
    private final LinkedHashMap<SchemeMappingKey, ResourceMapping> f_mapMappings;
    }
