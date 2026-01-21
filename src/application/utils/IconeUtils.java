package application.utils;

import application.models.Dossier;
import application.services.DossierService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilitaire pour corriger et attribuer les icônes des dossiers
 */
public class IconeUtils {
    
    // Map des icônes par type de dossier
    private static final Map<String, String> ICONES_PAR_MOT_CLE = new HashMap<>();
    
    static {
        // Initialiser les associations mot-clé → icône
        ICONES_PAR_MOT_CLE.put("BORDEREAU", "📋");
        ICONES_PAR_MOT_CLE.put("CHRONO", "🕐");
        ICONES_PAR_MOT_CLE.put("CONFIDENTIEL", "🔒");
        ICONES_PAR_MOT_CLE.put("CORBEILLE", "🗑️");
        ICONES_PAR_MOT_CLE.put("ENTRANT", "📥");
        ICONES_PAR_MOT_CLE.put("SORTANT", "📤");
        ICONES_PAR_MOT_CLE.put("DÉCISION", "⚖️");
        ICONES_PAR_MOT_CLE.put("MINISTÉRIEL", "⚖️");
        ICONES_PAR_MOT_CLE.put("DIVERS", "📚");
        ICONES_PAR_MOT_CLE.put("INTERNE", "🏢");
        ICONES_PAR_MOT_CLE.put("ARCHIVES", "📂");
        ICONES_PAR_MOT_CLE.put("PERSONNEL", "👥");
        ICONES_PAR_MOT_CLE.put("ADMIN", "⚙️");
        ICONES_PAR_MOT_CLE.put("COURRIER", "📮");
        ICONES_PAR_MOT_CLE.put("TEST", "🧪");
        ICONES_PAR_MOT_CLE.put("ROOT", "🏠");
    }
    
    /**
     * Vérifie si une icône est valide
     */
    public static boolean isIconeValide(String icone) {
        if (icone == null || icone.trim().isEmpty()) {
            return false;
        }
        
        // Vérifier si contient des "?"
        if (icone.contains("?")) {
            return false;
        }
        
        // Vérifier si c'est un emoji valide (plus de 1 caractère généralement)
        return icone.length() >= 1;
    }
    
    /**
     * Corrige une icône invalide
     */
    public static String corrigerIcone(String icone) {
        if (isIconeValide(icone)) {
            return icone;
        }
        return "📁"; // Icône par défaut
    }
    
    /**
     * Détermine l'icône appropriée selon le nom et le code du dossier
     */
    public static String determinerIcone(String nomDossier, String codeDossier) {
        // Vérifier d'abord le code
        if (codeDossier != null) {
            String codeUpper = codeDossier.toUpperCase();
            for (Map.Entry<String, String> entry : ICONES_PAR_MOT_CLE.entrySet()) {
                if (codeUpper.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        
        // Ensuite vérifier le nom
        if (nomDossier != null) {
            String nomUpper = nomDossier.toUpperCase();
            for (Map.Entry<String, String> entry : ICONES_PAR_MOT_CLE.entrySet()) {
                if (nomUpper.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        
        // Icône par défaut
        return "📁";
    }
    
    /**
     * Corrige l'icône d'un dossier si elle est invalide
     */
    public static String getIconeSafe(Dossier dossier) {
        if (dossier == null) {
            return "📁";
        }
        
        String icone = dossier.getIcone();
        
        // Si l'icône est invalide, en déterminer une appropriée
        if (!isIconeValide(icone)) {
            return determinerIcone(dossier.getNomDossier(), dossier.getCodeDossier());
        }
        
        return icone;
    }
    
    /**
     * Corrige tous les dossiers avec des icônes invalides en base de données
     */
    public static int corrigerTousLesDossiers() {
        DossierService dossierService = DossierService.getInstance();
        List<Dossier> dossiers = dossierService.getAllDossiers();
        
        int nombreCorrections = 0;
        
        for (Dossier dossier : dossiers) {
            String iconeActuelle = dossier.getIcone();
            
            if (!isIconeValide(iconeActuelle)) {
                String nouvelleIcone = determinerIcone(dossier.getNomDossier(), dossier.getCodeDossier());
                dossier.setIcone(nouvelleIcone);
                
                try {
                    dossierService.updateDossier(dossier);
                    nombreCorrections++;
                    System.out.println("✓ Corrigé: " + dossier.getNomDossier() + " → " + nouvelleIcone);
                } catch (Exception e) {
                    System.err.println("✗ Erreur correction: " + dossier.getNomDossier());
                }
            }
        }
        
        System.out.println("\n✅ " + nombreCorrections + " dossiers corrigés sur " + dossiers.size());
        
        return nombreCorrections;
    }
    
    /**
     * Liste des icônes disponibles
     */
    public static String[] getIconesDisponibles() {
        return new String[] {
            "📁", "📂", "📋", "📊", "📌", "📄", "📑", "📕", 
            "📗", "📘", "📙", "🗂️", "🗄️", "📦", "🏢", "👥", 
            "⚙️", "📮", "🔒", "⭐", "🕐", "📥", "📤", "⚖️", 
            "📚", "🧪", "🗑️", "🏠"
        };
    }
    
    /**
     * Formate l'affichage d'un dossier avec son icône
     */
    public static String formatterNomDossier(Dossier dossier) {
        if (dossier == null) {
            return "";
        }
        
        String icone = getIconeSafe(dossier);
        return icone + " " + dossier.getNomDossier();
    }
}