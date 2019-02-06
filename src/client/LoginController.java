package client;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static client.Main.mainStage;


public class LoginController {
    public TextField adressField;
    private String loginCoockie;
    @FXML
    Button loginButton;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;

    public void login() {
        HttpClientBuilder builder =  Main.builder;

        try (CloseableHttpClient httpclient = builder.build()) {
            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("username", loginField.getText()));
            form.add(new BasicNameValuePair("password", passField.getText()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
            HttpPost httpPost = new HttpPost(adressField.getText()+"/login");
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setEntity(entity);
            //TODO for debug
//            HttpHost proxy = new HttpHost("127.0.0.1", 9999, "http");
//            RequestConfig config = RequestConfig.custom()
//                    .setProxy(proxy)
//                    .build();
//            httpPost.setConfig(config);

            ResponseHandler<String> responseHandler = response -> {
                if(response.getHeaders("Location")[0].getValue().contains("login?error") || response.getStatusLine().getStatusCode() != 302){
                    Util.showErrorAlert("Неверный логин или пароль!");
                }else{
                    Main.adress = adressField.getText();
                    mainStage.close();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("MainController.fxml"));
                    Parent root=loader.load();
                    mainStage.setScene(new Scene(root));
                    mainStage.show();
                }
                return null;
            };
            httpclient.execute(httpPost, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
