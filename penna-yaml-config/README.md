# penna-yaml-config

This is a very simple wrapper implementation of `penna.api.config.ConfigManager` that looks up for a `penna.yaml` file
in your resources and applies the configuration from that file into penna.

Given `penna` is designed for apps running in [kubernetes](https://kubernetes.io), we believe that yaml is a more
natural configuration format than xml, so it should be convenient to set yaml files in k8s.

## Roadmap

- 1.0
- [x] read `penna.yaml`;
- [ ] allow for a separate yaml file for test;
- [ ] watch `penna.yaml` file for re-configuring the app upon app update;
