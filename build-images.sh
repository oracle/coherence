#!/usr/bin/env bash
#
# Copyright (c) 2000, 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#

# ---------------------------------------------------------------------------
# This script uses Buildah to build a multi-architecture Coherence image.
# The architectures built are linux/amd64 and linux/arm64.
# The images are pushed to the local Docker daemon unless NO_DAEMON=true.
# ---------------------------------------------------------------------------
set -x

BASEDIR=$(dirname "$0")

# Ensure the IMAGE_NAME has been set - this is the name of the image to build
if [ "${IMAGE_NAME}" == "" ]
then
  echo "ERROR: No IMAGE_NAME environment variable has been set"
  exit 1
fi
# Ensure the AMD_BASE_IMAGE has been set - this is the name of the base image for amd64
if [ "${AMD_BASE_IMAGE}" == "" ]
then
  echo "ERROR: No AMD_BASE_IMAGE environment variable has been set"
  exit 1
fi
# Ensure the ARM_BASE_IMAGE has been set - this is the name of the base image for arm64
if [ "${ARM_BASE_IMAGE}" == "" ]
then
  echo "ERROR: No ARM_BASE_IMAGE environment variable has been set"
  exit 1
fi
# Ensure the GRAAL_AMD_BASE_IMAGE has been set - this is the name of the base image for amd64
if [ "${GRAAL_AMD_BASE_IMAGE}" == "" ]
then
  echo "ERROR: No GRAAL_AMD_BASE_IMAGE environment variable has been set"
  exit 1
fi
# Ensure the GRAAL_ARM_BASE_IMAGE has been set - this is the name of the base image for arm64
if [ "${GRAAL_ARM_BASE_IMAGE}" == "" ]
then
  echo "ERROR: No ARM_BASE_IMAGE environment variable has been set"
  exit 1
fi

# Ensure there is a default architecture - if not set we assume amd64
if [ "${IMAGE_ARCH}" == "" ]
then
  IMAGE_ARCH="amd64"
fi

# Ensure there is a main class set
if [ "${MAIN_CLASS}" == "" ]
then
  MAIN_CLASS=com.tangosol.net.Coherence
fi

# Ensure there is an extend port set
if [ "${PORT_EXTEND}" == "" ]
then
  PORT_EXTEND=20000
fi

# Ensure there is a concurrent extend port set
if [ "${PORT_CONCURRENT_EXTEND}" == "" ]
then
  PORT_CONCURRENT_EXTEND=20001
fi

# Ensure there is a gRPC port set
if [ "${PORT_GRPC}" == "" ]
then
  PORT_GRPC=1408
fi

# Ensure there is a management port set
if [ "${PORT_MANAGEMENT}" == "" ]
then
  PORT_MANAGEMENT=30000
fi

# Ensure there is a metrics port set
if [ "${PORT_METRICS}" == "" ]
then
  PORT_METRICS=9612
fi

# Ensure there is a health port set
if [ "${PORT_HEALTH}" == "" ]
then
  PORT_HEALTH=6676
fi

# we must use docker format to use health checks
export BUILDAH_FORMAT=docker

# Build the entrypoint command line.
ENTRY_POINT="java"

CLASSPATH="/coherence/ext/conf:/coherence/ext/lib/*:/app/resources:/app/classes:/app/libs/*"

# The command line
CMD=""
CMD="${CMD} -cp ${CLASSPATH}"
CMD="${CMD} -XshowSettings:all"
CMD="${CMD} -XX:+PrintCommandLineFlags"
CMD="${CMD} -XX:+PrintFlagsFinal"
CMD="${CMD} -Djava.net.preferIPv4Stack=true"
CMD="${CMD} @/args/jvm-args.txt"

# The health check command line
HEALTH_CMD=""
HEALTH_CMD="${HEALTH_CMD} -cp ${CLASSPATH}"
HEALTH_CMD="${HEALTH_CMD} com.tangosol.util.HealthCheckClient"
HEALTH_CMD="${HEALTH_CMD} http://127.0.0.1:${PORT_HEALTH}/ready"

