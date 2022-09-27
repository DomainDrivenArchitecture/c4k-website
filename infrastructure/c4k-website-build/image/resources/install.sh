#!/bin/bash

apt update > /dev/null;

install -m 0700 /tmp/entrypoint.sh /
install -m 0700 /tmp/functions.sh /usr/local/bin/
install -m 0700 /tmp/exclude.pattern /home/$USER
install -m 0700 /tmp/project.clj /home/$USER/.lein/
(cd /home/$USER/.lein; lein deps)
