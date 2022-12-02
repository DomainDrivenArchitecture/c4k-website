#!/bin/bash

mkdir $BUILDDIR
mkdir $SOURCEDIR
mkdir -p $HASHFILEDIR

set -o nounset
set -o xtrace
set -o errexit
set -eo pipefail

source /usr/local/bin/functions.sh

filename="website.zip"
hashfilename="hashfile"

# create empty hashfile
# download website data
# compare current hash to hashfile
    # same? 
        # do nothing
    # not same?
        # overwrite hashfile with new hash
        # unzip website
        # execute scripts (if applicable)
        # build website
        # move files

echo "Downloading website data"
get-website-data $filename

echo "Check for new content"
currentHash=$( print-hash-from-file $filename )
touch $HASHFILEDIR/$hashfilename
if [[ $currentHash == $(cat $HASHFILEDIR/$hashfilename) ]]
    then
        echo "Nothing to do"
    else
        write-hashfile $currentHash $hashfilename
        unzip-website-data $filename
        echo "Executing Custom Scripts, if applicable"
        execute-scripts-when-existing
        echo "Building website"
        build-website
        echo "Moving files"
        move-website-files-to-target
fi




