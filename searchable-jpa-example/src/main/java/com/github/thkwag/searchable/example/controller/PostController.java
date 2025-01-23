package com.github.thkwag.searchable.example.controller;

import com.github.thkwag.searchable.core.condition.SearchCondition;
import com.github.thkwag.searchable.core.condition.parser.SearchableParamsParser;
import com.github.thkwag.searchable.example.dto.PostDTOs.PostListProjection;
import com.github.thkwag.searchable.example.dto.PostDTOs.PostSearchDTO;
import com.github.thkwag.searchable.example.dto.PostDTOs.PostUpdateDTO;
import com.github.thkwag.searchable.example.entity.Post;
import com.github.thkwag.searchable.example.enums.PostStatus;
import com.github.thkwag.searchable.example.service.PostSearchService;
import com.github.thkwag.searchable.openapi.annotation.SearchableParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostSearchService postSearchService;


    @Operation(summary = "Search Posts (GET)", description = "Search posts with various conditions using GET method")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<Post>> searchPostsGet(
            @RequestParam(required = false) @SearchableParams(PostSearchDTO.class) Map<String, String> params
    ) {
        SearchCondition<PostSearchDTO> condition = new SearchableParamsParser<>(PostSearchDTO.class).convert(params);
        return ResponseEntity.ok(postSearchService.findAllWithSearch(condition));

    }

    @Operation(summary = "Search Posts (POST)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/search")
    public ResponseEntity<Page<PostListProjection>> searchPosts(
            @RequestBody @Validated SearchCondition<PostSearchDTO> searchCondition
    ) {
        return ResponseEntity.ok(postSearchService.findAll(searchCondition));
    }

    @Operation(summary = "Create Post")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody @Validated Post post) {
        return ResponseEntity.ok(postSearchService.createPost(post));
    }

    @Operation(summary = "Update Post")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @RequestBody @Validated PostUpdateDTO post) {
        long updatedCount = postSearchService.updateById(id, post);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("Post not found with id: " + id);
        }

        return ResponseEntity.ok(postSearchService.findOneById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + id)));
    }

    @Operation(summary = "Delete Post")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        long deletedCount = postSearchService.deleteById(id);
        if (deletedCount == 0) {
            throw new IllegalArgumentException("Post not found with id: " + id);
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get Post by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post found"),
            @ApiResponse(responseCode = "204", description = "Post not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postSearchService.findOneById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + id)));
    }

    @Operation(summary = "Update Posts by Author")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PutMapping("/batch/author/{authorEmail}/status")
    public ResponseEntity<Long> updatePostStatusByAuthor(
            @PathVariable String authorEmail,
            @RequestParam PostStatus newStatus) {
        return ResponseEntity.ok(postSearchService.updatePostStatusByAuthor(authorEmail, newStatus));
    }

    @Operation(summary = "Update Posts View Count in Date Range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PutMapping("/batch/viewcount")
    public ResponseEntity<Long> updateViewCountInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam Long viewCount) {
        return ResponseEntity.ok(postSearchService.updateViewCountInDateRange(startDate, endDate, viewCount));
    }

    @Operation(summary = "Delete Posts by Status and Before Date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @DeleteMapping("/batch/status/{status}")
    public ResponseEntity<Long> deletePostsByStatusAndBeforeDate(
            @PathVariable PostStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeDate) {
        return ResponseEntity.ok(postSearchService.deletePostsByStatusAndBeforeDate(beforeDate));
    }

    @Operation(summary = "Delete Posts by Author and View Count")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @DeleteMapping("/batch/author/{authorEmail}/viewcount/{maxViewCount}")
    public ResponseEntity<Long> deletePostsByAuthorAndViewCount(
            @PathVariable String authorEmail,
            @PathVariable Long maxViewCount) {
        return ResponseEntity.ok(postSearchService.deletePostsByAuthorAndViewCount(authorEmail, maxViewCount));
    }

    @Operation(summary = "Delete Posts by Search Condition")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @DeleteMapping("/batch")
    public ResponseEntity<Long> deletePosts(
            @RequestBody @Validated SearchCondition<PostSearchDTO> searchCondition) {
        return ResponseEntity.ok(postSearchService.deletePosts(searchCondition));
    }
}