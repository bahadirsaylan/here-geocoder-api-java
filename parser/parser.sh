#!/bin/bash

INPUT=$1

java -cp target/parser-1.0-SNAPSHOT-jar-with-dependencies.jar com.here.geocoder.ParserApp $INPUT com_here_geocoder
