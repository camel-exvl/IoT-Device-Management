import React, {createContext, useReducer} from "react";
import {ConfigProvider, theme} from "antd";
import {SmileOutlined} from "@ant-design/icons";
import {ProLayout} from "@ant-design/pro-components";
import {BrowserRouter, Link, Navigate, Route, Routes} from "react-router-dom";
import Welcome from "./pages/Welcome.tsx";
import {AvatarProps} from "./components/AvatarDropDown.tsx";
import LoginPage from "./pages/User/Login";
import {UserInfo} from "./service/typing";

const route = {
    path: '/',
    routes: [
        {
            path: '/welcome',
            name: '欢迎',
            icon: <SmileOutlined/>,
        },
    ],
};

export const UserInfoReducer = (state: UserInfo, action: { type: string, payload: UserInfo }) => {
    switch (action.type) {
        case "set":
            return action.payload;
        default:
            return state;
    }
}
export const UserInfoContext = createContext<[UserInfo, React.Dispatch<{ type: string, payload: UserInfo }>]>([{
    userId: "",
    username: "never",
    email: ""
}, () => {
}]);

const App: React.FC = () => {
    const [userInfo, setUserInfo] = useReducer(UserInfoReducer, {userId: "", username: "Guest", email: ""});
    return (
        <UserInfoContext.Provider value={[userInfo, setUserInfo]}>
            <BrowserRouter>
                <ConfigProvider
                    theme={{
                        algorithm: theme.defaultAlgorithm,
                        token: {
                            colorPrimary: "#7464FA",
                            colorLink: "#7464FA",
                            colorInfo: "#7464FA",
                        },
                    }}
                >
                    <Routes>
                        <Route path="/user/login" element={<LoginPage/>}/>
                        <Route path="/*" element={
                            <ProLayout title={"物华天宝"} logo={"/logo.svg"} layout={"mix"} route={route}
                                       fixSiderbar={true} contentWidth={"Fluid"} avatarProps={AvatarProps([userInfo, setUserInfo])}
                                       menuItemRender={(menuItemProps, defaultDom) => {
                                           if (menuItemProps.isUrl || !menuItemProps.path) {
                                               return defaultDom;
                                           }
                                           return <Link to={menuItemProps.path}>{defaultDom}</Link>;
                                       }}>
                                <Routes>
                                    <Route path="/" element={<Navigate to="/welcome"/>}/>
                                    <Route path="/welcome" element={<Welcome/>}/>
                                </Routes>
                            </ProLayout>
                        }/>
                    </Routes>
                </ConfigProvider>
            </BrowserRouter>
        </UserInfoContext.Provider>
    );
};

export default App;