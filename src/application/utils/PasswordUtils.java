package application.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Utilitaires pour la gestion sécurisée des mots de passe
 */
public class PasswordUtils {
    
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // Critères de validation des mots de passe
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    
    /**
     * Hache un mot de passe avec un sel généré aléatoirement
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        
        try {
            // Génération d'un sel aléatoire
            byte[] salt = generateSalt();
            
            // Hachage du mot de passe avec le sel
            byte[] hashedPassword = hashPasswordWithSalt(password, salt);
            
            // Combinaison du sel et du hash pour le stockage
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
            
            // Encodage en Base64 pour le stockage
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme de hachage non disponible", e);
        }
    }
    
    /**
     * Vérifie si un mot de passe correspond au hash stocké
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        
        try {
            // Décodage du hash stocké
            byte[] combined = Base64.getDecoder().decode(storedHash);
            
            if (combined.length < SALT_LENGTH) {
                return false;
            }
            
            // Extraction du sel
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            
            // Extraction du hash
            byte[] storedPasswordHash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, SALT_LENGTH, storedPasswordHash, 0, storedPasswordHash.length);
            
            // Hachage du mot de passe fourni avec le même sel
            byte[] computedHash = hashPasswordWithSalt(password, salt);
            
            // Comparaison sécurisée des hashs
            return MessageDigest.isEqual(storedPasswordHash, computedHash);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du mot de passe: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Valide qu'un mot de passe respecte les critères de sécurité
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        
        // Vérification de la longueur
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        
        // Vérification des critères de complexité
        return UPPERCASE_PATTERN.matcher(password).matches() &&
               LOWERCASE_PATTERN.matcher(password).matches() &&
               DIGIT_PATTERN.matcher(password).matches() &&
               SPECIAL_CHAR_PATTERN.matcher(password).matches();
    }
    
    /**
     * Retourne les critères de validation pour affichage à l'utilisateur
     */
    public static String getPasswordCriteria() {
        return "Le mot de passe doit contenir :\n" +
               "- Au moins " + MIN_LENGTH + " caractères\n" +
               "- Au moins une lettre majuscule\n" +
               "- Au moins une lettre minuscule\n" +
               "- Au moins un chiffre\n" +
               "- Au moins un caractère spécial (!@#$%^&*...)";
    }
    
    /**
     * Évalue la force d'un mot de passe (0-4)
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // Longueur
        if (password.length() >= MIN_LENGTH) score++;
        if (password.length() >= 12) score++;
        
        // Complexité
        if (UPPERCASE_PATTERN.matcher(password).matches()) score++;
        if (LOWERCASE_PATTERN.matcher(password).matches()) score++;
        if (DIGIT_PATTERN.matcher(password).matches()) score++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) score++;
        
        // Variété de caractères
        if (password.length() >= 16 && score >= 4) score++;
        
        return Math.min(score, 4); // Maximum 4
    }
    
    /**
     * Retourne la description de la force du mot de passe
     */
    public static String getPasswordStrengthDescription(int strength) {
        switch (strength) {
            case 0:
            case 1:
                return "Très faible";
            case 2:
                return "Faible";
            case 3:
                return "Moyen";
            case 4:
                return "Fort";
            default:
                return "Inconnu";
        }
    }
    
    /**
     * Génère un mot de passe sécurisé
     */
    public static String generateSecurePassword(int length) {
        if (length < MIN_LENGTH) {
            length = MIN_LENGTH;
        }
        
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        
        StringBuilder password = new StringBuilder();
        
        // Assurer au moins un caractère de chaque type
        password.append(uppercase.charAt(secureRandom.nextInt(uppercase.length())));
        password.append(lowercase.charAt(secureRandom.nextInt(lowercase.length())));
        password.append(digits.charAt(secureRandom.nextInt(digits.length())));
        password.append(specialChars.charAt(secureRandom.nextInt(specialChars.length())));
        
        // Remplir le reste avec des caractères aléatoires
        String allChars = uppercase + lowercase + digits + specialChars;
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(secureRandom.nextInt(allChars.length())));
        }
        
        // Mélanger les caractères
        return shuffleString(password.toString());
    }
    
    /**
     * Génère un sel aléatoire
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }
    
    /**
     * Hache un mot de passe avec un sel spécifique
     */
    private static byte[] hashPasswordWithSalt(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        
        // Ajout du sel au digest
        digest.update(salt);
        
        // Hachage du mot de passe
        return digest.digest(password.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Mélange les caractères d'une chaîne
     */
    private static String shuffleString(String input) {
        char[] chars = input.toCharArray();
        
        for (int i = chars.length - 1; i > 0; i--) {
            int index = secureRandom.nextInt(i + 1);
            char temp = chars[index];
            chars[index] = chars[i];
            chars[i] = temp;
        }
        
        return new String(chars);
    }
    
    /**
     * Nettoie les données sensibles en mémoire
     */
    public static void clearSensitiveData(char[] data) {
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                data[i] = '\0';
            }
        }
    }
    
    /**
     * Vérifie si un mot de passe est dans une liste de mots de passe couramment utilisés
     */
    public static boolean isCommonPassword(String password) {
        if (password == null) {
            return false;
        }
        
        // Liste des mots de passe les plus couramment utilisés
        String[] commonPasswords = {
            "password", "123456", "123456789", "12345678", "12345", "1234567",
            "password123", "admin", "qwerty", "abc123", "letmein", "welcome",
            "monkey", "1234567890", "dragon", "123123", "football", "iloveyou",
            "master", "sunshine", "princess", "azerty", "trustno1", "000000"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common)) {
                return true;
            }
        }
        
        return false;
    }
}