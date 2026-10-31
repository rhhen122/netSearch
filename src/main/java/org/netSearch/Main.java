package org.netSearch;

import org.netSearch.util.*;
import org.netSearch.web.*;
import org.netSearch.System.*;
import org.netSearch.ui.*;
import org.netSearch.crawler.*;

import java.io.File;
import java.util.Objects;

public class Main {
    public static void main(String[] args) {

        HelpMenu nsHelpMenu = new HelpMenu();
        Greet nsGreet = new Greet();
        GetSite nsGetSite = new GetSite();
        netError nsError = new netError();
        LoadChecker nsLoadChecker = new LoadChecker();
        ExitCodeList nsExitLoadList = new ExitCodeList();
        StartUI nsStartUI = new StartUI();

        // Check Flags
        boolean loadStartUI = true;
        boolean infiniteCrawl = false;
        int maxPages = 500;
        int threadCount = 10;
        boolean loadChecker = true;
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            switch (argument) {
                case "-noloadcheck":
                    loadChecker = false;
                    break;
                case "-nogui":
                    loadStartUI = false;
                    break;
                case "-inf":
                    infiniteCrawl = true;
                    break;
                default:
                    if (argument.startsWith("-cap=")) {
                        try {
                            maxPages = Integer.parseInt(argument.substring(5));
                            if (maxPages < 1) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e) {
                            nsError.error(
                                    "Invalid page cap '" + argument.substring(5) +
                                            "'. Provide a positive whole number.",
                                    1
                            );
                            return;
                        }
                    }
                    if (argument.startsWith("-threads=")) {
                        try {
                            threadCount = Integer.parseInt(argument.substring(9));
                            if (threadCount < 1) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e) {
                            nsError.error(
                                    "Invalid thread count '" + argument.substring(9) +
                                            "'. Provide a positive whole number.",
                                    1
                            );
                            return;
                        }
                    }
            }
        }

        // Runs a system load check to ensure the system is capable of running the program
        if (loadChecker) {
            nsLoadChecker.LoadChecker();
        }

        // Checks for args and if none displays HelpMenu
        if (args.length == 0 || Objects.equals(args[0], "help")) {
            System.out.println(nsHelpMenu.helpmenu());
            return;
        }

        if (args.length < 3) {
            System.out.println(nsHelpMenu.helpmenu());
            nsError.error("Args not provided properly", 1);
            return;
        }

        String mode = args[0];
        String handlingDir = args[1];
        String startSite = args[2];

        // Standard mode
        if (Objects.equals(mode, "standard")) {
            nsGreet.greet("Starting UI");

            if (loadStartUI) {
                nsStartUI.startui(mode, handlingDir, startSite, threadCount);
            }

            CrawlManager crawler = new CrawlManager(
                    handlingDir,
                    startSite,
                    infiniteCrawl,
                    maxPages,
                    threadCount
            );
            crawler.start(startSite);

        } else if (Objects.equals(mode, "exitcodelist")) {
            System.out.println(nsExitLoadList.exitcodelist());
        }

        // Welcome message
        nsGreet.greet("Welcome!");
    }
}
