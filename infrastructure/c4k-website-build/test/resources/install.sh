#!/bin/bash

mkdir /etc/lein/

install -m 0700 /tmp/entrypoint.sh /
install -m 0700 /tmp/functions.sh /usr/local/bin/
install -m 0700 /tmp/exclude.pattern /etc/
install -m 0700 /tmp/project.clj /etc/lein/
