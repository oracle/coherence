/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package io;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofConfigProvider;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PofConfigDiscoveryTests
    {
    @Test
    public void shouldDiscoverConfigsByDefault()
        {
        ConfigurablePofContext pofContext = new ConfigurablePofContext("default-discovery-pof-config.xml");
        assertThat(pofContext.getClass(1000), is(PofTypeOne.class));
        assertThat(pofContext.getClass(2000), is(PofTypeTwo.class));
        assertThat(pofContext.getClass(3000), is(PofTypeThree.class));
        }

    @Test
    public void shouldEnabledDiscoveryOfConfigs()
        {
        ConfigurablePofContext pofContext = new ConfigurablePofContext("enabled-discovery-pof-config.xml");
        assertThat(pofContext.getClass(1000), is(PofTypeOne.class));
        assertThat(pofContext.getClass(2000), is(PofTypeTwo.class));
        assertThat(pofContext.getClass(3000), is(PofTypeThree.class));
        }

    @Test
    public void shouldIncludeEnabledDiscoveryOfConfigs()
        {
        ConfigurablePofContext pofContext = new ConfigurablePofContext("include-enabled-discovery-pof-config.xml");
        assertThat(pofContext.getClass(1000), is(PofTypeOne.class));
        assertThat(pofContext.getClass(2000), is(PofTypeTwo.class));
        assertThat(pofContext.getClass(3000), is(PofTypeThree.class));
        }

    @Test
    public void shouldDisabledDiscoveryOfConfigs()
        {
        ConfigurablePofContext pofContext = new ConfigurablePofContext("disabled-discovery-pof-config.xml");
        assertThat(pofContext.getClass(1000), is(PofTypeOne.class));
        assertThat(pofContext.isUserType(PofTypeTwo.class), is(false));
        assertThat(pofContext.isUserType(PofTypeThree.class), is(false));
        }

    @Test
    public void shouldIncludeDisabledDiscoveryOfConfigs()
        {
        ConfigurablePofContext pofContext = new ConfigurablePofContext("include-disabled-discovery-pof-config.xml");
        assertThat(pofContext.getClass(1000), is(PofTypeOne.class));
        assertThat(pofContext.isUserType(PofTypeTwo.class), is(false));
        assertThat(pofContext.isUserType(PofTypeThree.class), is(false));
        }

    public static class TestPofConfigProviderOne
            implements PofConfigProvider
        {
        @Override
        public String getConfigURI()
            {
            return "discovered-pof-config-one.xml";
            }
        }

    public static class TestPofConfigProviderTwo
            implements PofConfigProvider
        {
        @Override
        public String getConfigURI()
            {
            return "discovered-pof-config-two.xml";
            }
        }

    public static class PofTypeOne
            implements PortableObject
        {
        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    public static class PofTypeTwo
            implements PortableObject
        {
        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    public static class PofTypeThree
            implements PortableObject
        {
        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }
    }
