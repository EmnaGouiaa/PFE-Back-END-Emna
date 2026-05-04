package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;

@Data
public class TrelloCardDto {
    private String id;
    private String name;
    private String desc;
    private String idList;
    private String idBoard;
    private String due;
    private String url;
}