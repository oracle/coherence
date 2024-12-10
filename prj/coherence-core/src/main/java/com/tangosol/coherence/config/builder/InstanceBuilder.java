/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SimpleParameterList;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import static com.tangosol.coherence.config.builder.ParameterizedBuilderHelper.getAssignableValue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.lang.reflect.Constructor;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An {@link InstanceBuilder} is a {@link ParameterizedBuilder} implementation that additionally supports injection
 * based on Coherence &lt;instance%gt; or &lt;class-scheme&gt; configurations.
 * <p>
 * While supporting injection this class may also be used in situations where a {@link ParameterizedBuilder} is
 * required (must be passed) for a known type of the class.
 * <p>
 * For example, if you need a {@link ParameterizedBuilder} for a java.awt.Point class, but you don't
 * want to create an anonymous {@link ParameterizedBuilder} implementation for a Point, you can use the following:
 * <p>
 * <code>new InstanceBuilder(Point.class);</code>
 * <p>
 * Further you may also provide constructor parameters as follows:
 * <p>
 * <code>new InstanceBuilder(Point.class, 10, 12);</code>
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
public class InstanceBuilder<T>
        implements ParameterizedBuilder<T>, ParameterizedBuilder.ReflectionSupport,
            ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an {@link InstanceBuilder}.
     */
    public InstanceBuilder()
        {
        m_exprClassName             = new LiteralExpression<String>("undefined");
        m_listConstructorParameters = new ResolvableParameterList();
        }

    /**
     * Constructs a {@link InstanceBuilder} that will realize an instance of the specified {@link Class}.
     *
     * @param clzToRealize            the {@link Class} to realize
     * @param aConstructorParameters  the optional constructor parameters
     */
    public InstanceBuilder(Class<?> clzToRealize, Object... aConstructorParameters)
        {
        m_exprClassName             = new LiteralExpression<String>(clzToRealize.getName());
        m_listConstructorParameters = new SimpleParameterList(aConstructorParameters);
        }

    /**
     * Constructs a {@link InstanceBuilder} that will realize an instance of the specifically named class.
     *
     * @param exprClassName           an {@link Expression} that when evaluated will return the class name to realize
     * @param aConstructorParameters  the optional constructor parameters
     */
    public InstanceBuilder(Expression<String> exprClassName, Object... aConstructorParameters)
        {
        m_exprClassName             = exprClassName;
        m_listConstructorParameters = new SimpleParameterList(aConstructorParameters);
        }

    /**
     * Constructs a {@link InstanceBuilder} that will realize an instance of the specifically named class.
     *
     * @param sClassName              the name of the class to realize
     * @param aConstructorParameters  the optional constructor parameters
     */
    public InstanceBuilder(String sClassName, Object... aConstructorParameters)
        {
        m_exprClassName             = new LiteralExpression<String>(sClassName);
        m_listConstructorParameters = new SimpleParameterList(aConstructorParameters);
        }

    // ----- InstanceBuilder methods ----------------------------------------

    /**
     * Return the expression representing the name of the class this builder
     * will attempt to instantiate.
     *
     * @return an expression representing the class name
     */
    public Expression<String> getClassName()
        {
        return m_exprClassName;
        }

    /**
     * Sets the {@link Expression} that when evaluated will produce the name of the class to realize.
     *
     * @param exprClassName  the {@link Expression}
     */
    @Injectable("class-name")
    public void setClassName(Expression<String> exprClassName)
        {
        m_exprClassName = exprClassName;
        }

    /**
     * Return the {@link ParameterList} to be used for constructor parameters
     * when realizing the class.
     *
     * @return the {@link ParameterList} for the constructor
     */
    public ParameterList getConstructorParameterList()
        {
        return m_listConstructorParameters;
        }

    /**
     * Sets the {@link ParameterList} to be used for constructor parameters when realizing the class.
     *
     * @param listParameters  the {@link ParameterList} for the constructor
     */
    @Injectable("init-params")
    public void setConstructorParameterList(ParameterList listParameters)
        {
        m_listConstructorParameters = listParameters;
        }

    // ----- ParameterizedBuilder Interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    public T realize(ParameterResolver resolver, ClassLoader loader, ParameterList listConstructorParameters)
        {
        try
            {
            // ensure we have a classloader
            loader = Base.ensureClassLoader(loader);

            // resolve the actual class name
            String sClassName = m_exprClassName.evaluate(resolver);

            // attempt to load the class
            Class<?> clzClass = loader.loadClass(sClassName);

            // determine which list of parameters to use
            ParameterList listParameters = listConstructorParameters == null
                                           ? m_listConstructorParameters : listConstructorParameters;

            // find a constructor that is compatible with the constructor parameters
            Constructor<?>[] aConstructors          = clzClass.getConstructors();
            int              cConstructors          = aConstructors.length;
            int              cConstructorParameters = listParameters.size();
            Object[] aConstructorParameters = cConstructorParameters == 0 ? null : new Object[cConstructorParameters];

            // assume a compatible constructor is not found
            Constructor<?> constructor = null;

            for (int i = 0; i < cConstructors && constructor == null; i++)
                {
                if (aConstructors[i].getParameterTypes().length == cConstructorParameters)
                    {
                    // ensure that the constructor parameter types are compatible
                    try
                        {
                        Class<?>[] aParameterTypes = aConstructors[i].getParameterTypes();
                        int        j               = 0;

                        for (Parameter parameter : listParameters)
                            {
                            aConstructorParameters[j] = getAssignableValue(aParameterTypes[j], parameter, resolver,
                                loader);
                            j++;
                            }

                        constructor = aConstructors[i];
                        }

                    catch (Exception e)
                        {
                        // an exception means the constructor is not compatible
                        }
                    }
                }

            // did we find a compatible constructor?
            if (constructor == null)
                {
                throw new NoSuchMethodException(String.format(
                    "Unable to find a compatible constructor for [%s] with the parameters [%s]", sClassName,
                    listParameters));
                }
            else
                {
                return (T)constructor.newInstance(aConstructorParameters);
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    // ----- ParameterizedBuilder.ReflectionSupport interface ---------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean realizes(Class<?> clzClass, ParameterResolver resolver, ClassLoader loader)
        {
        try
            {
            // ensure we have a classloader
            loader = Base.ensureClassLoader(loader);

            // attempt to load the class name, but don't initialize it
            Class<?> clz = loader.loadClass(m_exprClassName.evaluate(resolver));

            // ensure the class is assignment compatible with the requested class
            return clzClass.isAssignableFrom(clz);
            }
        catch (ClassNotFoundException e)
            {
            return false;
            }
        }

    // ----- object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "InstanceBuilder{" +
                "className=" + m_exprClassName + ", " +
                "constructorParameters=" + m_listConstructorParameters +
                '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_exprClassName             = (Expression<String>) ExternalizableHelper.readObject(in, null);
        m_listConstructorParameters = (ParameterList) ExternalizableHelper.readObject(in, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_exprClassName);
        ExternalizableHelper.writeObject(out, m_listConstructorParameters);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        m_exprClassName             = (Expression<String>) reader.readObject(0);
        m_listConstructorParameters = (ParameterList) reader.readObject(1);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeObject(0, m_exprClassName);
        writer.writeObject(1, m_listConstructorParameters);
        }

    // ----- data members ---------------------------------------------------

    /**
     * An expression that when evaluated will return the name of the class to realize.
     */
    @JsonbProperty("exprClassName")
    private Expression<String> m_exprClassName;

    /**
     * The {@link ParameterList} containing the constructor {@link Parameter}s for realizing the class.
     */
    @JsonbProperty("constructorParams")
    private ParameterList m_listConstructorParameters;
    }
