# Docker

[Home](home) · [Installation and Setup](installation-and-setup)

## Current support

Docker is not required for the documented local build or desktop game. The inspected baseline does not contain a project Dockerfile, Compose file or committed container-launch workflow. Do not run `docker build .` expecting a supplied game image.

The existing CI configuration requests `eclipse-temurin:25-jdk` for the Gradle job. This provides the configured JDK image when the assigned GitLab runner uses a container-capable executor. It runs the build, JUnit tests and JavaDoc audit; it is not an interactive game deployment.

## Reproducibility

The wrapper fixes Gradle at 9.7.1 and verifies its distribution checksum. Build properties specify libGDX 1.14.2, LWJGL 3.4.1 and JUnit 5.13.4. The Java toolchain requests version 25. First-time builds still need dependency downloads, and the CI image tag is not pinned to an immutable digest.

## Remaining deployment work

A future Docker deliverable should commit the Dockerfile, document the build/test entry point, and record a successful clean-image run. Running the graphical game in a container additionally requires an explicit display/OpenGL/audio arrangement; headless JUnit success would not validate that setup.

For the working assessment path, follow [Installation and Setup](installation-and-setup). This page records the actual baseline rather than claiming the planned Docker packaging is already implemented.
