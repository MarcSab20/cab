package application.services;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.zip.*;

/**
 * Service de sauvegarde des documents sur disques amovibles
 */
public class BackupService {
    
    private static BackupService instance;
    private final DatabaseService databaseService;
    private final NetworkStorageService networkStorageService;
    
    private BackupService() {
        this.databaseService = DatabaseService.getInstance();
        this.networkStorageService = NetworkStorageService.getInstance();
    }
    
    public static synchronized BackupService getInstance() {
        if (instance == null) {
            instance = new BackupService();
        }
        return instance;
    }
    
    /**
     * Crée une sauvegarde complète de tous les documents
     */
    public boolean creerSauvegardeComplete(String cheminDestination, boolean compress, int userId) {
        return creerSauvegarde("complete", cheminDestination, compress, userId, null);
    }
    
    /**
     * Crée une sauvegarde incrémentale (documents modifiés depuis dernière sauvegarde)
     */
    public boolean creerSauvegardeIncrementale(String cheminDestination, boolean compress, int userId) {
        LocalDateTime derniereSauvegarde = getDateDerniereSauvegarde();
        return creerSauvegarde("incrementale", cheminDestination, compress, userId, derniereSauvegarde);
    }
    
    private boolean creerSauvegarde(String type, String cheminDestination, boolean compress, 
                                   int userId, LocalDateTime depuis) {
        int sauvegardeId = -1;
        
        try {
            // Créer l'enregistrement de sauvegarde
            sauvegardeId = creerEnregistrementSauvegarde(type, cheminDestination, compress, userId);
            
            if (sauvegardeId == -1) {
                return false;
            }
            
            // Récupérer les documents à sauvegarder
            String query = "SELECT chemin_serveur, code_document FROM documents WHERE statut != 'supprime'";
            if (depuis != null) {
                query += " AND date_modification >= ?";
            }
            
            Path destPath = Paths.get(cheminDestination);
            if (!Files.exists(destPath)) {
                Files.createDirectories(destPath);
            }
            
            // Créer un sous-répertoire avec la date
            String nomSauvegarde = "backup_" + 
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path sauvegardeDir = destPath.resolve(nomSauvegarde);
            Files.createDirectories(sauvegardeDir);
            
            int nombreFichiers = 0;
            long tailleTotale = 0;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                if (depuis != null) {
                    stmt.setTimestamp(1, Timestamp.valueOf(depuis));
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String cheminServeur = rs.getString("chemin_serveur");
                        String codeDocument = rs.getString("code_document");
                        
                        if (cheminServeur != null && networkStorageService.fichierExisteSurServeur(cheminServeur)) {
                            Path source = Paths.get(cheminServeur);
                            String nomFichier = source.getFileName().toString();
                            Path dest = sauvegardeDir.resolve(nomFichier);
                            
                            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                            nombreFichiers++;
                            tailleTotale += Files.size(dest);
                        }
                    }
                }
            }
            
            // Compression si demandée
            Path finalPath = sauvegardeDir;
            if (compress) {
                finalPath = compresserSauvegarde(sauvegardeDir);
                // Supprimer le répertoire non compressé
                deleteDirectory(sauvegardeDir.toFile());
            }
            
            // Mettre à jour l'enregistrement
            terminerSauvegarde(sauvegardeId, nombreFichiers, tailleTotale, finalPath.toString());
            
            System.out.println("✓ Sauvegarde " + type + " terminée: " + nombreFichiers + " fichiers");
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
            
            if (sauvegardeId != -1) {
                marquerSauvegardeErreur(sauvegardeId, e.getMessage());
            }
            
            return false;
        }
    }
    
    private Path compresserSauvegarde(Path source) throws IOException {
        Path zipPath = Paths.get(source.toString() + ".zip");
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            Files.walk(source)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String zipEntryName = source.relativize(path).toString();
                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        System.err.println("Erreur compression: " + e.getMessage());
                    }
                });
        }
        
        return zipPath;
    }
    
    private int creerEnregistrementSauvegarde(String type, String chemin, boolean compress, int userId) {
        String query = "INSERT INTO sauvegardes (type_sauvegarde, chemin_destination, compress, cree_par, statut) VALUES (?, ?, ?, ?, 'en_cours')";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, type);
            stmt.setString(2, chemin);
            stmt.setBoolean(3, compress);
            stmt.setInt(4, userId);
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur création enregistrement sauvegarde: " + e.getMessage());
        }
        
        return -1;
    }
    
    private void terminerSauvegarde(int id, int nombreFichiers, long tailleTotale, String chemin) {
        String query = "UPDATE sauvegardes SET statut = 'termine', nombre_fichiers = ?, taille_totale = ?, chemin_destination = ?, date_fin = NOW() WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, nombreFichiers);
            stmt.setLong(2, tailleTotale);
            stmt.setString(3, chemin);
            stmt.setInt(4, id);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur finalisation sauvegarde: " + e.getMessage());
        }
    }
    
    private void marquerSauvegardeErreur(int id, String message) {
        String query = "UPDATE sauvegardes SET statut = 'erreur', message_erreur = ?, date_fin = NOW() WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, message);
            stmt.setInt(2, id);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur marquage erreur: " + e.getMessage());
        }
    }
    
    private LocalDateTime getDateDerniereSauvegarde() {
        String query = "SELECT MAX(date_fin) FROM sauvegardes WHERE statut = 'termine'";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                return ts != null ? ts.toLocalDateTime() : null;
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération dernière sauvegarde: " + e.getMessage());
        }
        
        return null;
    }
    
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}