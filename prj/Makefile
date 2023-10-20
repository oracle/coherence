# ----------------------------------------------------------------------------------------------------------------------
# Copyright (c) 2022, 2023, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
# ----------------------------------------------------------------------------------------------------------------------
# This is the Makefile to build the Coherence.
# Although Coherence is built using Maven, there are sone nuances to the Maven commands.
# This Makefile makes remembering the commands a little simpler.
#
# ----------------------------------------------------------------------------------------------------------------------

ARGS            ?=
CURRENT_VERSION ?= $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout -nsu)
NO_TDE          ?= -Dtde.compile.not.required=true

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
all: ## A full build
	$(call mvn_build)

.PHONY: all-no-tde
all-no-tde: ## A full build without compiling TDE
	$(call mvn_build,$(NO_TDE))

define mvn_build
	mvn clean install -U -DskipTests $(1) $(ARGS)
endef

# ======================================================================================================================
# Run targets
# ======================================================================================================================
##@ Run

storage: ## Run a storage enabled Coherence member
	java -cp coherence/target/coherence-$(CURRENT_VERSION).jar \
		-Dcoherence.ttl=0 -Djava.net.preferIPv4Stack=true -Dcoherence.localhost=127.0.0.1 -Dcoherence.wka=127.0.0.1 \
		-Dtangosol.coherence.management=dynamic -Dcoherence.management.http=inherit -Dcoherence.metrics.http.enabled=true \
		-Dcoherence.member=Storage -Dcoherence.log.level=9 \
		$(ARGS) com.tangosol.net.DefaultCacheServer
