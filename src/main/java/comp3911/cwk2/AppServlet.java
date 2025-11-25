package comp3911.cwk2;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement; // PreparedStatement
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import freemarker.core.HTMLOutputFormat;
@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {

  private static final String CONNECTION_URL = "jdbc:sqlite:db.sqlite3";

  // Use parameterized queries to prevent SQL injection
  private static final String AUTH_QUERY   = "select password from user where username = ?";
  // Fix for Broken Access Control: Added subquery to ensure doctor can only see their own patients
  private static final String SEARCH_QUERY =
          "select * from patient where surname = ? AND gp_id = (select id from user where username = ?)";

  private final Configuration fm = new Configuration(Configuration.VERSION_2_3_28);
  private Connection database;

  @Override
  public void init() throws ServletException {
    configureTemplateEngine();
    connectToDatabase();
    // Fix for stored XSS: enable HTML output escaping
      fm.setOutputFormat(HTMLOutputFormat.INSTANCE);
      fm.setAutoEscapingPolicy(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
  }

  private void configureTemplateEngine() throws ServletException {
    try {
      fm.setDirectoryForTemplateLoading(new File("./templates"));
      fm.setDefaultEncoding("UTF-8");
      fm.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
      fm.setLogTemplateExceptions(false);
      fm.setWrapUncheckedExceptions(true);
    }
    catch (IOException error) {
      throw new ServletException(error.getMessage());
    }
  }

  private void connectToDatabase() throws ServletException {
    try {
      database = DriverManager.getConnection(CONNECTION_URL);
    }
    catch (SQLException error) {
      throw new ServletException(error.getMessage());
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException {
    try {
      Template template = fm.getTemplate("login.html");
      template.process(null, response.getWriter());
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
    }
    catch (TemplateException error) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException {
    // Get form parameters
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    String surname  = request.getParameter("surname");

    try {
      if (authenticated(username, password)) {
        // Get search results and merge with template
        Map<String, Object> model = new HashMap<>();
        // Fix: Pass username to searchResults to enforce access control check
        model.put("records", searchResults(surname, username));
        Template template = fm.getTemplate("details.html");
        template.process(model, response.getWriter());
      }
      else {
        Template template = fm.getTemplate("invalid.html");
        template.process(null, response.getWriter());
      }
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
    }
    catch (Exception error) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  // Use PreparedStatement to prevent SQL injection
  private boolean authenticated(String username, String password) throws SQLException {
    if (username == null || password == null) {
      return false;
    }

    try (PreparedStatement ps = database.prepareStatement(AUTH_QUERY)) { // Fix for SQL injection (auth)
      ps.setString(1, username);
      try (ResultSet results = ps.executeQuery()) {
        if (!results.next()) {
          return false;
        }
        String storedPassword = results.getString("password");
        return PasswordUtils.matches(password, storedPassword);
      }
    }
  }

  private List<Record> searchResults(String surname, String username) throws SQLException {
    List<Record> records = new ArrayList<>();

    if (surname == null) {
      surname = "";
    }

    try (PreparedStatement ps = database.prepareStatement(SEARCH_QUERY)) { // Fix for SQL injection (search)
      ps.setString(1, surname);
      ps.setString(2, username); // Fix: Bind username to the SQL query to filter by gp_id
      try (ResultSet results = ps.executeQuery()) {
        while (results.next()) {
          Record rec = new Record();
          rec.setSurname(results.getString(2));
          rec.setForename(results.getString(3));
          rec.setAddress(results.getString(4));
          rec.setDateOfBirth(results.getString(5));
          rec.setDoctorId(results.getString(6));
          rec.setDiagnosis(results.getString(7));
          records.add(rec);
        }
      }
    }

    return records;
  }
}
