
FROM openjdk:11.0.2-jre-slim-stretch

COPY accountgenerator /opt/accountgenerator/
WORKDIR /opt/accountgenerator

# Expose services ports
# 7545 HTTP JSON-RPC
EXPOSE 7545

ENTRYPOINT ["/opt/accountgenerator/bin/accountgenerator"]

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