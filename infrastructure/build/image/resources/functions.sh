#!/bin/bash

function generate-netrc-file() {
    echo "machine $GITHOST password $AUTHTOKEN" > ~/.netrc
}

function get-website-data() {    
    curl -H "Authorization: token $AUTHTOKEN" -o $SOURCEDIR/$1 $GITREPOURL
}

function get-hash-data() {
    curl -s -H "Authorization: token $AUTHTOKEN" $GITCOMMITURL | jq '.sha'
}

function write-hash-data() {
    echo $1 > $HASHFILEDIR/$2
}

function unzip-website-data() {
    unzip $SOURCEDIR/$1 -d $BUILDDIR
}

function build-website() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; bash generate.sh;)
}

function move-website-files-to-target() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; rsync -ru --exclude-from "/etc/exclude.pattern" --delete target/html/* $WEBSITEROOT;)
}

function install-hugo-from-deb() {
    curl -L "https://github.com/gohugoio/hugo/releases/download/v${HUGO_VERSION}/hugo_extended_${HUGO_VERSION}_linux-amd64.deb" -o hugo_extended_${HUGO_VERSION}_linux-amd64.deb
    curl -L "https://github.com/gohugoio/hugo/releases/download/v${HUGO_VERSION}/hugo_${HUGO_VERSION}_checksums.txt" -o checksums.txt
    EXPECTED_CHECKSUM="$(sha256sum hugo_extended_${HUGO_VERSION}_linux-amd64.deb)"
    ACTUAL_CHECKSUM="$(grep hugo_extended_${HUGO_VERSION}_linux-amd64.deb checksums.txt)"
    if [ "$EXPECTED_CHECKSUM" != "$ACTUAL_CHECKSUM" ]
    then
        >&2 echo 'ERROR: Invalid installer checksum'
        rm hugo.deb
        exit 1
    fi

    echo "Installing hugo"
    echo
    dpkg -i hugo_extended_${HUGO_VERSION}_linux-amd64.deb

    echo "Clean up"
    rm hugo_extended_${HUGO_VERSION}_linux-amd64.deb
    rm checksums.txt

}
