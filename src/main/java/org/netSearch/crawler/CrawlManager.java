
package org.netSearch.crawler;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class CrawlManager {

    private static final int DEFAULT_THREAD_COUNT = 10;
    private static final int DEFAULT_MAX_PAGES = 500;
    private static final String STOP = "__NETSEARCH_STOP__";

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final Set<String> visited = new HashSet<>();

    private final ExecutorService executor;
    private final String storageDir;
    private final String startingHost;
    private final boolean infiniteCrawl;
    private final boolean haltMode;
    private final boolean resumeMode;
    private final int maxPages;
    private final int threadCount;
    private final int initialQueuedCount;

    private int outstanding = 0;
    private int claimedCount = 0;
    private int activeTasks = 0;
    private int fetchingCount = 0;
    private boolean finished = false;

    public CrawlManager(String storageDir, String startingUrl) {
        this(storageDir, startingUrl, false, DEFAULT_MAX_PAGES, DEFAULT_THREAD_COUNT);
    }

    public CrawlManager(String storageDir, String startingUrl, boolean infiniteCrawl) {
        this(storageDir, startingUrl, infiniteCrawl, DEFAULT_MAX_PAGES, DEFAULT_THREAD_COUNT);
    }

    public CrawlManager(
            String storageDir,
            String startingUrl,
            boolean infiniteCrawl,
            int maxPages
    ) {
        this(storageDir, startingUrl, infiniteCrawl, maxPages, DEFAULT_THREAD_COUNT);
    }

    public CrawlManager(
            String storageDir,
            String startingUrl,
            boolean infiniteCrawl,
            int maxPages,
            int threadCount
    ) {
        this(storageDir, startingUrl, infiniteCrawl, maxPages, threadCount, false, null);
    }

    public CrawlManager(
            String storageDir,
            String startingUrl,
            int maxPages,
            int threadCount,
            List<String> pendingUrls
    ) {
        this(storageDir, startingUrl, false, maxPages, threadCount, true, pendingUrls);
    }

    private CrawlManager(
            String storageDir,
            String startingUrl,
            boolean infiniteCrawl,
            int maxPages,
            int threadCount,
            boolean haltMode,
            List<String> pendingUrls
    ) {
        if (maxPages < 1) {
            throw new IllegalArgumentException("Page cap must be a positive number.");
        }
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be a positive number.");
        }

        this.storageDir = storageDir;
        this.startingHost = URI.create(startingUrl).getHost();
        this.infiniteCrawl = infiniteCrawl;
        this.haltMode = haltMode;
        this.resumeMode = haltMode && pendingUrls != null;
        this.maxPages = maxPages;
        this.threadCount = threadCount;
        if (pendingUrls != null) {
            for (String url : pendingUrls) {
                visited.add(url);
                queue.offer(url);
                outstanding++;
            }
        }
        this.initialQueuedCount = pendingUrls == null ? 0 : pendingUrls.size();
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public List<String> start(String startingUrl) {

        System.out.println("==================================");
        System.out.println("         NETSEARCH CRAWLER");
        System.out.println("==================================");
        System.out.println("[INFO] Starting URL: " + startingUrl);
        System.out.println("[INFO] Threads: " + threadCount);
        System.out.println("[INFO] Storage: " + storageDir);
        System.out.println("[INFO] Page cap: " + (infiniteCrawl ? "none" : maxPages));
        System.out.println("[INFO] Infinite mode: " + infiniteCrawl);
        if (haltMode) {
            System.out.println("[INFO] Halt mode: checkpointing after " + maxPages + " pages");
        }
        System.out.println();

        if (infiniteCrawl) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println();
                System.out.println("[INFO] Stop requested. Finishing active pages...");
                stopCrawl();
                stopWorkers();
            }, "netsearch-shutdown"));
        }

        for (int i = 1; i <= threadCount; i++) {
            executor.submit(new CrawlWorker(this, storageDir, i));
        }

        if (initialQueuedCount == 0) {
            enqueue(startingUrl);
        }

        synchronized (this) {
            while (!finished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        List<String> pendingUrls = haltMode ? new ArrayList<>(queue) : List.of();
        stopWorkers();

        System.out.println();
        System.out.println("==================================");
        System.out.println("[DONE] Crawl finished.");
        if (haltMode) {
            System.out.println("[DONE] Pages processed this run: " + getProcessedPageCount());
        } else {
            System.out.println("[DONE] Pages discovered: " + getVisitedCount());
        }
        System.out.println("==================================");
        return pendingUrls;
    }

    public synchronized void enqueue(String url) {

        if (finished || url == null || url.isBlank()) {
            return;
        }

        try {
            URI uri = URI.create(url).normalize();

            String scheme = uri.getScheme();

            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http") &&
                            !scheme.equalsIgnoreCase("https"))) {
                System.out.println("[SKIPPED] Unsupported URL: " + url);
                return;
            }

            String host = uri.getHost();

            if (host == null) {
                return;
            }

            // Allow all HTTP and HTTPS hosts for now
            // We can add configurable domain restrictions later.

            uri = new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            );

            String cleanUrl = uri.toString();

            if (resumeMode && hasSavedPage(cleanUrl)) {
                return;
            }

            if (!visited.add(cleanUrl)) {
                return;
            }

            if (!haltMode && !infiniteCrawl && visited.size() > maxPages) {
                visited.remove(cleanUrl);
                return;
            }

            outstanding++;
            queue.offer(cleanUrl);

            System.out.println("[QUEUED] " + cleanUrl);
            System.out.println("[QUEUE] Outstanding pages: " + outstanding);

        } catch (Exception e) {
            System.out.println("[INVALID URL] " + url);
        }
    }

    private boolean hasSavedPage(String url) throws Exception {
        Path pageFile = Path.of(storageDir, PageFiles.hashUrl(url) + ".html");
        return Files.isRegularFile(pageFile);
    }

    public String getNextUrl() throws InterruptedException {
        if (!haltMode) {
            return queue.take();
        }

        while (true) {
            synchronized (this) {
                if (finished || claimedCount >= maxPages) {
                    return STOP;
                }
                fetchingCount++;
            }

            String url = queue.poll(100, TimeUnit.MILLISECONDS);
            synchronized (this) {
                fetchingCount--;
                if (url != null) {
                    if (finished || claimedCount >= maxPages) {
                        queue.offer(url);
                        finishIfReady();
                        return STOP;
                    }
                    claimedCount++;
                    activeTasks++;
                    return url;
                }

                finishIfReady();
                if (finished || claimedCount >= maxPages) {
                    return STOP;
                }
            }
        }
    }

    public boolean isStopSignal(String url) {
        return STOP.equals(url);
    }

    public synchronized void taskFinished() {

        outstanding--;
        if (haltMode) {
            activeTasks--;
            finishIfReady();
            return;
        }

        System.out.println("[QUEUE] Outstanding pages: " + outstanding);

        if (outstanding == 0 && !finished && !infiniteCrawl) {
            finished = true;

            System.out.println("[INFO] All pages processed.");
            notifyAll();
        }
    }

    public synchronized int getVisitedCount() {
        return visited.size();
    }

    private synchronized int getProcessedPageCount() {
        return claimedCount;
    }

    private void finishIfReady() {
        if (haltMode && !finished && fetchingCount == 0 &&
                ((claimedCount >= maxPages && activeTasks == 0) ||
                        (outstanding == 0 && activeTasks == 0))) {
            finished = true;
            System.out.println("[INFO] Halt run complete.");
            notifyAll();
        }
    }

    private synchronized void stopCrawl() {
        if (!finished) {
            finished = true;
            notifyAll();
        }

        queue.clear();
        for (int i = 0; i < threadCount; i++) {
            queue.offer(STOP);
        }
    }

    private void stopWorkers() {
        stopCrawl();
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
