package org.netSearch.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class GetAnchor {

    public String[] getAnchors(String html) {

        Document document = Jsoup.parse(html);

        List<String> links = new ArrayList<>();

        for (Element anchor : document.select("a[href]")) {
            links.add(anchor.attr("href"));
        }

        return links.toArray(new String[0]);
    }
}
