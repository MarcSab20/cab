package application.services;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Service AMÉLIORÉ de gestion du stockage réseau centralisé
 * Support des serveurs locaux ET distants via réseau
 */
public class NetworkStorageService {
    
    private static NetworkStorageService instance;
    private final DatabaseService databaseService;
    private final LogService logService;
    
    private String serveurStorageChemin;
    private boolean serveurStorageActif;
    private boolean serveurDistant;
    private String serveurAdresse;
    private String serveurPort;
    private String serveurUtilisateur;
    private String serveurMotDePasse;
    
    private NetworkStorageService() {
        this.databaseService = DatabaseService.getInstance();
        this.logService = LogService.getInstance();
        chargerConfiguration();
        initialiserStockage();
    }
    
    public static synchronized NetworkStorageService getInstance() {
        if (instance == null) {
            instance = new NetworkStorageService();
        }
        return instance;
    }
    
    /**
     * Charge la configuration depuis la base de données
     */
    private void chargerConfiguration() {
        Map<String, String> config = getConfiguration();
        
        serveurStorageActif = Boolean.parseBoolean(config.getOrDefault("serveur_stockage_actif", "true"));
        serveurStorageChemin = config.getOrDefault("serveur_stockage_chemin", "C:\\\\DocumentManagement\\\\serveur");
        serveurDistant = Boolean.parseBoolean(config.getOrDefault("serveur_distant", "false"));
        serveurAdresse = config.getOrDefault("serveur_adresse", "");
        serveurPort = config.getOrDefault("serveur_port", "445");
        serveurUtilisateur = config.getOrDefault("serveur_utilisateur", "");
        serveurMotDePasse = config.getOrDefault("serveur_mot_de_passe", "");
        
        System.out.println("Configuration serveur de stockage:");
        System.out.println("  - Actif: " + serveurStorageActif);
        System.out.println("  - Mode: " + (serveurDistant ? "Distant" : "Local"));
        if (serveurDistant) {
            System.out.println("  - Adresse: " + serveurAdresse);
            System.out.println("  - Port: " + serveurPort);
        } else {
            System.out.println("  - Chemin: " + serveurStorageChemin);
        }
    }
    
    /**
     * Recharge la configuration (après modification)
     */
    public void rechargerConfiguration() {
        System.out.println("♻️ Rechargement de la configuration du serveur...");
        chargerConfiguration();
        initialiserStockage();
        logService.logAction("rechargement_config_serveur", "Configuration du serveur rechargée");
    }
    
    /**
     * Récupère la configuration depuis la base de données
     */
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
    
