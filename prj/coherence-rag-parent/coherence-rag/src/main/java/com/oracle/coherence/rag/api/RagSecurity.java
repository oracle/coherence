/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.rag.ModelProvider;
import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.rag.model.ModelName;
import com.oracle.coherence.rag.util.CdiHelper;
import com.tangosol.net.CacheFactory;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Shared authorization and allowlist policy for RAG REST endpoints.
 *
 * @author Aleks Seovic  2026.04.28
 * @since 26.04
 */
public final class RagSecurity
    {
    private RagSecurity()
        {
        }

    /**
     * Require an authenticated caller.
     *
     * @param context  the request security context
     * @param route    the RAG route label
     *
     * @return a rejection response, or {@code null} when the caller is authenticated
     */
    public static Response requireAuthenticated(SecurityContext context, String route)
        {
        if (context == null || context.getAuthenticationScheme() == null || isAnonymous(context.getUserPrincipal()))
            {
            warn(route, GATE_ADMIN_AUTH, PRINCIPAL_ANONYMOUS, REASON_UNAUTHENTICATED);
            return Response.status(Response.Status.UNAUTHORIZED).build();
            }

        if (SecurityContext.BASIC_AUTH.equals(context.getAuthenticationScheme()))
            {
            warn(route, GATE_ADMIN_AUTH, principal(context), REASON_BASIC_AUTH_NOT_SUPPORTED);
            return Response.status(Response.Status.UNAUTHORIZED).build();
            }

        return null;
        }

    /**
     * Require an authenticated caller in the configured RAG admin role.
     *
     * @param context  the request security context
     * @param route    the RAG route label
     *
     * @return a rejection response, or {@code null} when the caller is an admin
     */
    public static Response requireAdmin(SecurityContext context, String route)
        {
        Response response = requireAuthenticated(context, route);
        if (response != null)
            {
            return response;
            }

        String sRole = com.tangosol.coherence.config.Config.getProperty(PROP_ADMIN_ROLE, DEFAULT_ADMIN_ROLE);
        boolean fAdmin;
        try
            {
            fAdmin = context.isUserInRole(sRole);
            }
        catch (RuntimeException e)
            {
            warn(route, GATE_ADMIN_AUTH, PRINCIPAL_ANONYMOUS, REASON_UNAUTHENTICATED);
            return Response.status(Response.Status.UNAUTHORIZED).build();
            }

        if (!fAdmin)
            {
            warn(route, GATE_ADMIN_AUTH, principal(context), REASON_NOT_ADMIN);
            return Response.status(Response.Status.FORBIDDEN).build();
            }

        return null;
        }

    /**
     * Return {@code true} when a configuration property should not be read back.
     *
     * @param property  the property name
     *
     * @return {@code true} if the property is sensitive
     */
    public static boolean isSensitiveConfigRead(String property)
        {
        return isSensitiveProperty(property) || isPolicyProperty(property);
        }

    /**
     * Validate a REST configuration write.
     *
     * @param property  the property name
     * @param value     the new property value
     */
    public static void validateConfigWrite(String property, String value)
        {
        if (property == null || property.isBlank())
            {
            throw new PolicyViolation(GATE_CONFIG_PROPERTY_ALLOWLIST, REASON_INVALID_REQUEST);
            }

        if (isPolicyProperty(property))
            {
            throw new PolicyViolation(GATE_CONFIG_PROPERTY_ALLOWLIST, REASON_POLICY_PROPERTY_NOT_WRITABLE);
            }

        if (isSensitiveProperty(property) || !allowedProperties().contains(property))
            {
            throw new PolicyViolation(GATE_CONFIG_PROPERTY_ALLOWLIST, REASON_PROPERTY_NOT_ALLOWED);
            }

        if (MODEL_PROPERTIES.contains(property))
            {
            validateModelDownload(value, GATE_DOWNLOAD_CONFIG_VALUE);
            }
        }

    /**
     * Validate model config JSON for Slice A sensitive destination fields.
     *
     * @param json  the JSON document
     */
    public static void validateModelConfigJson(String json)
        {
        if (json == null)
            {
            throw new PolicyViolation(GATE_MODEL_CONFIG_CONTENT_ALLOWLIST, REASON_INVALID_REQUEST);
            }

        try (var reader = Json.createReader(new StringReader(json)))
            {
            rejectSensitiveField(reader.readValue());
            }
        catch (PolicyViolation e)
            {
            throw e;
            }
        catch (RuntimeException e)
            {
            throw new PolicyViolation(GATE_MODEL_CONFIG_CONTENT_ALLOWLIST, REASON_INVALID_REQUEST);
            }
        }

    /**
     * Validate a store configuration mutation.
     *
     * @param config  the store configuration
     */
    public static void validateStoreConfig(StoreConfig config)
        {
        if (config == null)
            {
            throw new PolicyViolation(GATE_STORE_MODEL_PROVIDER_ALLOWLIST, REASON_INVALID_REQUEST);
            }

        validateModelDownload(config.getEmbeddingModel(), GATE_DOWNLOAD_STORE_CONFIG);
        }

    /**
     * Validate whether a model selector may trigger a HuggingFace download.
     *
     * @param sModel  the model selector
     * @param sGate   the rejection gate
     */
    public static void validateModelDownload(String sModel, String sGate)
        {
        if (sModel == null || sModel.isBlank())
            {
            return;
            }

        ModelName name;
        try
            {
            name = ModelName.of(sModel);
            name.name();
            }
        catch (RuntimeException e)
            {
            throw new PolicyViolation(sGate, REASON_DOWNLOAD_NOT_ALLOWLISTED, boundedValue(sModel));
            }

        String sProvider = name.provider();
        if ("-".equals(sProvider) || hasModelProvider(sProvider))
            {
            return;
            }

        Set<String> setAllowed = allowedHuggingFaceModels();
        if (setAllowed.isEmpty())
            {
            if (isProductionMode())
                {
                throw new PolicyViolation(sGate, REASON_PROD_MODE_EMPTY_ALLOWLIST, boundedValue(name.fullName()));
                }

            if (s_fWarnedUnrestrictedDownloads.compareAndSet(false, true))
                {
                Logger.warn("RAG HuggingFace model download allowlist is empty in dev mode; downloads are unrestricted for this process");
                }
            return;
            }

        if (!isModelAllowed(name, setAllowed))
            {
            throw new PolicyViolation(sGate, REASON_DOWNLOAD_NOT_ALLOWLISTED, boundedValue(name.fullName()));
            }
        }

    /**
     * Validate every URI in an import request before publishing any of them.
     *
     * @param uris  the submitted URI strings
     */
    public static void validateImportUris(Iterable<String> uris)
        {
        if (uris == null)
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_INVALID_REQUEST);
            }

        for (String sUri : uris)
            {
            validateImportUri(sUri);
            }
        }

    /**
     * Validate a single import URI.
     *
     * @param sUri  the URI string
     */
    public static void validateImportUri(String sUri)
        {
        if (sUri == null || sUri.isBlank())
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_INVALID_REQUEST);
            }

        URI uri;
        try
            {
            uri = URI.create(sUri);
            }
        catch (IllegalArgumentException e)
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_INVALID_REQUEST);
            }

        String sScheme = normalize(uri.getScheme());
        if (sScheme == null || !allowedSchemes().contains(sScheme))
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_SCHEME_NOT_ALLOWED);
            }

        switch (sScheme)
            {
            case SCHEME_FILE:
                validateFileUri(uri);
                break;
            case SCHEME_HTTP:
            case SCHEME_HTTPS:
                validateHttpUri(uri);
                break;
            case SCHEME_S3:
                validateHostLocation(uri, PROP_IMPORT_S3_ALLOWED_BUCKETS);
                break;
            case SCHEME_AZURE_BLOB:
                validateHostLocation(uri, PROP_IMPORT_AZURE_ALLOWED_CONTAINERS);
                break;
            case SCHEME_GCS:
                validateHostLocation(uri, PROP_IMPORT_GCS_ALLOWED_BUCKETS);
                break;
            case SCHEME_OCI_OS:
                validateOciLocation(uri);
                break;
            default:
                throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_SCHEME_NOT_ALLOWED);
            }
        }

    /**
     * Open a validated HTTP(S) connection, revalidating every redirect target.
     *
     * @param uri  the URI to open
     *
     * @return the final HTTP connection
     *
     * @throws IOException if a connection cannot be opened
     */
    public static HttpURLConnection openValidatedHttpConnection(URI uri)
            throws IOException
        {
        URI uriCurrent = uri;
        for (int i = 0; i < MAX_REDIRECTS; i++)
            {
            validateHttpUri(uriCurrent);

            HttpURLConnection connection = s_connectionFactory.open(uriCurrent);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(connectTimeoutMillis());
            connection.setReadTimeout(readTimeoutMillis());

            int nStatus = connection.getResponseCode();
            if (!isRedirect(nStatus))
                {
                return connection;
                }

            String sLocation = connection.getHeaderField("Location");
            connection.disconnect();
            if (sLocation == null || sLocation.isBlank())
                {
                throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_INVALID_REQUEST);
                }

            uriCurrent = uriCurrent.resolve(sLocation);
            }

        throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_INVALID_REQUEST);
        }

    /**
     * Log a bounded RAG REST rejection warning.
     *
     * @param route      the route enum value
     * @param gate       the gate enum value
     * @param principal  the principal field
     * @param reason     the reason enum value
     */
    public static void warn(String route, String gate, String principal, String reason)
        {
        warn(route, gate, principal, reason, null);
        }

    /**
     * Log a bounded RAG REST rejection warning.
     *
     * @param route       the route enum value
     * @param gate        the gate enum value
     * @param principal   the principal field
     * @param reason      the reason enum value
     * @param deniedModel optional denied model selector
     */
    public static void warn(String route, String gate, String principal, String reason, String deniedModel)
        {
        String sMessage = "Rejected RAG REST request: route=%s, gate=%s, principal=%s, reason=%s"
                .formatted(route, gate, boundedPrincipal(principal), reason);
        if (deniedModel != null)
            {
            sMessage += ", denied-model=" + deniedModel;
            }
        Logger.warn(sMessage);
        }

    /**
     * Return the configured HTTP import body byte cap.
     *
     * @return the byte cap
     */
    public static long maxImportBytes()
        {
        return com.tangosol.coherence.config.Config.getLong(PROP_IMPORT_MAX_BYTES, DEFAULT_IMPORT_MAX_BYTES);
        }

    /**
     * Return a bounded principal name for audit logging.
     *
     * @param context  the security context
     *
     * @return the bounded principal name
     */
    public static String principal(SecurityContext context)
        {
        return context == null || context.getUserPrincipal() == null
               ? PRINCIPAL_ANONYMOUS
               : boundedPrincipal(context.getUserPrincipal().getName());
        }

    /**
     * Override the DNS resolver for tests.
     *
     * @param resolver  the resolver to use
     *
     * @return the previous resolver
     */
    static AddressResolver setAddressResolverForTesting(AddressResolver resolver)
        {
        AddressResolver previous = s_resolver;
        s_resolver = resolver == null ? InetAddress::getAllByName : resolver;
        return previous;
        }

    /**
     * Override the HTTP connection factory for tests.
     *
     * @param factory  the connection factory to use
     *
     * @return the previous factory
     */
    static HttpConnectionFactory setHttpConnectionFactoryForTesting(HttpConnectionFactory factory)
        {
        HttpConnectionFactory previous = s_connectionFactory;
        s_connectionFactory = factory == null
                ? uri -> (HttpURLConnection) uri.toURL().openConnection()
                : factory;
        return previous;
        }

    /**
     * Reset cached HuggingFace download allowlist state for tests.
     */
    static void resetHuggingFaceAllowlistForTesting()
        {
        s_allowedHuggingFaceModels.set(null);
        s_fWarnedUnrestrictedDownloads.set(false);
        s_cInvalidHuggingFaceAllowlistWarnings.set(0);
        }

    /**
     * Return the number of invalid HuggingFace allowlist entries logged since
     * the last test reset.
     *
     * @return the warning count
     */
    static int invalidHuggingFaceAllowlistWarningCountForTesting()
        {
        return s_cInvalidHuggingFaceAllowlistWarnings.get();
        }

    private static void validateFileUri(URI uri)
        {
        Path pathTarget;
        try
            {
            pathTarget = Path.of(uri).toRealPath();
            }
        catch (Exception e)
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_PATH_NOT_ALLOWED);
            }

        for (String sRoot : configuredFileRoots())
            {
            try
                {
                Path pathRoot = Path.of(sRoot).toRealPath();
                if (pathTarget.startsWith(pathRoot))
                    {
                    return;
                    }
                }
            catch (IOException ignored)
                {
                }
            }

        throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_PATH_NOT_ALLOWED);
        }

    private static void validateHttpUri(URI uri)
        {
        String sHost = normalize(uri.getHost());
        if (sHost == null)
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_HOST_NOT_ALLOWED);
            }

        Set<String> setHost = configuredValues(PROP_IMPORT_HTTP_ALLOWED_HOSTS);
        if (!setHost.contains(sHost))
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_HOST_NOT_ALLOWED);
            }

        InetAddress[] aAddress;
        try
            {
            aAddress = s_resolver.resolve(sHost);
            }
        catch (UnknownHostException e)
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_DNS_ADDRESS_NOT_ALLOWED);
            }

        if (aAddress.length == 0)
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_DNS_ADDRESS_NOT_ALLOWED);
            }

        boolean fAllowPrivate = com.tangosol.coherence.config.Config.getBoolean(PROP_IMPORT_HTTP_ALLOW_PRIVATE, false);
        for (InetAddress address : aAddress)
            {
            validateAddress(address, fAllowPrivate);
            }
        }

    private static void validateAddress(InetAddress address, boolean fAllowPrivate)
        {
        if (isMetadataAddress(address))
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_METADATA_ADDRESS_NOT_ALLOWED);
            }

        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress())
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_PRIVATE_ADDRESS_NOT_ALLOWED);
            }

        if (!fAllowPrivate && address.isSiteLocalAddress())
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_PRIVATE_ADDRESS_NOT_ALLOWED);
            }
        }

    private static boolean isMetadataAddress(InetAddress address)
        {
        byte[] ab = address.getAddress();
        return ab.length == 4
               && (ab[0] & 0xFF) == 169
               && (ab[1] & 0xFF) == 254
               && (ab[2] & 0xFF) == 169
               && (ab[3] & 0xFF) == 254;
        }

    private static void validateHostLocation(URI uri, String sProperty)
        {
        String sHost = normalize(uri.getHost());
        if (sHost == null || !configuredValues(sProperty).contains(sHost))
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_PROVIDER_LOCATION_NOT_ALLOWED);
            }
        }

    private static void validateOciLocation(URI uri)
        {
        String sNamespace = normalize(uri.getHost());
        String sPath      = uri.getPath();
        if (sNamespace == null || sPath == null || sPath.length() <= 1)
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_PROVIDER_LOCATION_NOT_ALLOWED);
            }

        String[] asPart = sPath.substring(1).split("/", 2);
        if (asPart.length == 0 || asPart[0].isBlank())
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_PROVIDER_LOCATION_NOT_ALLOWED);
            }

        String sLocation = sNamespace + "/" + normalize(asPart[0]);
        if (!configuredValues(PROP_IMPORT_OCI_ALLOWED_LOCATIONS).contains(sLocation))
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_PROVIDER_LOCATION_NOT_ALLOWED);
            }
        }

    private static boolean hasModelProvider(String sProvider)
        {
        try
            {
            return CdiHelper.getNamedBean(ModelProvider.class, sProvider) != null;
            }
        catch (RuntimeException e)
            {
            return false;
            }
        }

    private static boolean isModelAllowed(ModelName name, Set<String> setAllowed)
        {
        String sFullName = normalize(name.fullName());
        for (String sAllowed : setAllowed)
            {
            if (sAllowed.equals(sFullName))
                {
                return true;
                }

            if (sAllowed.endsWith("/*") && sFullName.startsWith(sAllowed.substring(0, sAllowed.length() - 1)))
                {
                return true;
                }
            }
        return false;
        }

    private static boolean isProductionMode()
        {
        String sMode = normalize(com.tangosol.coherence.config.Config.getProperty("coherence.mode"));
        if (sMode == null)
            {
            sMode = normalize(CacheFactory.getLicenseMode());
            }
        return "prod".equals(sMode) || "production".equals(sMode);
        }

    private static int connectTimeoutMillis()
        {
        return checkedMillis(com.tangosol.coherence.config.Config.getLong(
                PROP_IMPORT_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT_MILLIS));
        }

    private static int readTimeoutMillis()
        {
        return checkedMillis(com.tangosol.coherence.config.Config.getLong(
                PROP_IMPORT_READ_TIMEOUT, DEFAULT_READ_TIMEOUT_MILLIS));
        }

    private static int checkedMillis(long cMillis)
        {
        if (cMillis < 0 || cMillis > Integer.MAX_VALUE)
            {
            throw new PolicyViolation(GATE_IMPORT_URI_ALLOWLIST, REASON_INVALID_REQUEST);
            }
        return (int) cMillis;
        }

    private static void rejectSensitiveField(JsonValue value)
        {
        if (value instanceof JsonObject object)
            {
            for (String sKey : object.keySet())
                {
                String sNormalized = sKey.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(".", "");
                if (SENSITIVE_JSON_FIELDS.contains(sNormalized))
                    {
                    throw new PolicyViolation(GATE_MODEL_CONFIG_CONTENT_ALLOWLIST, REASON_SENSITIVE_MODEL_CONFIG_FIELD);
                    }
                rejectSensitiveField(object.get(sKey));
                }
            }
        else if (value instanceof JsonArray array)
            {
            array.forEach(RagSecurity::rejectSensitiveField);
            }
        else if (value instanceof JsonString)
            {
            // string values are never inspected or logged
            }
        }

    private static boolean isSensitiveProperty(String property)
        {
        String sProperty = normalize(property);
        if (sProperty == null)
            {
            return true;
            }

        return sProperty.contains("api.key")
               || sProperty.contains("base.url")
               || sProperty.contains("endpoint")
               || sProperty.contains(".auth.")
               || sProperty.endsWith(".auth")
               || sProperty.startsWith("auth.")
               || sProperty.contains("password")
               || sProperty.contains("secret")
               || sProperty.contains("token")
               || sProperty.startsWith("server.")
               || sProperty.startsWith("helidon.")
               || sProperty.startsWith("mp.jwt.")
               || sProperty.startsWith("javax.net.ssl.")
               || sProperty.startsWith("coherence.security.");
        }

    private static boolean isPolicyProperty(String property)
        {
        String sProperty = normalize(property);
        return sProperty != null
               && (sProperty.startsWith("coherence.rag.security.")
                   || sProperty.startsWith("coherence.rag.config.write.")
                   || sProperty.startsWith("coherence.rag.import.")
                   || sProperty.startsWith("coherence.rag.store."));
        }

    private static Set<String> allowedProperties()
        {
        return configuredValues(PROP_CONFIG_WRITE_ALLOWED_PROPERTIES, DEFAULT_ALLOWED_PROPERTIES);
        }

    private static Set<String> allowedSchemes()
        {
        return configuredValues(PROP_IMPORT_ALLOWED_SCHEMES);
        }

    private static Set<String> allowedHuggingFaceModels()
        {
        List<String> listRaw = configuredHuggingFaceRawValues();
        String sRaw = String.join("\u0000", listRaw);

        AllowedModels allowedModels = s_allowedHuggingFaceModels.get();
        if (allowedModels != null && allowedModels.raw().equals(sRaw))
            {
            return allowedModels.models();
            }

        Set<String> setAllowed = validateAllowedHuggingFaceModels(listRaw);
        s_allowedHuggingFaceModels.set(new AllowedModels(sRaw, setAllowed));
        return setAllowed;
        }

    private static Set<String> configuredValues(String sProperty)
        {
        return configuredValues(sProperty, "");
        }

    private static Set<String> configuredValues(String sProperty, String sDefault)
        {
        return configuredRawValues(sProperty, sDefault)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(RagSecurity::normalize)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        }

    private static Set<String> configuredFileRoots()
        {
        return configuredRawValues(PROP_IMPORT_FILE_ALLOWED_ROOTS, "")
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        }

    private static java.util.stream.Stream<String> configuredRawValues(String sProperty, String sDefault)
        {
        try
            {
            var listValue = ConfigProvider.getConfig().getOptionalValues(sProperty, String.class);
            if (listValue.isPresent())
                {
                return listValue.get().stream();
                }
            }
        catch (RuntimeException ignored)
            {
            }

        String sValue = com.tangosol.coherence.config.Config.getProperty(sProperty, sDefault);
        return Arrays.stream(sValue.split(","));
        }

    private static List<String> configuredHuggingFaceRawValues()
        {
        String sValue = com.tangosol.coherence.config.Config.getProperty(PROP_HUGGINGFACE_ALLOWED_MODELS);
        if (sValue != null)
            {
            return List.of(sValue);
            }

        try
            {
            var listValue = ConfigProvider.getConfig().getOptionalValues(PROP_HUGGINGFACE_ALLOWED_MODELS, String.class);
            if (listValue.isPresent())
                {
                return List.copyOf(listValue.get());
                }
            }
        catch (RuntimeException ignored)
            {
            }

        return List.of("");
        }

    private static String normalize(String s)
        {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
        }

    private static Set<String> validateAllowedHuggingFaceModels(List<String> listRaw)
        {
        List<String> listEntry = listRaw.stream()
                .flatMap(s -> Arrays.stream(s.split(",", -1)))
                .map(String::trim)
                .toList();

        boolean fAllBlank = listEntry.stream().allMatch(String::isBlank);
        if (fAllBlank)
            {
            return Set.of();
            }

        return listEntry.stream()
                .map(RagSecurity::validateAllowedHuggingFaceModel)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        }

    private static String validateAllowedHuggingFaceModel(String sModel)
        {
        String sValue = normalize(sModel);
        if (sValue != null
            && (PATTERN_HUGGINGFACE_ALLOWED_EXACT.matcher(sValue).matches()
                || PATTERN_HUGGINGFACE_ALLOWED_OWNER_WILDCARD.matcher(sValue).matches()))
            {
            return sValue;
            }

        warnInvalidHuggingFaceAllowlistEntry(sModel);
        return null;
        }

    private static void warnInvalidHuggingFaceAllowlistEntry(String sEntry)
        {
        s_cInvalidHuggingFaceAllowlistWarnings.incrementAndGet();
        Logger.warn("Ignored RAG HuggingFace allowlist entry: property=%s, reason=%s, entry=%s"
                .formatted(PROP_HUGGINGFACE_ALLOWED_MODELS, REASON_HUGGINGFACE_ALLOWLIST_ENTRY_INVALID,
                        boundedValue(sEntry == null ? "" : sEntry)));
        }

    private static boolean isAnonymous(Principal principal)
        {
        if (principal == null)
            {
            return true;
            }

        String sName = normalize(principal.getName());
        return sName == null || sName.isBlank() || PRINCIPAL_ANONYMOUS.equals(sName);
        }

    private static String boundedPrincipal(String sPrincipal)
        {
        return boundedValue(sPrincipal == null ? PRINCIPAL_UNKNOWN : sPrincipal);
        }

    private static String boundedValue(String sValue)
        {
        StringBuilder sb = new StringBuilder(sValue.length());
        for (int i = 0; i < sValue.length(); i++)
            {
            char ch = sValue.charAt(i);
            sb.append(ch >= 32 && ch <= 126 ? ch : '?');
            }

        if (sb.length() <= MAX_PRINCIPAL_LENGTH)
            {
            return sb.toString();
            }

        return sb.substring(0, MAX_PRINCIPAL_LENGTH - TRUNCATION_MARKER.length()) + TRUNCATION_MARKER;
        }

    private static boolean isRedirect(int nStatus)
        {
        return nStatus == HttpURLConnection.HTTP_MOVED_PERM
               || nStatus == HttpURLConnection.HTTP_MOVED_TEMP
               || nStatus == HttpURLConnection.HTTP_SEE_OTHER
               || nStatus == 307
               || nStatus == 308;
        }

    // ---- inner interface: AddressResolver -------------------------------

    /**
     * DNS resolver abstraction for import URI validation tests.
     */
    interface AddressResolver
        {
        /**
         * Resolve a host name to addresses.
         *
         * @param host  the host name
         *
         * @return the resolved addresses
         *
         * @throws UnknownHostException if the host cannot be resolved
         */
        InetAddress[] resolve(String host) throws UnknownHostException;
        }

    // ---- inner interface: HttpConnectionFactory -------------------------

    /**
     * HTTP connection factory abstraction for import URI validation tests.
     */
    interface HttpConnectionFactory
        {
        /**
         * Open a connection to the specified URI.
         *
         * @param uri  the URI to open
         *
         * @return the HTTP connection
         *
         * @throws IOException if the connection cannot be opened
         */
        HttpURLConnection open(URI uri) throws IOException;
        }

    // ---- inner class: PolicyViolation -----------------------------------

    /**
     * Policy rejection with bounded audit fields.
     */
    public static final class PolicyViolation
            extends RuntimeException
        {
        public PolicyViolation(String gate, String reason)
            {
            this(gate, reason, null);
            }

        public PolicyViolation(String gate, String reason, String deniedModel)
            {
            super(reason);
            this.gate        = gate;
            this.reason      = reason;
            this.deniedModel = deniedModel;
            }

        /**
         * Return the gate that rejected the request.
         *
         * @return the gate enum value
         */
        public String gate()
            {
            return gate;
            }

        /**
         * Return the rejection reason.
         *
         * @return the reason enum value
         */
        public String reason()
            {
            return reason;
            }

        /**
         * Return the denied model selector, when applicable.
         *
         * @return the bounded model selector, or {@code null}
         */
        public String deniedModel()
            {
            return deniedModel;
            }

        private final String gate;
        private final String reason;
        private final String deniedModel;
        }

    // ---- constants -------------------------------------------------------

    public static final String ROUTE_CONFIG_READ     = "config-read";
    public static final String ROUTE_CONFIG_WRITE    = "config-write";
    public static final String ROUTE_MODEL_CONFIG    = "model-config";
    public static final String ROUTE_STORE_CONFIG    = "store-config";
    public static final String ROUTE_DOCUMENT_IMPORT = "document-import";
    public static final String ROUTE_CHAT_REQUEST    = "chat-request";
    public static final String ROUTE_SEARCH_REQUEST  = "search-request";

    public static final String GATE_ADMIN_AUTH                     = "admin-auth";
    public static final String GATE_CONFIG_PROPERTY_ALLOWLIST      = "config-property-allowlist";
    public static final String GATE_CONFIG_VALUE_ALLOWLIST         = "config-value-allowlist";
    public static final String GATE_CONFIG_READ_REDACTION          = "config-read-redaction";
    public static final String GATE_MODEL_CONFIG_CONTENT_ALLOWLIST = "model-config-content-allowlist";
    public static final String GATE_STORE_MODEL_PROVIDER_ALLOWLIST = "store-model-provider-allowlist";
    public static final String GATE_IMPORT_URI_ALLOWLIST           = "import-uri-allowlist";
    public static final String GATE_DOWNLOAD_CONFIG_VALUE          = "download-config-value";
    public static final String GATE_DOWNLOAD_STORE_CONFIG          = "download-store-config";
    public static final String GATE_DOWNLOAD_CHAT_REQUEST          = "download-chat-request";
    public static final String GATE_DOWNLOAD_SEARCH_REQUEST        = "download-search-request";

    public static final String REASON_UNAUTHENTICATED                 = "unauthenticated";
    public static final String REASON_NOT_ADMIN                       = "not-admin";
    public static final String REASON_BASIC_AUTH_NOT_SUPPORTED        = "basic-auth-not-supported";
    public static final String REASON_INVALID_REQUEST                 = "invalid-request";
    public static final String REASON_SENSITIVE_READ_DENIED           = "sensitive-read-denied";
    public static final String REASON_PROPERTY_NOT_ALLOWED            = "property-not-allowed";
    public static final String REASON_POLICY_PROPERTY_NOT_WRITABLE    = "policy-namespace-write-not-allowed";
    public static final String REASON_SENSITIVE_MODEL_CONFIG_FIELD    = "sensitive-model-config-field";
    public static final String REASON_SCHEME_NOT_ALLOWED              = "scheme-not-allowed";
    public static final String REASON_HOST_NOT_ALLOWED                = "host-not-allowed";
    public static final String REASON_PATH_NOT_ALLOWED                = "path-not-allowed";
    public static final String REASON_DNS_ADDRESS_NOT_ALLOWED         = "dns-address-not-allowed";
    public static final String REASON_PRIVATE_ADDRESS_NOT_ALLOWED     = "private-address-not-allowed";
    public static final String REASON_METADATA_ADDRESS_NOT_ALLOWED    = "metadata-address-not-allowed";
    public static final String REASON_PROVIDER_LOCATION_NOT_ALLOWED   = "provider-location-not-allowed";
    public static final String REASON_DOWNLOAD_NOT_ALLOWLISTED        = "download-not-allowlisted";
    public static final String REASON_PROD_MODE_EMPTY_ALLOWLIST       = "prod-mode-empty-allowlist";
    public static final String REASON_HUGGINGFACE_ALLOWLIST_ENTRY_INVALID = "huggingface-allowlist-entry-invalid";
    public static final String REASON_IMPORT_BODY_EXCEEDS_CAP         = "import-body-exceeds-cap";

    public static final String PROP_ADMIN_ROLE                           = "coherence.rag.security.admin-role";
    public static final String PROP_CONFIG_WRITE_ALLOWED_PROPERTIES      = "coherence.rag.config.write.allowed-properties";
    public static final String PROP_HUGGINGFACE_ALLOWED_MODELS           = "coherence.rag.security.huggingface.allowed-models";
    public static final String PROP_IMPORT_ALLOWED_SCHEMES               = "coherence.rag.import.allowed-schemes";
    public static final String PROP_IMPORT_FILE_ALLOWED_ROOTS            = "coherence.rag.import.file.allowed-roots";
    public static final String PROP_IMPORT_HTTP_ALLOWED_HOSTS            = "coherence.rag.import.http.allowed-hosts";
    public static final String PROP_IMPORT_HTTP_ALLOW_PRIVATE            = "coherence.rag.import.http.allow-private-addresses";
    public static final String PROP_IMPORT_CONNECT_TIMEOUT               = "coherence.rag.import.connect-timeout-ms";
    public static final String PROP_IMPORT_READ_TIMEOUT                  = "coherence.rag.import.read-timeout-ms";
    public static final String PROP_IMPORT_MAX_BYTES                     = "coherence.rag.import.max-bytes";
    public static final String PROP_IMPORT_S3_ALLOWED_BUCKETS            = "coherence.rag.import.s3.allowed-buckets";
    public static final String PROP_IMPORT_AZURE_ALLOWED_CONTAINERS      = "coherence.rag.import.azure.blob.allowed-containers";
    public static final String PROP_IMPORT_GCS_ALLOWED_BUCKETS           = "coherence.rag.import.gcs.allowed-buckets";
    public static final String PROP_IMPORT_OCI_ALLOWED_LOCATIONS         = "coherence.rag.import.oci.os.allowed-locations";

    private static final String DEFAULT_ADMIN_ROLE         = "admin";
    private static final String DEFAULT_ALLOWED_PROPERTIES = "model.embedding,model.chat,model.scoring,coherence.rag.default.parser";

    private static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000L;
    private static final long DEFAULT_READ_TIMEOUT_MILLIS    = 30_000L;
    private static final long DEFAULT_IMPORT_MAX_BYTES       = 52_428_800L;

    private static final String SCHEME_FILE       = "file";
    private static final String SCHEME_HTTP       = "http";
    private static final String SCHEME_HTTPS      = "https";
    private static final String SCHEME_S3         = "s3";
    private static final String SCHEME_AZURE_BLOB = "azure.blob";
    private static final String SCHEME_GCS        = "gcs";
    private static final String SCHEME_OCI_OS     = "oci.os";

    private static final Set<String> MODEL_PROPERTIES = Set.of("model.embedding", "model.scoring");
    private static final Set<String> SENSITIVE_JSON_FIELDS = Set.of(
            "baseurl", "apikey", "endpoint", "auth", "password", "secret", "token");

    private static final Pattern PATTERN_HUGGINGFACE_ALLOWED_EXACT =
            Pattern.compile("^[^/*\\s]+/[^/*\\s]+$");
    private static final Pattern PATTERN_HUGGINGFACE_ALLOWED_OWNER_WILDCARD =
            Pattern.compile("^[^/*\\s]+/\\*$");

    private static final String PRINCIPAL_ANONYMOUS = "anonymous";
    private static final String PRINCIPAL_UNKNOWN   = "unknown";
    private static final String TRUNCATION_MARKER   = "...";

    private static final int MAX_PRINCIPAL_LENGTH = 256;
    private static final int MAX_REDIRECTS        = 5;

    private static volatile AddressResolver s_resolver = InetAddress::getAllByName;

    private static volatile HttpConnectionFactory s_connectionFactory =
            uri -> (HttpURLConnection) uri.toURL().openConnection();

    private static final AtomicBoolean s_fWarnedUnrestrictedDownloads = new AtomicBoolean();
    private static final AtomicReference<AllowedModels> s_allowedHuggingFaceModels = new AtomicReference<>();
    private static final AtomicInteger s_cInvalidHuggingFaceAllowlistWarnings = new AtomicInteger();

    private record AllowedModels(String raw, Set<String> models)
        {
        }
    }
