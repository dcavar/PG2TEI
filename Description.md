# PG2TEI Description

(C) 2012 by [Damir Cavar][] and [Malgosia Cavar][]

The conversion process was presented at the:

* [TEI 2012 Annual Conference and Members Meeting](http://idhmc.tamu.edu/teiconference/)
at Texas A&M in November 2012

and at the 

* [Chicago Colloquium on Digital Humanities and Computer Science](http://chicagocolloquium.org) at
The University of Chicago in November 2012.


The PG2TEI-conversion pipeline is organized as follows:

* Downloading the _catalog.rdf_ from the Project Gutenberg web server.
* Parsing the RDF-catalog, selecting the texts that are available in HTML-format (or
some of the eBook-formats), and extracting the specific meta-information from the
RDF-XML.
* Downloading and parsing the RDF-XML-file for each individual book, extracting
the available meta-information.
* Generation of the TEI XML header with the extracted meta information.
* Conversion of the HTML-text to the ODT-format, filtering out the copyright statements
in the text body and moving them to the TEI XML header section.
* Conversion of the ODT-files to TEI XML.
* Generation of a new text-specific RDF-XML-file with the additional descriptor and link
to the newly generated TEI-XML-file.


## Downloading the Project Gutenberg RDF-XML-Catalog


## Parsing the RDF-XML-Catalog


## Parsing the RDF-XML for each individual book


## Generating the TEI XML Header 


## Converting HTML-Files to ODT


## Converting ODT-Files to TEI XML


## Merging Meta-Information and Text-Body to a New TEI XML


## Generating New RDF-XML-Files


## Resulting Resources

* Collection of the TEI XML and ODT books, and the corresponding RDF-files: URL
* Online corpus frontend based on Philologic: [LTL Philologic](http://ltl.emich.edu/philologic/)


### License

The code for the conversion pipeline is made available under the
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html) as is.
See [LICENSE.md](LICENSE.md).


[Damir Cavar]: http://cavar.me/damir/
[Java SE 7]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[LibreOffice]: http://www.libreoffice.org
[Malgosia Cavar]: http://cavar.me/malgosia/
[ODT]: http://opendocumentformat.org
[OpenOffice]: http://www.openoffice.org
[oXygen]: http://www.oxygenxml.com
[pandoc]: http://johnmacfarlane.net/pandoc/
[Project Gutenberg]: http://www.gutenberg.org/
[Saxon]: http://saxon.sourceforge.net
[TEI XML]: http://www.tei-c.org/
[TEI@Sourceforge]: http://tei.sourceforge.net
[textutil]: http://developer.apple.com/library/mac/#documentation/Darwin/Reference/ManPages/man1/textutil.1.html

