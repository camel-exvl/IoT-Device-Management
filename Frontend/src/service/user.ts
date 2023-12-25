import React from "react";
import {getFetcher, postFetcher, putFetcher} from "../utils.ts";
import {LoginData, ModifyInfoData, ModifyPasswordData, RegisterData, UserInfo} from "./typing";

export const Current = async (user: [UserInfo, React.Dispatch<{ type: string, payload: UserInfo }>]) => {
    const [userInfo, setUserInfo] = user

    getFetcher(`/user/current`).then((data) => {
        if (data.data !== undefined && data.data !== null && data.data.username !== userInfo?.username) {
            setUserInfo({type: "set", payload: data.data});
        }
        return data;
    });
}

export const Register = async (registerData: RegisterData) => {
    return postFetcher(`/user/register`, JSON.stringify(registerData));
}

export const Login = async (loginData: LoginData) => {
    return postFetcher(`/user/login`, JSON.stringify(loginData));
}

export const Logout = async () => {
    return getFetcher(`/user/logout`);
}

export const ModifyInfo = async (modifyInfoData: ModifyInfoData) => {
    return putFetcher(`/user/modify`, JSON.stringify(modifyInfoData));
}

export const ModifyPassword = async (modifyPasswordData: ModifyPasswordData) => {
    return putFetcher(`/user/modifyPassword`, JSON.stringify(modifyPasswordData));
}