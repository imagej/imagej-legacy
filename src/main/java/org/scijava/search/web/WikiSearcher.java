package org.scijava.search.web;


import org.scijava.plugin.Plugin;
import org.scijava.search.SearchResult;
import org.scijava.search.Searcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

/**
 * The Wiki search allows users to find help on http://imagej.net/
 *
 * @author Robert Haase, http://github.com/haesleinhuepf
 */
@Plugin(type = Searcher.class, name = "ImageJ Wiki")
public class WikiSearcher extends AbstractWebSearcher {

    public WikiSearcher() {
        super("ImageJ Wiki");
    }
    @Override public List<SearchResult> search(String text,
                                               boolean fuzzy)
    {
        try {
            URL url = new URL("http://imagej.net/index.php?title=Special%3ASearch&search=" + URLEncoder.encode(text) + "&go=Search&source=imagej");

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());

            parse(doc.getDocumentElement());
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return getSearchResults();
    }

    String currentHeading;
    String currentLink;

    private void parseHeading(Node node) {

        if (node.getTextContent() != null && node.getTextContent().trim().length() > 0) {
            currentHeading = node.getTextContent();
        }
        if (node.getAttributes() != null) {
            Node href = node.getAttributes().getNamedItem("href");
            if (href != null) {
                currentLink = "http://imagej.net" + href.getNodeValue();
            }
        }

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);

            parseHeading(childNode);
        }
    }

    String currentContent;

    private void parseContent(Node node) {
        if (node.getTextContent() != null) {
            currentContent = node.getTextContent();
        }

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);

            parse(childNode);
        }
    }

    private void saveLastItem() {
        if (currentHeading != null && currentHeading.length() > 0) {
            addResult(currentHeading, "", currentLink, currentContent);
        }
        currentHeading = "";
        currentLink = "";
        currentContent = "";
    }

    private void parse(Node node) {
        if (node.getNodeName().equals("div")) {
            Node item = node.getAttributes() == null ? null : node.getAttributes().getNamedItem("class");
            if (item != null) {
                System.out.println(item.getNodeValue());
            }
            if (item != null && item.getNodeValue().equals("mw-search-result-heading")) {

                if (currentHeading != null) {
                    saveLastItem();
                }
                parseHeading(node);

                return;
            }
            if (item != null && item.getNodeValue().equals("searchresult")) {
                System.out.println(node.getNodeName());
                parseContent(node);
                return;
            }
        }


        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);

            parse(childNode);
        }

    }
}
