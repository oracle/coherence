/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.component.net.Security;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.SimplePartitionKey;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import javax.security.auth.Subject;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GetPrincipalNameProcessor
        extends AbstractProcessor<SimplePartitionKey, String, Set<String>>
        implements ExternalizableLite, RemoteCallable<Set<String>>
    {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Set<String> process(InvocableMap.Entry<SimplePartitionKey, String> entry)
        {
        Logger.info("Entered GetPrincipalNameProcessor process()");
        // get the entry value to trigger the capturing authorizer
        entry.getValue();

        Subject subject = CapturingAuthorizer.getSubject(entry.asBinaryEntry().getBinaryKey());
        if (subject == null)
            {
            Logger.info("Leaving GetPrincipalNameProcessor process(), subject=" + subject);
            return Collections.emptySet();
            }
        Set<String> set = subject.getPrincipals()
                .stream()
                .map(Principal::getName)
                .collect(Collectors.toSet());
        Logger.info("In GetPrincipalNameProcessor process(), principals=" + set);
        entry.setValue("foo");
        return set;
        }

    @Override
    public Set<String> call() throws Exception
        {
        Logger.info("Entered GetPrincipalNameProcessor call()");
        Subject subject = Security.login(new CertAliasCallBackHandler());
        Logger.info("In GetPrincipalNameProcessor call(), subject=" + subject);

        Map<SimplePartitionKey, Set<String>> mapResult =
                Subject.doAs(subject, (PrivilegedAction<Map<SimplePartitionKey, Set<String>>>) () ->
            {
            Logger.info("In GetPrincipalNameProcessor call(), PrivilegedAction, subject=" + SecurityHelper.getCurrentSubject());
            Session session = Coherence.getInstance().getSession();
            NamedMap<SimplePartitionKey, String> map = session.getCache("test");

            // we invoke against every partition to ensure that the subject is propagated to all cluster members
            Set<SimplePartitionKey> setKey = new HashSet<>();
            for (int i = 0; i < 257; i++)
                {
                setKey.add(SimplePartitionKey.getPartitionKey(i));
                }

            return map.invokeAll(setKey, new GetPrincipalNameProcessor());
            });

        Set<String> set = mapResult.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Logger.info("In GetPrincipalNameProcessor call(), principals=" + set);
        return set;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }
    }
