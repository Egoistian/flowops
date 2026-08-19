#!/usr/bin/env bash
set -euo pipefail

flowops_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$flowops_root"

docker-compose config --format json | ruby -rjson -e '
  config = JSON.parse($stdin.read)
  services = config.fetch("services")

  frontend_ports = services.fetch("frontend").fetch("ports")
  expected_frontend_port = frontend_ports.any? do |port|
    port.fetch("host_ip", "") == "127.0.0.1" &&
      port.fetch("published").to_s == "4173" &&
      port.fetch("target").to_s == "80"
  end
  abort "frontend must publish only 127.0.0.1:4173:80" unless expected_frontend_port

  %w[backend postgres].each do |service_name|
    ports = services.fetch(service_name).fetch("ports", [])
    abort "#{service_name} must not publish host ports" unless ports.empty?
  end
'

printf 'PASS local-demo-network\n'
