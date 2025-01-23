package com.github.thkwag.searchable.example.service;

import com.github.thkwag.searchable.core.condition.SearchCondition;
import com.github.thkwag.searchable.core.condition.SearchConditionBuilder;
import com.github.thkwag.searchable.core.service.DefaultSearchableService;
import com.github.thkwag.searchable.example.dto.PostDTOs.PostListProjection;
import com.github.thkwag.searchable.example.dto.PostDTOs.PostSearchDTO;
import com.github.thkwag.searchable.example.dto.PostDTOs.PostUpdateDTO;
import com.github.thkwag.searchable.example.entity.Post;
import com.github.thkwag.searchable.example.enums.PostStatus;
import com.github.thkwag.searchable.example.repository.CommentRepository;
import com.github.thkwag.searchable.example.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PostSearchService extends DefaultSearchableService<Post, Long> {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public PostSearchService(PostRepository postRepository,
                             CommentRepository commentRepository,
                             EntityManager entityManager) {
        super(postRepository, entityManager);
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    public long updateById(Long id, PostUpdateDTO post) {
        SearchCondition<PostSearchDTO> condition = SearchConditionBuilder.create(PostSearchDTO.class)
                .where(group -> group
                        .equals("id", id)
                ).build();


        return updateWithSearch(condition, post);
    }

    public Optional<Post> findOneById(Long id) {
        SearchCondition<PostSearchDTO> condition = SearchConditionBuilder.create(PostSearchDTO.class)
                .where(group -> group
                        .equals("id", id)
                ).build();
        return findOneWithSearch(condition);
    }

    public Page<PostListProjection> findAll(SearchCondition<PostSearchDTO> condition) {
        return findAllWithSearch(condition, PostListProjection.class);
    }

    @Transactional
    public long deleteById(Long id) {
        commentRepository.deleteAllByPostId(id);

        SearchCondition<PostSearchDTO> condition = SearchConditionBuilder.create(PostSearchDTO.class)
                .where(group -> group
                        .equals("id", id)
                ).build();
        return deleteWithSearch(condition);
    }

    public long updatePostStatusByAuthor(String authorEmail, PostStatus newStatus) {
        SearchCondition<PostSearchDTO> condition = SearchConditionBuilder.create(PostSearchDTO.class)
                .where(group -> group
                        .equals("authorEmail", authorEmail)
                ).build();

        Post updateData = new Post();
        updateData.setStatus(newStatus);

        return updateWithSearch(condition, updateData);
    }

    public long updateViewCountInDateRange(LocalDateTime startDate, LocalDateTime endDate, Long viewCount) {
        SearchCondition<PostSearchDTO> condition = SearchConditionBuilder.create(PostSearchDTO.class)
                .where(group -> group
                        .between("createdAt", startDate, endDate)
                ).build();

        Post updateData = new Post();
        updateData.setViewCount(viewCount);

        return updateWithSearch(condition, updateData);
    }

    public long deletePostsByStatusAndBeforeDate(LocalDateTime beforeDate) {
        SearchCondition<PostSearchDTO> condition = SearchConditionBuilder.create(PostSearchDTO.class)
                .where(group -> group
                        .lessThan("createdAt", beforeDate)
                ).build();
        return deleteWithSearch(condition);
    }

    public long deletePostsByAuthorAndViewCount(String authorEmail, Long maxViewCount) {
        SearchCondition<PostSearchDTO> condition = SearchConditionBuilder.create(PostSearchDTO.class)
                .where(group -> group
                        .equals("authorEmail", authorEmail)
                        .lessThanOrEqualTo("viewCount", maxViewCount)
                ).build();
        return deleteWithSearch(condition);
    }

    public long deletePosts(SearchCondition<PostSearchDTO> searchCondition) {
        return deleteWithSearch(searchCondition);
    }

    public long updatePosts(SearchCondition<PostSearchDTO> condition, PostUpdateDTO updateDTO) {
        return updateWithSearch(condition, updateDTO);
    }

} 