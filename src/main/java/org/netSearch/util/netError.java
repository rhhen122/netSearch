package org.netSearch.util;

public class netError {
    public void error(String msg, int code) {
        System.out.println("netSearch Error: " + msg);
        System.out.println("Exiting with Code: " + code);
        System.exit(code);
    }
}
