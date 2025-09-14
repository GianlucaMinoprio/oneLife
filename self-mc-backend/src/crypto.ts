import jwt from "jsonwebtoken";

const APP_SECRET =
  process.env.JWT_SECRET || process.env.APP_SECRET || "dev_secret";

export function signLoginToken(data: {
  mc_uuid: string;
  country_code?: string | null;
  status: "unverified" | "verified" | "dead";
}) {
  return jwt.sign(
    { sub: data.mc_uuid, cc: data.country_code || null, st: data.status },
    APP_SECRET,
    { algorithm: "HS256", expiresIn: "2m" }
  );
}
