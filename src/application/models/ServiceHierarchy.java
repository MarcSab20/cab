package application.models;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Modèle représentant un service dans la hiérarchie organisationnelle
 * Utilisé pour le workflow de routage des courriers
 */
public class ServiceHierarchy {
    private int id;
    private String serviceCode;
    private String serviceName;
    private String parentServiceCode;
    private int niveau;
    private int ordreAffichage;
    private boolean actif;
    private LocalDateTime dateCreation;
    
    // Relations hiérarchiques
    private ServiceHierarchy parent;
    private List<ServiceHierarchy> enfants;
    
    // Constructeurs
    public ServiceHierarchy() {
        this.enfants = new ArrayList<>();
        this.actif = true;
    }
    
    public ServiceHierarchy(String serviceCode, String serviceName, int niveau) {
        this();
        this.serviceCode = serviceCode;
        this.serviceName = serviceName;
        this.niveau = niveau;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getServiceCode() {
        return serviceCode;
    }
    
    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getParentServiceCode() {
        return parentServiceCode;
    }
    
    public void setParentServiceCode(String parentServiceCode) {
        this.parentServiceCode = parentServiceCode;
    }
    
    public int getNiveau() {
        return niveau;
    }
    
    public void setNiveau(int niveau) {
        this.niveau = niveau;
    }
    
    public int getOrdreAffichage() {
        return ordreAffichage;
    }
    
    public void setOrdreAffichage(int ordreAffichage) {
        this.ordreAffichage = ordreAffichage;
    }
    
    public boolean isActif() {
        return actif;
    }
    
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public ServiceHierarchy getParent() {
        return parent;
    }
    
    public void setParent(ServiceHierarchy parent) {
        this.parent = parent;
    }
    
    public List<ServiceHierarchy> getEnfants() {
        return enfants;
    }
    
    public void setEnfants(List<ServiceHierarchy> enfants) {
        this.enfants = enfants;
    }
    
    // Méthodes utilitaires
    
    /**
     * Ajoute un service enfant
     */
    public void ajouterEnfant(ServiceHierarchy enfant) {
        if (!this.enfants.contains(enfant)) {
            this.enfants.add(enfant);
            enfant.setParent(this);
        }
    }
    
    /**
     * Supprime un service enfant
     */
    public void supprimerEnfant(ServiceHierarchy enfant) {
        this.enfants.remove(enfant);
        enfant.setParent(null);
    }
    
    /**
     * Vérifie si le service est une racine (niveau 0)
     */
    public boolean estRacine() {
        return this.niveau == 0 || this.parentServiceCode == null;
    }
    
    /**
     * Vérifie si le service est une feuille (pas d'enfants)
     */
    public boolean estFeuille() {
        return this.enfants.isEmpty();
    }
    
    /**
     * Retourne le chemin complet dans la hiérarchie
     */
    public String getCheminComplet() {
        if (parent == null) {
            return serviceName;
        }
        return parent.getCheminComplet() + " > " + serviceName;
    }
    
    /**
     * Retourne tous les descendants (enfants, petits-enfants, etc.)
     */
    public List<ServiceHierarchy> getTousLesDescendants() {
        List<ServiceHierarchy> descendants = new ArrayList<>();
        for (ServiceHierarchy enfant : enfants) {
            descendants.add(enfant);
            descendants.addAll(enfant.getTousLesDescendants());
        }
        return descendants;
    }
    
    /**
     * Retourne tous les ancêtres (parents, grands-parents, etc.)
     */
    public List<ServiceHierarchy> getTousLesAncetres() {
        List<ServiceHierarchy> ancetres = new ArrayList<>();
        ServiceHierarchy current = this.parent;
        while (current != null) {
            ancetres.add(current);
            current = current.getParent();
        }
        return ancetres;
    }
    
    /**
     * Vérifie si un service peut transférer vers un autre
     * Règles: on peut transférer vers les enfants directs, les frères, ou les parents
     */
    public boolean peutTransfererVers(ServiceHierarchy destination) {
        if (destination == null || !destination.isActif()) {
            return false;
        }
        
        // Peut transférer vers soi-même
        if (this.equals(destination)) {
            return true;
        }
        
        // Services de niveau 0 (CEMAA, CSP) peuvent transférer partout
        if (this.niveau == 0) {
            return true;
        }
        
        // Peut transférer vers le parent
        if (destination.equals(this.parent)) {
            return true;
        }
        
        // Peut transférer vers les enfants directs
        if (this.enfants.contains(destination)) {
            return true;
        }
        
        // Peut transférer vers les frères (même parent)
        if (this.parent != null && destination.parent != null &&
            this.parent.equals(destination.parent)) {
            return true;
        }
        
        // Peut transférer vers les services de niveau 0
        if (destination.niveau == 0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Calcule la profondeur dans l'arbre
     */
    public int getProfondeur() {
        int profondeur = 0;
        ServiceHierarchy current = this.parent;
        while (current != null) {
            profondeur++;
            current = current.getParent();
        }
        return profondeur;
    }
    
    /**
     * Retourne une représentation visuelle de la hiérarchie
     */
    public String getRepresentationVisuelle() {
        StringBuilder sb = new StringBuilder();
        int profondeur = getProfondeur();
        
        // Indentation selon la profondeur
        for (int i = 0; i < profondeur; i++) {
            sb.append("  ");
        }
        
        // Icône selon le niveau
        if (estRacine()) {
            sb.append("🏢 ");
        } else if (estFeuille()) {
            sb.append("📋 ");
        } else {
            sb.append("📁 ");
        }
        
        sb.append(serviceName);
        sb.append(" (").append(serviceCode).append(")");
        
        return sb.toString();
    }
    
    /**
     * Retourne le nombre total de descendants
     */
    public int getNombreDescendants() {
        return getTousLesDescendants().size();
    }
    
    /**
     * Vérifie si un service est un descendant de ce service
     */
    public boolean estDescendantDe(ServiceHierarchy potentielAncetre) {
        ServiceHierarchy current = this.parent;
        while (current != null) {
            if (current.equals(potentielAncetre)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
    
    /**
     * Retourne l'icône appropriée selon le niveau et le statut
     */
    public String getIcone() {
        if (niveau == 0) {
            return "🏢"; // Services de direction (CEMAA, CSP)
        } else if (estFeuille()) {
            return "📋"; // Services terminaux
        } else {
            return "📁"; // Services intermédiaires
        }
    }
    
    /**
     * Retourne une couleur selon le niveau
     */
    public String getCouleur() {
        switch (niveau) {
            case 0: return "#e74c3c"; // Rouge pour direction
            case 1: return "#3498db"; // Bleu pour sous-direction
            case 2: return "#27ae60"; // Vert pour cellules
            case 3: return "#f39c12"; // Orange pour sections
            default: return "#95a5a6"; // Gris pour autres
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceHierarchy that = (ServiceHierarchy) o;
        return Objects.equals(serviceCode, that.serviceCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(serviceCode);
    }
    
    @Override
    public String toString() {
        return serviceName + " (" + serviceCode + ")";
    }
}