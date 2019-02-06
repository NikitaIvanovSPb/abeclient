package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.http.client.CookieStore;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

public class Main extends Application {
    static CookieStore httpCookieStore = new BasicCookieStore();
    static Stage mainStage;
    static String adress;
    static HttpClientBuilder builder;

    @Override
    public void start(Stage mainStage) throws Exception{
        Main.mainStage = mainStage;
        initBuildaer();
        Parent root = FXMLLoader.load(getClass().getResource("LoginController.fxml"));
        mainStage.setTitle("ABE Client");
        mainStage.setScene(new Scene(root));
        mainStage.show();
    }

    static void initBuildaer(){
        final SSLConnectionSocketFactory sslsf;
        try {
            sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory())
                .register("https", sslsf)
                .build();

        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(200);
        builder = HttpClientBuilder.create().setDefaultCookieStore(Main.httpCookieStore)
                .setSSLSocketFactory(sslsf)
                .setConnectionManagerShared(true)
                .setConnectionManager(cm);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
