import {PageContainer} from '@ant-design/pro-components';
import {Card, Col, Row, Statistic, theme, Tooltip, Typography} from 'antd';
import React, {useContext, useEffect, useState} from 'react';
import {ApiOutlined, BarChartOutlined, ClusterOutlined, QuestionCircleOutlined} from "@ant-design/icons";
import {UserInfoContext} from "../../app.tsx";
import {Link} from "react-router-dom";
import Title from "antd/es/typography/Title";
import {GetActiveDeviceNums, GetDeviceStatistics} from "../../service/device.ts";
import {DeviceActiveNums, DeviceStatistics} from "../../service/typing";
import {Line, measureTextWidth, Pie} from "@ant-design/charts";

const DeviceType = new Map<number, string>([
    [0, "传感器"],
    [1, "智能家居"],
    [2, "执行器"],
    [3, "控制器"],
    [4, "网关"],
    [5, "终端"],
    [6, "嵌入式"],
    [7, "其他"],
]);

const Welcome: React.FC = () => {
        const [userInfo, setUserInfo] = useContext(UserInfoContext)
        const [deviceStatistics, setDeviceStatistics] = useState({} as DeviceStatistics);
        const [deviceActiveNums, setDeviceActiveNums] = useState([] as DeviceActiveNums[]);
        const [deviceActiveNumsLoading, setDeviceActiveNumsLoading] = useState(false);
        const [deviceType, setDeviceType] = useState([] as { type: string, num: number }[]);
        const [deviceStatisticsLoading, setDeviceStatisticsLoading] = useState(false);
        const {token} = theme.useToken();
        const pStyle = {
            fontSize: '14px',
            color: token.colorTextSecondary,
            lineHeight: '22px',
            marginTop: 16,
            marginBottom: 32,
            width: '65%',
        };

        useEffect(() => {
            if (userInfo.userId !== "") {
                setDeviceStatisticsLoading(true);
                GetDeviceStatistics().then((res) => {
                    setDeviceStatistics(res.data);

                    // convert deviceStatistics.deviceType type to string
                    const deviceType = [] as { type: string, num: number }[];
                    for (const type in res.data.deviceType) {
                        deviceType.push({type: DeviceType.get(parseInt(type)) || "", num: res.data.deviceType[type].num});
                    }
                    setDeviceType(deviceType);

                    setDeviceStatisticsLoading(false);
                })

                setDeviceActiveNumsLoading(true);
                GetActiveDeviceNums().then((res) => {
                    // convert timestamp to string
                    for (const deviceActiveNum of res.data) {
                        deviceActiveNum.time = new Date(parseInt(deviceActiveNum.time)).toLocaleTimeString().substring(0, 5);
                    }
                    setDeviceActiveNums(res.data);
                    setDeviceActiveNumsLoading(false);
                })
            }
        }, [userInfo]);

        function renderStatistic(containerWidth, text, style) {
            const {width: textWidth, height: textHeight} = measureTextWidth(text, style);
            const R = containerWidth / 2; // r^2 = (w / 2)^2 + (h - offsetY)^2

            let scale = 1;

            if (containerWidth < textWidth) {
                scale = Math.min(Math.sqrt(Math.abs(Math.pow(R, 2) / (Math.pow(textWidth / 2, 2) + Math.pow(textHeight, 2)))), 1);
            }

            const textStyleStr = `width:${containerWidth}px;`;
            return `<div style="${textStyleStr};font-size:${scale}em;line-height:${scale < 1 ? 1 : 'inherit'};">${text}</div>`;
        }

        return (
            <PageContainer>
                <Card
                    style={{
                        borderRadius: 8,
                    }}
                >
                    <div
                        style={{
                            fontSize: '20px',
                            color: token.colorTextHeading,
                        }}
                    >
                        欢迎访问『物华天宝』管理平台
                    </div>
                    <p style={pStyle}>
                        『物华天宝』是一个物联网设备管理平台，致力于为用户提供一个综合的解决方案，用于管理、监控和维护物联网设备。
                    </p>
                    {userInfo.userId === "" ?
                        <p style={pStyle}>您还未登录，请先<Link to="/user/login">登录</Link>以使用完整功能</p> :
                        <p style={pStyle}>欢迎您，{userInfo.username}</p>}
                </Card>
                {userInfo.userId === "" ? <></> :
                    <Typography style={{marginTop: 32}}>
                        <Title level={4} style={{marginBottom: 25}}>统计信息</Title>
                        <Row gutter={16}>
                            <Col span={8} xs={24} sm={24} md={8} lg={8}>
                                <Card bordered={false}
                                      style={{
                                          display: 'flex',
                                          flexDirection: 'column',
                                          justifyContent: 'space-between',
                                          height: '100%'
                                      }}>
                                    <Statistic
                                        title="设备总量"
                                        value={deviceStatistics?.deviceCount}
                                        loading={deviceStatisticsLoading}
                                        valueStyle={{color: token.colorPrimary}}
                                        prefix={<ClusterOutlined/>}
                                        suffix={"台"}
                                    />
                                </Card>
                            </Col>
                            <Col span={8} xs={24} sm={24} md={8} lg={8}>
                                <Card bordered={false}>
                                    <Statistic
                                        title="活跃设备"
                                        value={deviceStatistics?.activeDeviceCount + "/" + deviceStatistics?.deviceCount}
                                        loading={deviceStatisticsLoading}
                                        valueStyle={{color: token.colorPrimary}}
                                        prefix={<ApiOutlined/>}
                                        suffix={"台"}
                                    />
                                </Card>
                            </Col>
                            <Col span={8} xs={24} sm={24} md={8} lg={8}>
                                <Card bordered={false}>
                                    <Statistic
                                        title="接收的数据量"
                                        value={deviceStatistics?.messageCount}
                                        loading={deviceStatisticsLoading}
                                        valueStyle={{color: token.colorPrimary}}
                                        prefix={<BarChartOutlined/>}
                                        suffix={"条"}
                                    />
                                </Card>
                            </Col>
                        </Row>
                        <Row gutter={{xs: 8, sm: 16, md: 24, lg: 32}}>
                            <Col span={12} xs={24} sm={24} md={12} lg={12}>
                                <Card style={{marginTop: 32}}>
                                    <Title level={5} style={{marginBottom: 25}}>设备类型</Title>
                                    <Pie appendPadding={10} data={deviceType} angleField='num' colorField='type' radius={1}
                                         innerRadius={0.64} meta={{value: {formatter: (v) => `${v} 台`,},}} label={{
                                        type: 'inner',
                                        offset: '-50%',
                                        style: {textAlign: 'center',},
                                        autoRotate: false,
                                        content: '{value}',
                                    }} statistic={{
                                        title: {
                                            offsetY: -4,
                                            style: {lineHeight: '1.5',},
                                            customHtml: (container, view, datum) => {
                                                const {width, height} = container.getBoundingClientRect();
                                                const d = Math.sqrt(Math.pow(width / 2, 2) + Math.pow(height / 2, 2));
                                                const text = datum ? datum.type : '总计';
                                                return renderStatistic(d, text, {fontSize: "1rem",});
                                            },
                                        },
                                        content: {
                                            offsetY: 4,
                                            style: {lineHeight: '1.5',},
                                            customHtml: (container, view, datum, data) => {
                                                const {width} = container.getBoundingClientRect();
                                                const text = datum ? `${datum.num} 台` : `${data?.reduce((r, d) => r + d.num, 0)} 台`;
                                                return renderStatistic(width, text, {fontSize: "1rem",});
                                            },
                                        },
                                    }} legend={{
                                        position: 'top',
                                        layout: 'horizontal',
                                        marker: {symbol: 'square',},
                                        style: {fontSize: "1rem",},
                                    }}
                                         interactions={[{type: 'element-selected',}, {type: 'element-active',}, {type: 'pie-statistic-active',},]}/>
                                </Card>
                            </Col>
                            <Col span={12} xs={24} sm={24} md={12} lg={12}>
                                <Card style={{marginTop: 32}}>
                                    <Title level={5} style={{marginBottom: 25}}>
                                        活跃设备数量
                                        <Tooltip title={"1小时内有数据上传的设备"}><QuestionCircleOutlined/></Tooltip>
                                    </Title>
                                    <Line data={deviceActiveNums} xField='time' yField='activeNum' smooth={true}
                                          yAxis={{label: {formatter: (v) => `${v} 台`,},}}
                                          tooltip={{
                                              formatter: (v) => ({
                                                  name: '活跃设备数量',
                                                  value: `${v.activeNum} 台`,
                                              }),
                                          }}/>
                                </Card>
                            </Col>
                        </Row>
                    </Typography>
                }
            </PageContainer>
        )
            ;
    }
;

export default Welcome;