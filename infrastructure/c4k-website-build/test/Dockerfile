FROM clojure:lein

# Prepare Entrypoint Script
ADD resources /tmp

RUN /tmp/install.sh
RUN apt update
RUN apt -yqq --no-install-recommends --yes install curl default-jre-headless


RUN curl -L -o /tmp/serverspec.jar \
    https://github.com/DomainDrivenArchitecture/dda-serverspec-crate/releases/download/2.0.0/dda-serverspec-standalone.jar

COPY serverspec.edn /tmp/serverspec.edn

RUN java -jar /tmp/serverspec.jar /tmp/serverspec.edn -v