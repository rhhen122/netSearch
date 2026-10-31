# netSearch

> A lightweight Java web crawler that saves pages locally with searchable
> metadata.

netSearch starts at a URL, follows links it discovers, and stores each page as
HTML alongside a JSON file containing the page URL and title.

---

## ✨ Features

- Concurrent crawling with a default limit of **500 pages**
- Optional custom page cap or unlimited crawling
- HTML snapshots and matching JSON metadata files
- Optional desktop monitor, or quiet command-line operation
- Halt mode that checkpoints the remaining crawl queue for later resumption

## 🚀 Get started

**Requirements:** Java 21+ and Maven

Build the executable JAR:

```sh
mvn package
```

Start a crawl:

```sh
java -jar target/netSearch-1.1.0.jar standard <output-directory> <starting-url> [flags]
```

For example:

```sh
java -jar target/netSearch-1.1.0.jar standard ./pages https://example.com -nogui
```

## ⚙️ Flags

| Flag | Description |
| --- | --- |
| `-cap=<number>` | Set the maximum pages to crawl. Must be a positive whole number; defaults to `500`. |
| `-threads=<number>` | Set the number of crawler workers and monitor rows. Must be a positive whole number; defaults to `10`. |
| `-inf` | Crawl without a page limit until stopped with **Ctrl+C**. Overrides `-cap`. |
| `-nogui` | Run without opening the monitor window. |

Use the `halt` mode instead of `standard` to stop after the configured page cap
and save only the remaining crawl queue for a later run.

Combine flags as needed:

```sh
java -jar target/netSearch-1.1.0.jar standard ./pages https://example.com -cap=600 -threads=20 -nogui
```

Run in halt mode and continue the saved crawl in a later invocation:

```sh
java -jar target/netSearch-1.1.0.jar halt ./pages https://example.com -cap=500 -threads=10 -nogui
java -jar target/netSearch-1.1.0.jar halt -resume
```

Halt mode saves only its remaining URL queue and crawl settings (including the
starting URL) in `netSearch-halt.json` in the current working directory. It does
not save a list of URLs already parsed; saved page files are used to avoid
re-crawling completed pages after resuming. Resume from that directory; the
saved cap applies again to each resumed run. The checkpoint is removed when no
queued sites remain. Once a capped run stops, other tools can read the saved
page files while the pending queue remains in the checkpoint; `halt -resume`
continues with that queue afterward.

Or run without a page limit:

```sh
java -jar target/netSearch-1.1.0.jar standard ./pages https://example.com -inf -nogui
```

## 📁 Saved files

Each downloaded page is saved in the output directory using a SHA-256 filename:

```text
<sha256>.html
<sha256>.json
```

The JSON file contains the source URL and HTML page title:

```json
{
  "url": "https://example.com/",
  "title": "Example Domain"
}
```
