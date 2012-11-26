/*
 * RDFParser.java
 * 
 * (C) 2012 by Damir Cavar
 * 
 * Parses the raw RDF XML and collects the meta-data.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Damir Cavar
 */
public class RDFParser extends DefaultHandler {

    /**
     * A char buffer for the parser.
     */
    private StringBuffer charBuf;
    /**
     * The TEI document meta-information class.
     */
    private TEIDoc curTEIDoc;
    /**
     * The number of RDF-catalog entries.
     */
    private int entryCounter;
    /**
     * Flag for: within a language tag in the RDF structure. Default false
     */
    private boolean languageTag;
    /**
     * Flag for: within a subject tag.
     */
    private boolean subjectTag;
    private boolean subjectLCCTag;
    private boolean projGCategory;
    private boolean dcCreated;
    private boolean entryFound;
    String ebookURLStr;
    String outputFolder;

    /**
     * Constructor.
     */
    public RDFParser(String ebookURLStr, String outputFolder) {
        this.ebookURLStr = ebookURLStr;
        this.outputFolder = outputFolder;
        this.charBuf = new StringBuffer();
        this.entryCounter = 0;
        this.languageTag = false;
        this.subjectTag = false;
        this.subjectLCCTag = false;
        this.projGCategory = false;
        this.dcCreated = false;
        this.entryFound = false;
    }

    /**
     *
     * @return
     */
    public int getEntryCounter() {
        return (this.entryCounter);
    }

