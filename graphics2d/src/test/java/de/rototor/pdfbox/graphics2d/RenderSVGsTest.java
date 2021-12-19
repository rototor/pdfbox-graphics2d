package de.rototor.pdfbox.graphics2d;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.junit.Test;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.IOException;

public class RenderSVGsTest extends PdfBoxGraphics2DTestBase
{

    @Test
    public void testSVGs() throws IOException
    {
        renderSVG("barChart.svg", 0.45);
        renderSVG("gump-bench.svg", 1);
        renderSVG("json.svg", 150);
        renderSVG("heart.svg", 200);
        renderSVG("displayWebStats.svg", 200);
        renderSVG("compuserver_msn_Ford_Focus.svg", 0.7);
        renderSVG("watermark.svg", 0.4);
    }

    @Test
    public void renderFailureCases() throws IOException
    {
        // renderSVG("openhtml_536.svg", 1);
        renderSVG("openhtml_538_gradient.svg", .5);
    }

    @Test
    public void testGradientSVGEmulateObjectBoundingBox() throws IOException
    {
        renderSVG("long-gradient.svg", 0.55);
        renderSVG("tall-gradient.svg", 0.33);
        renderSVG("near-square-gradient.svg", 0.30);
        renderSVG("square-gradient.svg", 0.55);
        renderSVG("tall-gradient-downward-slope.svg", 0.33);
    }

    @Test
    public void testHorizontalGradient() throws IOException
    {
        renderSVG("horizontal-gradient.svg", 0.55);
    }

    @Test
    public void testSVGinCMYKColorspace() throws IOException, FontFormatException
    {
        renderSVGCMYK("atmospheric-composition.svg", 0.7);
    }

    private void renderSVG(String name, final double scale) throws IOException
    {
        String uri = String.valueOf(RenderSVGsTest.class.getResource(name));

        // create the document
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        Document document = f.createDocument(uri, RenderSVGsTest.class.getResourceAsStream(name));

        // create the GVT
        UserAgent userAgent = new UserAgentAdapter();
        DocumentLoader loader = new DocumentLoader(userAgent);
        BridgeContext bctx = new BridgeContext(userAgent, loader);
        bctx.setDynamicState(BridgeContext.STATIC);
        GVTBuilder builder = new GVTBuilder();
        final GraphicsNode gvtRoot = builder.build(bctx, document);

        this.exportGraphic("svg", name.replace(".svg", ""), new GraphicsExporter()
        {
            @Override
            public void draw(Graphics2D gfx)
            {
                gfx.scale(scale, scale);
                gvtRoot.paint(gfx);
            }
        });
    }

    private void renderSVGCMYK(String name, final double scale)
            throws IOException, FontFormatException
    {
        String uri = String.valueOf(RenderSVGsTest.class.getResource(name));

        // create the document
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        Document document = f.createDocument(uri, RenderSVGsTest.class.getResourceAsStream(name));

        // create the GVT
        File parentDir = new File("target/test/svg");
        UserAgent userAgent = new UserAgentAdapter();
        DocumentLoader loader = new DocumentLoader(userAgent);
        BridgeContext bctx = new BridgeContext(userAgent, loader);
        bctx.setDynamicState(BridgeContext.STATIC);
        GVTBuilder builder = new GVTBuilder();
        final GraphicsNode gvtRoot = builder.build(bctx, document);
        exportAsPNG(name, new GraphicsExporter() {
            @Override
            public void draw(Graphics2D gfx)
            {
                gfx.scale(scale, scale);
                gvtRoot.paint(gfx);
            }
        }, parentDir, (int) Math.round(scale + 0.5)*2);

        try
        {
            PDDocument pdfDocument = new PDDocument();

            // noinspection ResultOfMethodCallIgnored
            parentDir.mkdirs();

            PDPage page = new PDPage(PDRectangle.A4);
            pdfDocument.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page);

            PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(pdfDocument, 400, 400);

            ICC_Profile icc_profile = ICC_Profile.getInstance(PDDocument.class.getResourceAsStream(
                    "/org/apache/pdfbox/resources/icc/ISOcoated_v2_300_bas.icc"));
            PdfBoxGraphics2DColorMapper colorMapper = new RGBtoCMYKColorMapper(icc_profile,
                    pdfDocument);
            pdfBoxGraphics2D.setColorMapper(colorMapper);

            PdfBoxGraphics2DFontTextDrawer fontTextDrawer = null;
            contentStream.beginText();
            contentStream.setStrokingColor(0.0f, 0.0f, 0.0f, 1.0f);
            contentStream.setNonStrokingColor(0.0f, 0.0f, 0.0f, 1.0f);
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 15);
            contentStream.setTextMatrix(Matrix.getTranslateInstance(10, 800));
            contentStream.showText("Mode: CMYK colorspace");
            contentStream.endText();
            fontTextDrawer = new PdfBoxGraphics2DFontTextDrawer();

            pdfBoxGraphics2D.setFontTextDrawer(fontTextDrawer);

            pdfBoxGraphics2D.scale(scale, scale);
            gvtRoot.paint(pdfBoxGraphics2D);
            pdfBoxGraphics2D.dispose();

            PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
            Matrix matrix = new Matrix();
            matrix.translate(0, 300);
            contentStream.transform(matrix);
            contentStream.drawForm(appearanceStream);

            contentStream.close();

            String baseName = name.substring(0, name.lastIndexOf('.'));
            pdfDocument.save(new File(parentDir, baseName + ".pdf"));
            pdfDocument.close();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
