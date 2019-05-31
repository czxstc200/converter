#!/bin/bash
mvn clean install
cd controller/target
java -DRootDir=/home/rec/ -jar controller-1.0-SNAPSHOT.jar