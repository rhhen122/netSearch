
package org.netSearch.crawler;

import org.netSearch.web.GetSite;
import org.netSearch.web.GetAnchor;
import org.netSearch.ui.StartUI;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

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

        String filename = hashUrl(url) + ".html";
        File output = new File(directory, filename);

        Files.writeString(
                output.toPath(),
                html,
                StandardCharsets.UTF_8
        );

        System.out.println(
                "[WORKER " + workerId +
                        "] Saved: " + output.getAbsolutePath()
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

    // Generate SHA-256 filename from URL
    private String hashUrl(String url) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hash = digest.digest(
                url.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder result = new StringBuilder();

        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }
}