# Build the environment variable options
ENV_VARS=""
ENV_VARS="${ENV_VARS} -e COHERENCE_WKA=localhost"
ENV_VARS="${ENV_VARS} -e COHERENCE_EXTEND_PORT=${PORT_EXTEND}"
ENV_VARS="${ENV_VARS} -e COHERENCE_CONCURRENT_EXTEND_PORT=${PORT_CONCURRENT_EXTEND}"
ENV_VARS="${ENV_VARS} -e COHERENCE_GRPC_ENABLED=true"
ENV_VARS="${ENV_VARS} -e COHERENCE_GRPC_SERVER_PORT=${PORT_GRPC}"
ENV_VARS="${ENV_VARS} -e COHERENCE_MANAGEMENT_HTTP=all"
ENV_VARS="${ENV_VARS} -e COHERENCE_MANAGEMENT_HTTP_PORT=${PORT_MANAGEMENT}"
ENV_VARS="${ENV_VARS} -e COHERENCE_METRICS_HTTP_ENABLED=true"
ENV_VARS="${ENV_VARS} -e COHERENCE_METRICS_HTTP_PORT=${PORT_METRICS}"
ENV_VARS="${ENV_VARS} -e COHERENCE_HEALTH_HTTP_PORT=${PORT_HEALTH}"
ENV_VARS="${ENV_VARS} -e COHERENCE_TTL=0"
ENV_VARS="${ENV_VARS} -e COH_MAIN_CLASS=${MAIN_CLASS}"
ENV_VARS="${ENV_VARS} -e JAEGER_SAMPLER_TYPE=const"
ENV_VARS="${ENV_VARS} -e JAEGER_SAMPLER_PARAM=0"
ENV_VARS="${ENV_VARS} -e JAEGER_SERVICE_NAME=coherence"

# Build the exposed port list
PORT_LIST=""
PORT_LIST="${PORT_LIST} -p ${PORT_EXTEND}"
PORT_LIST="${PORT_LIST} -p ${PORT_CONCURRENT_EXTEND}"
PORT_LIST="${PORT_LIST} -p ${PORT_GRPC}"
PORT_LIST="${PORT_LIST} -p ${PORT_MANAGEMENT}"
PORT_LIST="${PORT_LIST} -p ${PORT_METRICS}"
PORT_LIST="${PORT_LIST} -p ${PORT_HEALTH}"

# The image creation date
CREATED=$(date)

# Common image builder function
# param 1: the image architecture, e.g. amd64 or arm64
# param 2: the image o/s e.g. linux
# param 3: the base image
# param 4: the image name
common_image(){
  # make sure the old container is removed
  buildah rm "container-${1}" || true
  buildah rmi "coherence:${1}" || true

  # Create the container from the base image, setting the architecture and O/S
  buildah from --arch "${1}" --os "${2}" --name "container-${1}" "${3}"

  # Add the configuration, entrypoint, ports, env-vars etc...
  buildah config --healthcheck-start-period 10s --healthcheck-interval 10s --healthcheck "CMD ${ENTRY_POINT} ${HEALTH_CMD}" "container-${1}"

  buildah config --arch "${1}" --os "${2}" \
      --entrypoint "[\"${ENTRY_POINT}\"]" --cmd "${CMD} ${MAIN_CLASS}" \
      ${ENV_VARS} ${PORT_LIST} \
      --annotation "org.opencontainers.image.created=${CREATED}" \
      --annotation "org.opencontainers.image.url=https://github.com/oracle/coherence/pkgs/container/coherence-ce" \
      --annotation "org.opencontainers.image.version=${COHERENCE_VERSION}" \
      --annotation "org.opencontainers.image.source=http://github.com/oracle/coherence" \
      --annotation "org.opencontainers.image.vendor=${PROJECT_VENDOR}" \
      --annotation "org.opencontainers.image.title=${PROJECT_DESCRIPTION} ${COHERENCE_VERSION}" \
      --label "org.opencontainers.image.url=https://github.com/oracle/coherence/pkgs/container/coherence-ce" \
      --label "org.opencontainers.image.version=${COHERENCE_VERSION}" \
      --label "org.opencontainers.image.source=http://github.com/oracle/coherence" \
      --label "org.opencontainers.image.vendor=${PROJECT_VENDOR}" \
      --label "org.opencontainers.image.title=${PROJECT_DESCRIPTION} ${COHERENCE_VERSION}" \
      "container-${1}"

  # Copy files into the container
  buildah copy "container-${1}" "${BASEDIR}/target/docker" /
  buildah copy "container-${1}" "${BASEDIR}/target/*.jar"  /app/libs

  # Commit the container to an image
  buildah commit "container-${1}" "coherence:${1}"

  # Export the image to the Docker daemon unless NO_DAEMON is true
  if [ "${NO_DAEMON}" != "true" ]
  then
    buildah push -f v2s2 "coherence:${1}" "docker-daemon:${4}"
    echo "Pushed ${2}/${1} image ${4} to Docker daemon"
  fi
}

