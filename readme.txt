            Coherence Source Depot and Build System
            =======================================

Contents
========

    * Overview
    * Prerequisites
    * Source Depot Structure
    * Build Instructions
    * Test Instructions
    * Branching Model
    * References

Overview
========

  This document describes the source depot and build system used to develop the
  Coherence product.

Prerequisites
=============

  To synchronize and build the Coherence source tree, you must first install
  a Perforce client on your development machine. The Perforce client software
  can be found in the following location:

    \\tangosol2\software\applications\perforce

  The Perforce server runs on perforce-coh.us.oracle.com:1666. Use your
  assigned user ID and password to log into the server.

  Next, create a client workspace named after your network ID and computer
  name (e.g. jhowes.jbook) that maps the //dev/main/... Perforce depot location
  to a local directory (e.g. c:\dev\main or ~/dev/main). For example, your
  client workspace might look like:

    //dev/main/... //jhowes.jbook/main/...

  Finally, open a shell in the local directory that maps to //dev/main/bin
  (for example, c:\dev\main\bin or ~/dev/main/bin) and source the appropriate
  "cfgXYZ.*" script for your O/S. This will set all environment variables
  necessary to build and test Coherence.

Source Depot Structure
======================

  All resources necessary to build Coherence are found under the
  //dev/main/... subtree:

  bin
    Scripts that set the necessary environment variables to build Coherence.

  doc
    Hand crafted documentation.

  ext
    Externally produced sources, libraries, or resources that may be included
    in the Coherence distribution.

  prj
    All Maven projects.

  tde
    All TDE-based project files and build artifacts. The tde directory is self-
    contained. That is, projects do not refer to any resources or libraries
    outside of the tde directory. The ext subdirectory is used to store
    libraries produced by the Maven build system that are necessary to compile
    TDE-based projects (e.g. tangosol.jar). The Maven build system copies
    libraries produced by TDE projects from the tde directory to the build
    directory.

  tests
    All Java source files and associated resources for functional and unit tests.

  tools
    All development tools necessary to build Coherence (e.g. Ant, Maven,
    JUnit, TDE, etc.).

  When you build Coherence and/or create a distribution of the project,
  the following directories are created under the local directory that maps to
  the source depot root:

  [project]/target
    All build artifacts, including classes, JAR files, WAR files, EAR files,
    JavaDoc, test output, etc.

  dist
    Distributions of Coherence.

Build Instructions
==================

  The Coherence build system is based upon Maven. To build Coherence, you must
  run the Maven build utility, passing in the desired goal that you would like
  to execute.

  To build Coherence go to the prj directory on your local filesystem and run the
  following command:

    mvn -s settings.xml package

  To clean all build artifacts from your build system, run the following
  command:

    mvn -s settings.xml clean

Test Instructions
=================

  See https://coherence.us.oracle.com/COHENG/Development-QA_11960746.html 

Branching Model
===============

  Mainline (//dev/main):

    The mainline is the primary development line. While newer code *may* be
    developed in special development branches (see below), the mainline is the
    ultimate destination for all new software development. Bug fixes and other
    changes made in release lines are expected to make it back into the
    mainline. Pre-releases are tracked using labels of the mainline. A label
    name must include the product, version, and build number
    (e.g. coherence-v3.0b310).

  Release Lines (//dev/release/coherence-vX.Y):

    During the course of testing, the mainline will be found to be ready for
    release. At this point, the mainline is branched into a release line named
    after the release version. In addition to the source, a distribution of the
    release is added (under the dist directory) to the new branch. Finally, a
    label of the new branch is created that includes the product, version, and
    build number (e.g coherence-v3.0b315).

    If a minor release is required (e.g. to fix a bug in a released version of
    the product), the fix is either integrated from the mainline into the
    release branch (if the fix affects both the mainline and the release
    branch) or submitted directly into the release branch (if the fix is
    isolated to the release branch). A distribution of the minor release is
    then created and added (under the dist directory) to the new branch.
    Finally, a label of the release branch is created that includes the
    product, version, and build number (e.g. coherence-v3.0.1b317).

    Occasionally, it may be necessary to create a patch of a release. In this
    scenario, the fix is either integrated from the mainline into the release
    branch (if the fix affects both the mainline and the release branch) or
    submitted directly into the release branch (if the fix is isolated to the
    release branch). A JAR file named after the issue that this patch fixes
    (e.g. coh-77.jar) is then created and added to the dist directory under a
    directory with a name that includes the version and build number
    (e.g. v2.5.1b290a).

  Project Lines (//dev/project/[project name]):

    Occasionally, mainline branches will be created for significant work that
    should be isolated from the mainline. These are known as project branches
    and they exist under //dev/project in a branch named after the project. The
    number and lifetime of project branches should be kept to a minimum, and
    project branches should be deleted as soon as the project work is
    integrated back into the mainline.

  Developer Sandboxes (//dev/sandbox/[name]):

    Each developer will have a dedicated place in the Perforce depot known as a
    sandbox that they can use as their own "personal" SCM system.

References
==========

  Perforce:

  * Getting Started:
    http://www.perforce.com/perforce/doc.042/manuals/p4win-gs/p4win-gs.pdf
    http://www.perforce.com/perforce/doc.042/manuals/boilerplates/quickstart.html

  * Best Practices:
    http://www.perforce.com/perforce/bestpractices.html
    http://www.perforce.com/perforce/life.html

  * User Guide:
    http://www.perforce.com/perforce/doc.042/manuals/p4guide/p4guide.pdf

  * Admin Guide:
    http://www.perforce.com/perforce/doc.042/manuals/p4sag/p4sag.pdf

  Ant:

  * User Guide:
    http://ant.apache.org/manual/index.html

  Maven:

  * User Guide:
    http://maven.apache.org
