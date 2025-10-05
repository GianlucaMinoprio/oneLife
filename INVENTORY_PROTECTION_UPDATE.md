# 🛡️ Inventory Protection Update

## What Was Fixed

Your OneLife plugin has been updated to **prevent inventory loss** when the backend database gets reset. This was the cause of your items disappearing earlier.

## Changes Made

### 1. Plugin Update (`Restrictor.java`)

The plugin now:

- ✅ **Detects existing players** using `hasPlayedBefore()`
- ✅ **Saves their inventory** before applying restrictions
- ✅ **Restores inventory** after verification completes
- ✅ **Only clears inventory for brand new players**

**Key additions:**

```java
// Before restricting an existing player
if (p.hasPlayedBefore()) {
    savedInventories.put(p.getUniqueId(), new SavedInventory(p));
    plugin.getLogger().info("OneLife: Saved inventory for existing player");
}

// After verification completes
SavedInventory saved = savedInventories.remove(p.getUniqueId());
if (saved != null) {
    saved.restore(p);
    plugin.getLogger().info("OneLife: Restored inventory for existing player");
}
```

### 2. Player Data Restored

Your inventory from before the reset has been restored:

- ✅ Backup file from October 5, 22:41:37 uploaded
- ✅ File contains all your items and XP

### 3. Backend Persistence Fixed

The backend code now uses persistent storage on Railway:

- ✅ Updated to use `/data/data.sqlite` when on Railway
- ✅ Backup scripts added (`npm run backup`)
- ✅ `.gitignore` updated to exclude database files

## Deployment Steps

### Step 1: ✅ Plugin Updated (DONE)

The new plugin JAR has been uploaded to your server at:
`/default/plugins/OneLife.jar`

### Step 2: ✅ Player Data Restored (DONE)

Your inventory backup has been restored.

### Step 3: 🔴 Set Up Railway Volume (REQUIRED)

**This is critical to prevent future database loss!**

1. Go to [Railway Dashboard](https://railway.app)
2. Select your `self-mc-backend` service
3. Navigate to **Settings** → **Volumes**
4. Click **"New Volume"**
5. Configure:
   - **Mount Path**: `/data`
   - **Size**: `1GB`
6. Click **"Add"**

### Step 4: 🔴 Deploy Backend Updates (REQUIRED)

```bash
cd /Users/gianluk/Documents/SdeProjects/oneLife/self-mc-backend
git add .
git commit -m "Add inventory protection and persistent storage"
git push origin main
```

### Step 5: Restart Your Minecraft Server

1. Go to your ApexMinecraftHosting control panel
2. Click **"Restart"** (not "Stop" then "Start", just "Restart")
3. Wait for the server to fully start

### Step 6: Test It Out

1. Join your Minecraft server
2. You should see your items are back!
3. Your verification status should already be set from before

## How It Works Now

### For Existing Players (You!)

1. Join server
2. Backend doesn't recognize you (database was reset)
3. **Plugin saves your inventory temporarily** 🆕
4. QR code appears for verification
5. You scan and verify with Self
6. **Plugin restores your inventory** 🆕
7. You continue playing with all your items!

### For New Players

1. Join server
2. Start with empty inventory (normal)
3. Verify with Self
4. Start their journey

## Verification

After deploying, check your server logs for these messages:

```
[OneLife] OneLife: Saved inventory for existing player uuid=bd5ac45c-e1eb-4416-b9ee-02c5df354784
[OneLife] OneLife: Restored inventory for existing player uuid=bd5ac45c-e1eb-4416-b9ee-02c5df354784
```

## Files Changed

### Plugin Files

- `/oneLife-plugin/src/main/java/com/onelife/listeners/Restrictor.java` - Added inventory protection

### Backend Files

- `/self-mc-backend/src/store.ts` - Added persistent storage path
- `/self-mc-backend/src/backup.ts` - New backup utilities
- `/self-mc-backend/package.json` - Added backup scripts
- `/self-mc-backend/.gitignore` - Exclude database files
- `/self-mc-backend/DEPLOYMENT.md` - Deployment guide

## Backup Your Database (Optional but Recommended)

After everything is working, create regular backups:

```bash
# Install Railway CLI (one time)
npm i -g @railway/cli

# Login and link to your project
railway login
railway link

# Create backup
railway run cat /data/data.sqlite > backup-$(date +%Y%m%d).sqlite
```

## Troubleshooting

### Inventory still empty after restart?

1. Make sure you restarted the server (not just reloaded plugins)
2. Check that the player data file was uploaded correctly
3. Look for errors in the server logs

### Backend still resetting?

1. Verify Railway volume is created and mounted at `/data`
2. Check Railway logs to ensure database path is correct
3. Look for: `"Using persistent storage at /data/data.sqlite"`

### Plugin errors?

1. Check server logs in `/default/logs/latest.log`
2. Ensure Paper/Spigot version is compatible (1.21.8)
3. Verify the JAR was uploaded to `/default/plugins/OneLife.jar`

## Success Indicators

✅ Server starts without errors  
✅ Inventory is visible when you join  
✅ Backend stays populated after deployments  
✅ New players can join and verify normally  
✅ Death still bans players correctly

## Next Steps

Once everything is working:

1. ✅ Commit and push the plugin changes
2. ✅ Set up regular backups (weekly recommended)
3. ✅ Monitor Railway volume usage
4. ✅ Consider upgrading Railway plan if needed

---

**Built on:** October 5, 2025  
**Plugin Version:** OneLife 0.1.0 (with inventory protection)  
**Backend Version:** Updated with persistent storage
