/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;
import com.oracle.coherence.common.base.Logger;


import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.EntityMBeanResponse;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.net.URI;

import java.util.Map;

/**
 * API resource for persistence coordinator MBean.
 *
 * @author sr 2017.09.08
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class PersistenceResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/snapshots", this::getSnapshots);
        router.addGet(sPathRoot + "/archives", this::getArchives);
        router.addGet(sPathRoot + "/archiveStores/{" + SNAPSHOT_NAME + "}", this::getArchiveStores);

        router.addPost(sPathRoot + "/forceRecovery", this::forceRecovery);
        router.addPost(sPathRoot + "/snapshots/{" + SNAPSHOT_NAME + "}", this::createSnapshot);
        router.addPost(sPathRoot + "/snapshots/{" + SNAPSHOT_NAME + "}/recover", this::recoverSnapshot);
        router.addPost(sPathRoot + "/archives/{" + SNAPSHOT_NAME + "}", this::archiveSnapshot);
        router.addPost(sPathRoot + "/archives/{" + SNAPSHOT_NAME + "}/retrieve", this::retrieveArchivedSnapshot);

        router.addDelete(sPathRoot + "/snapshots/{" + SNAPSHOT_NAME + "}", this::deleteSnapshot);
        router.addDelete(sPathRoot + "/archives/{" + SNAPSHOT_NAME + "}", this::deleteArchive);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return PersistenceManagerMBean(s) attributes of a service.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        return response(getResponseEntityForMbean(request, getQuery(request), LINKS));
        }

    /**
     * Return list of snapshots of a service.
     *
     * @return the response object
     */
    public Response getSnapshots(HttpRequest request)
        {
        Filter<String> filterAttributes = getAttributesFilter("snapshots", getExcludeList(request));
        QueryBuilder   queryBuilder     = getQuery(request);

        return response(getResponseEntityForMbean(request, queryBuilder, getParentUri(request), getCurrentUri(request),
                                                  filterAttributes, getLinksFilter(request)));
        }

    /**
     * Return list of archived snapshots of a service.
     *
     * @return the response object
     */
    public Response getArchives(HttpRequest request)
        {
        return response(getResponseFromMBeanOperation(request, getQuery(request),
                "archives", "listArchivedSnapshots"));
        }

    /**
     * Return list of archived snapshot stores for the provided snapshot.
     *
     * @return the response object
     */
    public Response getArchiveStores(HttpRequest request)
        {
        String   sSnapshotName = request.getFirstPathParameter(SNAPSHOT_NAME);
        String[] asSignature   = {String.class.getName()};
        Object[] aoArguments   = {sSnapshotName};

        return response(getResponseFromMBeanOperation(request, getQuery(request),
                "archiveStores", "listArchivedSnapshotStores", aoArguments, asSignature));
        }

    // ----- Post API -------------------------------------------------------

    /**
     * Call "forceRecovery" operation on PersistenceManagerMBean.
     *
     * @return the response object
     */
    public Response forceRecovery(HttpRequest request)
        {
        return executeMBeanOperation(request, getQuery(request), "forceRecovery", null, null);
        }

    /**
     * Call "createSnapshot" operation on PersistenceManagerMBean.
     *
     * @return the response object
     */
    public Response createSnapshot(HttpRequest request)
        {
        String   sSnapshotName = request.getFirstPathParameter(SNAPSHOT_NAME);
        String[] asSignature   = {String.class.getName()};
        Object[] aoArguments   = {sSnapshotName};

        return executeMBeanOperation(request, getQuery(request), "createSnapshot", aoArguments, asSignature);
        }

    /**
     * Call "recoverSnapshot" operation on PersistenceManagerMBean.
     *
     * @return the response object
     */
    public Response recoverSnapshot(HttpRequest request)
        {
        String   sSnapshotName = request.getFirstPathParameter(SNAPSHOT_NAME);
        String[] asSignature   = {String.class.getName()};
        Object[] aoArguments   = {sSnapshotName};

        return executeMBeanOperation(request, getQuery(request), "recoverSnapshot", aoArguments, asSignature);
        }

    /**
     * Call "archiveSnapshot" operation on PersistenceManagerMBean.
     *
     * @return the response object
     */
    public Response archiveSnapshot(HttpRequest request)
        {
        String   sSnapshotName = request.getFirstPathParameter(SNAPSHOT_NAME);
        String[] asSignature   = {String.class.getName()};
        Object[] aoArguments   = {sSnapshotName};

        return executeMBeanOperation(request, getQuery(request), "archiveSnapshot", aoArguments, asSignature);
        }

    /**
     * Call "retrieveArchivedSnapshot" operation on PersistenceManagerMBean.
     *
     * @return the response object
     */
    public Response retrieveArchivedSnapshot(HttpRequest request)
        {
        String   sSnapshotName = request.getFirstPathParameter(SNAPSHOT_NAME);
        String[] asSignature   = {String.class.getName()};
        Object[] aoArguments   = {sSnapshotName};

        return executeMBeanOperation(request, getQuery(request), "retrieveArchivedSnapshot", aoArguments, asSignature);
        }

    // ----- Delete API -----------------------------------------------------

    /**
     * Call "removeSnapshot" operation on PersistenceManagerMBean.
     *
     * @return the response object
     */
    public Response deleteSnapshot(HttpRequest request)
        {
        String   sSnapshotName = request.getFirstPathParameter(SNAPSHOT_NAME);
        String[] asSignature   = {String.class.getName()};
        Object[] aoArguments   = {sSnapshotName};

        // todo: remove the log when we are able to determine the cause for BUG 32011353
        Logger.info("PersistenceResource.DELETE, snapshot: " + sSnapshotName);

        return executeMBeanOperation(request, getQuery(request), "removeSnapshot", aoArguments, asSignature);
        }

    /**
     * Call "removeArchivedSnapshot" operation on PersistenceManagerMBean.
     *
     * @return the response object
     */
    public Response deleteArchive(HttpRequest request)
        {
        String   sSnapshotName = request.getFirstPathParameter(SNAPSHOT_NAME);
        String[] asSignature   = {String.class.getName()};
        Object[] aoArguments   = {sSnapshotName};

        return executeMBeanOperation(request, getQuery(request), "removeArchivedSnapshot", aoArguments, asSignature);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        URI uriSelf = getSubUri(uriParent, PERSISTENCE);

        return getResponseEntityForMbean(request, getQuery(request), uriParent, uriSelf, mapQuery, LINKS);
        }

    // ----- PersistenceResource methods-------------------------------------

    /**
     * MBean query to retrieve PersistenceController for the provided service.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request)
        {
        return createQueryBuilder(request).withBaseQuery(PERSISTENCE_CONTROLLER_QUERY).withService(getService(request));
        }

    // ----- constants ------------------------------------------------------

    public static String[] LINKS = {"snapshots", "archives"};
    }
