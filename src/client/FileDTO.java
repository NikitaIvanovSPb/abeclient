package client;

import lombok.Data;

@Data
public class FileDTO {
    private String guid;
    private String name;
    private String attributes;
    private UserFTPDTO ftp;
    private Long create;
    private String aesKeyBase64;
}
