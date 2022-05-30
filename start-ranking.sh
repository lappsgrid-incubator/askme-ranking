#!/usr/bin/env bash

IP=149.165.156.83

java  -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+CMSParallelRemarkEnabled -XX:CMSInitiatingOccupancyFraction=30 -XX:+UseCMSInitiatingOccupancyOnly -Xms4g -Xmx4G -jar service.jar
