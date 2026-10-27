FROM gradle:8.11.1-jdk17
USER root
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"
RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && curl -fL --retry 3 https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/android-tools.zip \
    && unzip -q /tmp/android-tools.zip -d /tmp/android-tools \
    && mv /tmp/android-tools/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest" \
    && rm -rf /tmp/android-tools /tmp/android-tools.zip \
    && yes | sdkmanager --licenses >/dev/null \
    && sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" \
    && chown -R gradle:gradle "$ANDROID_HOME"
USER gradle
