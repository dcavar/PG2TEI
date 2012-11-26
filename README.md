# PG2TEI

(C) 2012 by [Damir Cavar][] and [Malgosia Cavar][]


## [Project Gutenberg][] books to [TEI XML][] conversion

This repository contains code for the automatic conversion of
[Project Gutenberg][] books to the [TEI XML][] format. We focus on the generation of valid
[TEI Lite P5 XML](http://www.tei-c.org/Guidelines/Customization/Lite/) from HTML-sources.
The code is written in Java. It might use some Java 7 specific elements and
constructions, but should be easily adaptable to Java 6 or earlier versions.

You might need the following components to convert the [Project Gutenberg][] files
yourself:

* [Java SE 7][]
* The [TEI Subversion repository on SourceForge](http://tei.sourceforge.net)
* Some document conversion tool


### The TEI Subversion repository

For the conversion of [ODT][]-documents to TEI XML we make
use of XSLT-scripts that are part of the [TEI@Sourceforge][]
package.  You can check out a local copy of the trunk using the following command:

	svn co https://tei.svn.sourceforge.net/svnroot/tei/trunk ./TEI

For further details, see the links to the repository here:

* [TEI@Sourceforge][]

The necessary components will be in the _Stylesheets_ subfolder.  In particular relevant
is the _odttotei_ script.  You might have to change certain paths in the script, or
provide appropriate command line parameters when invoking it.

In addition to the XSLT-scripts in the TEI folder you will most likely need
[Saxon][].  If you use [oXygen][], you should provide the path to the
[oXygen][]-lib-folder via command line to _odttotei_ or directly in your adjusted script
(e.g. a version of _odttotei_).


### Document conversion tools

We make use of the [textutil][] tool that is distributed with the recent versions
of Mac OS X. [textutil][] makes batch conversion of different document types easy.
We use [textutil][] to convert the Project Gutenberg HTML-files to [ODT][].

You might want to try alternative conversion strategies, for example using:

* [pandoc][], a universal document converter that is available for all major platforms.
* [OpenOffice][] or [LibreOffice][] via command line for batch processing. You will find
a lot of descriptions of the command line usage online, see
[for example here](http://maketecheasier.com/batch-convert-documents-at-the-command-line/2011/09/16)...


### Configuration of the Java code

Since the PG2TEI-code is a quick and _dirty_ implementation of the conversion pipeline, with
a very defensive coding strategy, avoiding complications that might improve the
stability, but would cost coding time, there are some things in the code that need
specific adaptation.  You might experience crashes and error messages for individual
files.  We cannot avoid that. The conversion runs quite stable, and restarting the
converter skips already available target files.

Follow the instructions in these documents to set up the conversion process for your
specific environment:

* [Description](Description.md)
* [Adaptation](Adaptation.md)

If you discover serious bugs or problems with the code, please send us a message. Thanks!


### License

The code is made available under the
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
