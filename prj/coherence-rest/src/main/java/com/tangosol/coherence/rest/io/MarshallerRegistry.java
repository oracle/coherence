/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.rest.DefaultKeyConverter;

import com.tangosol.coherence.rest.util.JsonMap;
import com.tangosol.coherence.rest.util.StaticContent;

import com.tangosol.util.Base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Constructor;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Registry for marshaller instances.
 * <p>
 * This class allows marshaller lookup based on root object class and media
 * type, and attempts to create new marshallers for a given class and media
 * type combination.
 *
 * @author as  2011.07.10
 */
public class MarshallerRegistry
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Construct a MarshallerRegistry instance.
     * <p>
     * By default, a {@link JacksonJsonMarshaller} will be used for JSON
     * marshalling and a {@link JaxbXmlMarshaller} for XML marshalling. You
     * can override this behavior by calling the
     * {@link #setDefaultMarshaller(MediaType, Class)} method.
     */
    public MarshallerRegistry()
        {
        setDefaultMarshaller(MediaType.APPLICATION_JSON, JacksonJsonMarshaller.class);
        setDefaultMarshaller(MediaType.APPLICATION_XML,  JaxbXmlMarshaller.class);
        setDefaultMarshaller(MediaType.TEXT_PLAIN,       StringMarshaller.class);

        // register pass-through marshallers
        registerMarshaller(StaticContent.class, MediaType.WILDCARD_TYPE,    new StaticContentMarshaller());
        registerMarshaller(StaticContent.class, MediaType.APPLICATION_JSON, new JacksonJsonMarshaller<>(JsonMap.class));  // JsonMap --> JSON
        registerMarshaller(JsonMap.class,       MediaType.APPLICATION_JSON, new JacksonJsonMarshaller<>(JsonMap.class));  // JSON -> JsonMap

        // register marshallers for primitive types
        registerMarshaller(Short.class, MediaType.WILDCARD_TYPE,
                           new KeyConverterAdapter(new DefaultKeyConverter(Short.class)));
        registerMarshaller(Integer.class, MediaType.WILDCARD_TYPE,
                           new KeyConverterAdapter(new DefaultKeyConverter(Integer.class)));
        registerMarshaller(Long.class, MediaType.WILDCARD_TYPE,
                           new KeyConverterAdapter(new DefaultKeyConverter(Long.class)));
        registerMarshaller(Float.class, MediaType.WILDCARD_TYPE,
                           new KeyConverterAdapter(new DefaultKeyConverter(Float.class)));
        registerMarshaller(Double.class, MediaType.WILDCARD_TYPE,
                           new KeyConverterAdapter(new DefaultKeyConverter(Double.class)));

        // register marshallers for well-known reference types
        registerMarshaller(String.class, MediaType.APPLICATION_JSON,
                           JacksonJsonMarshaller.class);
        registerMarshaller(String.class, MediaType.WILDCARD_TYPE,
                           new KeyConverterAdapter(new DefaultKeyConverter(String.class)));
        registerMarshaller(BigDecimal.class, MediaType.WILDCARD_TYPE,
                           new KeyConverterAdapter(new DefaultKeyConverter(BigDecimal.class)));
        registerMarshaller(BigInteger.class, MediaType.WILDCARD_TYPE,
                           new KeyConverterAdapter(new DefaultKeyConverter(BigInteger.class)));
        }

    // ---- methods ---------------------------------------------------------

    /**
     * Return the marshaller for the specified root class and media type.
     *
     * @param clzRoot    root object class
     * @param mediaType  media type
     *
     * @return marshaller for the specified root class and media type
     */
    public Marshaller getMarshaller(Class clzRoot, MediaType mediaType)
        {
        String sMediaType = mediaType.getType() + "/" + mediaType.getSubtype();
        return getMarshaller(clzRoot, sMediaType);
        }

    /**
     * Return the marshaller for the specified root class and media type.
     *
     * @param clzRoot     root object class
     * @param sMediaType  media type
     *
     * @return marshaller for the specified root class and media type
     */
    public Marshaller getMarshaller(Class clzRoot, String sMediaType)
        {
        String     sKey       = createKey(clzRoot, sMediaType);
        Marshaller marshaller = m_mapMarshallers.get(sKey);

        if (marshaller == null)
            {
            marshaller = m_mapMarshallers.get(createKey(clzRoot, MediaType.WILDCARD));
            if (marshaller == null)
                {
                marshaller = createDefaultMarshaller(clzRoot, sMediaType);
                if (marshaller != null)
                    {
                    Marshaller old = m_mapMarshallers.putIfAbsent(sKey, marshaller);
                    if (old != null)
                        {
                        marshaller = old;
                        }
                    }
                }
            }

        return marshaller == NULL_MARSHALLER ? null : marshaller;
        }

    /**
     * Register a marshaller for the specified root class and media type.
     *
     * @param clzRoot        root object class
     * @param mediaType      media type
     * @param clzMarshaller  marshaller class
     */
    public void registerMarshaller(Class clzRoot, MediaType mediaType,
            Class clzMarshaller)
        {
        registerMarshaller(clzRoot, mediaType.toString(), clzMarshaller);
        }

    /**
     * Register a marshaller for the specified root class and media type.
     *
     * @param clzRoot        root object class
     * @param sMediaType     media type
     * @param clzMarshaller  marshaller class
     */
    public void registerMarshaller(Class clzRoot, String sMediaType,
            Class clzMarshaller)
        {
        registerMarshaller(clzRoot, sMediaType,
                createMarshaller(clzRoot, sMediaType, clzMarshaller));
        }

    /**
     * Register a marshaller for the specified root class and media type.
     *
     * @param clzRoot     root object class
     * @param mediaType   media type
     * @param marshaller  marshaller to register
     */
    public void registerMarshaller(Class clzRoot, MediaType mediaType,
                                   Marshaller marshaller)
        {
        registerMarshaller(clzRoot, mediaType.toString(), marshaller);
        }

    /**
     * Register a marshaller for the specified root class and media type.
     *
     * @param clzRoot     root object class
     * @param sMediaType  media type
     * @param marshaller  marshaller to register
     */
    public void registerMarshaller(Class clzRoot, String sMediaType,
                                   Marshaller marshaller)
        {
        m_mapMarshallers.put(createKey(clzRoot, sMediaType), marshaller);
        }

    /**
     * Configure the default marshaller class for the specified media type.
     *
     * @param mediaType      media type to set default marshaller for
     * @param clzMarshaller  default marshaller class
     */
    public void setDefaultMarshaller(MediaType mediaType, Class clzMarshaller)
        {
        setDefaultMarshaller(mediaType.toString(), clzMarshaller);
        }

    /**
     * Configure the default marshaller class for the specified media type.
     *
     * @param sMediaType     media type to set default marshaller for
     * @param clzMarshaller  default marshaller class
     */
    public void setDefaultMarshaller(String sMediaType, Class clzMarshaller)
        {
        m_mapDefaultMarshallers.put(sMediaType, clzMarshaller);
        }

    /**
     * Configure the default marshaller classes for the corresponding media types.
     *
     * @param mapMarshallers  a map of REST marshaller classes keyed by media type
     */
    public void setDefaultMarshallers(Map<String, Class> mapMarshallers)
        {
        for (Map.Entry<String, Class> entry : mapMarshallers.entrySet())
            {
            m_mapDefaultMarshallers.put(entry.getKey(), entry.getValue());
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Create a key for the marshallers map.
     *
     * @param clzRoot     root object class
     * @param sMediaType  media type
     *
     * @return key for the marshallers cache
     */
    protected String createKey(Class clzRoot, String sMediaType)
        {
        return clzRoot.getName() + ":" + sMediaType;
        }

    /**
     * Create default marshaller for the specified class and media type.
     *
     * @param clzRoot     root object class
     * @param sMediaType  media type
     *
     * @return default marshaller for the specified class and media type
     */
    protected Marshaller createDefaultMarshaller(Class clzRoot, String sMediaType)
        {
        Class clzDefaultMarshaller = m_mapDefaultMarshallers.get(sMediaType);
        if (clzDefaultMarshaller == null)
            {
            Logger.warn("Default marshaller for media type " + sMediaType + " is not registered.");
            return NULL_MARSHALLER;
            }

        return createMarshaller(clzRoot, sMediaType, clzDefaultMarshaller);
        }

    /**
     * Create marshaller instance.
     *
     * @param clzRoot        root object class
     * @param sMediaType     media type
     * @param clzMarshaller  marshaller class
     *
     * @return marshaller instance
     */
    protected Marshaller createMarshaller(Class<?> clzRoot, String sMediaType,
            Class<?> clzMarshaller)
        {
        if (clzMarshaller == null || !Marshaller.class.isAssignableFrom(clzMarshaller))
            {
            throw new IllegalArgumentException("invalid marshaller: " + clzMarshaller);
            }

        Constructor ctor = null;
        try
            {
            ctor = clzMarshaller.getConstructor(Class.class);
            }
        catch (NoSuchMethodException e)
            {
            // ignore
            }

        try
            {
            return ctor == null
                   ? (Marshaller) clzMarshaller.newInstance()
                   : (Marshaller) ctor.newInstance(clzRoot);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e,
                                              "error creating marshaller " + clzMarshaller +
                                              " for class \"" + clzRoot.getName() +
                                              "\" and media type \"" + sMediaType + "\"");
            }
        }

    // ---- inner class: NullMarshaller -------------------------------------

    /**
     * Marshaller implementation that does nothing.
     */
    private static class NullMarshaller implements Marshaller<Object>
        {
        @Override
        public void marshal(Object oValue, OutputStream out, MultivaluedMap<String, Object> httpHeaders)
                throws IOException
            {
            }

        @Override
        public Object unmarshal(InputStream in, MediaType mediaType)
                throws IOException
            {
            return null;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Special value that is used to optimize marshaller retrieval.
     */
    private static final Marshaller NULL_MARSHALLER = new NullMarshaller();

    // ---- data members ----------------------------------------------------

    /**
     * A map of registered Marshallers map, keyed by media type.
     */
    private ConcurrentMap<String, Marshaller> m_mapMarshallers =
            new ConcurrentHashMap<>();

    /**
     * A map of default marshallers, keyed by media type.
     */
    private ConcurrentMap<String, Class> m_mapDefaultMarshallers =
            new ConcurrentHashMap<>();
    }
