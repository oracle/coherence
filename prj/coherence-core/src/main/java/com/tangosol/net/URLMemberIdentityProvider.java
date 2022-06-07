/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.common.net.SSLSettings;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.util.Duration;
import com.tangosol.coherence.config.Config;
import com.tangosol.util.Resources;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * A {@link MemberIdentityProvider} that retrieves identity
 * values from URLs, files, or class path resources.
 *
 * @author Jonathan Knight
 * @since 22.06
 */
public class URLMemberIdentityProvider
        implements MemberIdentityProvider
    {
    @Override
    public String getMachineName()
        {
        return load("machine", PROP_MACHINE);
        }

    @Override
    public String getMemberName()
        {
        return load("member", PROP_MEMBER);
        }

    @Override
    public String getRackName()
        {
        return load("rack", PROP_RACK);
        }

    @Override
    public String getRoleName()
        {
        return load("role", PROP_ROLE);
        }

    @Override
    public String getSiteName()
        {
        return load("site", PROP_SITE);
        }

    @Override
    public void setDependencies(ClusterDependencies deps)
        {
        m_dependencies = deps;
        }

    // ----- helper methods -------------------------------------------------

    String load(String sName, String sProperty)
        {
        String sValue = Config.getProperty(sProperty);
        if (sValue != null && !sValue.isBlank())
            {
            try
                {
                URI    uri     = URI.create(sValue);
                String sScheme = uri.getScheme();
                if ("http".equalsIgnoreCase(sScheme) || "https".equalsIgnoreCase(sScheme))
                    {
                    // do http request
                    return doHttpRequest(uri);
                    }

                URL url;
                try
                    {
                    url = uri.toURL();
                    }
                catch (Exception e)
                    {
                    // try as a resource
                    url = Resources.findFileOrResource(sValue, null);
                    if (url == null)
                        {
                        Logger.err("Failed to load " + sName + " name from URL " + sValue);
                        return null;
                        }
                    }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream())))
                    {
                    String sLine = reader.readLine();
                    if (sLine == null || sLine.isBlank())
                        {
                        return null;
                        }
                    return sLine.trim();
                    }
                }
            catch (Throwable t)
                {
                Logger.err("Failed to load " + sName + " name from URL " + sValue, t);
                }
            }

        return null;
        }

    protected String doHttpRequest(URI uri) throws IOException
        {
        Duration                         timeout  = Config.getDuration(PROP_RETRY_TIMEOUT, DURATION_RETRY_TIMEOUT);
        Duration                         period   = Config.getDuration(PROP_RETRY_PERIOD, DURATION_RETRY_PERIOD);
        long                             cMillis  = period.as(Duration.Magnitude.MILLI);
        HttpRequest                      request  = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse.BodyHandler<String> handler  = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        HttpClient                       client   = ensureClient();

        try (Timeout ignored = Timeout.after(timeout.as(Duration.Magnitude.MILLI), TimeUnit.MILLISECONDS))
            {
            HttpResponse<String> response = client.send(request, handler);
            int                  nStatus  = response.statusCode();

            if (nStatus != 200)
                {
                Logger.info("Received " + nStatus + " response from " + uri + " - retry every " + period + " for " + timeout);

                while(nStatus != 200)
                    {
                    Blocking.sleep(cMillis);
                    response = client.send(request, handler);
                    nStatus  = response.statusCode();
                    }
                }

            return response.body();
            }
        catch (InterruptedException e)
            {
            throw Exceptions.ensureRuntimeException(e, "timeout while making request to " + uri);
            }
        }

    protected HttpClient ensureClient()
        {
        if (m_client == null)
            {
            synchronized (this)
                {
                if (m_client == null)
                    {
                    HttpClient.Builder builder = HttpClient.newBuilder();

                    if (m_dependencies != null)
                        {
                        String             sProvider = Config.getProperty(PROP_SOCKET_PROVIDER);
                        if (sProvider != null)
                            {
                            SocketProviderFactory factory     = m_dependencies.getSocketProviderFactory();
                            SocketProvider        provider    = factory.getSocketProvider(sProvider);
                            SSLSettings           sslSettings = factory.getSSLSettings(provider);
                            SSLContext            sslContext  = sslSettings == null ? null : sslSettings.getSSLContext();
                            if (sslContext != null)
                                {
                                builder.sslContext(sslContext);
                                builder.sslParameters(sslContext.getSupportedSSLParameters());
                                }
                            }
                        }

                    Duration timeout = Config.getDuration(PROP_HTTP_TIMEOUT, DURATION_HTTP_TIMEOUT);

                    m_client = builder.followRedirects(HttpClient.Redirect.ALWAYS)
                            .connectTimeout(timeout.asJavaDuration())
                            .build();
                    }
                }
            }
        return m_client;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The system property to use to set the URL to read the machine name from.
     */
    public static final String PROP_MACHINE = "coherence.machine.url";

    /**
     * The system property to use to set the URL to read the member name from.
     */
    public static final String PROP_MEMBER = "coherence.member.url";

    /**
     * The system property to use to set the URL to read the site name from.
     */
    public static final String PROP_SITE = "coherence.site.url";

    /**
     * The system property to use to set the URL to read the rack name from.
     */
    public static final String PROP_RACK = "coherence.rack.url";

    /**
     * The system property to use to set the URL to read the role name from.
     */
    public static final String PROP_ROLE = "coherence.role.url";

    /**
     * The system property to use to set the URL to read the role name from.
     */
    public static final String PROP_SOCKET_PROVIDER = "coherence.url.identity.socket.provider";

    /**
     * The system property to use to set the retry period.
     */
    public static final String PROP_RETRY_PERIOD = "coherence.url.identity.retry.period";

    /**
     * The default retry period.
     */
    public static final Duration DURATION_RETRY_PERIOD = new Duration(1, Duration.Magnitude.SECOND);

    /**
     * The system property to use to set the retry timeout.
     */
    public static final String PROP_RETRY_TIMEOUT = "coherence.url.identity.retry.timeout";

    /**
     * The default retry timeout.
     */
    public static final Duration DURATION_RETRY_TIMEOUT =  new Duration(5, Duration.Magnitude.MINUTE);

    /**
     * The system property to use to set the http connection timeout.
     */
    public static final String PROP_HTTP_TIMEOUT = "coherence.url.identity.http.timeout";

    /**
     * The default http connection timeout.
     */
    public static final Duration DURATION_HTTP_TIMEOUT =  new Duration(1, Duration.Magnitude.MINUTE);

    /**
     * The cluster dependencies.
     */
    protected ClusterDependencies m_dependencies;

    /**
     * The http client used to access http or https URLs.
     */
    protected volatile HttpClient m_client;
    }
