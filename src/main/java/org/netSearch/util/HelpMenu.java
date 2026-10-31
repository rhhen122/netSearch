package org.netSearch.util;

public class HelpMenu {
    public String helpmenu() {
        return "netSearch Help Menu\n" +
                "Usage: <mode> <handling-dir> <starting-site> [-cap=<number>] [-threads=<number>] [-inf] [-nogui]\n" +
                "<mode> - standard: Runs netSearch in standard mode\n" +
                "<mode> - exitcodelist: displays list of Exit codes and the correlating meaning\n" +
                "<mode> - help: Displays this list\n" +
                "-cap=<number>: Set the maximum number of pages to crawl (default: 500)\n" +
                "-threads=<number>: Set the number of crawler workers (default: 10)\n" +
                "-inf: Crawl without a page limit until manually stopped (Ctrl+C); overrides -cap\n" +
                "-nogui: Run without the UI";
    }
}
