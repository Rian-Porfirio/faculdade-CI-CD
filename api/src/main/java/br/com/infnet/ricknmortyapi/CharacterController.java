package br.com.infnet.ricknmortyapi;

import br.com.infnet.ricknmortyapi.model.Personagem;
import br.com.infnet.ricknmortyapi.repository.PersonagemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/characters")
@RestController
@Slf4j
@RequiredArgsConstructor
public class CharacterController {
    private final PersonagemRepository repository;
    // Buscar todos os personagens
    @GetMapping
    public ResponseEntity<?> getAllCharacters(@RequestHeader(value = "page", defaultValue = "0") String page,
                                              @RequestHeader(value = "size", defaultValue = "10") String size) {
        log.info("page: " + page + " size: " + size);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(repository.count()));
        List<Personagem> allCharacters = repository.findAll();
        return new ResponseEntity<>(allCharacters, headers, HttpStatus.OK);
    }
}
