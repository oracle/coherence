/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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
public class AsyncPeopleRepository
        extends AbstractAsyncRepository<String, Person>
    {
    private AsyncNamedMap<String, Person> people;

    public AsyncPeopleRepository(AsyncNamedMap<String, Person> people)
        {
        this.people = people;
        }

    protected AsyncNamedMap<String, Person> getMap()
        {
        return people;
        }

    protected String getId(Person entity)
        {
        return entity.getSsn();
        }

    protected Class<? extends Person> getEntityType()
        {
        return Person.class;
        }
    }
