/*
 * Fetcher.java
 *
 * (C) 2012 by Damir Cavar
 *
 * Download the Project Gutenberg catalog.rdf file.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Damir Cavar
 *
 */
public class Fetcher {

    public String catalogRDFURL;
    public String outputFolder;
    public String rdfFileName;

    /**
     * Constructor.
     */
    public Fetcher(String url, String oFolder, String catalogName) {
        this.catalogRDFURL = url;
        this.outputFolder = oFolder;
        this.rdfFileName = catalogName;
    }

    /**
     * Downloads the ZIP-compressed catalog of the Project Gutenberg archive in
     * RDF-format.
     *
     * @param url
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public void getCatalog(URL u, File outputFile)
            throws MalformedURLException, IOException {
        URLConnection uc = u.openConnection();
        String contentType = uc.getContentType();
        int contentLength = uc.getContentLength();
        if (contentType.startsWith("text/") || contentLength == -1) {
            throw new IOException("This is not a binary file.");
        }

        InputStream raw = uc.getInputStream();
        byte[] data;
        int offset;
        try (InputStream in = new BufferedInputStream(raw)) {
            data = new byte[contentLength];
            int bytesRead;
            offset = 0;
            while (offset < contentLength) {
                bytesRead = in.read(data, offset, data.length - offset);
                if (bytesRead == -1) {
                    break;
                }
                offset += bytesRead;
            }
        }

        if (offset != contentLength) {
            throw new IOException("Only read " + offset + " bytes; Expected "
                    + contentLength + " bytes");
        }

        try (FileOutputStream fop = new FileOutputStream(outputFile)) {
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            fop.write(data);
            fop.flush();
            fop.close();
        }
    }

    /**
     *
     * @param zipFile
     * @param outputFolder
     */
    public void unZip(File zipFile) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                if (!fileName.equals(this.rdfFileName)) {
                    ze = zis.getNextEntry();
                    continue;
                }
                File catFile = new File(this.outputFolder, fileName);
                try (FileOutputStream fos = new FileOutputStream(catFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                // we just want the catalog.rdf file
                break;
            }
            zis.closeEntry();
        }
    }
}
