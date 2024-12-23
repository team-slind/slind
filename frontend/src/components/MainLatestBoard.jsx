import "./css/MainLatestBoard.css";
import httpAxios from "../api/httpAxios";
import { useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
// import useAxios from "../useAxios";
import New from "./iconFolder/New";
import Like from "./iconFolder/Like";
import DisLike from "./iconFolder/DisLike";
import Comment from "./iconFolder/Comment";
import View from "./iconFolder/View";
const MainLatestBoard = () => {
  const axios = httpAxios;
  const idRef = useRef(0);
  const [latestPost, setLatestPost] = useState([]);
  const [isLoaded, setIsLoaded] = useState(false);
  const AxiosGetlatestPost = async () => {
    try {
      const response = await axios.get(
        "/api/article/main"
      );
      setLatestPost(response.data);
      setIsLoaded(true);
    } catch {
      console.error();
    }
  };
  useEffect(() => {
    AxiosGetlatestPost();
  }, []);

  return (
    <div className="mainLatestBoard-wrapper">
      <div className="board-header">
        <div className="board-name">
          <div className="boardIcon">
            <New />
          </div>
          <h3>최신글</h3>
        </div>
      </div>
      <div className="board-item-wrapper">
        <ul>
          {latestPost.map((item) => {
            return (
              <li>
                <div className="board-item-content">
                  <div className="item-board-name">
                    <Link
                      to={`/board/${item.boardTitle}`}
                      state={{
                        boardPk: 1,
                        boardName: item.boardTitle,
                        kind: 0, //kind: 0 -> 일반 게시판, kind: 1 -> 재판게시판
                      }}
                    >
                      {item.boardTitle}
                    </Link>
                  </div>
                  <div className="item-title">
                    <Link
                      to={`/board/${item.boardTitle}/Post/${item.articleTitle}`}
                      state={{
                        boardName: item.boardTitle,
                        articlePk: item.articlePk,
                        kind: 0, //kind: 0 -> 일반 게시판, kind: 1 -> 재판게시판
                      }}
                    >
                      {item.articleTitle}
                    </Link>
                  </div>
                  <div className="item-imoji-wrapper">
                    <div className="item-imoji-content">
                      <View />
                      <div className="item-imoji-count">{item.viewCount}</div>
                    </div>
                    <div className="item-imoji-content">
                      <Like />
                      <div className="item-imoji-count">{item.likeCount}</div>
                    </div>
                    <div className="item-imoji-content">
                      <DisLike />
                      <div className="item-imoji-count">
                        {item.dislikeCount}
                      </div>
                    </div>
                    <div className="item-imoji-content">
                      <Comment />
                      <div className="item-imoji-count">
                        {item.commentCount}
                      </div>
                    </div>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
};

export default MainLatestBoard;
