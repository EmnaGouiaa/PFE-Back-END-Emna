package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.configuration.TrelloProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrelloService {

    private final TrelloProperties trelloProperties;
    private final RestTemplate restTemplate;
    @PostConstruct
    public void debugTrello() {
        System.out.println("KEY=" + trelloProperties.getKey());
        System.out.println("TOKEN=" + trelloProperties.getToken());
    }
    public Map<String, Object> createBoard(String boardName) {
        String encodedName = URLEncoder.encode(boardName, StandardCharsets.UTF_8);
        String url = "https://api.trello.com/1/boards/?name=" + encodedName
                + "&defaultLists=false"
                + "&key=" + trelloProperties.getKey()
                + "&token=" + trelloProperties.getToken();

        return restTemplate.postForObject(url, null, Map.class);
    }

    public Map<String, Object> createList(String boardId, String listName) {
        String encodedName = URLEncoder.encode(listName, StandardCharsets.UTF_8);
        String url = "https://api.trello.com/1/boards/" + boardId + "/lists"
                + "?name=" + encodedName
                + "&key=" + trelloProperties.getKey()
                + "&token=" + trelloProperties.getToken();

        return restTemplate.postForObject(url, null, Map.class);
    }
    public List<Map<String, Object>> getCardsByBoard(String boardId) {
        String url = "https://api.trello.com/1/boards/" + boardId + "/cards"
                + "?key=" + trelloProperties.getKey()
                + "&token=" + trelloProperties.getToken();

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    public List<Map<String, Object>> getListsByBoard(String boardId) {
        String url = "https://api.trello.com/1/boards/" + boardId + "/lists"
                + "?key=" + trelloProperties.getKey()
                + "&token=" + trelloProperties.getToken();

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }
}