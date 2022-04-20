/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.AddressProvider;

import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.util.Base;

import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertEquals;


import java.net.InetSocketAddress;

/**
 *  {@link AddressProviderBuilderTest} provides unit tests for {@link AddressProviderBuilder}s implementations.
 *
 * @author jf 2015.02.27
 *
 * @since 12.2.1
 */
public class AddressProviderBuilderTest
    {

    @Test
    public void testDefaultLocalAddressProviderBuilderTest()
        {
        LocalAddressProviderBuilder builder = new LocalAddressProviderBuilder("127.0.0.1", 9090, 9090);
        AddressProvider provider = builder.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);

        assertNotNull(provider.getNextAddress());

        provider = builder.createAddressProvider(Base.getContextClassLoader());
        assertNotNull(provider.getNextAddress());
        }


    @Test
    public void testEmptyListBasedAddressProviderBuilderTest()
        {
        ListBasedAddressProviderBuilder builder = new ListBasedAddressProviderBuilder();
        AddressProvider provider = builder.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);

        assertNull(provider.getNextAddress());

        provider = builder.createAddressProvider(Base.getContextClassLoader());
        assertNull(provider.getNextAddress());
        }

    @Test
    public void testNullFactoryAddressProviderBuilderTest()
        {
        FactoryBasedAddressProviderBuilder builder = new FactoryBasedAddressProviderBuilder(null);
        AddressProvider provider = builder.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);

        assertNotNull(provider);
        assertNull(provider.getNextAddress());
        }

    @Test
    public void testNullCustomAddressProviderBuilderTest()
        {
        CustomAddressProviderBuilder builder = new CustomAddressProviderBuilder(null);
        AddressProvider provider = builder.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);

        assertNotNull(provider);
        assertNull(provider.getNextAddress());
        }

    @Test
    public void testMinimalFactoryBasedAddressProviderBuilderTest()
        {
        FactoryBasedAddressProviderBuilder builder = new FactoryBasedAddressProviderBuilder(new AddressProviderFactory()
        {
        @Override
        public AddressProvider createAddressProvider(ClassLoader loader)
            {
            return new AddressProvider()
            {

            private boolean done = false;

            @Override
            public InetSocketAddress getNextAddress()
                {
                if (done)
                    {
                    return null;
                    }
                else
                    {
                    done = true;
                    return new InetSocketAddress(0);
                    }
                }

            @Override
            public void accept()
                {
                throw new UnsupportedOperationException("not implemented");
                }

            @Override
            public void reject(Throwable eCause)
                {
                throw new UnsupportedOperationException("not implemented");
                }
            };
            }
        });

        AddressProvider provider = builder.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
        assertNotNull(provider);
        assertNotNull(provider.getNextAddress());
        assertNull(provider.getNextAddress());
        }


    @Test
    public void testMinimalCustomAddressProviderBuilderTest()
        {
        CustomAddressProviderBuilder builder =
            new CustomAddressProviderBuilder(new ParameterizedBuilder<AddressProvider>()
            {
            @Override
            public AddressProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new AddressProvider()
                    {
                    boolean done = false;

                    @Override
                    public InetSocketAddress getNextAddress()
                        {
                        if (done)
                            {
                            return null;
                            }
                        else
                            {
                            done = true;

                            return new InetSocketAddress(0);
                            }
                        }

                    @Override
                    public void accept()
                        {
                        throw new UnsupportedOperationException("not implemented");
                        }

                    @Override
                    public void reject(Throwable eCause)
                        {
                        throw new UnsupportedOperationException("not implemented");
                        }
                    };
                }


            });
        AddressProvider provider = builder.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);

        assertNotNull(provider.getNextAddress());
        assertNull(provider.getNextAddress());

        provider = builder.createAddressProvider(Base.getContextClassLoader());
        assertNotNull(provider.getNextAddress());
        assertNull(provider.getNextAddress());
        }

    @Test
    public void testListBasedAddressProviderBuilderTest()
        {
        final int                  MAX_ADDRS = 10;
        ListBasedAddressProviderBuilder builder   = new ListBasedAddressProviderBuilder();

        for (int i = 0; i < MAX_ADDRS; i++)
            {
            String sAddr = "1.1.0." + i + 1;

            builder.add(sAddr, 9090);
            }

        AddressProvider provider = builder.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);

        for (int i = 0; i < MAX_ADDRS; i++)
            {
            assertNotNull(provider.getNextAddress());
            }

        assertNull(provider.getNextAddress());
        }

    private void validateInvalidLocalAddressProviderBuilder(String sAddr, int nPort, int nPortAdjust)
        {
        LocalAddressProviderBuilder builder = new LocalAddressProviderBuilder(sAddr, nPort, nPortAdjust);

        assertNotNull(builder);

        try
            {
            AddressProvider provider = builder.createAddressProvider(null);

            assertNull("expected ConfigurationException, encountered " + provider, provider);
            }
        catch (ConfigurationException e)
            {
            // passed.  expected path.
            }
        catch (IllegalArgumentException iae)
            {
            // passed. expected path.
            }
        }

    @Test
    public void testInvalidLocalAddressProviderBuilderTest()
        {
        final String SADDR = "127.0.0.1";
        validateInvalidLocalAddressProviderBuilder(SADDR, LocalAddressProviderBuilder.MIN_PORT - 1 , 2);
        validateInvalidLocalAddressProviderBuilder(SADDR, LocalAddressProviderBuilder.MAX_PORT + 1, LocalAddressProviderBuilder.MAX_PORT + 2);
        validateInvalidLocalAddressProviderBuilder(SADDR, 1, LocalAddressProviderBuilder.MAX_PORT + 1);
        validateInvalidLocalAddressProviderBuilder(SADDR, 5, 4);
        validateInvalidLocalAddressProviderBuilder(NONEXISTENTHOSTNAME, 1, 2);
        }

    /**
     * Validate address and port
     *
     * @param sAddr hostname or ip address
     * @param nPort non-negative port address
     * @param fValid true iff if ConfigurationException should not be thrown
     */
    private ListBasedAddressProviderBuilder validateListBasedAddressProviderBuilder(String sAddr, int nPort, boolean fValid)
        {
        ListBasedAddressProviderBuilder builder   = new ListBasedAddressProviderBuilder();

        assertNotNull(builder);
        builder.add(sAddr, nPort);

        try
            {
            AddressProvider provider = builder.createAddressProvider(null);

            assertTrue("unexpected result: AddressListProviderBuilder realize did not throw ConfigurationException" , fValid);
            if (!fValid)
                {
                assertNull("expected ConfigurationException, encountered " + provider, provider);
                }
            assertEquals(InetAddressHelper.isHostName(sAddr), builder.isRefreshable());
            }
        catch (ConfigurationException e)
            {
            assertFalse("unexpected ConfigurationException for valid addr=" + sAddr + " port=" + nPort, fValid);
            }
        return builder;
        }

    @Test
    public void testValidListBasedAddressProviderBuilderTest()
        {
        final String SADDR = "127.0.0.1";
        validateListBasedAddressProviderBuilder(SADDR, LocalAddressProviderBuilder.MIN_PORT, VALID);
        validateListBasedAddressProviderBuilder(SADDR, LocalAddressProviderBuilder.MAX_PORT, VALID);
        validateListBasedAddressProviderBuilder("localhost", 1, VALID);
        }

    @Test
    public void testInvalidListBasedAddressProviderBuilderTest()
        {
        final String SADDR = "127.0.0.1";
        validateListBasedAddressProviderBuilder(SADDR, LocalAddressProviderBuilder.MIN_PORT - 1, INVALID);
        validateListBasedAddressProviderBuilder(SADDR, LocalAddressProviderBuilder.MAX_PORT + 1, INVALID);
        validateListBasedAddressProviderBuilder(null, 1, INVALID);

        // unresolveable address is skipped, not reported as a configuration exception.
        // Here is log message:
        //  <Info> The ConfigurableAddressProvider is skipping the unresolvable address "NONEXISTENTHOSTNAME:1".
        ListBasedAddressProviderBuilder builder = validateListBasedAddressProviderBuilder(NONEXISTENTHOSTNAME, 1, VALID);
        assertNull(builder.realize(null, null, null).getNextAddress());
        }

    // ----- constants ------------------------------------------------------
    final public static boolean VALID = true;
    final public static boolean INVALID = false;

    final public static String NONEXISTENTHOSTNAME = "non.existent.host.name.neverever";
    }
