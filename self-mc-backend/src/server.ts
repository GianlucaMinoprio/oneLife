import "dotenv/config";
import express from "express";
import cors from "cors";
import routes from "./route";

const app = express();
app.set("trust proxy", true);
const corsOrigin =
  process.env.CORS_ORIGIN === "*" || !process.env.CORS_ORIGIN
    ? true
    : process.env.CORS_ORIGIN.split(",").map((s) => s.trim());
app.use(cors({ origin: corsOrigin }));

// Use JSON body parser for all routes except the webhook, which needs raw body
app.use((req, res, next) => {
  if (req.path === "/self/webhook") return next();
  const limit = process.env.JSON_LIMIT || "1mb";
  return (express.json({ limit }) as any)(req, res, next);
});

app.get("/health", (_req, res) => res.json({ ok: true }));
app.use("/", routes);

const port = Number(process.env.PORT || 8080);
app.listen(port, () => {
  console.log(`Backend listening on :${port}`);
});
