FROM clojure:temurin-18-jammy

# Prepare Entrypoint Script
ADD resources /tmp

ENV BUILDDIR="website"

RUN /tmp/install.sh
RUN /tmp/entrypoint.sh
