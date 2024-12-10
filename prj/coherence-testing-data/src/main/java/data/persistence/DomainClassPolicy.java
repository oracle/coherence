/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.persistence;


/**
* Policy used to set the domain class to use for testing.
*/
public abstract class DomainClassPolicy
    {
    public abstract Object createPk(int nId);

    public abstract Object getPkFromEntity(Object o);

    public abstract Object newEntity(int nId, String sName);

    public static class PersonClass
            extends DomainClassPolicy
        {
        public Object createPk(int nId)
            {
            return nId;
            }

        public Object getPkFromEntity(Object obj)
            {
            return ((Person) obj).getId();
            }

        public Object newEntity(int nId, String sName)
            {
            return new Person(nId, sName);
            }
        }

    public static class CompoundPerson1Class
            extends DomainClassPolicy
        {
        public Object createPk(int nId)
            {
            return new PersonId(nId, String.valueOf(nId));
            }

        public Object getPkFromEntity(Object obj)
            {
            return new PersonId(((CompoundPerson1) obj).getId(),
                    ((CompoundPerson1) obj).getIdString());
            }

        public Object newEntity(int nId, String sName)
            {
            return new CompoundPerson1(nId, String.valueOf(nId), sName);
            }
        }

    public static class CompoundPerson2Class
            extends DomainClassPolicy
        {
        public Object createPk(int nId)
            {
            return new PersonId(nId, String.valueOf(nId));
            }

        public Object getPkFromEntity(Object obj)
            {
            return ((CompoundPerson2) obj).getPid();
            }

        public Object newEntity(int nId, String sName)
            {
            return new CompoundPerson2(nId, String.valueOf(nId), sName);
            }
        }
    }