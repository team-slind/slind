import "./css/Nav.css";
import { useState, useRef, useEffect } from "react";
import axios from "axios";
import httpAxios from "../api/httpAxios";
import useAxios from "../api/useAxios";
// import BookMark from "./icon/BookMark";
import Modal from "./Modal/CreateBoardModal";
import { Link } from "react-router-dom";
// import axios from "axios";
// const mockitem1 = {
//   boardPk: 1,
//   boardTitle: "상처치료해줄사람어디없나가만히놔두다간끊임없이덧나",
// };
// const mockitem2 = {
//   boardPk: 2,
//   boardTitle: "와우",
// };
// const mockitem3 = {
//   boardPk: 3,
//   boardTitle: "백준오브레전드",
// };
// const mockitem4 = {
//   boardPk: 4,
//   boardTitle: "t",
// };
// const wholeMock = [];
// // for (let i = 0; i < 30; i++) {
// //   wholeMock.push(mockitem);
// // }
// wholeMock.push(mockitem1);
// wholeMock.push(mockitem2);
// wholeMock.push(mockitem3);
// wholeMock.push(mockitem4);
const Nav = () => {
  const customAxiosaxios = useAxios();
  const idRef = useRef(0);

  const [originalBoardList, setOriginalBoardList] = useState([]);
  // const [filteredBoard, setfilteredBoard] = useState([]);
  const [viewBoard, setViewBoard] = useState([]);
  const [isLoaded, setIsLoaded] = useState(false);
  const AxiosGetApiBoard = async () => {
    try {
      const response = await httpAxios.get("/api/board");
      setOriginalBoardList(response.data);
      setViewBoard(response.data);
      setIsLoaded(true);
    } catch {
      console.error();
    }
  };
  useEffect(() => {
    AxiosGetApiBoard();
    // setData(prevData => [...prevData, ...newData]);
    // setHasMore(newData.length > 0);
  }, []);
  const ReLoadBoards = () => {
    AxiosGetApiBoard();
  };
  const onChange = (e) => {
    if (e.target.value === "") setViewBoard(originalBoardList);
    else {
      const filtered = originalBoardList.filter((item) => {
        if (
          item.boardTitle.toLowerCase().includes(e.target.value.toLowerCase())
        )
          return true;
      });
      setViewBoard(filtered);
    }
  };

  // useState를 사용하여 open상태를 변경한다. (open일때 true로 만들어 열리는 방식)
  const [modalOpen, setModalOpen] = useState(false);

  const openModal = () => {
    setModalOpen(true);
  };
  const closeModal = () => {
    setModalOpen(false);
  };

  // useEffect(() => {
  //   // console.log(`count: ${count} , ${input}`);
  // }, [inputBoard]);
  return (
    <div className="Nav-wrapper">
      {/* <div className="Nav-header">
        <div className="Bookmark-icon"> */}
      {/* <BookMark /> */}
      {/* </div>
        <h2 className="Bookmark-title">즐겨찾기</h2>
      </div>
      <div className="Nav-board">
        <ul className="Nav-board-list">
          {viewBoard.map((item) => (
            <Link key={idRef.current++} className="Nav-board-item" to={`/board/${item.boardTitle}`}>
              <li>{item.boardTitle}</li>
            </Link>
          ))}
        </ul>
      </div> */}
      <div className="Nav-header second-header">
        <h2 className="board-title">게시판</h2>
        <button onClick={openModal}>새로 만들기</button>
        <Modal
          open={modalOpen}
          close={closeModal}
          header="게시판 생성"
          reload={ReLoadBoards}
        ></Modal>
      </div>
      <div className="Nav-search">
        <input
          name="boardname"
          onChange={onChange}
          type="text"
          placeholder="게시판 입력"
        />
      </div>
      <div className="Nav-board">
        <ul className="Nav-board-list">
          {viewBoard.map((item) => (
            <Link
              key={idRef.current++}
              className="Nav-board-item"
              to={`/board/${item.boardTitle}`}
              state={{
                boardPk: item.boardPk,
                boardName: item.boardTitle,
                kind: 0, //kind: 0 -> 일반 게시판, kind: 1 -> 재판게시판
              }}
            >
              <li>{item.boardTitle}</li>
            </Link>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default Nav;
