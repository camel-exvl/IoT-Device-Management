import React, {useState} from "react";
import {LoginForm, ProFormText} from "@ant-design/pro-components";
import {RegisterData} from "../../../service/typing";
import {DoubleLeftOutlined, LockOutlined, UserOutlined} from "@ant-design/icons";
import {useEmotionCss} from "@ant-design/use-emotion-css";
import {Register} from "../../../service/user.ts";
import {Alert, Button, message} from "antd";
import {useNavigate} from "react-router-dom";

const RegisterMessage: React.FC<{
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

const RegisterPage: React.FC = () => {
    const [errorMsg, setErrorMsg] = useState<string>("");
    const navigate = useNavigate();

    const containerClassName = useEmotionCss(() => {
        return {
            display: 'flex',
            flexDirection: 'column',
            height: '100vh',
            // overflow: 'auto',
            // backgroundImage:
            //     "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
            // backgroundSize: '100% 100%',
        };
    });

    const handleSubmit = async (values: RegisterData) => {
        const {data, code} = await Register(values);
        switch (code) {
            case 201:
                setErrorMsg("");
                message.success('注册成功！');
                navigate("/user/login");
                return;
            case 400:
                setErrorMsg("注册失败：用户名、邮箱或密码不合法");
                break;
            case 409:
                setErrorMsg("注册失败：用户名或邮箱已存在");
                break;
            case 500:
                setErrorMsg("注册失败：服务器错误");
                break;
            default:
                setErrorMsg("注册失败，请重试");
        }
    }

    document.title = "注册 - 物华天宝";
    return (
        <div className={containerClassName}>
            <title>
                注册 - 物华天宝
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
                        await handleSubmit(values as RegisterData);
                    }}
                    submitter={{
                        searchConfig: {
                            submitText: '注册',
                        },
                    }}
                >

                    {errorMsg !== "" && (<RegisterMessage content={errorMsg}/>)}
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
                            <ProFormText
                                name="email"
                                fieldProps={{
                                    size: 'large',
                                    prefix: <UserOutlined/>,
                                }}
                                placeholder={"邮箱："}
                                rules={[{required: true, message: "请输入正确的邮箱！", type: "email",},]}
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
                            display: 'flex',
                            justifyContent: 'space-between',
                        }}
                    >
                        <Button
                            icon={<DoubleLeftOutlined/>}
                            type="link"
                            size="small"
                            onClick={() => navigate("/user/login")}>
                            已有账号，去登录
                        </Button>
                    </div>
                </LoginForm>
            </div>
        </div>
    );
}

export default RegisterPage;