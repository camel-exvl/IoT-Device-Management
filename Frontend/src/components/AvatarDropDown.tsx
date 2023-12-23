import React, {useCallback} from "react";
import {Avatar, message} from "antd";
import {LoginOutlined, LogoutOutlined, UserOutlined} from "@ant-design/icons";
import HeaderDropdown from "./HeaderDropDown.tsx";
import {MenuInfo} from "rc-menu/es/interface";
import {Current, Logout} from "../service/user.ts";
import {useNavigate} from "react-router-dom";
import {UserInfo} from "../service/typing";

type GlobalHeaderRightProps = {
    user: [UserInfo, React.Dispatch<{ type: string, payload: UserInfo }>]
    children?: React.ReactNode;
};

export const AvatarProps = (user: [UserInfo, React.Dispatch<{ type: string, payload: UserInfo }>]) => {
    const [userInfo, setUserInfo] = user;
    Current(user);
    return {
        src: null,
        title: userInfo?.username,
        render: (_, avatarChildren) => {
            return <><Avatar style={{backgroundColor: "#7464fa", verticalAlign: 'middle'}}
                             size="large">{userInfo?.username[0].toUpperCase()}</Avatar><AvatarDropdown
                user={user}>{avatarChildren}</AvatarDropdown></>;
        }
    };
}
const AvatarDropdown: React.FC<GlobalHeaderRightProps> = ({user, children}) => {
    const [userInfo, setUserInfo] = user;
    const navigate = useNavigate();

    const onMenuClick = useCallback(
        (event: MenuInfo) => {
            const {key} = event;
            switch (key) {
                case 'settings':
                    navigate('/user/settings');
                    return;
                case 'logout':
                    Logout().then(() => setUserInfo({
                        type: "set",
                        payload: {userId: "", username: "Guest", email: ""}
                    }));
                    message.success('退出成功！');
                    navigate('/welcome');
                    return;
                case 'login':
                    navigate('/user/login');
                    return;
            }
        },
        [navigate, setUserInfo],
    );

    const menuItems = [
        ...(userInfo?.username === "Guest" ? [] : [
            {
                key: 'settings',
                icon: <UserOutlined/>,
                label: '个人信息',
            },
            {
                type: 'divider' as const,
            },
        ]),
        (userInfo?.username === "Guest" ?
            {
                key: 'login',
                icon: <LoginOutlined/>,
                label: '登录',
            } : {
                key: 'logout',
                icon: <LogoutOutlined/>,
                label: '退出登录',
            }),
    ];

    return (
        <HeaderDropdown
            menu={{
                selectedKeys: [],
                onClick: onMenuClick,
                items: menuItems,
            }}
        >
            {children}
        </HeaderDropdown>
    );
};
