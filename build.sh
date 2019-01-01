#!/bin/bash
ROOT=~/dist/here-geocoding

cd restclient
mvn clean compile assembly:single
mkdir -p $ROOT/restclient
cp target/restclient-1.0-SNAPSHOT-jar-with-dependencies.jar $ROOT/restclient
cp restclient.sh $ROOT/restclient
chmod +x *.sh

cd ../parser
mvn clean compile assembly:single
mkdir -p $ROOT/parser
cp target/parser-1.0-SNAPSHOT-jar-with-dependencies.jar $ROOT/parser
cp parser.sh $ROOT/parser
chmod +x *.sh