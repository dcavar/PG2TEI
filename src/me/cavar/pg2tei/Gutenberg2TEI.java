/*
 * (C) 2012 by Damir Cavar
 *
 * Download the Project Gutenberg catalog.rdf file, fetch the RDF for each
 * individual book, generate TEI XML meta-header, fetch the HTML of the book,
 * convert to TEI XML.
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
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 *
 * @author Damir Cavar
 */
public class Gutenberg2TEI {

    /**
     *
     * @param outputFolder
     * @param catalogOutFN
     */
    public static void processRDF(String outputFolder, String catalogOutFN, String ebookURLStr) {
        System.out.println("Processing catalog.rdf");
        File rdfFile = new File(outputFolder, catalogOutFN);
        RDFParser myRdfP = new RDFParser(ebookURLStr, outputFolder);
        try {
            System.out.println(rdfFile.getAbsolutePath());
            myRdfP.parseDocument(rdfFile.getAbsolutePath());
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE, null, e);
        }
        System.out.printf("Number of entries: %d\n", myRdfP.getEntryCounter());
    }

    /**
     *
     * @param catalogURLStr
     * @param outputFolder
     * @param catalogOutFN
     */
    public static void fetchRDF(String catalogURLStr, String outputFolder, String catalogOutFN) {
        Fetcher myFetcher = new Fetcher(catalogURLStr, outputFolder, catalogOutFN);
        try {
            URL catalogURL = new URL(catalogURLStr);
            File tmpFile = new File(catalogURL.getFile());
            File zipFile = new File(outputFolder, tmpFile.getName());
            File rdfFile = new File(outputFolder, catalogOutFN);

            if (!rdfFile.exists()) {
                if (!zipFile.exists()) {
                    // get the catalog.rdf.zip file
                    System.out.print("Fetching Gutenberg Catalog-file as RDF-Zip... writing to ");
                    System.out.println(zipFile);
                    myFetcher.getCatalog(catalogURL, zipFile);
                    System.out.println("Done");
                }
                // unzip the catalog.rdf.zip file to catalog.rdf
                System.out.print("Unzipping the Catalog-file... ");
                myFetcher.unZip(zipFile);
                System.out.println("Done");
            }
        } catch (IOException e) {
            System.out.println("IOError");
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // Process command line
        Options options = new Options();

        options.addOption("c", true, "Catalogue URL");
        options.addOption("o", true, "Output folder");
        // options.addOption("f", true, "Resulting output catalogue file name");
        options.addOption("h", false, "Help");

        // the individual RDF-files are at this URL:
        // The RDF-file name is this.idN + ".rdf"
        String ebookURLStr = "http://www.gutenberg.org/ebooks/";

        // the URL to the catalog.rdf
        String catalogURLStr = "http://www.gutenberg.org/feeds/catalog.rdf.zip";
        String outputFolder = ".";
        String catalogOutFN = "catalog.rdf";

        CommandLineParser parser;
        parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                System.out.println("Project Gutenberg fetch RDF catalog, HTML-files and generate TEI XML");
                System.out.println("");
                return;
            }
            if (cmd.hasOption("c")) {
                catalogURLStr = cmd.getOptionValue("c");
            }
            if (cmd.hasOption("o")) {
                outputFolder = cmd.getOptionValue("o");
            }
            //if (cmd.hasOption("f")) {
            //    catalogOutFN = cmd.getOptionValue("f");
            //}

        } catch (ParseException ex) {
            System.out.println("Command line argument error:" + ex.getMessage());
        }


        // Do the fetching of the RDF catalog
        fetchRDF(catalogURLStr, outputFolder, catalogOutFN);

        // process the RDF file
        processRDF(outputFolder, catalogOutFN, ebookURLStr);
    }
}
