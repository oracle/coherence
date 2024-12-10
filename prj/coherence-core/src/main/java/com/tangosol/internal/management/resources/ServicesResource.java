/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.http.HttpException;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.EntityMBeanResponse;
import com.tangosol.internal.management.MBeanResponse;
import com.tangosol.net.management.MBeanAccessor;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.net.URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.Set;

import java.util.function.Predicate;

import java.util.stream.Collectors;

import javax.management.ObjectName;

/**
 * Handles management API requests for services in a cluster.
 *
 * @author sr 2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class ServicesResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/" + MEMBERS, this::getAllServiceMembers);
        router.addGet(sPathRoot + "/" + PROXY + "/" + MEMBERS, this::getAllProxyMembers);

        router.addPost(sPathRoot + "/persistence/snapshots/{" + SNAPSHOT_NAME + "}", this::executeSnapshotOperationAllServices);

        // child resources
        router.addRoutes(sPathRoot + "/{" + SERVICE_NAME +"}", new ServiceResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the list of services in a cluster.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        URI                 uriCurrent = getCurrentUri(request);
        EntityMBeanResponse response   = getResponseBodyForMBeanCollection(request, getQuery(request), new ServiceResource(),
                                                                           NAME, null, getParentUri(request), uriCurrent, uriCurrent, null);

        if (response == null && getService(request) != null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        return response == null
                ? response(new EntityMBeanResponse())
                : response(response);
        }

    /**
     * Returns the list of all ServiceMBean members in the cluster.
     *
     * @return the response object.
     */
    public Response getAllServiceMembers(HttpRequest request)
        {
        return response(getResponseBodyForMBeanCollection(request, getQuery(request), null,
                                                          getParentUri(request), getCurrentUri(request)));
        }

    /**
     * Returns the list of all ServiceMBean members in the cluster.
     *
     * @return the response object.
     */
    public Response getAllProxyMembers(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(CONNECTION_MANAGERS_QUERY);

        return response(getResponseBodyForMBeanCollection(request, queryBuilder, null,
                getParentUri(request), getCurrentUri(request)));
        }

    /**
     * Issues a "createSnapshot" operation on the PersistenceManagerMBean for
     * all persistence enabled services.
     * <p>
     * Note: the implementation does not (currently) perform a "safe" snapshot
     *       across all services (i.e. all or nothing).
     *
     * @return the response object
     */
    public Response executeSnapshotOperationAllServices(HttpRequest request)
        {
        String   sSnapshotName = request.getFirstPathParameter(SNAPSHOT_NAME);
        String[] asSignature   = {String.class.getName()};
        Object[] aoArguments   = {sSnapshotName};

        // get the list of services enabled for persistence
        EntityMBeanResponse response = getResponseBodyForMBeanCollection(request, instantiatePersistentServicesQuery(request), null,
                                                                         getParentUri(request), getCurrentUri(request));

        List<Map<String, Object>> listEntities = response.getEntities();

        if (listEntities.isEmpty())
            {
            Response.status(Response.Status.BAD_REQUEST).entity(response.toJson()).build();
            }
        else
            {
            // loop through each service and initiate the create snapshot
            // fail at the first non 200 response
            for (Map<String, Object> entity: listEntities)
                {
                String sService = (String) entity.get("service");
                Response resp = executeMBeanOperation(
                        request, instantiatePersistentServicesQuery(request, sService),
                        "createSnapshot",
                        aoArguments,
                        asSignature);

                if (resp.getStatus().getStatusCode() != 200)
                    {
                    // add a more useful error message and "re-throw" the Response with original status and message
                    MBeanResponse mbeanResponse = new MBeanResponse(request);
                    String        sMsg          = "Creation of a snapshot across all service failed for service " +
                                                    sService + '.';

                    mbeanResponse.addFailure(sMsg);
                    mbeanResponse.addFailure(resp.getEntity().toString());

                    return Response.status(resp.getStatus()).entity(mbeanResponse.toJson()).build();
                    }
                }
            }

        // fall through means snapshots were all submitted OK
        return Response.status(200).build();
        }

    // ---- AbstractManagementResource methods ------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        try
            {
            Filter<String>      filterLinks    = getLinksFilter(request, mapQuery);
            URI                 uriSelf        = getSubUri(uriParent, SERVICES);
            MBeanAccessor       accessor       = ensureBeanAccessor(request);
            EntityMBeanResponse responseEntity = createResponse(request, uriParent, uriSelf, filterLinks);
            Set<String>         setObjectNames = accessor.queryKeys(getQuery(request).build());

            if (setObjectNames != null && !setObjectNames.isEmpty())
                {
                // the logic below needs a bit of explanation. When Coherence is run
                // in WLS and applications are deployed to WLS domain partitions,
                // it can happen that there are services which has the same "name"
                // but they are running in different domain partitions and hence they are
                // different services in itself. So we have collect a single service
                // response per service per partition

                // the resultant set below will have a null element if there are services
                // which are not running inside any Domain partition or standalone Coherence
                // services
                Set<ObjectName>           setObjNames         = convertToObjectNames(setObjectNames);
                List<Map<String, Object>> listServices        = new ArrayList<>();
                Set<String>               setDomainPartitions = setObjNames.stream()
                                                                     .map(o -> o.getKeyProperty(DOMAIN_PARTITION))
                                                                     .collect(Collectors.toSet());


                for (String domainPartition : setDomainPartitions)
                    {
                    listServices.addAll(getResponseForDomainPartition(request, mapQuery, uriSelf, uriCurrent, setObjNames, domainPartition));
                    }

                responseEntity.setEntities(listServices);
                }
            return responseEntity;
            }
        catch (Exception e)
            {
            Logger.err("Exception occurred while getting response for an MBean collection for Services\n", e);
            throw new HttpException();
            }
        }

    // ----- ServicesResource methods-----------------------------------

    /**
     * The query string for searching list of services.
     *
     * @param request  the request
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request)
        {
        return createQueryBuilder(request).withBaseQuery(SERVICES_QUERY);
        }

    /**
     * Get the list if services for a single domain partition.
     *
     * @param request           the {@link HttpRequest}
     * @param mapQuery          the object query map, query map contains which child elements must be returned
     * @param uriSelf           the current URI
     * @param setObjectNames    the total set of ServiceMBean ObjectNames
     * @param sDomainPartition  the domain partition whose services needs to be returned
     *
     * @return the list of services running in this domain partition
     */
    protected List<Map<String, Object>> getResponseForDomainPartition(HttpRequest         request,
                                                                      Map<String, Object> mapQuery,
                                                                      URI                 uriSelf,
                                                                      URI                 uriCurrent,
                                                                      Set<ObjectName>     setObjectNames,
                                                                      String              sDomainPartition)
        {
        // for a single domain partition, collect a map<servicename, list service members>

        List<Map<String, Object>>     listChildEntities = new ArrayList<>();
        Map<String, List<ObjectName>> mapMBeanToName = setObjectNames
                .stream()
                .filter(matchesDomainPartition(sDomainPartition))
                .collect(Collectors.groupingBy(o -> o.getKeyProperty(NAME)));

        // iterate through each of the service and add a response for the service
        for (Map.Entry<String, List<ObjectName>> entry : mapMBeanToName.entrySet())
            {
            AbstractManagementResource resource = new ServiceResource();

            // what we are doing below is a trick to make sure that the search happens for
            // only the required domain partition(or null if no partition)
            // for example, the ServiceME]emberResource search for a list of
            // caches in the member, while doing so, it is imperative that
            // MBean are filtered such that only the required domain partition
            // is searched

            resource.setDomainPartitionFilter(createDomainPartitionPredicate(sDomainPartition));

            listChildEntities.add(resource.getQueryResult(request, uriSelf, uriCurrent, mapQuery, Collections.singletonMap(NAME, entry.getKey())
            ).toJson());
            }
        return listChildEntities;
        }

    /**
     * Create a predicate for matching the provided domain partition with the domain
     * partition of the MBean.
     *
     * @param domainPartition  the domain partition
     *
     * @return predicate for domin partition matching
     */
    protected Predicate<ObjectName> matchesDomainPartition(String domainPartition)
        {
        // if the incoming domain partition is null, it means that the key property
        // domainPartition must be null, otherwise equality is checked
        return o -> domainPartition == null
                ? o.getKeyProperty(DOMAIN_PARTITION) == null
                : domainPartition.equals(o.getKeyProperty(DOMAIN_PARTITION));
        }

    public Filter<String> createDomainPartitionPredicate(String sDomainPartitionName)
        {
        return sDomainPartitionName == null
                ? Objects::isNull
                : s -> s.equals(sDomainPartitionName);

        /*
        return uriInfo ->
        {
        ValueExtractor<ObjectName, String> extractor
                = new ReflectionExtractor<> ("getKeyProperty", new Object[]{DOMAIN_PARTITION});
        if (sDomainPartitionName == null)
            {
            return new IsNullFilter<>(extractor);
            }
        else
            {
            return new EqualsFilter<>(extractor, sDomainPartitionName);
            }
        };*/
        }

    // ----- helpers --------------------------------------------------------

    /**
     * MBean query to retrieve PersistenceController for the given service as a param.
     *
     *
     * @param request   the request
     * @param sService  service name
     *
     * @return the MBean query
     */
    protected QueryBuilder instantiatePersistentServicesQuery(HttpRequest request, String sService)
        {
        return createQueryBuilder(request).withBaseQuery(PERSISTENCE_CONTROLLER_QUERY).withService(sService);
        }

    /**
     * MBean query to retrieve PersistenceController for all the persistence services.
     *
     * @param request  the request
     *
     * @return the MBean query
     */
    protected QueryBuilder instantiatePersistentServicesQuery(HttpRequest request)
        {
        return createQueryBuilder(request).withBaseQuery(PERSISTENCE_CONTROLLER_QUERY).exact(false);
        }
    
    }
