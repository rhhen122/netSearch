
package org.netSearch.crawler;

import org.netSearch.web.GetSite;
import org.netSearch.web.GetAnchor;
import org.netSearch.ui.StartUI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CrawlWorker implements Runnable {

    private final CrawlManager manager;
    private final String storageDir;
    private final int workerId;

    public CrawlWorker(CrawlManager manager, String storageDir, int workerId) {
        this.manager = manager;
        this.storageDir = storageDir;
        this.workerId = workerId;
    }

    @Override
    public void run() {

        GetSite downloader = new GetSite();
        GetAnchor parser = new GetAnchor();

        System.out.println("[WORKER " + workerId + "] Started.");

        StartUI.updateWorkerStatus(
                workerId,
                "Idle - waiting for URL"
        );

        while (true) {

            String url;

            try {
                url = manager.getNextUrl();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                StartUI.updateWorkerStatus(
                        workerId,
                        "Stopped - interrupted"
                );

                return;
            }

            // Stop signal received
            if (manager.isStopSignal(url)) {

                System.out.println(
                        "[WORKER " + workerId + "] Stopped."
                );

                StartUI.updateWorkerStatus(
                        workerId,
                        "Stopped"
                );

                return;
            }

            try {

                crawl(url, downloader, parser);

            } catch (Exception e) {

                System.out.println(
                        "[WORKER " + workerId +
                                "] [ERROR] " + url +
                                " - " + e.getMessage()
                );

                StartUI.updateWorkerStatus(
                        workerId,
                        "ERROR: " + e.getMessage()
                );

            } finally {

                // Mark this page as processed
                manager.taskFinished();
            }

            // Return to idle after processing
            StartUI.updateWorkerStatus(
                    workerId,
                    "Idle - waiting for URL"
            );
        }
    }

    private void crawl(
            String url,
            GetSite downloader,
            GetAnchor parser
    ) throws Exception {

        // Downloading
        StartUI.updateWorkerStatus(
                workerId,
                "Downloading: " + url
        );

        System.out.println();
        System.out.println(
                "[WORKER " + workerId +
                        "] Downloading: " + url
        );

        String html = downloader.getsite(url);

        if (html == null || html.isBlank()) {

            System.out.println(
                    "[WORKER " + workerId + "] Empty response."
            );

            return;
        }

        if (html.startsWith("Error:")) {

            System.out.println(
                    "[WORKER " + workerId + "] " + html
            );

            return;
        }

        // Saving
        StartUI.updateWorkerStatus(
                workerId,
                "Saving: " + url
        );

        File directory = new File(storageDir);

        if (!directory.exists() && !directory.mkdirs()) {
            throw new Exception(
                    "Could not create storage directory."
            );
        }

        String pageHash = PageFiles.hashUrl(url);
        String filename = pageHash + ".html";
        File output = new File(directory, filename);

        Files.writeString(
                output.toPath(),
                html,
                StandardCharsets.UTF_8
        );

        Document document = Jsoup.parse(html);
        String metadata = "{\n" +
                "  \"url\": \"" + jsonEscape(url) + "\",\n" +
                "  \"title\": \"" + jsonEscape(document.title()) + "\"\n" +
                "}\n";
        File metadataOutput = new File(directory, pageHash + ".json");
        Files.writeString(
                metadataOutput.toPath(),
                metadata,
                StandardCharsets.UTF_8
        );

        System.out.println(
                "[WORKER " + workerId +
                        "] Saved: " + output.getAbsolutePath()
        );
        System.out.println(
                "[WORKER " + workerId +
                        "] Metadata saved: " + metadataOutput.getAbsolutePath()
        );

        // Parsing
        StartUI.updateWorkerStatus(
                workerId,
                "Parsing: " + url
        );

        String[] anchors = parser.getAnchors(html);

        System.out.println(
                "[WORKER " + workerId +
                        "] Found " + anchors.length + " links."
        );

        int found = 0;

        // Process discovered links
        for (String href : anchors) {

            try {

                if (href == null || href.isBlank()) {
                    continue;
                }

                // Resolve relative URLs
                URI resolved = URI.create(url)
                        .resolve(href.trim())
                        .normalize();

                String newUrl = resolved.toString();

                found++;

                // Add discovered URL to shared queue
                manager.enqueue(newUrl);

            } catch (Exception e) {

                System.out.println(
                        "[WORKER " + workerId +
                                "] [INVALID LINK] " + href
                );
            }
        }

        System.out.println(
                "[WORKER " + workerId +
                        "] Links found: " + found
        );

        System.out.println(
                "[WORKER " + workerId +
                        "] Page processing complete."
        );
    }

    private String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            switch (character) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }

        return escaped.toString();
    }
}
