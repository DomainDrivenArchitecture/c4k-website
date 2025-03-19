FROM clojure:lein

# Prepare Entrypoint Script
ADD resources /tmp

ENV HUGO_VERSION="0.138.0"
ENV GO_VERSION="1.22.6"
ENV BUILDDIR="/etc/website"
ENV SOURCEDIR="/etc/websitesource"
ENV WEBSITEROOT="/var/www/html/website/"
ENV HASHFILEDIR="/var/hashfile.d"
ENV PATH="${PATH}:/usr/local/go/bin"

RUN /tmp/install.sh
