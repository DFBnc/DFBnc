FROM openjdk:8-jdk
MAINTAINER Shane Mc Cormack <dataforce@dataforce.org.uk>

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8

COPY . /tmp/dfbnc/

RUN \
  useradd dfbnc && \
  mkdir /home/dfbnc && \
  mkdir /var/lib/dfbnc && \
  chown -R dfbnc /tmp/dfbnc && \
  chown -R dfbnc /home/dfbnc && \
  chown -R dfbnc /var/lib/dfbnc

USER dfbnc

RUN \
  cd /tmp/dfbnc && \
  if [ -e .git/shallow ]; then git fetch --unshallow; fi && \
  git fetch --tags && \
  ./gradlew jar && \
  mv /tmp/dfbnc/dist/dfbnc.jar /home/dfbnc/ && \
  rm -rf /tmp/dfbnc

EXPOSE 33262 33263

VOLUME ["/var/lib/dfbnc"]

WORKDIR /var/lib/dfbnc

CMD ["/usr/bin/java", "-jar", "/home/dfbnc/dfbnc.jar", "--config", "/var/lib/dfbnc", "--foreground"]
