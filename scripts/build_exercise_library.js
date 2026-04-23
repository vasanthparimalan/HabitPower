/**
 * build_exercise_library.js
 *
 * Converts exercise_source.json (developer source of truth) into
 * app/src/main/assets/exercise_library.json (runtime asset shipped in APK).
 *
 * Strips the `source` field (dev metadata) and sorts by category then name.
 *
 * Usage: node scripts/build_exercise_library.js
 */

const fs   = require('fs');
const path = require('path');

const SOURCE     = path.join(__dirname, '..', 'exercise_source.json');
const OUTPUT     = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'exercise_library.json');
const ASSETS_DIR = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'exercises');
const EXTENSIONS = ['webp', 'png', 'jpg', 'jpeg', 'svg'];

function resolveImageUri(e) {
  // If source file has an explicit imageUri that isn't wger-id based, keep it as-is
  if (e.imageUri && !e.imageUri.match(/^exercises\/\d+\.\w+$/)) return e.imageUri;

  // Try to find the actual image file on disk (any supported extension)
  if (e.wger_id) {
    for (const ext of EXTENSIONS) {
      const candidate = path.join(ASSETS_DIR, `${e.wger_id}.${ext}`);
      if (fs.existsSync(candidate)) return `exercises/${e.wger_id}.${ext}`;
    }
    // File not on disk yet — keep the reference so it resolves when images are added
    return `exercises/${e.wger_id}.webp`;
  }

  // Custom exercise: try slug-based filename
  if (e.name) {
    const slug = e.name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
    for (const ext of EXTENSIONS) {
      const candidate = path.join(ASSETS_DIR, `${slug}.${ext}`);
      if (fs.existsSync(candidate)) return `exercises/${slug}.${ext}`;
    }
  }

  return null;
}

function main() {
  if (!fs.existsSync(SOURCE)) {
    console.error('exercise_source.json not found. Run scripts/generate_source.js first.');
    process.exit(1);
  }

  const source = JSON.parse(fs.readFileSync(SOURCE, 'utf8'));

  const runtime = source.map(e => ({
    wger_id:       e.wger_id      ?? null,
    name:          e.name,
    category:      e.category,
    primaryMuscle: e.primaryMuscle ?? null,
    instructions:  e.instructions  ?? null,
    tags:          e.tags          ?? '',
    imageUri:      resolveImageUri(e),
  }));

  fs.writeFileSync(OUTPUT, JSON.stringify(runtime, null, 2), 'utf8');

  const counts = {};
  runtime.forEach(e => { counts[e.category] = (counts[e.category] || 0) + 1; });
  console.log(`Built ${runtime.length} exercises → exercise_library.json`);
  Object.entries(counts).sort().forEach(([cat, n]) => console.log(`  ${cat}: ${n}`));
}

main();
