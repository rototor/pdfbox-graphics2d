package de.rototor.pdfbox.graphics2d;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class PdfBoxGraphics2DPaintApplierTest
{
    @Test
    public void testhaveColorsTransparency()
    {
        assertFalse(PdfBoxGraphics2DPaintApplier.haveColorsTransparency(
                new Color[] { Color.BLACK, Color.BLUE }));
        assertTrue(PdfBoxGraphics2DPaintApplier.haveColorsTransparency(
                new Color[] { Color.BLACK, Color.BLUE, new Color(128, 128, 128, 128) }));
    }
}