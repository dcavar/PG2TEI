/*
 * HTMLBook.java
 * (C) 2012 by Damir Cavar (http://cavar.me/damir/)
 * 
 * This is a class for the HTML-version of a book in the Project
 * Gutenberg archive (www.gutenberg.org). The class will read the
 * HTML-content from a URL, parse the HTML in a DOM-object, and
 * extract the <pre>-tag-content and remove the <pre>-nodes from the HTML.
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Damir Cavar
 */
public class HTMLBook {

    /**
     * The local DOM of the HTML-file
     */
    public Document mydoc;
    /**
     * The URL-string of the HTML-document
     */
    public URL docURL;
    /**
     * The individual pre-paragraphs extracted from the DOM
     */
    public ArrayList<String> preParagraphs;
    /**
     * ID number used as folder and file name.
     */
    public String idN;
    /**
     * Output folder for the resulting HTML.
     */
    private String outputFolder;
    private String encoding;

    /**
     * Constructor.
     */
    public HTMLBook(String someurl, String outputFolder, int id, String encoding) {
        this.preParagraphs = new ArrayList<>();
        this.outputFolder = outputFolder;
        if (encoding.length() > 0) {
            this.encoding = encoding;
        }
        try {
            this.docURL = new URL(someurl);
        } catch (MalformedURLException ex) {
            Logger.getLogger(HTMLBook.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.idN = Integer.toString(id);
    }

    /**
     * Create the DOM from the HTML and extract the
     * <pre>-texts.
     */
    //public void process() {
    //    this.loadHTML();
    //}
    /**
     *
     * @throws MalformedURLException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public void loadHTML() {
        if (this.docURL == null) {
            return;
        }
        StringBuilder strb = new StringBuilder();
        String line;

        // to circumvent issues with redirection and 403 error
        // we need to set redirects and set the request property ??
        //read the RDF from the server
        try {
            HttpURLConnection con = (HttpURLConnection) this.docURL.openConnection();
            con.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            con.setReadTimeout(10000);
            con.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
            con.connect();
            BufferedReader readb;
            if (this.encoding != null) {
                readb = new BufferedReader(new InputStreamReader(con.getInputStream(), this.encoding));
            } else {
                readb = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }
            while ((line = readb.readLine()) != null) {
                strb.append(line).append('\n');
            }
            con.disconnect();
        } catch (MalformedURLException e) {
            Logger.getLogger(HTMLBook.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(HTMLBook.class.getName()).log(Level.SEVERE, null, e);
        }
        String clean = strb.toString();

        strb = new StringBuilder();
        // reluctant quantifier .*?
        Pattern p = Pattern.compile("(?<left><pre>)(?<content>.*?)(?<right></pre>)",
                Pattern.DOTALL
                | Pattern.CASE_INSENSITIVE
                | Pattern.MULTILINE
                | Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = p.matcher(clean);
        // Check all occurance
        int pos = 0;
        while (matcher.find(pos)) {
            strb.append(clean.substring(pos, matcher.start()));
            strb.append("\n");
            pos = matcher.end();
            for (String par : matcher.group("content").trim().split("\n\n")) {
                String tmp = par.trim();
                if (tmp.length() > 0) {
                    this.preParagraphs.add(tmp);
                }
            }
        }
        // append the rest of the HTML to buffer
        strb.append(clean.substring(pos));
        // get the new clean
        clean = strb.toString();
        // send buffer to Nirvana (do we have to do that?)
        strb = null;

        // create the outputPath for the HTML file
        String outputPath = this.outputFolder + File.separator + this.idN + File.separator;
        // create paths
        boolean status;
        status = new File(outputPath).mkdirs();

        // store HTML
        System.out.println("Saving HTML-file: " + outputPath + this.idN + ".html");
        File file = new File(outputPath + this.idN + ".html");
        try {
            FileOutputStream fop = new FileOutputStream(file);
            Writer out;
            if (this.encoding != null) {
                out = new OutputStreamWriter(fop, this.encoding);
            } else {
                out = new OutputStreamWriter(fop);
            }
            out.write(clean);
            out.close();
        } catch (IOException e) {
            Logger.getLogger(HTMLBook.class.getName()).log(Level.SEVERE, null, e);
        }
        System.out.println("Done");

        // convert HTML to ODT
        try {
            String fullPath = this.outputFolder + File.separator + this.idN
                    + File.separator + this.idN;
            // one should be able to use soffice (from the OpenOffice or LibreOffice
            // distro. For some reason LibreOffice does not want to do the job.
            // tried on a Mac:
            // /Applications/LibreOffice.app/Contents/MacOS/soffice --invisible --convert-to odt --outdir 10513 10513/10513.html
            // pandoc wants the complete archive, the HTML-file and all the images
            // and other parts that go with it, which is too much to add here now.
            // Instead:
            // on Mac OS X use textutil on the command line
            // (it is in the system's default path, no need for full path)
            Process proc = Runtime.getRuntime().exec("textutil -convert odt -baseurl '' -output "
                    + fullPath + ".odt "
                    + fullPath + ".html ");
            // Why ODT?
            // I tried all kinds of other formats, there was always some problem.
            // Direct HTML to TEI XML did not work, for various reasons.
            // Via ODT it basically works, i.e. I get what I want.
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            Logger.getLogger(HTMLBook.class.getName()).log(Level.SEVERE, null, e);
        }

        // convert ODT to TEI XML
        // this gets a little bit complicated:
        // One needs the TEI folder from SourceForge: http://sourceforge.net/projects/tei/
        // In the Stylesheets folder is the script odttotei
        // I had to set up some paths in odttotei, check it out yourself, in
        // particular the path to Saxon.
        // Set the paths below to point to your TEI-folder and Saxon-Jar.
        try {
            // String line;
            Process proc = Runtime.getRuntime().exec("/usr/local/share/TEI/Stylesheets/odttotei "
                    + "--apphome=/usr/local/share/TEI/Stylesheets "
                    + "--profiledir=/usr/local/share/TEI/Stylesheets/profiles "
                    + "--oxygenlib=/Applications/oxygen/lib "
                    + "--saxonjar=/usr/local/share/saxon/saxon9he.jar "
                    + this.outputFolder + File.separator + this.idN + File.separator + this.idN + ".odt ");
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            Logger.getLogger(HTMLBook.class.getName()).log(Level.SEVERE, null, e);
        }

        // parse the XML now into a DOM
        try {
            DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
            dbFac.setValidating(false);
            dbFac.setNamespaceAware(true);
            dbFac.setIgnoringComments(false);
            dbFac.setIgnoringElementContentWhitespace(false);
            dbFac.setExpandEntityReferences(false);
            DocumentBuilder dBuilder = dbFac.newDocumentBuilder();
            this.mydoc = dBuilder.parse(this.outputFolder + File.separator + this.idN + File.separator + this.idN + ".xml");
            if (this.mydoc != null) {
                this.mydoc.getDocumentElement().normalize();
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Logger.getLogger(HTMLBook.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Returns the linearized XML-representation of the RDF-DOM.
     *
     * @return String
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
            // create string from xml tree
            DOMSource source = new DOMSource(this.mydoc);
            trans.transform(source, result);
        } catch (TransformerConfigurationException e) {
            Logger.getLogger(TEIDoc.class.getName()).log(Level.SEVERE, null, e);
        } catch (TransformerException e) {
            Logger.getLogger(TEIDoc.class.getName()).log(Level.SEVERE, null, e);
        }
        return strw.toString();
    }
}
