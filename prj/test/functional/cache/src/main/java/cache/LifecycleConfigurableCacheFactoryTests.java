/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.io.DefaultSerializer;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.Dependencies;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceMonitor;
import com.tangosol.net.SimpleServiceMonitor;

import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;

import static org.junit.Assert.*;

public class LifecycleConfigurableCacheFactoryTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }


    // ----- helpers --------------------------------------------------------

    /**
     * @return classloader for this test
     */
    private ClassLoader getClassLoader()
        {
        return this.getClass().getClassLoader();
        }


    // ----- test methods -------------------------------------------------

    /**
     * Test the shutdown of services when stopping a DCCF.
     */
    @Test
    public void testStop()
        {
        CacheFactory.shutdown();

        Dependencies dependencies = DependenciesHelper.newInstance();
        ExtensibleConfigurableCacheFactory factory = new ExtensibleConfigurableCacheFactory(dependencies);
        factory.activate();

        Service service = factory.ensureService("DistributedCache");
        assertTrue(service.isRunning());

        factory.dispose();
        assertFalse(service.isRunning());
        }


    /**
     * Asserts that deactivating a {@link ExtensibleConfigurableCacheFactory}
     * will not shut down a service that was started with a different
     * {@link ClassLoader}.
     *
     * @throws InterruptedException
     */
    @Test
    public void testServiceShutdown()
            throws InterruptedException
        {
        CacheFactory.shutdown();
        ClassLoader loader = Base.getContextClassLoader();

        try
            {
            ClassLoader loader1 = new ScopedLoader("1", loader);
            ClassLoader loader2 = new ScopedLoader("2", loader);

            Thread.currentThread().setContextClassLoader(loader1);

            Dependencies dependencies1 = DependenciesHelper.newInstance("coherence-cache-config.xml", loader1);
            ExtensibleConfigurableCacheFactory factory1 = new ExtensibleConfigurableCacheFactory(dependencies1);

            factory1.activate();

            Service service1 = factory1.ensureService("DistributedCache");
            assertTrue(service1.isRunning());
            assertEquals(loader1, service1.getContextClassLoader());

            Thread.currentThread().setContextClassLoader(loader2);

            Dependencies dependencies2 = DependenciesHelper.newInstance("coherence-cache-config.xml", loader2);
            ExtensibleConfigurableCacheFactory factory2 = new ExtensibleConfigurableCacheFactory(dependencies2);

            factory2.activate();

            Service service2 = factory2.ensureService("DistributedCache");
            assertTrue(service1 == service2);
            assertTrue(service1.isRunning());

            // the context class loader for service2 should be loader1
            // because it created the service

            // instead of a simple assert, this will log a more descriptive
            // message if the service class loader doesn't match the
            // class loader that created the service
            if (!loader1.equals(service2.getContextClassLoader()))
                {
                ClassLoader loaderService2 =
                        ((com.tangosol.coherence.component.util.SafeService) service2)
                                .getService().getContextClassLoader();
                StringBuilder builder = new StringBuilder();

                builder.append("Expected the same ClassLoader for the safe layer of service ")
                        .append(service2)
                        .append(" and the underlying service;")
                        .append("\n\t service 2 'safe' loader:       ")
                        .append(service2.getContextClassLoader())
                        .append("\n\t service 2 'underlying' loader: ")
                        .append(loaderService2);
                log(builder.toString());
                throw new AssertionError(builder.toString());
                }

            // sanity check
            assertEquals(loader1, service1.getContextClassLoader());

            factory2.dispose();
            assertTrue(service1.isRunning());

            service1 = factory1.ensureService("DistributedCache");
            assertTrue(service1 == service2);

            factory1.dispose();
            assertFalse(service1.isRunning());
            }
        finally
            {
            Thread.currentThread().setContextClassLoader(loader);
            }
        }

    /**
     * Asserts that deactivating a {@link ExtensibleConfigurableCacheFactory}
     * will shut down a service that was started with a different
     * {@link ClassLoader}.
     *
     * @throws InterruptedException
     */
    @Test
    public void testServiceShutdownWithMonitor()
            throws InterruptedException
        {
        CacheFactory.shutdown();
        ClassLoader loader = Base.getContextClassLoader();

        System.out.println("started testServiceShutdown...");

        try
            {
            ClassLoader loader1 = new ScopedLoader("1", loader);
            ClassLoader loader2 = new ScopedLoader("2", loader);

            Thread.currentThread().setContextClassLoader(loader1);

            Dependencies dependencies1 = DependenciesHelper.newInstance("coherence-cache-config.xml", loader1);
            ExtensibleConfigurableCacheFactory factory1 = new ExtensibleConfigurableCacheFactory(dependencies1);

            ServiceMonitor monitor1 = new SimpleServiceMonitor();
            monitor1.setConfigurableCacheFactory(factory1);
            factory1.startServices();
            monitor1.registerServices(factory1.getServiceMap());
            factory1.activate();

            Service service1 = factory1.ensureService("DistributedCache");
            assertTrue(service1.isRunning());
            assertEquals(loader1, service1.getContextClassLoader());

            Thread.currentThread().setContextClassLoader(loader2);

            Dependencies dependencies2 = DependenciesHelper.newInstance("coherence-cache-config.xml", loader2);
            ExtensibleConfigurableCacheFactory factory2 = new ExtensibleConfigurableCacheFactory(dependencies2);

            ServiceMonitor monitor2 = new SimpleServiceMonitor();
            monitor2.setConfigurableCacheFactory(factory2);
            monitor2.registerServices(factory1.getServiceMap());
            factory2.activate();

            Service service2 = factory2.ensureService("DistributedCache");
            assertTrue(service2.isRunning());

            factory1.dispose();
            assertTrue(service1.isRunning());

            factory2.dispose();
            assertFalse(service1.isRunning());
            }
        finally
            {
            Thread.currentThread().setContextClassLoader(loader);
            }
        }

    /**
     * Test the usage of a named POF serializer in the defaults section
     * of the cache configuration.
     */
    @Test
    public void testDefaultPofSerializer()
        {
        String sConfig = "<cache-config xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
                + "xmlns='http://xmlns.oracle.com/coherence/coherence-cache-config' "
                + "xsi:schemaLocation='http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd'>\n"
                + "   <defaults>\n"
                + "     <serializer>pof</serializer>\n"
                + "   </defaults>\n"
                + "   <caching-scheme-mapping>\n"
                + "     <cache-mapping>\n"
                + "       <cache-name>*</cache-name>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "     </cache-mapping>\n"
                + "   </caching-scheme-mapping>\n"
                + "   <caching-schemes>\n"
                + "     <distributed-scheme>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "       <service-name>distributed-service</service-name>\n"
                + "       <backing-map-scheme>\n"
                + "         <local-scheme/>\n"
                + "       </backing-map-scheme>\n"
                + "       <autostart>true</autostart>\n"
                + "     </distributed-scheme>\n"
                + "   </caching-schemes>\n"
                + " </cache-config>";

        CacheFactory.shutdown();
        String sPofConfigUri = "test-pof-config.xml";

        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sConfig), getClassLoader(), sPofConfigUri);
        ExtensibleConfigurableCacheFactory factory = new ExtensibleConfigurableCacheFactory(dependencies);

        Serializer serializer = factory.ensureCache("test", null).getCacheService().getSerializer();

        assertTrue(serializer instanceof ConfigurablePofContext);
        assertTrue(serializer.toString().contains(sPofConfigUri));
        }

    /**
     * Test the usage of providing a POF config URI without any serializer
     * definitions in the cache configuration.
     */
    @Test
    public void testNonDefaultPofSerializer()
        {
        String sConfig = "<cache-config xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
                + "xmlns='http://xmlns.oracle.com/coherence/coherence-cache-config' "
                + "xsi:schemaLocation='http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd'>\n"
                + "   <caching-scheme-mapping>\n"
                + "     <cache-mapping>\n"
                + "       <cache-name>*</cache-name>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "     </cache-mapping>\n"
                + "   </caching-scheme-mapping>\n"
                + "   <caching-schemes>\n"
                + "     <distributed-scheme>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "       <service-name>distributed-service</service-name>\n"
                + "       <backing-map-scheme>\n"
                + "         <local-scheme/>\n"
                + "       </backing-map-scheme>\n"
                + "       <autostart>true</autostart>\n"
                + "     </distributed-scheme>\n"
                + "   </caching-schemes>\n"
                + " </cache-config>";

        CacheFactory.shutdown();
        String sPofConfigUri = "test-pof-config.xml";

        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sConfig), getClassLoader(), sPofConfigUri);
        ExtensibleConfigurableCacheFactory factory = new ExtensibleConfigurableCacheFactory(dependencies);

        Serializer serializer = factory.ensureCache("test", null).getCacheService().getSerializer();

        assertTrue(serializer instanceof ConfigurablePofContext);
        assertTrue(serializer.toString().contains(sPofConfigUri));
        }

    /**
     * Test the usage of a named POF serializer in a distributed-scheme configuration.
     */
    @Test
    public void testServiceSerializer()
        {
        String sConfig = "<cache-config xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
                + "xmlns='http://xmlns.oracle.com/coherence/coherence-cache-config' "
                + "xsi:schemaLocation='http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd'>\n"
                + "   <caching-scheme-mapping>\n"
                + "     <cache-mapping>\n"
                + "       <cache-name>*</cache-name>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "     </cache-mapping>\n"
                + "   </caching-scheme-mapping>\n"
                + "   <caching-schemes>\n"
                + "     <distributed-scheme>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "       <service-name>distributed-service</service-name>\n"
                + "       <serializer>\n"
                + "         <instance>\n"
                + "         <class-name>com.tangosol.io.DefaultSerializer</class-name>\n"
                + "         </instance>\n"
                + "       </serializer>\n"
                + "       <backing-map-scheme>\n"
                + "         <local-scheme/>\n"
                + "       </backing-map-scheme>\n"
                + "       <autostart>true</autostart>\n"
                + "     </distributed-scheme>\n"
                + "   </caching-schemes>\n"
                + " </cache-config>";

        CacheFactory.shutdown();
        String sPofConfigUri = "test-pof-config.xml";

        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sConfig), getClassLoader(), sPofConfigUri);
        ExtensibleConfigurableCacheFactory factory = new ExtensibleConfigurableCacheFactory(dependencies);

        Serializer serializer = factory.ensureCache("test", null).getCacheService().getSerializer();

        assertTrue(serializer instanceof DefaultSerializer);
        }


    /**
     * Assert that a defined serializer isn't modified by the presence of
     * a POF config URI
     */
    @Test
    public void testServicePofSerializer()
        {
        String sConfig = "<cache-config xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
                + "xmlns='http://xmlns.oracle.com/coherence/coherence-cache-config' "
                + "xsi:schemaLocation='http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd'>\n"
                + "   <caching-scheme-mapping>\n"
                + "     <cache-mapping>\n"
                + "       <cache-name>*</cache-name>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "     </cache-mapping>\n"
                + "   </caching-scheme-mapping>\n"
                + "   <caching-schemes>\n"
                + "     <distributed-scheme>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "       <service-name>distributed-service</service-name>\n"
                + "       <serializer>pof</serializer>\n"
                + "       <backing-map-scheme>\n"
                + "         <local-scheme/>\n"
                + "       </backing-map-scheme>\n"
                + "       <autostart>true</autostart>\n"
                + "     </distributed-scheme>\n"
                + "   </caching-schemes>\n"
                + " </cache-config>";

        CacheFactory.shutdown();
        String sPofConfigUri = "test-pof-config.xml";

        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sConfig), getClassLoader(), sPofConfigUri);
        ExtensibleConfigurableCacheFactory factory = new ExtensibleConfigurableCacheFactory(dependencies);

        Serializer serializer = factory.ensureCache("test", null).getCacheService().getSerializer();

        assertTrue(serializer instanceof ConfigurablePofContext);
        assertTrue(serializer.toString().contains(sPofConfigUri));
        }


    /**
     * Test the usage of a POF URI configuration for DCCF along a
     * java serializer in the defaults section of the cache configuration.
     */
    @Test
    public void testDefaultJavaSerializer()
        {
        String sConfig = "<cache-config xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
                + "xmlns='http://xmlns.oracle.com/coherence/coherence-cache-config' "
                + "xsi:schemaLocation='http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd'>\n"
                + "   <defaults>\n"
                + "     <serializer>java</serializer>\n"
                + "   </defaults>\n"
                + "   <caching-scheme-mapping>\n"
                + "     <cache-mapping>\n"
                + "       <cache-name>*</cache-name>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "     </cache-mapping>\n"
                + "   </caching-scheme-mapping>\n"
                + "   <caching-schemes>\n"
                + "     <distributed-scheme>\n"
                + "       <scheme-name>default-scheme</scheme-name>\n"
                + "       <service-name>distributed-service</service-name>\n"
                + "       <backing-map-scheme>\n"
                + "         <local-scheme/>\n"
                + "       </backing-map-scheme>\n"
                + "       <autostart>true</autostart>\n"
                + "     </distributed-scheme>\n"
                + "   </caching-schemes>\n"
                + " </cache-config>";

        CacheFactory.shutdown();
        String sPofConfigUri = "test-pof-config.xml";

        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sConfig), getClassLoader(), sPofConfigUri);
        ExtensibleConfigurableCacheFactory factory = new ExtensibleConfigurableCacheFactory(dependencies);

        Serializer serializer = factory.ensureCache("test", null).getCacheService().getSerializer();

        assertFalse(serializer instanceof ConfigurablePofContext);
        }
    }
