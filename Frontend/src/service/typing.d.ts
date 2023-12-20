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