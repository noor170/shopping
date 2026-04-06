package com.example.taskmanagement.service;

import com.example.taskmanagement.entity.Task;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class TaskExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public byte[] exportPdf(List<Task> tasks, String actor) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = 800;
                contentStream.beginText();
                contentStream.setFont(titleFont, 16);
                contentStream.newLineAtOffset(40, y);
                contentStream.showText("Task Export");
                contentStream.endText();

                y -= 24;
                contentStream.beginText();
                contentStream.setFont(bodyFont, 10);
                contentStream.newLineAtOffset(40, y);
                contentStream.showText("Generated for: " + actor);
                contentStream.endText();

                y -= 28;
                for (Task task : tasks) {
                    if (y < 80) {
                        break;
                    }

                    writeLine(contentStream, titleFont, 12, 40, y, "#" + task.getId() + " " + sanitize(task.getTitle(), 70));
                    y -= 16;
                    writeLine(contentStream, bodyFont, 10, 40, y,
                            "Status: " + task.getStatus()
                                    + " | Priority: " + task.getPriority()
                                    + " | Deadline: " + formatDate(task));
                    y -= 14;
                    writeLine(contentStream, bodyFont, 10, 40, y,
                            "Owner: " + task.getOwner().getUsername()
                                    + " | Updated: " + task.getUpdatedAt());
                    y -= 14;
                    writeLine(contentStream, bodyFont, 10, 40, y,
                            "Description: " + sanitize(task.getDescription(), 90));
                    y -= 20;
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to export tasks as PDF", ex);
        }
    }

    public byte[] exportExcel(List<Task> tasks) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Tasks");

            String[] headers = {"ID", "Title", "Description", "Status", "Priority", "Deadline", "Owner", "Updated At"};
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }

            int rowIndex = 1;
            for (Task task : tasks) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(task.getId());
                row.createCell(1).setCellValue(defaultString(task.getTitle()));
                row.createCell(2).setCellValue(defaultString(task.getDescription()));
                row.createCell(3).setCellValue(task.getStatus().name());
                row.createCell(4).setCellValue(task.getPriority().name());
                row.createCell(5).setCellValue(formatDate(task));
                row.createCell(6).setCellValue(task.getOwner().getUsername());
                row.createCell(7).setCellValue(task.getUpdatedAt() == null ? "" : task.getUpdatedAt().toString());
            }

            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to export tasks as Excel", ex);
        }
    }

    private void writeLine(PDPageContentStream contentStream, PDType1Font font, int size, float x, float y, String text)
            throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, size);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private String formatDate(Task task) {
        return task.getDeadline() == null ? "-" : DATE_FORMATTER.format(task.getDeadline());
    }

    private String sanitize(String value, int maxLength) {
        String normalized = defaultString(value)
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3) + "...";
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
