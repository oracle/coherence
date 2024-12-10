/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.ext.GensonBundle;

import com.oracle.coherence.io.json.internal.NullSetConverter;

import com.tangosol.coherence.config.Config;

import com.tangosol.util.Base;
import com.tangosol.util.NullImplementation;

import java.io.IOException;

import java.net.URL;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * A {@link GensonBundleProvider} providing a {@link GensonBundle} that configures package aliases.
 *
 * @since 20.06
 */
public class CoherenceBundleProvider
        implements GensonBundleProvider
    {
    // ----- GensonBundleProvider interface ---------------------------------

    @Override
    public GensonBundle provide()
        {
        return new CoherenceBundle();
        }

    // ----- inner class: CoherenceBundle -----------------------------------

    /**
     * A {@link GensonBundle} that adds package aliases.
     */
    protected static final class CoherenceBundle
            extends GensonBundle
        {
        // ----- GensonBundle interface -------------------------------------

        @Override
        public void configure(GensonBuilder builder)
            {
            builder.setEnforceTypeAliases(Config.getBoolean(JSON_TYPE_ALIAS_ENFORCEMENT, true))
                    .withConverter(NullSetConverter.INSTANCE, NullImplementation.NullSet.class);

            ExternalConfiguration extConfig = new ExternalConfiguration();
            extConfig.apply(builder, Base.getContextClassLoader());
            }
        }

    // ----- inner class: ExternalConfiguration -----------------------------

    /**
     * This class is responsible for loading external configuration files and applying
     * the configuration to the provided {@link GensonBuilder}.
     *
     * Two files will be scanned for on the classpath:
     * <ul>
     *     <li>{@value #TYPE_ALIASES_PROPERTIES}</li>
     *     <li>{@value #PACKAGE_ALIASES_PROPERTIES}</li>
     * </ul>
     *
     * The key/values found in these properties files will be applied to Genson.
     *
     * @since 20.12
     */
    protected static final class ExternalConfiguration
        {
        // ----- constructors -----------------------------------------------

        /**
         * Creates a new {@code ExternalConfiguration}.
         */
        protected ExternalConfiguration()
            {
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Apply the loaded configuration values to the provided {@link GensonBuilder}.
         *
         * @param builder  the {@link GensonBuilder} to apply the external configuration to
         * @param loader   the {@link ClassLoader} that should be used to load classes.
         *                 If the loader is {@code null}, then the context {@link ClassLoader}
         *                 will be used
         *
         * @return the {@link GensonBuilder} that may have been updated
         */
        protected GensonBuilder apply(GensonBuilder builder, ClassLoader loader)
            {
            Objects.requireNonNull(builder, "builder may not be null");

            final GensonBuilder  currentBuilder = builder;
            ClassLoader          clzLoader      = loader == null ? Base.getContextClassLoader() : loader;
            Map<Properties, URL> typeAliases    = loadConfigurationFiles(TYPE_ALIASES_PROPERTIES);
            Map<Properties, URL> pkgAliases     = loadConfigurationFiles(PACKAGE_ALIASES_PROPERTIES);

            typeAliases.forEach((properties, url) ->
                processProperties(properties, url, (sAlias, sAliasFor) ->
                    {
                    if (properties.containsKey(sAliasFor))
                        {
                        // compatibility alias
                        currentBuilder.addCompatibilityAlias(sAlias, sAliasFor);
                        }
                    else
                        {
                        currentBuilder.addAlias(sAlias, Class.forName(sAliasFor, false, clzLoader));
                        }
                    }));

            pkgAliases.forEach((properties, url) ->
                    processProperties(properties, url, currentBuilder::addPackageAlias));

            return currentBuilder;
            }

        /**
         * Process the properties loaded from the specified url.  The consumer is logic
         * that may handle the property key/value as needed.
         *
         * @param props     the properties
         * @param url       the location the properties were loaded from
         * @param consumer  logic to process each property
         */
        protected void processProperties(Properties props, URL url, ThrowingBiConsumer<String, String> consumer)
            {
            props.forEach((oAlias, oAliasFor) ->
                {
                try
                    {
                    consumer.accept(oAlias.toString(), oAliasFor.toString());
                    }
                catch (IllegalArgumentException iae)
                    {
                    throw Base.ensureRuntimeException(iae,
                            String.format("Failed to process type aliases defined in %s", url));
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                });
            }

        /**
         * Loads all JSON serializer configuration files from the classpath.
         *
         * @param sResourceName  the resource to load from the classpath
         *
         * @return a {@link Map} keyed by the loaded properties and
         *         associated with the URL from which it was loaded
         */
        protected Map<Properties, URL> loadConfigurationFiles(String sResourceName)
            {
            Objects.requireNonNull(sResourceName, "resourceName may not be null");

            Map<Properties, URL> props = new LinkedHashMap<>(4);
            try
                {
                Enumeration<URL> configFiles = Base.getContextClassLoader().getResources(sResourceName);
                while (configFiles.hasMoreElements())
                    {
                    Properties p   = new Properties();
                    URL        url = configFiles.nextElement();
                    try
                        {
                        p.load(url.openStream());
                        props.put(p, url);
                        }
                    catch (IOException e)
                        {
                        throw Base.ensureRuntimeException(e,
                                String.format("Unable to load properties from %s", url.toString()));
                        }
                    }
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e,
                        String.format("Error loading serializer configuration %s from classpath", sResourceName));
                }
            return props;
            }

        // ----- inner class: ThrowingBiConsumer ----------------------------

        /**
         * Similar to {@link BiConsumer}, but this allows throwing a checked exception.
         *
         * @param <T> the type of the first argument to the operation
         * @param <U> the type of the second argument to the operation
         */
        protected interface ThrowingBiConsumer<T, U>
            {
            void accept(T t, U u) throws Exception;
            }

        // ----- constants --------------------------------------------------

        /**
         * The name and expected location of the {@code type-aliases.properties} file.
         */
        protected static final String TYPE_ALIASES_PROPERTIES = "META-INF/type-aliases.properties";

        /**
         * The name and expected location of the {@code package-aliases.properties} file.
         */
        protected static final String PACKAGE_ALIASES_PROPERTIES = "META-INF/package-aliases.properties";
        }

    /**
     * System property to enable/disable the type alias enforcement.  Enabled by default.
     */
    protected static final String JSON_TYPE_ALIAS_ENFORCEMENT = "coherence.json.type.enforcement";
    }
