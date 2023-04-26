/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.coherence.common.base.Randoms;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tangosol.net.topic.Subscriber.completeOnEmpty;
import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"resource", "unchecked"})
public class PubSubRollingUpgradeTests
        extends BaseTopicsTests
    {
    @Test
    public void shouldContinuePubSubAfterUpgraded(TestInfo info) throws Exception
        {
        try (ClosableCluster clusterExtension = createRunningCluster(info.getDisplayName(), Version.Previous))
            {
            ConfigurableCacheFactory ccf        = clusterExtension.createSession(SessionBuilders.storageDisabledMember());
            NamedTopic<String>       topic      = ccf.ensureTopic(info.getDisplayName());
            String                   sGroupName = "test-group";
            try
                {
                try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroupName), completeOnEmpty());
                     Publisher<String>  publisher  = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                    {
                    Set<String> setPublished = new HashSet<>();
                    Set<String> setReceived  = new HashSet<>();
                    String      sSuffix      = Randoms.getRandomString(500, 500, true);

                    for (int i = 0; i < 750; i++)
                        {
                        String sMsg = "Message-" + i + sSuffix;
                        publisher.publish(sMsg).get(1, TimeUnit.MINUTES);
                        setPublished.add(sMsg);
                        }

                    Subscriber.Element<String> element = subscriber.receive().get(1, TimeUnit.MINUTES);
                    assertThat(element, is(notNullValue()));
                    int cReceived = 0;
                    while (element != null)
                        {
                        setReceived.add(element.getValue());
                        cReceived++;
                        if (cReceived >= 750)
                            {
                            break;
                            }
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        }

                    clusterExtension.relaunch(Version.Current);

                    for (int i = 750; i < 1500; i++)
                        {
                        String sMsg = "Message-" + i + sSuffix;
                        publisher.publish(sMsg).get(1, TimeUnit.MINUTES);
                        setPublished.add(sMsg);
                        }

                    element = subscriber.receive().get(1, TimeUnit.MINUTES);
                    while (element != null)
                        {
                        setReceived.add(element.getValue());
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        }

                    assertThat(setReceived, is(setPublished));
                    }
                }
            finally
                {
                topic.destroy();
                }
            }
        }
    }
