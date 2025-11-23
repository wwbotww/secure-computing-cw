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

    // --- FIX START: Lack of Transport Encryption ---
    // 原代码: Server server = new Server(8080);
    // 修复说明: 配置服务器使用 SSL/TLS (HTTPS) 以加密传输中的数据，防止嗅探。
    
    Server server = new Server();

    // 1. 配置 HTTP 设置 (用于 HTTPS)
    HttpConfiguration httpsConfig = new HttpConfiguration();
    httpsConfig.setSecureScheme("https");
    httpsConfig.setSecurePort(8443);
    httpsConfig.setOutputBufferSize(32768);
    httpsConfig.addCustomizer(new SecureRequestCustomizer()); // 添加 SSL 用于解析

    // 2. 配置 SSL 上下文工厂 (加载 Keystore)
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    // 注意: 这里的路径必须指向您生成的 keystore 文件
    sslContextFactory.setKeyStorePath("config/keystore.p12"); 
    sslContextFactory.setKeyStoreType("PKCS12");
    sslContextFactory.setKeyStorePassword("changeit"); // 对应您生成时设置的密码
    sslContextFactory.setKeyManagerPassword("changeit");

    // 3. 创建 HTTPS 连接器 (监听 8443 端口)
    ServerConnector sslConnector = new ServerConnector(server,
        new SslConnectionFactory(sslContextFactory, "http/1.1"),
        new HttpConnectionFactory(httpsConfig));
    sslConnector.setPort(8443);
    
    server.addConnector(sslConnector);
    // --- FIX END ---

    server.setHandler(handler);

    server.start();
    System.out.println("Server started on https://localhost:8443"); // 提示服务器已启动
    server.join();
  }
}