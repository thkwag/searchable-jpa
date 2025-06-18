package dev.simplecore.searchable.example.controller;

import dev.simplecore.searchable.core.condition.SearchCondition;
import dev.simplecore.searchable.core.condition.parser.SearchableParamsParser;
import dev.simplecore.searchable.example.dto.UserDTO;
import dev.simplecore.searchable.example.entity.User;
import dev.simplecore.searchable.example.service.UserService;
import dev.simplecore.searchable.openapi.annotation.SearchableParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "API endpoints for testing searchable annotations")
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users using searchable annotations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved users",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam(required = false) @SearchableParams(value = UserDTO.class) Map<String, String> params) {

        SearchCondition<UserDTO> condition = new SearchableParamsParser<UserDTO>(UserDTO.class).convert(params);
        return ResponseEntity.ok(userService.findAllWithSearch(condition));
    }
} 