package de.rototor.pdfbox.graphics2d;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.*;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.junit.Test;
import org.w3c.dom.Document;

import java.awt.*;
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
    public void testGradientSVG() throws IOException
    {
        renderSVG("long-gradient.svg", 0.55);
    }

    private void renderSVG(String name, final double scale) throws IOException
    {
        String uri = RenderSVGsTest.class.getResource(name).toString();

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

}
