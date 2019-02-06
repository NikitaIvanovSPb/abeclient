package client;

import lombok.Data;

@Data
public class UserFTPDTO {
    Long id;
    String url;
    Integer port;
    String login;
    String pass;
}
