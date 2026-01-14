package application.services;

import application.models.Document;
import application.models.User;
import application.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import application.models.User;
import application.models.StatutDocument;

/**
 * Service de gestion des documents avec nomenclature automatique et stockage réseau
 */
public class DocumentService {
    
    private static DocumentService instance;
    private final DatabaseService databaseService;
    private final NetworkStorageService networkStorageService;
    private SessionManager sessionManager;
    private String cheminStockageLocal;
    
    private DocumentService() {
        this.databaseService = DatabaseService.getInstance();
        this.networkStorageService = NetworkStorageService.getInstance();
        this.cheminStockageLocal = System.getProperty("user.home") + File.separator + 
                                   "Documents" + File.separator + "AppDocuments";
        initialiserStockageLocal();
    }
    
    public static synchronized DocumentService getInstance() {
        if (instance == null) {
            instance = new DocumentService();
        }
        return instance;
    }
    
    /**
     * Initialise le répertoire de stockage local
     */
    private void initialiserStockageLocal() {
        try {
            Path storagePath = Paths.get(cheminStockageLocal);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                System.out.println("✓ Répertoire de stockage local créé: " + cheminStockageLocal);
            }
        } catch (IOException e) {
            System.err.println("✗ Erreur création répertoire local: " + e.getMessage());
        }
    }
    
    /**
     * Crée un nouveau document avec nomenclature automatique
     * Version compatible avec User
     */
    public Document createDocument(Document document, File fichier, User user) throws Exception {
        return createDocument(document, fichier, user.getId());
    }
    
    /**
     * Crée un nouveau document avec nomenclature automatique
     */
    public Document createDocument(Document document, File fichier, int userId) throws Exception {
        // Validation
        if (document.getTitre() == null || document.getTitre().trim().isEmpty()) {
            throw new Exception("Le titre du document est obligatoire");
        }
        
        if (fichier == null || !fichier.exists()) {
            throw new Exception("Le fichier source est invalide");
        }
        
        // Générer code avec nomenclature si dossier spécifié
        if (document.getDossierId() != null && document.getDossierId() > 0) {
            String codeNomenclature = genererCodeDocumentNomenclature(document.getDossierId());
            document.setCodeDocument(codeNomenclature);
        } else {
            // Code simple pour documents sans dossier
            document.setCodeDocument("DOC-" + System.currentTimeMillis());
        }
        
        // Calculer le hash du fichier
        String hashFichier = networkStorageService.calculerHashFichier(fichier);
        document.setHashFichier(hashFichier);
        
        // Extraire informations du fichier
        document.setTailleFichier(fichier.length());
        document.setExtension(getExtension(fichier.getName()));
        document.setMimeType(getMimeType(fichier));
        
        // Copier localement
        String cheminLocal = copierFichierLocal(fichier, document.getCodeDocument());
        document.setCheminFichier(cheminLocal);
        
        // Stocker sur serveur réseau
        String cheminServeur = networkStorageService.stockerFichierServeur(
            new File(cheminLocal), 
            document.getCodeDocument()
        );
        document.setCheminServeur(cheminServeur);
        
        // Insérer en base
        insertDocument(document, userId);
        
        // Enregistrer l'activité
        enregistrerActivite(document.getId(), userId, "creation", 
                          "Document créé: " + document.getTitre());
        
        System.out.println("✓ Document créé: " + document.getCodeDocument());
        
        return document;
    }
    
    /**
     * Génère un code document avec nomenclature automatique
     * Format: INITIALES-ANNÉE-SÉQUENCE (ex: DIV-2025-0001)
     */
    private String genererCodeDocumentNomenclature(int dossierId) throws SQLException {
        int annee = Year.now().getValue();
        
        String query = "SELECT generer_code_document_nomenclature(?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, dossierId);
            stmt.setInt(2, annee);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        
        throw new SQLException("Impossible de générer le code document");
    }
    
    /**
     * Copie un fichier vers le stockage local
     */
    private String copierFichierLocal(File fichierSource, String codeDocument) throws IOException {
        String extension = getExtension(fichierSource.getName());
        String nomFichier = codeDocument + (extension.isEmpty() ? "" : "." + extension);
        
        Path destination = Paths.get(cheminStockageLocal, nomFichier);
        
        Files.copy(fichierSource.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        
        return destination.toString();
    }
    
    /**
     * Insère un document en base de données
     */
    private void insertDocument(Document document, int userId) throws SQLException {
        String query = "INSERT INTO documents (code_document, dossier_id, titre, type_document, " +
                      "chemin_fichier, chemin_serveur, taille_fichier, extension, mime_type, " +
                      "description, mots_cles, hash_fichier, confidentiel, cree_par, modifie_par) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, document.getCodeDocument());
            
            if (document.getDossierId() != null && document.getDossierId() > 0) {
                stmt.setInt(2, document.getDossierId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            
            stmt.setString(3, document.getTitre());
            stmt.setString(4, document.getTypeDocument());
            stmt.setString(5, document.getCheminFichier());
            stmt.setString(6, document.getCheminServeur());
            stmt.setLong(7, document.getTailleFichier());
            stmt.setString(8, document.getExtension());
            stmt.setString(9, document.getMimeType());
            stmt.setString(10, document.getDescription());
            stmt.setString(11, document.getMotsCles());
            stmt.setString(12, document.getHashFichier());
            stmt.setBoolean(13, document.isConfidentiel());
            stmt.setInt(14, userId);
            stmt.setInt(15, userId);
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    document.setId(rs.getInt(1));
                }
            }
        }
    }
    
    /**
     * Enregistre une activité sur un document
     */
    private void enregistrerActivite(int documentId, int userId, String action, String details) {
        String query = "INSERT INTO activites_documents (document_id, user_id, action, details) " +
                      "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, documentId);
            stmt.setInt(2, userId);
            stmt.setString(3, action);
            stmt.setString(4, details);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur enregistrement activité: " + e.getMessage());
        }
    }
    
    /**
     * Archive un document dans un dossier spécifique
     */
    public boolean archiverDocument(int documentId, int dossierId, int userId) {
        String query = "UPDATE documents SET archive = TRUE, dossier_id = ?, " +
                      "date_archivage = NOW(), modifie_par = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, dossierId);
            stmt.setInt(2, userId);
            stmt.setInt(3, documentId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                enregistrerActivite(documentId, userId, "archivage", 
                                  "Document archivé dans dossier ID: " + dossierId);
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Erreur archivage document: " + e.getMessage());
            return false;
        }
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
            System.err.println("Erreur récupération document: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère tous les documents
     */
    public List<Document> getAllDocuments() {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut != 'supprime' " +
                      "ORDER BY date_creation DESC";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                documents.add(mapResultSetToDocument(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération documents: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Recherche de documents
     */
    public List<Document> rechercherDocuments(String recherche) {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut != 'supprime' AND (" +
                      "code_document LIKE ? OR " +
                      "titre LIKE ? OR " +
                      "description LIKE ? OR " +
                      "mots_cles LIKE ?) " +
                      "ORDER BY date_creation DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            String pattern = "%" + recherche + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche documents: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Récupère les documents d'un dossier
     */
    public List<Document> getDocumentsByDossier(int dossierId) {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE dossier_id = ? " +
                      "AND statut != 'supprime' ORDER BY date_creation DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, dossierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération documents dossier: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Supprime un document (soft delete)
     */
    public boolean deleteDocument(int documentId, int userId) {
        String query = "UPDATE documents SET statut = 'supprime', modifie_par = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, documentId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                enregistrerActivite(documentId, userId, "suppression", "Document supprimé");
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression document: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Convertit un ResultSet en objet Document
     */
    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        Document document = new Document();
        
        document.setId(rs.getInt("id"));
        document.setCodeDocument(rs.getString("code_document"));
        document.setTitre(rs.getString("titre"));
        document.setTypeDocument(rs.getString("type_document"));
        
        int dossierId = rs.getInt("dossier_id");
        if (!rs.wasNull()) {
            document.setDossierId(dossierId);
        }
        
        document.setCheminFichier(rs.getString("chemin_fichier"));
        document.setCheminServeur(rs.getString("chemin_serveur"));
        document.setTailleFichier(rs.getLong("taille_fichier"));
        document.setExtension(rs.getString("extension"));
        document.setMimeType(rs.getString("mime_type"));
        document.setDescription(rs.getString("description"));
        document.setMotsCles(rs.getString("mots_cles"));
        
        int creePar = rs.getInt("cree_par");
        if (!rs.wasNull()) {
            document.setCreePar(creePar);
        }
        
        int modifiePar = rs.getInt("modifie_par");
        if (!rs.wasNull()) {
            document.setModifiePar(modifiePar);
        }
        
        document.setStatut(rs.getString("statut"));
        document.setVersion(rs.getInt("version"));
        document.setHashFichier(rs.getString("hash_fichier"));
        document.setConfidentiel(rs.getBoolean("confidentiel"));
        document.setArchive(rs.getBoolean("archive"));
        
        Timestamp dateArchivage = rs.getTimestamp("date_archivage");
        if (dateArchivage != null) {
            document.setDateArchivage(dateArchivage.toLocalDateTime());
        }
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            document.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        Timestamp dateModification = rs.getTimestamp("date_modification");
        if (dateModification != null) {
            document.setDateModification(dateModification.toLocalDateTime());
        }
        
        Timestamp dateExpiration = rs.getTimestamp("date_expiration");
        if (dateExpiration != null) {
            document.setDateExpiration(dateExpiration.toLocalDateTime());
        }
        
        // Informations supplémentaires
        document.setCreateurNom(rs.getString("createur_nom"));
        document.setCreateurService(rs.getString("createur_service"));
        document.setModificateurNom(rs.getString("modificateur_nom"));
        document.setNomDossier(rs.getString("nom_dossier"));
        document.setCodeDossierParent(rs.getString("code_dossier_parent"));
        document.setCheminDossier(rs.getString("chemin_dossier"));
        document.setIconeDossier(rs.getString("icone_dossier"));
        
        return document;
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
     * Détermine le type MIME d'un fichier
     */
    private String getMimeType(File fichier) {
        try {
            return Files.probeContentType(fichier.toPath());
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
    
    /**
     * Met à jour les métadonnées d'un document (sans le code)
     */
    public boolean updateDocument(Document document) throws Exception {
        String query = "UPDATE documents SET " +
                      "titre = ?, type_document = ?, description = ?, " +
                      "mots_cles = ?, confidentiel = ?, dossier_id = ?, " +
                      "modifie_par = ?, date_modification = NOW() " +
                      "WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, document.getTitre());
            stmt.setString(2, document.getTypeDocument());
            stmt.setString(3, document.getDescription());
            stmt.setString(4, document.getMotsCles());
            stmt.setBoolean(5, document.isConfidentiel());
            stmt.setInt(6, document.getDossierId());
            
            User currentUser = sessionManager.getCurrentUser();
            stmt.setInt(7, currentUser.getId());
            stmt.setInt(8, document.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Change le statut d'un document
     */
    public boolean changeStatut(int documentId, String nouveauStatut, int userId) {
        String query = "UPDATE documents SET statut = ?, modifie_par = ?, " +
                      "date_modification = NOW() WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nouveauStatut);
            stmt.setInt(2, userId);
            stmt.setInt(3, documentId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                enregistrerActivite(documentId, userId, "changement_statut", 
                                  "Statut changé vers: " + nouveauStatut);
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Erreur changement statut: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère les documents de la corbeille
     */
    public List<Document> getDocumentsCorbeille() {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut = 'supprime' " +
                      "ORDER BY date_modification DESC";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                documents.add(mapResultSetToDocument(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération corbeille: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Restaure un document de la corbeille
     */
    public boolean restaurerDocument(int documentId, int userId) {
        String query = "UPDATE documents SET statut = 'actif', modifie_par = ?, " +
                      "date_modification = NOW() WHERE id = ? AND statut = 'supprime'";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, documentId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                enregistrerActivite(documentId, userId, "restauration", 
                                  "Document restauré de la corbeille");
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Erreur restauration document: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Supprime définitivement un document (ADMIN SEULEMENT)
     */
    public boolean supprimerDefinitivement(int documentId, User user) {
        // Vérifier que l'utilisateur est administrateur
        if (user == null || !user.getRole().getNom().equals("Administrateur")) {
            System.err.println("Permission refusée: seul un administrateur peut supprimer définitivement");
            return false;
        }
        
        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Récupérer les informations du document avant suppression
                Document doc = getDocumentById(documentId);
                
                if (doc == null) {
                    conn.rollback();
                    return false;
                }
                
                // Supprimer les activités liées
                String deleteActivites = "DELETE FROM activites_documents WHERE document_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteActivites)) {
                    stmt.setInt(1, documentId);
                    stmt.executeUpdate();
                }
                
                // Supprimer les versions
                String deleteVersions = "DELETE FROM versions_documents WHERE document_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteVersions)) {
                    stmt.setInt(1, documentId);
                    stmt.executeUpdate();
                }
                
                // Supprimer le document
                String deleteDoc = "DELETE FROM documents WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteDoc)) {
                    stmt.setInt(1, documentId);
                    stmt.executeUpdate();
                }
                
                // Supprimer le fichier physique si possible
                try {
                    File fichier = new File(doc.getCheminFichier());
                    if (fichier.exists()) {
                        fichier.delete();
                    }
                } catch (Exception e) {
                    System.err.println("Impossible de supprimer le fichier physique: " + e.getMessage());
                }
                
                conn.commit();
                
                System.out.println("Document supprimé définitivement: " + doc.getCodeDocument());
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression définitive: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Déplace un document vers un autre dossier
     */
    public boolean deplacerDocument(int documentId, int nouveauDossierId, int userId) {
        String query = "UPDATE documents SET dossier_id = ?, modifie_par = ?, " +
                      "date_modification = NOW() WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, nouveauDossierId);
            stmt.setInt(2, userId);
            stmt.setInt(3, documentId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                enregistrerActivite(documentId, userId, "deplacement", 
                                  "Document déplacé vers dossier ID: " + nouveauDossierId);
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Erreur déplacement document: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère les documents récents
     */
    public List<Document> getDocumentsRecents(int limite) {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut != 'supprime' " +
                      "ORDER BY date_modification DESC LIMIT ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, limite);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération documents récents: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Récupère les documents favoris d'un utilisateur
     */
    public List<Document> getDocumentsFavoris(int userId) {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT d.* FROM v_documents_complets d " +
                      "INNER JOIN documents_favoris df ON d.id = df.document_id " +
                      "WHERE df.user_id = ? AND d.statut != 'supprime' " +
                      "ORDER BY df.date_ajout DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération favoris: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Ajoute/Retire un document des favoris
     */
    public boolean toggleFavori(int documentId, int userId) {
        try (Connection conn = databaseService.getConnection()) {
            // Vérifier si déjà dans les favoris
            String checkQuery = "SELECT COUNT(*) FROM documents_favoris WHERE document_id = ? AND user_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                stmt.setInt(1, documentId);
                stmt.setInt(2, userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Retirer des favoris
                        String deleteQuery = "DELETE FROM documents_favoris WHERE document_id = ? AND user_id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                            deleteStmt.setInt(1, documentId);
                            deleteStmt.setInt(2, userId);
                            deleteStmt.executeUpdate();
                        }
                        return false; // Retiré
                    } else {
                        // Ajouter aux favoris
                        String insertQuery = "INSERT INTO documents_favoris (document_id, user_id) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                            insertStmt.setInt(1, documentId);
                            insertStmt.setInt(2, userId);
                            insertStmt.executeUpdate();
                        }
                        return true; // Ajouté
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur gestion favoris: " + e.getMessage());
            return false;
        }
    }


    
    // Getters
    
    public String getCheminStockageLocal() {
        return cheminStockageLocal;
    }
}