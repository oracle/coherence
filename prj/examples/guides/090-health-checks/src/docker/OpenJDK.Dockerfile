# Copyright (c) 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
FROM openjdk:11-jre

ADD coherence.jar /coherence/lib/coherence.jar

ENV COHERENCE_HEALTH_HTTP_PORT=6676

HEALTHCHECK  --start-period=30s --interval=30s \
        CMD curl -f http://127.0.0.1:6676/ready || exit 1

ENTRYPOINT ["java"]
CMD ["-cp", "/coherence/lib/*", "com.tangosol.net.Coherence"]
