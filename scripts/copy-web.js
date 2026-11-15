// Copies the web app files into www/ so Capacitor can bundle them into the Android app.
const fs = require('fs'), path = require('path');
const root = path.join(__dirname, '..'), out = path.join(root, 'www');
fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out);
for (const f of ['index.html', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png', 'sw.js'])
  fs.copyFileSync(path.join(root, f), path.join(out, f));
console.log('Copied web files to www/');
