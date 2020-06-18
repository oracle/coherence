/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.transformer.ExtractorEventTransformer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2020.06.16
 */
@ExtendWith(WeldJunit5Extension.class)
public class MapEventTransformerIT
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                  .addBeanClass(TransformerBean.class)
                                                  .addBeanClass(TestTransformerFactory.class)
                                                  .addBeanClass(MapEventTransformerProducer.class)
                                                  .addBeanClass(ExtractorProducer.class)
                                                  .addBeanClass(ExtractorProducer.UniversalExtractorSupplier.class)
                                                  .addBeanClass(ExtractorProducer.UniversalExtractorsSupplier.class)
                                                  .addExtension(new CoherenceExtension()));


    @Test
    public void shouldCreateCustomTransformerOne() throws Exception
        {
        TransformerBean bean = weld.select(TransformerBean.class).get();
        assertThat(bean, is(notNullValue()));
        MapEventTransformer<String, String, String> transformer = bean.getTransformerOne();
        assertThat(transformer, is(instanceOf(CustomTransformer.class)));
        assertThat(((CustomTransformer) transformer).getSuffix(), is("foo"));
        }

    @Test
    public void shouldCreateCustomTransformerTwo() throws Exception
        {
        TransformerBean bean = weld.select(TransformerBean.class).get();
        assertThat(bean, is(notNullValue()));
        MapEventTransformer<String, String, String> transformer = bean.getTransformerTwo();
        assertThat(transformer, is(instanceOf(CustomTransformer.class)));
        assertThat(((CustomTransformer) transformer).getSuffix(), is("bar"));
        }

    @Test
    @SuppressWarnings("rawtypes")
    public void shouldCreateExtractorTransformer() throws Exception
        {
        TransformerBean bean = weld.select(TransformerBean.class).get();
        assertThat(bean, is(notNullValue()));
        MapEventTransformer<String, String, String> transformer = bean.getTransformerThree();
        assertThat(transformer, is(instanceOf(ExtractorEventTransformer.class)));
        ValueExtractor extractor = ((ExtractorEventTransformer) transformer).getNewValueExtractor();
        assertThat(extractor, is(instanceOf(UniversalExtractor.class)));
        assertThat(((UniversalExtractor) extractor).getName(), is("foo"));
        }

    @Test
    @SuppressWarnings("rawtypes")
    public void shouldCreateMultiExtractorTransformer() throws Exception
        {
        TransformerBean bean = weld.select(TransformerBean.class).get();
        assertThat(bean, is(notNullValue()));
        MapEventTransformer<String, String, String> transformer = bean.getTransformerFour();
        assertThat(transformer, is(instanceOf(ExtractorEventTransformer.class)));
        ValueExtractor extractor = ((ExtractorEventTransformer) transformer).getNewValueExtractor();
        assertThat(extractor, is(instanceOf(MultiExtractor.class)));
        }

    @Inherited
    @MapEventTransformerBinding
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestTransformer
        {
        @Nonbinding String value() default "foo";
        }

    @ApplicationScoped
    @TestTransformer
    public static class TestTransformerFactory
        implements MapEventTransformerFactory<TestTransformer, String, String, String>
        {
        @Override
        public MapEventTransformer<String, String, String> create(TestTransformer annotation)
            {
            return new CustomTransformer(annotation.value());
            }
        }

    @ApplicationScoped
    private static class TransformerBean
        {
        public MapEventTransformer<String, String, String> getTransformerOne()
            {
            return m_transformerOne;
            }

        public MapEventTransformer<String, String, String> getTransformerTwo()
            {
            return m_transformerTwo;
            }

        public MapEventTransformer<String, String, String> getTransformerThree()
            {
            return m_transformerThree;
            }

        public MapEventTransformer<String, String, String> getTransformerFour()
            {
            return m_transformerFour;
            }

        @Inject
        @TestTransformer
        private MapEventTransformer<String, String, String> m_transformerOne;

        @Inject
        @TestTransformer("bar")
        private MapEventTransformer<String, String, String> m_transformerTwo;

        @Inject
        @PropertyExtractor("foo")
        private MapEventTransformer<String, String, String> m_transformerThree;

        @Inject
        @PropertyExtractor("one")
        @PropertyExtractor("two")
        private MapEventTransformer<String, String, String> m_transformerFour;
        }

    private static class CustomTransformer
            implements MapEventTransformer<String, String, String>
        {
        public CustomTransformer(String sSuffix)
            {
            m_sSuffix = sSuffix;
            }

        @Override
        @SuppressWarnings("unchecked")
        public MapEvent<String, String> transform(MapEvent<String, String> event)
            {
            String sOld = transform(event.getOldValue());
            String sNew = transform(event.getNewValue());
            return new MapEvent<String, String>(event.getMap(), event.getId(), event.getKey(), sOld, sNew);
            }

        public String transform(String sValue)
            {
            return sValue == null ? null : sValue + "-" + m_sSuffix;
            }

        public String getSuffix()
            {
            return m_sSuffix;
            }

        private final String m_sSuffix;
        }
    }
