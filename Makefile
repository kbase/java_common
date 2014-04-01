JAR-PREFIX = kbase-common

ANT = ant

GITCOMMIT := $(shell git rev-parse --short HEAD)
EPOCH := $(shell date +%s)
TAGS := $(shell git tag --contains $(GITCOMMIT))
TAG := $(shell python internal/checktags.py $(TAGS))

ERR := $(findstring Two valid tags for this commit, $(TAG))

ifneq ($(ERR), )
$(error Tags are ambiguous for this commit: $(TAG))
endif 

COMMON-JAR = $(JAR-PREFIX)-$(TAG).jar

ifeq ($(TAG), )
COMMON-JAR = $(JAR-PREFIX)-$(EPOCH)-$(GITCOMMIT).jar
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
	
deploy:
	@echo "nothing to deploy"

deploy-client:
	@echo "nothing to deploy"

deploy-docs:
	@echo "nothing to deploy"

deploy-scripts:
	@echo "nothing to deploy"

deploy-service:
	@echo "nothing to deploy"

jenkins:
	$(ANT) jenkins

clean:
	$(ANT) clean
