package application.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
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
        
        // Mise à jour automatique de l'extension
        if (cheminFichier != null && cheminFichier.contains(".")) {
            this.extension = cheminFichier.substring(cheminFichier.lastIndexOf(".") + 1).toLowerCase();
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
        
        switch (extension.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "xml":
                return "text/xml";
            case "csv":
                return "text/csv";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * Met à jour la date de modification et l'utilisateur modificateur
     */
    public void marquerCommeModifie(User utilisateur) {
        this.dateModification = LocalDateTime.now();
        this.modifiePar = utilisateur;
        this.version++;
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
                '}';
    }
}

/**
 * Énumération des statuts possibles pour un document
 */
enum StatutDocument {
    ACTIF("Actif"),
    ARCHIVE("Archivé"),
    SUPPRIME("Supprimé"),
    BROUILLON("Brouillon"),
    EN_COURS("En cours"),
    VALIDE("Validé"),
    EXPIRE("Expiré"),
    SUSPENDU("Suspendu");
    
    private final String libelle;
    
    StatutDocument(String libelle) {
        this.libelle = libelle;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}