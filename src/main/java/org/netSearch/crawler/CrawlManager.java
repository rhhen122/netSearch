
package org.netSearch.crawler;

import java.net.URI;
import java.util.HashSet;
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
    private final int maxPages;
    private final int threadCount;

    private int outstanding = 0;
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
        if (maxPages < 1) {
            throw new IllegalArgumentException("Page cap must be a positive number.");
        }
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be a positive number.");
        }

        this.storageDir = storageDir;
        this.startingHost = URI.create(startingUrl).getHost();
        this.infiniteCrawl = infiniteCrawl;
        this.maxPages = maxPages;
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public void start(String startingUrl) {

        System.out.println("==================================");
        System.out.println("         NETSEARCH CRAWLER");
        System.out.println("==================================");
        System.out.println("[INFO] Starting URL: " + startingUrl);
        System.out.println("[INFO] Threads: " + threadCount);
        System.out.println("[INFO] Storage: " + storageDir);
        System.out.println("[INFO] Page cap: " + (infiniteCrawl ? "none" : maxPages));
        System.out.println("[INFO] Infinite mode: " + infiniteCrawl);
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

        enqueue(startingUrl);

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

        stopWorkers();

        System.out.println();
        System.out.println("==================================");
        System.out.println("[DONE] Crawl finished.");
        System.out.println("[DONE] Pages discovered: " + getVisitedCount());
        System.out.println("==================================");
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

            if (!visited.add(cleanUrl)) {
                return;
            }

            if (!infiniteCrawl && visited.size() > maxPages) {
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

    public String getNextUrl() throws InterruptedException {
        return queue.take();
    }

    public boolean isStopSignal(String url) {
        return STOP.equals(url);
    }

    public synchronized void taskFinished() {

        outstanding--;

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
