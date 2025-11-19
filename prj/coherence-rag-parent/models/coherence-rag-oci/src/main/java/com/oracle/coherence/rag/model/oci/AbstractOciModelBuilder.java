/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.oci;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;

import io.helidon.config.ConfigException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;

/**
 * Abstract base class for OCI GenAI models.
 * <p/>
 * This class provides helpers for OCI authentication, supporting multiple
 * authentication mechanisms including OCI config file, manual configuration via
 * properties, and instance principals for compute instances running in OCI.
 * <p/>
 * The authentication mechanism is determined by the availability of configuration:
 * <ol>
 * <li>OCI config file - If oci.config.file is specified, uses config file authentication</li>
 * <li>Manual configuration - If oci.tenant.id is specified, uses simple authentication with individual properties</li>
 * <li>Instance principals - Falls back to instance principals authentication for OCI compute instances</li>
 * </ol>
 * <p/>
 * Configuration properties for config file authentication:
 * <ul>
 * <li>oci.config.file - Path to OCI config file (optional, defaults to ~/.oci/config)</li>
 * <li>oci.config.profile - Profile name within config file (optional, defaults to DEFAULT)</li>
 * </ul>
 * <p/>
 * Configuration properties for manual authentication:
 * <ul>
 * <li>oci.tenant.id - OCI tenant OCID (required for manual auth)</li>
 * <li>oci.user.id - OCI user OCID (required for manual auth)</li>
 * <li>oci.region - OCI region identifier (required for manual auth)</li>
 * <li>oci.auth.fingerprint - API key fingerprint (required for manual auth)</li>
 * <li>oci.auth.key - Path to private key file (required for manual auth)</li>
 * </ul>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
public abstract class AbstractOciModelBuilder
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct model instance.
     *
     * @param config  Eclipse MP configuration
     */
    protected AbstractOciModelBuilder(Config config)
        {
        this.config = config;
        }

    // ---- methods ---------------------------------------------------------

    /**
     * Returns an OCI authentication details provider based on available configuration.
     * <p/>
     * This method attempts authentication in the following order:
     * <ol>
     * <li>Config file authentication if oci.config.file is provided</li>
     * <li>Simple authentication if oci.tenant.id is provided</li>
     * <li>Instance principals authentication as fallback</li>
     * </ol>
     *
     * @return configured AbstractAuthenticationDetailsProvider instance
     *
     * @throws RuntimeException if authentication setup fails
     */
    protected AbstractAuthenticationDetailsProvider authenticationDetailsProvider()
        {
        try
            {
            // try config file first
            String configFile = configFileName();
            if (configFile != null)
                {
                return new ConfigFileAuthenticationDetailsProvider(configFile, configProfile());
                }

            // fall back to config properties, if specified
            Optional<String> tenantId = config.getOptionalValue("oci.tenant.id", String.class);
            if (tenantId.isPresent())
                {
                return SimpleAuthenticationDetailsProvider.builder()
                                           .tenantId(tenantId.get())
                                           .userId(userId())
                                           .fingerprint(fingerprint())
                                           .region(Region.fromRegionCodeOrId(region()))
                                           .privateKeySupplier(privateKeySupplier())
                                           .build();
                }

            // if neither is configured, try instance principals (only works in OCI)
            return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    // ---- configuration accessors ----------------------------------------

    /**
     * Returns the OCI base URL from configuration.
     *
     * @return the base URL for OCI API, or {@code null} if not configured
     */
    protected String baseUrl()
        {
        return config.getOptionalValue("oci.base.url", String.class).orElse(null);
        }

    /**
     * Returns the OCI config file path from configuration.
     *
     * @return the path to OCI config file, or null if not configured
     */
    protected String configFileName()
        {
        return config.getOptionalValue("oci.config.file", String.class).orElse(null);
        }

    /**
     * Returns the OCI config profile from configuration.
     *
     * @return the profile name within config file, or null if not configured
     */
    protected String configProfile()
        {
        return config.getOptionalValue("oci.config.profile", String.class).orElse(null);
        }

    /**
     * Returns the OCI tenant ID from configuration.
     *
     * @return the tenant OCID
     *
     * @throws ConfigException if tenant ID is not configured
     */
    protected String tenantId()
        {
        return config.getOptionalValue("oci.tenant.id", String.class)
                .orElseThrow(() -> new ConfigException("OCI tenant ID is not set. Please set the config property 'oci.tenant.id'"));
        }

    /**
     * Returns the OCI user ID from configuration.
     *
     * @return the user OCID
     *
     * @throws ConfigException if user ID is not configured
     */
    protected String userId()
        {
        return config.getOptionalValue("oci.user.id", String.class)
                .orElseThrow(() -> new ConfigException("OCI user ID is not set. Please set the config property 'oci.user.id'"));
        }

    /**
     * Returns the OCI region from configuration.
     *
     * @return the region identifier
     *
     * @throws ConfigException if region is not configured
     */
    protected String region()
        {
        return config.getOptionalValue("oci.region", String.class)
                .orElseThrow(() -> new ConfigException("OCI region is not set. Please set the config property 'oci.region'"));
        }

    /**
     * Returns the OCI API key fingerprint from configuration.
     *
     * @return the fingerprint of the API key
     *
     * @throws ConfigException if fingerprint is not configured
     */
    protected String fingerprint()
        {
        return config.getOptionalValue("oci.auth.fingerprint", String.class)
                .orElseThrow(() -> new ConfigException("OCI fingerprint is not set. Please set the config property 'oci.auth.fingerprint'"));
        }

    /**
     * Returns the OCI private key file path from configuration.
     *
     * @return the path to private key file
     *
     * @throws ConfigException if private key path is not configured
     */
    protected String privateKey()
        {
        return config.getOptionalValue("oci.auth.key", String.class)
                .orElseThrow(() -> new ConfigException("OCI private key is not set. Please set the config property 'oci.auth.key'"));
        }

    /**
     * Creates a supplier for the private key input stream.
     *
     * @return supplier that provides an InputStream for the private key file
     */
    private Supplier<InputStream> privateKeySupplier()
        {
        return () ->
            {
            try
                {
                return new FileInputStream(privateKey());
                }
            catch (FileNotFoundException e)
                {
                throw new RuntimeException(e);
                }
            };
        }

    // ---- data members ----------------------------------------------------

    /**
     * MicroProfile Config instance for reading configuration properties.
     */
    Config config;
    }
