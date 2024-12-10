/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package repository;

import com.oracle.coherence.repository.AbstractRepository;
import com.oracle.coherence.repository.Accelerated;
import com.tangosol.net.NamedMap;

import data.repository.Person;

/**
 * Simple test repository implementation.
 *
 * @author Aleks Seovic  2021.02.12
 */
@Accelerated
// tag::doc[]
public class PeopleRepository
        extends AbstractRepository<String, Person>
    {
    private final NamedMap<String, Person> people;

    public PeopleRepository(NamedMap<String, Person> people)
        {
        this.people = people;
        }

    protected NamedMap<String, Person> getMap()            // <1>
        {
        return people;
        }

    protected String getId(Person person)                  // <2>
        {
        return person.getSsn();
        }

    protected Class<? extends Person> getEntityType()      // <3>
        {
        return Person.class;
        }
    }
// end::doc[]
