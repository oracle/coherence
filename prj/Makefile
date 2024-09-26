# ----------------------------------------------------------------------------------------------------------------------
# Copyright (c) 2022, 2024, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
# ----------------------------------------------------------------------------------------------------------------------
# This is the Makefile to build the Coherence.
# Although Coherence is built using Maven, there are sone nuances to the commands to properly do the required
# two stage build. This Makefile makes remember the commands a little simpler.
#
# ----------------------------------------------------------------------------------------------------------------------

ARGS            ?=
CURRENT_VERSION ?= $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout -nsu)
OS_NAME         ?= $(shell mvn help:evaluate -Dexpression=os.name -q -DforceStdout -nsu)
OS_ARCH         ?= $(shell mvn help:evaluate -Dexpression=os.arch -q -DforceStdout -nsu)

# ======================================================================================================================
# Makefile targets start here
# ======================================================================================================================

# ----------------------------------------------------------------------------------------------------------------------
# Display the Makefile help - this is a list of the targets with a description.
# This target MUST be the first target in the Makefile so that it is run when running make with no arguments
# ----------------------------------------------------------------------------------------------------------------------
help: ## Display this help.
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-25s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)


# ======================================================================================================================
# Build targets
# ======================================================================================================================
##@ Build

.PHONY: all
all: coherence modules ## A full build of Coherence with all modules

.PHONY: coherence
coherence: ## Build coherence.jar
	$(call mvn_build_coherence)

.PHONY: modules
modules: ## Build Coherence modules
	$(call mvn_build_modules)

.PHONY: docker
docker: ## Build the Coherence image (does not rebuild Coherence jars)
	mvn clean install -Pmodules,-coherence,docker -pl coherence-docker/ -nsu $(ARGS)

.PHONY: buildah
buildah: ## Run Buildah (does not rebuild Coherence jars)
	mvn clean install -Pmodules,-coherence,buildah -pl coherence-docker/ -nsu $(ARGS)

.PHONY: docker-no-graal
docker-no-graal: ## Build the Coherence images without including Graal (does not rebuild Coherence jars)
	mvn clean install -Pmodules,-coherence,docker -pl coherence-docker/ -nsu -Dgraal.image.skip=true $(ARGS)

.PHONY: docker-test
docker-test: ## Build the Coherence image (does not rebuild Coherence image)
	mvn verify -Pmodules,-coherence,docker-test -pl coherence-docker/ -nsu $(ARGS)

.PHONY: docker-push
docker-push: ## Push the Coherence image (does not rebuild Coherence image)
	mvn -DskipTests install -Pmodules,-coherence,docker-push -pl coherence-docker -nsu $(ARGS)

.PHONY: docker-push-no-graal
docker-push-no-graal: ## Push the Coherence images without including Graal (does not rebuild Coherence image)
	mvn -DskipTests install -Pmodules,-coherence,docker-push -pl coherence-docker -nsu -Dgraal.image.skip=true $(ARGS)

.PHONY: pof
pof: ## Dump the POF config information
	mvn exec:exec -pl coherence-liberte/ -nsu -Pmodules,-coherence

TEST_MODULE ?=
TEST_CLASS ?=
.PHONY: test-security
test-security: ## Run a functional test with security manager enabled (must specify TEST_MODULE and TEST_CLASS)
	mvn clean verify -Pmodules,-coherence -nsu -Dtest.security.enabled=true -Dproject.official=true \
		-pl test/functional/$(TEST_MODULE)/ -Dit.test=$(TEST_CLASS) -am -Dtest=_  \
		-Dfailsafe.failIfNoSpecifiedTests=false -Dsurefire.failIfNoSpecifiedTests=false

define mvn_build_coherence
	mvn clean install -T 1.5C -U -Dgradle.skip.test=true -DskipTests $(1) $(ARGS)
endef

define mvn_build_modules
	mvn clean install -T 1.5C -nsu -Pmodules,-coherence -Dgradle.skip.test=true -DskipTests $(1) $(ARGS)
endef

# ======================================================================================================================
# HNSW targets
# ======================================================================================================================

HNSW_CPP_SRC   = coherence-hnsw/src/main/cpp
HNSW_HDR_FILES = $(shell find ./$(HNSW_CPP_SRC) -type f -name "*.h")
HNSW_CPP_FILES = $(shell find ./$(HNSW_CPP_SRC) -type f -name "*.cpp")
HNSW_LIB_DIR   ?= coherence-hnsw/src/main/resources

##@ HNSW
.PHONY: hnsw
hnsw: hnsw-info coherence-hnsw/src/main/resources/libhnswlib-aarch64.dylib coherence-hnsw/src/main/resources/libhnswlib-x86-64.dylib coherence-hnsw/src/main/resources/libhnswlib-aarch64.so coherence-hnsw/src/main/resources/libhnswlib-x86-64.so coherence-hnsw/src/main/resources/libhnswlib-x86-64.dll ## Build the HNSW native C++ library and JNA bindings for the current O/S
	@echo "Done"

.PHONY: hnsw-info
hnsw-info:
	@echo "Checking HNSW shared libraries: OS is \"$(OS_NAME)\" Arch is \"$(OS_ARCH)\""

.PHONY: hnsw-macos-arm
hnsw-macos-arm: coherence-hnsw/src/main/resources/libhnswlib-aarch64.dylib

