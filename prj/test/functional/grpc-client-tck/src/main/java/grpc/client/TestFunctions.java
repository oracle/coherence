/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.tangosol.util.function.Remote;

/**
 * Named functions for JSON gRPC tests, where hardened metadata policy rejects
 * the RemoteConstructor representation of dynamic lambdas.
 *
 * @author phf  2026.09.30
 * @since 26.10
 */
public final class TestFunctions
    {
    private TestFunctions()
        {
        }

    /**
     * Append a suffix to an entry value.
     */
    public enum AppendSuffix
            implements Remote.BiFunction<Object, String, String>
        {
        INSTANCE;

        @Override
        public String apply(Object key, String value)
            {
            return value + "1";
            }
        }

    /**
     * Increase a person's age.
     */
    public enum IncreaseAge
            implements Remote.BiFunction<Object, Person, Person>
        {
        INSTANCE;

        @Override
        public Person apply(Object key, Person value)
            {
            value.setAge(value.getAge() + 10);
            return value;
            }
        }

    /**
     * Double an entry value.
     */
    public enum DoubleValue
            implements Remote.BiFunction<Object, Integer, Integer>
        {
        INSTANCE;

        @Override
        public Integer apply(Object key, Integer value)
            {
            return value + value;
            }
        }

    /**
     * Return null to remove a computed entry.
     */
    public enum RemoveValue
            implements Remote.BiFunction<Object, Integer, Integer>
        {
        INSTANCE;

        @Override
        public Integer apply(Object key, Integer value)
            {
            return null;
            }
        }

    /**
     * Return a continuous aggregation result unchanged.
     */
    public enum ReturnResult
            implements Remote.BiFunction<Object, Integer, Integer>
        {
        INSTANCE;

        @Override
        public Integer apply(Object key, Integer result)
            {
            return result;
            }
        }
    }
