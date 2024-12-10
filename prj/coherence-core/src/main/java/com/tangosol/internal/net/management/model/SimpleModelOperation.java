/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management.model;

import javax.management.MBeanOperationInfo;

import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * A simple operation that can be executed on an {@link AbstractModel MBean model}.
 *
 * @param <M> the type of the model the operations will execute on
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SimpleModelOperation<M>
        implements ModelOperation<M>
    {
    /**
     * Create a {@link SimpleModelOperation}.
     *
     * @param builder the {@link Builder} to use to configure the operation
     */
    private SimpleModelOperation(Builder<M> builder)
        {
        f_sName        = builder.f_sName;
        f_sDescription = builder.m_sDescription;
        f_aParams      = builder.m_listParams.toArray(new OpenMBeanParameterInfo[0]);
        f_typeReturn   = builder.m_typeReturn;
        f_function     = builder.m_function;
        }

    // ----- accessors ------------------------------------------------------

    @Override
    public String getName()
        {
        return f_sName;
        }

    @Override
    public BiFunction<M, Object[], ?> getFunction()
        {
        return f_function;
        }

    @Override
    public MBeanOperationInfo getOperation()
        {
        MBeanOperationInfo info = m_info;
        if (info == null)
            {
            info = m_info = new OpenMBeanOperationInfoSupport(f_sName, f_sDescription, f_aParams,
                    f_typeReturn, OpenMBeanOperationInfoSupport.ACTION);
            }
        return info;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create an operation {@link Builder}.
     *
     * @param sName  the name of the operation
     *
     * @return a builder
     */
    public static <M> Builder<M> builder(String sName, Class<? extends M> ignored)
        {
        return new Builder<>(sName);
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder that builds {@link SimpleModelOperation} instances.
     */
    public static class Builder<M>
        {
        /**
         * Private constructor.
         *
         * @param sName  the operation name
         */
        private Builder(String sName)
            {
            f_sName = sName;
            }

        /**
         * Set the operation description.
         *
         * @param sDescription  the operation description
         *
         * @return this builder
         */
        public Builder<M> withDescription(String sDescription)
            {
            m_sDescription = sDescription;
            return this;
            }

        /**
         * Set the operation parameters.
         *
         * @param sName         the parameter name
         * @param sDescription  the parameter description
         * @param type          the parameter type
         *
         * @return this builder
         */
        public Builder<M> withParameter(String sName, String sDescription, OpenType<?> type)
            {
            return withParameters(new OpenMBeanParameterInfoSupport(sName, sDescription, type));
            }

        /**
         * Set the operation parameters.
         *
         * @param aParams  the operation parameters
         *
         * @return this builder
         */
        public Builder<M> withParameters(OpenMBeanParameterInfo... aParams)
            {
            m_listParams.addAll(Arrays.asList(aParams));
            return this;
            }

        /**
         * Set the operation return type.
         *
         * @param type  the operation return type
         *
         * @return this builder
         */
        public Builder<M> returning(OpenType<?> type)
            {
            m_typeReturn = type;
            return this;
            }

        /**
         * Set the function that will be executed.
         *
         * @param fn  the function that will be executed
         *
         * @return this builder
         */
        public Builder<M> withFunction(BiConsumer<M, Object[]> fn)
            {
            m_function = fn == null ? (m, p) -> null : (m, p) -> {fn.accept(m, p); return null;};
            return this;
            }

        /**
         * Set the function that will be executed.
         *
         * @param fn  the function that will be executed
         *
         * @return this builder
         */
        public Builder<M> withFunction(BiFunction<M, Object[], ?> fn)
            {
            m_function = fn == null ? (m, p) -> null : fn;
            return this;
            }

        /**
         * Build a {@link SimpleModelOperation} from the state in this builder.
         *
         * @return a new a {@link SimpleModelOperation}
         */
        public SimpleModelOperation<M> build()
            {
            return new SimpleModelOperation<>(this);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the operation.
         */
        private final String f_sName;

        /**
         * The description of the operation.
         */
        private String m_sDescription;

        /**
         * The operation parameters.
         */
        private final List<OpenMBeanParameterInfo> m_listParams = new ArrayList<>();
        
        /**
         * The operation return type.
         */
        private OpenType<?> m_typeReturn = SimpleType.VOID;

        /**
         * The function to call when the operation is executed.
         */
        private BiFunction<M, Object[], ?> m_function = (m, p) -> null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the operation.
     */
    private final String f_sName;

    /**
     * The description of the operation.
     */
    private final String f_sDescription;


    /**
     * The operation parameters.
     */
    private final OpenMBeanParameterInfo[] f_aParams;
    
    /**
     * The operation return type.
     */
    private final OpenType<?> f_typeReturn;

    /**
     * The function to call when the operation is executed.
     */
    private final BiFunction<M, Object[], ?> f_function;

    /**
     * The lazily created {@link MBeanOperationInfo}.
     */
    private MBeanOperationInfo m_info;
    }
