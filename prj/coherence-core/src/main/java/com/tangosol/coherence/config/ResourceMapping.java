/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.application.ContainerContext;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.scheme.Scheme;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A base class for mapping elements.
 *
 * @author jk  2015.05.21
 * @since Coherence 14.1.1
 */
@SuppressWarnings("rawtypes")
public abstract class ResourceMapping<R>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link ResourceMapping} for resources that will use raw types by default.
     *
     * @param sNamePattern  the pattern that maps resource names to caching schemes
     * @param sSchemeName   the name of the caching scheme to which a resource matching
     *                      this {@link ResourceMapping} will be associated
     */
    public ResourceMapping(String sNamePattern, String sSchemeName)
        {
        Base.azzert(sNamePattern != null);
        Base.azzert(sSchemeName != null);

        f_sNamePattern = sNamePattern;
        f_sSchemeName = sSchemeName;
        m_parameterResolver            = new ResolvableParameterList();
        m_listEventInterceptorBuilders = new LinkedList<>();
        f_resourceRegistry = new SimpleResourceRegistry();
        f_subMappings = new ArrayList<>();
        }

    // ----- ResourceMapping methods --------------------------------------------

    /**
     * Obtain the xml element name of this mapping.
     *
     * @return  the xml element name of this mapping
     */
    public abstract String getConfigElementName();

    /**
     * Determine whether the specified schem is valid
     * for this mapping type.
     *
     * @param scheme  the scheme to validate
     *
     * @throws IllegalStateException if the scheme is not valid
     */
    public abstract void validateScheme(Scheme scheme);

    /**
     * Set the flag to indicate if this mapping is for internal resources
     * used by the service.
     *
     * @param  fInternal  true if this is for internal resource
     *
     * @return  this ResourceMapping object
     */
    public ResourceMapping setInternal(boolean fInternal)
        {
        m_fInternal = fInternal;

        return this;
        }

    /**
     * Check if this ResourceMapping is for internal resources.
     *
     * @return  true if this is for internal resources
     */
    public boolean isInternal()
        {
        return m_fInternal;
        }

    /**
     * Obtains the pattern used to match resource names
     * to this {@link ResourceMapping}.
     *
     * @return the pattern
     */
    public String getNamePattern()
        {
        return f_sNamePattern;
        }

    /**
     * Obtains the name of the caching scheme to be used
     * that match this {@link ResourceMapping}.
     *
     * @return the name of the associated caching scheme
     */
    public String getSchemeName()
        {
        return f_sSchemeName;
        }

    /**
     * Obtains the {@link ResourceRegistry} that holds resources
     * associated with the {@link ResourceMapping}.
     *
     * @return the {@link ResourceRegistry}
     */
    public ResourceRegistry getResourceRegistry()
        {
        return f_resourceRegistry;
        }

    /**
     * Obtains the {@link ParameterResolver} that is to be used to
     * resolve {@link Parameter}s associated with this {@link ResourceMapping}.
     *
     * @return the {@link ParameterResolver}
     */
    public ParameterResolver getParameterResolver()
        {
        return m_parameterResolver;
        }

    /**
     * Sets the {@link ParameterResolver} that is used to resolve
     * {@link Parameter}s associated with the {@link ResourceMapping}.
     *
     * @param resolver  the {@link ParameterResolver}
     */
    @Injectable("init-params")
    public void setParameterResolver(ParameterResolver resolver)
        {
        m_parameterResolver = resolver == null
                              ? new ResolvableParameterList()
                              : resolver;
        }

    /**
     * Obtains the {@link List} of {@link NamedEventInterceptorBuilder}s
     * for this {@link ResourceMapping}.
     *
     * @return an {@link List} over {@link NamedEventInterceptorBuilder}s
     *         or <code>null</code> if none are defined
     */
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders()
        {
        return m_listEventInterceptorBuilders;
        }

    /**
     * Sets the {@link List} of {@link NamedEventInterceptorBuilder}s for the {@link ResourceMapping}.
     *
     * @param listBuilders  the {@link List} of {@link NamedEventInterceptorBuilder}s
     */
    @Injectable("interceptors")
    public void setEventInterceptorBuilders(List<NamedEventInterceptorBuilder> listBuilders)
        {
        m_listEventInterceptorBuilders = listBuilders;
        }

    /**
     * Determines if the {@link ResourceMapping} is for (matches) the specified resource name.
     *
     * @param sName  the resource name to check for a match
     *
     * @return <code>true</code> if the {@link ResourceMapping} is for the specified resource name,
     *         <code>false</code> otherwise
     */
    public boolean isForName(String sName)
        {
        return (f_sNamePattern.endsWith("*") && sName.regionMatches(0, f_sNamePattern, 0, f_sNamePattern.length() - 1))
                || f_sNamePattern.equals(sName);
        }

    /**
     * Determines if the {@link ResourceMapping} pattern contains a * wildcard.
     *
     * @return <code>true</code> if the pattern contains a * wildcard,
     *         <code>false</code> otherwise
     */
    public boolean usesWildcard()
        {
        return f_sNamePattern.contains("*");
        }

    /**
     * Determines the value the wildcard * declared in the resource name
     * pattern for the {@link ResourceMapping} matches.  If the pattern does
     * not contain a wildcard * or the resource name does not match the
     * mapping, <code>null</code> is returned.
     * <p>
     * Examples:
     * <p>
     * 1. Calling mapping.getWildcardMatch("dist-test") on a ResourceMapping with
     * the resource name pattern "dist-*" will return "test".
     * <p>
     * 2. Calling mapping.getWildcardMatch("dist-*") on a ResourceMapping with
     * the resource name pattern "dist-*" will return "*".
     * <p>
     * 3. Calling mapping.getWildcardMatch("dist-fred") on a ResourceMapping with
     * the resource name pattern "dist-fred" will return <code>null</code>.
     * <p>
     * 4. Calling mapping.getWildcardMatch("dist-fred") on a ResourceMapping with
     * the resource name pattern "repl-*" will return <code>null</code>.
     * <p>
     * 5. Calling mapping.getWildcardMatch("dist-fred") on a ResourceMapping with
     * the resource name pattern "*" will return "dist-fred".
     *
     * @param sName  the resource name to match
     *
     * @return  the resource name string that matches the wildcard.
     */
    public String getWildcardMatch(String sName)
        {
        if (sName != null && !sName.isEmpty() && usesWildcard() && isForName(sName))
            {
            return f_sNamePattern.equals("*") ? sName : sName.substring(f_sNamePattern.indexOf("*"));
            }
        else
            {
            return null;
            }
        }

    /**
     * Get value of <code>sParamName</code> associated with this {@link CacheMapping}
     *
     * @param sParamName      parameter name to look up
     * @param paramValueType  parameter value type
     * @param <T>             parameter value type
     *
     * @return parameter value as an instance of paramValueType or null if parameter is not defined
     */
    public <T> T getValue(String sParamName, Class<T> paramValueType)
        {
        Parameter param = getParameterResolver().resolve(sParamName);
        return param == null ? null : param.evaluate(getParameterResolver()).as(paramValueType);
        }

    /**
     * Get value of <code>sParamName</code> associated with this {@link CacheMapping}
     *
     * @param sParamName  parameter name to look up
     *
     * @return parameter value or null if parameter is not found
     */
    public Object getValue(String sParamName)
        {
        ParameterResolver resolver = getParameterResolver();
        Parameter         param    = resolver.resolve(sParamName);

        return param == null ? null : param.evaluate(resolver).get();
        }

    /**
     * Determines the name of a resource given a value for the wildcard
     * (assuming the resource name pattern for the mapping is using a wildcard).
     * If the pattern does not contain a wildcard *, <code>null</code> will
     * be returned.
     * <p>
     * Examples:
     * <p>
     * 1. Calling mapping.getNameUsing("test") on a ResourceMapping with
     * the resource name pattern "dist-*" will return "dist-test".
     * <p>
     * 2. Calling mapping.getNameUsing("*") on a ResourceMapping with
     * the resource name pattern "dist-*" will return "dist-*".
     * <p>
     * 3. Calling mapping.getNameUsing("fred") on a ResourceMapping with
     * the resource name pattern "dist-fred" will return <code>null</code>.
     * <p>
     * 4. Calling mapping.getNameUsing("dist-fred") on a ResourceMapping with
     * the resource name pattern "*" will return "dist-fred".
     *
     * @param sWildCardValue the value to replace the wildcard * with
     *
     * @return  the resource name with the wildcard replaced with the specified value
     */
    public String getNameUsing(String sWildCardValue)
        {
        if (sWildCardValue != null && !sWildCardValue.isEmpty() && usesWildcard())
            {
            return f_sNamePattern.replaceAll("\\*", sWildCardValue);
            }
        else
            {
            return null;
            }

        }

    /**
     * Obtain the list of sub-mappings that this mapping contains
     *
     * @return the list of sub-mappings that this mapping contains.
     */
    public List<ResourceMapping> getSubMappings()
        {
        return f_subMappings;
        }

    public void preConstruct(ContainerContext context, ParameterResolver resolver, MapBuilder.Dependencies dependencies)
        {
        }

    public void postConstruct(ContainerContext context, R resource, ParameterResolver resolver, MapBuilder.Dependencies dependencies)
        {
        }

    // ----- data members ---------------------------------------------------

    /**
     * The flag to indicate if this {@link ResourceMapping} is for internal resources.
     */
    private boolean m_fInternal;

    /**
     * The pattern to be used to match and associate resource names with this {@link ResourceMapping}.
     */
    private final String f_sNamePattern;

    /**
     * The name of the caching scheme to which resource matching this {@link ResourceMapping} will be bound.
     */
    private final String f_sSchemeName;

    /**
     * The {@link ParameterResolver} to use for resolving {@link Parameter}s defined by this {@link ResourceMapping}.
     */
    private ParameterResolver m_parameterResolver;

    /**
     * The {@link ResourceRegistry} associated with this {@link ResourceMapping}.
     */
    private final ResourceRegistry f_resourceRegistry;

    /**
     * The {@link List} of {@link NamedEventInterceptorBuilder}s associated with this {@link ResourceMapping}.
     */
    private List<NamedEventInterceptorBuilder> m_listEventInterceptorBuilders;

    /**
     * The {@link List} of child mappings of this mapping.
     */
    private final List<ResourceMapping> f_subMappings;
    }