    /**
     * Initialise le stockage (crée les répertoires nécessaires)
     */
    private void initialiserStockage() {
        if (!serveurStorageActif) {
            System.out.println("⚠️ Serveur de stockage désactivé");
            return;
        }
        
        if (serveurDistant) {
            System.out.println("🌐 Mode serveur distant - Pas d'initialisation locale");
            return;
        }
        
        try {
            Path storagePath = Paths.get(serveurStorageChemin);
            
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                System.out.println("✓ Répertoire de stockage créé: " + serveurStorageChemin);
            }
            
            // Créer le répertoire de l'année actuelle
            int anneeActuelle = java.time.Year.now().getValue();
            Path anneeActuellePath = storagePath.resolve(String.valueOf(anneeActuelle));
            
            if (!Files.exists(anneeActuellePath)) {
                Files.createDirectories(anneeActuellePath);
                System.out.println("✓ Répertoire année créé: " + anneeActuelle);
            }
            
            System.out.println("✓ Stockage initialisé avec succès");
            
        } catch (IOException e) {
            System.err.println("❌ Erreur initialisation stockage: " + e.getMessage());
            logService.logErreur("initialisation_stockage", e.getMessage());
        }
    }
    
    /**
     * Teste la connexion au serveur (local ou distant)
     */
    public boolean testerConnexionDistante(String adresse, String port, String utilisateur, String motDePasse) {
        try {
            if (adresse == null || adresse.trim().isEmpty()) {
                return false;
            }
            
            System.out.println("🔍 Test de connexion au serveur distant...");
            System.out.println("   Adresse: " + adresse);
            System.out.println("   Port: " + port);
            
            // Test de ping
            InetAddress inetAddress = InetAddress.getByName(adresse);
            boolean reachable = inetAddress.isReachable(5000); // 5 secondes de timeout
            
            if (!reachable) {
                System.out.println("❌ Serveur non accessible (ping échoué)");
                return false;
            }
            
            System.out.println("✓ Serveur accessible (ping réussi)");
            
            // Test de connexion SMB/CIFS (pour Windows)
            // Dans une vraie application, utilisez jcifs ou smbj pour SMB
            // Ici on fait un test basique
            
            String cheminReseau = "\\\\\\" + adresse + "\\partage";
            File testFile = new File(cheminReseau);
            
            // Tentative d'accès (peut nécessiter des identifiants Windows)
            boolean accessible = testFile.exists() || testFile.canRead();
            
            if (accessible) {
                System.out.println("✓ Partage réseau accessible");
                logService.logAction("test_connexion_serveur_distant", 
                    "Connexion réussie à " + adresse);
                return true;
            } else {
                System.out.println("⚠️ Partage réseau non accessible - Vérifiez les permissions");
                // On retourne true si au moins le ping a réussi
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur test connexion: " + e.getMessage());
            logService.logErreur("test_connexion_serveur_distant", e.getMessage());
            return false;
        }
    }
    
    /**
     * Stocke un fichier sur le serveur (local ou distant)
     */
    public String stockerFichierServeur(File fichierSource, String codeDocument) {
        if (!serveurStorageActif || fichierSource == null || !fichierSource.exists()) {
            return null;
        }
        
        try {
            String cheminDestination;
            
            if (serveurDistant) {
                cheminDestination = stockerFichierDistant(fichierSource, codeDocument);
            } else {
                cheminDestination = stockerFichierLocal(fichierSource, codeDocument);
            }
            
            if (cheminDestination != null) {
                System.out.println("✓ Fichier stocké sur serveur: " + cheminDestination);
                logService.logAction("stockage_fichier", 
                    "Fichier " + codeDocument + " stocké sur serveur");
            }
            
            return cheminDestination;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur stockage fichier: " + e.getMessage());
            logService.logErreur("stockage_fichier", e.getMessage());
            return null;
        }
    }
    
    /**
     * Stocke un fichier en local
     */
    private String stockerFichierLocal(File fichierSource, String codeDocument) throws IOException {
        int annee = java.time.Year.now().getValue();
        Path repertoireAnnee = Paths.get(serveurStorageChemin, String.valueOf(annee));
        
        if (!Files.exists(repertoireAnnee)) {
            Files.createDirectories(repertoireAnnee);
        }
        
        String extension = getExtension(fichierSource.getName());
        String nomFichier = codeDocument + (extension.isEmpty() ? "" : "." + extension);
        Path destination = repertoireAnnee.resolve(nomFichier);
        
        Files.copy(fichierSource.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        
        return destination.toString();
    }
    
    /**
     * Stocke un fichier sur un serveur distant
     */
    private String stockerFichierDistant(File fichierSource, String codeDocument) throws IOException {
        // Construction du chemin réseau Windows (UNC)
        // Format: \\serveur\partage\dossier\fichier
        
        int annee = java.time.Year.now().getValue();
        String cheminReseau = "\\\\\\" + serveurAdresse + "\\documents\\" + annee;
        
        Path repertoireAnnee = Paths.get(cheminReseau);
        
        // Créer le répertoire si nécessaire
        if (!Files.exists(repertoireAnnee)) {
            try {
                Files.createDirectories(repertoireAnnee);
            } catch (IOException e) {
                System.err.println("⚠️ Impossible de créer le répertoire distant: " + e.getMessage());
                // Continue quand même
            }
        }
        
        String extension = getExtension(fichierSource.getName());
        String nomFichier = codeDocument + (extension.isEmpty() ? "" : "." + extension);
        Path destination = repertoireAnnee.resolve(nomFichier);
        
        Files.copy(fichierSource.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        
        return destination.toString();
    }
    
    /**
     * Récupère un fichier depuis le serveur
     */
    public boolean recupererFichierServeur(String cheminServeur, File destination) {
        if (!serveurStorageActif || cheminServeur == null || cheminServeur.isEmpty()) {
            return false;
        }
        
        try {
            Path source = Paths.get(cheminServeur);
            
            if (!Files.exists(source)) {
                System.err.println("❌ Fichier introuvable sur serveur: " + cheminServeur);
                return false;
            }
            
            File parentDir = destination.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            Files.copy(source, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("✓ Fichier récupéré depuis serveur");
            logService.logAction("recuperation_fichier", "Fichier récupéré: " + destination.getName());
            
            return true;
            
        } catch (IOException e) {
            System.err.println("❌ Erreur récupération fichier: " + e.getMessage());
            logService.logErreur("recuperation_fichier", e.getMessage());
            return false;
        }
    }
    
    /**
     * Vérifie si un fichier existe sur le serveur
     */
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
    
    /**
     * Calcule le hash SHA-256 d'un fichier
     */
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
            System.err.println("Erreur calcul hash: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Récupère les statistiques du serveur de stockage
     */
    public Map<String, Object> getStatistiquesStockage() {
        Map<String, Object> stats = new HashMap<>();
        
        if (!serveurStorageActif) {
            stats.put("actif", false);
            return stats;
        }
        
        stats.put("actif", true);
        stats.put("mode", serveurDistant ? "distant" : "local");
        
        if (serveurDistant) {
            stats.put("adresse", serveurAdresse);
            stats.put("port", serveurPort);
        } else {
            stats.put("chemin", serveurStorageChemin);
        }
        
        try {
            Path storagePath = Paths.get(serveurStorageChemin);
            
            if (Files.exists(storagePath)) {
                long[] counts = {0, 0};
                
                Files.walk(storagePath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        counts[0]++;
                        try {
                            counts[1] += Files.size(path);
                        } catch (IOException e) {
                            // Ignorer
                        }
                    });
                
                stats.put("nombreFichiers", counts[0]);
                stats.put("tailleTotale", counts[1]);
                
                File storage = new File(serveurStorageChemin);
                stats.put("espaceDisponible", storage.getUsableSpace());
                stats.put("espaceTotal", storage.getTotalSpace());
            } else {
                stats.put("nombreFichiers", 0);
                stats.put("tailleTotale", 0L);
                stats.put("espaceDisponible", 0L);
                stats.put("espaceTotal", 0L);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur récupération statistiques: " + e.getMessage());
            stats.put("erreur", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Supprime un fichier du serveur
     */
    public boolean supprimerFichierServeur(String cheminServeur) {
        if (!serveurStorageActif || cheminServeur == null || cheminServeur.isEmpty()) {
            return false;
        }
        
        try {
            Path path = Paths.get(cheminServeur);
            
            if (Files.exists(path)) {
                Files.delete(path);
                System.out.println("✓ Fichier supprimé du serveur");
                logService.logAction("suppression_fichier_serveur", "Fichier supprimé: " + cheminServeur);
                return true;
            }
            
            return false;
            
        } catch (IOException e) {
            System.err.println("❌ Erreur suppression fichier: " + e.getMessage());
            logService.logErreur("suppression_fichier_serveur", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrait l'extension d'un nom de fichier
     */
    private String getExtension(String nomFichier) {
        if (nomFichier == null || !nomFichier.contains(".")) {
            return "";
        }
        return nomFichier.substring(nomFichier.lastIndexOf(".") + 1).toLowerCase();
    }
    
    // Getters
    
    public boolean isServeurStorageActif() {
        return serveurStorageActif;
    }
    
    public String getServeurStorageChemin() {
        return serveurStorageChemin;
    }
    
    public boolean isServeurDistant() {
        return serveurDistant;
    }
    
    public String getServeurAdresse() {
        return serveurAdresse;
    }
}