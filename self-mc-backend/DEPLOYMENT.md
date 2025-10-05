# Deployment Guide

## Preventing Database Loss on Railway

Your SQLite database was being reset on each deployment because Railway uses ephemeral storage by default. Here's how to fix it:

## ✅ Recommended Solution: Use Railway Volumes

### Step 1: Create a Volume

1. Go to your Railway project dashboard
2. Select your `self-mc-backend` service
3. Navigate to **Settings** → **Volumes**
4. Click **"New Volume"**
5. Configure:
   - **Mount Path**: `/data`
   - **Size**: 1GB (more than enough for SQLite)
6. Click **"Add"**

### Step 2: Deploy

The code has already been updated to use `/data/data.sqlite` when running on Railway. Just push your changes:

```bash
git add .
git commit -m "Add persistent volume support"
git push origin main
```

### Step 3: Verify

After deployment, check the logs to ensure the database is being created in `/data/data.sqlite`.

## 🔄 Backup & Restore

### Manual Backup (Local Development)

```bash
npm run backup
```

This creates a timestamped backup in the `./backups` directory.

### Restore from Backup

```bash
npm run restore backups/backup-YYYY-MM-DDTHH-MM-SS.sqlite
```

### Railway Backup Strategy

Railway volumes persist across deployments but **not if you delete the service**. Options:

1. **Regular backups via Railway CLI:**

   ```bash
   # Install Railway CLI
   npm i -g @railway/cli

   # Login
   railway login

   # Connect to your project
   railway link

   # Download database
   railway run cat /data/data.sqlite > backup.sqlite
   ```

2. **Automated backups with Railway Cron:**
   - Add a cron service to your Railway project
   - Schedule daily backups to cloud storage (S3, Google Cloud Storage, etc.)

## 🔍 Troubleshooting

### Database still resetting?

1. Verify the volume is mounted at `/data`
2. Check Railway logs for any permission errors
3. Ensure `RAILWAY_ENVIRONMENT` env var exists (it's auto-set by Railway)

### Need to migrate data?

If you have an old database backup, you can restore it:

1. **Using Railway CLI:**

   ```bash
   railway shell
   # Then in the shell:
   cat > /data/data.sqlite
   # Paste your backup content (or use railway run)
   ```

2. **Via code:** Temporarily add an endpoint to upload a backup file

## 🚀 Alternative: Use PostgreSQL

For a more robust solution, consider PostgreSQL:

1. In Railway, add **New** → **Database** → **PostgreSQL**
2. Railway auto-creates `DATABASE_URL` environment variable
3. Migrate the store.ts to use PostgreSQL instead of SQLite

Benefits:

- Better for concurrent connections
- Built-in backups in Railway
- More scalable

Let me know if you want help migrating to PostgreSQL!

## 📊 Current Database Schema

```sql
CREATE TABLE bindings (
  nullifier TEXT PRIMARY KEY,        -- Self identity (stable per user per app)
  mc_uuid TEXT UNIQUE NOT NULL,      -- Minecraft UUID
  country_code TEXT NOT NULL,        -- ISO 3166 alpha-2 country code
  status TEXT NOT NULL,              -- 'bound' or 'dead_banned'
  created_at INTEGER NOT NULL,       -- Unix timestamp (ms)
  verified_at INTEGER,               -- Unix timestamp (ms)
  session_id TEXT,                   -- Temporary session ID
  session_expires_at INTEGER         -- Session expiration (Unix timestamp ms)
);

CREATE INDEX idx_bindings_mc_uuid ON bindings(mc_uuid);
```

## 🔐 Environment Variables

Required for Railway:

- `PORT` - Auto-set by Railway
- `RAILWAY_ENVIRONMENT` - Auto-set by Railway
- `CORS_ORIGIN` - Your allowed origins (comma-separated)

Optional:

- `SQLITE_PATH` - Override database path (defaults to `/data/data.sqlite` on Railway)
- `BACKUP_KEEP_COUNT` - Number of backups to keep (default: 10)
