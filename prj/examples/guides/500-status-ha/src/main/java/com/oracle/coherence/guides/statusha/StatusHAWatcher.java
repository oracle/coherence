/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.oracle.coherence.guides.statusha.fetcher.DataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.HTTPDataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.JMXDataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.MBeanServerProxyDataFetcher;

import com.oracle.coherence.guides.statusha.model.ServiceData;
import com.tangosol.discovery.NSLookup;
import com.tangosol.util.Base;
import javax.management.remote.JMXServiceURL;

import static com.oracle.coherence.guides.statusha.fetcher.AbstractDataFetcher.LIST_STATUS;

/**
 * An example of a utility to check the StatusHA values for services or individual service. This is useful
 * when you want to perform a rolling upgrade and ensure that your services are in a safe state while continuing.
 * <p>
 * This utility can connect to a cluster using a number of methods:
 * <ol>
 *     <li>Using MBeanServerProxy as a customer member</li>
 *     <li>Using JMX URL</li>
 *     <li>Using host:port</li>
 *     <li>Using Http to connect to Management over REST</li>
 * </ol>
 * <p>
 * It also has various options explained in the usage() method.
 *
 * @author tam 2021.08.02
 */
public class StatusHAWatcher {

    private static final String TYPE_JMX = "JMX";
    private static final String TYPE_MBS = "Cluster MBean Server";
    private static final String TYPE_HTTP = "HTTP";

