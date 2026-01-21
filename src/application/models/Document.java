package application.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle Document avec support de la nomenclature automatique et du stockage réseau
 */
public class Document {
    
    private int id;
    private String codeDocument;
    private Integer dossierId;
    private String titre;
    private String typeDocument;
    private String cheminFichier;
    private String cheminServeur;        // NOUVEAU
    private long tailleFichier;
    private String extension;
    private String mimeType;
    private String description;
    private String motsCles;
    private String nomAuteur;
    private Integer creePar;
    private Integer modifiePar;
    private String statut;
    private int version;
    private String hashFichier;          // NOUVEAU
    private boolean confidentiel;
    private boolean archive;             // NOUVEAU
    private LocalDateTime dateArchivage; // NOUVEAU
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private LocalDateTime dateExpiration;
    
    // Informations supplémentaires (jointures)
    private String createurNom;
    private String createurService;
    private String modificateurNom;
    private String nomDossier;
    private String codeDossierParent;
    private String cheminDossier;
    private String iconeDossier;
    
    public Document() {
        this.statut = "actif";
        this.version = 1;
        this.confidentiel = false;
        this.archive = false;
        this.dateCreation = LocalDateTime.now();
    }
    
    // ============ GETTERS ET SETTERS ============
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCodeDocument() {
        return codeDocument;
    }
    
    public void setCodeDocument(String codeDocument) {
        this.codeDocument = codeDocument;
    }
    
    public Integer getDossierId() {
        return dossierId;
    }
    
    public void setDossierId(Integer dossierId) {
        this.dossierId = dossierId;
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
    
    
    
    public String getNomAuteur() {
		return nomAuteur;
	}

	public void setNomAuteur(String nomAuteur) {
		this.nomAuteur = nomAuteur;
	}

	public void setTypeDocument(String typeDocument) {
        this.typeDocument = typeDocument;
    }
    
    public String getCheminFichier() {
        return cheminFichier;
    }
    
    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }
    
    public String getCheminServeur() {
        return cheminServeur;
    }
    
    public void setCheminServeur(String cheminServeur) {
        this.cheminServeur = cheminServeur;
    }
    
    public long getTailleFichier() {
        return tailleFichier;
    }
    
    public void setTailleFichier(long tailleFichier) {
        this.tailleFichier = tailleFichier;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getMotsCles() {
        return motsCles;
    }
    
    public void setMotsCles(String motsCles) {
        this.motsCles = motsCles;
    }
    
    public Integer getCreePar() {
        return creePar;
    }
    
    public void setCreePar(Integer creePar) {
        this.creePar = creePar;
    }
    
    public Integer getModifiePar() {
        return modifiePar;
    }
    
    public void setModifiePar(Integer modifiePar) {
        this.modifiePar = modifiePar;
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public String getHashFichier() {
        return hashFichier;
    }
    
    public void setHashFichier(String hashFichier) {
        this.hashFichier = hashFichier;
    }
    
    public boolean isConfidentiel() {
        return confidentiel;
    }
    
    public void setConfidentiel(boolean confidentiel) {
        this.confidentiel = confidentiel;
    }
    
    public boolean isArchive() {
        return archive;
    }
    
    public void setArchive(boolean archive) {
        this.archive = archive;
    }
    
    public LocalDateTime getDateArchivage() {
        return dateArchivage;
    }
    
    public void setDateArchivage(LocalDateTime dateArchivage) {
        this.dateArchivage = dateArchivage;
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
    
    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }
    
    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
    
    // Informations supplémentaires
    
    public String getCreateurNom() {
        return createurNom;
    }
    
    public void setCreateurNom(String createurNom) {
        this.createurNom = createurNom;
    }
    
    public String getCreateurService() {
        return createurService;
    }
    
    public void setCreateurService(String createurService) {
        this.createurService = createurService;
    }
    
    public String getModificateurNom() {
        return modificateurNom;
    }
    
    public void setModificateurNom(String modificateurNom) {
        this.modificateurNom = modificateurNom;
    }
    
    public String getNomDossier() {
        return nomDossier;
    }
    
    public void setNomDossier(String nomDossier) {
        this.nomDossier = nomDossier;
    }
    
    public String getCodeDossierParent() {
        return codeDossierParent;
    }
    
    public void setCodeDossierParent(String codeDossierParent) {
        this.codeDossierParent = codeDossierParent;
    }
    
    public String getCheminDossier() {
        return cheminDossier;
    }
    
    public void setCheminDossier(String cheminDossier) {
        this.cheminDossier = cheminDossier;
    }
    
    public String getIconeDossier() {
        return iconeDossier;
    }
    
    public void setIconeDossier(String iconeDossier) {
        this.iconeDossier = iconeDossier;
    }
    
    // ============ MÉTHODES UTILITAIRES ============
    
    /**
     * Vérifie si le document est disponible sur le serveur
     */
    public boolean estDisponibleSurServeur() {
        return cheminServeur != null && !cheminServeur.isEmpty();
    }
    
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
                ", codeDocument='" + codeDocument + '\'' +
                ", titre='" + titre + '\'' +
                ", typeDocument='" + typeDocument + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}