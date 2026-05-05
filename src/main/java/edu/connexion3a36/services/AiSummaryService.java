package edu.connexion3a36.services;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import edu.connexion3a36.entities.Chapitre;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
public class AiSummaryService {

    // ── Replace with your actual Anthropic API key ─────────────────────────
    private static final String API_KEY   = "";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile"; // or "llama3-8b-8192"

    // ── Call Claude API and return summary text ────────────────────────────
    public String generateSummary(Chapitre chapitre) throws Exception {
        String prompt = buildPrompt(chapitre);
        String requestBody = buildRequestJson(prompt);

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setRequestProperty("Authorization",  "Bearer " + API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status != 200) {
            throw new RuntimeException("API error " + status + ": " + response);
        }

        return extractTextFromResponse(response);
    }

    // ── Build the structured prompt ────────────────────────────────────────
    private String buildPrompt(Chapitre ch) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an educational assistant. Create a clear, structured summary of this chapter.\n\n");
        sb.append("Chapter title: ").append(ch.getTitre()).append("\n");
        if (ch.getContentType() != null)
            sb.append("Content type: ").append(ch.getContentType()).append("\n");
        if (ch.getDurationMinutes() != null)
            sb.append("Duration: ").append(ch.getDurationMinutes()).append(" minutes\n");
        sb.append("\nChapter content:\n").append(ch.getContenu()).append("\n\n");
        sb.append("Please provide:\n");
        sb.append("1. A 2-3 sentence overview of the chapter\n");
        sb.append("2. Key concepts (as bullet points, each starting with •)\n");
        sb.append("3. Main takeaways (as bullet points, each starting with ✓)\n");
        sb.append("4. A one-sentence conclusion\n\n");
        sb.append("Format cleanly. Write in French if the content is in French, English otherwise.");
        return sb.toString();
    }

    // ── Minimal JSON builder (no external library needed) ─────────────────
    private String buildRequestJson(String prompt) {
        String escaped = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"max_completion_tokens\":1024,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}]"
                + "}";
    }

    // ── Extract the text block from Claude's JSON response ────────────────
    private String extractTextFromResponse(String json) {
        // xAI/OpenAI format: choices[0].message.content
        int contentIdx = json.indexOf("\"content\":");
        if (contentIdx == -1) throw new RuntimeException("Unexpected API response: " + json);
        int start = json.indexOf("\"", contentIdx + 10) + 1;
        int end   = findJsonStringEnd(json, start);
        String raw = json.substring(start, end);
        return raw.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\t", "\t");
    }

    private int findJsonStringEnd(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"')  return i;
        }
        return json.length();
    }

    // ── Generate a nicely formatted PDF ───────────────────────────────────
    public File generatePdf(Chapitre chapitre, String summaryText, String destFolder) throws Exception {
        String safeTitle = chapitre.getTitre().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String fileName  = "Résumé_" + safeTitle + "_" + 
                           LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
        File outFile = new File(destFolder, fileName);

        PdfWriter   writer   = new PdfWriter(outFile);
        PdfDocument pdfDoc   = new PdfDocument(writer);
        Document    document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 50, 40, 50);

        PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        DeviceRgb primaryBlue  = new DeviceRgb(41,  121, 255);
        DeviceRgb lightBlue    = new DeviceRgb(232, 240, 254);
        DeviceRgb darkText     = new DeviceRgb(26,  26,  46);
        DeviceRgb mutedText    = new DeviceRgb(117, 117, 117);
        DeviceRgb accentGreen  = new DeviceRgb(46,  125, 50);

        // ── Header banner ─────────────────────────────────────────────────
        Table banner = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth();
        Cell bannerCell = new Cell()
                .add(new Paragraph("📚 StudyFlow — Résumé IA")
                        .setFont(fontBold).setFontSize(11)
                        .setFontColor(new DeviceRgb(255, 255, 255)))
                .setBackgroundColor(primaryBlue)
                .setPadding(10)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        banner.addCell(bannerCell);
        document.add(banner);
        document.add(new Paragraph(" "));

        // ── Chapter title ─────────────────────────────────────────────────
        document.add(new Paragraph(chapitre.getTitre())
                .setFont(fontBold)
                .setFontSize(22)
                .setFontColor(darkText)
                .setMarginTop(8)
                .setMarginBottom(4));

        // ── Meta chips row ────────────────────────────────────────────────
        StringBuilder meta = new StringBuilder("Résumé généré par IA");
        if (chapitre.getDurationMinutes() != null)
            meta.append("  •  ⏱ ").append(chapitre.getDurationMinutes()).append(" min");
        if (chapitre.getContentType() != null)
            meta.append("  •  ").append(chapitre.getContentType());
        meta.append("  •  ").append(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        document.add(new Paragraph(meta.toString())
                .setFont(fontRegular)
                .setFontSize(9)
                .setFontColor(mutedText)
                .setMarginBottom(16));

        // ── Divider ───────────────────────────────────────────────────────
        document.add(new LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
                .setMarginBottom(16));

        // ── AI summary content ────────────────────────────────────────────
        String[] lines = summaryText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                document.add(new Paragraph(" ").setFontSize(5));
                continue;
            }

            // Numbered section headers like "1.", "2.", "3.", "4."
            if (trimmed.matches("^\\d+\\..*")) {
                Table sectionBg = new Table(UnitValue.createPercentArray(new float[]{1}))
                        .useAllAvailableWidth()
                        .setMarginTop(10)
                        .setMarginBottom(4);
                Cell sc = new Cell()
                        .add(new Paragraph(trimmed)
                                .setFont(fontBold)
                                .setFontSize(12)
                                .setFontColor(primaryBlue))
                        .setBackgroundColor(lightBlue)
                        .setPadding(8)
                        .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                        .setBorderLeft(new com.itextpdf.layout.borders.SolidBorder(primaryBlue, 3));
                sectionBg.addCell(sc);
                document.add(sectionBg);
                continue;
            }

            // Key concept bullets  •
            if (trimmed.startsWith("•")) {
                document.add(new Paragraph("  " + trimmed)
                        .setFont(fontRegular)
                        .setFontSize(11)
                        .setFontColor(darkText)
                        .setMarginLeft(10)
                        .setMarginBottom(3));
                continue;
            }

            // Takeaway bullets  ✓
            if (trimmed.startsWith("✓")) {
                document.add(new Paragraph("  " + trimmed)
                        .setFont(fontBold)
                        .setFontSize(11)
                        .setFontColor(accentGreen)
                        .setMarginLeft(10)
                        .setMarginBottom(3));
                continue;
            }

            // Normal paragraph
            document.add(new Paragraph(trimmed)
                    .setFont(fontRegular)
                    .setFontSize(11)
                    .setFontColor(darkText)
                    .setMarginBottom(4));
        }

        // ── Footer ────────────────────────────────────────────────────────
        document.add(new Paragraph(" "));
        document.add(new LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)));
        document.add(new Paragraph("Généré automatiquement par StudyFlow AI · " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                .setFont(fontRegular)
                .setFontSize(8)
                .setFontColor(mutedText)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6));

        document.close();
        return outFile;
    }

    public String askQuestion(Chapitre chapitre, String question, List<String[]> history) throws Exception {

        // Build system prompt with chapter content as context
        String systemPrompt = "You are a helpful educational assistant. " +
                "You help students understand chapter content by answering their questions clearly and concisely. " +
                "Always base your answers on the chapter content provided. " +
                "If the question is not related to the chapter, politely redirect to the chapter topic. " +
                "Keep answers focused, friendly, and educational. " +
                "Write in French if the content is in French, English otherwise.\n\n" +
                "=== CHAPTER CONTEXT ===\n" +
                "Title: " + chapitre.getTitre() + "\n" +
                "Content: " + chapitre.getContenu() + "\n" +
                (chapitre.getDurationMinutes() != null ? "Duration: " + chapitre.getDurationMinutes() + " min\n" : "") +
                "======================";

        // Build messages array with full conversation history
        StringBuilder messagesJson = new StringBuilder("[");

        // System message first
        messagesJson.append("{\"role\":\"system\",\"content\":\"")
                .append(escapeJson(systemPrompt))
                .append("\"}");

        // Previous conversation turns
        for (String[] turn : history) {
            messagesJson.append(",{\"role\":\"")
                    .append(turn[0])
                    .append("\",\"content\":\"")
                    .append(escapeJson(turn[1]))
                    .append("\"}");
        }

        messagesJson.append("]");

        String requestBody = "{" +
                "\"model\":\"" + MODEL + "\"," +
                "\"max_completion_tokens\":512," +
                "\"messages\":" + messagesJson +
                "}";

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300)
                ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status != 200)
            throw new RuntimeException("API error " + status + ": " + response);

        return extractTextFromResponse(response);
    }

    // ── JSON escape helper (add this private method too) ──────────────────
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
