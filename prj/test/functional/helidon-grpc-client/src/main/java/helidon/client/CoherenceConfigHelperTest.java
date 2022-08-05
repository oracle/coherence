/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package helidon.client;

import com.oracle.coherence.helidon.client.CoherenceConfigHelper;
import io.helidon.config.ClasspathConfigSource;
import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.12.18
 */
@SuppressWarnings("unchecked")
public class CoherenceConfigHelperTest
    {
    @Test
    public void shouldHaveEmptyCoherenceConfigurationIfNoConfigBean()
        {
        BeanManager      beanManager = mock(BeanManager.class);
        Instance<Object> instance    = mock(Instance.class);
        Instance<Config> instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instance.stream()).thenReturn(Stream.empty());

        CoherenceConfigHelper helper = new CoherenceConfigHelper(beanManager);
        assertThat(helper.getCoherenceConfig(), is(notNullValue()));
        assertThat(helper.getCoherenceConfig().exists(), is(false));
        }

    @Test
    public void shouldHaveEmptyCoherenceConfigurationIfConfigBeanIsEmpty()
        {
        Config           config      = Config.empty();
        BeanManager      beanManager = mock(BeanManager.class);
        Instance<Object> instance    = mock(Instance.class);
        Instance<Config> instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        CoherenceConfigHelper helper = new CoherenceConfigHelper(beanManager);
        assertThat(helper.getCoherenceConfig(), is(notNullValue()));
        assertThat(helper.getCoherenceConfig().exists(), is(false));
        }

    @Test
    public void shouldHaveEmptyCoherenceConfigurationIfNoCoherenceConfig()
        {
        ConfigSource     source      = ClasspathConfigSource.create("config-test-no-coherence.yaml");
        Config           config      = Config.builder(source)
                                             .disableEnvironmentVariablesSource()
                                             .disableSystemPropertiesSource()
                                             .build();
        BeanManager      beanManager = mock(BeanManager.class);
        Instance<Object> instance    = mock(Instance.class);
        Instance<Config> instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        CoherenceConfigHelper helper = new CoherenceConfigHelper(beanManager);
        assertThat(helper.getCoherenceConfig(), is(notNullValue()));
        assertThat(helper.getCoherenceConfig().exists(), is(false));
        }

    @Test
    public void shouldHaveCorrectCoherenceConfiguration()
        {
        ConfigSource     source      = ClasspathConfigSource.create("config-test-with-coherence.yaml");
        Config           config      = Config.builder(source)
                                             .disableEnvironmentVariablesSource()
                                             .disableSystemPropertiesSource()
                                             .build();
        BeanManager      beanManager = mock(BeanManager.class);
        Instance<Object> instance    = mock(Instance.class);
        Instance<Config> instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        CoherenceConfigHelper helper = new CoherenceConfigHelper(beanManager);
        assertThat(helper.getCoherenceConfig(), is(notNullValue()));
        assertThat(helper.getCoherenceConfig().exists(), is(true));
        assertThat(helper.getCoherenceConfig().get("service.name").exists(), is(true));
        assertThat(helper.getCoherenceConfig().get("service.name").asString().get(), is("Foo"));
        }

    @Test
    public void shouldHaveNoSessionConfigurations()
        {
        ConfigSource     source      = ClasspathConfigSource.create("config-test-with-coherence.yaml");
        Config           config      = Config.builder(source)
                                             .disableEnvironmentVariablesSource()
                                             .disableSystemPropertiesSource()
                                             .build();
        BeanManager      beanManager = mock(BeanManager.class);
        Instance<Object> instance    = mock(Instance.class);
        Instance<Config> instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        CoherenceConfigHelper helper   = new CoherenceConfigHelper(beanManager);
        Map<String, Config>   sessions = helper.getSessions();
        assertThat(sessions, is(notNullValue()));
        assertThat(sessions.isEmpty(), is(true));
        }

    @Test
    public void shouldHaveOneSessionConfigurations()
        {
        ConfigSource     source      = ClasspathConfigSource.create("config-test-with-one-session.yaml");
        Config           config      = Config.builder(source)
                                             .disableEnvironmentVariablesSource()
                                             .disableSystemPropertiesSource()
                                             .build();
        BeanManager      beanManager = mock(BeanManager.class);
        Instance<Object> instance    = mock(Instance.class);
        Instance<Config> instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        CoherenceConfigHelper helper   = new CoherenceConfigHelper(beanManager);
        Map<String, Config>   sessions = helper.getSessions();
        assertThat(sessions, is(notNullValue()));
        assertThat(sessions.size(), is(1));
        }

    @Test
    public void shouldHaveMultipleSessionConfigurations()
        {
        ConfigSource     source      = ClasspathConfigSource.create("config-test-with-multiple-sessions.yaml");
        Config           config      = Config.builder(source)
                                             .disableEnvironmentVariablesSource()
                                             .disableSystemPropertiesSource()
                                             .build();
        BeanManager      beanManager = mock(BeanManager.class);
        Instance<Object> instance    = mock(Instance.class);
        Instance<Config> instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        CoherenceConfigHelper helper   = new CoherenceConfigHelper(beanManager);
        Map<String, Config>   sessions = helper.getSessions();
        assertThat(sessions, is(notNullValue()));
        assertThat(sessions.size(), is(2));
        }
    }
