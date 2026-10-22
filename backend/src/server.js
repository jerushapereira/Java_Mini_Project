import 'dotenv/config';
import pg from 'pg';
import { createApp } from './app.js';

const { Pool } = pg;
if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL is required. Copy .env.example to .env and configure it.');
const pool = new Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: true } : false, connectionTimeoutMillis: 5000, idleTimeoutMillis: 30000 });
const app = createApp({ pool, frontendOrigin: process.env.FRONTEND_ORIGIN ?? 'http://localhost:3000' });
const port = Number(process.env.PORT ?? 3000);
const server = app.listen(port, () => console.log(`Smart Expense & Budget Tracker listening on http://localhost:${port}`));

async function shutdown() {
  server.close(async () => { await pool.end(); process.exit(0); });
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
