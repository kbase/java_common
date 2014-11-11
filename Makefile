TARGET ?= /kb/deployment
TARGET-SCRIPT = $(TARGET)/bin/java_generic_script
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
	
deploy: deploy-scripts

deploy-client:
	@echo "nothing to deploy"

deploy-docs:
	@echo "nothing to deploy"

deploy-scripts:
	echo '#!/bin/bash' > $(TARGET-SCRIPT)
	echo "JARS=$(TARGET)/lib/jars" >> $(TARGET-SCRIPT)
	echo 'INITCP=$$JARS/kbase/common/kbase-common-dev-1415681177-41c302c.jar:$$JARS/jackson/jackson-annotations-2.2.3.jar:$$JARS/jackson/jackson-core-2.2.3.jar:$$JARS/jackson/jackson-databind-2.2.3.jar' >> $(TARGET-SCRIPT)
	echo 'FULLCP=$$(java -cp $$INITCP us.kbase.common.awe.task.JavaGenericScript $$1 kbase)' >> $(TARGET-SCRIPT)
	echo 'java -cp $$FULLCP us.kbase.common.awe.task.JavaGenericScript $$2 $$3 $$4 $$5 $$6 $$7' >> $(TARGET-SCRIPT)
	chmod 775 $(TARGET-SCRIPT)

deploy-service:
	@echo "nothing to deploy"

jenkins:
	$(ANT) jenkins

clean:
	$(ANT) clean
