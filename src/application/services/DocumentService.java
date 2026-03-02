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
 * VERSION AMÉLIORÉE avec recherche avancée
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
    
    // ==================== NOUVELLE FONCTIONNALITÉ: RECHERCHE DE DOCUMENTS ====================
    
    /**
     * Recherche avancée de documents
     * Recherche dans: code, titre, description, mots-clés, nom de fichier
     * @param recherche Terme de recherche
     * @return Liste des documents correspondants
     */
    public List<Document> rechercherDocuments(String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) {
            return getAllDocuments();
        }
        
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut != 'supprime' AND (" +
                      "code_document LIKE ? OR " +
                      "titre LIKE ? OR " +
                      "description LIKE ? OR " +
                      "mots_cles LIKE ? OR " +
                      "extension LIKE ?) " +
                      "ORDER BY date_modification DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            String pattern = "%" + recherche + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            stmt.setString(5, pattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
            System.out.println("🔍 Recherche '" + recherche + "': " + documents.size() + " résultat(s)");
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche documents: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Recherche de documents avec filtres avancés
     * @param recherche Terme de recherche
     * @param typeDocument Type de document (null pour tous)
     * @param dossierId ID du dossier (null pour tous)
     * @param confidentiel Filtre confidentialité (null pour tous)
     * @return Liste des documents correspondants
     */
    public List<Document> rechercherDocumentsAvance(String recherche, String typeDocument, 
                                                     Integer dossierId, Boolean confidentiel) {
        List<Document> documents = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder();
        
        queryBuilder.append("SELECT * FROM v_documents_complets WHERE statut != 'supprime' ");
        
        List<Object> params = new ArrayList<>();
        
        // Recherche textuelle
        if (recherche != null && !recherche.trim().isEmpty()) {
            queryBuilder.append("AND (code_document LIKE ? OR titre LIKE ? OR description LIKE ? OR mots_cles LIKE ?) ");
            String pattern = "%" + recherche + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        
        // Filtre par type
        if (typeDocument != null && !typeDocument.isEmpty()) {
            queryBuilder.append("AND type_document = ? ");
            params.add(typeDocument);
        }
        
        // Filtre par dossier
        if (dossierId != null) {
            queryBuilder.append("AND dossier_id = ? ");
            params.add(dossierId);
        }
        
        // Filtre par confidentialité
        if (confidentiel != null) {
            queryBuilder.append("AND confidentiel = ? ");
            params.add(confidentiel);
        }
        
        queryBuilder.append("ORDER BY date_modification DESC");
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {
            
            // Définir les paramètres
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Boolean) {
                    stmt.setBoolean(i + 1, (Boolean) param);
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
            System.out.println("🔍 Recherche avancée: " + documents.size() + " résultat(s)");
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche avancée: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Recherche de documents par extension
     * @param extension Extension du fichier (ex: "pdf", "docx")
     * @return Liste des documents
     */
    public List<Document> rechercherParExtension(String extension) {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut != 'supprime' " +
                      "AND extension LIKE ? ORDER BY date_modification DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, extension);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche par extension: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Recherche de documents par période
     * @param dateDebut Date de début (format SQL)
     * @param dateFin Date de fin (format SQL)
     * @return Liste des documents
     */
    public List<Document> rechercherParPeriode(Timestamp dateDebut, Timestamp dateFin) {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut != 'supprime' " +
                      "AND date_creation BETWEEN ? AND ? ORDER BY date_creation DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setTimestamp(1, dateDebut);
            stmt.setTimestamp(2, dateFin);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche par période: " + e.getMessage());
        }
        
        return documents;
    }
    
    // ==================== MÉTHODES EXISTANTES (conservées) ====================
    
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
     * Met à jour un document
     */
    public boolean updateDocument(Document document, int userId) throws Exception {
        if (document == null || document.getId() <= 0) {
            throw new Exception("Document invalide");
        }
        
        String query = "UPDATE documents SET " +
                      "titre = ?, " +
                      "type_document = ?, " +
                      "description = ?, " +
                      "mots_cles = ?, " +
                      "confidentiel = ?, " +
                      "modifie_par = ?, " +
                      "date_modification = NOW() " +
                      "WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, document.getTitre());
            stmt.setString(2, document.getTypeDocument());
            stmt.setString(3, document.getDescription());
            stmt.setString(4, document.getMotsCles());
            stmt.setBoolean(5, document.isConfidentiel());
            stmt.setInt(6, userId);
            stmt.setInt(7, document.getId());
            
            boolean result = stmt.executeUpdate() > 0;
            
            if (result) {
                enregistrerActivite(document.getId(), userId, "modification", 
                                  "Document modifié");
            }
            
            return result;
            
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour document: " + e.getMessage());
            throw new Exception("Erreur lors de la mise à jour du document", e);
        }
    }
    
    /**
     * Récupère tous les documents actifs
     */
    public List<Document> getAllDocuments() {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE statut != 'supprime' " +
                      "ORDER BY date_modification DESC";
        
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
     * Récupère les documents d'un dossier
     */
    public List<Document> getDocumentsByDossier(int dossierId) {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM v_documents_complets WHERE dossier_id = ? " +
                      "AND statut != 'supprime' ORDER BY date_modification DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, dossierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocument(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération documents du dossier: " + e.getMessage());
        }
        
        return documents;
    }
    
    /**
     * Supprime un document (mise à la corbeille)
     */
    public boolean supprimerDocument(int documentId, int userId) {
        String query = "UPDATE documents SET statut = 'supprime', modifie_par = ?, " +
                      "date_modification = NOW() WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, documentId);
            
            boolean result = stmt.executeUpdate() > 0;
            
            if (result) {
                enregistrerActivite(documentId, userId, "suppression", 
                                  "Document mis à la corbeille");
            }
            
            return result;
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression document: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Restaure un document de la corbeille
     */
    public boolean restaurerDocument(int documentId, int userId) {
        String query = "UPDATE documents SET statut = 'actif', modifie_par = ?, " +
                      "date_modification = NOW() WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, documentId);
            
            boolean result = stmt.executeUpdate() > 0;
            
            if (result) {
                enregistrerActivite(documentId, userId, "restauration", 
                                  "Document restauré");
            }
            
            return result;
            
        } catch (SQLException e) {
            System.err.println("Erreur restauration document: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Supprime définitivement un document (administrateurs uniquement)
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
    
    /**
     * Enregistre une activité sur un document
     */
    private void enregistrerActivite(int documentId, int userId, String typeActivite, String description) {
        String query = "INSERT INTO activites_documents (document_id, user_id, type_activite, description) " +
                      "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, documentId);
            stmt.setInt(2, userId);
            stmt.setString(3, typeActivite);
            stmt.setString(4, description);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur enregistrement activité: " + e.getMessage());
        }
    }
    
    /**
     * Convertit un ResultSet en objet Document
     */
    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        Document doc = new Document();
        
        doc.setId(rs.getInt("id"));
        doc.setCodeDocument(rs.getString("code_document"));
        
        int dossierId = rs.getInt("dossier_id");
        if (!rs.wasNull()) {
            doc.setDossierId(dossierId);
        }
        
        doc.setTitre(rs.getString("titre"));
        doc.setTypeDocument(rs.getString("type_document"));
        doc.setCheminFichier(rs.getString("chemin_fichier"));
        doc.setCheminServeur(rs.getString("chemin_serveur"));
        doc.setTailleFichier(rs.getLong("taille_fichier"));
        doc.setExtension(rs.getString("extension"));
        doc.setMimeType(rs.getString("mime_type"));
        doc.setDescription(rs.getString("description"));
        doc.setMotsCles(rs.getString("mots_cles"));
        doc.setHashFichier(rs.getString("hash_fichier"));
        doc.setConfidentiel(rs.getBoolean("confidentiel"));
        doc.setStatut(rs.getString("statut").toUpperCase());
        
        int creePar = rs.getInt("cree_par");
        if (!rs.wasNull()) {
            doc.setCreePar(creePar);
        }
        
        int modifiePar = rs.getInt("modifie_par");
        if (!rs.wasNull()) {
            doc.setModifiePar(modifiePar);
        }
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            doc.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        Timestamp dateModification = rs.getTimestamp("date_modification");
        if (dateModification != null) {
            doc.setDateModification(dateModification.toLocalDateTime());
        }
        
        // Informations supplémentaires de la vue
        doc.setNomDossier(rs.getString("nom_dossier"));
        doc.setIconeDossier(rs.getString("icone_dossier"));
        
        // Gestion de nom_auteur (peut ne pas exister dans la vue)
        try {
            doc.setNomAuteur(rs.getString("nom_auteur"));
        } catch (SQLException e) {
            // Si la colonne n'existe pas, utiliser une valeur par défaut
            doc.setNomAuteur("Utilisateur");
            System.out.println("⚠️ Colonne 'nom_auteur' non disponible dans la vue SQL");
        }
        
        doc.setVersion(rs.getInt("version_actuelle"));
        
        return doc;
    }
    
    /**
     * Extrait l'extension d'un nom de fichier
     */
    private String getExtension(String nomFichier) {
        if (nomFichier == null) return "";
        int lastDot = nomFichier.lastIndexOf('.');
        return (lastDot > 0) ? nomFichier.substring(lastDot + 1) : "";
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
    
    // Getters
    
    public String getCheminStockageLocal() {
        return cheminStockageLocal;
    }
}