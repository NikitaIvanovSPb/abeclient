package client;

import lombok.Data;

@Data
public class TokenDTO {
    private String guid;
    private String attrbutes;
    private boolean used;
    private Long create;
    private Long generate;
}
