/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.testing.SystemPropertyResource;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.unit.LowUnits;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies XML low-watermark configuration through modern and legacy factories.
 *
 * @author Aleks Seovic  2026.11.06
 * @since 27.01
 */
public class LowUnitsConfigurationTest
    {
    @Test
    public void shouldConfigureLiteralAndMacroPercentages()
        {
        assertCache("1000", "90%", 1000, 900);
        assertCache("1000", "90.5%", 1000, 905);
        assertCache("1000", "{low-watermark 90%}", 1000, 900);
        try (var ignored = new SystemPropertyResource("test.low.units", "85%"))
            {
            assertCache("1000", "${test.low.units}", 1000, 850);
            }
        }

    @Test
    public void shouldPreserveTypedMacroParameters() throws Exception
        {
        var context = mock(com.tangosol.config.xml.ProcessingContext.class);
        when(context.getExpressionParser()).thenReturn(
                com.tangosol.coherence.config.ParameterMacroExpressionParser.INSTANCE);
        var processor = new com.tangosol.coherence.config.xml.processor.LowUnitsProcessor();
        var expression = processor.process(context, XmlHelper.loadXml("<low-units>{watermark}</low-units>"));
        var resolver = new com.tangosol.coherence.config.ResolvableParameterList();
        var absolute = new com.tangosol.coherence.config.unit.Units(500);
        resolver.add(new com.tangosol.config.expression.Parameter("watermark", absolute));
        org.junit.Assert.assertSame(absolute, expression.evaluate(resolver));
        resolver.add(new com.tangosol.config.expression.Parameter("watermark", "90%"));
        assertEquals(0.9, ((LowUnits) expression.evaluate(resolver)).getRatio(), 0.0);
        }

    @Test
    public void shouldDefaultOmittedZeroAndEmptyValues()
        {
        try (var ignored = new SystemPropertyResource(PROPERTY, "0.9"))
            {
            assertCache("1000", null, 1000, 900);
            assertCache("1000", "", 1000, 900);
            assertCache("1000", "0", 1000, 900);
            assertCache("1000", "500", 1000, 500);
            assertCache("1000", "60%", 1000, 600);
            }
        }

    @Test
    public void shouldPreserveMemorySizesAndAutoscaling()
        {
        assertCache("100M", "90%", 104857600, 94371840);
        assertCache("100M", "50M", 104857600, 52428800);
        assertCache("4G", "90%", 4194304, 3774873);
        assertCache("4G", "2G", 4194304, 2097152);
        }

    @Test
    public void shouldRejectInvalidPercentagesAtRealization()
        {
        for (String s : new String[] {"0%", "100%", "NaN%", "90%%", "90%junk"})
            {
            assertThrows(s, RuntimeException.class, () -> realize("1000", s));
            }
        assertThrows(RuntimeException.class, () -> realize("90%", "0"));
        }

    @Test
    public void shouldHonorSchemeInheritance()
        {
        String sXml = """
                <cache-config>
                  <caching-schemes>
                    <local-scheme>
                      <scheme-name>base</scheme-name>
                      <high-units>1000</high-units>
                      <low-units>90%</low-units>
                    </local-scheme>
                    <local-scheme>
                      <scheme-name>local</scheme-name>
                      <scheme-ref>base</scheme-ref>
                      <high-units>2000</high-units>
                    </local-scheme>
                  </caching-schemes>
                </cache-config>
                """;
        var factory = factory(sXml);
        try
            {
            var scheme = (LocalScheme) factory.getCacheConfig().findSchemeBySchemeName("local");
            var cache = scheme.realizeMap(new NullParameterResolver(), dependencies(factory));
            assertEquals(1800, cache.getLowUnits());
            }
        finally
            {
            factory.dispose();
            }
        }

    @Test
    public void shouldConfigureLegacyXmlFactory()
        {
        try (var ignored = new SystemPropertyResource(PROPERTY, "0.9"))
            {
            int[] expected = {900, 900, 605, 500};
            int n = 0;
            for (String s : new String[] {"0", "90%", "60.5%", "500"})
                {
                var factory = new TestLegacyFactory();
                String sXml = configuration("1000", s).replace("<high-units>",
                        "<service-name>LowUnitsLegacy" + n + "</service-name>"
                                + "<eviction-policy>LRU</eviction-policy><high-units>");
                factory.setConfig(XmlHelper.loadXml(sXml));
                try
                    {
                    var cache = factory.ensureCache("test", getClass().getClassLoader());
                    for (int i = 0; i <= 1000; i++)
                        {
                        cache.put(i, i);
                        }
                    assertEquals(expected[n++], factory.cache.getLowUnits());
                    assertTrue(cache.size() <= 1000);
                    factory.releaseCache(cache);
                    }
                finally
                    {
                    factory.dispose();
                    }
                }
            }
        }

    private LocalCache realize(String sHigh, String sLow)
        {
        var factory = factory(configuration(sHigh, sLow));
        try
            {
            var scheme = (LocalScheme) factory.getCacheConfig().findSchemeBySchemeName("local");
            return scheme.realizeMap(new NullParameterResolver(), dependencies(factory));
            }
        finally
            {
            factory.dispose();
            }
        }

    private void assertCache(String sHigh, String sLow, int cHigh, int cLow)
        {
        var cache = realize(sHigh, sLow);
        assertEquals(cHigh, cache.getHighUnits());
        assertEquals(cLow, cache.getLowUnits());
        if (sLow == null || sLow.isEmpty() || sLow.equals("0") || sLow.contains("%"))
            {
            assertTrue(cache.getLowUnitsRatio() > 0.0);
            cache.setHighUnits(cHigh);
            assertEquals(cLow, cache.getLowUnits());
            }
        }

    private ExtensibleConfigurableCacheFactory factory(String sXml)
        {
        return new ExtensibleConfigurableCacheFactory(
                ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(XmlHelper.loadXml(sXml)));
        }

    private MapBuilder.Dependencies dependencies(ExtensibleConfigurableCacheFactory factory)
        {
        return new MapBuilder.Dependencies(factory, mock(BackingMapManagerContext.class),
                getClass().getClassLoader(), "test", "LocalCache");
        }

    private String configuration(String sHigh, String sLow)
        {
        return """
                <cache-config>
                  <caching-scheme-mapping>
                    <cache-mapping><cache-name>test</cache-name><scheme-name>local</scheme-name></cache-mapping>
                  </caching-scheme-mapping>
                  <caching-schemes>
                    <local-scheme>
                      <scheme-name>local</scheme-name>
                      <high-units>%s</high-units>
                      %s
                    </local-scheme>
                  </caching-schemes>
                </cache-config>
                """.formatted(sHigh, sLow == null ? "" : "<low-units>" + sLow + "</low-units>");
        }

    private static class TestLegacyFactory extends DefaultConfigurableCacheFactory
        {
        @Override
        protected LocalCache instantiateLocalCache(int cUnits, int cExpiryMillis)
            {
            cache = super.instantiateLocalCache(cUnits, cExpiryMillis);
            return cache;
            }

        private LocalCache cache;
        }

    private static final String PROPERTY = "coherence.local.cache.default.prune";
    }
