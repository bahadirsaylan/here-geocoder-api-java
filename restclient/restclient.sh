#!/bin/bash

OUTPUT=$1
ADDRESS_COL=$2

java -cp target/restclient-1.0-SNAPSHOT-jar-with-dependencies.jar com.here.geocoder.App $1 $2
