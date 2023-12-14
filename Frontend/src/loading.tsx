// 显示正在加载的message
import {Card, Skeleton} from "antd";
import React from "react";
import {ProLayout} from "@ant-design/pro-layout";
import {PageContainer} from "@ant-design/pro-components";
import {useIntl} from "@@/exports";

const loadingPage: React.FC = () => {
    // const [messageApi, contextHolder] = message.useMessage(); // 骨架屏
    // const showLoading = () => {
    //   messageApi.open({
    //     type: "loading",
    //     content: "数据加载中...",
    //     duration: 0,
    //   });
    //   // Dismiss manually and asynchronously
    //   setTimeout(messageApi.destroy, 2500);
    // };

    const intl = useIntl();

    // showLoading();
    return (
        <>
            <ProLayout title={intl.formatMessage({id: 'app.title'})} logo={false} layout={"mix"} menuRender={false}>
                <PageContainer header={{title: intl.formatMessage({id: 'pages.loading'})}}>
                    <Card>
                        <Skeleton active/>
                    </Card>
                </PageContainer>
            </ProLayout>
        </>
    );
}

export default loadingPage;
