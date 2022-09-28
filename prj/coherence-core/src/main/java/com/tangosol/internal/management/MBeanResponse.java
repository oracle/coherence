/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import com.tangosol.internal.http.HttpRequest;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;

import java.net.URI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Response wrapper object to be used for Coherence management over REST implementation.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4.0
 */
public class MBeanResponse
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default Constructor.
     */
    public MBeanResponse()
        {
        }

    /**
     * Construct an MBeanResponse instance.
     *
     * @param request      the {@link HttpRequest}
     */
    public MBeanResponse(HttpRequest request)
        {
        this(request, (Filter<String>) null);
        }

    /**
     * Construct a MBeanResponse instance.
     *
     * @param request      the {@link HttpRequest}
     * @param filterLinks  the links filter
     */
    public MBeanResponse(HttpRequest request, Filter<String> filterLinks)
        {
        m_fIncludeResourceLinks = !Boolean.parseBoolean(request.getHeaderString(HEADER_SKIP_LINKS));
        if (filterLinks == null)
            {
            filterLinks = Filters.always();
            }
        m_filterLinks = filterLinks;
        }

    // ----- MBeanResponse methods ------------------------------------------------------

    /**
     * Returns {@code true} if there are any FAILURE messages in the response.
     *
     * @return  {@code true} if there are any FAILURE messages in the response
     */
    public boolean hasFailures()
        {
        return m_listMessages.size() != 0 &&
                m_listMessages.stream().anyMatch(m -> m.f_sSeverity == Message.Severity.FAILURE);
        }

    /**
     * Add a failure message to the response.
     *
     * @param sMessage the message to be added
     */
    public void addFailure(String sMessage)
        {
        addMessage(Message.Severity.FAILURE, sMessage);
        }

    /**
     * Add a failure message for the corresponding field to the response. Used
     * in case of failures in put request to an Mbean, where an attribute updated failed.
     *
     * @param sField    the field which failed
     * @param sMessage  the message to be added
     */
    public void addFailure(String sField, String sMessage)
        {
        addMessage(Message.Severity.FAILURE, sField, sMessage);
        }

    /**
     * Add a message to the response.
     *
     * @param severity  the severity of the response
     * @param sField     the field which failed
     * @param sMessage   the message to be added
     */
    public void addMessage(Message.Severity severity, String sField, String sMessage)
        {
        add(new Message(severity, sField, sMessage));
        }

    /**
     * Add a message to the response.
     * @param severity  the severity of the response
     * @param sMessage  the message to be added
     */
    public void addMessage(Message.Severity severity, String sMessage)
        {
        add(new Message(severity, sMessage));
        }

    /**
     * Add a message to the response.
     *
     * @param message   the message to be added
     */
    public void add(Message message)
        {
        m_listMessages.add(message);
        }

    /**
     * Add the URI as the self link of the resource.
     *
     * @param uri  the URI of the resource
     */
    public void addSelfResourceLinks(URI uri)
        {
        addResourceLink(LINK_REL_SELF, uri);
        addResourceLink(LINK_REL_CANONICAL, uri);
        }

    /**
     * Add the URI as the parent link of the resource.
     *
     * @param uri  the URI of the parent resource
     */
    public void addParentResourceLink(URI uri)
        {
        addResourceLink(LINK_REL_PARENT, uri);
        }

    /**
     * Add a resource link to the response links.
     *
     * @param sRel  the name of link
     * @param uri   the URI of the link
     * @return
     */
    public void addResourceLink(String sRel, URI uri)
        {
        ResourceLink linkRes = new ResourceLink(sRel, uri);

        if (m_filterLinks.evaluate(linkRes.getRelationship()))
            {
            m_listLinks.add(linkRes);
            }
        }

    /**
     * Convert the response body to a Json object.
     * @return  the equivalent JSON object
     */
    public Map<String, Object> toJson()
        {
        Map<String, Object> mapResponse = new LinkedHashMap<>();
        populateJson(mapResponse);
        return mapResponse;
        }

    /**
     * Populate the body to the provided JSON object. The action included adding
     * links, messages to the response.
     *
     * @param mapResponse  the JSON Object to be populated
     */
    protected void populateJson(Map<String, Object> mapResponse)
        {
        List<Message> listMessages = m_listMessages;
        if (!listMessages.isEmpty())
            {
            List<Map<String, Object>> listMessageStrings = new ArrayList<>();
            for (Message message : listMessages)
                {
                listMessageStrings.add(message.toJson());
                }
            mapResponse.put(PROP_MESSAGES, listMessageStrings);
            }

        if (m_fIncludeResourceLinks)
            {
            List<Map<String, Object>> listLinks = getLinksJson();
            if (listLinks.size() > 0)
                {
                mapResponse.put(PROP_LINKS, listLinks);
                }
            }
        }

    protected List<Map<String, Object>> getLinksJson()
        {
        List<Map<String, Object>> listLinks = new ArrayList<>();
        for (ResourceLink link : m_listLinks)
            {
            listLinks.add(link.toJson());
            }
        return listLinks;
        }

    /**
     * Sets if this response contains a Security Exception.
     *
     * @param fIsSecurityException true if this response contains a Security Exception
     */
    public void setSecurityException(boolean fIsSecurityException)
        {
        m_fIsSecurityException = fIsSecurityException;
        }

    /**
     * Returns true if this response contains a Security Exception.
     * @return true if this response contains a Security Exception
     */
    public boolean isSecurityException()
        {
        return m_fIsSecurityException;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Link to the parent.
     */
    public static final String LINK_REL_PARENT = "parent";

    /**
     * Link to the current resource.
     */
    public static final String LINK_REL_SELF = "self";

    /**
     * Canonical link to the current resource.
     */
    public static final String LINK_REL_CANONICAL = "canonical";

    /**
     * The header which is passed in case the m_listLinks needs to be skipped.
     */
    public static final String HEADER_SKIP_LINKS = "X-Skip-Resource-Links";

    /**
     * The key to the object in the response where all the m_listLinks are populated.
     */
    public static final String PROP_LINKS = "links";

    /**
     * Key to the object which has any m_listMessages.
     */
    public static final String PROP_MESSAGES = "messages";

    // ----- data members ---------------------------------------------------

    /**
     * The list of messages to be sent in the response.
     */
    protected List<Message> m_listMessages = new ArrayList<>();

    /**
     * Boolean to decide whether to include resource links or not in the response.
     */
    protected boolean m_fIncludeResourceLinks = true;

    /**
     * The links to be added in the response.
     */
    protected List<ResourceLink> m_listLinks = new ArrayList<>();

    /**
     * The filter used for the links, users can specify which links to return
     * in the response by use of query parameters - includeLinks and excludeLinks.
     */
    protected Filter<String> m_filterLinks = null;

    /**
     * Indicates if this is a SecurityException.
     */
    protected boolean m_fIsSecurityException = false;
    }
