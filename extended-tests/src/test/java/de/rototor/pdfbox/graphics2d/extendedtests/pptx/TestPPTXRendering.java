package de.rototor.pdfbox.graphics2d.extendedtests.pptx;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public class TestPPTXRendering {

	@Test
	public void testPPTXRendering() throws IOException, InvalidFormatException {
		File parentDir = new File("target/test/pptx");
		parentDir.mkdirs();

		InputStream inputStream = TestPPTXRendering.class.getResourceAsStream("test.mod.pptx");
		assertNotNull(inputStream);
		XMLSlideShow ppt = new XMLSlideShow(OPCPackage.open(inputStream));

		PDDocument document = new PDDocument();

		for (XSLFSlide slide : ppt.getSlides()) {
			PDPage page = new PDPage();
			PDPageContentStream contentStream = new PDPageContentStream(document, page);

			PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 1000, 1000);
			slide.draw(pdfBoxGraphics2D);
			pdfBoxGraphics2D.dispose();

			contentStream.transform(Matrix.getScaleInstance(0.2f, 0.2f));
			contentStream.drawForm(pdfBoxGraphics2D.getXFormObject());
			contentStream.close();

			document.addPage(page);
		}

		document.save(new File(parentDir, "test.mod.pdf"));
	}
}
