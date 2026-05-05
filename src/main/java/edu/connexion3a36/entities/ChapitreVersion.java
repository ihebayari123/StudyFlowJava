package edu.connexion3a36.entities;

import java.time.LocalDateTime;

public class ChapitreVersion {

    private int id;
    private int versionNumber;
    private String titre;
    private String contenu;
    private int ordre;
    private String contentType;
    private String videoUrl;
    private String fileName;
    private String imageUrl;
    private Integer durationMinutes;
    private LocalDateTime createdAt;
    private String changeDescription;
    private String changesDetected;   // JSON string
    private double modificationPercentage;
    private String modifiedBy;
    private long chapitreId;

    public ChapitreVersion() {}

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getChangeDescription() { return changeDescription; }
    public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }

    public String getChangesDetected() { return changesDetected; }
    public void setChangesDetected(String changesDetected) { this.changesDetected = changesDetected; }

    public double getModificationPercentage() { return modificationPercentage; }
    public void setModificationPercentage(double modificationPercentage) { this.modificationPercentage = modificationPercentage; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public long getChapitreId() { return chapitreId; }
    public void setChapitreId(long chapitreId) { this.chapitreId = chapitreId; }
}
