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

# download website data
# check if hashfile exists
# if yes
    # hash the current file
    # compare current hash to hashfile
        # same? 
            # do nothing
        # not same?
            # overwrite hashfile with new hash
            # start the website build
# if not
    # hash the current file
    # write the hashfile
    # start the build

echo "Downloading website data"
get-website-data $filename
echo "Check for new content"
if [[ -f $hashfile ]]
    then
    currentHash=$(print-hash-from-file $filename )
    
    else
    write-hashfile $filename $hashfilename

fi


echo "Executing Custom Scripts, if applicable"
execute-scripts-when-existing
echo "Building website"
build-and-extract-website
echo "Moving files"
move-website-files-to-target    
