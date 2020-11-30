/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.inject.ConfigUri;
import com.oracle.coherence.inject.Name;
import com.oracle.coherence.cdi.data.Person;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.ConfigurablePofContext;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link SerializerProducer} using the
 * Weld JUnit extension.
 *
 * @author Jonathan Knight  2019.10.19
*/
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SerializerProducerIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addBeanClass(CustomSerializer.class)
                                                          .addBeanClass(CtorBean.class)
                                                          .addBeanClass(SerializerFieldsBean.class)
                                                          .addBeanClass(SerializerProducer.class));

    @Test
    void shouldGetDynamicSerializer()
        {
        Annotation qualifier = Name.Literal.of("java");
        Instance<Serializer> instance = weld.select(Serializer.class, qualifier);

        assertThat(instance.isResolvable(), is(true));
        assertThat(instance.get(), is(instanceOf(DefaultSerializer.class)));
        }

    @Test
    void shouldInjectDefaultSerializerUsingFieldName()
        {
        SerializerFieldsBean bean = weld.select(SerializerFieldsBean.class).get();
        assertThat(bean.getDefaultSerializer(), is(notNullValue()));
        assertThat(bean.getDefaultSerializer(), is(instanceOf(DefaultSerializer.class)));
        }

    @Test
    void shouldInjectJavaSerializerUsingFieldName()
        {
        SerializerFieldsBean bean = weld.select(SerializerFieldsBean.class).get();
        assertThat(bean.getJava(), is(notNullValue()));
        assertThat(bean.getJava(), is(instanceOf(DefaultSerializer.class)));
        }

    @Test
    void shouldInjectPofSerializerUsingFieldName()
        {
        SerializerFieldsBean bean = weld.select(SerializerFieldsBean.class).get();
        assertThat(bean.getPof(), is(notNullValue()));
        assertThat(bean.getPof(), is(instanceOf(ConfigurablePofContext.class)));
        }

    @Test
    void shouldInjectPofSerializerWithConfigUsingFieldName()
        {
        SerializerFieldsBean bean = weld.select(SerializerFieldsBean.class).get();
        Serializer serializer = bean.getPofWithURI();
        assertThat(serializer, is(notNullValue()));
        assertThat(serializer, is(instanceOf(ConfigurablePofContext.class)));
        ConfigurablePofContext pofContext = (ConfigurablePofContext) serializer;
        int id = pofContext.getUserTypeIdentifier(Person.class);
        assertThat(id, is(not(0)));
        }

    @Test
    void shouldInjectCustomSerializerUsingFieldName()
        {
        SerializerFieldsBean bean = weld.select(SerializerFieldsBean.class).get();
        assertThat(bean.getCustom(), is(notNullValue()));
        assertThat(bean.getCustom(), is(instanceOf(CustomSerializer.class)));
        }

    @Test
    void testCtorInjection()
        {
        CtorBean bean = weld.select(CtorBean.class).get();

        assertThat(bean.getSerializer(), notNullValue());
        assertThat(bean.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        }

    // ----- test beans -----------------------------------------------------

    @ApplicationScoped
    private static class SerializerFieldsBean
        {
        @Inject
        @Name("")
        private Serializer defaultSerializer;

        @Inject
        @Name("java")
        private Serializer java;

        @Inject
        @Name("pof")
        private Serializer pof;

        @Inject
        @Name("pof")
        @ConfigUri("test-pof-config.xml")
        private Serializer pofWithURI;

        @Inject
        @Name("custom")
        private Serializer custom;

        Serializer getDefaultSerializer()
            {
            return defaultSerializer;
            }

        Serializer getJava()
            {
            return java;
            }

        Serializer getPof()
            {
            return pof;
            }

        Serializer getPofWithURI()
            {
            return pofWithURI;
            }

        Serializer getCustom()
            {
            return custom;
            }
        }

    @ApplicationScoped
    static class CtorBean
        {
        private final Serializer serializer;

        @Inject
        CtorBean(@Name("pof") Serializer serializer)
            {
            this.serializer = serializer;
            }

        public Serializer getSerializer()
            {
            return serializer;
            }
        }

    @Named("custom")
    @ApplicationScoped
    static class CustomSerializer
            implements Serializer
        {
        @Override
        public void serialize(WriteBuffer.BufferOutput out, Object o)
            {
            }
        }
    }
