package org.netSearch.crawler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

public class HaltStateStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int STATE_VERSION = 1;

    private final Path stateFile;

    public HaltStateStore(Path stateFile) {
        this.stateFile = stateFile;
    }

    public State load() throws IOException {
        if (!Files.isRegularFile(stateFile)) {
            throw new IOException("No halt checkpoint found at " + stateFile.toAbsolutePath());
        }

        try {
            State state = GSON.fromJson(Files.readString(stateFile), State.class);
            validate(state);
            return state;
        } catch (JsonParseException | IllegalArgumentException e) {
            throw new IOException("Invalid halt checkpoint: " + e.getMessage(), e);
        }
    }

    public void save(
            String storageDir,
            String startingUrl,
            int maxPages,
            int threadCount,
            boolean loadStartUI,
            boolean loadChecker,
            List<String> pendingUrls,
            Set<String> visitedUrls
    ) throws IOException {
        State state = new State(
                STATE_VERSION,
                storageDir,
                startingUrl,
                maxPages,
                threadCount,
                loadStartUI,
                loadChecker,
                pendingUrls,
                visitedUrls
        );
        validate(state);

        Path parent = stateFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path tempFile = Files.createTempFile(parent, "netsearch-halt-", ".tmp");
        try {
            Files.writeString(tempFile, GSON.toJson(state));
            try {
                Files.move(tempFile, stateFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, stateFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void delete() throws IOException {
        Files.deleteIfExists(stateFile);
    }

    private static void validate(State state) {
        if (state == null || state.version != STATE_VERSION) {
            throw new IllegalArgumentException("Unsupported or missing checkpoint version.");
        }
        if (state.storageDir == null || state.storageDir.isBlank() ||
                state.startingUrl == null || state.startingUrl.isBlank()) {
            throw new IllegalArgumentException("Checkpoint is missing crawl settings.");
        }
        if (state.maxPages < 1 || state.threadCount < 1) {
            throw new IllegalArgumentException("Checkpoint has invalid page or thread limits.");
        }
        if (state.pendingUrls == null || state.pendingUrls.isEmpty() || state.visitedUrls == null) {
            throw new IllegalArgumentException("Checkpoint is missing its URL queue.");
        }
        for (String url : state.pendingUrls) {
            if (url == null || url.isBlank() || !state.visitedUrls.contains(url)) {
                throw new IllegalArgumentException("Checkpoint contains an invalid queued URL.");
            }
        }
    }

    public static class State {
        private int version;
        private String storageDir;
        private String startingUrl;
        private int maxPages;
        private int threadCount;
        private boolean loadStartUI;
        private boolean loadChecker;
        private List<String> pendingUrls;
        private Set<String> visitedUrls;

        private State() {
        }

        private State(
                int version,
                String storageDir,
                String startingUrl,
                int maxPages,
                int threadCount,
                boolean loadStartUI,
                boolean loadChecker,
                List<String> pendingUrls,
                Set<String> visitedUrls
        ) {
            this.version = version;
            this.storageDir = storageDir;
            this.startingUrl = startingUrl;
            this.maxPages = maxPages;
            this.threadCount = threadCount;
            this.loadStartUI = loadStartUI;
            this.loadChecker = loadChecker;
            this.pendingUrls = pendingUrls;
            this.visitedUrls = visitedUrls;
        }

        public String getStorageDir() {
            return storageDir;
        }

        public String getStartingUrl() {
            return startingUrl;
        }

        public int getMaxPages() {
            return maxPages;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public boolean isLoadStartUI() {
            return loadStartUI;
        }

        public boolean isLoadChecker() {
            return loadChecker;
        }

        public List<String> getPendingUrls() {
            return pendingUrls;
        }

        public Set<String> getVisitedUrls() {
            return visitedUrls;
        }
    }
}
