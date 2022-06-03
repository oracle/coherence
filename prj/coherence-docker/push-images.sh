#!/usr/bin/env bash
#
# Copyright (c) 2000, 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
set -x

buildah version

if [ "${DOCKER_REGISTRY}" != "" ] && [ "${DOCKER_USERNAME}" != "" ] && [ "${DOCKER_PASSWORD}" != "" ]
then
  buildah login -u "${DOCKER_USERNAME}" -p "${DOCKER_PASSWORD}" "${DOCKER_REGISTRY}"
fi

if [ "${NO_DAEMON}" != "true" ]
then
  buildah pull "docker-daemon:${IMAGE_NAME}-amd64"
  buildah pull "docker-daemon:${IMAGE_NAME}-arm64"
fi

# If the registry is docker.io then cut the registry from the front of the image name
# e.g. docker.io/foo/bar:1.0 becomes foo/bar:1.0
# This is because Buildah's local name of an image cuts docker.io from the name
REGISTRY=$(echo "${IMAGE_NAME}" | cut -d"/" -f1)
if [ "${REGISTRY}" == "docker.io" ]
then
  IMAGE_SUFFIX=$(echo "${IMAGE_NAME}" | cut -d"/" -f2-10)
else
  IMAGE_SUFFIX="${IMAGE_NAME}"
fi
LOCAL_NAME="localhost/${IMAGE_SUFFIX}"

buildah images

buildah manifest create "${IMAGE_NAME}"
buildah manifest add --arch amd64 --os linux "${IMAGE_NAME}" "${IMAGE_NAME}-amd64"
buildah manifest add --arch arm64 --os linux "${IMAGE_NAME}" "${IMAGE_NAME}-arm64"
buildah manifest inspect "${IMAGE_NAME}"

buildah manifest push --all -f v2s2 "${IMAGE_NAME}" "docker://${IMAGE_NAME}"



