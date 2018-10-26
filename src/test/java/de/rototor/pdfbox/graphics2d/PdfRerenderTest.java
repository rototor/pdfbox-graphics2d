package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class PdfRerenderTest {

	@Test
	public void testPDFRerender() throws IOException {
		rerenderPDF("heart.pdf");
		rerenderPDF("barChart.pdf");
	}

	protected void rerenderPDF(String name) throws IOException {
		File parentDir = new File("target/test");
		// noinspection ResultOfMethodCallIgnored
		parentDir.mkdirs();

		PDDocument document = new PDDocument();
		PDDocument sourceDoc = PDDocument.load(PdfRerenderTest.class.getResourceAsStream(name));

		for (PDPage sourcePage : sourceDoc.getPages()) {
			PDPage rerenderedPage = new PDPage(sourcePage.getMediaBox());
			document.addPage(rerenderedPage);
			PDPageContentStream cb = new PDPageContentStream(document, rerenderedPage);
			try {
				PdfBoxGraphics2D gfx = new PdfBoxGraphics2D(document, sourcePage.getMediaBox());

				// Do overfill for red with a transparent green
				gfx.setDrawControl(new PdfBoxGraphics2DDrawControlDefault() {
					boolean insideOwnDraw = false;

					@Override
					public void afterShapeFill(Shape shape, IDrawControlEnv env) {
						afterShapeDraw(shape, env);
					}

					@Override
					public void afterShapeDraw(Shape shape, IDrawControlEnv env) {
						if (insideOwnDraw)
							return;
						insideOwnDraw = true;
						Paint paint = env.getPaint();
						if (paint instanceof Color) {
							if (paint.equals(Color.RED)) {
								// We overfill with black a little bit
								PdfBoxGraphics2D graphics = env.getGraphics();
								Stroke prevStroke = graphics.getStroke();
								float additinalStrokeWidth = 1f;
								if (prevStroke instanceof BasicStroke) {
									BasicStroke basicStroke = ((BasicStroke) prevStroke);
									graphics.setStroke(new BasicStroke(
											basicStroke.getLineWidth() + additinalStrokeWidth, basicStroke.getEndCap(),
											basicStroke.getLineJoin(), basicStroke.getMiterLimit(),
											basicStroke.getDashArray(), basicStroke.getDashPhase()));
								} else {
									graphics.setStroke(new BasicStroke(additinalStrokeWidth));
								}
								graphics.setPaint(new Color(0, 255, 0, 128));
								graphics.draw(shape);

								graphics.setPaint(paint);
								graphics.setStroke(prevStroke);
							}
						}
						insideOwnDraw = false;
					}
				});

				PDFRenderer pdfRenderer = new PDFRenderer(sourceDoc) {
					@Override
					protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
						return new PageDrawer(parameters) {
							@Override
							protected Paint getPaint(PDColor color) throws IOException {
								PDColorSpace colorSpace = color.getColorSpace();

								// We always must handle patterns recursive
								if (colorSpace instanceof PDPattern)
									return super.getPaint(color);

								// Now our special logic
								if (colorSpace instanceof PDDeviceRGB) {
									float[] components = color.getComponents();
									boolean allBlack = true;
									for (float f : components) {
										if (f > 0.0)
											allBlack = false;
									}
									if (allBlack)
										return new PdfBoxGraphics2DCMYKColor(1f, 0.0f, 0.2f, 0.1f, 128);
								}

								// All other colors just stay the same...
								return super.getPaint(color);
							}

							@Override
							public void drawImage(PDImage pdImage) {
								// We dont like images, just skip them all
							}
						};
					}
				};
				pdfRenderer.renderPageToGraphics(sourceDoc.getPages().indexOf(sourcePage), gfx);
				gfx.dispose();

				PDFormXObject xFormObject = gfx.getXFormObject();
				cb.drawForm(xFormObject);
			} finally {
				cb.close();
			}
		}

		document.save(new File(parentDir, "rerendered_" + name));
		document.close();
	}
}
