# Tooling image for the Firebase emulator suite used by firebase.json.
# Only the toolchain lives here — the project source is bind-mounted at
# runtime, so edits to firestore.rules / auth config take effect on restart.
FROM node:22-bookworm

RUN apt-get update \
  && apt-get install -y --no-install-recommends openjdk-17-jre-headless \
  && rm -rf /var/lib/apt/lists/*

RUN npm install -g firebase-tools@15.34.2

WORKDIR /workspace
