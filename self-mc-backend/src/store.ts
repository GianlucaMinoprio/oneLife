import Database from "better-sqlite3";

export type Status = "bound" | "dead_banned";

export interface Binding {
  nullifier: string; // stable per user per app
  mc_uuid: string; // Mojang online UUID (string)
  country_code: string; // ISO 3166 alpha-2
  status: Status; // 'bound' or 'dead_banned'
  created_at: number;
  verified_at: number | null;
  session_id: string | null;
  session_expires_at: number | null;
}

const dbPath = process.env.SQLITE_PATH || "data.sqlite";
const db = new Database(dbPath);
db.pragma("journal_mode = WAL");
db.exec(`
CREATE TABLE IF NOT EXISTS bindings (
  nullifier TEXT PRIMARY KEY,
  mc_uuid TEXT UNIQUE NOT NULL,
  country_code TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('bound','dead_banned')),
  created_at INTEGER NOT NULL,
  verified_at INTEGER,
  session_id TEXT,
  session_expires_at INTEGER
);
CREATE INDEX IF NOT EXISTS idx_bindings_mc_uuid ON bindings(mc_uuid);
`);

export const Store = {
  getByUuid(mc_uuid: string): Binding | undefined {
    return db.prepare("SELECT * FROM bindings WHERE mc_uuid=?").get(mc_uuid) as
      | Binding
      | undefined;
  },
  getByNullifier(nullifier: string): Binding | undefined {
    return db
      .prepare("SELECT * FROM bindings WHERE nullifier=?")
      .get(nullifier) as Binding | undefined;
  },
  insert(b: Binding) {
    db.prepare(
      `
      INSERT INTO bindings(nullifier, mc_uuid, country_code, status, created_at, verified_at, session_id, session_expires_at)
      VALUES (@nullifier,@mc_uuid,@country_code,@status,@created_at,@verified_at,@session_id,@session_expires_at)
    `
    ).run(b as any);
  },
  updateStatusByUuid(mc_uuid: string, status: Status) {
    db.prepare(
      `UPDATE bindings SET status=?, verified_at=CASE WHEN ?='bound' THEN COALESCE(verified_at, strftime('%s','now')*1000) ELSE verified_at END WHERE mc_uuid=?`
    ).run(status, status, mc_uuid);
  },
  setSession(mc_uuid: string, session_id: string, expiresAt: number) {
    db.prepare(
      `UPDATE bindings SET session_id=?, session_expires_at=? WHERE mc_uuid=?`
    ).run(session_id, expiresAt, mc_uuid);
  },
  upsertOnVerify(payload: {
    nullifier: string;
    mc_uuid: string;
    country_code: string;
  }) {
    const existingByNull = this.getByNullifier(payload.nullifier);
    const existingByUuid = this.getByUuid(payload.mc_uuid);

    // conflict: same human tries a new MC account
    if (existingByNull && existingByNull.mc_uuid !== payload.mc_uuid) {
      throw new Error("NULLIFIER_ALREADY_BOUND_TO_DIFFERENT_UUID");
    }
    // handle existing row for this UUID
    if (existingByUuid && existingByUuid.nullifier !== payload.nullifier) {
      // if current row was a placeholder created at session time, upgrade it
      if (existingByUuid.nullifier.startsWith("pending:")) {
        db.prepare(
          `UPDATE bindings
           SET nullifier=?, country_code=?, status='bound',
               verified_at=strftime('%s','now')*1000,
               session_id=NULL, session_expires_at=NULL
           WHERE mc_uuid=?`
        ).run(payload.nullifier, payload.country_code, payload.mc_uuid);
        return;
      }
      // otherwise, true conflict: same MC tries a new human
      throw new Error("UUID_ALREADY_BOUND_TO_DIFFERENT_NULLIFIER");
    }

    if (!existingByNull && !existingByUuid) {
      this.insert({
        nullifier: payload.nullifier,
        mc_uuid: payload.mc_uuid,
        country_code: payload.country_code,
        status: "bound",
        created_at: Date.now(),
        verified_at: Date.now(),
        session_id: null,
        session_expires_at: null,
      });
    } else {
      db.prepare(
        `UPDATE bindings SET country_code=?, status='bound', verified_at=strftime('%s','now')*1000 WHERE nullifier=? AND mc_uuid=?`
      ).run(payload.country_code, payload.nullifier, payload.mc_uuid);
    }
  },
};
