package edu.connexion3a36.services;

import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.ChapitreVersion;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChapitreVersionService {

    private final Connection cnx;

    public ChapitreVersionService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ── Save a snapshot BEFORE update ────────────────────────────────────────
    public void saveVersion(Chapitre before, Chapitre after) {
        int nextVersion = getNextVersionNumber(before.getId());
        String changes  = detectChanges(before, after);
        double pct      = computeModificationPercentage(before, after);

        String sql = """
            INSERT INTO chapitre_version
              (chapitre_id, version_number, titre, contenu, ordre,
               content_type, video_url, file_name, image_url, duration_minutes,
               created_at, change_description, changes_detected,
               modification_percentage, modified_by)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setLong(1,   before.getId());
            pst.setInt(2,    nextVersion);
            pst.setString(3, before.getTitre());
            pst.setString(4, before.getContenu());
            pst.setInt(5,    before.getOrdre());
            pst.setString(6, before.getContentType());
            pst.setString(7, before.getVideoUrl());
            pst.setString(8, before.getFileName());
            pst.setString(9, before.getImageUrl());
            if (before.getDurationMinutes() != null)
                pst.setInt(10, before.getDurationMinutes());
            else
                pst.setNull(10, Types.INTEGER);
            pst.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            pst.setString(12, buildChangeDescription(before, after));
            pst.setString(13, changes);
            pst.setDouble(14, pct);
            pst.setString(15, "Admin");   // replace with logged-in user name later
            pst.executeUpdate();
            System.out.println("✅ Version " + nextVersion + " sauvegardée pour chapitre ID " + before.getId());
        } catch (SQLException e) {
            System.err.println("❌ Erreur saveVersion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Load all versions for a chapitre ─────────────────────────────────────
    public List<ChapitreVersion> findByChapitreId(long chapitreId) {
        List<ChapitreVersion> list = new ArrayList<>();
        String sql = "SELECT * FROM chapitre_version WHERE chapitre_id = ? ORDER BY version_number DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setLong(1, chapitreId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Erreur findByChapitreId: " + e.getMessage());
        }
        return list;
    }

    // ── Delete all versions for a chapitre ───────────────────────────────────
    public void deleteByChapitreId(long chapitreId) {
        try (PreparedStatement pst = cnx.prepareStatement(
                "DELETE FROM chapitre_version WHERE chapitre_id = ?")) {
            pst.setLong(1, chapitreId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur deleteByChapitreId: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int getNextVersionNumber(long chapitreId) {
        String sql = "SELECT COALESCE(MAX(version_number), 0) + 1 FROM chapitre_version WHERE chapitre_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setLong(1, chapitreId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getNextVersionNumber: " + e.getMessage());
        }
        return 1;
    }

    private String detectChanges(Chapitre before, Chapitre after) {
        // Simple JSON-like string listing changed fields
        StringBuilder sb = new StringBuilder("[");
        if (!eq(before.getTitre(),       after.getTitre()))       sb.append("\"titre\",");
        if (!eq(before.getContenu(),     after.getContenu()))     sb.append("\"contenu\",");
        if (before.getOrdre() != after.getOrdre())               sb.append("\"ordre\",");
        if (!eq(before.getContentType(), after.getContentType())) sb.append("\"contentType\",");
        if (!eq(before.getVideoUrl(),    after.getVideoUrl()))    sb.append("\"videoUrl\",");
        if (!eq(before.getFileName(),    after.getFileName()))    sb.append("\"fileName\",");
        if (!eq(before.getImageUrl(),    after.getImageUrl()))    sb.append("\"imageUrl\",");
        if (!eqInt(before.getDurationMinutes(), after.getDurationMinutes())) sb.append("\"duration\",");
        String s = sb.toString();
        if (s.endsWith(",")) s = s.substring(0, s.length() - 1);
        return s + "]";
    }

    private String buildChangeDescription(Chapitre before, Chapitre after) {
        List<String> parts = new ArrayList<>();
        if (!eq(before.getTitre(),       after.getTitre()))       parts.add("Titre modifié");
        if (!eq(before.getContenu(),     after.getContenu()))     parts.add("Contenu modifié");
        if (before.getOrdre() != after.getOrdre())               parts.add("Ordre: " + before.getOrdre() + "→" + after.getOrdre());
        if (!eq(before.getContentType(), after.getContentType())) parts.add("Type modifié");
        if (!eq(before.getVideoUrl(),    after.getVideoUrl()))    parts.add("Vidéo modifiée");
        if (!eq(before.getFileName(),    after.getFileName()))    parts.add("Fichier modifié");
        if (!eq(before.getImageUrl(),    after.getImageUrl()))    parts.add("Image modifiée");
        if (!eqInt(before.getDurationMinutes(), after.getDurationMinutes())) parts.add("Durée modifiée");
        return parts.isEmpty() ? "Aucune modification détectée" : String.join(", ", parts);
    }

    private double computeModificationPercentage(Chapitre before, Chapitre after) {
        int total = 8, changed = 0;
        if (!eq(before.getTitre(),       after.getTitre()))       changed++;
        if (!eq(before.getContenu(),     after.getContenu()))     changed++;
        if (before.getOrdre() != after.getOrdre())               changed++;
        if (!eq(before.getContentType(), after.getContentType())) changed++;
        if (!eq(before.getVideoUrl(),    after.getVideoUrl()))    changed++;
        if (!eq(before.getFileName(),    after.getFileName()))    changed++;
        if (!eq(before.getImageUrl(),    after.getImageUrl()))    changed++;
        if (!eqInt(before.getDurationMinutes(), after.getDurationMinutes())) changed++;
        return Math.round((changed * 100.0 / total) * 10.0) / 10.0;
    }

    private boolean eq(String a, String b) {
        return (a == null ? "" : a).equals(b == null ? "" : b);
    }
    private boolean eqInt(Integer a, Integer b) {
        return (a == null ? 0 : a) == (b == null ? 0 : b);
    }

    private ChapitreVersion mapRow(ResultSet rs) throws SQLException {
        ChapitreVersion v = new ChapitreVersion();
        v.setId(rs.getInt("id"));
        v.setChapitreId(rs.getLong("chapitre_id"));
        v.setVersionNumber(rs.getInt("version_number"));
        v.setTitre(rs.getString("titre"));
        v.setContenu(rs.getString("contenu"));
        v.setOrdre(rs.getInt("ordre"));
        v.setContentType(rs.getString("content_type"));
        v.setVideoUrl(rs.getString("video_url"));
        v.setFileName(rs.getString("file_name"));
        v.setImageUrl(rs.getString("image_url"));
        int dur = rs.getInt("duration_minutes");
        v.setDurationMinutes(rs.wasNull() ? null : dur);
        Timestamp ts = rs.getTimestamp("created_at");
        v.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        v.setChangeDescription(rs.getString("change_description"));
        v.setChangesDetected(rs.getString("changes_detected"));
        v.setModificationPercentage(rs.getDouble("modification_percentage"));
        v.setModifiedBy(rs.getString("modified_by"));
        return v;
    }
}