import {PageContainer} from '@ant-design/pro-components';
import {Card, theme} from 'antd';
import React from 'react';

const Welcome: React.FC = () => {
    const {token} = theme.useToken();
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
                <p
                    style={{
                        fontSize: '14px',
                        color: token.colorTextSecondary,
                        lineHeight: '22px',
                        marginTop: 16,
                        marginBottom: 32,
                        width: '65%',
                    }}
                >
                    『物华天宝』是一个物联网设备管理平台，致力于为用户提供一个综合的解决方案，用于管理、监控和维护物联网设备。
                </p>
            </Card>
        </PageContainer>
    );
};

export default Welcome;
