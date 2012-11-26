/*
 * BookRDF.java
 * (C) 2012 by Damir Cavar (http://cavar.me/damir/)
 * 
 * This class holds a DOM-object with the RDF-content of an individual
 * Project Gutenberg book. It extracts the relevant meta-information from
 * the RDF and adds the URL and descriptor for the TEI XML files.
 * 
 * The resuting data structures are a RDF DOM that can be serialized as 
 * the new RDF XML with the TEI XML link, as well as internal meta-information
 * fields that can be used to extend the meta-information of the TEI XML
 * files.
 * 
 * 
 * License:
 * ========
 * 
 * Copyright 2012 Damir Cavar (http://cavar.me/damir/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package me.cavar.pg2tei;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Damir Cavar
 */
public class BookRDF {

    /**
     * The key for the HTML-URL
     */
    public static final int HTML = 0;
    /**
     * The key for the HTML-ZIP-URL
     */
    public static final int HTML_ZIP = 1;
    /**
     * The key for the EPUB-URL
     */
    public static final int EPUB = 2;
    /**
     * The key for the EPUB-URL without images
     */
    public static final int EPUB_NO_I = 3;
    /**
     * The String-representation of the URL of the RDF-file.
     */
    private URL rdfURL;
    /**
     * DOM-object holding the RDF-content
     */
    private Document mydoc;
    /**
     * Stores the URLs for the different target formats
     */
    HashMap<Integer, String[]> myHM;
    /**
     * Contains the date string, when the document was issued.
     */
    public String issued;
    /**
     * Contains the language code.
     */
    public String language;
    /**
     * Contains the publisher string.
     */
    public String publisher;
    /**
     * Contains the rights string.
     */
    public String rights;
    /**
     * Contains the title of the text.
     */
    public String title;

