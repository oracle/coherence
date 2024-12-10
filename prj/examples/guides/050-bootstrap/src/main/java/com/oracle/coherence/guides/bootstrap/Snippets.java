/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.bootstrap;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Various snippets of code showing how to use the bootstrap API.
 */
public class Snippets {

    public void getDefaultCoherenceInstance() {
        // # tag::getCoherence[]
        Coherence coherence = Coherence.getInstance();
        // # end::getCoherence[]
    }

    public void ensureRunning() throws Exception {
        // # tag::ensureRunning[]
        Coherence coherence = Coherence.getInstance()
                                       .whenStarted()
                                       .get(5, TimeUnit.MINUTES);
        // # end::ensureRunning[]
    }

    public void getDefaultCoherenceInstanceByName() {
        // # tag::getCoherenceByName[]
        Coherence coherence = Coherence.getInstance(Coherence.DEFAULT_NAME);
        // # end::getCoherenceByName[]
    }

    public void getDefaultSession() {
        // # tag::getDefaultSession[]
        Coherence                coherence = Coherence.getInstance();
        Session                  session   = coherence.getSession();
        NamedMap<String, String> map       = session.getMap("test");
        // # end::getDefaultSession[]
    }

    public void getNamedSession() {
        // # tag::getNamedSession[]
        Coherence                coherence = Coherence.getInstance();
        Session                  session   = coherence.getSession("foo");
        NamedMap<String, String> map       = session.getMap("test");
        // # end::getNamedSession[]
    }

    public void findNamedSession() {
        // # tag::findNamedSession[]
        Optional<Session> optional = Coherence.findSession("foo");
        if (optional.isPresent()) {
            Session                  session = optional.get();
            NamedMap<String, String> map     = session.getMap("test");
        }
        // # end::findNamedSession[]
    }

    public void simpleClusterMember() {
        // # tag::simple[]
        Coherence coherence = Coherence.clusterMember();
        coherence.start();
        // # end::simple[]
    }

    public void simpleClusterMemberWait() throws Exception {
        // # tag::simpleWait[]
        Coherence coherence = Coherence.clusterMember()
                                       .start()
                                       .get(5, TimeUnit.MINUTES);
        // # end::simpleWait[]
    }

    public void simpleClusterMemberWaitHealthy() throws Exception {
        // # tag::simpleWaitReady[]
        Coherence coherence = Coherence.clusterMember()
                                       .start()
                                       .get(5, TimeUnit.MINUTES);

        coherence.getManagement().allHealthChecksReady();
        // # end::simpleWaitReady[]
    }

    public void configureClusterMember() {
        // # tag::configureClusterMember[]
        SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
                                                                        .named("foo")
                                                                        .withScopeName("Foo")
                                                                        .withConfigUri("foo-cache-config.xml")
                                                                        .build();

        CoherenceConfiguration config = CoherenceConfiguration.builder()
                                                              .withSession(sessionConfiguration)
                                                              .withSession(SessionConfiguration.defaultSession())
                                                              .build();

        Coherence coherence = Coherence.clusterMember(config)
                                       .start()
                                       .join();
        // # end::configureClusterMember[]
    }

    public void simpleClient() {
        // # tag::simpleClient[]
        Coherence coherence = Coherence.client();
        coherence.start();
        // # end::simpleClient[]
    }

    public void simpleFixedClient() {
        // # tag::simpleFixedClient[]
        Coherence coherence = Coherence.fixedClient();
        coherence.start();
        // # end::simpleFixedClient[]
    }

    public void configureClient() {
        // # tag::configureClient[]
        SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
                                                                        .named("Foo")
                                                                        .withScopeName("Foo")
                                                                        .withConfigUri("foo-cache-config.xml")
                                                                        .build();

        CoherenceConfiguration config = CoherenceConfiguration.builder()
                                                              .withSession(sessionConfiguration)
                                                              .withSession(SessionConfiguration.defaultSession())
                                                              .build();

        Coherence coherence = Coherence.client(config)
                                       .start()
                                       .join();
        // # end::configureClient[]
    }
}
