#!/usr/bin/env bash

rm jvm/src/main/resources/app/*
sbt templateJS/clean templateJS/fullOptJS templateJVM/assembly