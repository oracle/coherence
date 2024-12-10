# Copyright (c) 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
FROM gcr.io/distroless/java11-debian11

# Configure the image's health check command
# Health checks will only work with Coherence 22.06 and later
HEALTHCHECK  --start-period=10s --interval=30s \
    CMD ["java", \
    "-cp", "/app/libs/coherence.jar", \
    "com.tangosol.util.HealthCheckClient", \
    "http://127.0.0.1:6676/ready", \
    "||", "exit", "1"]

# Expose any default ports
# The default Coherence Extend port
EXPOSE 20000
# The default Coherence gRPC port
EXPOSE 1408
# The default Coherence metrics port
EXPOSE 9612

# Set the entry point to be the Java command to run
ENTRYPOINT ["java", "-cp", "/app/classes:/app/libs/*", "com.tangosol.net.Coherence"]

# Set any environment variables
# Set the health check port to a fixed value (corresponding to the command above)
ENV COHERENCE_HEALTH_HTTP_PORT=6676
# Fix the Extend Proxy to listen on port 20000
ENV COHERENCE_EXTEND_PORT=20000
# Enable Coherence metics
ENV COHERENCE_METRICS_HTTP_ENABLED=true
# Set the Coherence log level to debug logging
ENV COHERENCE_LOG_LEVEL=9
# Effectively disabled multicast, which does not work in containers anyway
ENV COHERENCE_TTL=0

# Copy all the application files into the /app directory in the image
# This is the default structure supported by the Coherence Operator
COPY app app
