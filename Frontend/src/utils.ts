import type {Response} from "./service/typing";

// const prefix = "http://127.0.0.1:8080/api";
const prefix = "https://iot.camel-zy.top/api";

export async function getFetcher(key: string) {

    const resp = (await fetch(prefix + key, {credentials: "include", mode: "cors"}).then((res) =>
        res.json()
    )) as Response<any>;

    console.log(resp);
    return {data: resp.data, code: resp.code};
}

export async function postFetcher(
    key: string,
    //注：Record类型用于创建一个具有指定属性类型的新对象类型
    body: string
) {
    const resp = (await fetch(prefix + key, {
        credentials: "include",
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: body,
        mode: "cors",
    }).then((res) => res.json())) as Response<any>;

    console.log(resp);
    return {data: resp.data, code: resp.code};
}

export async function putFetcher(
    key: string,
    //注：Record类型用于创建一个具有指定属性类型的新对象类型
    body: string
) {
    const resp = (await fetch(prefix + key, {
        credentials: "include",
        method: "PUT",
        headers: {"Content-Type": "application/json"},
        body: body,
        mode: "cors",
    }).then((res) => res.json())) as Response<any>;

    console.log(resp);
    return {data: resp.data, code: resp.code};
}

export async function deleteFetcher(key: string) {
    const resp = (await fetch(prefix + key, {
        credentials: "include",
        method: "DELETE",
        mode: "cors",
    }).then((res) => res.json())) as Response<any>;

    console.log(resp);
    return {data: resp.data, code: resp.code};
}