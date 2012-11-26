/*
 * TEIDoc.java
 *
 * (C) 2012 by Damir Cavar
 *
 * Holds the meta-information of the TEI XML document.
 * 
 * This code is part of the package to convert Project Gutenberg books to
 * TEI XML P5/Light.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Damir Cavar
 */
public class TEIDoc {

    /**
     * The ID string (number) of the book.
     */
    public String id;
    /**
     * The publisher string of the book.
     */
    public String publisher;
    /**
     * The title of the text.
     */
    public ArrayList<String> title;
    /**
     * The creator identification of the text.
     */
    public String creator;
    /**
     * The friendly title of the text.
     */
    public String friendlyTitle;
    /**
     * The ISO standard used for language codes.
     */
    public String languageISO;
    /**
     * The language code.
     */
    public String languageCode;
    /**
     * The licensing information.
     */
    public ArrayList<String> rights;
    /**
     * A list of keywords or subject phrases.
     */
    public ArrayList<String> subject;
    /**
     * Date in W3C Date and Time Format
     */
    public String createdW3CDTF;
    /**
     * Library of Congress Subject Headings
     */
    public ArrayList<String> subjectHeadingsLCC;
    /**
     * Library of Congress Classification
     */
    public String classificationLCC;
    /**
     * Project Gutenberg category
     */
    public String projGCategory;
    /**
     * DC description
     */
    public String description;
    /**
     * Contributor list
     */
    public ArrayList<String> contributors;
    /**
     * table of contents.
     */
    public String toc;
    /**
     * Project Gutenberg Number ID.
     */
    public int idN;
    /**
     * DOM representation.
     */
    public Document mydom;

    /**
     * Simple constructor.
     */
    public TEIDoc() {
        this.title = new ArrayList<>();
        this.subject = new ArrayList<>();
        this.contributors = new ArrayList<>();
        this.rights = new ArrayList<>();
        this.subjectHeadingsLCC = new ArrayList<>();
    }

    /**
     *
     * @param doc
     * @return
     */
    public String getTEIXMLString(Document doc) {
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
            // create string from xml tree
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(TEIDoc.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(TEIDoc.class.getName()).log(Level.SEVERE, null, ex);
        }
        return strw.toString();
    }

