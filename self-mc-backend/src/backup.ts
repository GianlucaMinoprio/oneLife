import fs from "fs";
import path from "path";
import { exec } from "child_process";
import { promisify } from "util";

const execAsync = promisify(exec);

/**
 * Simple backup script for SQLite database
 * Can be run as a cron job or triggered manually
 */
export async function backupDatabase(
  dbPath: string,
  backupDir: string = "./backups"
) {
  try {
    // Create backup directory if it doesn't exist
    if (!fs.existsSync(backupDir)) {
      fs.mkdirSync(backupDir, { recursive: true });
    }

    // Generate backup filename with timestamp
    const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
    const backupPath = path.join(backupDir, `backup-${timestamp}.sqlite`);

    // Copy the database file
    fs.copyFileSync(dbPath, backupPath);

    console.log(`✅ Database backed up to: ${backupPath}`);

    // Optional: Keep only last N backups (e.g., 10)
    const backupFiles = fs
      .readdirSync(backupDir)
      .filter((f) => f.startsWith("backup-") && f.endsWith(".sqlite"))
      .sort()
      .reverse();

    const keepCount = parseInt(process.env.BACKUP_KEEP_COUNT || "10");
    if (backupFiles.length > keepCount) {
      const toDelete = backupFiles.slice(keepCount);
      toDelete.forEach((file) => {
        const filePath = path.join(backupDir, file);
        fs.unlinkSync(filePath);
        console.log(`🗑️  Deleted old backup: ${file}`);
      });
    }

    return backupPath;
  } catch (error) {
    console.error("❌ Backup failed:", error);
    throw error;
  }
}

/**
 * Restore database from backup
 */
export function restoreDatabase(backupPath: string, dbPath: string) {
  try {
    if (!fs.existsSync(backupPath)) {
      throw new Error(`Backup file not found: ${backupPath}`);
    }

    // Create a backup of current database before restoring
    if (fs.existsSync(dbPath)) {
      const tempBackup = `${dbPath}.before-restore`;
      fs.copyFileSync(dbPath, tempBackup);
      console.log(`📦 Current database backed up to: ${tempBackup}`);
    }

    // Restore the backup
    fs.copyFileSync(backupPath, dbPath);
    console.log(`✅ Database restored from: ${backupPath}`);
  } catch (error) {
    console.error("❌ Restore failed:", error);
    throw error;
  }
}

// CLI usage
if (import.meta.url === `file://${process.argv[1]}`) {
  const command = process.argv[2];
  const dbPath =
    process.env.SQLITE_PATH ||
    (process.env.RAILWAY_ENVIRONMENT ? "/data/data.sqlite" : "data.sqlite");

  if (command === "backup") {
    backupDatabase(dbPath).then(() => process.exit(0));
  } else if (command === "restore" && process.argv[3]) {
    restoreDatabase(process.argv[3], dbPath);
    process.exit(0);
  } else {
    console.log("Usage:");
    console.log("  npm run backup        # Create a backup");
    console.log("  npm run restore <file> # Restore from backup");
    process.exit(1);
  }
}
