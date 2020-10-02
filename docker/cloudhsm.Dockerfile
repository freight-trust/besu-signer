# Use the amazon linux image
FROM amazonlinux:2

# Install CloudHSM client
RUN yum install -y https://s3.amazonaws.com/cloudhsmv2-software/CloudHsmClient/EL7/cloudhsm-client-latest.el7.x86_64.rpm

# Install CloudHSM Java library
RUN yum install -y https://s3.amazonaws.com/cloudhsmv2-software/CloudHsmClient/EL7/cloudhsm-client-jce-latest.el7.x86_64.rpm

# Install Java, Maven, wget, unzip and ncurses-compat-libs
RUN yum install -y java wget unzip ncurses-compat-libs
RUN yum install -y which

COPY cloudhsm-entrypoint.sh /opt/accountgenerator/bin/cloudhsm-entrypoint.sh
RUN chmod +x /opt/accountgenerator/bin/cloudhsm-entrypoint.sh

COPY accountgenerator /opt/accountgenerator/
WORKDIR /opt/accountgenerator

# Expose services ports
# 7545 HTTP JSON-RPC
EXPOSE 7545

ENTRYPOINT ["/opt/accountgenerator/bin/cloudhsm-entrypoint.sh"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="accountgenerator" \
      org.label-schema.description="Ethereum account generator application" \
      org.label-schema.url="https://docs.accountgenerator.pegasys.tech/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/Adhara-Tech/accountgenerator.git" \
      org.label-schema.vendor="Pegasys" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"
