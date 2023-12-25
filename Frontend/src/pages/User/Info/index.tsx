import {UserInfoContext} from "../../../app.tsx";
import React, {useContext, useEffect, useState} from "react";
import {PageContainer, ProForm, ProFormText} from "@ant-design/pro-components";
import {Alert, Card, message} from "antd";
import {ModifyInfoData, ModifyPasswordData} from "../../../service/typing";
import {Current, ModifyInfo, ModifyPassword} from "../../../service/user.ts";
import {useNavigate} from "react-router-dom";
import Title from "antd/es/typography/Title";

const UserInfoPage: React.FC = () => {
    const [errorMsg, setErrorMsg] = useState<string>("");
    const [userInfo, setUserInfo] = useContext(UserInfoContext);
    const navigate = useNavigate();

    useEffect(() => {
        if (userInfo.userId === "") {
            navigate("/user/login", {replace: true, state: {from: "/user/info"}});
        }
    }, [userInfo.userId]);

    const handleSubmit = async (values: ModifyInfoData) => {
        const {data, code} = await ModifyInfo(values);
        switch (code) {
            case 200:
                setErrorMsg("");
                message.success('修改成功！');
                await Current([userInfo, setUserInfo])
                return;
            case 404:
                setErrorMsg("修改失败：用户不存在");
                break;
            case 500:
                setErrorMsg("修改失败：服务器错误");
                break;
            default:
                setErrorMsg("修改失败，请重试");
        }
    }

    const handlePasswordSubmit = async (values: ModifyPasswordData) => {
        const {data, code} = await ModifyPassword(values);
        switch (code) {
            case 200:
                setErrorMsg("");
                message.success('修改成功！');
                setUserInfo({type: "set", payload: {userId: "", username: "Guest", email: ""}});
                navigate("/user/login", {replace: true});
                return;
            case 401:
                setErrorMsg("修改失败：密码错误");
                break;
            case 404:
                setErrorMsg("修改失败：用户不存在");
                break;
            case 500:
                setErrorMsg("修改失败：服务器错误");
                break;
            default:
                setErrorMsg("修改失败，请重试");
        }
    }

    return (
        <PageContainer>
            <Card>
                <ProForm layout="horizontal" onFinish={async (values) => {
                    await handleSubmit(values as ModifyInfoData);
                }}>
                    {errorMsg !== "" && (<Alert style={{marginBottom: 24,}} message={errorMsg} type="error" showIcon/>)}
                    <ProFormText
                        name="username"
                        label="用户名"
                        placeholder="请输入用户名"
                        width="md"
                        initialValue={userInfo.username}
                        rules={[{required: true, message: "请输入用户名！",}, {
                            validator(_, value, callback) {
                                const regExp = /^[a-zA-Z0-9_-]{6,20}$/;
                                if (value != "" && !regExp.test(value)) {
                                    callback("用户名不合法(长度在6-20位, 只能包含大小写字母、数字、下划线和减号)");
                                } else {
                                    callback();
                                }
                            },
                        },]}
                    />

                    <ProFormText
                        name="email"
                        label="邮箱"
                        placeholder="请输入邮箱"
                        width="md"
                        initialValue={userInfo.email}
                        rules={[{required: true, message: "请输入正确的邮箱！", type: "email",},]}
                    />
                </ProForm>
            </Card>
            <Title level={4}>修改密码</Title>
            <Card>
                <ProForm layout="horizontal" onFinish={async (values) => {
                    await handlePasswordSubmit(values as ModifyPasswordData);
                }}>
                    <ProFormText.Password name="oldPassword" label="原密码" placeholder="请输入原密码" width="md"
                                          rules={[{required: true}]}/>
                    <ProFormText.Password
                        name="newPassword"
                        label="新密码"
                        placeholder="请输入新密码"
                        width="md"
                        rules={[{required: true, message: "请输入新密码！",}, {
                            validator(_, value, callback) {
                                const regExp = /^[a-zA-Z0-9_-]{6,20}$/;
                                if (value != "" && !regExp.test(value)) {
                                    callback("密码不合法(长度在6-20位, 只能包含大小写字母、数字、下划线和减号)");
                                } else {
                                    callback();
                                }
                            },
                        },]}
                    />
                </ProForm>
            </Card>
        </PageContainer>
    );
}

export default UserInfoPage;