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

        // Run load checker so the system doesn't crash
        nsLoadChecker.LoadChecker();

        // Check Flags
        boolean loadStartUI = true;
        boolean infiniteCrawl = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-nogui":
                    loadStartUI = false;
                    break;
                case "-inf":
                    infiniteCrawl = true;
                    break;
            }
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
                nsStartUI.startui(mode, handlingDir, startSite);
            }

            CrawlManager crawler = new CrawlManager(
                    handlingDir,
                    startSite,
                    infiniteCrawl
            );
            crawler.start(startSite);

        } else if (Objects.equals(mode, "exitcodelist")) {
            System.out.println(nsExitLoadList.exitcodelist());
        }

        // Welcome message
        nsGreet.greet("Welcome!");
    }
}
