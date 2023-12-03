package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.multipdf.PDFCloneUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

/**
 * Internal class to allow access to the clone functionality.
 */
class InternalCOSCloner extends PDFCloneUtility
{
    /**
     * Creates a new instance for the given target document.
     *
     * @param dest the destination PDF document that will receive the clones
     */
    public InternalCOSCloner(PDDocument dest)
    {
        super(dest);
    }

    @Override
    public <TCOSBase extends COSBase> TCOSBase cloneForNewDocument(TCOSBase base) throws IOException
    {
        return super.cloneForNewDocument(base);
    }
}
