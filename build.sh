#!/bin/bash

gradle build -x test
docker build $* -t yti-datamodel-api .
