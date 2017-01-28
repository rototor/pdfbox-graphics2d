package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PdfBoxGraphics2dTest {

	@Test
	public void testSimpleGraphics2d() throws IOException {

		exportGraphic("simple", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException {
				BufferedImage img1 = ImageIO.read(PdfBoxGraphics2dTest.class.getResourceAsStream("colortest.png"));
				BufferedImage img2 = ImageIO.read(PdfBoxGraphics2dTest.class.getResourceAsStream("pixeltest.png"));

				gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				gfx.drawImage(img1, 70, 50, 100, 50, null);

				gfx.setColor(Color.YELLOW);
				gfx.drawRect(20, 20, 100, 100);
				gfx.setColor(Color.GREEN);
				gfx.fillRect(10, 10, 50, 50);

				gfx.setColor(Color.RED);
				gfx.drawString("Hello World!", 30, 120);
				gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				gfx.drawImage(img2, 30, 50, 50, 50, null);
			}
		});
	}

	interface GraphicsExporter {
		void draw(Graphics2D gfx) throws IOException;
	}

	private void exportGraphic(String name, GraphicsExporter exporter) throws IOException {
		PDDocument document = new PDDocument();

		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);
		File parentDir = new File("target/test/simple");
		parentDir.mkdirs();

		PDPageContentStream contentStream = new PDPageContentStream(document, page);

		PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 200, 200);
		exporter.draw(pdfBoxGraphics2D);
		pdfBoxGraphics2D.dispose();

		BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D imageGraphics = image.createGraphics();
		exporter.draw(imageGraphics);
		imageGraphics.dispose();
		ImageIO.write(image, "PNG", new File(parentDir, name + ".png"));

		PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
		Matrix matrix = new Matrix();
		matrix.translate(0, 20);
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);

		matrix.scale(1.5f, 1.5f);
		matrix.translate(0, 100);
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);
		contentStream.close();

		document.save(new File(parentDir, name + ".pdf"));
		document.close();
	}
}