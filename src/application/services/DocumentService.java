package application.services;

import application.models.Document;
import application.models.User;
import application.models.VersionDocument;
import application.models.Dossier;
import application.models.StatutDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des documents avec fonctionnalités complètes
 * - Génération automatique de codes
 * - Gestion des versions
 * - Organisation en dossiers
 * - Upload et téléchargement de fichiers
 */
public class DocumentService {
    
    private static DocumentService instance;
    private final DatabaseService databaseService;
    private final DossierService dossierService;
    
    // Répertoire racine pour le stockage des fichiers
    private static final String STORAGE_ROOT = "C:\\DocumentManagement\\files";
    
    private DocumentService() {
        this.databaseService = DatabaseService.getInstance();
        this.dossierService = DossierService.getInstance();
        initializeStorage();
    }
    
    public static synchronized DocumentService getInstance() {
        if (instance == null) {
            instance = new DocumentService();
        }
        return instance;
    }
    
    /**
     * Initialise le système de stockage des fichiers
     */
    private void initializeStorage() {
        try {
            Path storagePath = Paths.get(STORAGE_ROOT);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                System.out.println("Répertoire de stockage créé: " + STORAGE_ROOT);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire de stockage: " + e.getMessage());
        }
    }
    
    /**
     * Génère un code document automatique
     * Format: DOC-[TYPE]-[ANNÉE]-[SÉQUENCE]-[SERVICE]
     */
    public String genererCodeDocument(String typeDocument, String serviceCode) {
        String query = "SELECT generer_code_document(?, ?) AS code";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, typeDocument);
            stmt.setString(2, serviceCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("code");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la génération du code document: " + e.getMessage());
        }
        
