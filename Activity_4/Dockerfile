# Build from the repository root: docker build -f Activity_4/Dockerfile -t comp6120-game .
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace/Activity_2
COPY Activity_2/ ./
RUN chmod +x gradlew && ./gradlew clean build javadocAudit :lwjgl3:installDist --no-daemon

FROM eclipse-temurin:25-jre-noble AS java-runtime

# Selkies supplies the display, window manager, PulseAudio and browser audio/video stream.
FROM ghcr.io/linuxserver/baseimage-selkies:debiantrixie AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    libasound2t64 libopenal1 libxi6 libxrandr2 libxcursor1 libxinerama1 \
    libgl1-mesa-dri libglx-mesa0 fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

COPY --from=java-runtime /opt/java/openjdk/ /opt/java/openjdk/
COPY --from=build /workspace/Activity_2/lwjgl3/build/install/lwjgl3/ /opt/game/
COPY --chmod=0755 Activity_4/start-game.sh Activity_4/healthcheck.sh /usr/local/bin/
# The base image runs this application as its unprivileged desktop user, abc.
COPY --chmod=0755 Activity_4/autostart.sh /defaults/autostart
COPY --chmod=0755 Activity_4/init-game.sh /custom-cont-init.d/50-game-autostart.sh

ENV JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/java/openjdk/bin:$PATH \
    JAVA_OPTS="-Duser.home=/config" \
    TITLE="COMP6120 Game" \
    PIXELFLUX_WAYLAND=false \
    LIBGL_ALWAYS_SOFTWARE=true \
    AUTO_GPU=false \
    ALSOFT_DRIVERS=pulse \
    PULSE_SINK=output \
    START_DOCKER=false \
    SELKIES_AUDIO_ENABLED=true \
    SELKIES_AUDIO_ON_START=true \
    SELKIES_AUDIO_DEVICE_NAME=output.monitor \
    SELKIES_MICROPHONE_ENABLED=false \
    SELKIES_WEBCAM_ENABLED=false \
    SELKIES_ENABLE_CLIPBOARD=false \
    SELKIES_ENABLE_BINARY_CLIPBOARD=false \
    NO_WEBCAM=true

WORKDIR /opt/game
EXPOSE 3001
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=3 \
    CMD ["/usr/local/bin/healthcheck.sh"]
# Keep the base image's /init entrypoint: it starts/supervises desktop, streaming and audio.
