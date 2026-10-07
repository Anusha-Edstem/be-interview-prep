#!/usr/bin/env node
'use strict';

const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const { readPayload, allow, surfaceToModel, extractFileEdit } = require('./_util.cjs');

const ROOT = process.env.CLAUDE_PROJECT_DIR || path.resolve(__dirname, '..', '..');
const LINTED = /^src\/(?:main|test)\/java\/.+\.java$/;
const WRAPPER = process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw';
const OUTPUT_LIMIT = 2000;
const TIMEOUT_MS = 180000;

function relativeToRoot(filePath) {
  const rel = path.relative(ROOT, path.resolve(ROOT, filePath)).replace(/\\/g, '/');
  return rel.startsWith('..') ? null : rel;
}

function spotlessCheck(rel) {
  const wrapper = path.join(ROOT, WRAPPER);
  if (!fs.existsSync(wrapper)) return null;
  const pattern = rel.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  try {
    execFileSync(wrapper, ['-q', '-o', 'spotless:check', `-DspotlessFiles=.*${pattern}`], {
      cwd: ROOT,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: TIMEOUT_MS,
    });
    return null;
  } catch (err) {
    if (err.code === 'ENOENT' || err.killed) return null;
    return ((err.stdout || '') + (err.stderr || '')).trim() || `spotless:check failed on ${rel}`;
  }
}

(async () => {
  const payload = await readPayload();
  const { filePath } = extractFileEdit(payload);
  if (!filePath) return allow();

  const rel = relativeToRoot(filePath);
  if (!rel || !LINTED.test(rel)) return allow();

  const finding = spotlessCheck(rel);
  if (!finding) return allow();

  return surfaceToModel(
    `Spotless flagged ${rel} - run ./mvnw spotless:apply before moving on, because the gate commands run the same check:\n${finding.slice(
      0,
      OUTPUT_LIMIT,
    )}`,
  );
})();
