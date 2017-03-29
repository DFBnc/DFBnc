FROM java:8-jdk
MAINTAINER Shane Mc Cormack <dataforce@dataforce.org.uk>

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

COPY . /dfbnc/

RUN \
  cd /dfbnc && \
  if [ -e .git/shallow ]; then git fetch --unshallow; fi && \
  git fetch --tags && \
  ./gradlew jar && \
  mv /dfbnc/dist/dfbnc.jar / && \
  rm -rf /dfbnc

EXPOSE 33262 33263

VOLUME ["/var/lib/dfbnc"]

WORKDIR /var/lib/dfbnc

CMD ["/usr/bin/java", "-jar", "/dfbnc.jar", "--config", "/var/lib/dfbnc", "--foreground"]
