/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.tests.streams;


import com.tangosol.coherence.config.Config;
import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.stream.RemoteStream;
import data.pof.Person;
import data.pof.PortablePerson;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.runners.Parameterized;


/**
 * @author Aleksandar Seovic  2015.08.13
 */
public abstract class AbstractStreamTest
    {
    public AbstractStreamTest(boolean fParallel)
        {
        m_fParallel = fParallel;
        }

    protected InvocableMap<String, Person> getPeopleMap()
        {
        return new WrapperNamedCache<>(populateMap(new HashMap<>()), "test");
        }

    protected RemoteStream<InvocableMap.Entry<String, Person>> getStream()
        {
        return getStream((Filter) null);
        }

    protected <E> RemoteStream<E> getStream(ValueExtractor<? super Person, ? extends E> extractor)
        {
        return getStream(null, extractor);
        }

    protected RemoteStream<InvocableMap.Entry<String, Person>> getStream(Filter filter)
        {
        return m_fParallel
               ? getPeopleMap().stream(filter).parallel()
               : getPeopleMap().stream(filter).sequential();
        }

    @SuppressWarnings("unchecked")
    protected <E> RemoteStream<E> getStream(Filter filter, ValueExtractor<? super Person, ? extends E> extractor)
        {
        return m_fParallel
               ? (RemoteStream<E>) getPeopleMap().stream(filter, extractor).parallel()
               : (RemoteStream<E>) getPeopleMap().stream(filter, extractor).sequential();
        }

    protected Map<String, Person> populateMap(Map<String, Person> people)
        {
        PortablePerson aleks    = new PortablePerson("Aleks", DOB_ALEKS, 40);
        PortablePerson marija   = new PortablePerson("Marija", DOB_MARIJA, 36);
        PortablePerson ana      = new PortablePerson("Ana Maria", DOB_ANA, 10);
        PortablePerson novak    = new PortablePerson("Novak", DOB_NOVAK, 6);
        PortablePerson kristina = new PortablePerson("Kristina", DOB_KRISTINA, 1);

        aleks.setChildren(new Person[] {ana, novak, kristina});
        marija.setChildren(new Person[] {ana, novak, kristina});

        people.put("Aleks", aleks);
        people.put("Marija", marija);
        people.put("Ana", ana);
        people.put("Novak", novak);
        people.put("Kristina", kristina);

        return people;
        }

    private static final Date DOB_ALEKS    = new Date( 74,  7, 24);
    private static final Date DOB_MARIJA   = new Date( 78,  1, 20);
    private static final Date DOB_ANA      = new Date(104,  7, 14);
    private static final Date DOB_NOVAK    = new Date(107, 11, 28);
    private static final Date DOB_KRISTINA = new Date(113,  1, 13);

    @Parameterized.Parameters(name = "parallel={0}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][] {{true}, {false}});
        }

    protected boolean m_fParallel;
    }