    /**
     * Main entrypoint
     *
     * @param args arguments
     */
    public static void main(String[] args) throws MalformedURLException {
        int delaySeconds = 5;
        String serviceName = null; // default to all services
        String connectionType = TYPE_MBS;
        String url = null;
        int timeout = -1;
        String statusHaValue = null;

        // process the command line arguments
        int i = 0;
        int nLength = args.length;
        while (i < nLength) {
            if ("-d".equals(args[i])) {
                if (++i >= nLength) {
                    usage("Delay must be supplied");
                }
                try {
                    delaySeconds = Integer.parseInt(args[i++]);
                } catch (Exception e) {
                    usage("Invalid value for delay of [" + args[i - 1] + "]");
                }
            } else if ("-t".equals(args[i])) {
                if (++i >= nLength) {
                    usage("Timeout must be supplied");
                }
                try {
                    timeout = Integer.parseInt(args[i++]);
                } catch (Exception e) {
                    usage("Invalid value for timeout of [" + args[i - 1] + "]");
                }
            } else if ("-s".equals(args[i])) {
                if (++i >= nLength) {
                    usage("Service name must be supplied");
                }
                serviceName = args[i++];
            } else if ("-w".equals(args[i])) {
                if (++i >= nLength) {
                    usage("StatusHA value must be supplied");
                }
                statusHaValue = args[i++];
            } else if ("-m".equals(args[i])) {
                connectionType = TYPE_MBS;
                i++;
            } else if ("-j".equals(args[i]) || "-h".equals(args[i])) {
                boolean isJMX = "-j".equals(args[i]);
                if (++i >= nLength) {
                    usage((isJMX ? "JMX" : "HTTP") + " URL must be specified");
                }
                connectionType = isJMX ? TYPE_JMX : TYPE_HTTP;
                url = args[i++];
            } else if ("-hp".equals(args[i]) || "-n".equals(args[i])) {
                boolean isNameService = "-n".equals(args[i]);
                if (++i >= nLength) {
                    usage("host:port must be supplied");
                }
                try {
                    if (isNameService) {
                        String[] hostPort = args[i++].split(":");
                        JMXServiceURL jmxServiceURL = NSLookup.lookupJMXServiceURL(
                                new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1])));
                        url = jmxServiceURL.toString();
                    } else {
                        url = "service:jmx:rmi:///jndi/rmi://" + args[i++] + "/jmxrmi";
                    }
                    connectionType = TYPE_JMX;
                } catch (NumberFormatException e) {
                    usage("Invalid value for host:port of [" + args[i - 1] + "]");
                } catch (Exception ioe) {
                    usage("Unable to connect to NameService on " + args[ i -1] + ", " + ioe.getMessage());
                }
            } else if ("-u".equals(args[i])) {
                usage("");
                i++;
            } else {
                usage("Invalid argument [" + args[i] + "]");
            }
        }

        if (statusHaValue != null && timeout == -1 ||
            statusHaValue == null && timeout > 0) {
            usage("Both -w and -t must be specified");
        }

        if (statusHaValue != null && !LIST_STATUS.contains(statusHaValue)) {
            usage("Invalid value for status HA of [" + statusHaValue + "]");
        }

        // Display useful information
        System.out.println("\nConnection:   " + connectionType);
        if (url != null) {
            System.out.println("URL:          " + url);
        }
        System.out.println("Service:      " + (serviceName == null ? "all" : serviceName));
        System.out.println("Delay:        " + delaySeconds + " seconds\n");

        if (statusHaValue != null) {
            System.out.println("Waiting for:  " + statusHaValue);
            System.out.println("Timeout:      " + timeout + " seconds\n");
        }

        DataFetcher dataFetcher =
                  connectionType.equals(TYPE_MBS) ? new MBeanServerProxyDataFetcher(serviceName)
                : connectionType.equals(TYPE_HTTP) ? new HTTPDataFetcher(url, serviceName)
                : new JMXDataFetcher(url, serviceName);

        // get initial set of services to setup header format
        Set<String> setServices = dataFetcher.getServiceNames();

        int maxLength = setServices.stream().mapToInt(String::length).max().orElse(0);

        int serviceCount = setServices.size();
        if (serviceCount == 0) {
            usage("No services found");
        }

        String headerFormat = "%-32s %-" + (maxLength + 2) + "s %15s  %-15s %11s %11s %11s %11s  %-12s\n";
        String dataFormat = "%-32s %-" + (maxLength + 2) + "s %,15d  %-15s %,11d %,11d %,11d %,11d  %s\n";

        System.out.println("\nCluster Name: " + dataFetcher.getClusterName() + " (" + dataFetcher.getClusterVersion() + ")");

        if (statusHaValue == null) {
            System.out.println("\nPress CTRL-C to quit");
        }
        else {
            System.out.println("Waiting for Status HA value of " + statusHaValue + " or better within " + timeout + " seconds");
        }

        int c = 0;
        int pageSize = 20;
        long startTime = System.currentTimeMillis();
        Set<String> setStatusHAValues = new HashSet<>();

        // continue until the user issues CTRL-C
        while (true) {
            if (c++ % (pageSize / serviceCount) == 0) {
                displayHeader(headerFormat);
            }

            setStatusHAValues.clear();

            dataFetcher.getStatusHaData()
                       .stream()
                       .sorted(Comparator.comparing(ServiceData::getServiceName))
                       .forEach(data -> {
                           System.out.printf(dataFormat,
                                   new Date().toString(),
                                   data.getServiceName(),
                                   data.getStorageCount(),
                                   data.getHAStatus(),
                                   data.getPartitionCount(),
                                   data.getPartitionsEndangered(),
                                   data.getPartitionsVulnerable(),
                                   data.getPartitionsUnbalanced(),
                                   data.getStatus());
                           setStatusHAValues.add(data.getHAStatus());
                       });

            if (statusHaValue != null) {
                long now = System.currentTimeMillis();

                // check that the statusHA value is safer than what was requested and if so, then exit successfully
                if (setStatusHAValues.size() == 1 && isStatusHASaferThan(setStatusHAValues.iterator().next(), statusHaValue)) {
                    System.out.println("Status HA value of " + statusHaValue + " or better reached in " + ((now - startTime) / 1000) + " seconds");
                    System.exit(0);
                }
                if (timeout != -1 && now- startTime > timeout * 1000L) {
                    System.out.println("Status HA value of " + statusHaValue + " or better NOT reached in " + timeout + " seconds");
                    System.exit(1);
                }
            }

            Base.sleep(delaySeconds * 1000L);
            if (serviceCount > 1) {
                System.out.println("");
            }
        }
    }

    /**
     * Return true if the statusHA value is safer that the safestStatusHA value. E.g. MACHINE-SAFE is safer than NODE-SAFE,
     * but MACHINE-SAFE is not safer than RACK-SAFE.
     *
     * @param statusHAValue   the statusHA value to check
     * @param safestStatusHA  the safest value that will match
     * @return true if the statusHA value is safer that the safestStatusHA value.
     */
    private static boolean isStatusHASaferThan(String statusHAValue, String safestStatusHA) {
        int thisIndex   = LIST_STATUS.indexOf(statusHAValue);
        int safestIndex = LIST_STATUS.indexOf(safestStatusHA);
        return thisIndex >= safestIndex;

    }

    /**
     * Display the header.
     *
     * @param headerFormat the header format to use
     */
    private static void displayHeader(String headerFormat) {
        System.out.println("");
        System.out.printf(headerFormat, "Date/Time", "Service Name", "Storage Count", "StatusHA",
                "Partitions", "Endangered", "Vulnerable", "Unbalanced", "Status");
        System.out.printf(headerFormat, "----------------------------", "------------", "--------------",
                "------------", "-----------",
                "-----------", "-----------", "-----------", "-------------");
    }

    /**
     * Display usage information and exit.
     *
     * @param errorMessage optional error message
     */
    private static void usage(String errorMessage) {
        String usageMessage = errorMessage +
                "\n\n" +
                "Usage: StatusHAWatcher [options]\n\n" +
                "Connection options:\n" +
                " -m               Use MBeanServer from cluster. Requires correct cluster config to join cluster. (Default)\n" +
                " -h url           Use Management over REST to connect to cluster\n" +
                " -j url           Use JMX URL to connect to cluster\n" +
                " -hp host:port    Connect to a JMX process via host:port\n" +
                " -n host:port     Use NameService to connect via cluster host:port\n\n" +
                "Other Options:\n" +
                " -d delay         Delay between each check in seconds\n" +
                " -s service       Service name to monitor or all services if not specified\n" +
                " -t timeout       Timeout to wait for statusHA value (used in conjunction with -w below)\n" +
                " -w statusHaValue Wait until the statusHA value for all services is safer than the specified value and\n" +
                "                  return 0 if it is, or non-zero if the timeout has been reached\n" +
                " -u               Display usage\n\n" +
                "StatusHA meanings:\n" +
                "  ENDANGERED - abnormal termination of any cluster node that runs this service may cause data loss\n" +
                "  NODE-SAFE - any single cluster node could be stopped without data loss\n" +
                "  MACHINE-SAFE - all the cluster nodes running on any given machine could be stopped at once without data loss\n" +
                "  RACK-SAFE - all the cluster nodes running on any given rack could be stopped at once without data loss\n" +
                "  SITE-SAFE - all the cluster nodes running on any given rack could be stopped at once without data loss\n\n" +
                "Partition meanings:\n" +
                "  Endangered  - The total number of partitions that are not currently backed up\n" +
                "  Vulnerable  - The total number of partitions that are backed up on the same machine\n" +
                "                where the primary partition owner resides\n" +
                "  Unbalanced  - The total number of primary and backup partitions which remain to be transferred until the\n" +
                "                partition distribution across the storage enabled service members is fully balanced\n" +
                "  Remaining   - The number of partition transfers that remain to be completed before the service\n" +
                "                achieves the goals set by this strategy\n";
        System.err.println(usageMessage);
        System.exit(1);
    }
}
