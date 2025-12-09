package application.models;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Modèle pour les statistiques de workflow
 */
public class WorkflowStats {
    private String serviceCode;
    private String serviceName;
    private int niveau;
    
    // Statistiques globales
    private int totalCourriersTraites;
    private int courriersEnCours;
    private int courriersEnAttente;
    private double dureeMoyenneTraitement; // En heures
    private double tauxReussite;
    
    // Statistiques temporelles
    private Map<String, Integer> repartitionParMois;
    private Map<String, Double> dureeParMois;
    
    // Flux de courriers (pour graphes)
    private List<TransitionFlux> transitions;
    private int nombreEntrees;
    private int nombreSorties;
    
    // Goulots d'étranglement
    private double delaiMoyenReception; // Temps d'attente avant traitement
    private int courriersEnRetard;
    private List<String> servicesGoulots; // Services qui ralentissent
    
    public WorkflowStats() {
        this.repartitionParMois = new HashMap<>();
        this.dureeParMois = new HashMap<>();
        this.transitions = new ArrayList<>();
        this.servicesGoulots = new ArrayList<>();
    }
    
    public WorkflowStats(String serviceCode, String serviceName, int niveau) {
        this();
        this.serviceCode = serviceCode;
        this.serviceName = serviceName;
        this.niveau = niveau;
    }
    
    // Getters et Setters
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
    
    public int getNiveau() {
        return niveau;
    }
    
    public void setNiveau(int niveau) {
        this.niveau = niveau;
    }
    
    public int getTotalCourriersTraites() {
        return totalCourriersTraites;
    }
    
    public void setTotalCourriersTraites(int totalCourriersTraites) {
        this.totalCourriersTraites = totalCourriersTraites;
    }
    
    public int getCourriersEnCours() {
        return courriersEnCours;
    }
    
    public void setCourriersEnCours(int courriersEnCours) {
        this.courriersEnCours = courriersEnCours;
    }
    
    public int getCourriersEnAttente() {
        return courriersEnAttente;
    }
    
    public void setCourriersEnAttente(int courriersEnAttente) {
        this.courriersEnAttente = courriersEnAttente;
    }
    
    public double getDureeMoyenneTraitement() {
        return dureeMoyenneTraitement;
    }
    
    public void setDureeMoyenneTraitement(double dureeMoyenneTraitement) {
        this.dureeMoyenneTraitement = dureeMoyenneTraitement;
    }
    
    public double getTauxReussite() {
        return tauxReussite;
    }
    
    public void setTauxReussite(double tauxReussite) {
        this.tauxReussite = tauxReussite;
    }
    
    public Map<String, Integer> getRepartitionParMois() {
        return repartitionParMois;
    }
    
    public void setRepartitionParMois(Map<String, Integer> repartitionParMois) {
        this.repartitionParMois = repartitionParMois;
    }
    
    public Map<String, Double> getDureeParMois() {
        return dureeParMois;
    }
    
    public void setDureeParMois(Map<String, Double> dureeParMois) {
        this.dureeParMois = dureeParMois;
    }
    
    public List<TransitionFlux> getTransitions() {
        return transitions;
    }
    
    public void setTransitions(List<TransitionFlux> transitions) {
        this.transitions = transitions;
    }
    
    public void addTransition(TransitionFlux transition) {
        this.transitions.add(transition);
    }
    
    public int getNombreEntrees() {
        return nombreEntrees;
    }
    
    public void setNombreEntrees(int nombreEntrees) {
        this.nombreEntrees = nombreEntrees;
    }
    
    public int getNombreSorties() {
        return nombreSorties;
    }
    
    public void setNombreSorties(int nombreSorties) {
        this.nombreSorties = nombreSorties;
    }
    
    public double getDelaiMoyenReception() {
        return delaiMoyenReception;
    }
    
    public void setDelaiMoyenReception(double delaiMoyenReception) {
        this.delaiMoyenReception = delaiMoyenReception;
    }
    
    public int getCourriersEnRetard() {
        return courriersEnRetard;
    }
    
    public void setCourriersEnRetard(int courriersEnRetard) {
        this.courriersEnRetard = courriersEnRetard;
    }
    
