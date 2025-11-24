package comp3911.cwk2;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class AppServer {
  public static void main(String[] args) throws Exception {
    Log.setLog(new StdErrLog());

    ServletHandler handler = new ServletHandler();
    handler.addServletWithMapping(AppServlet.class, "/*");

    // FIX START: Lack of Transport Encryption 
    Server server = new Server();

    //Configure HTTP settings for HTTPS
    HttpConfiguration httpsConfig = new HttpConfiguration();
    httpsConfig.setSecureScheme("https");
    httpsConfig.setSecurePort(8443);
    httpsConfig.setOutputBufferSize(32768);
    httpsConfig.addCustomizer(new SecureRequestCustomizer()); 

    // Configure the SSL context factory (load the keystore)
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setKeyStorePath("config/keystore.p12"); 
    sslContextFactory.setKeyStoreType("PKCS12");
    sslContextFactory.setKeyStorePassword("changeit"); 
    sslContextFactory.setKeyManagerPassword("changeit");

    // Create an HTTPS connector 
    ServerConnector sslConnector = new ServerConnector(server,
        new SslConnectionFactory(sslContextFactory, "http/1.1"),
        new HttpConnectionFactory(httpsConfig));
    sslConnector.setPort(8443);
    
    server.addConnector(sslConnector);
    // FIX END 

    server.setHandler(handler);

    server.start();
    System.out.println("Server started on https://localhost:8443"); 
    server.join();
  }
}
