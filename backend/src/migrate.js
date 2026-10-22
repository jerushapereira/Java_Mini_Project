import 'dotenv/config';
import pg from 'pg';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL is required.');
const pool = new pg.Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: true } : false });
const file = fileURLToPath(new URL('../migrations/001_initial.sql', import.meta.url));
try {
  await pool.query(await readFile(file, 'utf8'));
  console.log('Migration 001 applied.');
} finally {
  await pool.end();
}
