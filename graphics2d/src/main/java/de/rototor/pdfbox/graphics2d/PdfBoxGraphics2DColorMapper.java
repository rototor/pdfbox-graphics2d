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

import java.awt.*;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

public class PdfBoxGraphics2DColorMapper implements IPdfBoxGraphics2DColorMapper {
	@Override
	public PDColor mapColor(Color color, IColorMapperEnv env) {
		if (color == null)
			return new PDColor(new float[] { 1f, 1f, 1f }, PDDeviceRGB.INSTANCE);

		// Support for legacy iText 2 CMYK Color Class
		if (color.getClass().getSimpleName().equals("CMYKColor")) {
			float c = PdfBoxGraphics2DPaintApplier.getPropertyValue(color, "getCyan");
			float m = PdfBoxGraphics2DPaintApplier.getPropertyValue(color, "getMagenta");
			float y = PdfBoxGraphics2DPaintApplier.getPropertyValue(color, "getYellow");
			float k = PdfBoxGraphics2DPaintApplier.getPropertyValue(color, "getBlack");
			return new PDColor(new float[] { c, m, y, k }, PDDeviceCMYK.INSTANCE);
		}

		// Our universal color carrier.
		if (color instanceof IPdfBoxGraphics2DColor)
			return ((IPdfBoxGraphics2DColor) color).toPDColor();

		float[] components = new float[] { color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f };
		return new PDColor(components, PDDeviceRGB.INSTANCE);
	}
}
