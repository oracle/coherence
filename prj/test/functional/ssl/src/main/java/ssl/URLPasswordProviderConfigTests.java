/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.internal.net.ssl.ManagerDependencies;

import com.tangosol.net.PasswordProvider;
import com.tangosol.net.SocketProviderFactory.DefaultDependencies;
import com.tangosol.net.URLPasswordProvider;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class URLPasswordProviderConfigTests
        extends BaseSocketProviderTests
    {
    @Test
    @SuppressWarnings({"rawtypes"})
    public void shouldHaveURLPassword()
        {
        SocketProviderBuilder                builder      = getSocketProviderBuilder("url-password-config.xml");
        DefaultDependencies                  dependencies = (DefaultDependencies) builder.getDependencies();
        Map                                  map          = dependencies.getSSLDependenciesBuilderMap();
        SSLSocketProviderDependenciesBuilder builderDeps  = (SSLSocketProviderDependenciesBuilder) map.get("");
        ManagerDependencies                  mgrDeps      = builderDeps.getIdentityManager();

        assertThat(mgrDeps, is(notNullValue()));

        PasswordProvider passwordProvider = mgrDeps.getPrivateKeyPasswordProvider();
        assertThat(passwordProvider, is(instanceOf(URLPasswordProvider.class)));

        URLPasswordProvider urlPasswordProvider = (URLPasswordProvider) passwordProvider;
        assertThat(urlPasswordProvider.getURL(), is("file:/secret.txt"));
        assertThat(urlPasswordProvider.isFirstLineOnly(), is(true));

        passwordProvider = mgrDeps.getKeystoreDependencies().getPasswordProvider();
        assertThat(passwordProvider, is(instanceOf(URLPasswordProvider.class)));

        urlPasswordProvider = (URLPasswordProvider) passwordProvider;
        assertThat(urlPasswordProvider.getURL(), is("file:/password.txt"));
        assertThat(urlPasswordProvider.isFirstLineOnly(), is(false));
        }
    }
