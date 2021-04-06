package org.apache.pdfbox.multipdf;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

/**
 * Temporary Workaround to allow access to the lowlevel functionality
 *
 * DO NOT USE!!!
 */
@Deprecated
public class InternalDeprecatedCOSCloner extends PDFCloneUtility
{
    /**
     * Creates a new instance for the given target document.
     *
     * @param dest the destination PDF document that will receive the clones
     */
    public InternalDeprecatedCOSCloner(PDDocument dest)
    {
        super(dest);
    }

    public COSBase cloneForNewDocument(Object base) throws IOException
    {
        return super.cloneForNewDocument(base);
    }
}
