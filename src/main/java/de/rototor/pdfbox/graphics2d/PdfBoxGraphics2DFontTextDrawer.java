package de.rototor.pdfbox.graphics2d;

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontFactory;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.*;
import java.text.AttributedCharacterIterator;
import java.util.*;
import java.util.List;

/**
 * Default implementation to draw fonts
 */
public class PdfBoxGraphics2DFontTextDrawer implements IPdfBoxGraphics2DFontTextDrawer, Closeable {
	/**
	 * Close / delete all resources associated with this drawer. This mainly means
	 * deleting all temporary files. You can not use this object after a call to
	 * close.
	 * 
	 * Calling close multiple times does nothing.
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void close() throws IOException {
		for (File tempFile : tempFiles)
			tempFile.delete();
		tempFiles.clear();
		fontFiles.clear();
		fontMap.clear();
	}

	private static class FontEntry {
		String overrideName;
		File file;
	}

	private final List<FontEntry> fontFiles = new ArrayList<FontEntry>();
	private final List<File> tempFiles = new ArrayList<File>();
	private final Map<String, PDFont> fontMap = new HashMap<String, PDFont>();
	private PDFont defaultFont;

	/**
	 * Register a font. If possible, try to use a font file, i.e.
	 * {@link #registerFont(String,File)}. This method will lead to the creation of
	 * a temporary file which stores the font data.
	 * 
	 * @param fontName
	 *            the name of the font to use. If null, the name is taken from the
	 *            font.
	 * @param fontStream
	 *            the input stream of the font. This file must be a ttf/otf file!
	 *            You have to close the stream outside, this method will not close
	 *            the stream.
	 */
	@SuppressWarnings("WeakerAccess")
	public void registerFont(String fontName, InputStream fontStream) throws IOException, FontFormatException {
		File fontFile = File.createTempFile("pdfboxgfx2dfont", ".ttf");
		FileOutputStream out = new FileOutputStream(fontFile);
		try {
			IOUtils.copy(fontStream, out);
		} finally {
			out.close();
		}
		fontFile.deleteOnExit();
		tempFiles.add(fontFile);
		registerFont(fontName, fontFile);
	}

	/**
	 * Register a font.
	 * 
	 * @param fontName
	 *            the name of the font to use. If null, the name is taken from the
	 *            font.
	 * @param fontFile
	 *            the font file. This file must exist for the live time of this
	 *            object, as the font data will be read lazy on demand
	 */
	@SuppressWarnings("WeakerAccess")
	public void registerFont(String fontName, File fontFile) throws IOException, FontFormatException {
		FontEntry entry = new FontEntry();
		entry.overrideName = fontName;
		entry.file = fontFile;
		fontFiles.add(entry);
	}

	/**
	 * Override for registerFont(null,fontFile)
	 * 
	 * @param fontFile
	 *            the font file
	 */
	@SuppressWarnings("WeakerAccess")
	public void registerFont(File fontFile) throws IOException, FontFormatException {
		registerFont(null, fontFile);
	}

	/**
	 * Override for registerFont(null,fontStream)
	 * 
	 * @param fontStream
	 *            the font file
	 */
	@SuppressWarnings("WeakerAccess")
	public void registerFont(InputStream fontStream) throws IOException, FontFormatException {
		registerFont(null, fontStream);
	}

