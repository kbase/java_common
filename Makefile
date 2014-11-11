KB_TOP ?= /kb/deployment
KB_RUNTIME ?= /kb/runtime
JAVA_HOME ?= $KB_RUNTIME/java
AWE_WORKER_SCRIPT = $(KB_TOP)/bin/java_generic_script
JAR-PREFIX = kbase-common

ANT = ant

GITCOMMIT := $(shell git rev-parse --short HEAD)
EPOCH := $(shell date +%s)
#TODO use --points-at vs. --contains when git 1.7.10 available
TAGS := $(shell git tag --contains $(GITCOMMIT))
TAG := $(shell python internal/checktags.py $(TAGS))

ERR := $(findstring Two valid tags for this commit, $(TAG))

ifneq ($(ERR), )
$(error Tags are ambiguous for this commit: $(TAG))
endif 

COMMON-JAR = $(JAR-PREFIX)-$(TAG)

ifeq ($(TAG), )
COMMON-JAR = $(JAR-PREFIX)-$(EPOCH)-$(GITCOMMIT)
endif

# make sure our make test works
.PHONY : test

default: build-libs build-docs

build-libs:
	$(ANT) compile -Dcompile.jarfile=$(COMMON-JAR)

build-docs: build-libs
	-rm -r docs 
	$(ANT) javadoc

test:
	$(ANT) test -Dcompile.jarfile=$(COMMON-JAR)

test-client:
	@echo "no client"
	
test-service:
	@echo "no service"

test-scripts:
	@echo "no scripts to test"

test-worker:
	echo "$$($(AWE_WORKER_SCRIPT) kbase-common-0.0.8.jar:jackson-annotations-2.2.3.jar:jackson-core-2.2.3.jar:jackson-databind-2.2.3.jar 12345 us.kbase.common.awe.test.TempTask listToMap 5B5B226B31222C227632222C226B34222C227635225D5D 7B7D)" | grep status=done || exit 1
	echo "Test passed"
	
deploy: deploy-worker

deploy-client:
	@echo "nothing to deploy"

deploy-docs:
	@echo "nothing to deploy"

deploy-scripts:
	@echo "nothing to deploy"

deploy-worker:
	echo '#!/bin/bash' > $(AWE_WORKER_SCRIPT)
	echo 'export KB_TOP=$(KB_TOP)' >> $(AWE_WORKER_SCRIPT)
	echo 'export KB_RUNTIME=$(KB_RUNTIME)' >> $(AWE_WORKER_SCRIPT)
	echo 'export JAVA_HOME=$(JAVA_HOME)' >> $(AWE_WORKER_SCRIPT)
	echo 'export PATH=$$KB_RUNTIME/bin:$$KB_TOP/bin:$$JAVA_HOME/bin:$$PATH' >> $(AWE_WORKER_SCRIPT)
	echo 'JARS=$(KB_TOP)/lib/jars' >> $(AWE_WORKER_SCRIPT)
	echo 'INITCP=$$JARS/kbase/common/kbase-common-0.0.8.jar:$$JARS/jackson/jackson-annotations-2.2.3.jar:$$JARS/jackson/jackson-core-2.2.3.jar:$$JARS/jackson/jackson-databind-2.2.3.jar' >> $(AWE_WORKER_SCRIPT)
	echo 'FULLCP=$$(java -cp $$INITCP us.kbase.common.awe.task.JavaGenericScript $$1 kbase)' >> $(AWE_WORKER_SCRIPT)
	echo 'java -cp $$FULLCP us.kbase.common.awe.task.JavaGenericScript $$2 $$3 $$4 $$5 $$6 $$7' >> $(AWE_WORKER_SCRIPT)
	chmod 775 $(AWE_WORKER_SCRIPT)

deploy-service:
	@echo "nothing to deploy"

jenkins:
	$(ANT) jenkins

clean:
	$(ANT) clean
