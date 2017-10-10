/*
 * Copyright 2017 Emmeran Seehuber

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDICCBased;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Encodes all images using lossless compression. Tries to reuse images as much
 * as possible. You can share an instance of this class with multiple
 * PdfBoxGraphics2D objects.
 */
public class PdfBoxGraphics2DLosslessImageEncoder implements IPdfBoxGraphics2DImageEncoder {
	private Map<ImageSoftReference, SoftReference<PDImageXObject>> imageMap = new HashMap<ImageSoftReference, SoftReference<PDImageXObject>>();
	private SoftReference<PDDocument> doc;

	@Override
	public PDImageXObject encodeImage(PDDocument document, PDPageContentStream contentStream, Image image) {
		final BufferedImage bi;

		if (image instanceof BufferedImage) {
			bi = (BufferedImage) image;
		} else {
			int width = image.getWidth(null);
			int height = image.getHeight(null);
			bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics graphics = bi.getGraphics();
			if (!graphics.drawImage(image, 0, 0, null, null))
				throw new IllegalStateException("Not fully loaded images are not supported.");
			graphics.dispose();
		}

		try {
			if (doc == null || doc.get() != document) {
				imageMap = new HashMap<ImageSoftReference, SoftReference<PDImageXObject>>();
				doc = new SoftReference<PDDocument>(document);
			}
			SoftReference<PDImageXObject> pdImageXObjectSoftReference = imageMap.get(new ImageSoftReference(image));
			PDImageXObject imageXObject = pdImageXObjectSoftReference == null ? null
					: pdImageXObjectSoftReference.get();
			if (imageXObject == null) {
				imageXObject = LosslessFactory.createFromImage(document, bi);

				/*
				 * Do we have a color profile we need to embed?
				 */
				if (bi.getColorModel().getColorSpace() instanceof ICC_ColorSpace) {
					ICC_Profile profile = ((ICC_ColorSpace) bi.getColorModel().getColorSpace()).getProfile();
					/*
					 * Only tag a profile if it is not the default sRGB profile.
					 */
					if (bi.getColorModel().getColorSpace() != ICC_ColorSpace.getInstance(ICC_ColorSpace.CS_sRGB)) {
						PDICCBased pdProfile = new PDICCBased(document);
						OutputStream outputStream = pdProfile.getPDStream().createOutputStream();
						outputStream.write(profile.getData());
						outputStream.close();
						imageXObject.setColorSpace(pdProfile);
					}
				}
				imageMap.put(new ImageSoftReference(image), new SoftReference<PDImageXObject>(imageXObject));
			}

			return imageXObject;
		} catch (IOException e) {
			throw new RuntimeException("Could not encode Image", e);
		}
	}

	private class ImageSoftReference extends SoftReference<Image> {
		ImageSoftReference(Image referent) {
			super(referent);
		}

		@Override
		public boolean equals(Object obj) {
			assert obj instanceof ImageSoftReference;
			return ((ImageSoftReference) obj).get() == get();
		}

		@Override
		public int hashCode() {
			Image image = get();
			if (image == null)
				return 0;
			return image.hashCode();
		}
	}
}
