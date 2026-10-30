# netSearch

netSearch is a Java command-line web crawler. It downloads pages starting from a
given URL, follows links it finds, and saves each page and its metadata to a
local directory.

## Requirements

- Java 21 or later
- Maven

## Build

```sh
mvn package
```

## Run

```sh
java -jar target/netSearch-1.0-SNAPSHOT.jar standard <output-directory> <starting-url> [flags]
```

For example:

```sh
java -jar target/netSearch-1.0-SNAPSHOT.jar standard ./pages https://example.com -nogui
```

### Flags

- `-nogui` runs the crawler without opening the monitor window.
- `-inf` removes the default 500-page limit and keeps the crawler running until
  you stop it with Ctrl+C. The crawler stays running even if it runs out of
  undiscovered links. Pages already being downloaded are allowed up to 30
  seconds to finish when stopping.

Flags can be used together:

```sh
java -jar target/netSearch-1.0-SNAPSHOT.jar standard ./pages https://example.com -inf -nogui
```

## Output

Each downloaded page is saved as a SHA-256-named `.html` file. A matching
`.json` file stores the page URL and HTML title, for example:

```json
{
  "url": "https://example.com/",
  "title": "Example Domain"
}
```