coherence-hnsw/src/main/resources/libhnswlib-aarch64.dylib: $(HNSW_HDR_FILES) $(HNSW_CPP_FILES)
    ifeq ($(OS_NAME),Mac OS X)
    ifeq ($(OS_ARCH),aarch64)
		echo "Building HNSW library for MacOS Arm64: $(HNSW_LIB_DIR)/libhnswlib-aarch64.dylib"
		mkdir -p $(HNSW_LIB_DIR) || true
		g++ -O3 -shared -std=c++11 $(HNSW_CPP_SRC)/bindings.cpp -I $(HNSW_CPP_SRC)/hnswlib -o $(HNSW_LIB_DIR)/libhnswlib-aarch64.dylib
    endif
    endif

.PHONY: hnsw-macos-x86
hnsw-macos-x86: coherence-hnsw/src/main/resources/libhnswlib-x86-64.dylib

coherence-hnsw/src/main/resources/libhnswlib-x86-64.dylib: $(HNSW_HDR_FILES) $(HNSW_CPP_FILES)
    ifeq ($(OS_NAME),Mac OS X)
    ifeq ($(OS_ARCH),x86_64)
		echo "Building HNSW library for MacOS x86_64: $(HNSW_LIB_DIR)/libhnswlib-x86-64.dylib"
		mkdir -p $(HNSW_LIB_DIR) || true
		g++ -O3 -shared -std=c++11 $(HNSW_CPP_SRC)/bindings.cpp -I $(HNSW_CPP_SRC)/hnswlib -o $(HNSW_LIB_DIR)/libhnswlib-x86-64.dylib
    endif
    endif

.PHONY: hnsw-linux-aarch64
hnsw-linux-aarch64: coherence-hnsw/src/main/resources/libhnswlib-aarch64.so

coherence-hnsw/src/main/resources/libhnswlib-aarch64.so: $(HNSW_HDR_FILES) $(HNSW_CPP_FILES)
    ifeq ($(OS_NAME),Linux)
    ifeq ($(OS_ARCH),aarch64)
		echo "Building HNSW library for Linux aarch64: $(HNSW_LIB_DIR)/libhnswlib-aarch64.so"
		mkdir -p $(HNSW_LIB_DIR) || true
		g++ -O3 -fPIC -shared -std=c++11 $(HNSW_CPP_SRC)/bindings.cpp -I $(HNSW_CPP_SRC)/hnswlib -o $(HNSW_LIB_DIR)/libhnswlib-aarch64.so
    endif
    endif

.PHONY: hnsw-linux-x86
hnsw-linux-x86: coherence-hnsw/src/main/resources/libhnswlib-x86-64.so

coherence-hnsw/src/main/resources/libhnswlib-x86-64.so: $(HNSW_HDR_FILES) $(HNSW_CPP_FILES)
    ifeq ($(UNAME_S),Linux)
    ifeq ($(OS_ARCH),amd64)
		echo "Building HNSW library for Linux x86_64: $(HNSW_LIB_DIR)/libhnswlib-x86-64.so"
		mkdir -p $(HNSW_LIB_DIR) || true
		g++ -O3 -fPIC -shared -std=c++11 $(HNSW_CPP_SRC)/bindings.cpp -I $(HNSW_CPP_SRC)/hnswlib -o $(HNSW_LIB_DIR)/libhnswlib-x86-64.so
    endif
    endif

.PHONY: hnsw-windows-x86
hnsw-windows-x86: coherence-hnsw/src/main/resources/libhnswlib-x86-64.dll

coherence-hnsw/src/main/resources/libhnswlib-x86-64.dll: $(HNSW_HDR_FILES) $(HNSW_CPP_FILES)
    ifeq ($(OS),Windows_NT)
		echo "Building HNSW library for Windows x86_64: $(HNSW_LIB_DIR)/libhnswlib-x86-64.dll"
		mkdir -p $(HNSW_LIB_DIR) || true
		g++ -O3 -shared -std=c++11 $(HNSW_CPP_SRC)/bindings.cpp -I $(HNSW_CPP_SRC)/hnswlib -o $(HNSW_LIB_DIR)/libhnswlib-x86-64.dll
    endif

# To build on Windows using the cl.exe compiler use the following commands:
#
# cl /D_USRDLL /D_WINDLL coherence-hnsw\src\main\cpp\bindings.cpp /I prj\coherence-hnsw\src\main\cpp\hnswlib /MT /link /DLL /OUT:prj\coherence-hnsw\target\libs\libhnswlib-x86-64.dll
# ren prj\coherence-hnsw\target\libs\libhnswlib-x86-64.lib libhnswlib-x86-64.libw
#


# ======================================================================================================================
# Run targets
# ======================================================================================================================
##@ Run

storage: # Run a storage enabled Coherence member
	java -cp coherence/target/coherence-$(CURRENT_VERSION).jar:coherence-json/target/coherence-json-$(CURRENT_VERSION).jar \
		-Dcoherence.ttl=0 -Djava.net.preferIPv4Stack=true -Dcoherence.localhost=127.0.0.1 -Dcoherence.wka=127.0.0.1 \
        -Dtangosol.coherence.management=dynamic -Dcoherence.management.http=inherit -Dcoherence.metrics.http.enabled=true \
		-Dcoherence.member=Storage -Dcoherence.log.level=9 \
		$(ARGS) com.tangosol.net.Coherence

