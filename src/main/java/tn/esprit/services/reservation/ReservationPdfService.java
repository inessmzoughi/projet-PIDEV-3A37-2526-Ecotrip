package tn.esprit.services.reservation;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import tn.esprit.models.Reservation;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.models.enums.ReservationType;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationPdfService {

    // ── EcoTrip brand colors ──────────────────────────────────────────────
    private static final DeviceRgb GREEN_DARK   = new DeviceRgb(13,  61,  24);
    private static final DeviceRgb GREEN_MED    = new DeviceRgb(26,  95,  42);
    private static final DeviceRgb GREEN_LIGHT  = new DeviceRgb(240, 253, 244);
    private static final DeviceRgb AMBER        = new DeviceRgb(180, 83,  9);
    private static final DeviceRgb AMBER_LIGHT  = new DeviceRgb(254, 243, 199);
    private static final DeviceRgb BLUE_DARK    = new DeviceRgb(30,  58,  95);
    private static final DeviceRgb BLUE_LIGHT   = new DeviceRgb(239, 246, 255);
    private static final DeviceRgb RED_DARK     = new DeviceRgb(124, 45,  18);
    private static final DeviceRgb RED_LIGHT    = new DeviceRgb(254, 242, 242);
    private static final DeviceRgb SLATE_50     = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb SLATE_200    = new DeviceRgb(226, 232, 240);
    private static final DeviceRgb SLATE_400    = new DeviceRgb(148, 163, 184);
    private static final DeviceRgb SLATE_700    = new DeviceRgb(51,  65,  85);
    private static final DeviceRgb WHITE        = new DeviceRgb(255, 255, 255);

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_LONG =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);

    private PdfFont fontBold;
    private PdfFont fontRegular;

    /**
     * Generate a professional PDF report for the given reservations.
     *
     * @param reservations list of reservations to include
     * @param outputFile   destination file
     * @param reportTitle  title shown on the cover page
     */
    public void generate(List<Reservation> reservations,
                         File outputFile,
                         String reportTitle) throws Exception {

        fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        PdfDocument pdf = new PdfDocument(new PdfWriter(outputFile));
        Document doc    = new Document(pdf, PageSize.A4);
        doc.setMargins(0, 0, 0, 0); // we handle margins per-element

        // ── Page 1: Cover ──────────────────────────────────────────────────
        addCoverPage(doc, pdf, reservations, reportTitle);

        // ── Page 2: Statistics ─────────────────────────────────────────────
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addStatsPage(doc, pdf, reservations);

        // ── Page 3+: Data table ────────────────────────────────────────────
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addTablePage(doc, pdf, reservations);

        doc.close();
    }

    // ═══════════════════════════════════════════════════
    //  PAGE 1 — COVER
    // ═══════════════════════════════════════════════════

    private void addCoverPage(Document doc, PdfDocument pdf,
                              List<Reservation> reservations,
                              String reportTitle) throws Exception {

        PdfCanvas canvas = new PdfCanvas(pdf.addNewPage());
        float W = PageSize.A4.getWidth();
        float H = PageSize.A4.getHeight();

        // Dark green top band
        canvas.setFillColor(GREEN_DARK)
                .rectangle(0, H - 220, W, 220)
                .fill();

        // Decorative green strip at bottom
        canvas.setFillColor(GREEN_MED)
                .rectangle(0, 0, W, 12)
                .fill();

        // Small accent rect
        canvas.setFillColor(new DeviceRgb(74, 222, 128))
                .rectangle(0, H - 228, 6, 228)
                .fill();

        canvas.release();

        // ── Content inside the cover ───────────────────
        // Brand name
        Paragraph brand = new Paragraph("ECOTRIP")
                .setFont(fontBold)
                .setFontSize(13)
                .setFontColor(new DeviceRgb(74, 222, 128))
                .setMarginTop(40)
                .setMarginLeft(50)
                .setCharacterSpacing(4);
        doc.add(brand);

        // Report title
        Paragraph title = new Paragraph(reportTitle)
                .setFont(fontBold)
                .setFontSize(32)
                .setFontColor(WHITE)
                .setMarginLeft(50)
                .setMarginTop(8)
                .setMarginBottom(4);
        doc.add(title);

        // Subtitle
        Paragraph subtitle = new Paragraph(
                "Rapport généré le " + LocalDate.now().format(FMT_LONG))
                .setFont(fontRegular)
                .setFontSize(13)
                .setFontColor(new DeviceRgb(148, 200, 160))
                .setMarginLeft(50)
                .setMarginBottom(60);
        doc.add(subtitle);

        // ── Cover summary boxes ────────────────────────
        long total     = reservations.size();
        long confirmed = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
        long pending   = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING).count();
        double revenue = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .mapToDouble(Reservation::getTotalPrice).sum();

        Table summaryBoxes = new Table(UnitValue.createPercentArray(new float[]{1,1,1,1}))
                .useAllAvailableWidth()
                .setMarginLeft(40)
                .setMarginRight(40)
                .setMarginTop(20);

        summaryBoxes.addCell(coverBox("📋", String.valueOf(total),
                "Réservations totales", SLATE_50, SLATE_700));
        summaryBoxes.addCell(coverBox("✅", String.valueOf(confirmed),
                "Confirmées", GREEN_LIGHT, GREEN_MED));
        summaryBoxes.addCell(coverBox("⏳", String.valueOf(pending),
                "En attente", AMBER_LIGHT, AMBER));
        summaryBoxes.addCell(coverBox("💰",
                String.format("%.0f TND", revenue),
                "Revenus confirmés", BLUE_LIGHT, BLUE_DARK));

        doc.add(summaryBoxes);

        // ── Cover description ──────────────────────────
        Paragraph desc = new Paragraph(
                "Ce rapport présente une analyse complète des réservations EcoTrip "
                        + "incluant les statistiques par type, par statut, les revenus générés "
                        + "et le détail complet de chaque réservation.")
                .setFont(fontRegular)
                .setFontSize(12)
                .setFontColor(SLATE_700)
                .setMarginLeft(50)
                .setMarginRight(50)
                .setMarginTop(40)
                .setTextAlignment(TextAlignment.JUSTIFIED);
        doc.add(desc);

        // ── Cover table of contents ────────────────────
        Paragraph tocTitle = new Paragraph("Table des matières")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(GREEN_DARK)
                .setMarginLeft(50)
                .setMarginTop(30)
                .setMarginBottom(10);
        doc.add(tocTitle);

        String[][] toc = {
                {"01", "Statistiques et insights", "Page 2"},
                {"02", "Tableau des réservations", "Page 3+"}
        };
        for (String[] row : toc) {
            Table tocRow = new Table(
                    UnitValue.createPercentArray(new float[]{0.5f, 8f, 1.5f}))
                    .useAllAvailableWidth()
                    .setMarginLeft(50)
                    .setMarginRight(50)
                    .setMarginBottom(4);
            tocRow.addCell(tocCell(row[0], fontBold, GREEN_DARK));
            tocRow.addCell(tocCell(row[1], fontRegular, SLATE_700));
            tocRow.addCell(tocCell(row[2], fontRegular, SLATE_400)
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(tocRow);
        }

        // ── Footer line ────────────────────────────────
        Paragraph footer = new Paragraph(
                "EcoTrip Platform  ·  Rapport de réservations  ·  Confidentiel")
                .setFont(fontRegular)
                .setFontSize(9)
                .setFontColor(SLATE_400)
                .setMarginLeft(50)
                .setMarginTop(60)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(footer);
    }

    private Cell coverBox(String icon, String value, String label,
                          DeviceRgb bg, DeviceRgb fg) {
        Cell cell = new Cell()
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setPadding(16)
                .setMargin(4);

        cell.add(new Paragraph(value)
                .setFont(fontBold)
                .setFontSize(24)
                .setFontColor(fg)
                .setMarginBottom(2));
        cell.add(new Paragraph(label)
                .setFont(fontRegular)
                .setFontSize(10)
                .setFontColor(fg)
                .setMarginBottom(0));
        return cell;
    }

    private Cell tocCell(String text, PdfFont font, DeviceRgb color) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(12)
                        .setFontColor(color));
    }

    // ═══════════════════════════════════════════════════
    //  PAGE 2 — STATISTICS
    // ═══════════════════════════════════════════════════

    private void addStatsPage(Document doc, PdfDocument pdf,
                              List<Reservation> reservations) throws Exception {

        addPageHeader(doc, pdf, "Statistiques & Insights");

        int total      = reservations.size();
        long heb       = count(reservations, ReservationType.HEBERGEMENT);
        long act       = count(reservations, ReservationType.ACTIVITY);
        long trans     = count(reservations, ReservationType.TRANSPORT);
        long confirmed = countStatus(reservations, ReservationStatus.CONFIRMED);
        long pending   = countStatus(reservations, ReservationStatus.PENDING);
        long cancelled = countStatus(reservations, ReservationStatus.CANCELLED);
        double revenue = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .mapToDouble(Reservation::getTotalPrice).sum();
        double avgTotal = total == 0 ? 0 :
                reservations.stream().mapToDouble(Reservation::getTotalPrice).average().orElse(0);
        double maxTotal = reservations.stream()
                .mapToDouble(Reservation::getTotalPrice).max().orElse(0);

        // ── Section: Key metrics ───────────────────────
        addSectionTitle(doc, "01  Métriques clés");

        Table metrics = new Table(UnitValue.createPercentArray(new float[]{1,1,1}))
                .useAllAvailableWidth()
                .setMarginLeft(40).setMarginRight(40).setMarginBottom(24);

        metrics.addCell(metricCard("📋 Total réservations",
                String.valueOf(total), "toutes périodes confondues",
                SLATE_50, SLATE_700));
        metrics.addCell(metricCard("💰 Revenus confirmés",
                String.format("%.2f TND", revenue), "réservations confirmées uniquement",
                GREEN_LIGHT, GREEN_DARK));
        metrics.addCell(metricCard("📊 Prix moyen",
                String.format("%.2f TND", avgTotal), "par réservation",
                BLUE_LIGHT, BLUE_DARK));
        metrics.addCell(metricCard("🏆 Prix maximum",
                String.format("%.2f TND", maxTotal), "réservation la plus chère",
                AMBER_LIGHT, AMBER));
        metrics.addCell(metricCard("✅ Taux de confirmation",
                total == 0 ? "0%" : String.format("%.0f%%", (double)confirmed/total*100),
                confirmed + " confirmées sur " + total, GREEN_LIGHT, GREEN_DARK));
        metrics.addCell(metricCard("❌ Taux d'annulation",
                total == 0 ? "0%" : String.format("%.0f%%", (double)cancelled/total*100),
                cancelled + " annulées sur " + total, RED_LIGHT, RED_DARK));

        doc.add(metrics);

        // ── Section: By type bar chart ─────────────────
        addSectionTitle(doc, "02  Répartition par type");
        addBarChart(doc, pdf, new String[]{"🏨 Hébergements","🧭 Activités","🚌 Transports"},
                new long[]{heb, act, trans}, total,
                new DeviceRgb[]{GREEN_DARK, BLUE_DARK, RED_DARK});

        // ── Section: By status bar chart ───────────────
        addSectionTitle(doc, "03  Répartition par statut");
        addBarChart(doc, pdf,
                new String[]{"✅ Confirmées","⏳ En attente","❌ Annulées"},
                new long[]{confirmed, pending, cancelled}, total,
                new DeviceRgb[]{GREEN_MED, AMBER, RED_DARK});

        // ── Section: Monthly distribution ──────────────
        addSectionTitle(doc, "04  Distribution mensuelle (date d'arrivée)");
        addMonthlyChart(doc, pdf, reservations);

        // ── Section: Top persons ───────────────────────
        addSectionTitle(doc, "05  Distribution du nombre de personnes");
        addPersonsChart(doc, pdf, reservations);

        addPageFooter(doc);
    }

    private void addBarChart(Document doc, PdfDocument pdf,
                             String[] labels, long[] values, int total,
                             DeviceRgb[] colors) {
        Table chart = new Table(UnitValue.createPercentArray(new float[]{2.5f, 6f, 1f}))
                .useAllAvailableWidth()
                .setMarginLeft(40).setMarginRight(40).setMarginBottom(20);

        long max = Arrays.stream(values).max().orElse(1);

        for (int i = 0; i < labels.length; i++) {
            long   val = values[i];
            String pct = total == 0 ? "0%"
                    : String.format("%.0f%%", (double) val / total * 100);

            // Label cell
            chart.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPaddingRight(8).setPaddingTop(6)
                    .add(new Paragraph(labels[i])
                            .setFont(fontRegular).setFontSize(11).setFontColor(SLATE_700)));

            // Bar cell
            Table barRow = new Table(UnitValue.createPercentArray(
                    new float[]{(float) val / max * 100,
                            (float)(max - val) / max * 100}))
                    .useAllAvailableWidth();
            barRow.addCell(new Cell().setHeight(22)
                    .setBackgroundColor(colors[i]).setBorder(Border.NO_BORDER));
            if (val < max) {
                barRow.addCell(new Cell().setHeight(22)
                        .setBackgroundColor(SLATE_200).setBorder(Border.NO_BORDER));
            }
            chart.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPaddingTop(4).add(barRow));

            // Value cell
            chart.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPaddingLeft(8).setPaddingTop(6)
                    .add(new Paragraph(val + "  (" + pct + ")")
                            .setFont(fontBold).setFontSize(11).setFontColor(SLATE_700)
                            .setTextAlignment(TextAlignment.RIGHT)));
        }
        doc.add(chart);
    }

    private void addMonthlyChart(Document doc, PdfDocument pdf,
                                 List<Reservation> reservations) {
        // Group by month of dateFrom
        Map<String, Long> monthly = reservations.stream()
                .filter(r -> r.getDateFrom() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getDateFrom().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                        TreeMap::new, Collectors.counting()));

        if (monthly.isEmpty()) {
            doc.add(new Paragraph("Aucune donnée disponible.")
                    .setFont(fontRegular).setFontSize(11).setFontColor(SLATE_400)
                    .setMarginLeft(40).setMarginBottom(16));
            return;
        }

        long max = monthly.values().stream().mapToLong(Long::longValue).max().orElse(1);
        int  totalCount = reservations.size();

        Table chart = new Table(UnitValue.createPercentArray(new float[]{2f, 6f, 1f}))
                .useAllAvailableWidth()
                .setMarginLeft(40).setMarginRight(40).setMarginBottom(20);

        monthly.forEach((month, count) -> {
            chart.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingTop(5)
                    .add(new Paragraph(month)
                            .setFont(fontRegular).setFontSize(11).setFontColor(SLATE_700)));

            float ratio = (float) count / max;
            Table bar = new Table(ratio < 1
                    ? UnitValue.createPercentArray(new float[]{ratio * 100, (1 - ratio) * 100})
                    : UnitValue.createPercentArray(new float[]{100f}))
                    .useAllAvailableWidth();
            bar.addCell(new Cell().setHeight(20)
                    .setBackgroundColor(GREEN_MED).setBorder(Border.NO_BORDER));
            if (ratio < 1)
                bar.addCell(new Cell().setHeight(20)
                        .setBackgroundColor(SLATE_200).setBorder(Border.NO_BORDER));
            chart.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingTop(3).add(bar));

            String pct = String.format("%.0f%%", (double) count / totalCount * 100);
            chart.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPaddingLeft(8).setPaddingTop(5)
                    .add(new Paragraph(count + " (" + pct + ")")
                            .setFont(fontBold).setFontSize(11).setFontColor(SLATE_700)
                            .setTextAlignment(TextAlignment.RIGHT)));
        });

        doc.add(chart);
    }

    private void addPersonsChart(Document doc, PdfDocument pdf,
                                 List<Reservation> reservations) {
        Map<Integer, Long> persons = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getNumberOfPersons,
                        TreeMap::new, Collectors.counting()));

        if (persons.isEmpty()) { return; }

        long max = persons.values().stream().mapToLong(Long::longValue).max().orElse(1);
        int  totalCount = reservations.size();

        Table chart = new Table(UnitValue.createPercentArray(new float[]{1.5f, 6f, 1.5f}))
                .useAllAvailableWidth()
                .setMarginLeft(40).setMarginRight(40).setMarginBottom(20);

        persons.forEach((n, count) -> {
            chart.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingTop(5)
                    .add(new Paragraph(n + " pers.")
                            .setFont(fontRegular).setFontSize(11).setFontColor(SLATE_700)));

            float ratio = (float) count / max;
            Table bar = new Table(ratio < 1
                    ? UnitValue.createPercentArray(new float[]{ratio*100, (1-ratio)*100})
                    : UnitValue.createPercentArray(new float[]{100f}))
                    .useAllAvailableWidth();
            bar.addCell(new Cell().setHeight(18)
                    .setBackgroundColor(BLUE_DARK).setBorder(Border.NO_BORDER));
            if (ratio < 1)
                bar.addCell(new Cell().setHeight(18)
                        .setBackgroundColor(SLATE_200).setBorder(Border.NO_BORDER));
            chart.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingTop(2).add(bar));

            String pct = String.format("%.0f%%", (double)count/totalCount*100);
            chart.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPaddingLeft(8).setPaddingTop(5)
                    .add(new Paragraph(count + " (" + pct + ")")
                            .setFont(fontBold).setFontSize(11).setFontColor(SLATE_700)
                            .setTextAlignment(TextAlignment.RIGHT)));
        });

        doc.add(chart);
    }

    private Cell metricCard(String label, String value, String sub,
                            DeviceRgb bg, DeviceRgb fg) {
        Cell cell = new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(SLATE_200, 1))
                .setPadding(14)
                .setMargin(4);
        cell.add(new Paragraph(label)
                .setFont(fontBold).setFontSize(10).setFontColor(fg)
                .setMarginBottom(4));
        cell.add(new Paragraph(value)
                .setFont(fontBold).setFontSize(20).setFontColor(fg)
                .setMarginBottom(2));
        cell.add(new Paragraph(sub)
                .setFont(fontRegular).setFontSize(9).setFontColor(SLATE_400));
        return cell;
    }

    // ═══════════════════════════════════════════════════
    //  PAGE 3+ — DATA TABLE
    // ═══════════════════════════════════════════════════

    private void addTablePage(Document doc, PdfDocument pdf,
                              List<Reservation> reservations) throws Exception {

        addPageHeader(doc, pdf, "Détail des réservations");

        // Table header
        float[] colWidths = {0.6f, 1f, 1.4f, 2f, 1.2f, 1.2f, 0.8f, 1.2f, 1.4f};
        Table table = new Table(UnitValue.createPercentArray(colWidths))
                .useAllAvailableWidth()
                .setMarginLeft(30).setMarginRight(30)
                .setFontSize(9);

        // ── Header row ─────────────────────────────────
        String[] headers = {"#","Utilisateur","Type","Élément",
                "Arrivée","Départ","Pers.","Total","Statut"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(GREEN_DARK)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(9)
                    .add(new Paragraph(h)
                            .setFont(fontBold)
                            .setFontColor(WHITE)
                            .setFontSize(9)));
        }

        // ── Data rows ──────────────────────────────────
        boolean odd = true;
        for (Reservation r : reservations) {
            DeviceRgb rowBg = odd ? WHITE : SLATE_50;
            DeviceRgb statusBg;
            DeviceRgb statusFg;
            String statusText;

            switch (r.getStatus()) {
                case CONFIRMED -> {
                    statusBg = GREEN_LIGHT; statusFg = GREEN_DARK;
                    statusText = "Confirmee";
                }
                case PENDING   -> {
                    statusBg = AMBER_LIGHT; statusFg = AMBER;
                    statusText = "En attente";
                }
                default        -> {
                    statusBg = RED_LIGHT; statusFg = RED_DARK;
                    statusText = "Annulee";
                }
            }

            String typeIcon = switch (r.getReservationType()) {
                case HEBERGEMENT -> "HEB"; case ACTIVITY -> "ACT"; default -> "TRS";
            };
            DeviceRgb typeBg = switch (r.getReservationType()) {
                case HEBERGEMENT -> GREEN_LIGHT;
                case ACTIVITY    -> BLUE_LIGHT;
                case TRANSPORT   -> RED_LIGHT;
            };
            DeviceRgb typeFg = switch (r.getReservationType()) {
                case HEBERGEMENT -> GREEN_DARK;
                case ACTIVITY    -> BLUE_DARK;
                case TRANSPORT   -> RED_DARK;
            };

            table.addCell(dataCell("#" + r.getId(), rowBg, false));
            table.addCell(dataCell( r.getUsername(r.getUserId()), rowBg, false));
            // Type with colored badge
            table.addCell(new Cell()
                    .setBackgroundColor(rowBg).setBorder(Border.NO_BORDER).setPadding(7)
                    .add(new Paragraph(typeIcon)
                            .setFont(fontBold).setFontSize(8)
                            .setFontColor(typeFg)
                            .setBackgroundColor(typeBg)
                            .setPadding(3)));
            table.addCell(dataCell(resolveLabel(r), rowBg, false));
            table.addCell(dataCell(r.getDateFrom() != null
                    ? r.getDateFrom().format(FMT) : "—", rowBg, false));
            table.addCell(dataCell(r.getDateTo() != null
                    ? r.getDateTo().format(FMT) : "—", rowBg, false));
            table.addCell(dataCell(String.valueOf(r.getNumberOfPersons()), rowBg, true));
            table.addCell(dataCell(
                    String.format("%.2f TND", r.getTotalPrice()), rowBg, true));
            // Status with colored badge
            table.addCell(new Cell()
                    .setBackgroundColor(rowBg).setBorder(Border.NO_BORDER).setPadding(7)
                    .add(new Paragraph(statusText)
                            .setFont(fontBold).setFontSize(8)
                            .setFontColor(statusFg)
                            .setBackgroundColor(statusBg)
                            .setPadding(3)));

            odd = !odd;
        }

        doc.add(table);

        // ── Summary row ────────────────────────────────
        double grandTotal = reservations.stream()
                .mapToDouble(Reservation::getTotalPrice).sum();
        double revenue = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .mapToDouble(Reservation::getTotalPrice).sum();

        Table summary = new Table(UnitValue.createPercentArray(new float[]{6f, 1.2f, 1.4f}))
                .useAllAvailableWidth()
                .setMarginLeft(30).setMarginRight(30).setMarginTop(8);

        summary.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(8)
                .add(new Paragraph(reservations.size() + " réservation(s) au total")
                        .setFont(fontBold).setFontSize(10).setFontColor(SLATE_700)));
        summary.addCell(new Cell()
                .setBackgroundColor(SLATE_50)
                .setBorder(new SolidBorder(SLATE_200, 1)).setPadding(8)
                .add(new Paragraph(String.format("%.2f TND", grandTotal))
                        .setFont(fontBold).setFontSize(10).setFontColor(SLATE_700)
                        .setTextAlignment(TextAlignment.CENTER)));
        summary.addCell(new Cell()
                .setBackgroundColor(GREEN_LIGHT)
                .setBorder(new SolidBorder(GREEN_MED, 1)).setPadding(8)
                .add(new Paragraph("Revenus: " + String.format("%.2f TND", revenue))
                        .setFont(fontBold).setFontSize(10).setFontColor(GREEN_DARK)
                        .setTextAlignment(TextAlignment.CENTER)));
        doc.add(summary);

        addPageFooter(doc);
    }

    private Cell dataCell(String text, DeviceRgb bg, boolean centered) {
        return new Cell()
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setPadding(7)
                .add(new Paragraph(text)
                        .setFont(fontRegular)
                        .setFontSize(9)
                        .setFontColor(SLATE_700)
                        .setTextAlignment(centered
                                ? TextAlignment.CENTER : TextAlignment.LEFT));
    }

    // ═══════════════════════════════════════════════════
    //  SHARED LAYOUT HELPERS
    // ═══════════════════════════════════════════════════

    private void addPageHeader(Document doc, PdfDocument pdf,
                               String pageTitle) throws Exception {
        PdfCanvas canvas = new PdfCanvas(
                pdf.getPage(pdf.getNumberOfPages()));
        float W = PageSize.A4.getWidth();

        // Green top strip
        canvas.setFillColor(GREEN_DARK)
                .rectangle(0, PageSize.A4.getHeight() - 56, W, 56)
                .fill();
        canvas.setFillColor(new DeviceRgb(74, 222, 128))
                .rectangle(0, PageSize.A4.getHeight() - 60, 4, 60)
                .fill();
        canvas.release();

        // Brand + title row
        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 3}))
                .useAllAvailableWidth()
                .setMarginLeft(30).setMarginRight(30);

        header.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(12)
                .add(new Paragraph("ECOTRIP")
                        .setFont(fontBold).setFontSize(11).setFontColor(WHITE)
                        .setCharacterSpacing(2)));
        header.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(12)
                .add(new Paragraph(pageTitle)
                        .setFont(fontBold).setFontSize(14).setFontColor(WHITE)
                        .setTextAlignment(TextAlignment.RIGHT)));
        doc.add(header);

        doc.add(new Paragraph(" ").setMarginTop(16)); // spacer
    }

    private void addPageFooter(Document doc) {
        Paragraph footer = new Paragraph(
                "EcoTrip  ·  Document confidentiel  ·  Généré le "
                        + LocalDate.now().format(FMT))
                .setFont(fontRegular)
                .setFontSize(8)
                .setFontColor(SLATE_400)
                .setMarginLeft(30).setMarginTop(20)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(footer);
    }

    private void addSectionTitle(Document doc, String title) {
        Paragraph p = new Paragraph(title)
                .setFont(fontBold)
                .setFontSize(13)
                .setFontColor(GREEN_DARK)
                .setMarginLeft(40)
                .setMarginTop(20)
                .setMarginBottom(10)
                .setBorderBottom(new SolidBorder(GREEN_MED, 2))
                .setPaddingBottom(6);
        doc.add(p);
    }

    // ═══════════════════════════════════════════════════
    //  DATA HELPERS
    // ═══════════════════════════════════════════════════

    private long count(List<Reservation> list, ReservationType type) {
        return list.stream()
                .filter(r -> r.getReservationType() == type).count();
    }

    private long countStatus(List<Reservation> list, ReservationStatus status) {
        return list.stream()
                .filter(r -> r.getStatus() == status).count();
    }

    private String resolveLabel(Reservation r) {
        if (r.getDetails() != null) {
            Object name = r.getDetails().get(switch (r.getReservationType()) {
                case HEBERGEMENT -> "hebergementNom";
                case ACTIVITY    -> "activityTitle";
                case TRANSPORT   -> "transportType";
            });
            if (name != null) return name.toString();
        }
        return r.getReservationType().name() + " #" + r.getReservationId();
    }
}