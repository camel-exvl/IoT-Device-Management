import React, {useEffect, useRef, useState} from "react";
import {ActionType, PageContainer, ProColumns} from "@ant-design/pro-components";
import {Button, Card, message, Popconfirm, Select, Space, Table} from "antd";
import {ProTable} from "@ant-design/pro-table/lib";
import {SearchDevice} from "../../service/device.ts";
import {DeleteAllMessage, DeleteBulkMessage, GetMessageList} from "../../service/message.ts";
import {MessageListData} from "../../service/typing";
import {useNavigate} from "react-router-dom";
import {DeleteOutlined} from "@ant-design/icons";
import {Map, Polyline} from '@pansy/react-amap';
import {Loca, ScatterLayer, useLoca} from '@pansy/react-amap-loca';
import Title from "antd/es/typography/Title";

const MessagePage: React.FC = () => {
    const actionRef = useRef<ActionType>();
    const map = useRef<AMap.Map>();
    const polyline = useRef<AMap.Polyline>();
    const [deviceListLoading, setDeviceListLoading] = useState(true);
    const [deviceList, setDeviceList] = useState([] as { label: string, value: string }[]);
    const [deviceSelected, setDeviceSelected] = useState("");
    const [messageApi, contextHolder] = message.useMessage();
    const [geoJSONCollectionAlert, setGeoJSONCollectionAlert] = useState({});
    const [geoJSONCollectionNoAlert, setGeoJSONCollectionNoAlert] = useState({});
    const [geoCenter, setGeoCenter] = useState<AMap.LocationValue>([120.21201, 30.2084]);
    const [geoPath, setGeoPath] = useState<AMap.LocationValue[]>([]);
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

    const convertToGeoJSON = (message: MessageListData) => {
        const {lng, lat, id, info, value, alert, time} = message;
        return {
            type: 'Feature',
            geometry: {
                type: 'Point',
                coordinates: [lng, lat],
            },
            properties: {
                id: id,
                info: info,
                value: value,
                alert: alert,
                time: time,
            },
        };
    };

    const Scatter = () => {
        const {loca} = useLoca();
        const [scatterRed, setScatterRed] = useState<Loca.ScatterLayer>();
        const [scatterYellow, setScatterYellow] = useState<Loca.ScatterLayer>();

        useEffect(() => {
            if (scatterRed && scatterYellow && loca) {
                const geoRed = new window.Loca.GeoJSONSource({
                    data: geoJSONCollectionAlert,
                });
                scatterRed.setSource(geoRed);
                scatterRed.setStyle({
                    unit: 'meter',
                    size: [5000, 5000],
                    borderWidth: 0,
                    texture: 'https://a.amap.com/Loca/static/loca-v2/demos/images/breath_red.png',
                    duration: 500,
                    animate: true,
                });
                loca.add(scatterRed);

                const geoYellow = new window.Loca.GeoJSONSource({
                    data: geoJSONCollectionNoAlert,
                });
                scatterYellow.setSource(geoYellow);
                scatterYellow.setStyle({
                    unit: 'meter',
                    size: [3000, 3000],
                    borderWidth: 0,
                    texture: 'https://a.amap.com/Loca/static/loca-v2/demos/images/breath_yellow.png',
                    duration: 1000,
                    animate: true,
                });
                loca.add(scatterYellow);

                loca.animate.start();
            }
        }, [scatterRed, scatterYellow, loca])

        return (
            <>
                <ScatterLayer
                    zIndex={113}
                    opacity={1}
                    visible={true}
                    zooms={[2, 22]}
                    events={{
                        created: (instance) => {
                            setScatterRed(instance);
                        }
                    }}
                />
                <ScatterLayer
                    zIndex={112}
                    opacity={1}
                    visible={true}
                    zooms={[2, 22]}
                    events={{
                        created: (instance) => {
                            setScatterYellow(instance);
                        }
                    }}
                />
            </>
        )
    }


    useEffect(() => {
        if (map.current) {
            map.current.setCenter(geoCenter);
        }
    }, [geoCenter]);

    useEffect(() => {
        console.log(geoPath, polyline);
        if (polyline.current && geoPath.length > 0) {
            polyline.current.setPath(geoPath);
        }
    }, [geoPath]);

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

                        if (res.data.messages.length === 0) {
                            setGeoCenter([120.21201, 30.2084]);
                            setGeoPath([]);
                            setGeoJSONCollectionAlert({});
                            setGeoJSONCollectionNoAlert({});
                            return {
                                data: [],
                                success: true,
                                total: 0,
                            };
                        }

                        setGeoCenter([res.data.messages[0].lng, res.data.messages[0].lat]);
                        const geoPath = res.data.messages.map((message) => [message.lng, message.lat] as AMap.LocationValue);
                        setGeoPath(geoPath)
                        const geoJSONFeatures = res.data.messages.map(convertToGeoJSON);
                        const geoJSONCollectionAlert = {
                            type: 'FeatureCollection',
                            features: geoJSONFeatures.filter((feature) => feature.properties.alert),
                        };
                        const geoJSONCollectionNoAlert = {
                            type: 'FeatureCollection',
                            features: geoJSONFeatures.filter((feature) => !feature.properties.alert),
                        };
                        setGeoJSONCollectionAlert(geoJSONCollectionAlert);
                        setGeoJSONCollectionNoAlert(geoJSONCollectionNoAlert);
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
                                    DeleteBulkMessage(deviceSelected, props.selectedRowKeys as string[]).then((res) => {
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
            <Title level={4} style={{marginTop: 24}}>历史轨迹</Title>
            <Card>
                <div style={{height: 500}}>
                    <Map
                        ref={map} zoom={11} Loca={{}} center={geoCenter} pitch={40}
                        // normal, light, whitesmoke, macaron
                        mapStyle="amap://styles/light" viewMode="3D"
                        WebGLParams={{
                            preserveDrawingBuffer: undefined
                        }}>
                        <Loca>
                            {geoPath.length > 0 &&
                                <Polyline path={geoPath} ref={polyline}/>}
                            <Scatter/>
                        </Loca>
                    </Map>
                </div>
            </Card>
        </PageContainer>
    );
}
export default MessagePage;