    /**
     *
     * @return Document
     * @throws ParserConfigurationException
     */
    public void genTEIXMLDom() {
        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            this.mydom = docBuilder.newDocument();

            // TEI root element
            Element root = this.mydom.createElement("TEI");
            root.setAttribute("xmlns", "http://www.tei-c.org/ns/1.0");
            this.mydom.appendChild(root);
            this.mydom.insertBefore(this.mydom.createProcessingInstruction("xml-model",
                    "href=\"http://www.tei-c.org/release/xml/tei/custom/schema/relaxng/teilite.rng\""
                    + " schematypens=\"http://relaxng.org/ns/structure/1.0\""),
                    root);

            // create teiHeader section
            Element teiHeader = this.mydom.createElement("teiHeader");
            root.appendChild(teiHeader);

            // create fileDescription section
            Element fileDesc = this.mydom.createElement("fileDesc");
            teiHeader.appendChild(fileDesc);

            // create titleStmt
            Element titleStmt = this.mydom.createElement("titleStmt");
            // check for multi-line titles
            Element ltmp;
            Element tmp = this.mydom.createElement("title");
            tmp.setAttribute("type", "full");
            int numTitles = this.title.size();
            if (numTitles > 0) {
                String tmpStr = this.title.get(numTitles - 1);
                if (tmpStr.contains("\n")) {
                    // make multiple title statements
                    String[] subStrs = tmpStr.split("\n");

                    // the first one is main
                    ltmp = this.mydom.createElement("title");
                    ltmp.setAttribute("type", "main");
                    ltmp.appendChild(this.mydom.createTextNode(subStrs[0]));
                    tmp.appendChild(ltmp);

                    // TODO
                    // all others are sub should not be... but...
                    for (int i = 1; i < subStrs.length; i++) {
                        ltmp = this.mydom.createElement("title");
                        ltmp.setAttribute("type", "sub");
                        ltmp.appendChild(this.mydom.createTextNode(subStrs[i]));
                        tmp.appendChild(ltmp);
                    }
                    // titleStmt.appendChild(tmp);
                } else { // make single title statement
                    ltmp = this.mydom.createElement("title");
                    ltmp.setAttribute("type", "main");
                    ltmp.appendChild(this.mydom.createTextNode(this.title.get(numTitles - 1)));
                    tmp.appendChild(ltmp);
                }
            }
            ltmp = this.mydom.createElement("title");
            ltmp.setAttribute("type", "alt");
            ltmp.appendChild(this.mydom.createTextNode(this.friendlyTitle));
            tmp.appendChild(ltmp);
            titleStmt.appendChild(tmp);

            // add author information
            tmp = this.mydom.createElement("author");
            if (this.creator != null) {
                // split year of from author string
                Pattern nameSplitRE = Pattern.compile("(?<name>[\\w,\\s]+)(?<year>,\\s+\\d\\d\\d\\d\\s?-+\\s?\\d\\d\\d\\d)");
                Matcher matcher = nameSplitRE.matcher(this.creator);
                if (matcher.find(0)) {
                    String tmpN = matcher.group("name");
                    //String tmpY = matcher.group("year");
                    tmp.appendChild(this.mydom.createTextNode(tmpN));
                } else {
                    tmp.appendChild(this.mydom.createTextNode(this.creator));
                }
            }
            titleStmt.appendChild(tmp);
            fileDesc.appendChild(titleStmt);

            // publicationStmt in fileDesc
            Element publicationStmt = this.mydom.createElement("publicationStmt");
            tmp = this.mydom.createElement("publisher");
            if (this.publisher != null) {
                tmp.appendChild(this.mydom.createTextNode(this.publisher));
            }
            publicationStmt.appendChild(tmp);
            // date
            tmp = this.mydom.createElement("date");
            if (this.createdW3CDTF != null) {
                tmp.appendChild(this.mydom.createTextNode(this.createdW3CDTF));
            }
            publicationStmt.appendChild(tmp);
            // availability
            tmp = this.mydom.createElement("availability");
            ltmp = this.mydom.createElement("licence");
            for (String right : this.rights) {
                Element ptmp = this.mydom.createElement("p");
                ptmp.appendChild(this.mydom.createTextNode(right));
                ltmp.appendChild(ptmp);
            }
            tmp.appendChild(ltmp);
            publicationStmt.appendChild(tmp);
            // distributor
            tmp = this.mydom.createElement("distributor");
            // tmp.appendChild(doc.createTextNode("Distributed in the TEI XML format by:"));
            Element ptmp;
            ptmp = this.mydom.createElement("name");
            ptmp.setAttribute("xml:id", "DC");
            ptmp.appendChild(this.mydom.createTextNode("Damir Cavar"));
            tmp.appendChild(ptmp);
            ptmp = this.mydom.createElement("name");
            ptmp.setAttribute("xml:id", "LTL");
            ptmp.appendChild(this.mydom.createTextNode("Language Technology Lab"));
            tmp.appendChild(ptmp);
            ptmp = this.mydom.createElement("name");
            ptmp.setAttribute("xml:id", "ILIT");
            ptmp.appendChild(this.mydom.createTextNode("Institute for Language Information and Technology"));
            tmp.appendChild(ptmp);
            ptmp = this.mydom.createElement("name");
            ptmp.setAttribute("xml:id", "EMU");
            ptmp.appendChild(this.mydom.createTextNode("Eastern Michigan University"));
            tmp.appendChild(ptmp);

            ltmp = this.mydom.createElement("address");
            // add address info
            ptmp = this.mydom.createElement("addrLine");
            ptmp.appendChild(this.mydom.createTextNode("2000 E. Huron River Dr., Suite 104"));
            ltmp.appendChild(ptmp);
            ptmp = this.mydom.createElement("addrLine");
            ptmp.appendChild(this.mydom.createTextNode("Ypsilanti, MI 48197"));
            ltmp.appendChild(ptmp);
            ptmp = this.mydom.createElement("addrLine");
            ptmp.appendChild(this.mydom.createTextNode("USA"));
            ltmp.appendChild(ptmp);
            tmp.appendChild(ltmp);
            publicationStmt.appendChild(tmp);
            // idno
            tmp = this.mydom.createElement("idno");
            if (this.id != null) {
                tmp.appendChild(this.mydom.createTextNode(Integer.toString(this.idN)));
            }
            publicationStmt.appendChild(tmp);
            fileDesc.appendChild(publicationStmt);

            // --------------------------------------------
            // sourceDesc in fileDesc
            Element sourceDesc = this.mydom.createElement("sourceDesc");
            // contains a <p> with a description of the source
            tmp = this.mydom.createElement("p");
            tmp.appendChild(this.mydom.createTextNode("This text was automatically "
                    + "converted from the corresponding HTML formated text found in the "
                    + "Project Gutenberg (http://www.gutenberg.org/) collection."));
            sourceDesc.appendChild(tmp);
            if (this.creator != null) {
                tmp = this.mydom.createElement("p");
                tmp.appendChild(this.mydom.createTextNode("Creator: " + this.creator));
                sourceDesc.appendChild(tmp);
            }
            if (this.description != null) {
                tmp = this.mydom.createElement("p");
                tmp.appendChild(this.mydom.createTextNode(this.description));
                sourceDesc.appendChild(tmp);
            }
            fileDesc.appendChild(sourceDesc);


            // --------------------------------------------
            // encodingDesc in teiHeader
            Element encodingDesc = this.mydom.createElement("encodingDesc");
            // contains a appInfo with a description of the source
            tmp = this.mydom.createElement("appInfo");
            // appInfo contains application
            ltmp = this.mydom.createElement("application");
            ltmp.setAttribute("ident", "gutenberg2tei");
            ltmp.setAttribute("version", "1.0");
            ptmp = this.mydom.createElement("desc");
            ptmp.appendChild(this.mydom.createTextNode("Conversion tool using the RDF file catalog and meta-information "
                    + "and conversion of HTML to TEI XML."));
            ltmp.appendChild(ptmp);
            tmp.appendChild(ltmp);
            encodingDesc.appendChild(tmp);
            // contains projectDesc
            tmp = this.mydom.createElement("projectDesc");
            ltmp = this.mydom.createElement("p");
            ltmp.appendChild(this.mydom.createTextNode("The conversion of the Project Gutenberg "
                    + "texts to the TEI XML format started as an independent project at ILIT, EMU."));
            tmp.appendChild(ltmp);
            encodingDesc.appendChild(tmp);
            // contains samplingDecl
            tmp = this.mydom.createElement("samplingDecl");
            ltmp = this.mydom.createElement("p");
            ltmp.appendChild(this.mydom.createTextNode(""));
            tmp.appendChild(ltmp);
            encodingDesc.appendChild(tmp);
            // classDecl
            tmp = this.mydom.createElement("classDecl");
            ltmp = this.mydom.createElement("taxonomy");
            ltmp.setAttribute("xml:id", "lcsh");
            ptmp = this.mydom.createElement("bibl");
            ptmp.appendChild(this.mydom.createTextNode("Library of Congress Subject Headings"));
            ltmp.appendChild(ptmp);
            tmp.appendChild(ltmp);
            ltmp = this.mydom.createElement("taxonomy");
            ltmp.setAttribute("xml:id", "lc");
            ptmp = this.mydom.createElement("bibl");
            ptmp.appendChild(this.mydom.createTextNode("Library of Congress Classification"));
            ltmp.appendChild(ptmp);
            tmp.appendChild(ltmp);
            ltmp = this.mydom.createElement("taxonomy");
            ltmp.setAttribute("xml:id", "pg");
            ptmp = this.mydom.createElement("bibl");
            ptmp.appendChild(this.mydom.createTextNode("Project Gutenberg Category"));
            ltmp.appendChild(ptmp);
            tmp.appendChild(ltmp);
            encodingDesc.appendChild(tmp);

            teiHeader.appendChild(encodingDesc);

            // --------------------------------------------
            // profileDesc in teiHeader
            Element profileDesc = this.mydom.createElement("profileDesc");
            // contains creation contains date
            tmp = this.mydom.createElement("creation");
            ltmp = this.mydom.createElement("date");
            ltmp.appendChild(this.mydom.createTextNode(this.createdW3CDTF));
            tmp.appendChild(ltmp);
            // add the author name here too, although not correct...
            //ltmp = doc.createElement("name");
            //ltmp.appendChild(doc.createTextNode(this.creator));
            //tmp.appendChild(ltmp);
            profileDesc.appendChild(tmp);
            // contains langUsage
            if (this.languageCode != null) {
                tmp = this.mydom.createElement("langUsage");
                ltmp = this.mydom.createElement("language");
                ltmp.setAttribute("ident", this.languageCode);
                Locale aLocale = Locale.forLanguageTag(this.languageCode);
                ltmp.appendChild(this.mydom.createTextNode(aLocale.getDisplayLanguage() + "."));
                tmp.appendChild(ltmp);
                profileDesc.appendChild(tmp);
            }
            // textClass
            tmp = this.mydom.createElement("textClass");
            ltmp = this.mydom.createElement("keywords");
            ltmp.setAttribute("scheme", "#lcsh");
            if (this.subjectHeadingsLCC.size() > 0) {
                for (String term : this.subjectHeadingsLCC) {
                    ptmp = this.mydom.createElement("term");
                    ptmp.appendChild(this.mydom.createTextNode(term));
                    ltmp.appendChild(ptmp);
                }
            } else {
                ptmp = this.mydom.createElement("term");
                // ptmp.appendChild(doc.createTextNode(term));
                ltmp.appendChild(ptmp);
            }
            tmp.appendChild(ltmp);
            if (this.classificationLCC != null) {
                ltmp = this.mydom.createElement("classCode");
                ltmp.setAttribute("scheme", "#lc");
                ltmp.appendChild(this.mydom.createTextNode(this.classificationLCC));
                tmp.appendChild(ltmp);
            }
            if (this.projGCategory != null) {
                ltmp = this.mydom.createElement("classCode");
                ltmp.setAttribute("scheme", "#pg");
                ltmp.appendChild(this.mydom.createTextNode(this.projGCategory));
                tmp.appendChild(ltmp);
            }
            profileDesc.appendChild(tmp);
            teiHeader.appendChild(profileDesc);

            // --------------------------------------------
            // revisionDesc in teiHeader
            Element revisionDesc = this.mydom.createElement("revisionDesc");
            // contains a list of change tags
            tmp = this.mydom.createElement("change");
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
            Date t = new Date();
            tmp.setAttribute("when", ft.format(t));
            tmp.setAttribute("who", "#DC");
            tmp.appendChild(this.mydom.createTextNode("Initial conversion from HTML to TEI XML."));
            revisionDesc.appendChild(tmp);
            teiHeader.appendChild(revisionDesc);
            // create text section
            // Element text = this.mydom.createElement("text");
            // !!!
            // skip that and just append the text-Element from the converted HTML-document.
            // !!!
            // root.appendChild(text);
        } catch (ParserConfigurationException e) {
            Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     *
     * @return
     */
    public String getPrettyPrint() {
        OutputStream bOut = new ByteArrayOutputStream();
        String ret = "";
        try {
            for (String tmptitle : this.title) {
                bOut.write(tmptitle.getBytes());
                bOut.write("\n".getBytes());
            }
            bOut.write(this.friendlyTitle.getBytes());
            bOut.write("\n".getBytes());
            bOut.write(this.creator.getBytes());
            bOut.write("\n".getBytes());
            bOut.write(this.publisher.getBytes());
            bOut.write("\n".getBytes());
            bOut.write(this.description.getBytes());
            bOut.write("\n".getBytes());
            ret = bOut.toString();
            bOut.close();
        } catch (IOException e) {
            Logger.getLogger(TEIDoc.class.getName()).log(Level.SEVERE, null, e);
        }
        return ret;
    }
}
