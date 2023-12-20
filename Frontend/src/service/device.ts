import {deleteFetcher, getFetcher, postFetcher, putFetcher} from "../utils.ts";

export const SearchDevice = async (name: string, type: number) => {
    return getFetcher(`/device/search?name=${name}&type=${type}`);
}

export const GetDeviceStatistics = async () => {
    return getFetcher(`/device/statistics`);
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