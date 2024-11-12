package com.team2.slind.board.service;

import com.team2.slind.board.dto.request.BoardCreateRequest;
import com.team2.slind.board.dto.response.BoardResponse;
import com.team2.slind.board.mapper.BoardMapper;
import com.team2.slind.board.vo.Board;
import com.team2.slind.common.exception.BoardNotFoundException;
import com.team2.slind.common.exception.DuplicateTitleException;
import com.team2.slind.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardMapper boardMapper;
    static Long memberPk = 1L;

    public ResponseEntity createBoard(BoardCreateRequest boardCreateRequest) {
        String title = boardCreateRequest.getTitle();
        if (checkDuplicate(title)){
            throw new DuplicateTitleException(DuplicateTitleException.DUPLICATE_BOARD_TITLE);
        }
        Board board = Board.builder().title(title).memberPk(memberPk).build();
        boardMapper.addBoard(board);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity checkDuplicateBoard(String title) {
        if (checkDuplicate(title)){
            throw new DuplicateTitleException(DuplicateTitleException.DUPLICATE_BOARD_TITLE);
        }
        return ResponseEntity.ok().build();
    }

    public boolean checkDuplicate(String title) {
        Optional<Board> board = boardMapper.findByBoardTitle(title);
        return board.isPresent();
    }

    public ResponseEntity deleteBoard(Long boardPk) {
        Board board = boardMapper.findByBoardPk(boardPk).orElseThrow(()->
                new BoardNotFoundException(BoardNotFoundException.BOARD_NOT_FOUND));
        if (board.getMemberPk()!=memberPk){
            throw new UnauthorizedException(UnauthorizedException.UNAUTHORIZED_DELETE_BOARD);
        }
        Long result = boardMapper.deleteByBoardPk(boardPk);
        if (result == 0L){
            throw new BoardNotFoundException(BoardNotFoundException.BOARD_NOT_FOUND);
        }
        return ResponseEntity.ok().build();

    }

    public ResponseEntity<List<BoardResponse>> getBoardList(){
        List<BoardResponse> responseList = boardMapper.findAllBoards().stream().map(board ->
                BoardResponse.builder().boardPk(board.getBoardPk()).boardTitle(board.getTitle()).build())
                .toList();

        return ResponseEntity.ok().body(responseList);
    }
}
