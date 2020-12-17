package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.junit.Test;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.text.AttributedString;

public class InvalidPDFTest
{

    @Test
    public void testInvalidPDF() throws Exception
    {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(new PDRectangle(800, 450));
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        PdfBoxGraphics2D g = new PdfBoxGraphics2D(document, page.getMediaBox());

        PdfBoxGraphics2DFontTextDrawer textDrawer = new PdfBoxGraphics2DFontTextDrawer();
        g.setFontTextDrawer(textDrawer);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g.scale(0.41688379364252215, 0.41688379364252215);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f));
        g.fillRect(0, 0, 800, 450);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 1.0f));
        Path2D p;
        g.setPaint(new Color(0xFFFFFFFF));
        g.fill(new Rectangle2D.Double(0, 0, 1919, 1080));
        g.setColor(new Color(0x00FFFFFF));
        g.fillRect(0, 0, 1919, 1080);

        g.setColor(new Color(0x00FFFFFF));
        g.fillRect(0, 0, 1919, 1080);

        g.setColor(new Color(0x00FFFFFF));
        g.fillRect(0, 0, 1919, 1080);

        g.setStroke(new BasicStroke(0.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0.25f,
                null, 0.0f));

        g.setPaint(new Color(0x00FFFFFF));
        p = new Path2D.Double(1);
        p.moveTo(1401.6755905511811, 1029.7417322834647);
        p.lineTo(1787.9811023622046, 1029.7417322834647);
        p.lineTo(1787.9811023622046, 1054.828346456693);
        p.lineTo(1401.6755905511811, 1054.828346456693);
        p.closePath();
        g.fill(p);
        AttributedString as;
        as = new AttributedString("OpenText Confidential. ©2019 All Rights ");
        as.addAttribute(TextAttribute.FONT, new Font("Arial", Font.PLAIN, 18), 0, 40);
        as.addAttribute(TextAttribute.FOREGROUND, new Color(0xFF000000), 0, 40);
        as.addAttribute(TextAttribute.FAMILY, "Arial", 0, 40);
        as.addAttribute(TextAttribute.SIZE, 18.0, 0, 40);
        g.drawString(as.getIterator(), 1408.7622f, 1039.0594f);

        as = new AttributedString("Reserved.");
        as.addAttribute(TextAttribute.FONT, new Font("Arial", Font.PLAIN, 18), 0, 9);
        as.addAttribute(TextAttribute.FOREGROUND, new Color(0xFF000000), 0, 9);
        as.addAttribute(TextAttribute.FAMILY, "Arial", 0, 9);
        as.addAttribute(TextAttribute.SIZE, 18.0, 0, 9);
        g.drawString(as.getIterator(), 1408.7622f, 1058.58f);

        g.setTransform(
                 new AffineTransform(0.41688379364252215f, 0.0f, 0.0f, 0.41688379364252215f,
                         0.0f, 0.0f));
        g.setPaint(new Color(0x00FFFFFF));
        g.setStroke(new BasicStroke(0.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0.25f,
                null, 0.0f));
        p = new Path2D.Double(1);
        p.moveTo(1401.6755905511811, 1029.7417322834647);
        p.lineTo(1787.9811023622046, 1029.7417322834647);
        p.lineTo(1787.9811023622046, 1054.828346456693);
        p.lineTo(1401.6755905511811, 1054.828346456693);
        p.closePath();
        g.draw(p);

        g.setPaint(new Color(0x00FFFFFF));

        g.setColor(new Color(0x00FFFFFF));
        g.setPaint(new Color(0x00FFFFFF));
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f,
                null, 0.0f));
        g.setTransform(
                new AffineTransform(0.41688379364252215f, 0.0f, 0.0f, 0.41688379364252215f,
                        0.0f, 0.0f));

        g.setStroke(new BasicStroke(0.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0.25f,
                null, 0.0f));

        g.setPaint(new Color(0x00FFFFFF));
        p = new Path2D.Double(1);
        p.moveTo(1777.067716535433, 1029.5433070866143);
        p.lineTo(1828.0346456692912, 1029.5433070866143);
        p.lineTo(1828.0346456692912, 1054.6015748031498);
        p.lineTo(1777.067716535433, 1054.6015748031498);
        p.closePath();
        g.fill(p);

        as = new AttributedString("1");
        as.addAttribute(TextAttribute.FONT, new Font("Arial", Font.PLAIN, 18), 0, 1);
        as.addAttribute(TextAttribute.FOREGROUND, new Color(0xFF000000), 0, 1);
        as.addAttribute(TextAttribute.FAMILY, "Arial", 0, 1);
        as.addAttribute(TextAttribute.SIZE, 18.0, 0, 1);
        g.drawString(as.getIterator(), 1784.1543f, 1049.9705f);
        g.setTransform(
                new AffineTransform(0.41688379364252215f, 0.0f, 0.0f, 0.41688379364252215f,
                        0.0f, 0.0f));
        g.setPaint(new Color(0x00FFFFFF));
        g.setStroke(new BasicStroke(0.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0.25f,
                null, 0.0f));
        p = new Path2D.Double(1);
        p.moveTo(1777.067716535433, 1029.5433070866143);
        p.lineTo(1828.0346456692912, 1029.5433070866143);
        p.lineTo(1828.0346456692912, 1054.6015748031498);
        p.lineTo(1777.067716535433, 1054.6015748031498);
        p.closePath();
        g.draw(p);

        g.setPaint(new Color(0x00FFFFFF));

        g.setColor(new Color(0x00FFFFFF));
        g.setPaint(new Color(0x00FFFFFF));
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f,
                null, 0.0f));
        g.setTransform(
                new AffineTransform(0.41688379364252215f, 0.0f, 0.0f, 0.41688379364252215f,
                        0.0f, 0.0f));

        g.setStroke(new BasicStroke(0.992126f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                0.992126f, null, 0.0f));

        g.setPaint(new Color(0x00FFFFFF));
        p = new Path2D.Double(1);
        p.moveTo(0.0, 1003.9748031496063);
        p.lineTo(1919.8488188976378, 1004.0031496062992);
        g.fill(p);

        g.setTransform(
                new AffineTransform(0.41688379364252215f, 0.0f, 0.0f, 0.41688379364252215f,
                        0.0f, 0.0f));
        g.setPaint(new Color(0xFFD9D9D9));
        g.setStroke(new BasicStroke(0.992126f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                0.992126f, null, 0.0f));
        p = new Path2D.Double(1);
        p.moveTo(0.0, 1003.9748031496063);
        p.lineTo(1919.8488188976378, 1004.0031496062992);
        g.draw(p);

        g.setPaint(new Color(0xFFD9D9D9));

        g.setColor(new Color(0x00FFFFFF));
        g.setPaint(new Color(0x00FFFFFF));
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f,
                null, 0.0f));
        g.setTransform(
                new AffineTransform(0.41688379364252215f, 0.0f, 0.0f, 0.41688379364252215f,
                        0.0f, 0.0f));

        g.dispose();

        PDFormXObject appearanceStream = g.getXFormObject();
        contentStream.drawForm(appearanceStream);
        contentStream.close();

        File f = new File("target/test/invalidpdf/fonttest.pdf");
        f.getParentFile().mkdirs();
        document.save(f);
    }

    @Test
    public void testInvalidPDF2() throws Exception
    {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(new PDRectangle(800, 450));
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        PdfBoxGraphics2D g = new PdfBoxGraphics2D(document, page.getMediaBox());

        PdfBoxGraphics2DFontTextDrawer textDrawer = new PdfBoxGraphics2DFontTextDrawer();
        g.setFontTextDrawer(textDrawer);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        Path2D p;
        {
            g.setTransform(
                    new AffineTransform(0.41688379364252215f, 0.0f, 0.0f, 0.41688379364252215f,
                            0.0f, 0.0f));
            g.setPaint(new Color(0x00FFFFFF));
            g.setStroke(new BasicStroke(0.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0.25f,
                    null, 0.0f));
            p = new Path2D.Double(1);
            p.moveTo(1401.6755905511811, 1029.7417322834647);
            p.lineTo(1787.9811023622046, 1029.7417322834647);
            p.lineTo(1787.9811023622046, 1054.828346456693);
            p.lineTo(1401.6755905511811, 1054.828346456693);
            p.closePath();
            g.draw(p);
        }

        g.dispose();

        PDFormXObject appearanceStream = g.getXFormObject();
        contentStream.drawForm(appearanceStream);
        contentStream.close();

        File f = new File("target/test/invalidpdf/fonttest2.pdf");
        f.getParentFile().mkdirs();
        document.save(f);
    }
}
