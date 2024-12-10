/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

import com.oracle.coherence.common.base.Logger;

import com.sun.net.httpserver.Headers;

import java.net.URI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Collectors;

/**
 * A http request router.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public class RequestRouter
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a router.
     *
     * @param asRoots an optional array of endpoint roots
     */
    public RequestRouter(String... asRoots)
        {
        if (asRoots == null || asRoots.length == 0)
            {
            f_listRootPaths = Collections.emptyList();
            }
        else
            {
            f_listRootPaths = Arrays.stream(asRoots)
                    .filter(s -> !"/".equals(s))
                    .filter(s -> s.length() > 0)
                    .map(RequestRouter::validatePath)
                    .distinct()
                    .collect(Collectors.toList());
            }
        }

    // ----- RequestRouter methods ------------------------------------------

    /**
     * Set the default consumes media type for endpoints that do not specify a media type.
     *
     * @param asMediaTypes  the default consumes media types
     */
    public void setDefaultConsumes(String... asMediaTypes)
        {
        m_asConsumes = asMediaTypes;
        }

    /**
     * Set the default produces media type for endpoints that do not specify a media type.
     *
     * @param asMediaTypes  the default produces media types
     */
    public void setDefaultProduces(String... asMediaTypes)
        {
        m_asProduces = asMediaTypes;
        }

    /**
     * Add a default response headers to send for all responses.
     *
     * @param sKey    the name of the header
     * @param sValue  the value for the header
     */
    public void addDefaultResponseHeader(String sKey, String sValue)
        {
        f_commonResponseHeaders.add(sKey, sValue);
        }

    /**
     * Add a request pre-processor.
     * <p>
     * RequestPreprocessors will be called in the order they have been added.
     *
     * @param processor  the {@link RequestPreprocessor} to add
     */
    public void addRequestPreprocessor(RequestPreprocessor processor)
        {
        f_listRequestPreprocessor.add(processor);
        }

    /**
     * Add a request handler pre-processor
     * <p>
     * RequestHandlerPreprocessors will be called in the order they have been added.
     *
     * @param processor  the {@link RequestHandlerPreprocessor} to add
     */
    public void addRequestHandlerPreprocessor(RequestHandlerPreprocessor processor)
        {
        f_listRequestHandlerPreprocessor.add(processor);
        }

    /**
     * Add a GET endpoint.
     *
     * @param sPath    the request path
     * @param handler  the GET request handler
     *
     * @return the added {@link Endpoint}
     */
    public Endpoint addGet(String sPath, RequestHandler handler)
        {
        return addRoute(HttpMethod.GET, sPath, handler);
        }

    /**
     * Add a GET endpoint.
     *
     * @param sPath    the request path
     * @param handler  the GET request handler
     *
     * @return the added {@link Endpoint}
     */
    public Endpoint addGet(String sPath, SimpleRequestHandler handler)
        {
        return addRoute(HttpMethod.GET, sPath, handler);
        }

    /**
     * Add a POST endpoint.
     *
     * @param sPath    the request path
     * @param handler  the POST request handler
     *
     * @return the added {@link Endpoint}
     */
    public Endpoint addPost(String sPath, RequestHandler handler)
        {
        return addRoute(HttpMethod.POST, sPath, handler);
        }

    /**
     * Add a PUT endpoint.
     *
     * @param sPath    the request path
     * @param handler  the PUT request handler
     *
     * @return the added {@link Endpoint}
     */
    public Endpoint addPut(String sPath, RequestHandler handler)
        {
        return addRoute(HttpMethod.PUT, sPath, handler);
        }

    /**
     * Add a DELETE endpoint.
     *
     * @param sPath    the request path
     * @param handler  the DELETE request handler
     *
     * @return the added {@link Endpoint}
     */
    public Endpoint addDelete(String sPath, RequestHandler handler)
        {
        return addRoute(HttpMethod.DELETE, sPath, handler);
        }

    /**
     * Add a request endpoint.
     *
     * @param method   the http method the endpoint handles
     * @param sPath    the request path
     * @param handler  the POST request handler
     *
     * @return the added {@link Endpoint}
     */
    public Endpoint addRoute(HttpMethod method, String sPath, RequestHandler handler)
        {
        // ensure the path has a leading /
        if (sPath.isEmpty() || sPath.charAt(0) != '/')
            {
            sPath = '/' + sPath;
            }

        Endpoint endpoint = new Endpoint(handler)
                .produces(m_asProduces);

        if (method == HttpMethod.POST || method == HttpMethod.PUT)
            {
            endpoint.consumes(m_asConsumes);
            }

        if (sPath.contains("{"))
            {
            String[] asSegment = sPath.split("/");
            StringBuilder s = new StringBuilder();
            for (int i = 1; i < asSegment.length; i++)
                {
                s.append('/');
                String sSegment = asSegment[i];
                int cChar = sSegment.length();
                if (sSegment.charAt(0) == '{' && sSegment.charAt(cChar - 1) == '}')
                    {
                    String sName = sSegment.substring(1, sSegment.length() - 1);
                    s.append("(?<").append(sName).append(">((?!/).)+)");
                    if (i + 1 == asSegment.length)
                        {
                        s.append("(/?)");
                        }
                    }
                else
                    {
                    s.append(sSegment);
                    }
                }
            f_mapRegexRoutes.computeIfAbsent(method, k -> new HashMap<>())
                    .compute(Pattern.compile(s.toString()), (k, list) ->
                        {
                        if (list == null)
                            {
                            list = new ArrayList<>();
                            }
                        list.add(endpoint);
                        return list;
                        });
            }
        else
            {
            f_mapRoutes.computeIfAbsent(method, k -> new HashMap<>())
                    .compute(sPath, (k, list) ->
                        {
                        if (list == null)
                            {
                            list = new ArrayList<>();
                            }
                        list.add(endpoint);
                        return list;
                        });
            }
        return endpoint;
        }

    /**
     * Add the specified {@link Routes} to this {@link RequestRouter}.
     *
     * @param sRootPath  the root path
     * @param routes     the routes to add
     */
    public void addRoutes(String sRootPath, Routes routes)
        {
        routes.addRoutes(this, sRootPath);
        }

    /**
     * Route a {@link HttpRequest} to an endpoint.
     *
     * @param request  the request to route
     *
     * @return  the {@link Response} to send to the caller
     */
    public Response route(HttpRequest request)
        {
        Response response;

        try
            {
            if (!f_listRequestPreprocessor.isEmpty())
                {
                for (RequestPreprocessor processor : f_listRequestPreprocessor)
                    {
                    Optional<Response> opt = processor.process(request);
                    if (opt.isPresent())
                        {
                        // the pre-processor returned a response so we're done
                        return opt.get();
                        }
                    }
                }

            RequestHandler[] aHandlerExact = NO_HANDLERS;
            RequestHandler[] aHandlerRegEx = NO_HANDLERS;
            boolean          fMatchedPath  = false;
            URI              uri   = request.getRequestURI();
            String           sPath = uri.getPath();
            String           sRoot = f_listRootPaths.stream()
                                                   .filter(sPath::startsWith)
                                                   .findFirst()
                                                   .orElse("");

            if (sRoot == null || sRoot.length() == 0)
                {
                // strip the base and try again
                int nBase = request.getBaseURI().getPath().length();
                if (nBase > 1)
                    {
                    sPath = uri.getPath().substring(nBase - 1);
                    sRoot = f_listRootPaths.stream()
                            .filter(sPath::startsWith)
                            .findFirst()
                            .orElse("");
                    }
                }

            // strip any trailing slashes from the path
            sPath = sPath.substring(sRoot.length());
            while(sPath.endsWith("/") && sPath.length() != 1)
                {
                sPath = sPath.substring(0, sPath.length() - 1);
                }

            // if the path is now empty then use a single slash as the path
            if (sPath.isEmpty())
                {
                sPath = "/";
                }

            // Find any exact route matches for the path
            Map<String, List<Endpoint>> mapEndpoint   = f_mapRoutes.get(request.getMethod());
            if (mapEndpoint != null)
                {
                List<Endpoint> listEndpoint = mapEndpoint.get(sPath);
                if (listEndpoint != null && !listEndpoint.isEmpty())
                    {
                    fMatchedPath = true;
                    aHandlerExact = listEndpoint.stream()
                                              .filter(e -> matchesMediaTypes(request, e))
                                              .map(Endpoint::getHandler)
                                              .toArray(RequestHandler[]::new);
                    }
                }

            // Find any regular expression route matches for the path
            Map<Pattern, List<Endpoint>> mapRegexHandler = f_mapRegexRoutes.get(request.getMethod());
            if (mapRegexHandler != null)
                {
                List<RequestHandler> listMatched    = new ArrayList<>();
                Matcher              matcherMatched = null;
                for (Map.Entry<Pattern, List<Endpoint>> entry : mapRegexHandler.entrySet())
                    {
                    Matcher matcher = entry.getKey().matcher(sPath);
                    if (matcher.matches())
                        {
                        fMatchedPath = true;
                        if (matcherMatched == null || matcher.groupCount() <= matcherMatched.groupCount())
                            {
                            // either we have not matched anything yet, or this matcher has the same or fewer groups
                            // than the previous match, so may be a more exact match
                            List<Endpoint> listEndpoint = entry.getValue();
                            if (listEndpoint != null && !listEndpoint.isEmpty())
                                {
                                // Get a list of handlers matching the requested media type
                                List<RequestHandler> list = listEndpoint.stream()
                                        .filter(e -> matchesMediaTypes(request, e))
                                        .map(Endpoint::getHandler)
                                        .map(h -> new RegExRequestHandler(h, matcher))
                                        .collect(Collectors.toList());

                                if (list.size() > 0)
                                    {
                                    // found some matching endpoints
                                    if (matcherMatched == null || matcher.groupCount() < matcherMatched.groupCount())
                                        {
                                        // This matcher has fewer groups than the previous match, hence it is
                                        // a more exact match than the previously matched endpoints; so clear
                                        // out the previous matches
                                        listMatched.clear();
                                        matcherMatched = matcher;
                                        }
                                    listMatched.addAll(list);
                                    }
                                }
                            }
                        }
                    }
                aHandlerRegEx = listMatched.toArray(new RequestHandler[0]);
                }

            if (aHandlerExact.length == 0 && aHandlerRegEx.length == 0)
                {
                if (fMatchedPath)
                    {
                    // there was one or more handlers for the path but none matched the request media types
                    response = Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
                    }
                else
                    {
                    // no handlers found for the request
                    response = Response.notFound().build();
                    }
                }
            else if (aHandlerExact.length == 1)
                {
                // An exact match takes precedence over a reg-ex match
                for (RequestHandlerPreprocessor processor : f_listRequestHandlerPreprocessor)
                    {
                    processor.process(request, aHandlerExact[0]);
                    }
                response = aHandlerExact[0].handle(request);
                }
            else if (aHandlerRegEx.length == 1)
                {
                for (RequestHandlerPreprocessor processor : f_listRequestHandlerPreprocessor)
                    {
                    processor.process(request, aHandlerRegEx[0]);
                    }
                response = aHandlerRegEx[0].handle(request);
                }
            else
                {
                // Multiple possible endpoints were found, this is likely a code error
                Logger.err("Found too many endpoints matching http request path='" + sPath
                                   + " mediaType=");
                response = Response.notFound().build();
                }
            }
        catch (HttpException e)
            {
            response = Response.status(e.getStatus()).entity(e.getMessage()).build();
            }
        catch (Throwable e)
            {
            Logger.err(e);
            response = Response.serverError().build();
            }

        // return response with common response headers
        return response.addHeadersIfNotPresent(f_commonResponseHeaders);
        }

    /**
     * Print all the routes to the log.
     */
    public void dumpRoutes()
        {
        Set<String> setEndpoint = new TreeSet<>();

        for(HttpMethod method : HttpMethod.values())
            {
            for (String sRoot : f_listRootPaths)
                {
                f_mapRoutes.getOrDefault(method, Collections.emptyMap())
                        .keySet()
                        .forEach(s -> setEndpoint.add(method.name() + ": " + sRoot + s));

                f_mapRegexRoutes.getOrDefault(method, Collections.emptyMap())
                        .keySet()
                        .stream()
                        .map(Pattern::pattern)
                        .forEach(s -> setEndpoint.add(method.name() + ": " + sRoot + s));
                }
            }

        Logger.info("Routes:");
        setEndpoint.forEach(s -> Logger.info("    " + s));
        }

    // ----- helper methods -------------------------------------------------

    private boolean matchesMediaTypes(HttpRequest request, Endpoint endpoint)
        {
        // ToDo: should probably implement this correctly if we ever do more than support json
        return true;
        }

    private static String validatePath(String sPath)
        {
        // ensure there is a leading /
        if (sPath.charAt(0) != '/')
            {
            sPath = '/' + sPath;
            }

        // ensure there is no trailing leading /
        if (sPath.charAt(sPath.length() - 1) == '/')
            {
            sPath = sPath.substring(0, sPath.length() - 1);
            }

        return sPath;
        }

    // ----- inner interface: RequestHandler --------------------------------

    /**
     * A handler of http requests.
     */
    @FunctionalInterface
    public interface RequestHandler
        {
        /**
         * Handle a http request.
         *
         * @param request   the request to handle
         *
         * @return the response that will be sent
         */
        Response handle(HttpRequest request);
        }

    // ----- inner interface: SimpleRequestHandler --------------------------

    /**
     * A simple {@link RequestHandler} implementation.
     */
    @FunctionalInterface
    public interface SimpleRequestHandler
            extends RequestHandler
        {
        /**
         * Handles a http request.
         */
        Response handle();

        @Override
        default Response handle(HttpRequest request)
            {
            return handle();
            }
        }

    // ----- inner interface: SimpleRequestHandler --------------------------

    /**
     * A simple reg-ex {@link RequestHandler} implementation.
     */
    public class RegExRequestHandler
            implements RequestHandler
        {
        public RegExRequestHandler(RequestHandler handler, Matcher matcher)
            {
            m_handler = handler;
            m_matcher = matcher;
            }

        @Override
        public Response handle(HttpRequest request)
            {
            request.setPathParameters(new RegexPathParameters(m_matcher));
            return m_handler.handle(request);
            }

        private final RequestHandler m_handler;

        private final Matcher m_matcher;
        }

    // ----- inner class: Endpoint ------------------------------------------

    /**
     * An endpoint that the router will route requests to.
     */
    public static class Endpoint
        {
        /**
         * Create an endpoint wrapping a request handler.
         *
         * @param handler  the request handler
         */
        public Endpoint(RequestHandler handler)
            {
            f_handler = handler;
            }

        /**
         * Return the endpoint {@link RequestHandler}.
         *
         * @return the endpoint {@link RequestHandler}
         */
        public RequestHandler getHandler()
            {
            return f_handler;
            }

        /**
         * Set the consumes media types.
         *
         * @param asMediaType  the consumes media types
         */
        public Endpoint consumes(String... asMediaType)
            {
            m_asConsumes = asMediaType;
            return this;
            }

        /**
         * Returns the consumes media types.
         *
         * @return the consumes media types
         */
        public String[] getConsumes()
            {
            return m_asConsumes;
            }

        /**
         * Set the produces media types.
         *
         * @param asMediaType  the produces media types
         */
        public Endpoint produces(String... asMediaType)
            {
            m_asProduces = asMediaType;
            return this;
            }

        /**
         * Returns the produces media types.
         *
         * @return the produces media types
         */
        public String[] getProduces()
            {
            return m_asProduces;
            }

        /**
         * Add {@link RequestHandlerPreprocessor} instances that will be called
         * before the request handler.
         *
         * @param processors  the pre-processors to add
         *
         * @return this {@link Endpoint}
         */
        public Endpoint requestPreProcessors(RequestHandlerPreprocessor... processors)
            {
            f_aPreProcessor = processors;
            return this;
            }

        /**
         * Return the array of request pre-processors.
         *
         * @return the array of request pre-processors
         */
        public RequestHandlerPreprocessor[] getRequestPreProcessors()
            {
            return f_aPreProcessor;
            }

        // ----- data members -----------------------------------------------

        /**
         * The endpoint's request handler.
         */
        private final RequestHandler f_handler;

        /**
         * Any pre-processors to call before calling the handler.
         */
        private RequestHandlerPreprocessor[] f_aPreProcessor;

        /**
         * The list of produces media types.
         */
        private String[] m_asProduces;

        /**
         * The list of consumes media types.
         */
        private String[] m_asConsumes;
        }

    // ----- inner interface: Routes ----------------------------------------

    /**
     * A class that can configure a router with routes to endpoints.
     */
    public interface Routes
        {
        void addRoutes(RequestRouter router, String sPathRoot);
        }

    // ----- inner interface: RequestPreprocessor ---------------------------

    /**
     * A pre-processor for http requests.
     */
    public interface RequestPreprocessor
        {
        /**
         * Process the specified http request.
         *
         * @param request  the request to process
         *
         * @return  an optional {@link Response} which, if supplied will cause that response
         *          to be sent to the caller and no further processing to occur
         */
        Optional<Response> process(HttpRequest request);
        }

    // ----- inner interface: RequestHandlerPreprocessor --------------------

    /**
     * A pre-processor for http requests before they are sent to a handler.
     */
    public interface RequestHandlerPreprocessor
        {
        /**
         * Process the specified http request before it is sent to the specified {@link RequestHandler}.
         *
         * @param request  the request to process
         * @param handler  the {@link RequestHandler} that will handle the request
         */
        void process(HttpRequest request, RequestHandler handler);
        }

    // ----- constants ------------------------------------------------------

    private static final RequestHandler[] NO_HANDLERS = new RequestHandler[0];

    // ----- data members ---------------------------------------------------

    /**
     * The list of request root paths.
     */
    private final List<String> f_listRootPaths;

    /**
     * A map of exact path endpoints.
     */
    private final Map<HttpMethod, Map<String, List<Endpoint>>> f_mapRoutes = new HashMap<>();

    /**
     * A map of regular expression path matching endpoints.
     */
    private final Map<HttpMethod, Map<Pattern, List<Endpoint>>> f_mapRegexRoutes = new HashMap<>();

    /**
     * The default produces media types.
     */
    private String[] m_asProduces = new String[0];

    /**
     * The default consumes media types.
     */
    private String[] m_asConsumes = new String[0];

    /**
     * Common headers to add to the response.
     */
    private final Headers f_commonResponseHeaders = new Headers();

    /**
     * Common request pre-processors.
     */
    private final List<RequestPreprocessor> f_listRequestPreprocessor = new ArrayList<>();

    /**
     * Common request handler pre-processors.
     */
    private final List<RequestHandlerPreprocessor> f_listRequestHandlerPreprocessor = new ArrayList<>();
    }
