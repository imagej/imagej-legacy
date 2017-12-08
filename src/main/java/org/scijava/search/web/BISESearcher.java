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
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import static ij.IJ.showStatus;

/**
 * Searcher plugin for the Bio Imaging Search Engine  (http://biii.eu/search)
 *
 * @author Robert Haase, http://github.com/haesleinhuepfv
 */
@Plugin(type = Searcher.class, name = "BISE")
public class BISESearcher extends AbstractWebSearcher
{

    public BISESearcher() {
        super("BISE");
    }

    @Override public List<SearchResult> search(String text,
                                               boolean fuzzy)
    {
        try {
            //URL url = new URL("file:///c:/structure/temp/biii.eu_search2.html");
            URL url = new URL("http://biii.eu/search?search_api_fulltext=" + URLEncoder.encode(text) + "&source=imagej");

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());

            parse(doc.getDocumentElement());
            saveLastItem();


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
                currentLink = "http://biii.eu" + href.getNodeValue();
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
            if (item != null && item.getNodeValue().equals("views-field views-field-title")) {

                if (currentHeading != null) {
                    saveLastItem();
                }
                parseHeading(node);

                return;
            }
            if (item != null && item.getNodeValue().equals("views-field views-field-search-api-excerpt")) {
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
