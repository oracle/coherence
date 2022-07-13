/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client;

import com.oracle.coherence.common.base.Classes;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.guides.client.model.TenantMetaData;
import com.oracle.coherence.guides.client.webserver.WebServer;

import com.oracle.coherence.io.json.JsonSerializer;
import com.oracle.coherence.io.json.genson.GenericType;

import com.tangosol.internal.http.RequestRouter;

import com.tangosol.net.Coherence;

import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.util.Resources;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Coherence lifecycle listener to start and the multi-tenant client application.
 */
public class Application
        implements Coherence.LifecycleListener
    {
    public static final JsonSerializer SERIALIZER = new JsonSerializer(null, builder ->
                    {
                    builder.setEnforceTypeAliases(false);
                    return builder.useIndentation(true);
                    }, false);

    private WebServer webServer;

    @Override
    public void onEvent(CoherenceLifecycleEvent event)
        {
        if (event.getType() == CoherenceLifecycleEvent.Type.STARTED)
            {
            // Coherence has started
            start(event.getCoherence());
            }
        else if (event.getType() == CoherenceLifecycleEvent.Type.STOPPING)
            {
            stop(event.getCoherence());
            }
        }

    /**
     * The application entry point.
     */
    public synchronized void start(Coherence coherence)
        {
        if (webServer == null)
            {
            Session session = coherence.getSession();
            NamedMap<String, TenantMetaData> tenants = session.getMap("tenants");

            TenantController tenantController = new TenantController(tenants, SERIALIZER);
            UserController userController = new UserController(tenants, SERIALIZER);

            RequestRouter router = new RequestRouter("/");

            router.addGet("/ready", tenantController::ready);

            router.addGet("/tenants/{tenant}", tenantController::get);
            router.addPost("/tenants", tenantController::create);
            router.addPut("/tenants/{tenant}", tenantController::update);
            router.addDelete("/tenants/{tenant}", tenantController::delete);

            router.addGet("/users/{user}", userController::get);
            router.addPost("/users", userController::create);
            router.addPut("/users/{user}", userController::update);
            router.addDelete("/users/{user}", userController::delete);

            webServer = new WebServer(router);
            coherence.getManagement().register(webServer);
            webServer.start();
            }
        }

    public synchronized void stop(Coherence coherence)
        {
        if (webServer != null)
            {
            coherence.getManagement().unregister(webServer);
            webServer.stop();
            webServer = null;
            }
        }

    /**
     * Load the tenant meta-data from a json file.
     *
     * @return  the tenant mata-data
     */
    protected static Map<String, TenantMetaData> loadTenants(String fileName)
        {
        // find the tenants file on the class path or as a file
        URL url = Resources.findFileOrResource(fileName, Classes.getContextClassLoader());

        // read and deserialize the json file into a map
        try (InputStream in = url.openStream())
            {
            JsonSerializer serializer = new JsonSerializer();

            GenericType<List<TenantMetaData>> type = new GenericType<>(){};
            List<TenantMetaData> list = serializer.underlying().deserialize(in.readAllBytes(), type);

            return list.stream()
                    .collect(Collectors.toMap(TenantMetaData::getTenant, m -> m));
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }
    }
