/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@link StaticFactoryInstanceBuilder} is a {@link ParameterizedBuilder} 
 * that has been configured to realize objects based on the properties defined 
 * by an &lt;instance&gt; configuration element that uses the static
 * &lt;class-factory-name&gt; approach.
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
public class StaticFactoryInstanceBuilder<T>
        implements ParameterizedBuilder<T>, ParameterizedBuilder.ReflectionSupport,
            ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link StaticFactoryInstanceBuilder}.
     */
    public StaticFactoryInstanceBuilder()
        {
        m_exprFactoryClassName  = new LiteralExpression<String>("undefined");
        m_exprFactoryMethodName = new LiteralExpression<String>("undefined");
        m_listMethodParameters  = new SimpleParameterList();
        }

    // ----- StaticFactoryInstanceBuilder methods ---------------------------

    /**
     * Return the {@link Expression} that when evaluated will produce the name of the class
     * containing a <strong>static</strong> factory method that will realize instances
     * for this {@link ParameterizedBuilder}.
     *
     * @return the factory class name {@link Expression}
     */
    public Expression<String> getFactoryClassName()
        {
        return m_exprFactoryClassName;
        }

    /**
     * Sets the {@link Expression} that when evaluated will produce the name of the class
     * containing a <strong>static</strong> factory method that will realize instances
     * for this {@link ParameterizedBuilder}.
     *
     * @param exprFactoryClassName  the {@link Expression}
     */
    @Injectable("class-factory-name")
    public void setFactoryClassName(Expression<String> exprFactoryClassName)
        {
        m_exprFactoryClassName = exprFactoryClassName;
        }

    /**
     * Return the {@link Expression} that when evaluated will produce the name of the factory class
     * <strong>static</strong>  method that will realize instances for this {@link ParameterizedBuilder}.
     *
     * @return the factory method name {@link Expression}
     */
    public Expression<String> getFactoryMethodName()
        {
        return m_exprFactoryMethodName;
        }

    /**
     * Set the {@link Expression} that when evaluated will produce the name of the factory class
     * <strong>static</strong>  method that will realize instances for this {@link ParameterizedBuilder}.
     *
     * @param exprFactoryMethodName  the {@link Expression}
     */
    @Injectable("method-name")
    public void setFactoryMethodName(Expression<String> exprFactoryMethodName)
        {
        m_exprFactoryMethodName = exprFactoryMethodName;
        }

    /**
     * Returns the {@link ParameterList} to use to resolve factory method parameters when realizing the class.
     *
     * @return  the {@link ParameterList} for method parameters
     */
    public ParameterList getFactoryMethodParameters()
        {
        return m_listMethodParameters;
        }

    /**
     * Sets the {@link ParameterList} to use to resolve factory method parameters when realizing the class.
     *
     * @param listParameters  the {@link ParameterList} for method parameters
     */
    @Injectable("init-params")
    public void setFactoryMethodParameters(ParameterList listParameters)
        {
        if (listParameters != null)
            {
            m_listMethodParameters = listParameters;
            }
        }

    /**
     * Ensures we have a non-null {@link ClassLoader} that we can use for loading classes.
     *
     * @param loader  the proposed {@link ClassLoader}, which may be <code>null</code>
     *
     * @return a non-null {@link ClassLoader}
     */
    protected ClassLoader ensureClassLoader(ClassLoader loader)
        {
        return loader == null ? getClass().getClassLoader() : loader;
        }

    // ----- ParameterizedBuilder Interface --------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    public T realize(ParameterResolver resolver, ClassLoader loader, ParameterList listMethodParameters)
        {
        try
            {
            // ensure we have a classloader
            loader = ensureClassLoader(loader);

            // load the factory class
            String   sClassName = m_exprFactoryClassName.evaluate(resolver);
            Class<?> clzFactory = loader.loadClass(sClassName);

            // determine which parameter list we should use
            ParameterList listParameters = listMethodParameters == null ? m_listMethodParameters : listMethodParameters;

            // find a static method with the required name and compatible parameter types
            String   sMethodName       = m_exprFactoryMethodName.evaluate(resolver);
            int      cMethodParameters = listParameters.size();
            Method   compatibleMethod  = null;
            Object[] aMethodParameters = cMethodParameters == 0 ? null : new Object[cMethodParameters];
            Method[] aMethods          = clzFactory.getMethods();

            for (int i = 0; i < aMethods.length && compatibleMethod == null; i++)
                {
                // only consider static methods with the required number of parameters, the required name and a return type
                if (aMethods[i].getName().equals(sMethodName)
                    && aMethods[i].getParameterTypes().length == cMethodParameters
                    && Modifier.isStatic(aMethods[i].getModifiers()) && (aMethods[i].getReturnType() != null))
                    {
                    // determine the compatible method parameter values
                    Class<?>[] aMethodParameterTypes = aMethods[i].getParameterTypes();
                    boolean    fIsCompatible         = true;

                    try
                        {
                        int j = 0;

                        for (Parameter parameter : listParameters)
                            {
                            aMethodParameters[j] = getAssignableValue(aMethodParameterTypes[j], parameter, resolver,
                                loader);
                            j++;
                            }
                        }
                    catch (Exception exception)
                        {
                        // this method is incompatible as parameter types don't match
                        fIsCompatible = false;
                        }

                    if (fIsCompatible)
                        {
                        compatibleMethod = aMethods[i];
                        }
                    }
                }

            // did we find a compatible method?
            if (compatibleMethod == null)
                {
                throw new NoSuchMethodException(String.format(
                    "Unable to find a compatible method for [%s] with the parameters [%s]", sMethodName,
                    listParameters));
                }
            else
                {
                return (T)compatibleMethod.invoke(clzFactory, aMethodParameters);
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
            loader = ensureClassLoader(loader);

            // attempt to factory load the class name, but don't initialize it
            String   sClassName = m_exprFactoryClassName.evaluate(resolver);
            Class<?> clzFactory = loader.loadClass(sClassName);

            // determine if there is a static method on the factory that has the correct name
            // NOTE: we don't attempt to use the types to find an exact match as the parameters may not be provided)
            String sMethodName      = m_exprFactoryMethodName.evaluate(resolver);
            Method compatibleMethod = null;

            for (Method method : clzFactory.getMethods())
                {
                if (method.getName().equals(sMethodName) && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() != null && clzClass.isAssignableFrom(method.getReturnType()))
                    {
                    compatibleMethod = method;

                    break;
                    }
                }

            return compatibleMethod != null;
            }
        catch (ClassNotFoundException e)
            {
            return false;
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_exprFactoryClassName  = (Expression<String>) ExternalizableHelper.readObject(in, null);
        m_exprFactoryMethodName = (Expression<String>) ExternalizableHelper.readObject(in, null);
        m_listMethodParameters  = (ParameterList) ExternalizableHelper.readObject(in, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_exprFactoryClassName);
        ExternalizableHelper.writeObject(out, m_exprFactoryMethodName);
        ExternalizableHelper.writeObject(out, m_listMethodParameters);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        m_exprFactoryClassName  = (Expression<String>) reader.readObject(0);
        m_exprFactoryMethodName = (Expression<String>) reader.readObject(1);
        m_listMethodParameters  = (ParameterList) reader.readObject(2);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeObject(0, m_exprFactoryClassName);
        writer.writeObject(1, m_exprFactoryMethodName);
        writer.writeObject(2, m_listMethodParameters);
        }

    // ----- data members ---------------------------------------------------

    /**
     * An {@link Expression} that when evaluated will produce the name of a class
     * providing a factory method to use when realizing instances for this {@link ParameterizedBuilder}.
     */
    @JsonbProperty("exprFactoryClassName")
    private Expression<String> m_exprFactoryClassName;

    /**
     * An {@link Expression} that when evaluated will produce the name of the <strong>static</strong>
     * method on the factory class that can be used to realize instances for this {@link ParameterizedBuilder}.
     */
    @JsonbProperty("exprFactoryMethodName")
    private Expression<String> m_exprFactoryMethodName;

    /**
     * The {@link ParameterList} to use for resolving {@link Parameter}s and calling the factory method.
     */
    @JsonbProperty("methodParameters")
    private ParameterList m_listMethodParameters;
    }
