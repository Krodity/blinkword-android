# BlinkWord

An offline speed-reading app for Android. BlinkWord shows a book one word at a
time in a fixed spot (RSVP, *rapid serial visual presentation*), so your eyes
never move across the page. It can also read the book aloud with an on-device
neural voice.

- **RSVP reader** from 100 to 1000 words per minute, with the anchor letter of
  each word highlighted so your eye always lands in the same place.
- **Imports** `.txt`, `.md`, `.epub` and `.pdf` files, or text you paste in.
- **Discover** tab for searching and downloading free books from
  [Project Gutenberg](https://www.gutenberg.org/).
- **Read aloud** with the system TTS or offline neural voices (Piper, Kokoro,
  Matcha, run through [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)).
- **Library** with collections, search, sorting, list or grid view, and covers.
- **Stats**: words read, time spent, average WPM, streaks, a daily word goal,
  and achievements.
- **Backup / restore** of the library and reading progress as a small JSON file.
- No accounts and no tracking. The only network traffic is Gutenberg searches
  and downloads, plus the neural voice models when you choose to download them.

---

## Contents

1. [Installing](#installing)
2. [Using the app](#using-the-app)
3. [How the reader times words](#how-the-reader-times-words)
4. [Neural voices](#neural-voices)
5. [Backups](#backups)
6. [Building from source](#building-from-source)
7. [Project layout](#project-layout)
8. [Troubleshooting](#troubleshooting)

---

## Installing

BlinkWord needs **Android 8.0 (API 26) or newer** on an **arm64** device. The
build only includes `arm64-v8a` native libraries, which covers nearly every
phone sold since 2017. There is no Play Store listing. Build the APK yourself
(see [Building from source](#building-from-source)) and sideload it:

```bash
adb install -r app/build/outputs/apk/debug/blinkword-1.0.0-debug.apk
```

## Using the app

The bottom bar has four tabs: **Library**, **Discover**, **Stats**, and
**Settings** (a sheet that opens from the reader and library).

### Adding books

Tap **+** in the Library and either:

- **Paste text**: give it a title, paste, and tap *Import pasted text*.
- **Choose a file…**: pick a `.txt`, `.md`, `.epub` or `.pdf` with the system
  file picker.
  - **ePub** keeps its chapter list (the *Chapters* sheet in the reader) and
    its cover image.
  - **Markdown** is turned into plain text, so headings, links and emphasis
    markers don't get flashed at you as words.
  - **PDF** text is pulled out with PdfBox. Scanned PDFs that are only images
    have no text to pull out.

Or open **Discover**, search by title or author, and tap a result to add it.
BlinkWord picks the best readable format the book offers (ePub, then plain
text) and also downloads its cover.

### The library

- **Search** at the top filters by title.
- **Sort** by last opened, date added, title (A–Z), or progress.
- Switch between **list and grid** views. Grid thumbnails come in several sizes.
- **Collections**: long-press a book → *Add to collection…*, or create one from
  the collection picker. A book can belong to more than one collection.
- Each book shows a progress ring and when you last opened it.

### Reading

Open a book to get the reader:

| Control | What it does |
|---|---|
| **Play / Pause** | Starts or stops the word stream |
| **Previous / Next word** | Steps one word while paused |
| **Previous / Next sentence** | Jumps to the start of the previous or next sentence |
| **WPM slider** | 100–1000 words per minute, in steps of 5 |
| **Long-press the word** | Opens the full text. Tap any word there to carry on from it |
| **Chapters** | ePub chapter list. Tap one to jump to it |
| **Read aloud** | Speaks the book instead of flashing it. The screen shows the current sentence with a bit of context on either side |

Your place is saved as you read, so you can leave a book and come back to it
at any time.

### Settings

- **Words per minute** and **font size**
- **Theme**: Light, Dark, or Sepia
- **Anchor letter highlight**: colours the anchor letter of each word (its
  optimal recognition point)
- **Focus guides**: small tick marks above and below the anchor letter
- **Daily word goal** for the stats ring and streaks
- **Voice**: pick a system TTS voice and speed, or switch on *Use neural
  voices* (see below)
- **Backup**: *Export* and *Restore*

### Stats

Shows words read, time spent reading, average WPM, books finished, and your
current and best streak. It has a words-per-day chart (this week / all time)
and these achievements:

| Achievement | How to unlock |
|---|---|
| First Steps | Read your first words |
| Speed Demon | Read at 600 WPM or faster |
| Bookworm | Finish a book |
| Streak Master | Read 7 days in a row |
| Marathon | Read 10,000 words in one day |
| Centurion | Read 100,000 words in total |

## How the reader times words

- **Anchor letter (ORP).** Each word has its leading and trailing punctuation
  stripped first. Punctuation inside a word, like *don't* or *well-known*,
  stays. The anchor letter then comes from the standard RSVP table based on
  how many letters are left, and the word is drawn so that letter always sits
  in the same column. A token with no letters at all (like a lone `--`) is
  centred with no highlight.
- **Per-word timing.** The base time per word is `60 000 / WPM` milliseconds.
  Long words and punctuation (commas, ends of sentences, ends of paragraphs)
  add *a fraction of that base time*, not a fixed number of milliseconds. That
  keeps the pauses feeling the same at 150 WPM and at 900 WPM.
- **Sentences** are found with an abbreviation-aware splitter (so "Dr." and
  "e.g." don't end a sentence). Sentence skipping and read-aloud both use it.

## Neural voices

Under **Settings → Neural voices** you can download offline voice models. Each
one runs fully on the device through sherpa-onnx. The list covers a few
different model types, so you have something to switch to if one doesn't suit
you:

| Voice | Notes |
|---|---|
| Matcha · LJSpeech | Fast, clear US female voice |
| Piper · Lessac (high) | US, fp16 |
| Piper · Ryan (high) | US male, fp16 |
| Piper · Alan (UK) | British male, fp16 |
| Kokoro v1.1 (many voices) | One large model that contains many speakers. Pick a speaker once it's installed |
| Kokoro v0.19 (compressed) | int8: smaller, audibly rougher |
| Piper · Amy (tiny) | int8, smallest download |

Models are downloaded from the sherpa-onnx GitHub releases and unpacked as
they download (bzip2 → tar → app storage), so a 100 MB model never sits on the
phone as a temporary archive. Use the **Sample** button to preview a voice and
**Remove** to delete it. When *Use neural voices* is off, BlinkWord falls back
to whatever system TTS engine the phone has.

## Backups

**Settings → Backup → Export** writes a JSON file through the system file
picker. It holds your library list, collections, reading positions, stats and
settings, but **not the book text**, so it stays a few kilobytes.

**Restore** merges the backup into what's already on the phone. Nothing is
replaced outright:

- Books from **Discover** are downloaded again from their saved source URL.
- Books **imported from local files** can't be downloaded again. Their reading
  position is kept and applied when you import a book with the same title.
- A reading position from the backup only wins if it's **further along** than
  the one on the phone, so an old backup never rewinds a book.
- Daily stats keep the **higher** figure for each day, so restoring the same
  file twice changes nothing.

Android's built-in cloud backup is turned off (`allowBackup="false"`). Your
data only leaves the phone when you export it.

## Building from source

Requirements:

- JDK 17 or newer (21 recommended)
- Android SDK with platform **37** (compile/target SDK 37, min SDK 26)
- Internet access the first time, so Gradle can download dependencies

```bash
git clone https://github.com/Krodity/blinkword-android.git
cd blinkword-android
echo "sdk.dir=$ANDROID_HOME" > local.properties   # or open it in Android Studio
./gradlew assembleDebug                            # APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest                        # JVM unit tests
```

Notes:

- `app/libs/sherpa-onnx-1.13.8.aar` is the official prebuilt AAR from
  sherpa-onnx (Apache-2.0). It's committed to the repo because upstream doesn't
  publish it to Maven Central.
- The debug build is signed with `~/.config/.android/debug.keystore` if that
  file exists, and with AGP's default debug key if it doesn't. The release
  build uses the same debug key. It's a sideload-only app, so swap in your own
  signing config before distributing it.
- Room schema history lives in `app/schemas/` (database version 6).

## Project layout

```
app/src/main/java/uk/krodity/blinkword/
├── MainActivity.kt
├── logic/          pure Kotlin, unit-tested: ORP, word timing, tokenizer,
│                   sentences, markdown→text, stats, achievements, backup merge
├── data/
│   ├── *Dao.kt, AppDatabase.kt   Room: documents, chapters, chunks,
│   │                             collections, reading days
│   ├── SettingsRepository.kt     DataStore preferences
│   ├── importers/                txt / md / epub (jsoup) / pdf (PdfBox)
│   ├── discover/                 Project Gutenberg search + download
│   ├── backup/                   JSON export / restore
│   └── tts/                      system TTS + sherpa-onnx neural voices
└── ui/             Jetpack Compose screens + theme
```

Large books are stored as **content chunks** rather than one huge string, so
opening a book with hundreds of thousands of words doesn't load it all into
memory at once. The full-text view lays out one `Text` per paragraph, so only
the paragraphs on screen are ever composed.

## Troubleshooting

| Problem | Fix |
|---|---|
| *Couldn't reach Project Gutenberg* | Check your connection. Gutenberg sometimes rate-limits, so wait a minute and try again |
| A PDF imports with no text | It's a scanned PDF (images only). Run OCR on it first, or find an ePub copy |
| *No speech voices installed on this device* | Install a TTS engine (e.g. Google Speech Services), or download a neural voice |
| A neural voice download stops partway | Retry. Partial downloads are thrown away and never half-installed |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | The installed APK was signed with a different key. Uninstall it first (this deletes app data, so export a backup beforehand) |

## License

MIT. See [LICENSE](LICENSE). sherpa-onnx is Apache-2.0. The neural voice
models have their own licenses, listed on their upstream pages.