	@Override
	public boolean canDrawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env)
			throws IOException, FontFormatException {
		boolean run = true;
		StringBuilder sb = new StringBuilder();
		while (run) {

			Font attributeFont = (Font) iterator.getAttribute(TextAttribute.FONT);
			if (attributeFont == null)
				attributeFont = env.getFont();
			if (mapFont(attributeFont, env) == null)
				return false;

			Paint paint = (Paint) iterator.getAttribute(TextAttribute.FOREGROUND);
			if (paint == null)
				paint = env.getPaint();

			/*
			 * When the paint has a shading, we can not draw the text correct, as we can not
			 * apply shadings on text in PDF.
			 */
			if (env.applyPaint(paint) != null)
				return false;

			/*
			 * We can not do a Background on the text currently.
			 */
			if (iterator.getAttribute(TextAttribute.BACKGROUND) != null)
				return false;

			run = iterateRun(iterator, sb);
			String s = sb.toString();
			int l = s.length();
			for (int i = 0; i < l;) {
				int codePoint = s.codePointAt(i);
				switch (Character.getDirectionality(codePoint)) {
				/*
				 * We can handle normal LTR.
				 */
				case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
				case Character.DIRECTIONALITY_EUROPEAN_NUMBER:
				case Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR:
				case Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR:
					break;
				default:
					/*
					 * Default: We can not handle this
					 */
					return false;
				}

				if (!attributeFont.canDisplay(codePoint))
					return false;

				i += Character.charCount(codePoint);
			}
		}
		return true;
	}

	@Override
	public void drawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env)
			throws IOException, FontFormatException {
		PDPageContentStream contentStream = env.getContentStream();

		contentStream.beginText();

		Matrix textMatrix = new Matrix();
		textMatrix.scale(1, -1);
		contentStream.setTextMatrix(textMatrix);

		StringBuilder sb = new StringBuilder();
		boolean run = true;
		while (run) {

			Font attributeFont = (Font) iterator.getAttribute(TextAttribute.FONT);
			if (attributeFont == null)
				attributeFont = env.getFont();

			Number fontSize = ((Number) iterator.getAttribute(TextAttribute.SIZE));
			if (fontSize != null)
				attributeFont = attributeFont.deriveFont(fontSize.floatValue());
			applyFont(attributeFont, env);

			Paint paint = (Paint) iterator.getAttribute(TextAttribute.FOREGROUND);
			if (paint == null)
				paint = env.getPaint();
			env.applyPaint(paint);

			run = iterateRun(iterator, sb);
			String text = sb.toString();

			/*
			 * If we force the text write we may encounter situations where the font can not
			 * display the characters. PDFBox will throw an exception in this case. We will
			 * just silently ignore the text and not display it instead.
			 */
			try {
				contentStream.showText(text);
			} catch (IllegalArgumentException e) {
				System.err.println("PDFBoxGraphics: Can not map text " + text + " with font "
						+ attributeFont.getFontName() + ": " + e.getMessage());
			}
		}
		contentStream.endText();
	}

	private void applyFont(Font font, IFontTextDrawerEnv env) throws IOException, FontFormatException {
		PDFont fontToUse = mapFont(font, env);
		if (fontToUse == null) {
			fontToUse = defaultFont;
		}
		env.getContentStream().setFont(fontToUse, font.getSize2D());
	}

	private PDFont mapFont(final Font font, final IFontTextDrawerEnv env) throws IOException, FontFormatException {
		if (fontMap.size() == 0) {
			if (defaultFont == null) {
				defaultFont = PDFontFactory.createDefaultFont();
				fontMap.put("Dialog", defaultFont);
				fontMap.put("SansSerif", defaultFont);
			}
		}

		/*
		 * If we have any font registering's, we must perform them now
		 */
		for (final FontEntry fontEntry : fontFiles) {
			if (fontEntry.overrideName == null) {
				Font javaFont = Font.createFont(Font.TRUETYPE_FONT, fontEntry.file);
				fontEntry.overrideName = javaFont.getFontName();
			}
			if (fontEntry.file.getName().toLowerCase(Locale.US).endsWith(".ttc")) {
				TrueTypeCollection collection = new TrueTypeCollection(fontEntry.file);
				collection.processAllFonts(new TrueTypeCollection.TrueTypeFontProcessor() {
					@Override
					public void process(TrueTypeFont ttf) throws IOException {
						PDFont pdFont = PDType0Font.load(env.getDocument(), ttf, true);
						fontMap.put(fontEntry.overrideName, pdFont);
						fontMap.put(pdFont.getName(), pdFont);
					}
				});
			} else {
				/*
				 * We load the font using the file.
				 */
				PDFont pdFont = PDType0Font.load(env.getDocument(), fontEntry.file);
				fontMap.put(fontEntry.overrideName, pdFont);
			}
		}
		fontFiles.clear();

		return fontMap.get(font.getFontName());
	}

	private boolean iterateRun(AttributedCharacterIterator iterator, StringBuilder sb) {
		sb.setLength(0);
		int charCount = iterator.getRunLimit() - iterator.getRunStart();
		while (charCount-- >= 0) {
			char c = iterator.current();
			iterator.next();
			if (c == AttributedCharacterIterator.DONE) {
				return false;
			} else {
				sb.append(c);
			}
		}
		return true;
	}

}