    /**
     *
     */
    public BookRDF(String someurl, String fileID) {
        try {
            this.rdfURL = new URL(someurl);
        } catch (MalformedURLException ex) {
            Logger.getLogger(BookRDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.myHM = new HashMap<>();
    }

    /**
     *
     * @return Document
     */
    public Document getDOM() {
        return this.mydoc;
    }

    /**
     *
     */
    public void addDocumentInfo(String xmlURL, long size, String charSet, String id) {
        // Create a fileNode with the relevant information
        Element fileNode = this.mydoc.createElement("pgterms:file");
        fileNode.setAttribute("rdf:about", xmlURL + id + "/" + id + ".xml");
        // <dcterms:extent
        Element extentNode = this.mydoc.createElement("dcterms:extent");
        extentNode.setAttribute("rdf:datatype", "http://www.w3.org/2001/XMLSchema#integer");
        extentNode.appendChild(this.mydoc.createTextNode(String.valueOf(size)));
        fileNode.appendChild(extentNode);
        // <dcterms:format>
        Element formatNode = this.mydoc.createElement("dcterms:format");
        // <rdf:Description>
        Element rdfDescriptionNode = this.mydoc.createElement("rdf:Description");
        Element dcamMemberOfNode = this.mydoc.createElement("dcam:memberOf");
        dcamMemberOfNode.setAttribute("rdf:resource", "http://purl.org/dc/terms/IMT");
        rdfDescriptionNode.appendChild(dcamMemberOfNode);
        Element rdfValueNode = this.mydoc.createElement("rdf:value");
        rdfValueNode.setAttribute("rdf:datatype", "http://purl.org/dc/terms/IMT");
        rdfValueNode.appendChild(this.mydoc.createTextNode("text/xml; charset=" + charSet));
        rdfDescriptionNode.appendChild(rdfValueNode);
        formatNode.appendChild(rdfDescriptionNode);
        fileNode.appendChild(formatNode);
        // <dcterms:isFormatOf 
        Element isFormatOfNode = this.mydoc.createElement("dcterms:isFormatOf");
        // get the resource descriptor!
        isFormatOfNode.setAttribute("rdf:resource", "ebooks/" + id);
        fileNode.appendChild(isFormatOfNode);
        Element dctermsModifiedNode = this.mydoc.createElement("dcterms:modified");
        dctermsModifiedNode.setAttribute("rdf:datatype", "http://www.w3.org/2001/XMLSchema#dateTime");
        Date now = new Date(); // 2004-09-14T19:51:46
        SimpleDateFormat dtf = new SimpleDateFormat("yyyyy-mm-dd");
        SimpleDateFormat ttf = new SimpleDateFormat("hh:mm:ss");
        dctermsModifiedNode.appendChild(this.mydoc.createTextNode(dtf.format(now) + "T" + ttf.format(now)));
        fileNode.appendChild(dctermsModifiedNode);

        // create the node:
        // <dcterms:hasFormat rdf:resource="..."/>
        Element hasFormatNode = this.mydoc.createElement("dcterms:hasFormat");
        hasFormatNode.setAttribute("rdf:resource", xmlURL + id + "/" + id + ".xml");

        // insert the nodes in the DOM
        NodeList nodes;
        nodes = this.mydoc.getElementsByTagName("dcterms:hasFormat");
        if (nodes.getLength() > 0) {
            Node e = nodes.item(0);
            e.getParentNode().insertBefore(hasFormatNode, e);
        } else {
            // find the <pgterms:ebook 
            NodeList ebookNodes = this.mydoc.getElementsByTagName("pgterms:ebook");
            if (ebookNodes.getLength() > 0) {
                Node ebookN = ebookNodes.item(0);
                Node first = ebookN.getFirstChild();
                if (first.getNodeName().equals("creator")) {
                    Node next = first.getNextSibling();
                    ebookN.insertBefore(hasFormatNode, next);
                } else {
                    ebookN.insertBefore(hasFormatNode, first);
                }
            } else {
                Element pgTermsEbookNode = this.mydoc.createElement("pgterms:ebook");
                pgTermsEbookNode.setAttribute("rdf:about", "eboks/" + id);
                pgTermsEbookNode.appendChild(hasFormatNode);
                this.mydoc.appendChild(pgTermsEbookNode);
            }
        }
        nodes = this.mydoc.getElementsByTagName("pgterms:file");
        if (nodes.getLength() > 0) {
            Node e = nodes.item(0);
            e.getParentNode().insertBefore(fileNode, e);
        } else {
            this.mydoc.appendChild(fileNode);
        }
    }

    /**
     *
     * @throws MalformedURLException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public void loadRDF() {
        if (this.rdfURL == null) {
            return;
        }

        StringBuilder strb = new StringBuilder();
        String line;

        // to circumvent issues with redirection and 403 error
        // we need to set redirects and set the request property ??
        //read the RDF from the server
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) this.rdfURL.openConnection();
            con.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            con.setReadTimeout(10000);
            con.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
            con.connect();
            BufferedReader readb = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while ((line = readb.readLine()) != null) {
                strb.append(line).append('\n');
            }
            con.disconnect();
        } catch (MalformedURLException e) {
            Logger.getLogger(BookRDF.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(BookRDF.class.getName()).log(Level.SEVERE, null, e);
        }

        // parse the XML now into a DOM
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            this.mydoc = dBuilder.parse(new InputSource(new StringReader(strb.toString())));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Logger.getLogger(BookRDF.class.getName()).log(Level.SEVERE, null, e);
        }
        if (this.mydoc != null) {
            this.mydoc.getDocumentElement().normalize();
        }
    }

    /**
     *
     */
    public void formatURLsFromDoc() {
        // get the URLs for the relevant formats
        // to be correct, one has to extract the pgterms:file attribute
        // and check whether the contained rdf:value is of type text/html
        // this hack here is faster.

        if (this.mydoc == null) {
            return;
        }
        NamespaceContext nsctx = new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                String uri;
                switch (prefix) {
                    case "rdf":
                        uri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
                        break;
                    case "cc":
                        uri = "http://web.resource.org/cc/";
                        break;
                    case "dcam":
                        uri = "http://purl.org/dc/dcam/";
                        break;
                    case "dcterms":
                        uri = "http://purl.org/dc/terms/";
                        break;
                    case "marcrel":
                        uri = "http://www.loc.gov/loc.terms/relators/";
                        break;
                    case "pgterms":
                        uri = "http://www.gutenberg.org/2009/pgterms/";
                        break;
                    default:
                        uri = null;
                        break;
                }
                return uri;
            }

            // Dummy implementation - not used!
            @Override
            public Iterator getPrefixes(String val) {
                return null;
            }

            // Dummy implemenation - not used!
            @Override
            public String getPrefix(String uri) {
                return null;
            }
        };

        NodeList nodes;
        // get the individual file-type URLs, and track file type and
        // character encoding

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(nsctx);
        XPathExpression typeEncoding;
        try {
            typeEncoding = xpath.compile(".//rdf:value/text()");
            nodes = this.mydoc.getElementsByTagName("pgterms:file");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node fileNode = nodes.item(i);
                NamedNodeMap attrs = fileNode.getAttributes();
                Node attr = attrs.getNamedItem("rdf:about");
                String fileURL;
                if (attr != null) {
                    fileURL = attr.getNodeValue();
                } else {
                    fileURL = "";
                }
                Object result = typeEncoding.evaluate(fileNode, XPathConstants.NODESET);
                NodeList valNodes = (NodeList) result;
                String encoding = "";
                String mimetype = "";
                for (int t = 0; t < valNodes.getLength(); t++) {
                    String val = valNodes.item(t).getNodeValue();
                    if (val.contains("; ")) {
                        String[] parts = val.split(";\\s+");
                        mimetype = parts[0];
                        encoding = parts[1].replace("charset=", "");
                        break;
                    } else {
                        mimetype = val.trim();
                    }
                }
                //System.out.printf("URL: %s\nMime type: %s\nEncoding: %s\n", fileURL, mimetype, encoding);

                String[] res = {fileURL, mimetype, encoding};
                if (fileURL.endsWith("-h.htm") && mimetype.equals("text/html")) {
                    this.myHM.put(BookRDF.HTML, res);
                } else if (fileURL.endsWith("-h.zip") && mimetype.equals("text/html")) {
                    this.myHM.put(BookRDF.HTML_ZIP, res);
                } else if (fileURL.endsWith("epub.images") && mimetype.equals("application/epub+zip")) {
                    this.myHM.put(BookRDF.EPUB, res);
                } else if (fileURL.endsWith("epub.noimages") && mimetype.equals("application/epub+zip")) {
                    this.myHM.put(BookRDF.EPUB_NO_I, res);
                }
            }
        } catch (XPathExpressionException ex) {
            Logger.getLogger(BookRDF.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     *
     */
    public void extractMetaInfo() {
        if (this.mydoc == null) {
            return;
        }
        NodeList nodes;
        // find:
        // dcterms:language
        nodes = this.mydoc.getElementsByTagName("dcterms:language");
        if (nodes.getLength() > 0) {
            this.language = nodes.item(0).getTextContent();
        }
        // dcterms:publisher
        nodes = this.mydoc.getElementsByTagName("dcterms:publisher");
        if (nodes.getLength() > 0) {
            this.publisher = nodes.item(0).getTextContent();
        }
        // dcterms:rights
        nodes = this.mydoc.getElementsByTagName("dcterms:rights");
        if (nodes.getLength() > 0) {
            this.rights = nodes.item(0).getTextContent();
        }
        // dcterms:title
        nodes = this.mydoc.getElementsByTagName("dcterms:title");
        if (nodes.getLength() > 0) {
            this.title = nodes.item(0).getTextContent();
        }
        // dcterms:issued
        nodes = this.mydoc.getElementsByTagName("dcterms:issued");
        if (nodes.getLength() > 0) {
            this.issued = nodes.item(0).getTextContent();
        }
    }

    /**
     * Loads the RDF, extracts specific format URLs from it, and the
     * meta-information.
     */
    public void process() {
        this.loadRDF();
        this.formatURLsFromDoc();
        this.extractMetaInfo();
    }

    /**
     * Returns the linearized XML-representation of the RDF-DOM.
     *
     * @return
     */
    public String linearize() {
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans;
        StringWriter strw = new StringWriter();
        StreamResult result = new StreamResult(strw);
        try {
            trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(this.mydoc);
            trans.transform(source, result);
        } catch (TransformerConfigurationException e) {
            Logger.getLogger(BookRDF.class.getName()).log(Level.SEVERE, null, e);
        } catch (TransformerException e) {
            Logger.getLogger(BookRDF.class.getName()).log(Level.SEVERE, null, e);
        }
        return strw.toString();
    }
}
