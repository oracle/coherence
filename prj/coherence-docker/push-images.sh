#!/usr/bin/env bash
#
# Copyright (c) 2000, 2023, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
set -x -e

buildah version

if [ "${DOCKER_REGISTRY}" != "" ] && [ "${DOCKER_USERNAME}" != "" ] && [ "${DOCKER_PASSWORD}" != "" ]
then
  buildah login -u "${DOCKER_USERNAME}" -p "${DOCKER_PASSWORD}" "${DOCKER_REGISTRY}"
fi

if [ "${NO_DAEMON}" != "true" ]
then
  buildah pull "docker-daemon:${IMAGE_NAME}-amd64"
  buildah pull "docker-daemon:${IMAGE_NAME}-arm64"
  if [ "${NO_GRAAL}" != "true" ]
  then
    buildah pull "docker-daemon:${IMAGE_NAME}-graal-amd64"
    buildah pull "docker-daemon:${IMAGE_NAME}-graal-arm64"
  fi
fi

buildah images

buildah rmi "${IMAGE_NAME}" || true
buildah manifest rm "${IMAGE_NAME}" || true
buildah manifest create "${IMAGE_NAME}"
buildah manifest add --arch amd64 --os linux "${IMAGE_NAME}" "${IMAGE_NAME}-amd64"
buildah manifest add --arch arm64 --os linux "${IMAGE_NAME}" "${IMAGE_NAME}-arm64"
buildah manifest inspect "${IMAGE_NAME}"

buildah manifest push --all -f v2s2 "${IMAGE_NAME}" "docker://${IMAGE_NAME}"

if [ "${NO_GRAAL}" != "true" ]
then
  # Create Graal based images
  buildah manifest rm "${IMAGE_NAME}-graal" || true
  buildah manifest create "${IMAGE_NAME}-graal"
  buildah manifest add --arch amd64 --os linux "${IMAGE_NAME}-graal" "${IMAGE_NAME}-graal-amd64"
  buildah manifest add --arch arm64 --os linux "${IMAGE_NAME}-graal" "${IMAGE_NAME}-graal-arm64"
  buildah manifest inspect "${IMAGE_NAME}-graal"

  buildah manifest push --all -f v2s2 "${IMAGE_NAME}-graal" "docker://${IMAGE_NAME}-graal"
fi

# prune all old images that are no longer associated to a tag
buildah rmi --prune || true



