package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class PdfBoxGraphics2dTest {

	@Test
	public void testSimpleGraphics2d() throws IOException {
		PDDocument document = new PDDocument();

		// Create a new blank page and add it to the document
		PDPage page = new PDPage();
		document.addPage(page);
		File parentDir = new File("target/test/simple");
		parentDir.mkdirs();


		PDPageContentStream contentStream = new PDPageContentStream(document, page);
		contentStream.


		// Save the newly created document
		document.save(new File(parentDir, "BlankPage.pdf"));

		// finally make sure that the document is properly
		// closed.
		document.close();
	}
}