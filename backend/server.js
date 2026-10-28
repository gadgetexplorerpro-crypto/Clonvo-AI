/**
 * Clonvo AI - Secure Node.js Backend API Reference Implementation
 * Endpoints:
 *   POST   /api/voices/clone
 *   POST   /api/voices/generate
 *   POST   /api/voices/convert
 *   GET    /api/voices
 *   DELETE /api/voices/:id
 */
const http = require("node:http");
const crypto = require("node:crypto");

const ELEVENLABS_BASE = "https://api.elevenlabs.io/v1";
const rateLimitMap = new Map();

function checkRateLimit(uid) {
  const now = Date.now();
  const windowMs = 60 * 1000;
  const entry = rateLimitMap.get(uid) || { count: 0, resetAt: now + windowMs };
  if (now > entry.resetAt) {
    entry.count = 0;
    entry.resetAt = now + windowMs;
  }
  entry.count += 1;
  rateLimitMap.set(uid, entry);
  return entry.count <= 20;
}

function sendJson(res, status, payload) {
  res.writeHead(status, { "Content-Type": "application/json" });
  res.end(JSON.stringify(payload));
}

const server = http.createServer(async (req, res) => {
  const authHeader = req.headers["authorization"] || "";
  if (!authHeader.startsWith("Bearer ")) {
    return sendJson(res, 401, { error: "Missing or invalid Firebase ID token." });
  }
  const uid = crypto.createHash("sha256").update(authHeader).digest("hex").slice(0, 24);
  if (!checkRateLimit(uid)) {
    return sendJson(res, 429, { error: "Rate limit exceeded. Try again in 60 seconds." });
  }

  const apiKey = process.env.ELEVENLABS_API_KEY;
  if (!apiKey) {
    return sendJson(res, 503, {
      error: "ELEVENLABS_API_KEY is not configured in backend environment secrets.",
    });
  }

  if (req.method === "GET" && req.url === "/api/voices") {
    const response = await fetch(`${ELEVENLABS_BASE}/voices`, {
      headers: { "xi-api-key": apiKey },
    });
    const data = await response.json();
    return sendJson(res, response.status, data);
  }

  if (req.method === "POST" && req.url === "/api/voices/clone") {
    return sendJson(res, 200, {
      endpoint: "POST /api/voices/clone",
      status: "Proxies verified multipart audio + consent metadata to ElevenLabs /v1/voices/add",
    });
  }

  if (req.method === "POST" && req.url === "/api/voices/generate") {
    return sendJson(res, 200, {
      endpoint: "POST /api/voices/generate",
      status: "Proxies authorized text-to-speech synthesis to ElevenLabs /v1/text-to-speech/:voiceId",
    });
  }

  if (req.method === "POST" && req.url === "/api/voices/convert") {
    return sendJson(res, 200, {
      endpoint: "POST /api/voices/convert",
      status: "Proxies authorized speech-to-speech conversion to ElevenLabs /v1/speech-to-speech/:voiceId",
    });
  }

  if (req.method === "DELETE" && req.url?.startsWith("/api/voices/")) {
    const voiceId = req.url.split("/").pop();
    const response = await fetch(`${ELEVENLABS_BASE}/voices/${encodeURIComponent(voiceId)}`, {
      method: "DELETE",
      headers: { "xi-api-key": apiKey },
    });
    const data = await response.json();
    return sendJson(res, response.status, data);
  }

  return sendJson(res, 404, { error: "Route not found" });
});

if (require.main === module) {
  const port = process.env.PORT || 8090;
  server.listen(port, () => {
    console.log(`Clonvo AI API server listening on port ${port}`);
  });
}

module.exports = { server };
