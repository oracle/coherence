/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import com.tangosol.coherence.http.JavaHttpServer;

import com.tangosol.internal.http.BaseHttpHandler;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;
import com.tangosol.internal.http.SimpleHttpHandler;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class URLMemberIdentityProviderTests
    {
    @BeforeClass
    public static void startServer() throws Exception
        {
        System.setProperty(URLMemberIdentityProvider.PROP_RETRY_TIMEOUT, "1s");

        SimpleHttpHandler handler = new SimpleHttpHandler(BaseHttpHandler.StringBodyWriter.INSTANCE);

        RequestRouter router = handler.getRouter();
        router.addGet("machine", URLMemberIdentityProviderTests::getMachine);
        router.addGet("member", URLMemberIdentityProviderTests::getMember);
        router.addGet("rack", URLMemberIdentityProviderTests::getRack);
        router.addGet("role", URLMemberIdentityProviderTests::getRole);
        router.addGet("site", URLMemberIdentityProviderTests::getSite);


        s_server = new JavaHttpServer();
        s_server.setLocalAddress("0.0.0.0");
        s_server.setLocalPort(0);
        s_server.setResourceConfig(handler);
        s_server.start();

        s_sBaseURL = "http://127.0.0.1:" + s_server.getListenPort() + "/";
        }

    @AfterClass
    public static void stopServer() throws Exception
        {
        if (s_server != null)
            {
            s_server.stop();
            }
        }

    // ----- machine --------------------------------------------------------
    
    @Test
    public void shouldReturnNullForBadURL()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_MACHINE, "xyz://foo");
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.load("test", URLMemberIdentityProvider.PROP_MACHINE);
        assertThat(sResult, is(nullValue()));
        }

    @Test
    public void shouldReturnNullForNonexistentResource()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_MACHINE, "foo.bar");
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.load("test", URLMemberIdentityProvider.PROP_MACHINE);
        assertThat(sResult, is(nullValue()));
        }

    @Test
    public void shouldLoadMachineFromURL()
        {
        String sExpected = "test-machine";

        System.setProperty(URLMemberIdentityProvider.PROP_MACHINE, s_sBaseURL + "machine");
        s_machineResponse = Response.ok(asStream(sExpected)).build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMachineName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadMachineFromURLWhenNotListening()
        {
        String sExpected = "test-machine";
        int    port      = AbstractTestInfrastructure.getAvailablePorts().next(); // known free port

        System.setProperty(URLMemberIdentityProvider.PROP_MACHINE, "http://127.0.0.1:" + port + "/machine");
        s_machineResponse = Response.ok(asStream(sExpected)).build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMachineName();
        assertThat(sResult, is(nullValue()));
        }

    @Test
    public void shouldLoadMachineFromURLWhenNotFound()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_MACHINE, s_sBaseURL + "machine");
        s_machineResponse = Response.notFound().build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMachineName();
        assertThat(sResult, is(nullValue()));
        }

    @Test
    public void shouldLoadMachineFromFileURL() throws Exception
        {
        Path   path      = Files.createTempFile("machine", ".txt");
        String sExpected = "test-machine-two";
        write(path, sExpected);

        System.setProperty(URLMemberIdentityProvider.PROP_MACHINE, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMachineName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadMachineFromFile()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_MACHINE, "test-machine.txt");
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMachineName();
        assertThat(sResult, is("my-test-machine"));
        }

    @Test
    public void shouldLoadMachineFromFileURLWhenFileNotFound() throws Exception
        {
        Path path = Files.createTempFile("machine", ".txt");
        System.setProperty(URLMemberIdentityProvider.PROP_MACHINE, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMachineName();
        assertThat(sResult, is(nullValue()));
        }
    
    // ----- member --------------------------------------------------------
    
    @Test
    public void shouldLoadMemberFromURL()
        {
        String sExpected = "test-member";

        System.setProperty(URLMemberIdentityProvider.PROP_MEMBER, s_sBaseURL + "member");
        s_memberResponse = Response.ok(asStream(sExpected)).build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMemberName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadMemberFromURLWhenNotFound()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_MEMBER, s_sBaseURL + "member");
        s_memberResponse = Response.notFound().build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMemberName();
        assertThat(sResult, is(nullValue()));
        }

    @Test
    public void shouldLoadMemberFromFileURL() throws Exception
        {
        Path   path      = Files.createTempFile("member", ".txt");
        String sExpected = "test-member-two";
        write(path, sExpected);

        System.setProperty(URLMemberIdentityProvider.PROP_MEMBER, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMemberName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadMemberFromFile()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_MEMBER, "test-member.txt");
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMemberName();
        assertThat(sResult, is("my-test-member"));
        }

    @Test
    public void shouldLoadMemberFromFileURLWhenFileNotFound() throws Exception
        {
        Path path = Files.createTempFile("member", ".txt");
        System.setProperty(URLMemberIdentityProvider.PROP_MEMBER, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getMemberName();
        assertThat(sResult, is(nullValue()));
        }

    // ----- rack -----------------------------------------------------------

    @Test
    public void shouldLoadRackFromURL()
        {
        String sExpected = "test-rack";

        System.setProperty(URLMemberIdentityProvider.PROP_RACK, s_sBaseURL + "rack");
        s_rackResponse = Response.ok(asStream(sExpected)).build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRackName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadRackFromURLWhenNotFound()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_RACK, s_sBaseURL + "rack");
        s_rackResponse = Response.notFound().build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRackName();
        assertThat(sResult, is(nullValue()));
        }

    @Test
    public void shouldLoadRackFromFileURL() throws Exception
        {
        Path   path      = Files.createTempFile("rack", ".txt");
        String sExpected = "test-rack-two";
        write(path, sExpected);

        System.setProperty(URLMemberIdentityProvider.PROP_RACK, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRackName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadRackFromFile()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_RACK, "test-rack.txt");
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRackName();
        assertThat(sResult, is("my-test-rack"));
        }

    @Test
    public void shouldLoadRackFromFileURLWhenFileNotFound() throws Exception
        {
        Path path = Files.createTempFile("rack", ".txt");
        System.setProperty(URLMemberIdentityProvider.PROP_RACK, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRackName();
        assertThat(sResult, is(nullValue()));
        }

    // ----- role -----------------------------------------------------------

    @Test
    public void shouldLoadRoleFromURL()
        {
        String sExpected = "test-role";

        System.setProperty(URLMemberIdentityProvider.PROP_ROLE, s_sBaseURL + "role");
        s_roleResponse = Response.ok(asStream(sExpected)).build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRoleName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadRoleFromURLWhenNotFound()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_ROLE, s_sBaseURL + "role");
        s_roleResponse = Response.notFound().build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRoleName();
        assertThat(sResult, is(nullValue()));
        }

    @Test
    public void shouldLoadRoleFromFileURL() throws Exception
        {
        Path   path      = Files.createTempFile("role", ".txt");
        String sExpected = "test-role-two";
        write(path, sExpected);

        System.setProperty(URLMemberIdentityProvider.PROP_ROLE, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRoleName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadRoleFromFile()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_ROLE, "test-role.txt");
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRoleName();
        assertThat(sResult, is("my-test-role"));
        }

    @Test
    public void shouldLoadRoleFromFileURLWhenFileNotFound() throws Exception
        {
        Path path = Files.createTempFile("role", ".txt");
        System.setProperty(URLMemberIdentityProvider.PROP_ROLE, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getRoleName();
        assertThat(sResult, is(nullValue()));
        }

    // ----- site -----------------------------------------------------------

    @Test
    public void shouldLoadSiteFromURL()
        {
        String sExpected = "test-site";

        System.setProperty(URLMemberIdentityProvider.PROP_SITE, s_sBaseURL + "site");
        s_siteResponse = Response.ok(asStream(sExpected)).build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getSiteName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadSiteFromURLWhenNotFound()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_SITE, s_sBaseURL + "site");
        s_siteResponse = Response.notFound().build();
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getSiteName();
        assertThat(sResult, is(nullValue()));
        }

    @Test
    public void shouldLoadSiteFromFileURL() throws Exception
        {
        Path   path      = Files.createTempFile("site", ".txt");
        String sExpected = "test-site-two";
        write(path, sExpected);

        System.setProperty(URLMemberIdentityProvider.PROP_SITE, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getSiteName();
        assertThat(sResult, is(sExpected));
        }

    @Test
    public void shouldLoadSiteFromFile()
        {
        System.setProperty(URLMemberIdentityProvider.PROP_SITE, "test-site.txt");
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getSiteName();
        assertThat(sResult, is("my-test-site"));
        }

    @Test
    public void shouldLoadSiteFromFileURLWhenFileNotFound() throws Exception
        {
        Path path = Files.createTempFile("site", ".txt");
        System.setProperty(URLMemberIdentityProvider.PROP_SITE, path.toUri().toURL().toExternalForm());
        URLMemberIdentityProvider provider = new URLMemberIdentityProvider();
        String sResult = provider.getSiteName();
        assertThat(sResult, is(nullValue()));
        }

    // ----- helper methods -------------------------------------------------

    private InputStream asStream(String sValue)
        {
        return new ByteArrayInputStream(sValue.getBytes(StandardCharsets.UTF_8));
        }

    private void write(Path path, String sValue) throws Exception
        {
        try (PrintWriter writer = new PrintWriter(path.toFile()))
            {
            writer.write(sValue);
            }
        }

    private static Response getMachine()
        {
        return s_machineResponse == null
                ? Response.notFound().build()
                : s_machineResponse;
        }

    private static Response getMember()
        {
        return s_memberResponse == null
                ? Response.notFound().build()
                : s_memberResponse;
        }

    private static Response getRack()
        {
        return s_rackResponse == null
                ? Response.notFound().build()
                : s_rackResponse;
        }

    private static Response getRole()
        {
        return s_roleResponse == null
                ? Response.notFound().build()
                : s_roleResponse;
        }

    private static Response getSite()
        {
        return s_siteResponse == null
                ? Response.notFound().build()
                : s_siteResponse;
        }

    // ----- data members ---------------------------------------------------

    private static Response s_machineResponse;

    private static Response s_memberResponse;

    private static Response s_rackResponse;

    private static Response s_roleResponse;

    private static Response s_siteResponse;

    private static String s_sBaseURL;

    private static JavaHttpServer s_server;
    }
