package cz.znj.kvr.sw.exp.java.pdf.pdfbox;

import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
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
public class ReadTextTest
{
	@Test
	public void testRead()
	{
		try (InputStream documentStream = getClass().getResourceAsStream("test-table.pdf");
		     PDDocument document = PDDocument.load(documentStream)) {
			COSDocument cosDocument = document.getDocument();
			PDFTextStripper stripper = new PDFTextStripper();
			//stripper.setPageStart("1");
			//stripper.setPageEnd("10");
			String text = stripper.getText(document);
			System.out.print(text);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
