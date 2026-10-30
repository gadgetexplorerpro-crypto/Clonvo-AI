// Writes a sandbox-only emulator config derived from the repo's firebase.json.
//
// Two deviations from the committed config, both required inside Docker:
//   1. hosts are bound to 0.0.0.0 so sibling services and the port-3000 proxy
//      can reach the emulators (firebase.json binds 127.0.0.1);
//   2. rules paths are made absolute, because the generated config lives in
//      /tmp and firebase resolves relative paths against the config file.
// The committed firebase.json is left untouched.
const fs = require("node:fs");

const PROJECT_DIR = process.env.PROJECT_DIR || "/workspace";
const OUTPUT = "/tmp/firebase.base44.json";

const config = JSON.parse(fs.readFileSync(`${PROJECT_DIR}/firebase.json`, "utf8"));

for (const entry of config.firestore || []) {
  if (entry.rules && !entry.rules.startsWith("/")) {
    entry.rules = `${PROJECT_DIR}/${entry.rules}`;
  }
}

config.emulators = {
  firestore: { host: "0.0.0.0", port: 8085 },
  auth: { host: "0.0.0.0", port: 9099 },
  ui: { enabled: false },
};

fs.writeFileSync(OUTPUT, JSON.stringify(config, null, 2));
console.log(`[base44] wrote ${OUTPUT}`);
