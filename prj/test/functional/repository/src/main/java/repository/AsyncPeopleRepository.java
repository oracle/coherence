/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package repository;

import com.oracle.coherence.repository.AbstractAsyncRepository;
import com.oracle.coherence.repository.Accelerated;
import com.tangosol.net.AsyncNamedMap;

import data.repository.Person;

/**
 * Simple test async repository implementation.
 *
 * @since 21.06
 */
@Accelerated
// tag::doc[]
public class AsyncPeopleRepository
        extends AbstractAsyncRepository<String, Person>
    {
    private final AsyncNamedMap<String, Person> people;

    public AsyncPeopleRepository(AsyncNamedMap<String, Person> people)
        {
        this.people = people;
        }

    protected AsyncNamedMap<String, Person> getMap()          // <1>
        {
        return people;
        }

    protected String getId(Person entity)                     // <2>
        {
        return entity.getSsn();
        }

    protected Class<? extends Person> getEntityType()         // <3>
        {
        return Person.class;
        }
    }
// end::doc[]
