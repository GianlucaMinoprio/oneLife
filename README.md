# oneLife

Minecraft server with only one life through proof of humanity (via Self). This repo contains:

- `minecraft-server/`: Paper 1.21.8 server preloaded with the OneLife plugin
- `self-mc-backend/`: Node/Express backend that handles verification and player state

## Prerequisites

- Java 21 (for Paper 1.21.x)
- Node.js 20+ and npm
- Optional (for mobile verification on real device): ngrok (or any HTTPS tunnel)

## 1) Start the backend (port 8080)

Create a `.env` file in `self-mc-backend/` (or export env vars) and then install + run.

```bash
cd /Users/gianluk/Documents/SdeProjects/oneLife/self-mc-backend

# .env (recommended defaults for local)
cat > .env << 'EOF'
PORT=8080
CORS_ORIGIN=*
JWT_SECRET=dev_secret
SQLITE_PATH=./data.sqlite

# Self verification settings
SELF_SCOPE=minecraft-poh
SELF_APP_ID=mc-one-life
SELF_IS_PROD=false
SELF_ENDPOINT_TYPE=http
SELF_VERIFY_URL=http://localhost:8080/self/verify
EOF

npm install
npm run dev
```

The backend will listen on `http://localhost:8080` and persist data in `self-mc-backend/data.sqlite`.

## 2) Point the plugin to your backend

Edit `minecraft-server/plugins/OneLife/config.yml` and set:

```yaml
backendBase: "http://localhost:8080"
pollSeconds: 3
# configId can stay as-is; it's only used for a fallback QR link
```

## 3) Run the Minecraft server (port 25565)

```bash
cd /Users/gianluk/Documents/SdeProjects/oneLife/minecraft-server
java -Xms1G -Xmx2G -jar paper-1.21.8-60.jar nogui
```

Notes:

- `eula.txt` is already accepted. If the server asks for EULA, set `eula=true`.
- Default online mode is enabled (`online-mode=true` in `server.properties`). Use a regular Mojang/Microsoft account.

## 4) Connect and verify

1. Launch Minecraft Java Edition 1.21.8 and connect to `localhost:25565`.
2. On first join, you’ll receive a map with a QR code. Scan it to start Self verification.
3. After verification succeeds, the plugin will remove restrictions and apply nationality cosmetics.
4. You can always use `/verify` in-game to show the QR again.

## Using a phone? Expose the backend with HTTPS

Mobile devices can’t reach `localhost`. Use a tunnel and update config:

```bash
# Example with ngrok
ngrok http 8080
# Copy the https URL it gives you, e.g. https://abc123.ngrok-free.app
```

Then update both places:

- `minecraft-server/plugins/OneLife/config.yml` → `backendBase: "https://abc123.ngrok-free.app"`
- `self-mc-backend/.env` → `SELF_VERIFY_URL=https://abc123.ngrok-free.app/self/verify` and `SELF_ENDPOINT_TYPE=https`

Restart the backend and run `/verify` in-game to get a fresh QR.

### Production-style .env (recommended with ngrok)

If you're using ngrok and want a production-like setup, use this `.env` in `self-mc-backend/` (replace with your ngrok URL):

```bash
PORT=8080
APP_SECRET=change_me_signing_secret
SELF_APP_ID=mc-one-life
SELF_CONFIG_ID=0x7b6436b0c98f62380866d9432c2af0ee08ce16a171bda6951aecd95ee1307d61
SELF_SCOPE="minecraft-poh"
SELF_ENDPOINT=NGROK_URL
NODE_ENV="production"
SELF_ENV="production"

# Storage and CORS
SQLITE_PATH=./data.sqlite
CORS_ORIGIN=*
```

- Set the plugin `minecraft-server/plugins/OneLife/config.yml` to:

```yaml
backendBase: NGROK_URL
pollSeconds: 3
```

Restart the backend and the Minecraft server after changing these values.

## Troubleshooting

- Backend health: visit `http://localhost:8080/health` → `{ ok: true }`
- Can’t complete verification on phone: ensure your backend is reachable over HTTPS (tunnel) and both `backendBase` and `SELF_VERIFY_URL` use the same public URL.
- Port conflicts: change `PORT` in `.env` and `backendBase` accordingly; Minecraft server port is set by `server.properties` (`server-port=25565`).
- Rebuilding the plugin (optional):
  - The repo already ships with `OneLife-0.1.0.jar` in `minecraft-server/plugins/`.
  - If you change Java sources under `oneLife-plugin/`, build with your Gradle setup and replace the jar in `minecraft-server/plugins/`.
