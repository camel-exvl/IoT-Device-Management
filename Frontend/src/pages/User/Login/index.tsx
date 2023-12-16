import {LockOutlined, UserOutlined,} from '@ant-design/icons';
import {LoginForm, ProFormCheckbox, ProFormText,} from '@ant-design/pro-components';
import {Alert, message} from 'antd';
import React, {useContext, useState} from 'react';
import {useEmotionCss} from "@ant-design/use-emotion-css";
import {Current, Login} from "../../../service/user.ts";
import {LoginData} from "../../../service/typing";
import {useNavigate} from "react-router-dom";
import {UserInfoContext} from "../../../App.tsx";

const LoginMessage: React.FC<{
    content: string;
}> = ({content}) => {
    return (
        <Alert
            style={{
                marginBottom: 24,
            }}
            message={content}
            type="error"
            showIcon
        />
    );
};

const LoginPage: React.FC = () => {
        const [errorMsg, setErrorMsg] = useState<string>("");
        const [userInfo, setUserInfo] = useContext(UserInfoContext);
        const navigate = useNavigate();

        const containerClassName = useEmotionCss(() => {
            return {
                display: 'flex',
                flexDirection: 'column',
                height: '100vh',
                overflow: 'auto',
                backgroundImage:
                    "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
                backgroundSize: '100% 100%',
            };
        });

        const handleSubmit = async (values: LoginData) => {
            const {data, code} = await Login(values);
            switch (code) {
                case 200:
                    setErrorMsg("");
                    message.success('登录成功！');
                    await Current([userInfo, setUserInfo]);
                    navigate("/")
                    return;
                case 404:
                    setErrorMsg("登录失败：用户不存在");
                    break;
                case 401:
                    setErrorMsg("登录失败：密码错误");
                    break;
                case 500:
                    setErrorMsg("登录失败：服务器错误");
                    break;
                default:
                    setErrorMsg("登录失败，请重试");
            }
        }

        return (
            <div className={containerClassName}>
                <title>
                    登录 - 物华天宝
                </title>
                <div
                    style={{
                        flex: '1',
                        padding: '32px 0',
                    }}
                >
                    <LoginForm
                        contentStyle={{
                            minWidth: 280,
                            maxWidth: '75vw',
                        }}
                        containerStyle={{
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                        }}
                        logo={<img alt="logo" src="/logo.svg"/>}
                        title="物华天宝"
                        subTitle="联·动未来，智·控生活"
                        initialValues={{
                            autoLogin: true,
                        }}
                        onFinish={async (values) => {
                            await handleSubmit(values as LoginData);
                        }}
                    >

                        {errorMsg !== "" && (<LoginMessage content={errorMsg}/>)}
                        {(
                            <>
                                <ProFormText
                                    name="username"
                                    fieldProps={{
                                        size: 'large',
                                        prefix: <UserOutlined/>,
                                    }}
                                    placeholder={"用户名："}
                                    rules={[{required: true, message: "请输入用户名！",},]}
                                />
                                <ProFormText.Password
                                    name="password"
                                    fieldProps={{
                                        size: 'large',
                                        prefix: <LockOutlined/>,
                                    }}
                                    placeholder={"密码："}
                                    rules={[{required: true, message: "请输入密码！",},]}
                                />
                            </>
                        )}

                        <div
                            style={{
                                marginBottom: 24,
                            }}
                        >
                            <ProFormCheckbox noStyle name="rememberMe">
                                自动登录
                            </ProFormCheckbox>
                        </div>
                    </LoginForm>
                </div>
            </div>
        );
    }
;

export default LoginPage;
