#!/usr/bin/env bash

version=`cat ../../VERSION`

base_version=`echo $version | sed 's/-SNAPSHOT//'`
echo "Building lappsgrid/stanford-vassar:$base_version"
docker build --build-arg VERSION=$version -t lappsgrid/lingpipe-vassar:$base_version .
