FROM envoyproxy/envoy:v1.28-latest

COPY envoy.yaml /etc/envoy/envoy.yaml

CMD ["/usr/local/bin/envoy", "-c", "/etc/envoy/envoy.yaml", "-l", "info"] 