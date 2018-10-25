#!/usr/bin/env bash

sbt +clean +compile +templateJS/publishSigned +templateJVM/publishSigned sonatypeRelease