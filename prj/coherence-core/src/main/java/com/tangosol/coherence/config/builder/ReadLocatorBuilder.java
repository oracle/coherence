/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.ServiceInfo;

import com.tangosol.net.partition.Ownership;

import com.tangosol.util.Base;

import java.util.function.BiFunction;


/**
 * The {@link ReadLocatorBuilder} class builds a {@link BiFunction} that given
 * the current {@link Ownership ownership} of a partition and {@link PartitionedService
 * service} will return a {@link Member member} to target reads against.
 *
 * @author hr
 * @since 21.12
 */
public class ReadLocatorBuilder
        extends DefaultBuilderCustomization<BiFunction<Ownership, PartitionedService, Member>>
        implements ParameterizedBuilder<BiFunction<Ownership, PartitionedService, Member>>
    {
    // ----- UnitCalculatorBuilder methods  ---------------------------------

    /**
     * Return a string that represents the member to target reads against. Valid
     * values are:
     * <ul>
     *     <li>primary</li>
     *     <li>closest</li>
     *     <li>random</li>
     *     <li>random-backup</li>
     * </ul>
     *
     * @param resolver  the {@link ParameterResolver}
     *
     * @return a string that represents the member to target reads against
     */
    public String getMemberLocatorType(ParameterResolver resolver)
        {
        return m_exprLocator.evaluate(resolver);
        }

    /**
     * Set the string that represents the member to target reads against. Valid
     * values are:
     * <ul>
     *     <li>primary</li>
     *     <li>closest</li>
     *     <li>random</li>
     *     <li>random-backup</li>
     * </ul>
     *
     * @param expr  the string that represents the member to target reads against
     */
    @Injectable
    public void setMemberLocatorType(Expression<String> expr)
        {
        m_exprLocator = expr;
        }

    // ----- ParameterizedBuilder methods  ----------------------------------

    @Override
    public BiFunction<Ownership, PartitionedService, Member> realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        ParameterizedBuilder<BiFunction<Ownership, PartitionedService, Member>> bldr = getCustomBuilder();

        if (bldr == null)
            {
            // use a built-in calculator
            String sType = getMemberLocatorType(resolver).toUpperCase();

            switch (sType)
                {
                case "PRIMARY":
                    return null;
                case "CLOSEST":
                    return CLOSEST;
                case "RANDOM":
                    return RANDOM;
                case "RANDOM-BACKUP":
                    return RANDOM_BACKUP;
                }

            throw new ConfigurationException("Unexpected read locator type: " + sType,
                    "Choose one of the following read-locator types: primary, closest, random, or random-backup");
            }

        // create a custom calculator
        return bldr.realize(resolver, loader, listParameters);
        }

    // ----- constants ------------------------------------------------------

    /**
     * A BiFunction implementation that returns the primary member of the ownership
     * chain.
     */
    public static final BiFunction<Ownership, PartitionedService, Member> PRIMARY =
            (owners, service) -> service.getInfo().getServiceMember(owners.getPrimaryOwner());

    /**
     * A BiFunction implementation that returns a random member of the ownership
     * chain.
     */
    public static final BiFunction<Ownership, PartitionedService, Member> RANDOM = (owners, service) ->
        {
        int cBackups = owners.getBackupCount();
        if (cBackups < 1)
            {
            return PRIMARY.apply(owners, service);
            }

        int iStore = Base.getRandom().nextInt(cBackups + 1);

        return service.getInfo().getServiceMember(owners.getOwner(iStore));
        };

    /**
     * A BiFunction implementation that returns a random backup member of the
     * ownership chain.
     */
    public static final BiFunction<Ownership, PartitionedService, Member> RANDOM_BACKUP = (owners, service) ->
        {
        int cBackups = owners.getBackupCount();
        if (cBackups < 1)
            {
            return PRIMARY.apply(owners, service);
            }

        int iStore = Base.getRandom().nextInt(cBackups) + 1;

        return service.getInfo().getServiceMember(owners.getOwner(iStore));
        };

    /**
     * A BiFunction implementation that returns a member that is 'closest' to
     * this member based on provided metadata (member, machine, rack, or site).
     */
    public static final BiFunction<Ownership, PartitionedService, Member> CLOSEST = (owners, service) ->
            {
            int cBackups = owners.getBackupCount();
            if (cBackups < 1)
                {
                return PRIMARY.apply(owners, service);
                }

            ServiceInfo info = service.getInfo();

            Member memberThis    = service.getCluster().getLocalMember();
            Member memberClosest = null;

            for (int i = 0; i <= cBackups; ++i)
                {
                Member member = info.getServiceMember(owners.getOwner(i));
                if (member != null)
                    {
                    if (memberClosest == null)
                        {
                        memberClosest = member;
                        }

                    if (memberThis.equals(member))
                        {
                        return member;
                        }
                    else if (memberThis.getMachineId() == member.getMachineId())
                        {
                        return member;
                        }
                    else if (Base.equals(memberThis.getRackName(), member.getRackName()) &&
                             !Base.equals(memberClosest.getRackName(), memberThis.getRackName()))
                        {
                        memberClosest = member;
                        }
                    else if (Base.equals(memberThis.getSiteName(), member.getSiteName()) &&
                             !Base.equals(memberClosest.getSiteName(), memberThis.getSiteName()))
                        {
                        memberClosest = member;
                        }
                    }
                }

            return memberClosest;
            };


    // ----- data members ---------------------------------------------------

    /**
     * The UnitCalculator type.
     */
    private Expression<String> m_exprLocator = new LiteralExpression<String>("PRIMARY");
    }
