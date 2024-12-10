/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cdi;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.util.Nonbinding;

import com.oracle.coherence.cdi.AnnotationInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for the {@link AnnotationInstance} class.
 *
 * @author Jonathan Knight  2019.10.24
 */
public class AnnotationInstanceTest
    {

    static Stream<Annotation> getBeanAnnotations()
        {
        return Stream.of(BeanOne.class.getAnnotations());
        }

    static Stream<Arguments> getBeanOneBeanTwoAnnotationsPairs()
        {
        List<Arguments> arguments = new ArrayList<>();
        Annotation[] annotations = BeanOne.class.getAnnotations();
        for (Annotation annotation : annotations)
            {
            arguments.add(Arguments.of(annotation, BeanTwo.class.getAnnotation(annotation.annotationType())));
            }
        return arguments.stream();
        }

    static Stream<Arguments> getBeanOneBeanThreeAnnotationsPairs()
        {
        List<Arguments> arguments = new ArrayList<>();
        Annotation[] annotations = BeanOne.class.getAnnotations();
        for (Annotation annotation : annotations)
            {
            arguments.add(Arguments.of(annotation, BeanThree.class.getAnnotation(annotation.annotationType())));
            }
        return arguments.stream();
        }

    @Test
    void shouldBeEqualForSimpleAnnotation()
        {
        AnnotationInstance one = AnnotationInstance.create(Simple.class.getAnnotation(AnnOne.class));
        AnnotationInstance two = AnnotationInstance.create(Simple.class.getAnnotation(AnnOne.class));

        assertThat(one.equals(two), is(true));
        assertThat(two.equals(one), is(true));
        }

    @Test
    void shouldHaveZeroHashCodeSimpleAnnotation()
        {
        AnnotationInstance instance = AnnotationInstance.create(Simple.class.getAnnotation(AnnOne.class));
        assertThat(instance.hashCode(), is(0));
        }

    @ParameterizedTest
    @MethodSource("getBeanAnnotations")
    void shouldBeEqual(Annotation annotation)
        {
        AnnotationInstance one = AnnotationInstance.create(annotation);
        AnnotationInstance two = AnnotationInstance.create(annotation);

        assertThat(one.equals(two), is(true));
        assertThat(two.equals(one), is(true));
        }

    @Test
    void shouldBNoteEqualIfDifferentAnnotation()
        {
        AnnotationInstance one = AnnotationInstance.create(BeanOne.class.getAnnotation(AnnString.class));
        AnnotationInstance two = AnnotationInstance.create(BeanOne.class.getAnnotation(AnnFloat.class));

        assertThat(one.equals(two), is(false));
        assertThat(two.equals(one), is(false));
        }

    @Test
    void shouldBNoteEqualToNonAnnotationInstance()
        {
        AnnotationInstance one = AnnotationInstance.create(BeanOne.class.getAnnotation(AnnString.class));

        assertThat(one.equals("foo"), is(false));
        }

    @Test
    void shouldBNoteEqualToNull()
        {
        AnnotationInstance one = AnnotationInstance.create(BeanOne.class.getAnnotation(AnnString.class));

        assertThat(one.equals(null), is(false));
        }

    @ParameterizedTest
    @MethodSource("getBeanAnnotations")
    void shouldHaveSameHashCode(Annotation annotation)
        {
        AnnotationInstance one = AnnotationInstance.create(annotation);
        AnnotationInstance two = AnnotationInstance.create(annotation);

        assertThat(one.hashCode(), is(two.hashCode()));
        }

    @ParameterizedTest
    @MethodSource("getBeanAnnotations")
    void shouldHaveToString(Annotation annotation)
        {
        AnnotationInstance instance = AnnotationInstance.create(annotation);

        assertThat(instance.toString(), is(notNullValue()));
        assertThat(instance.toString(), is(not("")));
        }

    @ParameterizedTest
    @MethodSource("getBeanOneBeanTwoAnnotationsPairs")
    void shouldNotBeEqual(Annotation annotationOne, Annotation annotationTwo)
        {
        AnnotationInstance one = AnnotationInstance.create(annotationOne);
        AnnotationInstance two = AnnotationInstance.create(annotationTwo);

        assertThat(one.equals(two), is(false));
        assertThat(two.equals(one), is(false));
        }

    @ParameterizedTest
    @MethodSource("getBeanOneBeanTwoAnnotationsPairs")
    void shouldNotHaveSameHashCode(Annotation annotationOne, Annotation annotationTwo)
        {
        AnnotationInstance one = AnnotationInstance.create(annotationOne);
        AnnotationInstance two = AnnotationInstance.create(annotationTwo);

        assertThat(one.hashCode(), is(not(two.hashCode())));
        }

    @ParameterizedTest
    @MethodSource("getBeanOneBeanThreeAnnotationsPairs")
    void shouldBeEqualIgnoringNonBindingValues(Annotation annotationOne, Annotation annotationTwo)
        {
        AnnotationInstance one = AnnotationInstance.create(annotationOne);
        AnnotationInstance two = AnnotationInstance.create(annotationTwo);

        assertThat(one.equals(two), is(true));
        assertThat(two.equals(one), is(true));
        }

    @ParameterizedTest
    @MethodSource("getBeanOneBeanThreeAnnotationsPairs")
    void shouldHaveSameHashCodeIgnoringNonBindingValues(Annotation annotationOne, Annotation annotationTwo)
        {
        AnnotationInstance one = AnnotationInstance.create(annotationOne);
        AnnotationInstance two = AnnotationInstance.create(annotationTwo);

        assertThat(one.hashCode(), is(two.hashCode()));
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnOne
        {
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnByteArray
        {
        byte[] value() default {1, 2, 3};

        @Nonbinding byte[] nonBinding() default {1, 2, 3};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnShortArray
        {
        short[] value() default {4, 5, 6};

        @Nonbinding short[] nonBinding() default {4, 5, 6};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnIntArray
        {
        int[] value() default {7, 8, 9};

        @Nonbinding int[] nonBinding() default {7, 8, 9};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnLongArray
        {
        long[] value() default {10L, 11L, 12L};

        @Nonbinding long[] nonBinding() default {10L, 11L, 12L};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnFloatArray
        {
        float[] value() default {13.1f, 14.2f, 15.3f};

        @Nonbinding float[] nonBinding() default {13.1f, 14.2f, 15.3f};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnDoubleArray
        {
        double[] value() default {16.1d, 17.2d, 18.3d};

        @Nonbinding double[] nonBinding() default {16.1d, 17.2d, 18.3d};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnCharArray
        {
        char[] value() default {'a', 'b', 'c'};

        @Nonbinding char[] nonBinding() default {'a', 'b', 'c'};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnBooleanArray
        {
        boolean[] value() default {true, false};

        @Nonbinding boolean[] nonBinding() default {true, false};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnStringArray
        {
        String[] value() default {"one", "two", "three"};

        @Nonbinding String[] nonBinding() default {"one", "two", "three"};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnClassArray
        {
        Class[] value() default {Integer.class, Byte.class};

        @Nonbinding Class[] nonBinding() default {Integer.class, Byte.class};
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnByte
        {
        byte value() default 1;

        @Nonbinding byte nonBinding() default 1;
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnShort
        {
        short value() default 2;

        @Nonbinding short nonBinding() default 2;
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnInt
        {
        int value() default 3;

        @Nonbinding int nonBinding() default 3;
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnLong
        {
        long value() default 4L;

        @Nonbinding long nonBinding() default 4L;
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnFloat
        {
        float value() default 5.6f;

        @Nonbinding float nonBinding() default 5.6f;
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnDouble
        {
        double value() default 7.8f;

        @Nonbinding double nonBinding() default 7.8f;
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnChar
        {
        char value() default 'z';

        @Nonbinding char nonBinding() default 'z';
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnBoolean
        {
        boolean value() default true;

        @Nonbinding boolean nonBinding() default true;
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnString
        {
        String value() default "foo";

        @Nonbinding String nonBinding() default "foo";
        }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnClass
        {
        Class value() default String.class;

        @Nonbinding Class nonBinding() default String.class;
        }

    @AnnOne
    static class Simple
        {
        }

    @AnnByteArray
    @AnnShortArray
    @AnnIntArray
    @AnnLongArray
    @AnnFloatArray
    @AnnDoubleArray
    @AnnCharArray
    @AnnBooleanArray
    @AnnStringArray
    @AnnClassArray
    @AnnByte
    @AnnShort
    @AnnInt
    @AnnLong
    @AnnFloat
    @AnnDouble
    @AnnChar
    @AnnBoolean
    @AnnString
    @AnnClass
    static class BeanOne
        {
        }

    @AnnByteArray(value = {101, 102, 103})
    @AnnShortArray(value = {104, 105, 106})
    @AnnIntArray(value = {107, 108, 109})
    @AnnLongArray(value = {110L, 111L, 112L})
    @AnnFloatArray(value = {113.1f, 114.2f, 115.3f})
    @AnnDoubleArray(value = {116.1d, 117.2d, 118.3d})
    @AnnCharArray(value = {'x', 'y', 'z'})
    @AnnBooleanArray(value = {false, true})
    @AnnStringArray(value = {"three", "four"})
    @AnnClassArray(value = {Float.class, Double.class})
    @AnnByte(value = 100)
    @AnnShort(value = 100)
    @AnnInt(value = 300)
    @AnnLong(value = 400L)
    @AnnFloat(value = 500.1f)
    @AnnDouble(value = 600.1d)
    @AnnChar(value = 'n')
    @AnnBoolean(value = false)
    @AnnString(value = "bar")
    @AnnClass(value = Boolean.class)
    static class BeanTwo
        {
        }

    @AnnByteArray(nonBinding = {101, 102, 103})
    @AnnShortArray(nonBinding = {104, 105, 106})
    @AnnIntArray(nonBinding = {107, 108, 109})
    @AnnLongArray(nonBinding = {110L, 111L, 112L})
    @AnnFloatArray(nonBinding = {113.1f, 114.2f, 115.3f})
    @AnnDoubleArray(nonBinding = {116.1d, 117.2d, 118.3d})
    @AnnCharArray(nonBinding = {'x', 'y', 'z'})
    @AnnBooleanArray(nonBinding = {false, true})
    @AnnStringArray(nonBinding = {"three", "four"})
    @AnnClassArray(nonBinding = {Float.class, Double.class})
    @AnnByte(nonBinding = 100)
    @AnnShort(nonBinding = 100)
    @AnnInt(nonBinding = 300)
    @AnnLong(nonBinding = 400L)
    @AnnFloat(nonBinding = 500.1f)
    @AnnDouble(nonBinding = 600.1d)
    @AnnChar(nonBinding = 'n')
    @AnnBoolean(nonBinding = false)
    @AnnString(nonBinding = "bar")
    @AnnClass(nonBinding = Boolean.class)
    static class BeanThree
        {
        }
    }
