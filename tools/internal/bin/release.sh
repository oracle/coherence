#!/bin/bash
#
# Copyright (c) 2020, 2026 Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
# Oracle internal script to build and perform release.
#
# This script will be run from the jenkins agent that will perform release.

update_versions() {
  NEW_VERSION=$1
  echo "Updating version to $NEW_VERSION"

  REV_UPDATE="s|<revision>\(.*\)</revision>|<revision>${NEW_VERSION}</revision>|"

  CURRENT_YEAR=`date +%Y`
  COPYRIGHT_YEARS=$(cat ${VERSION_POM} | grep "Copyright (c)" | head -n1 | sed "s|\(.*Copyright (c)\)\( .* \)\(Oracle.*\)|\2|" | xargs)
  [ -n "${DEBUG_ON}" ] && echo "File: ${VERSION_POM} | ${COPYRIGHT_YEARS}"
  if [ -z "$(echo $COPYRIGHT_YEARS | grep $CURRENT_YEAR)" ]; then
    if [ ${#COPYRIGHT_YEARS} -ge 5 ]; then
      START_YEAR=$(echo ${COPYRIGHT_YEARS} | cut -d, -f1 | xargs)
    else
      START_YEAR=$COPYRIGHT_YEARS
    fi
    COPYRIGHT_YEARS_UPDATE="s| ${COPYRIGHT_YEARS} | ${START_YEAR}, ${CURRENT_YEAR}, |"
    sed -i'' -e "${COPYRIGHT_YEARS_UPDATE}" "${VERSION_POM}"
  fi

  sed -i'' -e "${REV_UPDATE}" "${VERSION_POM}"

  # Update the revision in all of the examples pom.xml files
  ALL_EXAMPLE_POMS=$(find ${MVN_ROOT}/examples/* -name pom.xml -mindepth 1)
  for f in ${ALL_EXAMPLE_POMS}
  do
    sed -i'' -e "${REV_UPDATE}" "${f}"
    COPYRIGHT_YEARS=$(cat ${f} | grep "Copyright (c)" | head -n1 | sed "s|\(.*Copyright (c)\)\( .* \)\(Oracle.*\)|\2|" | xargs)
    [ -n "${DEBUG_ON}" ] && echo "File: ${f} | ${COPYRIGHT_YEARS}"
    if [ -z "$(echo $COPYRIGHT_YEARS | grep $CURRENT_YEAR)" ]; then
      if [ ${#COPYRIGHT_YEARS} -ge 5 ]; then
        START_YEAR=$(echo ${COPYRIGHT_YEARS} | cut -d, -f1 | xargs)
      else
        START_YEAR=$COPYRIGHT_YEARS
      fi
      COPYRIGHT_YEARS_UPDATE="s| ${COPYRIGHT_YEARS} | ${START_YEAR}, ${CURRENT_YEAR}, |"
      sed -i'' -e "${COPYRIGHT_YEARS_UPDATE}" "$f"
    fi
  done

  # Update the revision in all of the examples gradle.properties files
  ALL_EXAMPLE_PROPERTIES=$(find "${MVN_ROOT}"/examples/* -name gradle.properties -mindepth 1)
  for f in ${ALL_EXAMPLE_PROPERTIES}
  do
    sed -i'' -e "s|coherenceVersion=\(.*\)|coherenceVersion=${NEW_VERSION}|" "${f}"
    COPYRIGHT_YEARS=$(cat ${f} | grep "Copyright (c)" | head -n1 | sed "s|\(.*Copyright (c)\)\( .* \)\(Oracle.*\)|\2|" | xargs)
    [ -n "${DEBUG_ON}" ] && echo "File: ${f} | ${COPYRIGHT_YEARS}"
    if [ -z "$(echo $COPYRIGHT_YEARS | grep $CURRENT_YEAR)" ]; then
      if [ ${#COPYRIGHT_YEARS} -ge 5 ]; then
        START_YEAR=$(echo ${COPYRIGHT_YEARS} | cut -d, -f1 | xargs)
      else
        START_YEAR=$COPYRIGHT_YEARS
      fi
      COPYRIGHT_YEARS_UPDATE="s| ${COPYRIGHT_YEARS} | ${START_YEAR}, ${CURRENT_YEAR}, |"
      sed -i'' -e "${COPYRIGHT_YEARS_UPDATE}" "$f"
    fi
  done
}

update_changelist() {
  CL_NUMBER=$1
  # add all pom files to the CL and verify something was added
  p4 edit -c "$CL_NUMBER" ${VERSION_POM} &>/dev/null

  echo "ALL_EXAMPLE_POMS : ${ALL_EXAMPLE_POMS}"
  pom_count=1
  for f in ${ALL_EXAMPLE_POMS}
  do
    echo "+++ p4 edit POM file : $f"
    p4 edit -c "$CL_NUMBER" "${f}" &>/dev/null
    pom_count=$((pom_count+1))
    echo "+++ pom_count : $pom_count"
  done

  echo "---"
  echo "ALL_EXAMPLE_PROPERTIES : ${ALL_EXAMPLE_PROPERTIES}"
  prop_count=0
  for f in ${ALL_EXAMPLE_PROPERTIES}
  do
    echo "+++ p4 edit prop file : $f"
    p4 edit -c "$CL_NUMBER" "${f}" &>/dev/null
    prop_count=$((prop_count+1))
    echo "+++ prop_count : $prop_count"
  done

  echo "--- p4 describe -s $CL_NUMBER ---"
  p4 describe -s $CL_NUMBER
  echo "---------------------------------"

  POMS_COUNT=$(p4 describe $CL_NUMBER | grep pom.xml | wc -l | tr -d ' ')
  if [ $POMS_COUNT != $pom_count ]; then
    echo "Problem adding poms to  - expected $pom_count pom.xml files but only added $POMS_COUNT"
    return 1
  fi

  PROP_COUNT=$(p4 describe $CL_NUMBER | grep gradle.properties | wc -l | tr -d ' ')
  if [ $PROP_COUNT != $prop_count ]; then
    echo "Problem adding gradle.properties to  - expected $prop_count gradle.properties files but only added $PROP_COUNT"
    return 1
  fi
}

release() {
  ##############################################################################
  # Stage 1 : Find the release version from the 'revision' property value.     #
  ##############################################################################

  CURRENT_VERSION=`grep -E "<revision>" ${VERSION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`
  if [ -n "$MILESTONE_RELEASE_VERSION" ]; then
    VERSION_RELEASE=$MILESTONE_RELEASE_VERSION
  else
    VERSION_RELEASE=${CURRENT_VERSION%-SNAPSHOT}
  fi

  if [ -z "$VERSION_RELEASE" ]; then
    echo "Failed to find the required release version to use for performing the release. Release will be aborted."
    return 1
  fi

  echo "CURRENT_VERSION == $CURRENT_VERSION"
  echo "VERSION_RELEASE == $VERSION_RELEASE"

  if [[ -n "$MILESTONE_RELEASE_VERSION" || -z "$P4_CHANGELIST" || "now" = "$P4_CHANGELIST" ]]; then  
    update_versions ${VERSION_RELEASE}
    if [ $? -ne 0 ]; then
      echo "[ERROR] updating version to $VERSION_RELEASE failed ... exiting"
      return 1
    fi
  fi

  if [ "false" = "$DRY_RUN" ]; then
    # create a change list for the release version
    if [[ -n "$P4_CHANGELIST" && "now" != "$P4_CHANGELIST" ]]; then
      CL_NUMBER=$P4_CHANGELIST
    else
      CL_NUMBER=$(echo -e "Change: new\nDescription: release: update poms to version $VERSION_RELEASE\nFiles: " | p4 change -i | sed 's/Change \([0-9].*\) created./\1/')
      update_changelist ${CL_NUMBER}
      if [ $? -ne 0 ]; then
        echo "[ERROR] updating CL failed ... exiting"
        return 1
      fi
    fi

    if [[ "true" = "$DO_PUSH" && -z "$MILESTONE_RELEASE_VERSION" ]]; then
      CL_SUBMITTED=$(p4 submit -c $CL_NUMBER | grep "Change .* submitted" | sed 's/Change \([0-9]*\) submitted./\1/')
      echo "SUCCESS: POM revision updated with release version $VERSION_RELEASE and submitted $CL_SUBMITTED."
    fi
  else
    if [[ -n "$P4_CHANGELIST" && "now" != "$P4_CHANGELIST" ]]; then
      CL_NUMBER=$P4_CHANGELIST
    else
      CL_NUMBER=$(p4 changes -m1 ... | awk '{print $2}')
    fi
    echo "DRY_RUN set to 'true'... skipping remote operation of release version update ..."
  fi

  ##############################################################################
  # Stage 2 Build and Deploy the artifacts.                                    #
  ##############################################################################

  LOG_RELEASE=$(mktemp /tmp/coh-release-XXXXXX)

  echo "Build CL_NUMBER: ${CL_NUMBER}"
  MVN_CMD_OPTS="-B -e -f $MVN_ROOT/pom.xml $MVN_SETTINGS_ARGS -Dgpg.passphrase=$GPG_PASSPHRASE -DskipTests=true -Dgradle.skip.test=true -Dproject.official=true -Dproject.build.number=${CL_SUBMITTED:-$CL_NUMBER}"

  if [[ "false" = "$DRY_RUN" || "true" = "$DO_PUSH" ]]; then
    # deploy to ossrh
    echo "[INFO] Executing mvn $MVN_CMD_OPTS -Pcoherence,-modules,${RELEASE_PROFILES} clean deploy | tee $LOG_RELEASE"
    #mvn $MVN_CMD_OPTS -Pcoherence,-modules,${RELEASE_PROFILES} clean deploy | tee $LOG_RELEASE
    mvn $MVN_CMD_OPTS -Pcoherence,-modules,${RELEASE_PROFILES} clean install | tee $LOG_RELEASE
    STATUS=$?

    BUILD_RESULT=$(tail -n200 $LOG_RELEASE | grep -E "BUILD (SUCCESS|FAILURE|ERROR)" | sed 's/.*BUILD \(.*\)/\1/')
    if [[ $STATUS != 0 || "SUCCESS" != "$BUILD_RESULT" ]]; then
      echo "BUILD_STATUS = $STATUS, Result of maven deploy: $BUILD_RESULT; see $LOG_RELEASE"
      return 1
    fi

    echo "" > $LOG_RELEASE
    echo "[INFO] Executing mvn $MVN_CMD_OPTS -P-coherence,modules,${RELEASE_PROFILES} -nsu clean deploy | tee $LOG_RELEASE"
    #mvn $MVN_CMD_OPTS -P-coherence,modules,${RELEASE_PROFILES} -nsu clean deploy | tee $LOG_RELEASE
    mvn $MVN_CMD_OPTS -P-coherence,modules,${RELEASE_PROFILES} -nsu clean install | tee $LOG_RELEASE
    STATUS=$?

    BUILD_RESULT=$(tail -n200 $LOG_RELEASE | grep -E "BUILD (SUCCESS|FAILURE|ERROR)" | sed 's/.*BUILD \(.*\)/\1/')
    if [[ $STATUS != 0 || "SUCCESS" != "$BUILD_RESULT" ]]; then
      echo "BUILD_STATUS = $STATUS, Result of maven deploy: $BUILD_RESULT; see $LOG_RELEASE"
      return 1
    fi
  else
    echo "DRY_RUN set to 'true'... skipping remote operation of deploying artifacts ..."
    # mvn clean deploy -DaltDeploymentRepository=local::default::file:/tmp/test-repo
    # mvn clean deploy -DaltDeploymentRepository=local::default::file:/tmp/test-repo -Pmodules,-coherence -nsu -DskipTests
    mvn $MVN_CMD_OPTS -DaltDeploymentRepository=local::default::file:/tmp/test-repo clean deploy | tee $LOG_RELEASE
    STATUS=$?

    BUILD_RESULT=$(tail -n200 $LOG_RELEASE | grep -E "BUILD (SUCCESS|FAILURE|ERROR)" | sed 's/.*BUILD \(.*\)/\1/')
    if [[ $STATUS != 0 || "SUCCESS" != "$BUILD_RESULT" ]]; then
      echo "BUILD_STATUS = $STATUS, Result of maven deploy: $BUILD_RESULT; see $LOG_RELEASE"
      return 1
    fi

    echo "" > $LOG_RELEASE
    echo "[INFO] Executing mvn $MVN_CMD_OPTS -P-coherence,modules -nsu clean deploy | tee $LOG_RELEASE"
    mvn $MVN_CMD_OPTS -P-coherence,modules -nsu -DaltDeploymentRepository=local::default::file:/tmp/test-repo clean deploy | tee $LOG_RELEASE
    STATUS=$?

    BUILD_RESULT=$(tail -n200 $LOG_RELEASE | grep -E "BUILD (SUCCESS|FAILURE|ERROR)" | sed 's/.*BUILD \(.*\)/\1/')
    if [[ $STATUS != 0 || "SUCCESS" != "$BUILD_RESULT" ]]; then
      echo "BUILD_STATUS = $STATUS, Result of maven deploy: $BUILD_RESULT; see $LOG_RELEASE"
      return 1
    fi
  fi
}

archive_release_artifacts() {
  CURRENT_VERSION=`grep -E "<revision>" ${VERSION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`
  if [ -n "$MILESTONE_RELEASE_VERSION" ]; then
    VERSION_RELEASE=$MILESTONE_RELEASE_VERSION
  else
    VERSION_RELEASE=${CURRENT_VERSION%-SNAPSHOT}
  fi

  if [ -z "$VERSION_RELEASE" ]; then
    echo "Failed to find the required release version to use for performing the release. Release will be aborted."
    return 1
  fi

  echo "CURRENT_VERSION == $CURRENT_VERSION"
  echo "VERSION_RELEASE == $VERSION_RELEASE"

  pushd $HOME/.m2/repository
  ALL_RELEASE_ARTIFACTS=$(find . -type f | cut -c3- | grep -E "com/oracle/coherence/ce/.*/${VERSION_RELEASE}" | grep -v SNAPSHOT | grep -v tests | grep -v coherence-javadoc | grep -v main | grep -v liberte | grep -v coherence-core | grep -v docs)
  for ARTIFACT in ${ALL_RELEASE_ARTIFACTS}; do
    echo $ARTIFACT
  done

  popd

  rm -fr $WORKSPACE/.deploy
  mkdir $WORKSPACE/.deploy
  ls $WORKSPACE/.deploy
  DEPLOY_RELEASE_ARTIFACTS_ARCHIVE=$WORKSPACE/.deploy/coherence-ce-${VERSION_RELEASE}-release.tar

  tar -cvf $DEPLOY_RELEASE_ARTIFACTS_ARCHIVE -C $HOME/.m2/repository $ALL_RELEASE_ARTIFACTS
  tar -tvf $DEPLOY_RELEASE_ARTIFACTS_ARCHIVE
}

extract_release_artifacts() {
  CURRENT_VERSION=`grep -E "<revision>" ${VERSION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`
  if [ -n "$MILESTONE_RELEASE_VERSION" ]; then
    VERSION_RELEASE=$MILESTONE_RELEASE_VERSION
  else
    VERSION_RELEASE=${CURRENT_VERSION%-SNAPSHOT}
  fi

  if [ -z "$VERSION_RELEASE" ]; then
    echo "Failed to find the required release version to use for performing the release. Release will be aborted."
    return 1
  fi

  echo "CURRENT_VERSION == $CURRENT_VERSION"
  echo "VERSION_RELEASE == $VERSION_RELEASE"

  DEPLOY_RELEASE_ARTIFACTS_ARCHIVE=${2:-$WORKSPACE/.deploy/coherence-ce-${VERSION_RELEASE}-release.tar}
  if [ ! -e ${DEPLOY_RELEASE_ARTIFACTS_ARCHIVE} ]; then
    echo "[ERROR] Required input archive containing deployment artifacts for the release $VERSION_RELEASE does not exist ... Exiting ..."
    return 1
  fi

  mkdir -p ${1:-$WORKSPACE/dist/publish}
  tar -xvf $DEPLOY_RELEASE_ARTIFACTS_ARCHIVE -C ${1:-$WORKSPACE/dist/publish}

  if [[ -n "$3" && "deploy" = "$3" ]]; then
    MVN_DEPLOY_OPTS=${MVN_CMD_OPTS:--nsu -Drevision=$VERSION_RELEASE}
    echo "MVN_DEPLOY_OPTS == $MVN_DEPLOY_OPTS"
    mvn -f $MVN_ROOT $MVN_DEPLOY_OPTS -Pcoherence,modules -DaltDeploymentRepository=${MVN_DEPLOY_REPO:-local::default::file:/tmp/test-deploy-repo} jar:jar deploy:deploy
  fi
}

upload_zip_artifacts() {
  if [ -f ${VERSION_POM} ]; then
    CURRENT_VERSION=`grep -E "<revision>" ${VERSION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`
  else
    CURRENT_VERSION=`grep -E "<revision>" $(find ${MVN_ROOT}/coherence-bom/* -name pom.xml -mindepth 1) | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`
  fi

  RELEASE_VERSION=${CURRENT_VERSION%-SNAPSHOT}

  # create temp output dir.
  OUTDIR=$(mktemp -d /tmp/$JOB_NAME-$BUILD_NUMBER-XXXXXX)
  echo "OUTDIR = $OUTDIR"
  wget -q "https://github.com/stedolan/jq/releases/download/jq-1.7.1/jq-linux64" -O $OUTDIR/jq
  chmod +x $OUTDIR/jq
  jq --version

  curl -s -L -H 'Accept: application/vnd.github.v3+json' -H 'Content-Type: application/json' -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/repos/$REPO_NAME/releases?per_page=4 > $OUTDIR/release.json
  RELEASE_NAMES=`jq -r .[].name $OUTDIR/release.json`
  echo "RELEASE_NAMES = $RELEASE_NAMES"

  if [[ $? -eq 0 && -n "$(echo $RELEASE_NAMES | grep $RELEASE_VERSION)" ]]; then
    IFS=$'\n' read -a RELEASES <<< "$RELEASE_NAMES"
    for release in "$RELEASES"
    do
      if [ -n "$(echo $release | grep $RELEASE_VERSION)" ]; then 
        echo "RELEASE_NAME = $release"
        RELEASE_ID=`jq -r --arg release "$release" '.[] | select(.name==$release) | .id' $OUTDIR/release.json`
      fi
    done
  fi
  echo "RELEASE_ID = $RELEASE_ID"

  if [[ "false" = "$DRY_RUN" && -z "$RELEASE_ID" ]]; then
    curl -s -L -H 'Accept: application/vnd.github.v3+json' -H 'Content-Type: application/json' -H "Authorization: token $GITHUB_TOKEN" -X POST https://api.github.com/repos/$REPO_NAME/releases -d "{\"name\":\" Coherence CE v${RELEASE_VERSION}\",\"tag_name\":\"v$RELEASE_VERSION\",\"draft\":true}" > $OUTDIR/release.json
  elif [ -n "$RELEASE_ID" ]; then
    curl -s -L -H 'Accept: application/vnd.github.v3+json' -H 'Content-Type: application/json' -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/repos/$REPO_NAME/releases/$RELEASE_ID > $OUTDIR/release.json
  else
    echo "[INFO] Required Release for version $RELEASE_VERSION not found on Github"
    return 1
  fi

  cat $OUTDIR/release.json
  UPLOAD_URL_PART=`cat $OUTDIR/release.json | grep upload_url | tr -d '"' | awk '{print $2}' | cut -d{ -f1`
  echo "UPLOAD_URL_PART == $UPLOAD_URL_PART"

  if [ -n "$UPLOAD_URL_PART" ]; then
    UPLOAD_ARTIFACT=$ARCHIVES_DIR/docs.zip
    ARTIFACT_NAME=$(basename $f)
    UPLOAD_URL="${UPLOAD_URL_PART}?name=$ARTIFACT_NAME"
    echo "UPLOAD_URL = $UPLOAD_URL"

    if [ "false" = "$DRY_RUN" ]; then
      UPLOAD_STATUS=`curl -s -w %{http_code} --output /dev/null -L -H 'Accept: application/vnd.github.v3+json' -H 'Content-Type: application/zip' \
                       -H "Authorization: token $GITHUB_TOKEN" -X POST $UPLOAD_URL --upload-file ${UPLOAD_ARTIFACT}`
      echo "UPLOAD_STATUS = $UPLOAD_STATUS"
      sleep 10
      if [[ $UPLOAD_STATUS -ge 200 && $UPLOAD_STATUS -lt 300 ]]; then
        echo "[INFO] Successfully deployed zip: ${UPLOAD_ARTIFACT}"
      fi
    else
      echo "[INFO] DRY_RUN set to true ... skipping upload ..."
    fi
  fi
}

release_image() {
  if [ -n "$MVN_SETTINGS" ]; then
    MVN_SETTINGS_ARGS="-s $MVN_SETTINGS"
  else
    MVN_SETTINGS_ARGS=""
  fi

  docker rmi -f $DOCKER_REGISTRY/coherence-ce:$1 || true
  if [[ "false" = "$DRY_RUN" && "true" = "$DO_PUSH" ]]; then
    if [[ -z "$DOCKER_USERNAME" || -z "$DOCKER_PASSWORD" ]]; then
      echo "[ERROR] Required DOCKER_USERNAME or DOCKER_PASSWORD is not set for the given docker registry."
      return 1
    fi
    if [ -n "$DOCKER_REGISTRY" ]; then
      docker login $DOCKER_REGISTRY -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
    else
      docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
    fi
    mvn -B -e -f $MVN_ROOT/pom.xml $MVN_SETTINGS_ARGS -Dproject.official=true -Dproject.build.number=${CL_SUBMITTED:-$CL_NUMBER} -DskipTests -Drevision=$1 -Ddocker.skip.tests=true -Ddocker.registry=$DOCKER_REGISTRY -Pdocker,docker-push -am -pl coherence-docker clean package
  else
    mvn -B -e -f $MVN_ROOT/pom.xml $MVN_SETTINGS_ARGS -Dproject.official=true -Dproject.build.number=${CL_SUBMITTED:-$CL_NUMBER} -DskipTests=true -Drevision=$1 -Ddocker.skip.tests=true -Ddocker.registry=$DOCKER_REGISTRY -Pdocker -am -pl coherence-docker clean package
  fi
}

git_tag() {
  if [ -n "$RELEASE_VERSION" ]; then
    GIT_BRANCH=v$RELEASE_VERSION
  else
    if [ -z "`echo $RELEASE_ROOT | grep "release/coherence"`" ]; then
      if [ -z "$MILESTONE_RELEASE_VERSION" ]; then
        GIT_BRANCH=v${RELEASE_ROOT#*\/v}
      else
        GIT_BRANCH=main
      fi
    else
      GIT_BRANCH=v${RELEASE_ROOT#*-v}
    fi
  fi

  echo "GIT BRANCH to use : $GIT_BRANCH"

  if [ -e $WORKSPACE/github-coherence-$GIT_BRANCH ]; then
    rm -fr $WORKSPACE/github-coherence-$GIT_BRANCH
    ls $WORKSPACE
  fi

  for i in {1..5}; do git clone --depth ${1:-10} --single-branch -b $GIT_BRANCH $GIT_REPO $WORKSPACE/github-coherence-$GIT_BRANCH \
    && exitCode=0 && break || exitCode=$? \
    && echo "The command 'git clone' failed. Attempt ${i} of 5" \
    && sleep 10; done;

  if [ ${exitCode} != 0 ]; then
    exit 1
  fi

  cd $WORKSPACE/github-coherence-$GIT_BRANCH

  git status
  git log -n 2

  if [ -n "$MILESTONE_RELEASE_VERSION" ]; then
    VERSION_RELEASE=$MILESTONE_RELEASE_VERSION
  else
    RELEASE_COMMIT=$(git log -n 2 --author="Coherence Release User" --reverse -i --pretty=format:%H | head -1)
    echo "RELEASE_COMMIT = $RELEASE_COMMIT"
    if [ -n "$RELEASE_COMMIT" ]; then
      git checkout $RELEASE_COMMIT
      VERSION_RELEASE=`grep -E "<revision>" ${VERSION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`
    fi
  fi

  echo "+++ Tagging the repository for release: $VERSION_RELEASE +++"

  if [[ -z `git tag -l | grep "^$VERSION_RELEASE$"` && -z `echo $VERSION_RELEASE | grep SNAPSHOT` ]]; then
    git tag $VERSION_RELEASE
    echo "+++ Tag description after running git tag command. +++"
    git describe --all $VERSION_RELEASE

    if [ "true" = "$DO_PUSH" ]; then
      echo "DO_PUSH set to : $DO_PUSH"
      git ls-remote --tags

      git push origin $VERSION_RELEASE
      GIT_TAG=`git ls-remote --tags | grep $VERSION_RELEASE`
      echo "GIT_TAG == $GIT_TAG"
      if [ -z "$GIT_TAG" ]; then
        echo "Failed to push the tag for release : $VERSION_RELEASE"
        return 1
      fi
    else
      echo "DO_PUSH set to : $DO_PUSH. Skipping remote operation of pushing tag."
    fi
  else
    echo "TAG: $VERSION_RELEASE already exist or not a valid tag. Nothing to do."
  fi
}

# Derive next snapshot version by parsing the current version in use.
next_version() {
  CURRENT_VERSION=`grep -E "<revision>" ${VERSION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`
  REVISION=${CURRENT_VERSION%-SNAPSHOT}
  VERSION_PARTS_DASH=(${REVISION//-/ })
  VERSION_PARTS_LENGTH=${#VERSION_PARTS_DASH[@]}
  if [ $VERSION_PARTS_LENGTH -eq 3 ]; then
    PATCH=$(( ${VERSION_PARTS_DASH[2]} + 1 ))
    VERSION_NEXT="${VERSION_PARTS_DASH[0]}-${VERSION_PARTS_DASH[1]}-$PATCH-SNAPSHOT"
  else
    VERSION_PARTS_DOTS=(${REVISION//./ })
    if [ ${#VERSION_PARTS_DOTS[@]} -eq 2 ]; then
      VERSION_NEXT="${VERSION_PARTS_DOTS[0]}.${VERSION_PARTS_DOTS[1]}.1-SNAPSHOT"
    else
      PATCH=$(( ${VERSION_PARTS_DOTS[2]} + 1 ))
      VERSION_NEXT="${VERSION_PARTS_DOTS[0]}.${VERSION_PARTS_DOTS[1]}.$PATCH-SNAPSHOT"
    fi
  fi
  echo $VERSION_NEXT
}

build_push_next_revision() {
  if [ -z "$VERSION_NEXT" ]; then
    VERSION_NEXT=`next_version`
  fi

  echo "++++++ Updating the revision with the next version $VERSION_NEXT. +++++"
  LOG_NEXT_VERSION_SET=$(mktemp /tmp/coh-versions-set-XXXXXX)
  update_versions $VERSION_NEXT

  if [ -n "$MVN_SETTINGS" ]; then
    MVN_SETTINGS_ARGS="-s $MVN_SETTINGS"
  else
    MVN_SETTINGS_ARGS=""
  fi

  # Doing sanity check to verify project is built successfully with next revision before p4 submit.
  mvn -B -e -f $MVN_ROOT/pom.xml $MVN_SETTINGS_ARGS -DskipTests=true clean verify | tee $LOG_NEXT_VERSION_SET
  STATUS=$?

  BUILD_RESULT=$(tail -n200 $LOG_NEXT_VERSION_SET | grep -E "BUILD (SUCCESS|FAILURE|ERROR)" | sed 's/.*BUILD \(.*\)/\1/')
  echo "Result of maven build with next revision: $BUILD_RESULT"

  if [[ $STATUS != 0 || "SUCCESS" != "$BUILD_RESULT" ]]; then
    echo "Maven build failed with next version: $VERSION_NEXT"
    return 1
  fi

  if [[ "false" = "$DRY_RUN" && "true" = "$DO_PUSH" && -z "$MILESTONE_RELEASE_VERSION" ]]; then
    # create a change list for the next version
    CL_NUMBER_NEXT=$(echo -e "Change: new\nDescription: release: update poms to version $VERSION_NEXT\nFiles: " | p4 change -i | sed 's/Change \([0-9].*\) created./\1/')
    echo "++++++ CL_NUMBER_NEXT == $CL_NUMBER_NEXT ++++++"

    update_changelist $CL_NUMBER_NEXT

    echo "++++++++ Output of P4 diff ++++++++++++"
    p4 diff
    echo "+++++++++++++++++++++++++++++++++++++++"

    CL_VERSION_NEXT_SUBMITTED=$(p4 submit -c $CL_NUMBER_NEXT | grep "Change .* submitted" | sed 's/Change \([0-9]*\) submitted./\1/')
    echo "SUCCESS: release $RELEASE_VERSION performed and submitted $CL_VERSION_NEXT_SUBMITTED with next revision $VERSION_NEXT"
  else
    echo "DRY_RUN set to 'true' OR this is milestone release... skipping remote operation of pushing next version update ..."
  fi
}

git_clone_branch_ghpages() {
  local GITHUB_CLONE_DIR=$WORKSPACE/github-ghpages
  rm -fr $GITHUB_CLONE_DIR
  mkdir -p $GITHUB_CLONE_DIR
  cd $GITHUB_CLONE_DIR

  for i in {1..5}; do git clone --depth ${1:-1} --single-branch -b gh-pages $GIT_REPO . \
    && exitCode=0 && break || exitCode=$? \
    && echo "The command 'git clone' failed. Attempt ${i} of 5" \
    && sleep 10; done;

  if [ ${exitCode} != 0 ]; then
    exit 1
  fi

  git pull
  git status
}

create_docs_zip() {
  REVISION=`grep -E "<revision>" ${VERSION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`

  # Clean output dirs first if they exist
  if [ -d "$WORKSPACE/$REVISION" ]; then
    echo "$WORKSPACE/$REVISION dir exist ... cleaning dir ..."
    rm -fr $WORKSPACE/$REVISION
  fi

  # create output dirs for storing artifacts for the publishing
  mkdir -p $WORKSPACE/$REVISION/docs/api

  # Build api docs
  mvn -B -e -f $MVN_ROOT/pom.xml $MVN_SETTINGS_ARGS -Dproject.official=true -DskipTests=true -P-coherence,modules,javadoc -nsu -am -pl coherence-javadoc clean install
  cp -r $MVN_ROOT/coherence-javadoc/target/javadoc/apidocs/* $WORKSPACE/$REVISION/docs/api/

  # Build docs
  mvn -B -e -f $MVN_ROOT/pom.xml $MVN_SETTINGS_ARGS -P docs -pl docs clean install
  if [ $? -eq 0 ]; then
    echo "[INFO] Maven sitegen: SUCCESS"
    IS_SITE_SUCCESS=true
    cp -r $MVN_ROOT/docs/target/docs/* $WORKSPACE/$REVISION/docs/
  else
    echo "[INFO] Maven sitegen: FAILED"
    IS_SITE_SUCCESS=false
  fi

  rm -fr $ARCHIVES_DIR ; mkdir $ARCHIVES_DIR
  echo "Creating docs.zip for the release : $ARCHIVES_DIR/docs.zip"
  cd $WORKSPACE/$REVISION/docs
  zip -r $ARCHIVES_DIR/docs.zip *
  ls -lh $ARCHIVES_DIR/*.zip
}

publish_api_docs() {
  REVISION=`grep -E "<revision>" ${VERSION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`

  git_clone_branch_ghpages
  echo "[INFO] In DIR: `pwd`"
  echo "Performing git commit of new release version api and docs ..."

  if [ ! -d $WORKSPACE/$REVISION ]; then
    if [ -f $ARCHIVES_DIR/docs.zip ]; then
      mkdir -p $REVISION/docs
      unzip $ARCHIVES_DIR/docs.zip -d $REVISION/docs
    else
      echo "[ERROR] Required output dir: $WORKSPACE/$REVISION or docs.zip containing generated docs could not be found ... Exiting ..."
      return 1
    fi
  else
    mv $WORKSPACE/$REVISION .
  fi

  rm -fr $REVISION/$API_JAVA
  mkdir -p $REVISION/$API_JAVA
  mv $REVISION/docs/api/* $REVISION/$API_JAVA/
  rm -fr $REVISION/docs/api

  sleep 10
  git add $REVISION/*
  git commit -m "release: adding api and sitegen docs for version $REVISION"

  git status
  cat .git/config
  sleep 5

  if [[ "false" = "$DRY_RUN" && "true" = "$DO_PUSH" ]]; then
    echo "git user == $(git config --global user.name)"
    echo "Pushing api and docs changes ..."
    git push origin gh-pages
  fi
}

update_latest_doclink() {
  REVISION=`grep -E "<revision>" $VERSION_POM | sed 's/.*<revision>\(.*\)<\/revision>/\1/'`
  echo "CURRENT REVISION = $REVISION"

  git_clone_branch_ghpages

  if [ ! -e latest ]; then
    mkdir latest
  fi

  if [ -e $REVISION ]; then
    cd latest
    rm -fr $RELEASE_VERSION
    ls -ls $RELEASE_VERSION
    ln -s ../$REVISION $RELEASE_VERSION
    ls -ls $RELEASE_VERSION
    git add .
    cd ..
    git commit -a -m "release: updating latest site link for version $RELEASE_VERSION"
    git status

    if [[ "false" = "$DRY_RUN" && "true" = "$DO_PUSH" ]]; then
      echo "git user == $(git config --global user.name)"
      echo "Pushing latest $RELEASE_VERSION doc link update..."
      git push origin gh-pages
    fi
  else
    echo "Required dir $REVISION containing the docs does not exist."
    return 1
  fi
}

displayopts() {
  set -o posix ; set ; set +o posix
}

DRY_RUN=${DRY_RUN:-true}
DO_PUSH=${DO_PUSH:-false}
DO_TAG=${DO_TAG:-false}
SET_PROXY=${SET_PROXY:-true}
RELEASE_PROFILES="release,ossrh,gradleProxy,-default,-examples${MVN_PROFILES:-}"

if [[ "false" = "$DRY_RUN" && -z "$GITHUB_TOKEN" ]]; then
  echo "Required GITHUB_TOKEN value for coherence-bot user is missing. Exiting ..."
  exit 1
fi

REPO_NAME=${REPO_NAME_TO_USE:-oracle/coherence}
GIT_REPO=https://coherence-bot:${GITHUB_TOKEN}@github.com/${REPO_NAME}
RAR="/usr/local/packages/aime/ias/run_as_root"
API_JAVA="api/java"
ARCHIVES_DIR="${WORKSPACE}/.archives"

if [ "true" = "$SET_PROXY" ]; then
  no_proxy=localhost,127.0.0.1,*.oracle.com,.us.oracle.com,.oraclecorp.com,/var/run/docker.sock
  http_proxy=http://www-proxy-hqdc.us.oracle.com:80
  https_proxy=$http_proxy
  HTTPS_PROXY=$http_proxy
  NO_PROXY=$no_proxy
  HTTP_PROXY=$http_proxy
  export no_proxy http_proxy https_proxy HTTPS_PROXY NO_PROXY HTTP_PROXY
fi

if [ -n "$P4_PORT" ]; then
  P4USER=$P4_USER
  P4CLIENT=$P4_CLIENT
  P4TICKET=$P4_TICKET
  P4PORT=$P4_PORT
  export P4USER P4CLIENT P4TICKET P4PORT MVN_ROOT
fi

if [ -n "$RELEASE_VERSION" ]; then
  RELEASE_ROOT=$WORKSPACE/v$RELEASE_VERSION
else
  RELEASE_ROOT=$WORKSPACE
fi
MVN_ROOT=$RELEASE_ROOT/prj

# set the pom.xml file that has the <revision> property with the actual version
VERSION_POM=${MVN_ROOT}/coherence-bom/pom.xml

if [ -n "$MVN_SETTINGS" ]; then
  MVN_SETTINGS_ARGS="-s $MVN_SETTINGS -Dsettings.security=$MVN_SEC_SETTINGS"
else
  MVN_SETTINGS_ARGS=""
fi

## check for mvn in PATH
MVN_PATH=$(which mvn)
if [ $? != 0 ]; then
  export PATH="$RELEASE_ROOT/tools/maven/bin:$PATH"
fi

## check for p4 in PATH
P4_PATH=$(which p4)
if [ $? != 0 ]; then
  export PATH=$WORKSPACE/../../tools/hudson.plugins.perforce.PerforceToolInstallation/any_platform:$PATH
fi

echo "-------------- ENV - VAR - STARTS ---------"
env | sort
echo "-------------- ENV - VAR - ENDS -----------"

# Invoke individual function pass as first argument to the script.
if [ -n "$1" ]; then
  $1 ${@:2}
fi
