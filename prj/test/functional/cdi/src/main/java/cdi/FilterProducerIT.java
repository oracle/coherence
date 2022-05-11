/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.util.Nonbinding;

import javax.inject.Inject;

import com.oracle.coherence.cdi.AlwaysFilter;
import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.FilterBinding;
import com.oracle.coherence.cdi.FilterFactory;
import com.oracle.coherence.cdi.WhereFilter;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.AllFilter;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for filter producers and annotations.
 *
 * @author Jonathan Knight  2019.10.24
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilterProducerIT
    {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addPackages(CoherenceExtension.class)
                                                          .addExtension(new CoherenceExtension())
                                                          .addBeanClass(TestServerCoherenceProducer.class)
                                                          .addBeanClass(TestFilterFactory.class)
                                                          .addBeanClass(FilterBean.class));

    @Test
    void shouldInjectAlwaysFilter()
        {
        FilterBean filterBean = weld.select(FilterBean.class).get();
        assertThat(filterBean, is(notNullValue()));
        assertThat(filterBean.getAlwaysFilter(), is(instanceOf(com.tangosol.util.filter.AlwaysFilter.class)));
        }

    @Test
    void shouldInjectFilterFromCohQL()
        {
        FilterBean filterBean = weld.select(FilterBean.class).get();
        assertThat(filterBean, is(notNullValue()));

        Filter<BeanOne> filter = filterBean.getWhereFilter();
        assertThat(filter, is(notNullValue()));

        BeanOne one = new BeanOne("foo", new BeanTwo(100));
        BeanOne two = new BeanOne("foo", new BeanTwo(200));

        assertThat(filter.evaluate(one), is(true));
        assertThat(filter.evaluate(two), is(false));
        }

    @Test
    void shouldInjectCustomFilter()
        {
        FilterBean filterBean = weld.select(FilterBean.class).get();
        assertThat(filterBean, is(notNullValue()));

        Filter<BeanOne> filter = filterBean.getCustomFilter();
        assertThat(filter, is(notNullValue()));

        BeanOne one = new BeanOne("foo", new BeanTwo(100));
        BeanOne two = new BeanOne("bar", new BeanTwo(100));

        assertThat(filter.evaluate(one), is(true));
        assertThat(filter.evaluate(two), is(false));
        }

    @Test
    void shouldInjectAndFilter()
        {
        FilterBean filterBean = weld.select(FilterBean.class).get();
        assertThat(filterBean, is(notNullValue()));

        Filter<BeanOne> filter = filterBean.getAndFilter();
        assertThat(filter, is(instanceOf(AllFilter.class)));

        BeanOne one = new BeanOne("bar", new BeanTwo(19));
        BeanOne two = new BeanOne("bar", new BeanTwo(100));
        BeanOne three = new BeanOne("foo", new BeanTwo(19));

        assertThat(filter.evaluate(one), is(true));
        assertThat(filter.evaluate(two), is(false));
        assertThat(filter.evaluate(three), is(false));
        }

    // ----- helper classes -------------------------------------------------

    @Inherited
    @FilterBinding
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestFilter
        {
        @Nonbinding String value() default "foo";
        }
    @TestFilter
    @ApplicationScoped
    public static class TestFilterFactory
            implements FilterFactory<TestFilter, BeanOne>
        {
        @Override
        public Filter<BeanOne> create(TestFilter annotation)
            {
            ValueExtractor<BeanOne, String> extractor = BeanOne::getField;
            return Filters.equal(extractor, annotation.value());
            }
        }
    @ApplicationScoped
    private static class FilterBean
        {
        @Inject
        @AlwaysFilter
        private Filter<String> alwaysFilter;

        @Inject
        @TestFilter
        private Filter<BeanOne> customFilter;

        @Inject
        @WhereFilter("beanTwo.field = 100")
        private Filter<BeanOne> whereFilter;

        @Inject
        @WhereFilter("beanTwo.field = 19")
        @TestFilter("bar")
        private Filter<BeanOne> andFilter;

        public Filter<String> getAlwaysFilter()
            {
            return alwaysFilter;
            }

        public Filter<BeanOne> getCustomFilter()
            {
            return customFilter;
            }

        public Filter<BeanOne> getWhereFilter()
            {
            return whereFilter;
            }

        public Filter<BeanOne> getAndFilter()
            {
            return andFilter;
            }
        }

    public class BeanOne
        {
        private String field;

        private BeanTwo beanTwo;

        private BeanOne(String field, BeanTwo beanTwo)
            {
            this.field = field;
            this.beanTwo = beanTwo;
            }

        public String getField()
            {
            return field;
            }

        public BeanTwo getBeanTwo()
            {
            return beanTwo;
            }
        }

    public class BeanTwo
        {
        private int field;

        private BeanTwo(int field)
            {
            this.field = field;
            }

        public int getField()
            {
            return field;
            }

        public void setField(int field)
            {
            this.field = field;
            }
        }
    }
