import React, {createContext, useEffect, useReducer, useState} from "react";
import {ConfigProvider, theme} from "antd";
import {BarChartOutlined, ControlOutlined, SmileOutlined, UserOutlined} from "@ant-design/icons";
import {ProLayout} from "@ant-design/pro-components";
import {BrowserRouter, Link, Navigate, Route, Routes} from "react-router-dom";
import Welcome from "./pages/Welcome/Welcome.tsx";
import {AvatarProps} from "./components/AvatarDropDown.tsx";
import LoginPage from "./pages/User/Login";
import {UserInfo} from "./service/typing";
import DevicePage from "./pages/Device";
import MessagePage from "./pages/Message";
import UserInfoPage from "./pages/User/Info";
import RegisterPage from "./pages/User/Register/register.tsx";

const route = {
    path: '/',
    routes: [
        {
            path: '/welcome',
            name: '欢迎',
            icon: <SmileOutlined/>,
        },
        {
            path: '/user/settings',
            name: '个人信息',
            icon: <UserOutlined/>,
        },
        {
            path: '/device',
            name: '设备管理',
            icon: <ControlOutlined/>,
        },
        {
            path: '/message',
            name: '上报数据',
            icon: <BarChartOutlined/>,
        }
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
    const [pathname, setPathname] = useState(window.location.pathname);
    useEffect(() => {
        setPathname(window.location.pathname);
    }, [window.location.pathname]);
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
                        <Route path="/user/register" element={<RegisterPage/>}/>
                        <Route path="/*" element={
                            <ProLayout title={"物华天宝"} logo={"/logo.svg"} layout={"mix"} route={route}
                                       fixSiderbar={true} contentWidth={"Fluid"} location={{pathname}}
                                       avatarProps={AvatarProps([userInfo, setUserInfo])}
                                       menuItemRender={(menuItemProps, defaultDom) => {
                                           if (menuItemProps.isUrl || !menuItemProps.path) {
                                               return defaultDom;
                                           }
                                           return <Link to={menuItemProps.path} onClick={() => {
                                               setPathname(menuItemProps.path || "/welcome");
                                           }}>{defaultDom}</Link>;
                                       }}>
                                <Routes>
                                    <Route path="/" element={<Navigate replace to={"/welcome"}/>}/>
                                    <Route path="/welcome" element={<Welcome/>}/>
                                    <Route path="/user/settings" element={<UserInfoPage/>}/>
                                    <Route path="/device" element={<DevicePage/>}/>
                                    <Route path="/message" element={<MessagePage/>}/>
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