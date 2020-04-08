/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.net.URI;

import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Response;

/**
 * API resource for persistence coordinator MBean.
 *
 * @author sr 2017.09.08
 * @since 12.2.1.4.0
 */
public class PersistenceResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a PersistenceResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public PersistenceResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return PersistenceManagerMBean(s) attributes of a service.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        return response(getResponseEntityForMbean(getQuery(), LINKS));
        }

    /**
     * Return list of snapshots of a service.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path("snapshots")
    public Response getSnapshots()
        {
        Filter<String> filterAttributes = getAttributesFilter("snapshots", getExcludeList(null));
        QueryBuilder   bldrQuery        = getQuery();

        return response(getResponseEntityForMbean(bldrQuery, getParentUri(), getCurrentUri(),
                filterAttributes, getLinksFilter()));
        }

    /**
     * Return list of archived snapshots of a service.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path("archives")
    public Response getArchive()
        {
        return response(getResponseFromMBeanOperation(getQuery(),
                "archives", "listArchivedSnapshots"));
        }

    /**
     * Return list of archived snapshot stores for the provided snapshot.
     *
     * @param sSnapshotName  the snapshot name
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path("archiveStores/{" + SNAPSHOT_NAME + "}")
    public Response getArchiveStores(@PathParam(SNAPSHOT_NAME) String sSnapshotName)
        {
        String[] asSignature = {String.class.getName()};
        Object[] aoArguments = {sSnapshotName};

        return response(getResponseFromMBeanOperation(getQuery(),
                "archiveStores", "listArchivedSnapshotStores", aoArguments, asSignature));
        }

    // ----- Post API -------------------------------------------------------

    /**
     * Call "forceRecovery" operation on PersistenceManagerMBean.
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("forceRecovery")
    public Response forceRecovery()
        {
        return executeMBeanOperation(getQuery(), "forceRecovery", null, null);
        }

    /**
     * Call "createSnapshot" operation on PersistenceManagerMBean.
     *
     * @param sSnapshotName  the snapshot name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("snapshots/{" + SNAPSHOT_NAME + "}")
    public Response executeSnapshotOperation(@PathParam(SNAPSHOT_NAME) String sSnapshotName)
        {
        String[] asSignature = {String.class.getName()};
        Object[] aoArguments = {sSnapshotName};

        return executeMBeanOperation(getQuery(), "createSnapshot", aoArguments, asSignature);
        }

    /**
     * Call "recoverSnapshot" operation on PersistenceManagerMBean.
     *
     * @param sSnapshotName  the snapshot name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("snapshots/{" + SNAPSHOT_NAME + "}" + "/recover")
    public Response recoverSnapshot(@PathParam(SNAPSHOT_NAME) String sSnapshotName)
        {
        String[] asSignature = {String.class.getName()};
        Object[] aoArguments = {sSnapshotName};

        return executeMBeanOperation(getQuery(), "recoverSnapshot", aoArguments, asSignature);
        }

    /**
     * Call "archiveSnapshot" operation on PersistenceManagerMBean.
     *
     * @param sSnapshotName  the snapshot name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("archives/{" + SNAPSHOT_NAME + "}")
    public Response executeArchiveOperation(@PathParam(SNAPSHOT_NAME) String sSnapshotName)
        {
        String[] asSignature = {String.class.getName()};
        Object[] aoArguments = {sSnapshotName};

        return executeMBeanOperation(getQuery(), "archiveSnapshot", aoArguments, asSignature);
        }

    /**
     * Call "retrieveArchivedSnapshot" operation on PersistenceManagerMBean.
     *
     * @param sSnapshotName  the snapshot name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("archives/{" + SNAPSHOT_NAME + "}/retrieve")
    public Response retrieveArchivedSnapshot(@PathParam(SNAPSHOT_NAME) String sSnapshotName)
        {
        String[] asSignature = {String.class.getName()};
        Object[] aoArguments = {sSnapshotName};

        return executeMBeanOperation(getQuery(), "retrieveArchivedSnapshot", aoArguments, asSignature);
        }

    // ----- Delete API -----------------------------------------------------

    /**
     * Call "removeSnapshot" operation on PersistenceManagerMBean.
     *
     * @param sSnapshotName  the snapshot name
     *
     * @return the response object
     */
    @DELETE
    @Produces(MEDIA_TYPES)
    @Path("snapshots/{" + SNAPSHOT_NAME + "}")
    public Response deleteSnapshot(@PathParam(SNAPSHOT_NAME) String sSnapshotName)
        {
        String[] asSignature = {String.class.getName()};
        Object[] aoArguments = {sSnapshotName};

        return executeMBeanOperation(getQuery(), "removeSnapshot", aoArguments, asSignature);
        }

    /**
     * Call "removeArchivedSnapshot" operation on PersistenceManagerMBean.
     *
     * @param sSnapshotName  the snapshot name
     *
     * @return the response object
     */
    @DELETE
    @Produces(MEDIA_TYPES)
    @Path("archives/{" + SNAPSHOT_NAME + "}")
    public Response deleteArchive(@PathParam(SNAPSHOT_NAME) String sSnapshotName)
        {
        String[] asSignature = {String.class.getName()};
        Object[] aoArguments = {sSnapshotName};

        return executeMBeanOperation(getQuery(), "removeArchivedSnapshot", aoArguments, asSignature);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        URI uriSelf = getSubUri(uriParent, PERSISTENCE);

        return getResponseEntityForMbean(getQuery(), uriParent, uriSelf, mapQuery, LINKS);
        }

    // ----- PersistenceResource methods-------------------------------------

    /**
     * MBean query to retrieve PersistenceController for the provided service.
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery()
        {
        return createQueryBuilder().withBaseQuery(PERSISTENCE_CONTROLLER_QUERY).withService(getService());
        }

    // ----- constants ------------------------------------------------------

    public static String[] LINKS = {"snapshots", "archives"};
    }
