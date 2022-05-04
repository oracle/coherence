/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.Dependencies;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.partition.cache.EntryEvent.Type;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlHelper;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 * Test interceptor registration from XML cache-configuration files.  Ensure that
 * the duplicate interceptor are detected.  Handle scheme-ref properly.
 */
public class InterceptorRegistrationTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default Constructor.
    */
    public InterceptorRegistrationTests()
        {
        super();
        }



    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.log",  "/Users/pmackin/log/log.txt");

        AbstractFunctionalTest._startup();
        }

    @BeforeClass
    public static void _shutdown()
        {
        AbstractFunctionalTest._shutdown();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test interceptor registration with simple scheme.
     */
    @Test
    public void testSimple() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 1);
        }


    /**
     * Test 2 un-named interceptors in a list using the same class.
     */
    @Test
    public void test2InListSameClass() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.UnnamedInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.UnnamedInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 2);
        }

    /**
     * Test 2 interceptors in a list using the same class and same names.
     */
    @Test
    public void test2InListSameClassAndNames() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist2-*</cache-name>\n" +
            "      <scheme-name>dist12</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <name>alpha</name>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "          <interceptor>\n" +
            "              <name>alpha</name>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist12</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <name>beta</name>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 1);
        }

    /**
     * Test 2 interceptors using the same class but unique names.
     */
    @Test
    public void test2InListSameClassUniqueNames() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <name>gamma</name>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "          <interceptor>\n" +
            "              <name>alpha</name>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 2);
        }

    /**
     * Test interceptor registration with 2 unnamed duplicates.
     */
    @Test
    public void test2UnNamedDup() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist12</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 1);
        }

    /**
     * Test interceptor registration with 2 named  duplicates.
     */
    @Test
    public void test2NamedDup() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist2-*</cache-name>\n" +
            "      <scheme-name>dist12</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <name>beta</name>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist12</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <name>beta</name>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 1);
        }

    /**
     * Test interceptor registration with a scheme referencing the 2 deep scheme-ref chain.
     */
    @Test
    public void testRef2Deep() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <scheme-ref>a</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>a</scheme-name>\n" +
            "      <scheme-ref>b</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>b</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 1);
        }

    /**
     * Test interceptor registration with a scheme referencing a 2 deep scheme plus another standalone schemes.
     */
    @Test
    public void testRef2Deep1Standalone() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <scheme-ref>a</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>a</scheme-name>\n" +
            "      <scheme-ref>b</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>b</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>standalone</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 1);
        }

    /**
     * Test a scheme referencing a 3 deep scheme where 2 service names are used.
     **/
    @Test
    public void testRef3Deep2Service() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist2-*</cache-name>\n" +
            "      <scheme-name>dist2</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <scheme-ref>dist2</scheme-ref>\n" +
            "      <thread-count>1</thread-count>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist2</scheme-name>\n" +
            "      <scheme-ref>dist3</scheme-ref>\n" +
            "      <service-name>SERVICE-A</service-name>\n" +
            "      <backup-count>2</backup-count>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist3</scheme-name>\n" +
            "      <scheme-ref>dist4</scheme-ref>\n" +
            "      <partition-count>3</partition-count>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist4</scheme-name>\n" +
            "      <task-timeout>50</task-timeout>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 2, true);
        }

    /**
     * Test interceptor registration with 2 schemes referencing the same scheme-ref chain.
     */
    @Test
    public void test2Ref2Deep() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <scheme-ref>a</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist12</scheme-name>\n" +
            "      <scheme-ref>a</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>a</scheme-name>\n" +
            "      <scheme-ref>b</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>b</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.RegistrationCounterInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 1);
        }

    /**
     * Test duplicate un-named interceptors in a referenced scheme.
     */
    @Test
    public void test2Ref1Deep2UnNamed() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <scheme-ref>parent</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist12</scheme-name>\n" +
            "      <scheme-ref>parent</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>parent</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.UnnamedInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.UnnamedInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 2);
        }

    /**
     * Test duplicate interceptors referenced by schemes with different services.
     */
    @Test
    public void test2Ref1Deep2Unamed2Service() throws InterruptedException
        {
        String sConfig =
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
            "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
            "\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist-*</cache-name>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>dist2-*</cache-name>\n" +
            "      <scheme-name>dist2</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "    <cache-mapping>\n" +
            "     <cache-name>results</cache-name>\n" +
            "     <scheme-name>dist-results</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "\n" +
            "  <caching-schemes>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist1</scheme-name>\n" +
            "      <scheme-ref>dist3</scheme-ref>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist2</scheme-name>\n" +
            "      <scheme-ref>dist3</scheme-ref>\n" +
            "      <service-name>SERVICE-A</service-name>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist3</scheme-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <interceptors>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.UnnamedInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "          <interceptor>\n" +
            "              <instance>\n" +
            "                  <class-name>events.common.UnnamedInterceptor</class-name>\n" +
            "              </instance>\n" +
            "          </interceptor>\n" +
            "      </interceptors>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>dist-results</scheme-name>\n" +
            "      <service-name>ResultsService</service-name>\n" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme/>\n" +
            "      </backing-map-scheme>\n" +
            "      <autostart>true</autostart>\n" +
            "    </distributed-scheme>\n" +
            "\n" +
            "  </caching-schemes>\n" +
            "</cache-config>\n";

        validateInterceptor(sConfig, 4, true);
        }

    /**
     * Test register multiple unnamed interceptors with DCCF.
     */
    @Test
    public void testRegisterUnnamedInterceptorsWithDCCF()
            throws Exception
        {
        try
            {
            ConfigurableCacheFactory ccf = new DefaultConfigurableCacheFactory(new SimpleParser(true).parseXml(m_sConfig));

            CacheFactory.ensureCluster();

            ccf.ensureCache("distributed-test", null).put(1, 1);
            assertEquals(1, TestInterceptor.count);
            ccf.ensureCache("nearcache-test", null).put(2, 2);
            assertEquals(2, TestInterceptor.count);

            //reset the counter
            TestInterceptor.count = 0;
            }
        finally
            {
            CacheFactory.shutdown();
            }
        }

    /**
     * Test register near-cache interceptor with ECCF.
     */
    @Test
    public void testRegisterNearCacheInterceptorWithECCF()
            throws Exception
        {
        try
            {
            Dependencies dependencies = DependenciesHelper.newInstance(new SimpleParser(true).parseXml(m_sConfig));

            ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(dependencies);
            CacheFactory.ensureCluster();

            eccf.ensureCache("nearcache-test", getClassLoader()).put(4,4);
            assertEquals(1, TestInterceptor.count);

            //reset the counter
            TestInterceptor.count = 0;
            }
        finally
            {
            CacheFactory.shutdown();
            }
        }

    /**
     * Test register near-cache interceptor with ECCF and subclass.
     */
    @Test
    public void testRegisterNearCacheInterceptorWithECCFAndSubclass()
            throws Exception
        {
        try
            {
            Dependencies dependencies = DependenciesHelper.newInstance(new SimpleParser(true).parseXml(m_sConfigSubclass));

            ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(dependencies);
            CacheFactory.ensureCluster();

            eccf.ensureCache("nearcache-test", getClassLoader()).put(4,4);
            assertEquals(1, TestInterceptorSubclass.count);

            //reset the counter
            TestInterceptorSubclass.count = 0;
            }
        finally
            {
            CacheFactory.shutdown();
            }
        }

    // ----- helpers --------------------------------------------------------


    /**
     * Validate that n interceptors are registered.
     *
     * @param  sConfig        the cache config XML string
     * @param  cInterceptors  the number of interceptors expected
     */
    protected void validateInterceptor(String sConfig, int cInterceptors) throws InterruptedException
        {
        validateInterceptor(sConfig, cInterceptors, false);
        }

    /**
     * Validate that n interceptors are registered.
     *
     * @param  sConfig        the cache config XML string
     * @param  cInterceptors  the number of interceptors expected
     * @param fPutDist2       true if a put to dist2-test should be done
     */
    protected void validateInterceptor(String sConfig, int cInterceptors, boolean fPutDist2) throws InterruptedException
        {
        ClassLoader loader = null;
        CacheFactory.getCacheFactoryBuilder().setCacheConfiguration(loader, XmlHelper.loadXml(sConfig));

        ConfigurableCacheFactory ccf = CacheFactory.getConfigurableCacheFactory(loader);

        NamedCache testCache = ccf.ensureCache("dist-test", loader);
        testCache.put(1,1);

        if (fPutDist2)
            {
            NamedCache test2Cache = ccf.ensureCache("dist2-test", loader);
            test2Cache.put(1,1);
            }

        NamedCache resultCache = ccf.ensureCache("results", loader);

        try
            {
            Eventually.assertThat(invoking(resultCache).size(), is(cInterceptors));
            }
        finally
            {
            resultCache.clear();
            ccf.dispose();
            }
        }

    /**
     * Wait for the results to be ready.
     *
     * @param results the results cache
     * @param cResults of results
     */
    protected void waitForResult(NamedCache results, int cResults)
        {
        Eventually.assertThat(invoking(results).size(), greaterThanOrEqualTo(cResults));
        }

    private ClassLoader getClassLoader()
        {
        return this.getClass().getClassLoader();
        }

    /**
     * Simple TestInterceptor class for the tests
     */
    @EntryEvents(Type.INSERTING)
    public static class TestInterceptor<T extends Event<? extends Enum>> implements EventInterceptor<T>
        {
         public static int count;

         public void onEvent(T event)
             {
             count++;
             }
        }

    /**
     * Simple TestInterceptorSubclass to test fix for Bug 33125540.
     */
    public static class TestInterceptorSubclass extends TestInterceptor
        {
        public TestInterceptorSubclass()
            {
            super();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Cache config file to use for these tests.
     */
    public static final String CFG_FILE = "interceptor-registration-cache-config.xml";

    /**
     * Used as a signal to know when the results cache has been updated.
     */
    public volatile boolean m_fResult;

    /**
     *  Local Cache Config used for tests.
     */
    String m_sConfig =
           "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
           "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
           "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
           "\n" +
           "  <caching-scheme-mapping>\n" +
           "    <cache-mapping>\n" +
           "      <cache-name>distributed-*</cache-name>\n" +
           "      <scheme-name>dist</scheme-name>\n" +
           "      <interceptors>\n" +
           "          <interceptor>\n" +
           "              <instance>\n" +
           "                  <class-name>events.InterceptorRegistrationTests$TestInterceptor</class-name>\n" +
           "              </instance>\n" +
           "          </interceptor>\n" +
           "      </interceptors>\n" +
           "    </cache-mapping>\n" +
           "    <cache-mapping>\n" +
           "      <cache-name>nearcache-*</cache-name>\n" +
           "      <scheme-name>near</scheme-name>\n" +
           "      <interceptors>\n" +
           "          <interceptor>\n" +
           "              <instance>\n" +
           "                  <class-name>events.InterceptorRegistrationTests$TestInterceptor</class-name>\n" +
           "              </instance>\n" +
           "          </interceptor>\n" +
           "      </interceptors>\n" +
           "    </cache-mapping>\n" +
           "  </caching-scheme-mapping>\n" +
           "\n" +
           "  <caching-schemes>\n" +
           "\n" +
           "    <distributed-scheme>\n" +
           "      <scheme-name>dist</scheme-name>\n" +
           "      <scheme-ref>dist3</scheme-ref>\n" +
           "    </distributed-scheme>\n" +
           "\n" +
           "    <near-scheme>\n" +
           "      <scheme-name>near</scheme-name>\n" +
           "      <front-scheme>\n" +
           "        <local-scheme/>\n" +
           "      </front-scheme>\n" +
           "      <back-scheme>\n" +
           "        <distributed-scheme>\n" +
           "          <scheme-ref>dist3</scheme-ref>\n" +
           "        </distributed-scheme>\n" +
           "      </back-scheme>\n" +
           "    </near-scheme>\n" +
           "\n" +
           "    <distributed-scheme>\n" +
           "      <scheme-name>dist3</scheme-name>\n" +
           "      <backing-map-scheme>\n" +
           "        <local-scheme/>\n" +
           "      </backing-map-scheme>\n" +
           "    </distributed-scheme>\n" +
           "\n" +
           "  </caching-schemes>\n" +
           "</cache-config>\n";

    /**
     *  Local Cache Config used for tests for TestInterceptorSubclass.
     */
    String m_sConfigSubclass =
           "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
           "              xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
           "              xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\">\n" +
           "\n" +
           "  <caching-scheme-mapping>\n" +
           "    <cache-mapping>\n" +
           "      <cache-name>distributed-*</cache-name>\n" +
           "      <scheme-name>dist</scheme-name>\n" +
           "      <interceptors>\n" +
           "          <interceptor>\n" +
           "              <instance>\n" +
           "                  <class-name>events.InterceptorRegistrationTests$TestInterceptorSubclass</class-name>\n" +
           "              </instance>\n" +
           "          </interceptor>\n" +
           "      </interceptors>\n" +
           "    </cache-mapping>\n" +
           "    <cache-mapping>\n" +
           "      <cache-name>nearcache-*</cache-name>\n" +
           "      <scheme-name>near</scheme-name>\n" +
           "      <interceptors>\n" +
           "          <interceptor>\n" +
           "              <instance>\n" +
           "                  <class-name>events.InterceptorRegistrationTests$TestInterceptorSubclass</class-name>\n" +
           "              </instance>\n" +
           "          </interceptor>\n" +
           "      </interceptors>\n" +
           "    </cache-mapping>\n" +
           "  </caching-scheme-mapping>\n" +
           "\n" +
           "  <caching-schemes>\n" +
           "\n" +
           "    <distributed-scheme>\n" +
           "      <scheme-name>dist</scheme-name>\n" +
           "      <scheme-ref>dist3</scheme-ref>\n" +
           "    </distributed-scheme>\n" +
           "\n" +
           "    <near-scheme>\n" +
           "      <scheme-name>near</scheme-name>\n" +
           "      <front-scheme>\n" +
           "        <local-scheme/>\n" +
           "      </front-scheme>\n" +
           "      <back-scheme>\n" +
           "        <distributed-scheme>\n" +
           "          <scheme-ref>dist3</scheme-ref>\n" +
           "        </distributed-scheme>\n" +
           "      </back-scheme>\n" +
           "    </near-scheme>\n" +
           "\n" +
           "    <distributed-scheme>\n" +
           "      <scheme-name>dist3</scheme-name>\n" +
           "      <backing-map-scheme>\n" +
           "        <local-scheme/>\n" +
           "      </backing-map-scheme>\n" +
           "    </distributed-scheme>\n" +
           "\n" +
           "  </caching-schemes>\n" +
           "</cache-config>\n";
    }
