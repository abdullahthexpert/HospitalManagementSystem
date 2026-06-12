package hms.software;

// Requires: itextpdf-5.5.13.jar in your IntelliJ project Libraries
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.util.List;

public class ReportGenerator {

    private static final Font TITLE_FONT    = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font HEADER_FONT   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font CELL_FONT     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);

    public static void generateTableReport(String file_path, String title,
                                            String[] headers, List<String[]> rows) {
        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(file_path));
            doc.open();

            Paragraph p_title = new Paragraph("HMS - " + title, TITLE_FONT);
            p_title.setAlignment(Element.ALIGN_CENTER);
            doc.add(p_title);
            doc.add(new Paragraph("Generated: " + java.time.LocalDate.now(), SUBTITLE_FONT));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
                cell.setBackgroundColor(new BaseColor(41, 128, 185));
                cell.setPadding(6);
                table.addCell(cell);
            }

            boolean alternate = false;
            for (String[] row : rows) {
                for (String val : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", CELL_FONT));
                    cell.setPadding(5);
                    if (alternate) cell.setBackgroundColor(new BaseColor(236, 240, 241));
                    table.addCell(cell);
                }
                alternate = !alternate;
            }

            doc.add(table);
            doc.close();
            Logger.log("INFO", "ReportGenerator", "Report saved: " + file_path);

        } catch (Exception e) {
            Logger.log("ERROR", "ReportGenerator", "Failed: " + title, e);
        }
    }
}
