package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;

/**
 * This color class represents a CMYK Color. You can use this class if you want to paint with DeviceCMYK Colors
 */
public class PdfBoxGraphics2DCMYKColor extends PdfBoxGraphics2DColor
{
    private final float c, m, y, k;

    public PdfBoxGraphics2DCMYKColor(int c, int m, int y, int k, int alpha)
    {
        this(c / 255f, m / 255f, y / 255f, k / 255f, alpha);
    }

    public PdfBoxGraphics2DCMYKColor(int c, int m, int y, int k)
    {
        this(c / 255f, m / 255f, y / 255f, k / 255f);
    }

    public PdfBoxGraphics2DCMYKColor(float c, float m, float y, float k)
    {
        this(c, m, y, k, 255);
    }

    public PdfBoxGraphics2DCMYKColor(float c, float m, float y, float k, int alpha)
    {
        this(c, m, y, k, alpha, PDDeviceCMYK.INSTANCE);
    }

    public PdfBoxGraphics2DCMYKColor(float c, float m, float y, float k, int alpha,
            PDColorSpace colorSpace)
    {
        this(c, m, y, k, alpha, colorSpace, false);
    }

    public PdfBoxGraphics2DCMYKColor(float c, float m, float y, float k, int alpha,
            PDColorSpace colorSpace, boolean overprint)
    {
        super(new PDColor(new float[] { c, m, y, k }, colorSpace), alpha, overprint);
        this.c = c;
        this.m = m;
        this.y = y;
        this.k = k;
    }

    public float getC()
    {
        return c;
    }

    public float getM()
    {
        return m;
    }

    public float getY()
    {
        return y;
    }

    public float getK()
    {
        return k;
    }

}
