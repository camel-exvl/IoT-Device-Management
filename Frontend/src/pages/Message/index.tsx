import React, {useContext, useEffect, useRef, useState} from "react";
import {ActionType, PageContainer, ProColumns} from "@ant-design/pro-components";
import {Button, Card, message, Popconfirm, Select, Space, Table} from "antd";
import {UserInfoContext} from "../../app.tsx";
import {ProTable} from "@ant-design/pro-table/lib";
import {SearchDevice} from "../../service/device.ts";
import {DeleteAllMessage, DeleteBulkMessage, GetMessageList} from "../../service/message.ts";
import {MessageListData} from "../../service/typing";
import {useNavigate} from "react-router-dom";
import {DeleteOutlined} from "@ant-design/icons";

const MessagePage: React.FC = () => {
    const actionRef = useRef<ActionType>();
    const [deviceListLoading, setDeviceListLoading] = useState(true);
    const [deviceList, setDeviceList] = useState([] as { label: string, value: string }[]);
    const [deviceSelected, setDeviceSelected] = useState("");
    const [messageApi, contextHolder] = message.useMessage();
    const [userInfo, setUserInfo] = useContext(UserInfoContext);
    const navigate = useNavigate();

    useEffect(() => {
        SearchDevice().then((res) => {
            if (res.code === 401) {
                navigate("/user/login", {replace: true, state: {from: "/message"}});
            }
            setDeviceListLoading(false);
            // map res to deviceList{label, value}
            const deviceList = [] as { label: string, value: string }[];
            for (const device of res.data) {
                deviceList.push({label: device.name, value: device.id});
            }
            setDeviceList(deviceList);
            return res.data;
        });
    }, [navigate]);

    const SelectFilterOption = (input: string, option?: { label: string; value: string }) =>
        (option?.label ?? '').toLowerCase().includes(input.toLowerCase());

    const onSelectChange = (value: string) => {
        setDeviceSelected(value);
        actionRef.current?.clearSelected?.();
        actionRef.current?.reload();
    };

    const columns: ProColumns<MessageListData>[] = [
        {
            dataIndex: 'index',
            valueType: 'index',
            width: 48,
        },
        {
            title: "设备ID",
            dataIndex: "id",
            key: "id",
            hideInSearch: true,
            hideInTable: true,
        },
        {
            title: "信息",
            dataIndex: "info",
            key: "info",
            hideInSearch: true,
            ellipsis: true,
        },
        {
            title: "数值",
            dataIndex: "value",
            key: "value",
            hideInSearch: true,
        },
        {
            title: "是否报警",
            dataIndex: "alert",
            key: "alert",
            hideInSearch: true,
            valueEnum: {
                false: {text: "否", status: "Success"},
                true: {text: "是", status: "Error"},
            },
            filters: true,
            onFilter: true,
        },
        {
            title: "经度",
            dataIndex: "lng",
            key: "lng",
            hideInSearch: true,
            hideInTable: true,
        },
        {
            title: "纬度",
            dataIndex: "lat",
            key: "lat",
            hideInSearch: true,
            hideInTable: true,
        },
        {
            title: "时间",
            dataIndex: "time",
            key: "time",
            hideInSearch: true,
            valueType: "dateTime",
        },
    ];

    return (
        <PageContainer>
            {contextHolder}
            <Card>
                <Select placeholder={"请选择设备"} showSearch={true} style={{marginLeft: 24, width: 200}}
                        options={deviceList} loading={deviceListLoading} filterOption={SelectFilterOption}
                        onChange={onSelectChange}/>
                <ProTable<MessageListData>
                    columns={columns} actionRef={actionRef} rowSelection={{selections: [Table.SELECTION_INVERT],}}
                    request={async (params, sort, filter) => {
                        if (deviceSelected === "") {
                            return {
                                data: [],
                                success: false,
                                total: 0,
                            };
                        }
                        const res = await GetMessageList(deviceSelected, (params.current ?? 1) - 1, params.pageSize ?? 10);
                        return {
                            data: res.data.messages,
                            success: true,
                            total: res.data.total,
                        };
                    }}
                    tableAlertRender={({selectedRowKeys, selectedRows, onCleanSelected,}) => {
                        return (
                            <Space size={24}>
                                <span>
                                    已选 {selectedRowKeys.length} 项
                                    <a style={{marginInlineStart: 8}} onClick={onCleanSelected}>
                                        取消选择
                                    </a>
                                </span>
                                <span>{`报警数量: ${selectedRows.reduce(
                                    (pre, item) => pre + (item.alert ? 1 : 0), 0,)} 条`}
                                </span>
                            </Space>
                        );
                    }}
                    tableAlertOptionRender={(props) => {
                        return (
                            <Space size={16}>
                                <Popconfirm title={"确定删除选中的消息吗？"} onConfirm={() => {
                                    DeleteBulkMessage(deviceSelected,props.selectedRowKeys as string[]).then((res) => {
                                        if (res.code === 200) {
                                            messageApi.success("删除成功");
                                            if (actionRef.current) {
                                                actionRef.current.clearSelected?.();
                                                actionRef.current.reload();
                                            }
                                        } else {
                                            messageApi.error("删除失败");
                                        }
                                    })
                                }}>
                                    <a>批量删除</a>
                                </Popconfirm>
                            </Space>
                        );
                    }}
                    rowKey="id" search={false} pagination={{pageSize: 10,}}
                    dateFormatter="string" headerTitle="消息列表"
                    toolBarRender={() => [
                        <Popconfirm title={"确定删除本设备所有消息吗？"} onConfirm={() => {
                            DeleteAllMessage(deviceSelected).then((res) => {
                                if (res.code === 200) {
                                    messageApi.success("删除成功");
                                    if (actionRef.current) {
                                        actionRef.current.reload();
                                    }
                                } else {
                                    messageApi.error("删除失败");
                                }
                            });

                        }}>
                            <Button key="button" icon={<DeleteOutlined/>} disabled={deviceSelected === ""}
                                    type="primary">删除本设备所有消息</Button>
                        </Popconfirm>]}
                    options={{fullScreen: false, reload: true, density: false, setting: false}}/>
            </Card>
        </PageContainer>
    );
}

export default MessagePage;