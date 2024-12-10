/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import com.tangosol.internal.http.HttpRequest;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The MBeanResponse used for sending entities i.e. actual MBean
 * object/collection itself.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4.0
 */
public class EntityMBeanResponse
        extends MBeanResponse
    {
    // ----- constructors -----------------------------------------------------------------

    /**
     * Default Constructor.
     */
    public EntityMBeanResponse()
        {
        }

    /**
     * Construct an EntityMBeanResponse instance.
     *
     * @param request      the request context
     * @param filterLinks  the links filter
     */
    public EntityMBeanResponse(HttpRequest request, Filter<String> filterLinks)
        {
        super(request, filterLinks);
        }

    // ----- EntityMBeanResponse methods ------------------------------------------------------

    /**
     * Return {@code true} if this response has an entity.
     *
     * @return {@code true} if this response has an entity
     */
    public boolean hasEntity()
        {
        return !m_listEntities.isEmpty() || !m_mapEntity.isEmpty();
        }

    /**
     * Returns {@code true} if this response has no entity.
     *
     * @return  {@code true} if this response has no entity
     */
    public boolean isEmpty()
        {
        return !hasEntity();
        }

    /**
     * Return the entity object,
     *
     * @return the entity object
     */
    public Map<String, Object> getEntity()
        {
        return m_mapEntity;
        }

    /**
     * Update the entity object.
     *
     * @param entity  the entity object
     */
    public void setEntity(Map<String, Object> entity)
        {
        m_mapEntity = entity;
        }

    /**
     * Return the entities object,
     *
     * @return the entities object
     */
    public List<Map<String, Object>> getEntities()
        {
        return m_listEntities;
        }

    /**
     * Update the entities object.
     *
     * @param entities  the entities object
     */
    public void setEntities(List<Map<String, Object>>  entities)
        {
        m_listEntities = entities;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateJson(Map<String, Object> object)
        {
        super.populateJson(object);

        // populate the json item/items
        Map<String, Object> map = getEntity();
        if (map != null)
            {
            // merge the entity into the top level JSONObject to match the Oracle REST style guide
            for (String sKey : map.keySet())
                {
                Base.azzert(!(PROP_LINKS.equals(sKey) || PROP_MESSAGES.equals(sKey)),
                "An entity contains a property with a reserved property name: " + sKey);

                object.put(sKey, map.get(sKey));
                }
            }
        List<Map<String, Object>> listEntities = getEntities();
        if (listEntities != null && !listEntities.isEmpty())
            {
            object.put("items", listEntities);
            }
        }

    // ----- data members ------------------------------------------------------

    /**
     * The entity object to be sent in the response.
     */
    protected Map<String, Object> m_mapEntity = new LinkedHashMap<>();

    /**
     * The entity array to be sent in the response.
     */
    protected List<Map<String, Object>> m_listEntities = new ArrayList<>();
    }

