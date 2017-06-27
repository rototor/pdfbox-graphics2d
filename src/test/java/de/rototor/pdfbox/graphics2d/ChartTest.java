package de.rototor.pdfbox.graphics2d;

public class ChartTest
{
    private static final String FILE_PATH = "C:/aaa/chart_test_file.pdf";

    public static void main(String[] args)
    {
        ChartFileBuilder builder = new ChartFileBuilder(FILE_PATH);
        builder.go();
    }
}