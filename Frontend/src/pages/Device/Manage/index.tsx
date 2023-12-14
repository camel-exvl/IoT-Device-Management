import React from "react";
import {useIntl, useModel} from "@@/exports";
import {Card, theme} from "antd";
import {PageContainer} from "@ant-design/pro-components";
import {ProTable} from "@ant-design/pro-table/lib";

const Manage: React.FC = () => {
  const {token} = theme.useToken();
  const {initialState} = useModel('@@initialState');
  const intl = useIntl();
  type DeviceItem = {
    id: string;
    name: string;
    type: string;
    description: string;
  };
  // const getDeviceList =
  //   async (params, sort, filter,) => {
  //     const msg = await list({
  //       page: params.current,
  //       pageSize: params.pageSize,
  //     });
  //     return {
  //       data: msg.result,
  //       // success 请返回 true，
  //       // 不然 table 会停止解析数据，即使有数据
  //       success: true,
  //       // 不传会使用 data 的长度，如果是分页一定要传
  //       total: 10
  //     };
  //   };
  return (
      <PageContainer>
        <Card>
          <ProTable<DeviceItem>


          />

        </Card>
      </PageContainer>);
}

export default Manage;
