package application.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Modèle représentant un document dans le système
 */
public class Document {
    private int id;
    private String titre;
    private String typeDocument;
    private String cheminFichier;
    private long tailleFichier;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private User creePar;
    private User modifiePar;
    private StatutDocument statut;
    private List<String> tags;
    private String description;
    private String extension;
    private String mimeType;
    private int version;
    private String hash; // Pour vérification d'intégrité
    private boolean confidentiel;
    private LocalDateTime dateExpiration;
    private String motsCles;
    
    // Constructeurs
    public Document() {
        this.tags = new ArrayList<>();
        this.statut = StatutDocument.ACTIF;
        this.dateCreation = LocalDateTime.now();
        this.version = 1;
        this.confidentiel = false;
    }
    
    public Document(String titre, String typeDocument, String cheminFichier, User creePar) {
        this();
        this.titre = titre;
        this.typeDocument = typeDocument;
        this.cheminFichier = cheminFichier;
        this.creePar = creePar;
        
        // Extraction de l'extension
        if (cheminFichier != null && cheminFichier.contains(".")) {
            this.extension = cheminFichier.substring(cheminFichier.lastIndexOf(".") + 1).toLowerCase();
            this.mimeType = getMimeTypeFromExtension();
        }
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitre() {
        return titre;
    }
    
    public void setTitre(String titre) {
        this.titre = titre;
    }
    
    public String getTypeDocument() {
        return typeDocument;
    }
    
    public void setTypeDocument(String typeDocument) {
        this.typeDocument = typeDocument;
    }
    
    public String getCheminFichier() {
        return cheminFichier;
    }
    
    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
        
        // Mise à jour automatique de l'extension et du type MIME
        if (cheminFichier != null && cheminFichier.contains(".")) {
            this.extension = cheminFichier.substring(cheminFichier.lastIndexOf(".") + 1).toLowerCase();
            this.mimeType = getMimeTypeFromExtension();
        }
    }
    
    public long getTailleFichier() {
        return tailleFichier;
    }
    
    public void setTailleFichier(long tailleFichier) {
        this.tailleFichier = tailleFichier;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public LocalDateTime getDateModification() {
        return dateModification;
    }
    
    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }
    
    public User getCreePar() {
        return creePar;
    }
    
    public void setCreePar(User creePar) {
        this.creePar = creePar;
    }
    
    public User getModifiePar() {
        return modifiePar;
    }
    
    public void setModifiePar(User modifiePar) {
        this.modifiePar = modifiePar;
    }
    
    public StatutDocument getStatut() {
        return statut;
    }
    