        // Fallback si la fonction MySQL ne fonctionne pas
        return genererCodeDocumentFallback(typeDocument, serviceCode);
    }
    
    /**
     * Génération de code en fallback (sans fonction MySQL)
     */
    private String genererCodeDocumentFallback(String typeDocument, String serviceCode) {
        int annee = java.time.Year.now().getValue();
        int sequence = getNextSequence(typeDocument, annee);
        
        return String.format("DOC-%s-%d-%04d-%s", 
            typeDocument, annee, sequence, serviceCode);
    }
    
    /**
     * Récupère la prochaine séquence pour un type de document
     */
    private int getNextSequence(String typeDocument, int annee) {
        String query = """
            INSERT INTO sequences_documents (type_document, annee, derniere_sequence)
            VALUES (?, ?, 1)
            ON DUPLICATE KEY UPDATE derniere_sequence = derniere_sequence + 1
        """;
        
        String selectQuery = """
            SELECT derniere_sequence FROM sequences_documents
            WHERE type_document = ? AND annee = ?
        """;
        
        try (Connection conn = databaseService.getConnection()) {
            // Insérer ou mettre à jour
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, typeDocument);
                stmt.setInt(2, annee);
                stmt.executeUpdate();
            }
            
            // Récupérer la séquence
            try (PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
                stmt.setString(1, typeDocument);
                stmt.setInt(2, annee);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("derniere_sequence");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la séquence: " + e.getMessage());
        }
        
        return 1; // Par défaut
    }
    
    /**
     * Crée un nouveau document avec upload de fichier
     */
    public boolean createDocument(Document document, File fichierSource, User user) {
        try {
            // Validation
            if (fichierSource == null || !fichierSource.exists()) {
                System.err.println("Fichier source invalide");
                return false;
            }
            
            // Génération du code si nécessaire
            if (document.getCodeDocument() == null) {
                String serviceCode = user.getServiceCode() != null ? user.getServiceCode() : "SYS";
                document.setCodeDocument(genererCodeDocument(
                    document.getTypeDocument(), 
                    serviceCode
                ));
            }
            
            // Calcul du hash du fichier
            String hash = calculerHashFichier(fichierSource);
            document.setHash(hash);
            
            // Stockage du fichier
            String cheminStockage = stockerFichier(fichierSource, document.getCodeDocument());
            if (cheminStockage == null) {
                return false;
            }
            
            document.setCheminFichier(cheminStockage);
            document.setTailleFichier(fichierSource.length());
            document.setCreePar(user);
            document.setDateCreation(LocalDateTime.now());
            document.setVersion(1);
            document.setStatut(StatutDocument.ACTIF);
            
            // Insertion en base de données
            return insertDocument(document);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du document: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Insère un document en base de données
     */
    private boolean insertDocument(Document document) {
        String query = """
            INSERT INTO documents (
                code_document, titre, type_document, dossier_id, chemin_fichier, 
                taille_fichier, extension, mime_type, description, mots_cles,
                cree_par, statut, version, hash_fichier, confidentiel, date_creation
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, document.getCodeDocument());
            stmt.setString(2, document.getTitre());
            stmt.setString(3, document.getTypeDocument());
            
            if (document.getDossierId() != null) {
                stmt.setInt(4, document.getDossierId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            stmt.setString(5, document.getCheminFichier());
            stmt.setLong(6, document.getTailleFichier());
            stmt.setString(7, document.getExtension());
            stmt.setString(8, document.getMimeType());
            stmt.setString(9, document.getDescription());
            stmt.setString(10, document.getMotsCles());
            
            if (document.getCreePar() != null) {
                stmt.setInt(11, document.getCreePar().getId());
            } else {
                stmt.setNull(11, Types.INTEGER);
            }
            
            stmt.setString(12, document.getStatut().name().toLowerCase());
            stmt.setInt(13, document.getVersion());
            stmt.setString(14, document.getHash());
            stmt.setBoolean(15, document.isConfidentiel());
            stmt.setTimestamp(16, Timestamp.valueOf(document.getDateCreation()));
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        document.setId(generatedKeys.getInt(1));
                    }
                }
                
                // Enregistrer l'activité
                logActivite(document.getId(), document.getCreePar().getId(), "creation", 
                    "Document créé: " + document.getTitre());
                
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion du document: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Récupère tous les documents d'un dossier
     */
    public List<Document> getDocumentsByDossier(int dossierId) {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE dossier_id = ? AND statut != 'supprime' ORDER BY date_creation DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, dossierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des documents: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Récupère un document par son code
     */
    public Document getDocumentByCode(String codeDocument) {
        String query = "SELECT * FROM v_documents_complets WHERE code_document = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, codeDocument);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDocument(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du document: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère un document par son ID
     */
    public Document getDocumentById(int id) {
        String query = "SELECT * FROM v_documents_complets WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDocument(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du document: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère tous les documents
     */
    public List<Document> getAllDocuments() {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut != 'supprime' ORDER BY date_creation DESC";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                documents.add(mapResultSetToDocument(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des documents: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Met à jour un document (nouvelle version)
     */
    public boolean updateDocument(Document document, File nouveauFichier, User user) {
        try {
            // Vérifier les permissions
            if (!peutModifierDocument(document, user)) {
                System.err.println("Permissions insuffisantes pour modifier ce document");
                return false;
            }
            
            if (nouveauFichier != null && nouveauFichier.exists()) {
                // Archiver la version actuelle
                archiverVersionActuelle(document);
                
                // Calculer le nouveau hash
                String nouveauHash = calculerHashFichier(nouveauFichier);
                
                // Stocker le nouveau fichier
                String nouveauChemin = stockerFichier(nouveauFichier, document.getCodeDocument());
                if (nouveauChemin == null) {
                    return false;
                }
                
                // Mettre à jour le document
                document.setCheminFichier(nouveauChemin);
                document.setTailleFichier(nouveauFichier.length());
                document.setHash(nouveauHash);
                document.setVersion(document.getVersion() + 1);
            }
            
            document.setModifiePar(user);
            document.setDateModification(LocalDateTime.now());
            
            // Mise à jour en base
            String query = """
                UPDATE documents SET 
                    titre = ?, description = ?, mots_cles = ?, dossier_id = ?,
                    chemin_fichier = ?, taille_fichier = ?, hash_fichier = ?,
                    version = ?, modifie_par = ?, date_modification = ?, statut = ?
                WHERE id = ?
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, document.getTitre());
                stmt.setString(2, document.getDescription());
                stmt.setString(3, document.getMotsCles());
                
                if (document.getDossierId() != null) {
                    stmt.setInt(4, document.getDossierId());
                } else {
                    stmt.setNull(4, Types.INTEGER);
                }
                
                stmt.setString(5, document.getCheminFichier());
                stmt.setLong(6, document.getTailleFichier());
                stmt.setString(7, document.getHash());
                stmt.setInt(8, document.getVersion());
                stmt.setInt(9, user.getId());
                stmt.setTimestamp(10, Timestamp.valueOf(document.getDateModification()));
                stmt.setString(11, document.getStatut().name().toLowerCase());
                stmt.setInt(12, document.getId());
                
                boolean success = stmt.executeUpdate() > 0;
                
                if (success) {
                    logActivite(document.getId(), user.getId(), "modification",
                        "Document mis à jour: version " + document.getVersion());
                }
                
                return success;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du document: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Supprime un document (uniquement pour niveau 0, 1 et admin)
     */
    public boolean deleteDocument(int documentId, User user) {
        // Vérifier les permissions
        if (user == null || user.getNiveauAutorite() > 1) {
            System.err.println("Permissions insuffisantes pour supprimer un document");
            return false;
        }
        
        Document document = getDocumentById(documentId);
        if (document == null) {
            return false;
        }
        
        String query = "DELETE FROM documents WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, documentId);
            boolean success = stmt.executeUpdate() > 0;
            
            if (success) {
                // Supprimer le fichier physique
                supprimerFichierPhysique(document.getCheminFichier());
                
                logActivite(documentId, user.getId(), "suppression",
                    "Document supprimé: " + document.getTitre());
            }
            
            return success;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du document: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Déplace un document vers un autre dossier
     */
    public boolean deplacerDocument(int documentId, int nouveauDossierId) {
        String query = "UPDATE documents SET dossier_id = ?, date_modification = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, nouveauDossierId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, documentId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors du déplacement du document: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère les versions d'un document
     */
    public List<VersionDocument> getVersionsDocument(int documentId) {
        List<VersionDocument> versions = new ArrayList<>();
        String query = """
            SELECT v.*, u.nom, u.prenom, d.code_document
            FROM versions_documents v
            LEFT JOIN users u ON v.cree_par = u.id
            LEFT JOIN documents d ON v.document_id = d.id
            WHERE v.document_id = ?
            ORDER BY v.numero_version DESC
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, documentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    versions.add(mapResultSetToVersionDocument(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des versions: " + e.getMessage());
        }
        
        return versions;
    }
    
    /**
     * Recherche de documents
     */
    public List<Document> rechercherDocuments(String recherche, String typeDoc, String statutDoc, Integer dossierId) {
        List<Document> documents = new ArrayList<>();
        StringBuilder query = new StringBuilder("SELECT * FROM v_documents_complets WHERE statut != 'supprime'");
        
        List<Object> params = new ArrayList<>();
        
        if (recherche != null && !recherche.isEmpty()) {
            query.append(" AND (titre LIKE ? OR description LIKE ? OR mots_cles LIKE ? OR code_document LIKE ?)");
            String searchPattern = "%" + recherche + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (typeDoc != null && !typeDoc.isEmpty() && !typeDoc.equals("Tous")) {
            query.append(" AND type_document = ?");
            params.add(typeDoc);
        }
        
        if (statutDoc != null && !statutDoc.isEmpty() && !statutDoc.equals("Tous")) {
            query.append(" AND statut = ?");
            params.add(statutDoc.toLowerCase());
        }
        
        if (dossierId != null) {
            query.append(" AND dossier_id = ?");
            params.add(dossierId);
        }
        
        query.append(" ORDER BY date_creation DESC LIMIT 100");
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche: " + e.getMessage());
        }
        
        return documents;
    }
    
    // Méthodes utilitaires privées
    
    /**
     * Stocke un fichier physiquement
     */
    private String stockerFichier(File fichierSource, String codeDocument) {
        try {
            // Créer un sous-répertoire basé sur l'année
            int annee = java.time.Year.now().getValue();
            Path repertoireAnnee = Paths.get(STORAGE_ROOT, String.valueOf(annee));
            Files.createDirectories(repertoireAnnee);
            
            // Générer le nom de fichier
            String extension = getExtension(fichierSource.getName());
            String nomFichier = codeDocument + (extension.isEmpty() ? "" : "." + extension);
            
            Path destination = repertoireAnnee.resolve(nomFichier);
            
            // Copier le fichier
            Files.copy(fichierSource.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            
            return destination.toString();
        } catch (IOException e) {
            System.err.println("Erreur lors du stockage du fichier: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Supprime un fichier physique
     */
    private void supprimerFichierPhysique(String cheminFichier) {
        try {
            if (cheminFichier != null) {
                Files.deleteIfExists(Paths.get(cheminFichier));
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression du fichier: " + e.getMessage());
        }
    }
    
    /**
     * Calcule le hash SHA-256 d'un fichier
     */
    private String calculerHashFichier(File fichier) {
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
            System.err.println("Erreur lors du calcul du hash: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Archive la version actuelle d'un document
     */
    private void archiverVersionActuelle(Document document) {
        String query = """
            INSERT INTO versions_documents (document_id, numero_version, chemin_fichier, 
                                           taille_fichier, hash_fichier, cree_par)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, document.getId());
            stmt.setInt(2, document.getVersion());
            stmt.setString(3, document.getCheminFichier());
            stmt.setLong(4, document.getTailleFichier());
            stmt.setString(5, document.getHash());
            
            if (document.getModifiePar() != null) {
                stmt.setInt(6, document.getModifiePar().getId());
            } else if (document.getCreePar() != null) {
                stmt.setInt(6, document.getCreePar().getId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'archivage de la version: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie si un utilisateur peut modifier un document
     */
    private boolean peutModifierDocument(Document document, User user) {
        if (user == null) return false;
        
        // Niveau 0 et 1 peuvent tout modifier
        if (user.getNiveauAutorite() <= 1) return true;
        
        // Sinon, seulement ses propres documents
        return document.getCreePar() != null && 
               document.getCreePar().getId() == user.getId();
    }
    
    /**
     * Enregistre une activité sur un document
     */
    private void logActivite(int documentId, int userId, String action, String details) {
        String query = """
            INSERT INTO activites_documents (document_id, user_id, action, details)
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, documentId);
            stmt.setInt(2, userId);
            stmt.setString(3, action);
            stmt.setString(4, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'enregistrement de l'activité: " + e.getMessage());
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
    
    /**
     * Mappe un ResultSet vers un Document
     */
    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        Document doc = new Document();
        
        doc.setId(rs.getInt("id"));
        doc.setCodeDocument(rs.getString("code_document"));
        doc.setTitre(rs.getString("titre"));
        doc.setTypeDocument(rs.getString("type_document"));
        
        int dossierId = rs.getInt("dossier_id");
        if (!rs.wasNull()) {
            doc.setDossierId(dossierId);
        }
        
        doc.setCheminFichier(rs.getString("chemin_fichier"));
        doc.setTailleFichier(rs.getLong("taille_fichier"));
        doc.setExtension(rs.getString("extension"));
        doc.setMimeType(rs.getString("mime_type"));
        doc.setDescription(rs.getString("description"));
        doc.setMotsCles(rs.getString("mots_cles"));
        doc.setVersion(rs.getInt("version"));
        doc.setHash(rs.getString("hash_fichier"));
        doc.setConfidentiel(rs.getBoolean("confidentiel"));
        
        // Statut
        String statutStr = rs.getString("statut");
        doc.setStatut(StatutDocument.fromDatabase(statutStr));
        
        // Dates
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            doc.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        Timestamp dateModification = rs.getTimestamp("date_modification");
        if (dateModification != null) {
            doc.setDateModification(dateModification.toLocalDateTime());
        }
        
        Timestamp dateExpiration = rs.getTimestamp("date_expiration");
        if (dateExpiration != null) {
            doc.setDateExpiration(dateExpiration.toLocalDateTime());
        }
        
        // Créateur (si présent dans la vue)
        try {
            int createurId = rs.getInt("cree_par");
            if (!rs.wasNull()) {
                User createur = new User();
                createur.setId(createurId);
                
                String createurNom = rs.getString("createur_nom");
                String createurPrenom = rs.getString("createur_prenom");
                
                if (createurNom != null && createurPrenom != null) {
                    createur.setNom(createurNom);
                    createur.setPrenom(createurPrenom);
                }
                
                doc.setCreePar(createur);
            }
        } catch (SQLException e) {
            // Colonnes non présentes, ignorer
        }
        
        return doc;
    }
    
    /**
     * Mappe un ResultSet vers une VersionDocument
     */
    private VersionDocument mapResultSetToVersionDocument(ResultSet rs) throws SQLException {
        VersionDocument version = new VersionDocument();
        
        version.setId(rs.getInt("id"));
        version.setDocumentId(rs.getInt("document_id"));
        version.setNumeroVersion(rs.getInt("numero_version"));
        version.setCheminFichier(rs.getString("chemin_fichier"));
        version.setTailleFichier(rs.getLong("taille_fichier"));
        version.setHashFichier(rs.getString("hash_fichier"));
        version.setCommentaire(rs.getString("commentaire"));
        
        int createurId = rs.getInt("cree_par");
        if (!rs.wasNull()) {
            version.setCreePar(createurId);
        }
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            version.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        // Informations additionnelles
        try {
            String nom = rs.getString("nom");
            String prenom = rs.getString("prenom");
            if (nom != null && prenom != null) {
                version.setCreateurNom(prenom + " " + nom);
            }
            
            version.setCodeDocument(rs.getString("code_document"));
        } catch (SQLException e) {
            // Colonnes non présentes
        }
        
        return version;
    }
}