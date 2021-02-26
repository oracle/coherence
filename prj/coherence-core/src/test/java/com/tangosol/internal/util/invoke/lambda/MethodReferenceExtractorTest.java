/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke.lambda;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import com.tangosol.util.function.Remote;
import data.repository.Person;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link MethodReferenceExtractor}.
 *
 * @author Aleks Seovic  2021.02.25
 */
@SuppressWarnings("Convert2MethodRef")
public class MethodReferenceExtractorTest
    {
    private Person person = new Person("aleks")
            .name("Aleks")
            .age(46)
            .height(79L)
            .weight(260.0);

    @Test
    public void testCreation()
        {
        ValueExtractor<Person, String> ex = new MethodReferenceExtractor<>(Person::getName);
        assertThat(ex.extract(person), is("Aleks"));
        assertThat(ex.getCanonicalName(), is("name"));
        }

    @Test
    public void testReflectionExtractorEquality()
        {
        ValueExtractor<Person, String> ex1 = new MethodReferenceExtractor<>(Person::getName);
        ValueExtractor<Person, String> ex2 = new ReflectionExtractor<>("getName");

        assertThat(ex1, is(ex2));
        assertThat(ex1.hashCode(), is(ex2.hashCode()));
        }

    @Test
    public void testUniversalExtractorEquality()
        {
        ValueExtractor<Person, String> ex1 = new MethodReferenceExtractor<>(Person::getName);
        ValueExtractor<Person, String> ex2 = new UniversalExtractor<>("name");

        assertThat(ex1, is(ex2));
        assertThat(ex1.hashCode(), is(ex2.hashCode()));
        }

    @Test
    public void testDefaultSerialization()
        {
        ValueExtractor<Person, String> ex1 = new MethodReferenceExtractor<>(Person::getName);
        Binary bin = ExternalizableHelper.toBinary(ex1);

        ValueExtractor<Person, String> ex2 = ExternalizableHelper.fromBinary(bin);
        assertThat(ex1, is(ex2));
        assertThat(ex2.extract(person), is("Aleks"));
        assertThat(ex2.getCanonicalName(), is("name"));
        }

    @Test
    public void testPofSerialization()
        {
        ValueExtractor<Person, String> ex1 = new MethodReferenceExtractor<>(Person::getName);
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1, MethodReferenceExtractor.class, new PortableObjectSerializer(1));
        Binary bin = ExternalizableHelper.toBinary(ex1, ctx);

        ValueExtractor<Person, String> ex2 = ExternalizableHelper.fromBinary(bin, ctx);
        assertThat(ex1, is(ex2));
        assertThat(ex2.extract(person), is("Aleks"));
        assertThat(ex2.getCanonicalName(), is("name"));
        }

    @Test
    public void testEnsureRemotableValueExtractor()
        {
        ValueExtractor<Person, String> ex = Lambdas.ensureRemotable(Person::getName);
        assertThat(ex, instanceOf(MethodReferenceExtractor.class));
        assertThat(ex.extract(person), is("Aleks"));
        assertThat(ex.getCanonicalName(), is("name"));

        ex = Lambdas.ensureRemotable(p -> p.getName());
        assertThat(ex, not(instanceOf(MethodReferenceExtractor.class)));
        assertThat(ex.extract(person), is("Aleks"));
        assertThat(ex.getCanonicalName(), nullValue());
        }

    @Test
    public void testEnsureRemotableFunction()
        {
        Remote.Function<Person, String> ex = Lambdas.ensureRemotable(Person::getName);
        assertThat(ex, instanceOf(MethodReferenceExtractor.class));
        assertThat(ex.apply(person), is("Aleks"));

        ex = Lambdas.ensureRemotable(p -> p.getName());
        assertThat(ex, not(instanceOf(MethodReferenceExtractor.class)));
        assertThat(ex.apply(person), is("Aleks"));
        }

    @Test
    public void testEnsureRemotableToIntFunction()
        {
        Remote.ToIntFunction<Person> ex = Lambdas.ensureRemotable(Person::getAge);
        assertThat(ex, instanceOf(MethodReferenceExtractor.class));
        assertThat(ex.applyAsInt(person), is(46));

        ex = Lambdas.ensureRemotable(p -> p.getAge());
        assertThat(ex, not(instanceOf(MethodReferenceExtractor.class)));
        assertThat(ex.applyAsInt(person), is(46));
        }

    @Test
    public void testEnsureRemotableToLongFunction()
        {
        Remote.ToLongFunction<Person> ex = Lambdas.ensureRemotable(Person::getHeight);
        assertThat(ex, instanceOf(MethodReferenceExtractor.class));
        assertThat(ex.applyAsLong(person), is(79L));

        ex = Lambdas.ensureRemotable(p -> p.getHeight());
        assertThat(ex, not(instanceOf(MethodReferenceExtractor.class)));
        assertThat(ex.applyAsLong(person), is(79L));
        }

    @Test
    public void testEnsureRemotableToDoubleFunction()
        {
        Remote.ToDoubleFunction<Person> ex = Lambdas.ensureRemotable(Person::getWeight);
        assertThat(ex, instanceOf(MethodReferenceExtractor.class));
        assertThat(ex.applyAsDouble(person), is(260.0));

        ex = Lambdas.ensureRemotable(p -> p.getWeight());
        assertThat(ex, not(instanceOf(MethodReferenceExtractor.class)));
        assertThat(ex.applyAsDouble(person), is(260.0));
        }
    }