    /**
     *
     * @param fname
     * @throws ParserConfigurationException
     * @throws SAXNotRecognizedException
     * @throws SAXNotSupportedException
     */
    public void parseDocument(String fname) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        // we have to switch of SECURE_PROCESSING because of the limits
        // we have more than 64,000 replacements of entities etc.
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        try {
            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();
            //parse the file and also register this class for call backs
            sp.parse(fname, this);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            Logger.getLogger(RDFParser.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param attributes
     * @throws SAXException
     */
    //Event Handlers
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (this.charBuf.length() > 0) {
            this.charBuf.delete(0, this.charBuf.length());
        }
        if (qName.equalsIgnoreCase("pgterms:etext")) {
            this.entryFound = true;
            this.entryCounter += 1;
            this.curTEIDoc = new TEIDoc();
            for (int i = 0; i < attributes.getLength(); i++) {
                String name = attributes.getQName(i);
                String type = attributes.getType(i);
                String val = attributes.getValue(i);
                if (name.equalsIgnoreCase("rdf:ID")) {
                    this.curTEIDoc.id = val;
                    // get the numerical doc ID out
                    Pattern tokenRE = Pattern.compile("\\d+$");
                    Matcher matcher = tokenRE.matcher(val);
                    int pos = 0;
                    if (matcher.find(pos)) {
                        String tmp = matcher.group();
                        System.out.printf("Num ID: %s\n", tmp);
                        this.curTEIDoc.idN = Integer.parseInt(tmp);
                    }
                }
            }
            return;
        }
        if (!this.entryFound) {
            return;
        }
        if (qName.equalsIgnoreCase("dc:language")) {
            this.languageTag = true;
        } else if (qName.equalsIgnoreCase("dcterms:W3CDTF")) {
            this.dcCreated = true;
        } else if (qName.equalsIgnoreCase("dcterms:ISO639-2")) {
            if (this.languageTag) {
                this.curTEIDoc.languageISO = "ISO639-2";
            }
        } else if (qName.equalsIgnoreCase("dcterms:LCSH")) {
            this.subjectTag = true;
        } else if (qName.equalsIgnoreCase("dcterms:LCC")) {
            this.subjectLCCTag = true;
        } else if (qName.equalsIgnoreCase("dc:type")) {
            this.projGCategory = true;
        } else if (qName.equalsIgnoreCase("dc:rights")) {
            for (int i = 0; i < attributes.getLength(); i++) {
                String name = attributes.getQName(i);
                String type = attributes.getType(i);
                String val = attributes.getValue(i);
                if (name.equalsIgnoreCase("rdf:resource")) {
                    this.curTEIDoc.rights.add(val);
                    // get the numerical doc ID out
                    //Pattern tokenRE = Pattern.compile("\\d+$");
                    //Matcher matcher = tokenRE.matcher(val);
                    //int pos = 0;
                    //if (matcher.find(pos)) {
                    //    String tmp = matcher.group();
                    //    System.out.printf("Num ID: %s\n", tmp);
                    //    this.curTEIDoc.idN = Integer.parseInt(tmp);
                    //}
                }
            }
        }
    }

    /**
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.charBuf.append(ch, start, length);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        if (!this.entryFound) {
            return;
        }
        if (qName.equalsIgnoreCase("pgterms:etext")) {
            this.entryFound = false;
            // fetch file-specific RDF
            String fileID = Integer.toString(this.curTEIDoc.idN);
            // Create the XML-file
            // TODO this needs to be determined!
            // if files exist, skip...
            File rfile = new File(this.outputFolder + File.separator + fileID + File.separator + fileID + ".rdf");
            File xfile = new File(this.outputFolder + File.separator + fileID + File.separator + fileID + ".xml");
            if (rfile.exists() && xfile.exists()) {
                return;
            }
            
            BookRDF myBookRDF = new BookRDF(this.ebookURLStr + fileID + ".rdf", fileID);
            myBookRDF.process();
            // copy over the meta info
            if (myBookRDF.issued != null) {
            }
            if (myBookRDF.language != null) {
                if (this.curTEIDoc.languageCode == null) {
                    this.curTEIDoc.languageCode = myBookRDF.language;
                }
            }
            if (myBookRDF.publisher != null) {
                if (this.curTEIDoc.publisher == null) {
                    this.curTEIDoc.publisher = myBookRDF.publisher;
                }
            }
            if (myBookRDF.rights != null) {
                this.curTEIDoc.rights.add(myBookRDF.rights);
            }
            if (myBookRDF.title != null) {
                this.curTEIDoc.title.add(myBookRDF.title);
            }

            HTMLBook myHTMLBook;
            // get the HTML-file
            if (myBookRDF.myHM.containsKey(BookRDF.HTML)) {
                String[] val = (String[]) myBookRDF.myHM.get(BookRDF.HTML);
                
                myHTMLBook = new HTMLBook(val[0], this.outputFolder, this.curTEIDoc.idN, val[2]);
                myHTMLBook.loadHTML();
                // if there are pre-paragraphs, append them to license in
                if (myHTMLBook.preParagraphs.size() > 0) {
                    for (String right : myHTMLBook.preParagraphs) {
                        this.curTEIDoc.rights.add(right);
                    }
                }
                // the TEIDoc.
            } else if (myBookRDF.myHM.containsKey(BookRDF.HTML_ZIP)) {
                // fix this TODO
                return;
            } else if (myBookRDF.myHM.containsKey(BookRDF.EPUB)) {
                // fix this TODO
                return;
            } else if (myBookRDF.myHM.containsKey(BookRDF.EPUB_NO_I)) {
                // fix this TODO
                return;
            } else {
                // no appropriate file, nothing to convert
                return;
            }
            // did not work, there is no DOM, no time for a cleaner solution
            if (myHTMLBook.mydoc == null) {
                return;
            }

            // convert meta-information to TEI XML DOM
            this.curTEIDoc.genTEIXMLDom();
            if (this.curTEIDoc.mydom == null) {
                return;
            }

            if (this.curTEIDoc.mydom != null) {
                // get the <text>-Element and move it over to 
                NodeList nodes = myHTMLBook.mydoc.getElementsByTagName("text");
                if (nodes.getLength() == 1) {
                    Node text = nodes.item(0);
                    // import node
                    this.curTEIDoc.mydom.getDocumentElement().appendChild(this.curTEIDoc.mydom.importNode(text, true));
                }
            }
            // serialize the XML
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
                DOMSource source = new DOMSource(this.curTEIDoc.mydom);
                trans.transform(source, result);
            } catch (TransformerConfigurationException e) {
                Logger.getLogger(RDFParser.class.getName()).log(Level.SEVERE, null, e);
            } catch (TransformerException e) {
                Logger.getLogger(RDFParser.class.getName()).log(Level.SEVERE, null, e);
            }

            // store TEI XML
            String clean = strw.toString();
            strw = null;
            // first clean those refs, there might be a better version, but...
            // file:///usr/local/share/ProcessProjectGutenberg/39180/
            clean = clean.replaceAll("file:///.+/" + fileID + "/", "");
            String xmlOutFN = this.outputFolder + File.separator
                    + fileID + File.separator
                    + fileID + ".xml";
            File file;
            System.out.println("Saving TEI XML: " + xmlOutFN);
            file = new File(xmlOutFN);
            try (FileOutputStream fop = new FileOutputStream(file)) {
                if (!file.exists()) {
                    file.createNewFile();
                }
                fop.write(clean.getBytes());
                fop.flush();
                fop.close();
            } catch (IOException e) {
                Logger.getLogger(RDFParser.class.getName()).log(Level.SEVERE, null, e);
            }
            System.out.println("Done");

            // determine size and file URL and call
            file = new File(xmlOutFN);
            // myBookRDF.addDocumentInfo to generate the new RDF DOM
            // store the RDF DOM next to the TEI XML file
            myBookRDF.addDocumentInfo("http://ltl.emich.edu/gutenberg/", file.length(), "utf-8", fileID);

            // Linearize the BookRDF
            String rdfFN = this.outputFolder + File.separator
                    + fileID + File.separator
                    + fileID + ".rdf";
            System.out.println("Saving RDF for file: " + rdfFN);
            file = new File(rdfFN);
            try (FileOutputStream fop = new FileOutputStream(file)) {
                if (!file.exists()) {
                    file.createNewFile();
                }
                fop.write(myBookRDF.linearize().getBytes());
                fop.flush();
                fop.close();
            } catch (IOException e) {
                Logger.getLogger(RDFParser.class.getName()).log(Level.SEVERE, null, e);
            }
            System.out.println("Done");
        } else if (qName.equalsIgnoreCase("dc:publisher")) {
            this.curTEIDoc.publisher = this.charBuf.toString();
        } else if (qName.equalsIgnoreCase("dc:title")) {
            this.curTEIDoc.title.add(this.charBuf.toString());
        } else if (qName.equalsIgnoreCase("dc:creator")) {
            this.curTEIDoc.creator = this.charBuf.toString();
        } else if (qName.equalsIgnoreCase("dcterms:W3CDTF")) {
            this.dcCreated = false;
        } else if (qName.equalsIgnoreCase("dc:language")) {
            this.languageTag = false;
        } else if (qName.equalsIgnoreCase("pgterms:friendlytitle")) {
            this.curTEIDoc.friendlyTitle = this.charBuf.toString();
        } else if (qName.equalsIgnoreCase("rdf:value")) {
            if (this.languageTag) {
                this.curTEIDoc.languageCode = this.charBuf.toString();
            } else if (this.subjectTag) {
                this.curTEIDoc.subjectHeadingsLCC.add(this.charBuf.toString());
            } else if (this.subjectLCCTag) {
                this.curTEIDoc.classificationLCC = this.charBuf.toString();
            } else if (this.projGCategory) {
                this.curTEIDoc.projGCategory = this.charBuf.toString();
            } else if (this.dcCreated) {
                this.curTEIDoc.createdW3CDTF = this.charBuf.toString();
            }
        } else if (qName.equalsIgnoreCase("dcterms:LCSH")) {
            this.subjectTag = false;
        } else if (qName.equalsIgnoreCase("dcterms:LCC")) {
            this.subjectLCCTag = false;
        } else if (qName.equalsIgnoreCase("dc:tableOfContents")) {
            this.curTEIDoc.toc = this.charBuf.toString();
        } else if (qName.equalsIgnoreCase("dc:type")) {
            this.projGCategory = false;
        } else if (qName.equalsIgnoreCase("dc:description")) {
            this.curTEIDoc.description = this.charBuf.toString();
        }
    }
}
