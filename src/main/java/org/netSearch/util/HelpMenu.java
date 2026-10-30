package org.netSearch.util;

public class HelpMenu {
    public String helpmenu() {
        return "netSearch Help Menu\n" +
                "Usage: <mode> <handling-dir> <starting-site>\n" +
                "<mode> - standard: Runs netSearch in standard mode\n" +
                "<mode> - exitcodelist: displays list of Exit codes and the correlating meaning\n" +
                "<mode> - help: Displays this list";
    }
}
