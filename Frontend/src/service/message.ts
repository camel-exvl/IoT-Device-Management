import {deleteFetcher, getFetcher} from "../utils.ts";
import {MessageList} from "./typing";

export const GetMessageList = async (deviceID: string, pageNum: number, pageSize: number, timeAsc: boolean) => {
    return await getFetcher(`/message/list?deviceID=${deviceID}&pageNum=${pageNum}&pageSize=${pageSize}&timeAsc=${timeAsc}`) as {
        data: MessageList,
        code: number
    };
}

export const DeleteAllMessage = async (deviceID: string) => {
    return await deleteFetcher(`/message/delete/all?deviceID=${deviceID}`);
}

export const DeleteBulkMessage = async (deviceID: string, messageID: string[]) => {
    return await deleteFetcher(`/message/delete/bulk?deviceID=${deviceID}&messageID=${messageID}`);
}