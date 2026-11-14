# WolfPlayer — personal offline music player (PWA)

## Put it on your phone (GitHub Pages, free, ~5 min)
1. Create a new public GitHub repo, e.g. `wolfplayer`.
2. Upload all files in this folder (index.html, manifest.webmanifest, sw.js, icon-192.png, icon-512.png) to the repo root.
3. Repo → Settings → Pages → Source: "Deploy from a branch", Branch: `main` / root → Save.
4. Wait ~1 minute, then open `https://<your-username>.github.io/wolfplayer/` in Chrome on your phone.
5. Chrome menu (⋮) → "Add to Home screen" → "Install". It now opens like a normal app.

## Using it
- **+ Add music**: pick audio (MP3, M4A, OGG, WAV, FLAC*) or video (MP4, WEBM*) files. Video files play as audio only.
  *Format support depends on Chrome's codecs; anything Chrome can't play is skipped with a message.
- Songs are copied into the app's private storage, so they stay even if you delete the originals.
- Filenames like `Artist - Title.mp3` are split into artist/title automatically. ⋮ on a song to rename / set artist.
- **Playlists** tab → + New → tick songs → Add. ▲ moves a song up. ⋮ to rename/delete.
- Player controls: **Shuffle**; **Repeat** cycles Off → Repeat playlist → Repeat song (shows a "1"). 
  Previous restarts the song if more than 3 seconds in.
- Keeps playing with the screen off; lock-screen / notification / Bluetooth-headphone controls work.

## Notes
- Your music never leaves the phone. GitHub only hosts the app code.
- Clearing Chrome's site data for the app (or uninstalling it) deletes the stored songs.
- If music stops when the screen is off, set Chrome's battery usage to "Unrestricted" in Android settings.
