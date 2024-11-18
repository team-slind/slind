import "./css/Login.css";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Logo from "../assets/RealLogoWithoutBackground.png";
import { useAuth } from "../AuthContext";
import axios from "axios";
const Login = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [input, setInput] = useState({
    id: "",
    password: "",
  });
  const onChange = (e) => {
    console.log(e.target.name, e.target.value);
    setInput({
      ...input,
      [e.target.name]: e.target.value,
    });
  };

  const Login = async () => {
    try {
      const response = await axios.post(
        "http://localhost:8080/api/member/signin",
        input
      );

      const tokens = response.data; // Assumes the response contains access_token and refresh_token
      login(tokens); // Set the tokens in context and localStorage

      // Redirect or do something after login
    } catch (error) {
      console.error("Login failed:", error);
    }
  };

  const chechValueForLogin = () => {
    Login();
    // setData(prevData => [...prevData, ...newData]);
    // setHasMore(newData.length > 0);
  };
  const gotoJoin = () => {
    navigate("/Join");
  };
  const gotoFindPassword = () => {
    navigate("/FindPassword");
  };
  return (
    <div className="body-wrapper">
      <div className="login-wrapper">
        <div className="login-header-content">
          <Link to="/">
            <img src={Logo} alt="logo" />
          </Link>
        </div>
        <div className="login-warning"></div>
        <div className="login-form-wrapper">
          <input
            name="id"
            value={input.id}
            onChange={onChange}
            type="text"
            placeholder="아이디 입력"
          />
          <input
            name="password"
            value={input.password}
            onChange={onChange}
            type="password"
            placeholder="비밀번호 입력"
          />
          <button onClick={chechValueForLogin}>로그인</button>
        </div>
        <div className="login-other-wrapper">
          <div className="login-other-content">
            <div>비밀번호 기억이 안나세요?</div>
            <button onClick={gotoFindPassword}>비밀 번호 찾기</button>
          </div>
          <div className="login-other-content">
            <div>회원이 아니세요?</div>
            <button onClick={gotoJoin}>회원가입 하기</button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
