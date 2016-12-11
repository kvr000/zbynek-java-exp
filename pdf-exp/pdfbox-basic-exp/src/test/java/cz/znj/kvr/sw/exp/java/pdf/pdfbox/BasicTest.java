package cz.znj.kvr.sw.exp.java.pdf.pdfbox;

import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.text.PDFTextStripper;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * @author
 * 	Zbyněk Vyškovský
 */
@Log4j2
@Test(groups =  "unit")
public class BasicTest
{
	@Test
	public void testParse()
	{
		try (InputStream documentStream = getClass().getResourceAsStream("test-table.pdf");
		     PDDocument document = PDDocument.load(documentStream)) {
			PDDocumentCatalog catalog = document.getDocumentCatalog();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
