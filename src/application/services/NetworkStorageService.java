package application.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de gestion du stockage réseau centralisé
 */
public class NetworkStorageService {
    
    private static NetworkStorageService instance;
    private final DatabaseService databaseService;
    
    private String serveurStorageChemin;
    private boolean serveurStorageActif;
    
    private NetworkStorageService() {
        this.databaseService = DatabaseService.getInstance();
        chargerConfiguration();
        initialiserStockage();
    }
    
    public static synchronized NetworkStorageService getInstance() {
        if (instance == null) {
            instance = new NetworkStorageService();
        }
        return instance;
    }
    
    private void chargerConfiguration() {
        Map<String, String> config = getConfiguration();
        
        serveurStorageActif = Boolean.parseBoolean(config.getOrDefault("serveur_stockage_actif", "true"));
        serveurStorageChemin = config.getOrDefault("serveur_stockage_chemin", "C:\\\\DocumentManagement\\\\serveur");
        
        System.out.println("Configuration serveur de stockage:");
        System.out.println("  - Actif: " + serveurStorageActif);
        System.out.println("  - Chemin: " + serveurStorageChemin);
    }
    
    private Map<String, String> getConfiguration() {
        Map<String, String> config = new HashMap<>();
        String query = "SELECT cle, valeur FROM config_serveur";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                config.put(rs.getString("cle"), rs.getString("valeur"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement configuration: " + e.getMessage());
        }
        
        return config;
    }
    
    private void initialiserStockage() {
        if (!serveurStorageActif) {
            return;
        }
        
        try {
            Path storagePath = Paths.get(serveurStorageChemin);
            
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            
            int anneeActuelle = java.time.Year.now().getValue();
            Path anneeActuellePath = storagePath.resolve(String.valueOf(anneeActuelle));
            
            if (!Files.exists(anneeActuellePath)) {
                Files.createDirectories(anneeActuellePath);
            }
            
        } catch (IOException e) {
            System.err.println("Erreur initialisation stockage: " + e.getMessage());
        }
    }
    
    public String stockerFichierServeur(File fichierSource, String codeDocument) {
        if (!serveurStorageActif || fichierSource == null || !fichierSource.exists()) {
            return null;
        }
        
        try {
            int annee = java.time.Year.now().getValue();
            Path repertoireAnnee = Paths.get(serveurStorageChemin, String.valueOf(annee));
            
            if (!Files.exists(repertoireAnnee)) {
                Files.createDirectories(repertoireAnnee);
            }
            
            String extension = getExtension(fichierSource.getName());
            String nomFichier = codeDocument + (extension.isEmpty() ? "" : "." + extension);
            Path destination = repertoireAnnee.resolve(nomFichier);
            
            Files.copy(fichierSource.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("Fichier copié sur serveur: " + destination);
            
            return destination.toString();
            
        } catch (IOException e) {
            System.err.println("Erreur copie sur serveur: " + e.getMessage());
            return null;
        }
    }
    
    public boolean recupererFichierServeur(String cheminServeur, File destination) {
        if (!serveurStorageActif || cheminServeur == null || cheminServeur.isEmpty()) {
            return false;
        }
        
        try {
            Path source = Paths.get(cheminServeur);
            
            if (!Files.exists(source)) {
                return false;
            }
            
            File parentDir = destination.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            Files.copy(source, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Erreur récupération depuis serveur: " + e.getMessage());
            return false;
        }
    }
    
    public boolean fichierExisteSurServeur(String cheminServeur) {
        if (!serveurStorageActif || cheminServeur == null || cheminServeur.isEmpty()) {
            return false;
        }
        
        try {
            Path path = Paths.get(cheminServeur);
            return Files.exists(path) && Files.isRegularFile(path);
        } catch (Exception e) {
            return false;
        }
    }
    
    public String calculerHashFichier(File fichier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(fichier.toPath());
            byte[] hashBytes = digest.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            return null;
        }
    }
    
    public Map<String, Object> getStatistiquesStockage() {
        Map<String, Object> stats = new HashMap<>();
        
        if (!serveurStorageActif) {
            stats.put("actif", false);
            return stats;
        }
        
        try {
            Path storagePath = Paths.get(serveurStorageChemin);
            
            long[] counts = {0, 0};
            
            Files.walk(storagePath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    counts[0]++;
                    try {
                        counts[1] += Files.size(path);
                    } catch (IOException e) {
                    }
                });
            
            stats.put("actif", true);
            stats.put("chemin", serveurStorageChemin);
            stats.put("nombreFichiers", counts[0]);
            stats.put("tailleTotale", counts[1]);
            
            File storage = new File(serveurStorageChemin);
            stats.put("espaceDisponible", storage.getUsableSpace());
            
        } catch (Exception e) {
            stats.put("erreur", e.getMessage());
        }
        
        return stats;
    }
    
    private String getExtension(String nomFichier) {
        if (nomFichier == null || !nomFichier.contains(".")) {
            return "";
        }
        return nomFichier.substring(nomFichier.lastIndexOf(".") + 1).toLowerCase();
    }
    
    public boolean isServeurStorageActif() {
        return serveurStorageActif;
    }
    
    public String getServeurStorageChemin() {
        return serveurStorageChemin;
    }
}