    public void setStatut(StatutDocument statut) {
        this.statut = statut;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getExtension() {
        return extension;
    }
    
    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public String getHash() {
        return hash;
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public boolean isConfidentiel() {
        return confidentiel;
    }
    
    public void setConfidentiel(boolean confidentiel) {
        this.confidentiel = confidentiel;
    }
    
    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }
    
    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
    
    public String getMotsCles() {
        return motsCles;
    }
    
    public void setMotsCles(String motsCles) {
        this.motsCles = motsCles;
    }
    
    // Méthodes utilitaires
    
    /**
     * Ajoute un tag au document
     */
    public void ajouterTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !this.tags.contains(tag.trim())) {
            this.tags.add(tag.trim());
        }
    }
    
    /**
     * Supprime un tag du document
     */
    public void supprimerTag(String tag) {
        this.tags.remove(tag);
    }
    
    /**
     * Vérifie si le document a un tag spécifique
     */
    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }
    
    /**
     * Retourne les tags sous forme de chaîne séparée par des virgules
     */
    public String getTagsAsString() {
        return String.join(", ", tags);
    }
    
    /**
     * Définit les tags à partir d'une chaîne séparée par des virgules
     */
    public void setTagsFromString(String tagsString) {
        if (tagsString != null && !tagsString.trim().isEmpty()) {
            this.tags = new ArrayList<>(Arrays.asList(tagsString.split("\\s*,\\s*")));
        } else {
            this.tags = new ArrayList<>();
        }
    }
    
    /**
     * Retourne la taille du fichier formatée
     */
    public String getTailleFormatee() {
        if (tailleFichier == 0) return "0 B";
        
        String[] unites = {"B", "KB", "MB", "GB", "TB"};
        int uniteIndex = 0;
        double taille = tailleFichier;
        
        while (taille >= 1024 && uniteIndex < unites.length - 1) {
            taille /= 1024;
            uniteIndex++;
        }
        
        return String.format("%.1f %s", taille, unites[uniteIndex]);
    }
    
    /**
     * Vérifie si le document est expiré
     */
    public boolean isExpire() {
        return dateExpiration != null && LocalDateTime.now().isAfter(dateExpiration);
    }
    
    /**
     * Retourne le nom du fichier sans le chemin
     */
    public String getNomFichier() {
        if (cheminFichier == null) return null;
        
        String separateur = cheminFichier.contains("/") ? "/" : "\\";
        int lastIndex = cheminFichier.lastIndexOf(separateur);
        
        return lastIndex >= 0 ? cheminFichier.substring(lastIndex + 1) : cheminFichier;
    }
    
    /**
     * Retourne le type MIME basé sur l'extension
     */
    public String getMimeTypeFromExtension() {
        if (extension == null) return "application/octet-stream";
        
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "xml" -> "text/xml";
            case "csv" -> "text/csv";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            case "7z" -> "application/x-7z-compressed";
            case "json" -> "application/json";
            case "mp3" -> "audio/mpeg";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            default -> "application/octet-stream";
        };
    }
    
    /**
     * Retourne l'icône associée au type de fichier
     */
    public String getIcone() {
        if (extension == null) return "📄";
        
        return switch (extension.toLowerCase()) {
            case "pdf" -> "📕";
            case "doc", "docx" -> "📘";
            case "xls", "xlsx" -> "📗";
            case "ppt", "pptx" -> "📙";
            case "txt" -> "📝";
            case "html", "htm" -> "🌐";
            case "xml" -> "📋";
            case "csv" -> "📊";
            case "jpg", "jpeg", "png", "gif", "bmp", "svg" -> "🖼️";
            case "zip", "rar", "7z" -> "📦";
            case "mp3" -> "🎵";
            case "mp4", "avi" -> "🎬";
            case "json" -> "📋";
            default -> "📄";
        };
    }
    
    /**
     * Met à jour la date de modification et l'utilisateur modificateur
     */
    public void marquerCommeModifie(User utilisateur) {
        this.dateModification = LocalDateTime.now();
        this.modifiePar = utilisateur;
        this.version++;
    }
    
    /**
     * Retourne la date de création formatée
     */
    public String getDateCreationFormatee() {
        if (dateCreation == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateCreation.format(formatter);
    }
    
    /**
     * Retourne la date de modification formatée
     */
    public String getDateModificationFormatee() {
        if (dateModification == null) return "Jamais modifié";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateModification.format(formatter);
    }
    
    /**
     * Retourne le nom de l'auteur
     */
    public String getNomAuteur() {
        return creePar != null ? creePar.getNomComplet() : "Inconnu";
    }
    
    /**
     * Retourne le nom du dernier modificateur
     */
    public String getNomModificateur() {
        return modifiePar != null ? modifiePar.getNomComplet() : "Aucun";
    }
    
    /**
     * Vérifie si le document peut être modifié
     */
    public boolean peutEtreModifie() {
        return statut.peutEtreModifie();
    }
    
    /**
     * Archive le document
     */
    public void archiver() {
        if (!statut.isActif()) {
            throw new IllegalStateException("Seuls les documents actifs peuvent être archivés");
        }
        this.statut = StatutDocument.ARCHIVE;
    }
    
    /**
     * Valide le document
     */
    public void valider(User validateur) {
        if (statut != StatutDocument.ATTENTE_VALIDATION) {
            throw new IllegalStateException("Seuls les documents en attente peuvent être validés");
        }
        this.statut = StatutDocument.VALIDE;
        this.marquerCommeModifie(validateur);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return id == document.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", typeDocument='" + typeDocument + '\'' +
                ", extension='" + extension + '\'' +
                ", tailleFichier=" + getTailleFormatee() +
                ", statut=" + statut +
                ", version=" + version +
                ", confidentiel=" + confidentiel +
                '}';
    }
}