buildah version

if [ "${DOCKER_HUB_USERNAME}" != "" ] && [ "${DOCKER_HUB_PASSWORD}" != "" ]
then
  buildah login -u "${DOCKER_HUB_USERNAME}" -p "${DOCKER_HUB_PASSWORD}" "docker.io"
fi

if [ "${DOCKER_REGISTRY}" != "" ] && [ "${DOCKER_USERNAME}" != "" ] && [ "${DOCKER_PASSWORD}" != "" ]
then
  buildah login -u "${DOCKER_USERNAME}" -p "${DOCKER_PASSWORD}" "${DOCKER_REGISTRY}"
fi

if [ "${OCR_DOCKER_USERNAME}" != "" ] && [ "${OCR_DOCKER_USERNAME}" != "" ]
then
  buildah login -u "${OCR_DOCKER_USERNAME}" -p "${OCR_DOCKER_PASSWORD}" "${OCR_DOCKER_SERVER}"
fi
# pull in all the base images
buildah pull "docker-daemon:${AMD_BASE_IMAGE}" || true
buildah pull "docker-daemon:${ARM_BASE_IMAGE}" || true
buildah pull "docker-daemon:${GRAAL_AMD_BASE_IMAGE}" || true
buildah pull "docker-daemon:${GRAAL_ARM_BASE_IMAGE}" || true

# Build the amd64 image
common_image amd64 linux "${AMD_BASE_IMAGE}" "${IMAGE_NAME}-amd64"

# Build the arm64 image
common_image arm64 linux "${ARM_BASE_IMAGE}" "${IMAGE_NAME}-arm64"

# Push the relevant image to the docker daemon base on the build machine's o/s architecture
if [ "${NO_DAEMON}" != "true" ]
then
  buildah push -f v2s2 "coherence:${IMAGE_ARCH}" "docker-daemon:${IMAGE_NAME}"
  echo "Pushed linux/${IMAGE_ARCH} image ${IMAGE_NAME} to Docker daemon"
fi

# Build the amd64 Graal image
common_image amd64 linux "${GRAAL_AMD_BASE_IMAGE}" "${IMAGE_NAME}-graal-amd64"

# Build the arm64 Graal image
common_image arm64 linux "${GRAAL_ARM_BASE_IMAGE}" "${IMAGE_NAME}-graal-arm64"

# Push the relevant Graal image to the docker daemon base on the build machine's o/s architecture
if [ "${NO_DAEMON}" != "true" ]
then
  buildah push -f v2s2 "coherence:${IMAGE_ARCH}" "docker-daemon:${IMAGE_NAME}-graal"
  echo "Pushed linux/${IMAGE_ARCH} image ${IMAGE_NAME} to Docker daemon"
fi

# Clean-up
buildah rm "container-amd64" || true
buildah rmi "coherence:amd64" || true
buildah rm "container-arm64" || true
buildah rmi "coherence:arm64" || true


