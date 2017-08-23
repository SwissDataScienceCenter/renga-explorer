#!/bin/bash

sbt ++$TRAVIS_SCALA_VERSION publish

sbt ++$TRAVIS_SCALA_VERSION docker:publishLocal
docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD" $DOCKER_REGISTRY
docker tag renga-explorer:0.1.0-SNAPSHOT $DOCKER_REGISTRY/swissdatasciencecenter/images/renga-explorer:0.1.0-SNAPSHOT
docker push $DOCKER_REGISTRY/swissdatasciencecenter/images/renga-explorer:0.1.0-SNAPSHOT
