package client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import ru.nikita.openabeapi.OpenABEApi;
import ru.nikita.openabeapi.Schema;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import javax.sound.midi.Patch;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static client.Main.mainStage;

public class MainController implements Initializable {
    Gson gson = new Gson();
    @FXML
    public MenuBar menuBar;
    @FXML
    public GridPane grid;
    @FXML
    public TextField fileIdField;
    @FXML
    public Button tokenField;
    @FXML
    public ListView<String> listView;
    private HttpClientBuilder builder;

    public MainController() {
        builder = Main.builder;
    }


    public void getTokens(){
        try (CloseableHttpClient httpclient = builder.build()) {
            HttpGet httpPost = new HttpGet(Main.adress +"/user/tokens");
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String body = httpclient.execute(httpPost, responseHandler);
            List<String> tokens = Arrays.stream(gson.fromJson(body, TokenDTO[].class))
                    .map(TokenDTO::getGuid)
                    .collect(Collectors.toList());
            ObservableList<String> items = FXCollections.observableArrayList(tokens);
            listView.setItems(items);
//            listView.refresh();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getFileById(){
        String fileId = fileIdField.getText();
        if(fileId == null || fileId.isEmpty() || !Util.isUUID(fileId)){
            Util.showErrorAlert("Введите корректный id файла");
            return;
        }
        try (CloseableHttpClient httpclient = builder.build()) {
            HttpPost httpPost = new HttpPost(Main.adress +"/user/file");
            List <NameValuePair> nvps = new ArrayList <>();
            nvps.add(new BasicNameValuePair("file_guid", fileId));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            ResponseHandler<String> responseHandler = response -> {
                if(response.getStatusLine().getStatusCode() == 200){
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(response.getEntity().getContent(), writer, "UTF-8");
                    Gson gson = new Gson();
                    FileDTO fileDTO = gson.fromJson(writer.toString(), FileDTO.class);
                    if(fileDTO == null) {Util.showErrorAlert("ошибка"); return null;}
                    File encrFile = downloadFile(fileDTO);
                    if (encrFile == null) { Util.showErrorAlert("Файл не был скачен(Внутренняя ошибка сервера)!"); return null;}
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Выберете ключ");
                    File keyFile = fileChooser.showOpenDialog(Main.mainStage);
                    if (keyFile != null) {
                        try {
                            OpenABEApi openABEApi = new OpenABEApi();
                            String dekodingAesKey = openABEApi.decoding(Schema.CP, "working", keyFile, fileDTO.getAesKeyBase64());
                            if(dekodingAesKey == null) {Util.showErrorAlert("Произошла ошибка при дешифрации ключа"); return null;}

                            byte[] decode = decodeAES(Files.readAllBytes(Paths.get(encrFile.getAbsolutePath())), dekodingAesKey);
                            if(decode == null) {Util.showErrorAlert("Произошла ошибка при дешифрации файла"); return null; }
                            fileChooser = new FileChooser();
                            fileChooser.setTitle("Выберете файл для сохранения");
                            File decodinFile = fileChooser.showSaveDialog(Main.mainStage);
                            if(decodinFile == null) return null;

                            Files.write(Paths.get(decodinFile.getAbsolutePath()), decode);
                            Util.showSuccessAlert("Файл был успешно расшифрован и сохранен");
                        } catch (IOException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                    Files.deleteIfExists(Paths.get(encrFile.getAbsolutePath()));
                }else if(response.getStatusLine().getStatusCode() == 500){
                    Util.showErrorAlert("Данный файл не найден на сервере");
                }
                return null;
            };
            httpclient.execute(httpPost, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }





    }

    private File downloadFile(FileDTO fileDTO){
        UserFTPDTO ftp = fileDTO.getFtp();
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftp.getUrl(), ftp.getPort());
            ftpClient.login(ftp.getLogin(), ftp.getPass());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            String remoteFile1 = "/" + fileDTO.getGuid();
            if(ftpClient.listFiles(remoteFile1).length == 0){
                Util.showErrorAlert("Файл не найден!");
            }else {
                File downloadFile1 = new File("/home/nikita/test/" + fileDTO.getGuid());
                OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1));
                boolean success = ftpClient.retrieveFile(remoteFile1, outputStream1);
                outputStream1.close();
                if (success) {
                    return downloadFile1;
                }else{
                    return null;
                }
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getTokens();
    }

    public void generateSelected(){
        String selectedItem = listView.getSelectionModel().getSelectedItem();
        try (CloseableHttpClient httpclient = builder.build()) {
            HttpPost httpPost = new HttpPost( Main.adress +"/user/token");
            List <NameValuePair> nvps = new ArrayList <>();
            nvps.add(new BasicNameValuePair("token_guid", selectedItem));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            //TODO for debug
//            HttpHost proxy = new HttpHost("127.0.0.1", 9999, "http");
//            RequestConfig config = RequestConfig.custom()
//                    .setProxy(proxy)
//                    .build();
//            httpPost.setConfig(config);

            ResponseHandler<String> responseHandler = response -> {
                if(response.getStatusLine().getStatusCode() == 200){
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(response.getEntity().getContent(), writer, "UTF-8");
                    switch (writer.toString()){
                        case "used":
                            Util.showErrorAlert("Данный токен уже был использован");
                            break;
                        case "error":
                            Util.showErrorAlert("Произошла ошибка");
                        default:
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle("Сохранить ключ");
                            File file = fileChooser.showSaveDialog(Main.mainStage);
                            if (file != null) {
                                try {
                                    Files.write(Paths.get(file.getAbsolutePath()),  writer.toString().getBytes());
                                } catch (IOException ex) {
                                    System.out.println(ex.getMessage());
                                }
                            }
                            Util.showSuccessAlert("Клучь добавлен");
                    }
                }
                getTokens();
                return null;
            };
            httpclient.execute(httpPost, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public byte[] decodeAES(byte[] toDecode, String base64Key){
        try {
            byte[] iv = new byte[16];
            byte[] key = new byte[32];
            if(getIvAndKeyFromBase64String(iv, key, base64Key)) {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
                return cipher.doFinal(toDecode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean getIvAndKeyFromBase64String(byte[] iv, byte[] key, String base64){
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            for (int i = 0; i < 16; i++) {
                iv[i] = data[i];
            }
            for (int i = 0; i < 32; i++) {
                key[i] = data[i + 16];
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
