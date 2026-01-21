package application.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modèle représentant une version d'un document
 */
public class VersionDocument {
    private int id;
    private int documentId;
    private int numeroVersion;
    private String cheminFichier;
    private long tailleFichier;
    private String hashFichier;
    private String commentaire;
    private Integer creePar;
    private LocalDateTime dateCreation;
    
    // Champs additionnels (non en base)
    private String createurNom;
    private String codeDocument;
    
    // Constructeurs
    public VersionDocument() {
        this.dateCreation = LocalDateTime.now();
    }
    
    public VersionDocument(int documentId, int numeroVersion, String cheminFichier, long tailleFichier) {
        this();
        this.documentId = documentId;
        this.numeroVersion = numeroVersion;
        this.cheminFichier = cheminFichier;
        this.tailleFichier = tailleFichier;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }
    
    public int getNumeroVersion() {
        return numeroVersion;
    }
    
    public void setNumeroVersion(int numeroVersion) {
        this.numeroVersion = numeroVersion;
    }
    
    public String getCheminFichier() {
        return cheminFichier;
    }
    
    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }
    
    public long getTailleFichier() {
        return tailleFichier;
    }
    
    public void setTailleFichier(long tailleFichier) {
        this.tailleFichier = tailleFichier;
    }
    
    public String getHashFichier() {
        return hashFichier;
    }
    
    public void setHashFichier(String hashFichier) {
        this.hashFichier = hashFichier;
    }
    
    public String getCommentaire() {
        return commentaire;
    }
    
    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
    
    public Integer getCreePar() {
        return creePar;
    }
    
    public void setCreePar(Integer creePar) {
        this.creePar = creePar;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public String getCreateurNom() {
        return createurNom;
    }
    
    public void setCreateurNom(String createurNom) {
        this.createurNom = createurNom;
    }
    
    public String getCodeDocument() {
        return codeDocument;
    }
    
    public void setCodeDocument(String codeDocument) {
        this.codeDocument = codeDocument;
    }
    
    // Méthodes utilitaires
    
    /**
     * Retourne la taille formatée du fichier
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
     * Retourne la date de création formatée
     */
    public String getDateCreationFormatee() {
        if (dateCreation == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateCreation.format(formatter);
    }
    
    /**
     * Retourne le libellé de la version
     */
    public String getLibelleVersion() {
        return "v" + numeroVersion + ".0";
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
     * Vérifie si cette version est actuelle
     */
    public boolean estVersionActuelle(int versionActuelle) {
        return this.numeroVersion == versionActuelle;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionDocument that = (VersionDocument) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "VersionDocument{" +
                "id=" + id +
                ", documentId=" + documentId +
                ", numeroVersion=" + numeroVersion +
                ", taille=" + getTailleFormatee() +
                ", dateCreation=" + getDateCreationFormatee() +
                '}';
    }
}