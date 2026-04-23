/**
 * generate_source.js
 *
 * Builds exercise_source.json (the developer source of truth) by:
 *   1. Loading the cached wger exercise list (wger_exercises_cache.json)
 *   2. Filtering to clean English-only entries
 *   3. Merging instructions/primaryMuscle from the existing exercise_library.json
 *   4. Appending custom exercises (yoga, stretching, HIIT) not in wger
 *
 * Run once, then manually curate exercise_source.json as needed.
 * Run scripts/build_exercise_library.js afterward to produce the runtime asset.
 *
 * Usage: node scripts/generate_source.js
 */

const fs = require('fs');
const path = require('path');

const WGER_CACHE = path.join(__dirname, '..', 'wger_exercises_cache.json');
const EXISTING_LIBRARY = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'exercise_library.json');
const OUTPUT = path.join(__dirname, '..', 'exercise_source.json');

// wger category name → our ExerciseCategory enum
const CATEGORY_MAP = {
  'Abs':       'STRENGTH',
  'Arms':      'STRENGTH',
  'Back':      'STRENGTH',
  'Chest':     'STRENGTH',
  'Legs':      'STRENGTH',
  'Shoulders': 'STRENGTH',
  'Calves':    'STRENGTH',
  'Cardio':    'CARDIO_OTHER',
};

function mapCategory(wgerCat) {
  return CATEGORY_MAP[wgerCat] || 'STRENGTH';
}

function isCleanEnglishName(name) {
  if (!name || typeof name !== 'string') return false;
  // No non-ASCII characters (rejects Spanish, Italian, German, etc.)
  if (/[^\x00-\x7F]/.test(name)) return false;
  // Reasonable length
  if (name.length < 4 || name.length > 80) return false;
  // Not ALL CAPS (>60% of letters uppercase = data quality issue)
  const letters = name.match(/[a-zA-Z]/g) || [];
  const uppers  = name.match(/[A-Z]/g) || [];
  if (letters.length > 3 && uppers.length / letters.length > 0.6) return false;
  // Skip test-variant suffixes
  if (/ (HD|NB|MP|SZ-bar)$/.test(name)) return false;
  // Skip pipe-separated multi-variants
  if ((name.match(/\|/g) || []).length > 0) return false;
  // Skip obviously single-word non-English stubs
  const wordCount = name.trim().split(/\s+/).length;
  if (wordCount === 1 && name.length < 6) return false;
  return true;
}

function normalizeKey(name) {
  return name.toLowerCase().replace(/[^a-z0-9]/g, '');
}

function main() {
  const wger     = JSON.parse(fs.readFileSync(WGER_CACHE, 'utf8'));
  const existing = JSON.parse(fs.readFileSync(EXISTING_LIBRARY, 'utf8'));

  // Build lookup from existing library (by normalized name) for instructions + muscle
  const existingByKey = {};
  existing.forEach(e => {
    existingByKey[normalizeKey(e.name)] = e;
  });

  // Track which wger IDs map to which existing exercise categories
  const seenKeys = new Set();
  const wgerEntries = [];

  for (const w of wger) {
    if (!isCleanEnglishName(w.name)) continue;
    const key = normalizeKey(w.name);
    if (seenKeys.has(key)) continue;
    seenKeys.add(key);

    const match = existingByKey[key];
    const category = mapCategory(w.category);
    const imageUri = `exercises/${w.id}.webp`;

    wgerEntries.push({
      wger_id:      w.id,
      source:       'wger',
      name:         w.name,
      category:     category,
      primaryMuscle: match?.primaryMuscle || null,
      instructions: match?.instructions   || null,
      tags:         match?.tags           || category.toLowerCase().replace('_other', ''),
      imageUri:     imageUri,
    });
  }

  // Custom exercises: yoga, stretching, HIIT from our existing library
  // that are not STRENGTH (wger doesn't cover these well)
  const customCategories = new Set(['YOGA', 'STRETCHING', 'HIIT', 'CARDIO_OTHER']);
  const customEntries = [];

  for (const e of existing) {
    if (!customCategories.has(e.category)) continue;
    // Skip any that matched a wger entry (already covered above)
    if (seenKeys.has(normalizeKey(e.name))) continue;
    customEntries.push({
      wger_id:      null,
      source:       'custom',
      name:         e.name,
      category:     e.category,
      primaryMuscle: e.primaryMuscle || null,
      instructions: e.instructions  || null,
      tags:         e.tags          || e.category.toLowerCase(),
      imageUri:     null,
    });
  }

  const all = [...wgerEntries, ...customEntries].sort((a, b) => {
    if (a.category < b.category) return -1;
    if (a.category > b.category) return 1;
    return a.name.localeCompare(b.name);
  });

  fs.writeFileSync(OUTPUT, JSON.stringify(all, null, 2), 'utf8');

  const counts = {};
  all.forEach(e => { counts[e.category] = (counts[e.category] || 0) + 1; });
  console.log(`Generated ${all.length} exercises → exercise_source.json`);
  console.log('  wger:', wgerEntries.length, '  custom:', customEntries.length);
  Object.entries(counts).sort().forEach(([cat, n]) => console.log(`  ${cat}: ${n}`));
}

main();
