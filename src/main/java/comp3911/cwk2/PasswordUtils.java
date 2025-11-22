package comp3911.cwk2;

import org.mindrot.jbcrypt.BCrypt;

final class PasswordUtils {
  private static final int BCRYPT_COST = 12;

  private PasswordUtils() { }

  static boolean isBcryptHash(String value) {
    if (value == null) {
      return false;
    }
    boolean hasPrefix =
      value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    return hasPrefix && value.length() == 60;
  }

  static boolean requiresMigration(String storedValue) {
    return storedValue != null && !isBcryptHash(storedValue);
  }

  static String hashPassword(String plainText) {
    if (plainText == null) {
      throw new IllegalArgumentException("Plain-text password must not be null.");
    }
    return BCrypt.hashpw(plainText, BCrypt.gensalt(BCRYPT_COST));
  }

  static boolean matches(String suppliedPlainText, String storedValue) {
    if (suppliedPlainText == null || storedValue == null) {
      return false;
    }
    if (isBcryptHash(storedValue)) {
      try {
        return BCrypt.checkpw(suppliedPlainText, storedValue);
      }
      catch (IllegalArgumentException error) {
        return false;
      }
    }
    return storedValue.equals(suppliedPlainText);
  }
}

