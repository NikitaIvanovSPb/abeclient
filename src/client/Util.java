package client;

import javafx.scene.control.Alert;

import java.util.regex.Pattern;

public class Util {
    public static void showErrorAlert(String text){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle("Ошибка");
        alert.setContentText(text);
        alert.show();
    }

    public static void showSuccessAlert(String text){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setTitle("Успех!");
        alert.setContentText(text);
        alert.show();
    }

    public static boolean isUUID(String text){
        Pattern pattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        return pattern.matcher(text).matches();
    }

}
