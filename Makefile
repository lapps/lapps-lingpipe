VERSION=$(shell cat VERSION)
WAR=LingpipeServices\#$(VERSION).war

include ../master.mk

docker:
	cp target/$WAR src/docker
	cd src/docker && ./docker.sh build $(VERSION)

push:
	src/docker/docker.sh push

tag:
	src/docker/docker.sh tag $(VERSION)

start:
	src/docker/docker.sh start

stop:
	src/docker/docker.sh stop
