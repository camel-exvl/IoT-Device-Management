import React from "react";
import {getFetcher, postFetcher} from "../utils.ts";
import {LoginData, UserInfo} from "./typing";

export const Current = async (user: [UserInfo, React.Dispatch<{ type: string, payload: UserInfo }>]) => {
    const [userInfo, setUserInfo] = user

    getFetcher(`/user/current`).then((data) => {
        if (data.data !== undefined && data.data !== null && data.data.username !== userInfo?.username) {
            setUserInfo({type: "set", payload: data.data});
        }
        return data;
    });
}

export const Login = async (loginData: LoginData) => {
    return postFetcher(`/user/login`, JSON.stringify(loginData));
}

export const Logout = async () => {
    return getFetcher(`/user/logout`);
}