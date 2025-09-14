import dotenv from "dotenv";
import express from "express";
import cors from "cors";
import routes from "./route";

dotenv.config();
const app = express();
app.use(cors({ origin: true }));

// Use JSON body parser for all routes except the webhook, which needs raw body
app.use((req, res, next) => {
  if (req.path === "/self/webhook") return next();
  return (express.json({ limit: "1mb" }) as any)(req, res, next);
});

app.get("/health", (_req, res) => res.json({ ok: true }));
app.use("/", routes);

const port = Number(process.env.PORT || 8080);
app.listen(port, () => {
  console.log(`Backend listening on :${port}`);
});
