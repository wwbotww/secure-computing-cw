package comp3911.cwk2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PasswordMigration {
  private static final String CONNECTION_URL = "jdbc:sqlite:db.sqlite3";

  public static void main(String[] args) throws SQLException {
    try (Connection connection = DriverManager.getConnection(CONNECTION_URL)) {
      connection.setAutoCommit(false);
      try {
        int migrated = migratePasswords(connection);
        connection.commit();
        System.out.printf("Password migration complete. %d password(s) updated.%n", migrated);
      }
      catch (Exception error) {
        connection.rollback();
        throw new SQLException("Password migration failed", error);
      }
    }
  }

  private static int migratePasswords(Connection connection) throws SQLException {
    List<UserPassword> toUpdate = loadPlaintextPasswords(connection);
    if (toUpdate.isEmpty()) {
      System.out.println("No plaintext passwords detected. No updates performed.");
      return 0;
    }

    try (PreparedStatement update =
      connection.prepareStatement("update user set password = ? where username = ?")) {
      for (UserPassword row : toUpdate) {
        String hashed = PasswordUtils.hashPassword(row.password);
        update.setString(1, hashed);
        update.setString(2, row.username);
        update.addBatch();
      }
      update.executeBatch();
    }

    return toUpdate.size();
  }

  private static List<UserPassword> loadPlaintextPasswords(Connection connection)
   throws SQLException {
    List<UserPassword> rows = new ArrayList<>();
    try (Statement select = connection.createStatement();
      ResultSet results = select.executeQuery("select username, password from user")) {
      while (results.next()) {
        String username = results.getString("username");
        String password = results.getString("password");
        if (PasswordUtils.requiresMigration(password)) {
          rows.add(new UserPassword(username, password));
        }
      }
    }
    return rows;
  }

  private static final class UserPassword {
    private final String username;
    private final String password;

    private UserPassword(String username, String password) {
      this.username = username;
      this.password = password;
    }
  }
}

