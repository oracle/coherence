/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.oracle.coherence.cdi.data.Person;
import com.oracle.coherence.cdi.data.PhoneNumber;

import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.cache.BackingMapBinaryEntry;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.MultiExtractor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ValueExtractorProducerTest
    {

    private static ConfigurablePofContext pofContext = new ConfigurablePofContext("test-pof-config.xml");

    private static Person person;

    private static PhoneNumber phoneNumber;

    private static Binary binaryKey;

    private static Binary binaryPerson;

    private static Map.Entry<String, Person> entry;

    @BeforeAll
    static void setup()
        {
        phoneNumber = new PhoneNumber(44, "04242424242");
        person = new Person("Arthur", "Dent",
                            LocalDate.of(1978, 3, 8),
                            phoneNumber);

        binaryKey = ExternalizableHelper.toBinary("AD", pofContext);
        binaryPerson = ExternalizableHelper.toBinary(person, pofContext);

        BackingMapContext ctx = mock(BackingMapContext.class);
        Map<ValueExtractor, MapIndex> index = new HashMap<>();

        when(ctx.getIndexMap()).thenReturn(index);

        entry = new BackingMapBinaryEntry(binaryKey, binaryPerson, binaryPerson, null)
            {
            @Override
            public Object getKey()
                {
                return "AD";
                }

            @Override
            public Object getValue()
                {
                return person;
                }

            @Override
            public BackingMapContext getBackingMapContext()
                {
                return ctx;
                }

            @Override
            public Serializer getSerializer()
                {
                return pofContext;
                }
            };
        }

    @Test
    void shouldCreatePropertyExtractor()
        {
        PropertyExtractor annotation = PropertyExtractor.Literal.of("firstName");
        ExtractorProducer.UniversalExtractorSupplier supplier = new ExtractorProducer.UniversalExtractorSupplier();
        ValueExtractor extractor = supplier.create(annotation);

        assertThat(extractor, is(notNullValue()));
        assertThat(extractor.extract(person), is(person.getFirstName()));
        }

    @Test
    void shouldCreateMultiPropertyExtractor()
        {
        PropertyExtractor annotation1 = PropertyExtractor.Literal.of("firstName");
        PropertyExtractor annotation2 = PropertyExtractor.Literal.of("lastName");
        PropertyExtractor.Extractors annotation = PropertyExtractor.Extractors.Literal.of(annotation1, annotation2);
        ExtractorProducer.UniversalExtractorsSupplier supplier = new ExtractorProducer.UniversalExtractorsSupplier();
        ValueExtractor extractor = supplier.create(annotation);

        assertThat(extractor, is(notNullValue()));
        Object extracted = extractor.extract(person);
        assertThat(extracted, is(instanceOf(Iterable.class)));
        assertThat(((Iterable<String>) extracted), contains(person.getFirstName(), person.getLastName()));
        }

    @Test
    void shouldCreateChainedExtractor()
        {
        ChainedExtractor annotation = ChainedExtractor.Literal.of("phoneNumber", "countryCode");
        ExtractorProducer.ChainedExtractorSupplier supplier = new ExtractorProducer.ChainedExtractorSupplier();
        ValueExtractor extractor = supplier.create(annotation);

        assertThat(extractor, is(notNullValue()));
        assertThat(extractor.extract(person), is(person.getPhoneNumber().getCountryCode()));
        }

    @Test
    void shouldCreateMultiChainedExtractor()
        {
        ChainedExtractor annotation1 = ChainedExtractor.Literal.of("phoneNumber", "countryCode");
        ChainedExtractor annotation2 = ChainedExtractor.Literal.of("phoneNumber", "number");
        ChainedExtractor.Extractors annotation = ChainedExtractor.Extractors.Literal.of(annotation1, annotation2);
        ExtractorProducer.ChainedExtractorsSupplier supplier = new ExtractorProducer.ChainedExtractorsSupplier();
        ValueExtractor extractor = supplier.create(annotation);

        assertThat(extractor, is(notNullValue()));

        Object extracted = extractor.extract(person);
        assertThat(extracted, is(instanceOf(Iterable.class)));
        assertThat(((Iterable<String>) extracted), contains(phoneNumber.getCountryCode(), phoneNumber.getNumber()));
        }

    @Test
    void shouldCreatePofExtractor()
        {
        PofExtractor annotation = PofExtractor.Literal.of(Integer.class, 3, 0);
        ExtractorProducer.PofExtractorSupplier supplier = new ExtractorProducer.PofExtractorSupplier();
        ValueExtractor extractor = supplier.create(annotation);

        assertThat(extractor, is(notNullValue()));

        assertThat(InvocableMapHelper.extractFromEntry(extractor, entry), is(person.getPhoneNumber().getCountryCode()));
        }

    @Test
    void shouldCreateMultiPofExtractor()
        {
        PofExtractor annotation1 = PofExtractor.Literal.of(3, 0);
        PofExtractor annotation2 = PofExtractor.Literal.of(3, 1);
        PofExtractor.Extractors annotation = PofExtractor.Extractors.Literal.of(annotation1, annotation2);
        ExtractorProducer.PofExtractorsSupplier supplier = new ExtractorProducer.PofExtractorsSupplier();
        ValueExtractor extractor = supplier.create(annotation);

        assertThat(extractor, is(instanceOf(MultiExtractor.class)));

        BackingMapContext ctx = mock(BackingMapContext.class);
        Map<ValueExtractor, MapIndex> index = new HashMap<>();

        when(ctx.getIndexMap()).thenReturn(index);

        BinaryEntry entry = new BackingMapBinaryEntry(binaryKey, binaryPerson, binaryPerson, null)
            {
            @Override
            public BackingMapContext getBackingMapContext()
                {
                return ctx;
                }
            };

        Object extracted = InvocableMapHelper.extractFromEntry(extractor, entry);

        assertThat(extracted, is(instanceOf(Iterable.class)));
        assertThat((Iterable<String>) extracted, contains(phoneNumber.getCountryCode(), phoneNumber.getNumber()));
        }
    }
