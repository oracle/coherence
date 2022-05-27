/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.comparator;

import org.glassfish.hk2.api.AnnotationLiteral;
import org.junit.Test;

import javax.annotation.Priority;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PriorityComparatorTest
    {
    @Test
    public void shouldSort()
        {
        List<PriorityHolder> list = new ArrayList<>();
        list.add(new PriorityHolder(10));
        list.add(new PriorityHolder(5));
        list.add(new PriorityHolder(100));
        list.add(new PriorityHolder(20));

        list.sort(new PriorityComparator<>(PriorityHolder::getPriority, 0));

        assertThat(list.get(0).getPriority().value(), is(5));
        assertThat(list.get(1).getPriority().value(), is(10));
        assertThat(list.get(2).getPriority().value(), is(20));
        assertThat(list.get(3).getPriority().value(), is(100));
        }

    @Test
    public void shouldSortServiceLoaderProvider()
        {
        List<ServiceLoader.Provider<Runnable>> list = new ArrayList<>();
        list.add(new ProviderStub(new ServiceOne()));
        list.add(new ProviderStub(new ServiceTwo()));
        list.add(new ProviderStub(new ServiceThree()));
        list.add(new ProviderStub(new ServiceFour()));
        list.add(new ProviderStub(new ServiceFive()));

        list.sort(PriorityComparator.forServiceLoader());

        assertThat(list.get(0).get(), is(instanceOf(ServiceFive.class)));
        assertThat(list.get(1).get(), is(instanceOf(ServiceTwo.class)));
        assertThat(list.get(2).get(), is(instanceOf(ServiceOne.class)));
        assertThat(list.get(3).get(), is(instanceOf(ServiceFour.class)));
        assertThat(list.get(4).get(), is(instanceOf(ServiceThree.class)));
        }

    public static class PriorityHolder
        {
        public PriorityHolder(int nPriority)
            {
            m_priority = new PriorityLiteral(nPriority);
            }

        public Priority getPriority()
            {
            return m_priority;
            }

        private final Priority m_priority;
        }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    public static class PriorityLiteral
            extends AnnotationLiteral<Priority>
            implements Priority
        {
        public PriorityLiteral(int nPriority)
            {
            m_nPriority = nPriority;
            }

        @Override
        public int value()
            {
            return m_nPriority;
            }

        private final int m_nPriority;
        }

    public static class ProviderStub
            implements ServiceLoader.Provider<Runnable>
        {
        public ProviderStub(Runnable f_runnable)
            {
            this.f_runnable = f_runnable;
            }

        @Override
        public Class<? extends Runnable> type()
            {
            return f_runnable.getClass();
            }

        @Override
        public Runnable get()
            {
            return f_runnable;
            }

        private final Runnable f_runnable;
        }

    @Priority(10)
    public static class ServiceOne
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    @Priority(5)
    public static class ServiceTwo
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    @Priority(100)
    public static class ServiceThree
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    @Priority(20)
    public static class ServiceFour
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    public static class ServiceFive
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }
    }
