/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.util;

import com.oracle.coherence.common.util.Options;
import com.oracle.coherence.common.base.Timeout;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link Options}.
 *
 * @author bko 2014.07.24
 */
public class OptionsTest
    {
    /**
     * Ensure that we can create {@link Options} and request an option
     */
    @Test
    public void shouldCreateAndRequestAnOption()
        {
        Timeout timeout = Timeout.after(5, TimeUnit.MINUTES);

        Options<Timeout> options = Options.from(Timeout.class, timeout);

        assertThat(options.get(Timeout.class), is(timeout));
        }

    /**
     * Ensure that {@link Options} maintain a set by concrete type.
     */
    @Test
    public void shouldMaintainASetByConcreteType()
        {
        Timeout fiveMinutes = Timeout.after(5, TimeUnit.MINUTES);
        Timeout oneSecond   = Timeout.after(1, TimeUnit.SECONDS);

        Options<Timeout> options = Options.from(Timeout.class, fiveMinutes, oneSecond);

        assertThat(options.get(Timeout.class), is(oneSecond));
        }

    /**
     * Ensure that {@link Options} maintain a set by concrete type.
     */
    @Test
    public void shouldMaintainASetIfOptionIsLambda()
        {
        Pet cat = Pet.cat();

        Options<Pet> options = Options.from(Pet.class, cat);

        assertThat(options.get(Pet.class), is(cat));
        }

    /**
     * Ensure that {@link Options} maintain a set by concrete type.
     */
    @Test
    public void shouldMaintainASetIfOptionIsSubclass()
        {
        Mouse mouse = new Mouse();

        Options<Pet> options = Options.from(Pet.class, mouse);

        assertThat(options.get(Mouse.class), is(mouse));
        }

    /**
     * Ensure that {@link Options} maintain a set by concrete type.
     */
    @Test
    public void shouldMaintainASetIfOptionIsAnonymousClass()
        {
        Pet bird = () -> "Bird";

        Options<Pet> options = Options.from(Pet.class, bird);

        assertThat(options.get(Pet.class), is(bird));
        }

    /**
     * Ensure that {@link Options} can return a default using a
     * static method annotated with {@link Options.Default}.
     */
    @Test
    public void shouldDetermineDefaultUsingAnnotatedStaticMethod()
        {
        Options<Duration> options = Options.empty();

        assertThat(options.get(Duration.class), is(Duration.getDefault()));
        }

    /**
     * Ensure that {@link Options} can return a default using a
     * static field annotated with {@link Options.Default}.
     */
    @Test
    public void shouldDetermineDefaultUsingAnnotatedStaticField()
        {
        Options<Device> options = Options.empty();

        assertThat(options.get(Device.class), is(Device.DEFAULT));
        }

    /**
     * Ensure that {@link Options} can return a default using an
     * enum annotated with {@link Options.Default}.
     */
    @Test
    public void shouldDetermineDefaultUsingAnnotatedEnum()
        {
        Options<Meal> options = Options.empty();

        assertThat(options.get(Meal.class), is(Meal.CHICKEN));
        }

    /**
     * Ensure that {@link Options} can return a default using a
     * constructor annotated with {@link Options.Default}.
     */
    @Test
    public void shouldDetermineDefaultUsingAnnotatedConstructor()
        {
        Options<Beverage> options = Options.empty();

        assertThat(options.get(Beverage.class).toString(), is("Beer"));
        }

    @Test
    public void shouldDetermineDefaultUsingLambda() throws Exception
        {
        Options<Pet> options = Options.empty();

        assertThat(options.get(Pet.class), is(notNullValue()));
        assertThat(options.get(Pet.class).getType(), is("Dog"));
        }

    public enum Duration
        {
        SECOND,
        MINUTE,
        HOUR;

        @Options.Default
        public static Duration getDefault()
            {
            return SECOND;
            }
        }

    public enum Device
        {
        CASSETTE,
        FLOPPY,
        TAPE,
        HARD_DRIVE,
        SOLID_STATE_DRIVE;

        @Options.Default
        public static Device DEFAULT = FLOPPY;
        }

    public enum Meal
        {
            TOAST,
            SOUP,
            STEAK,

            @Options.Default
            CHICKEN,

            FISH
        }

    public static class Beverage
        {
        @Options.Default
        public Beverage()
            {
            }

        @Override
        public String toString()
            {
            return "Beer";
            }
        }

    public static interface Animal
        {
        public String getType();

        }

    public static interface Pet
            extends Animal
        {

        public String getType();

        @Options.Default
        public static Pet dog()
            {
            return () -> "Dog";
            }

        public static Pet cat()
            {
            return () -> "Cat";
            }
        }

    public class Mouse
            implements Pet
        {
        @Override
        public String getType()
            {
            return "Mouse";
            }
        }
    }
