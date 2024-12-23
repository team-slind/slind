package com.team2.slind.article.service;

import com.team2.slind.article.dto.mapper.ArticleDetailMapperDTO;
import com.team2.slind.article.dto.response.*;
import com.team2.slind.common.dto.request.BoardPkCreateUpdateRequest;
import com.team2.slind.article.dto.request.ArticleReactionRequest;
import com.team2.slind.common.dto.request.ArticlePkCreateUpdateRequest;
import com.team2.slind.article.mapper.ArticleMapper;
import com.team2.slind.article.mapper.ArticleReactionMapper;
import com.team2.slind.article.vo.Article;
import com.team2.slind.article.vo.ArticleReaction;
import com.team2.slind.board.mapper.BoardMapper;
import com.team2.slind.board.vo.Board;
import com.team2.slind.common.exception.*;
import com.team2.slind.judgement.mapper.JudgementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    private final ArticleMapper articleMapper;
    private final BoardMapper boardMapper;
    private final ArticleReactionMapper articleReactionMapper;
    private final JudgementMapper judgementMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private Logger logger = LoggerFactory.getLogger(ArticleService.class);
    private static final int PAGE_LIST_SIZE = 10;
    private static final int ARTICLE_LIST_SIZE = 12;


    @Transactional
    public ResponseEntity<ArticlePkResponse> createArticle(BoardPkCreateUpdateRequest boardPkCreateUpdateRequest, Long memberPk) {
        Long boardPk = boardPkCreateUpdateRequest.getBoardPk();
        boardMapper.findByBoardPk(boardPk).orElseThrow(()-> new BoardNotFoundException(BoardNotFoundException.BOARD_NOT_FOUND));
        String title = boardPkCreateUpdateRequest.getTitle();
        if (title == null || title.trim().isEmpty() ){
            throw new ContentException(ContentException.EMPTY_TITLE);
        }

        String articleContent = boardPkCreateUpdateRequest.getArticleContent();
        if (articleContent == null || articleContent.trim().isEmpty() ){
            throw new ContentException(ContentException.EMPTY_CONTENT);
        }

        Article article = Article.builder().title(title).
                articleContent(articleContent).memberPk(memberPk)
                .articleBoard(Board.builder().boardPk(boardPk).build())
                .build();

        articleMapper.saveArticle(article);
        Long articlePk = articleMapper.findCreatedArticlePk();
        return ResponseEntity.ok().body(new ArticlePkResponse(articlePk));
    }

    /**
     * @return
     *   메인 화면 최근 게시글에 띄울
     *   전체 게시판 중 가장 최근 게시글 10개 리스트 반환
     */
    public ResponseEntity<List<ArticleMainResponse>> findMainArticles() {
        List<Article> articleList = articleMapper.findRecentArticles();

        List<ArticleMainResponse> resultList = articleList.stream().map(article ->
            ArticleMainResponse.builder().articlePk(article.getArticlePk())
                    .boardPk(article.getArticleBoard().getBoardPk())
                    .boardTitle(article.getArticleBoard().getTitle())
                    .articleTitle(article.getTitle())
                    .viewCount(article.getViewCount())
                    .likeCount(article.getLikeCount())
                    .dislikeCount(article.getDislikeCount())
                    .commentCount(article.getComments().size())
                    .build()
        ).toList();
        return ResponseEntity.ok().body(resultList);
    }


    @Transactional
    public ResponseEntity<ArticlePkResponse> updateArticle
            (ArticlePkCreateUpdateRequest articlePkCreateUpdateRequest, Long memberPk) {
        Long articlePk = articlePkCreateUpdateRequest.getArticlePk();
        Article article = articleMapper.findByPk(articlePk).orElseThrow(()->
                new ArticleNotFoundException(ArticleNotFoundException.ARTICLE_NOT_FOUND)
        );

        if (!article.getMemberPk().equals(memberPk)) {
            throw new UnauthorizedException(UnauthorizedException.UNAUTHORIZED_UPDATE_ARTICLE);
        }
        article.update(articlePkCreateUpdateRequest.getTitle(), articlePkCreateUpdateRequest.getArticleContent());
        articleMapper.updateArticle(article);
        return ResponseEntity.ok().body(new ArticlePkResponse(articlePk));
    }
    @Transactional
    public ResponseEntity<Void> deleteArticle(Long articlePk, Long memberPk) {
        Long articleMemberPk = articleMapper.findMemberByPk(articlePk).orElseThrow(()->
        new ArticleNotFoundException(ArticleNotFoundException.ARTICLE_NOT_FOUND));

        if (!articleMemberPk.equals(memberPk)) {
            throw new UnauthorizedException(UnauthorizedException.UNAUTHORIZED_DELETE_ARTICLE);
        }
        Long result = articleMapper.deleteArticle(articlePk);
        if (result==0L){
            throw new AlreadyDeletedException(AlreadyDeletedException.ALREADY_DELETED_ARTICLE);
        }

        //레디스에서 이 articlePk를 Key로 가진 랭킹 지우기
        String key = "ranking:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Long removedCount = redisTemplate.opsForZSet().remove(key, String.valueOf(articlePk));
        logger.info("Redis에서 Article Key 삭제 완료. Key:{}, RemovedCount:{}", key, removedCount);
        return ResponseEntity.ok().build();

    }


    public ResponseEntity<Void> createReaction(
            ArticleReactionRequest articleReactionRequest, Long memberPk) {
        Long articlePk = articleReactionRequest.getArticlePk();
        logger.info("=========createReaction=============");
        logger.info("articlePk:{}", articlePk);
        if (articleMapper.findCountByPk(articlePk) == 0) {
            throw new ArticleNotFoundException(ArticleNotFoundException.ARTICLE_NOT_FOUND);
        }
        Boolean requestIsLike = articleReactionRequest.getIsLike();
        Boolean isUp = articleReactionRequest.getIsUp();
        //memberPk의 게시글 반응이 있다면 가져오기
        Optional<ArticleReaction> reactionOpt = articleReactionMapper.findByArticlePkAndMemberPk(articlePk, memberPk);
        //up일 때
        if (isUp) {
            logger.info("=====================반응 +1 API =======================");
            if (reactionOpt.isPresent()) {
                ArticleReaction reaction = reactionOpt.get();
                // 가져온 반응 데이터가 null이 아니면서 requestIsLike와 isLike가 같다면
                if (reaction.getIsLike().equals(requestIsLike)) {
                    // 이미 좋아한/싫어한 게시글입니다 Exception 반환
                    throw new AlreadyReactedException(AlreadyReactedException.ALREADY_REACTED_ARTICLE);
                } else {
                    logger.info("기존 반응이 존재합니다. 다른 반응으로 교체합니다.");
                    // 기존 반응이 있고, requestIsLike와 isLike가 다르면 반응 바꾸기
                    articleReactionMapper.updateReaction(requestIsLike, memberPk, articlePk);

                    logger.info("Finished Reaction Table Update");
                    // 게시글 count 변경 :  기존 반응 count -1
                    changeArticleReactionCount(reaction.getIsLike(), false, articlePk);
                    logger.info("Finished Article Table Count Value Update");
                }
            } else {
                // null이면 새로운 반응 save
                logger.info("기존 반응이 없으므로 INSERT 로직을 수행합니다.");
                logger.info("articlePk:{}", articlePk);
                logger.info("memberPk:{}", memberPk);
                logger.info("requestIsLike:{}", requestIsLike);
                ArticleReaction newReaction = ArticleReaction.builder()
                        .articlePk(articlePk)
                        .memberPk(memberPk)
                        .isLike(requestIsLike)
                        .build();
                articleReactionMapper.saveReaction(newReaction);
                logger.info("FINISHED INSERT NEW REACTION");
            }
            //새로운 반응 +1
            changeArticleReactionCount(requestIsLike, true, articlePk);
            logger.info("FINISHED UPDATE TABLE REACTION VALUE");


        }else{
            logger.info("=====================반응 -1 API =======================");

            //down 일때
            //null이면 nullException 반환 (취소할 반응이 없었던 것)
            ArticleReaction reaction = reactionOpt.orElseThrow(()->
                    new NoReactionExcetpion(NoReactionExcetpion.NO_REACTION_FOR_DOWN_IN_ARTICLE));
            //requestIsLike와 isLike가 같지 않다면 Exception 반환
            if (requestIsLike != reaction.getIsLike()){
                throw new NoReactionExcetpion(NoReactionExcetpion.NO_REACTION_FOR_DOWN_IN_ARTICLE);
            }
            articleReactionMapper.deleteReactionByArticlePk(reaction.getArticleReactionPk());
            logger.info("기존 반응 삭제 완료");
            changeArticleReactionCount(requestIsLike, false, articlePk);
            logger.info("기존 값 -1 완료");
        }
        return ResponseEntity.ok().build();
    }
    @Transactional
    public void changeArticleReactionCount(Boolean isLike, Boolean isUp, Long articlePk){
        Integer upCount = isUp ? 1 : -1;
        Integer result = null;
        logger.info("upCount:{}", upCount);
        logger.info("articlePk:{}", articlePk);

        //인기글 점수 산정을 위해 점수 더하기 or 빼기
        String key = "ranking:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(articlePk), upCount);
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
        if(isLike){
            result = articleMapper.updateLikeCount(upCount, articlePk);
        }else{
            result = articleMapper.updateDislikeCount(upCount, articlePk);
        }
        if (result==0){
            throw new FailedReactionException(FailedReactionException.FAILED_REACTION);
        }

    }

    public ResponseEntity<ArticleListResponse> getArticleList(Long boardPk, Integer sort, Integer page) {
        List<Article> articleList = new ArrayList<>();
        Long totalRecords = articleMapper.findTotalRecords(boardPk);
        logger.info("totalRecords:{}", totalRecords);
        Long totalPages = (totalRecords/ARTICLE_LIST_SIZE);
        if (totalRecords % ARTICLE_LIST_SIZE != 0) {
            totalPages++;
        }
        if (page < 1 || page > totalPages) {
            // return emtpy list
            return ResponseEntity.ok().body(ArticleListResponse.builder()
                    .list(new ArrayList<>())
                    .currentPage(page)
                    .totalPages(totalPages.intValue())
                    .startPage(1)
                    .endPage(1)
                    .hasPrevious(false)
                    .hasNext(false)
                    .build());
        }
        logger.info("totalPages:{}", totalPages);
        Integer startPage = (page - 1) / PAGE_LIST_SIZE * PAGE_LIST_SIZE + 1;
        Integer endPage = Math.min(startPage + PAGE_LIST_SIZE - 1, totalPages.intValue());
        Boolean hasPrevious = startPage > 1;
        Boolean hasNext = endPage < totalPages.intValue();
        Integer start = (page - 1) * ARTICLE_LIST_SIZE;

        if (sort == 0) {
            articleList = articleMapper.findByBoardPk(boardPk, start, ARTICLE_LIST_SIZE);
        } else if (sort == 1) {
            articleList = articleMapper.findByBoardPkOrderByViewCount(boardPk, start, ARTICLE_LIST_SIZE);
        } else if (sort == 2) {
            articleList = articleMapper.findByBoardPkOrderByLikeCount(boardPk, start, ARTICLE_LIST_SIZE);
        } else {
            throw new InvalidRequestException(InvalidRequestException.WRONG_REQUEST);
        }

        List<ArticleMainResponse> list = articleList.stream().map(article ->
                ArticleMainResponse.builder()
                        .articlePk(article.getArticlePk())
                        .boardPk(article.getArticleBoard().getBoardPk())
                        .boardTitle(article.getArticleBoard().getTitle())
                        .articleTitle(article.getTitle())
                        .viewCount(article.getViewCount())
                        .likeCount(article.getLikeCount())
                        .dislikeCount(article.getDislikeCount())
                        .commentCount(article.getComments().size())
                        .build()
        ).toList();

        return ResponseEntity.ok().body(ArticleListResponse.builder()
                .list(list)
                .currentPage(page)
                .totalPages(totalPages.intValue())
                .startPage(startPage)
                .endPage(endPage)
                .hasPrevious(hasPrevious)
                .hasNext(hasNext)
                .build());
    }
    @Transactional
    public ResponseEntity<ArticleDetailResponse> getArticleDetail(Long articlePk, Long memberPk) {
        ArticleDetailMapperDTO article = articleMapper.findArticleDetail(articlePk).orElseThrow(()->
                new ArticleNotFoundException(ArticleNotFoundException.ARTICLE_NOT_FOUND));
        int size = article.getComments() != null ?article.getComments().size():0;
        logger.info("viewCnt : {}", article.getViewCnt());
        logger.info("comments Size : {}", article.getComments().size());
        Optional<Boolean> reactionOpt = articleReactionMapper.FindIsLikeByArticlePkAndMemberPk(articlePk, memberPk);
        Boolean isLike = reactionOpt.orElse(false);
        Boolean isDislike = !reactionOpt.orElse(true);
        System.out.println(memberPk);
        Boolean isMine = article.getMemberPk().equals(memberPk);
        Optional<Long> judgementPkOpt = judgementMapper.findPkByArticlePk(articlePk);
        Boolean isJudgement = judgementPkOpt.isPresent();
        updateViewCount(articlePk);
        ArticleDetailResponse response = ArticleDetailResponse.builder()
                .articlePk(articlePk)
                .boardPk(article.getBoardPk()).title(article.getTitle())
                .articleContent(article.getArticleContent()).nickname(article.getNickname())
                .likeCount(article.getLikeCount()).dislikeCount(article.getDislikeCount())
                .viewCnt(article.getViewCnt()).commentCount(size)
                .createdDttm(article.getCreatedDttm())
                .isLike(isLike).isDislike(isDislike).isMine(isMine)
                .isJudgement(isJudgement).judgementPk(judgementPkOpt.orElse(null)).build();
        return ResponseEntity.ok().body(response);
    }
    public void updateViewCount(Long articlePk){
        articleMapper.updateViewCount(articlePk);

        //오늘의 인기글을 위해서 redis에 점수 더하기
        String key = "ranking:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(articlePk), 0.5);
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    public ResponseEntity<List<HotArticleResponse>> getHotArticles() {
        //상위 10개 점수 가져오기
        String key = "ranking:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 9);
        List<HotArticleResponse> responses = Optional.ofNullable(typedTuples)
                .map(tuples -> tuples.stream().map(tuple -> {
                    Long articlePk = Long.parseLong(tuple.getValue());
                    HotArticleResponse item = articleMapper.findHotArticleResponses(articlePk)
                            .orElseThrow(() -> new ArticleNotFoundException(
                                    ArticleNotFoundException.ARTICLE_NOT_FOUND));
                    return item;
                }).toList()
                ).orElseGet(ArrayList::new);
        return ResponseEntity.ok().body(responses);

    }






}
