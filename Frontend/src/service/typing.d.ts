export interface Response<T> {
    success: boolean; // 请求是否成功处理并返回
    data: T | null; // 返回的数据
    code: number; // 错误码
    errorMessage: string; // 错误信息
}

export interface UserInfo {
    userId: string;
    username: string;
    email: string;
}

export interface RegisterData {
    username: string;
    email: string;
    password: string;
}

export interface LoginData {
    username: string;
    password: string;
    rememberMe: boolean;
}

export interface DeviceStatistics {
    deviceCount: number;
    activeDeviceCount: number;
    messageCount: number;
    deviceType: {type: number, num: number}[];
}

export interface DeviceActiveNums {
    time: number;
    activeNum: number;
}

export interface DeviceListData {
    id: string,
    name: string,
    type: number,
    description: string,
}

export interface MessageListData {
    id: string,
    info: string,
    value: number;
    alert: boolean;
    lng: number;
    lat: number;
    time: number;
}

export interface MessageList {
    total: number;
    messages: MessageListData[];
}