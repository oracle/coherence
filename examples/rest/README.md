# Coherence REST Example

## Overview

This example shows a basic application built using Oracle JavaScript Extension Toolkit (JET)
and how Coherence REST endpoints can be integrated into such an application.

This application showcases various features including:

* Accessing, creating and deleting cache entries
* Querying and sorting cache entries 
* Calling named entry processors 
* Using JSON pass-through to natively store and retrieve JSON documents in a cache
* Using Binary pass-through to store binary objects in a cache 
* Using Server Sent Events (SSE) to respond to cache changes

<img src="../assets/rest-application.png" width="800"/>

## Table of Contents

* [Prerequisites](#prerequisites)
* [Build Instructions](#build-instructions)
* [Running the Example](#running-the-example)
* [Accessing the UI](#accessing-the-ui)
* [Changing the Hostname or Port](#changing-the-hostname-or-port)
* [References](#references)

## Prerequisites

In order to build and run the examples, you must have the following installed:

* Maven 3.5.4+
* JDK 11+
* A web browser supported by Oracle JET version 9.0.0

> Note: Internet Explorer does not support Server Sent Events (SSE) and as such you must
> use a supported browser for the SSE example. All other examples here will work with
> Internet Explorer.
> Refer to: http://www.w3schools.com/html/html5_serversentevents.asp

## Build Instructions

Issue the following to build the REST example:

```bash
mvn clean compile
```

## Running the Example

First, start a cache server and http proxy using:

```bash
mvn exec:exec -DhttpProxy
```

Once started, reference code data for Countries and States will be loaded and
the application startup page will be automatically loaded.

Although not required, you can also start additional cache servers (without HTTP server)
by using:

```bash
mvn exec:exec -DcacheServer
```

## Accessing the UI

To access the application, open the following URL (if not already opened).

    http://127.0.0.1:8080/application/index.html

* *Departments*

   This page shows how to query, create, update, remove or populate default
   departments using standard REST API's in Coherence.

* *Products*

   This page shows how to query, create, update, remove or populate default
   products using standard REST API's in Coherence.

   There are also custom entry processors to increase product prices and
   receive more stock of an item.

* *Contacts*

   This page shows how to query, create, update, remove or populate default
   contacts using standard REST API's in Coherence.  The contacts also
   has composite keys and shows the use of a KeyConverter class to work with these keys.

   There are also examples of how to sort queries that are returned from REST calls.
   
   The objects used in this examples serialized using the Portable Object Format (POF). Classes
   have been instrumented with the `PortableType` annotation and the [POF Maven Plugin](https://github.com/oracle/coherence/tree/master/prj/plugins/maven/pof-maven-plugin)
   used to automatically generate consistent (and correct) implementations of Evolvable POF serialization methods.

* *Server Sent Events (SSE)*

   This page uses SSE to listen for events from the Coherence REST API's.

* *JSON Pass Through*

   This page shows how to insert native JSON objects into a cache via Coherence REST.

* *Binary Pass Through*

   This page shows how to insert Binary objects into a cache via Coherence REST.

For most pages, the REST operations for each operation are displayed in a
message box at the top of the screen after the operations complete.

## Changing the Hostname or Port

The HTTP server listens on all IP Addresses but you can change the
address and port that the application runs on by passing the following to your
`mvn exec:exec` command:

```bash
mvn exec:exec -DhttpProxy -Dhttp.address=x.x.x.x -Dhttp.port=7777
```      

##References

* [Coherence Community Website](https://coherence.community/)
* [Coherence Community Edition (CE) - GitHub](https://github.com/oracle/coherence)
* [Oracle JavaScript Extension Toolkit (JET)](https://www.oracle.com/webfolder/technetwork/jet/index.html)
