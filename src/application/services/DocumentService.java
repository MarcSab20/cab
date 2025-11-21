package application.services;

import application.models.Document;
import application.models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DocumentService {
    
    private static DocumentService instance;
    private final DatabaseService databaseService;
    
    private DocumentService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized DocumentService getInstance() {
        if (instance == null) {
            instance = new DocumentService();
        }
        return instance;
    }
    
    public List<Document> getAllDocuments() {
        List<Document> documents = new ArrayList<>();
        String query = "SELECT * FROM documents ORDER BY date_creation DESC";
        
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
    
    public Document getDocumentById(int id) {
        String query = "SELECT * FROM documents WHERE id = ?";
        
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
    
    public boolean saveDocument(Document document) {
        if (document.getId() == 0) {
            return insertDocument(document);
        } else {
            return updateDocument(document);
        }
    }
    
    private boolean insertDocument(Document document) {
        String query = """
            INSERT INTO documents (titre, type_document, chemin_fichier, taille_fichier,
                                 cree_par, statut, description, extension, mime_type,
                                 version, confidentiel, mots_cles, date_creation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, document.getTitre());
            stmt.setString(2, document.getTypeDocument());
            stmt.setString(3, document.getCheminFichier());
            stmt.setLong(4, document.getTailleFichier());
            stmt.setInt(5, document.getCreePar() != null ? document.getCreePar().getId() : null);
            stmt.setString(6, document.getStatut().name().toLowerCase());
            stmt.setString(7, document.getDescription());
            stmt.setString(8, document.getExtension());
            stmt.setString(9, document.getMimeType());
            stmt.setInt(10, document.getVersion());
            stmt.setBoolean(11, document.isConfidentiel());
            stmt.setString(12, document.getMotsCles());
            stmt.setTimestamp(13, Timestamp.valueOf(document.getDateCreation()));
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        document.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion du document: " + e.getMessage());
        }
        
        return false;
    }
    
    private boolean updateDocument(Document document) {
        String query = """
            UPDATE documents SET titre = ?, description = ?, statut = ?,
                               mots_cles = ?, date_modification = ?, modifie_par = ?
            WHERE id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, document.getTitre());
            stmt.setString(2, document.getDescription());
            stmt.setString(3, document.getStatut().name().toLowerCase());
            stmt.setString(4, document.getMotsCles());
            stmt.setTimestamp(5, document.getDateModification() != null ?
                Timestamp.valueOf(document.getDateModification()) : null);
            stmt.setInt(6, document.getModifiePar() != null ? document.getModifiePar().getId() : null);
            stmt.setInt(7, document.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du document: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean deleteDocument(int id) {
        String query = "DELETE FROM documents WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du document: " + e.getMessage());
        }
        
        return false;
    }
    
    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        Document document = new Document();
        document.setId(rs.getInt("id"));
        document.setTitre(rs.getString("titre"));
        document.setTypeDocument(rs.getString("type_document"));
        document.setCheminFichier(rs.getString("chemin_fichier"));
        document.setTailleFichier(rs.getLong("taille_fichier"));
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            document.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        Timestamp dateModification = rs.getTimestamp("date_modification");
        if (dateModification != null) {
            document.setDateModification(dateModification.toLocalDateTime());
        }
        
        document.setDescription(rs.getString("description"));
        document.setExtension(rs.getString("extension"));
        document.setMimeType(rs.getString("mime_type"));
        document.setVersion(rs.getInt("version"));
        document.setConfidentiel(rs.getBoolean("confidentiel"));
        document.setMotsCles(rs.getString("mots_cles"));
        
        return document;
    }
}