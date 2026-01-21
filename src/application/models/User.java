package application.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle représentant un utilisateur du système
 */
public class User {
    private int id;
    private String code;
    private String password;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dernierAcces;
    private String sessionToken;
    private String serviceCode;
    private int niveauAutorite;
    
    // Constructeurs
    public User() {}
    
    public User(String code, String password, String nom, String prenom, String email, Role role) {
        this.code = code;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
        this.actif = true;
        this.dateCreation = LocalDateTime.now();
        this.niveauAutorite = 0;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getNom() {
        return nom;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public String getPrenom() {
        return prenom;
    }
    
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
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
    
    public LocalDateTime getDernierAcces() {
        return dernierAcces;
    }
    
    public void setDernierAcces(LocalDateTime dernierAcces) {
        this.dernierAcces = dernierAcces;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    public String getServiceCode() {
        return serviceCode;
    }
    
    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }
    
    public int getNiveauAutorite() {
        return niveauAutorite;
    }
    
    public void setNiveauAutorite(int niveauAutorite) {
        this.niveauAutorite = niveauAutorite;
    }
    
    public String getNomComplet() {
        return prenom + " " + nom;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(code, user.code);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", serviceCode='" + serviceCode + '\'' +
                ", niveauAutorite=" + niveauAutorite +
                ", actif=" + actif +
                '}';
    }
}