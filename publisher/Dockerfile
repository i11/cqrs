FROM ubuntu:bionic as build

ARG ROOTFS=/build/rootfs
ARG DEBS=/build/debs
ARG REQUIRED_PACKAGES="coreutils bash zlib1g libtinfo5 libselinux1"

ARG JAVA_VERSION_MAJOR=8
ARG JAVA_VERSION_MINOR=161
ARG JAVA_VERSION_BUILD=12
ARG JAVA_PACKAGE=server-jre
ARG JAVA_SHA256_SUM=eb5776cacfd57fbf0ffb907f68c58a1cc6f823e761f4e75d78a6e3240846534e
ARG JAVA_URL_ELEMENT=2f38c3b165be4555a1fa6e98c45e0808

ENV DEBIAN_FRONTEND=noninteractive

# Build pre-requisites
RUN bash -c 'mkdir -p $ROOTFS $DEBS $ROOTFS/{bin,sbin,usr/bin,usr/sbin,usr/lib,usr/share,opt}'

# Fix permissions
RUN chown -Rv _apt:root $DEBS \
        && chmod -Rv 700 $DEBS

# Install pre-requisites
RUN apt-get update \
        && apt-get -y install apt-utils curl unzip locales

# UTF8 support
RUN echo "LC_ALL=en_US.UTF-8" > /etc/environment \
        && echo "en_US.UTF-8 UTF-8" > /etc/locale.gen \
        && echo "LANG=en_US.UTF-8" > /etc/locale.conf \
        && locale-gen en_US.UTF-8 \
        && cp -R /usr/lib/locale $ROOTFS/usr/lib/locale \
        && cp -R /usr/share/locale $ROOTFS/usr/share/locale \
        && cp -R /usr/share/locales $ROOTFS/usr/share/locales \
        && cp -R /usr/share/i18n $ROOTFS/usr/share/i18n

# Unpack required packges to rootfs
RUN  cd $DEBS \
  && for pkg in $REQUIRED_PACKAGES; do \
       apt-get download $pkg \
         && apt-cache depends --recurse --no-recommends --no-suggests --no-conflicts --no-breaks --no-replaces --no-enhances --no-pre-depends -i $pkg | grep '^[a-zA-Z0-9]' | xargs apt-get download ; \
     done
RUN if [ "x$(ls $DEBS/)" = "x" ]; then \
      echo No required packages specified; \
    else \
      for pkg in $DEBS/*.deb; do \
        dpkg -x $pkg $ROOTFS; \
      done; \
    fi

# Base
RUN echo "root:x:0:0:root:/home:/bin/sh" > $ROOTFS/etc/passwd \
        && echo "root:x:0:" > $ROOTFS/etc/group

# CA certs
RUN apt-get install -y ca-certificates ca-certificates-java \
        && update-ca-certificates \
        && cp -r /etc/ssl $ROOTFS/etc \
        && cp -r /usr/share/ca-certificates $ROOTFS/usr/share

# Java
RUN curl -jkLH "Cookie: oraclelicense=accept-securebackup-cookie" -o java.tar.gz\
    http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-b${JAVA_VERSION_BUILD}/${JAVA_URL_ELEMENT}/${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz &&\
  echo "$JAVA_SHA256_SUM  java.tar.gz" | sha256sum -c - &&\
  gunzip -c java.tar.gz | tar -xf - -C $ROOTFS/opt && rm -f java.tar.gz &&\
  ln -s /opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR} $ROOTFS/opt/jdk &&\
  curl -jkLH "Cookie: oraclelicense=accept-securebackup-cookie" -o jce_policy-8.zip http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip &&\
  unzip jce_policy-8.zip -d /tmp &&\
  cp /tmp/UnlimitedJCEPolicyJDK8/*.jar $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/security/ &&\
  rm -rf jce_policy-8.zip /tmp/UnlimitedJCEPolicyJDK8 &&\
  rm -rf $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/*src.zip \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/lib/missioncontrol \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/lib/visualvm \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/lib/*javafx* \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/plugin.jar \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/ext/jfxrt.jar \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/bin/javaws \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/javaws.jar \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/desktop \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/plugin \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/deploy* \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/*javafx* \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/*jfx* \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/amd64/libdecora_sse.so \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/amd64/libprism_*.so \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/amd64/libfxplugins.so \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/amd64/libglass.so \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/amd64/libgstreamer-lite.so \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/amd64/libjavafx*.so \
         $ROOTFS/opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/jre/lib/amd64/libjfx*.so

FROM scratch
LABEL author="ilja@bobkevic.com"

ENV LC_ALL=en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV JAVA_HOME=/opt/jdk
ENV PATH=${PATH}:${JAVA_HOME}/bin
ENV ROOTFS=/build/rootfs

COPY --from=build $ROOTFS /

COPY target/publisher-*.jar /usr/share/app/publisher.jar

ENTRYPOINT ["/bin/bash", "-c", "exec /opt/jdk/bin/java $JVM_DEFAULT_ARGS $JVM_ARGS -jar /usr/share/app/publisher.jar \"$@\"", "bash"]