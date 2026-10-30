package org.netSearch.util;

public class HelpMenu {
    public String helpmenu() {
        return "netSearch Help Menu\n" +
                "Usage: <mode> <handling-dir> <starting-site> [-inf] [-nogui]\n" +
                "<mode> - standard: Runs netSearch in standard mode\n" +
                "<mode> - exitcodelist: displays list of Exit codes and the correlating meaning\n" +
                "<mode> - help: Displays this list\n" +
                "-inf: Crawl without the 500-page limit until manually stopped (Ctrl+C)\n" +
                "-nogui: Run without the UI";
    }
}
