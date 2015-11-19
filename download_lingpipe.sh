#!/bin/bash

wget -nc http://lingpipe-download.s3.amazonaws.com/lingpipe-4.1.0.jar
mvn install:install-file -DgroupId=com.aliasi -DartifactId=lingpipe -Dversion=4.1.0 -Dpackaging=jar -Dfile=lingpipe-4.1.0.jar
