#!/usr/bin/env bash

command=$1
version=$2

IMAGE=lappsgrid/lingpipe-vassar

function check_version() {
    if [ "$version" = "" ] ; then
        echo "Version number is missing"
        exit 1
    fi
}

case $command in
    build)
        check_version
        base_version=`echo $version | sed 's/-SNAPSHOT//'`
        echo "Building lappsgrid/stanford-vassar:$base_version"
        docker build --build-arg VERSION=$version -t $IMAGE .
        ;;
    push)
        docker push $IMAGE
        ;;
    tag)
        check_version
        docker tag $IMAGE $IMAGE:$version
        docker push $IMAGE:$version
        ;;
    start)
        docker run -d -p 8080:8080 --name lingpipe $IMAGE
        ;;
    stop)
        docker rm -f lingpipe
        ;;
    *)
        echo "Unrecognized command $command"
        exit 1
        ;;
esac


