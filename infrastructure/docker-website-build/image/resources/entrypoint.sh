#!/bin/bash

source /usr/local/bin/functions.sh

function main() {
    get-and-unzip-website-data
    build-and-extract-website
    move-website-files-to-target    
}

main

while true; do
    sleep 1m
done
