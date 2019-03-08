#!/bin/bash

gradle build
docker build $* -t yti-datamodel-api .