    public List<String> getServicesGoulots() {
        return servicesGoulots;
    }
    
    public void setServicesGoulots(List<String> servicesGoulots) {
        this.servicesGoulots = servicesGoulots;
    }
    
    public void addServiceGoulot(String serviceCode) {
        if (!this.servicesGoulots.contains(serviceCode)) {
            this.servicesGoulots.add(serviceCode);
        }
    }
    
    /**
     * Retourne la durée moyenne formatée
     */
    public String getDureeMoyenneFormatee() {
        if (dureeMoyenneTraitement < 1) {
            return String.format("%.0f min", dureeMoyenneTraitement * 60);
        } else if (dureeMoyenneTraitement < 24) {
            return String.format("%.1f h", dureeMoyenneTraitement);
        } else {
            return String.format("%.1f j", dureeMoyenneTraitement / 24);
        }
    }
    
    /**
     * Vérifie si le service est un goulot d'étranglement
     */
    public boolean estGoulot() {
        return delaiMoyenReception > 24 || // Plus de 24h d'attente
               (courriersEnCours > 0 && courriersEnRetard > courriersEnCours * 0.3); // Plus de 30% en retard
    }
    
    /**
     * Calcule le score de performance (0-100)
     */
    public int getScorePerformance() {
        double score = 100.0;
        
        // Pénalité pour les retards
        if (totalCourriersTraites > 0) {
            double tauxRetard = (double) courriersEnRetard / totalCourriersTraites;
            score -= tauxRetard * 30;
        }
        
        // Pénalité pour le délai d'attente
        if (delaiMoyenReception > 48) {
            score -= 20;
        } else if (delaiMoyenReception > 24) {
            score -= 10;
        }
        
        // Bonus pour la rapidité
        if (dureeMoyenneTraitement < 4) {
            score += 10;
        }
        
        return Math.max(0, Math.min(100, (int) score));
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowStats{service=%s, traites=%d, enCours=%d, duree=%.1fh, score=%d%%}",
            serviceName, totalCourriersTraites, courriersEnCours, dureeMoyenneTraitement, getScorePerformance());
    }
    
    /**
     * Classe interne pour représenter une transition entre services
     */
    public static class TransitionFlux {
        private String serviceSource;
        private String serviceDestination;
        private int nombreCourriers;
        private double dureeMoyenne;
        
        public TransitionFlux(String serviceSource, String serviceDestination, int nombreCourriers, double dureeMoyenne) {
            this.serviceSource = serviceSource;
            this.serviceDestination = serviceDestination;
            this.nombreCourriers = nombreCourriers;
            this.dureeMoyenne = dureeMoyenne;
        }
        
        public String getServiceSource() {
            return serviceSource;
        }
        
        public void setServiceSource(String serviceSource) {
            this.serviceSource = serviceSource;
        }
        
        public String getServiceDestination() {
            return serviceDestination;
        }
        
        public void setServiceDestination(String serviceDestination) {
            this.serviceDestination = serviceDestination;
        }
        
        public int getNombreCourriers() {
            return nombreCourriers;
        }
        
        public void setNombreCourriers(int nombreCourriers) {
            this.nombreCourriers = nombreCourriers;
        }
        
        public double getDureeMoyenne() {
            return dureeMoyenne;
        }
        
        public void setDureeMoyenne(double dureeMoyenne) {
            this.dureeMoyenne = dureeMoyenne;
        }
        
        /**
         * Retourne l'épaisseur du trait pour le graphe (proportionnel au nombre)
         */
        public double getEpaisseurTrait() {
            return Math.min(10, 1 + nombreCourriers / 5.0);
        }
        
        /**
         * Retourne la couleur selon la durée moyenne
         */
        public String getCouleur() {
            if (dureeMoyenne < 4) {
                return "#27ae60"; // Vert - Rapide
            } else if (dureeMoyenne < 24) {
                return "#f39c12"; // Orange - Moyen
            } else {
                return "#e74c3c"; // Rouge - Lent
            }
        }
        
        @Override
        public String toString() {
            return String.format("%s → %s (%d courriers, %.1fh)", 
                serviceSource, serviceDestination, nombreCourriers, dureeMoyenne);
        }
    }
}