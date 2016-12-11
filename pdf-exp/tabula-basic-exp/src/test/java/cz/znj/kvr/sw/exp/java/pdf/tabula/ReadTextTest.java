package cz.znj.kvr.sw.exp.java.pdf.tabula;

import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.testng.annotations.Test;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.ExtractionAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


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
			ObjectExtractor oe = new ObjectExtractor(document);

			ExtractionAlgorithm extractor = new BasicExtractionAlgorithm();

			PageIterator it = oe.extract();
			while (it.hasNext()) {
				Page page = it.next();

				for (Table table : extractor.extract(page)) {
					for (List<RectangularTextContainer> row : table.getRows()) {
						for (RectangularTextContainer cell : row) {
							System.out.println(cell.getText());
						}
					}
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
