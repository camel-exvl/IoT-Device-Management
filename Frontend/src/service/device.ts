import {deleteFetcher, getFetcher, postFetcher, putFetcher} from "../utils.ts";
import {DeviceListData} from "./typing";

export const SearchDevice = async (name?: string, type?: number) => {
    const nameFetch = name || "";
    const typeFetch = type || "";
    return await getFetcher(`/device/search?name=${nameFetch}&type=${typeFetch}`) as {
        data: DeviceListData[],
        code: number
    };
}

export const GetDeviceStatistics = async () => {
    return getFetcher(`/device/statistics`);
}

export const GetActiveDeviceNums = async () => {
    return getFetcher(`/device/active`);
}

export const CreateDevice = async (name: string, type: number, description: string) => {
    return postFetcher(`/device/create`, JSON.stringify({name, type, description}));
}

export const ModifyDevice = async (id: string, name: string, type: number, description: string) => {
    return putFetcher(`/device/modify`, JSON.stringify({id, name, type, description}));
}

export const DeleteDevice = async (id: string) => {
    return deleteFetcher(`/device/delete?id=${id}`);
}