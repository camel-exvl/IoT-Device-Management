import React, {useContext, useRef} from "react";
import {Button, Card, message, Popconfirm} from "antd";
import {
    ActionType,
    ModalForm,
    PageContainer,
    ProColumns,
    ProForm,
    ProFormSelect,
    ProFormText,
    ProFormTextArea
} from "@ant-design/pro-components";
import {ProTable} from "@ant-design/pro-table/lib";
import {CreateDevice, DeleteDevice, SearchDevice, ModifyDevice} from "../../service/device.ts";
import {PlusOutlined} from "@ant-design/icons";
import {UserInfoContext} from "../../app.tsx";
import {Navigate} from "react-router-dom";

const DevicePage: React.FC = () => {
    const [createModalVisible, handleModalVisible] = React.useState<boolean>(false);
    const actionRef = useRef<ActionType>();
    const [messageApi, contextHolder] = message.useMessage();
    const [userInfo, setUserInfo] = useContext(UserInfoContext);

    type DeviceItem = {
        id: string;
        name: string;
        type: string;
        description: string;
    };

    const columns: ProColumns<DeviceItem>[] = [
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
            title: "设备名称",
            dataIndex: "name",
            key: "name",
            formItemProps: {
                rules: [
                    {
                        required: true,
                        message: "设备名称为必填项",
                    },
                ],
            },
        },
        {
            title: "设备类型",
            dataIndex: "type",
            key: "type",
            valueType: "select",
            valueEnum: {
                0: {text: "传感器"},
                1: {text: "智能家居"},
                2: {text: "执行器"},
                3: {text: "控制器"},
                4: {text: "网关"},
                5: {text: "终端"},
                6: {text: "嵌入式"},
                7: {text: "其他"},
            },
            filters: true,
            onFilter: true,
            formItemProps: {
                rules: [
                    {
                        required: true,
                        message: "设备类型为必填项",
                    },
                ],
            }
        },
        {
            title: "设备描述",
            dataIndex: "description",
            key: "description",
            ellipsis: true,
            hideInSearch: true,
        },
        {
            title: '操作',
            valueType: 'option',
            key: 'option',
            render: (text, record, _, action) => [
                <a
                    key="editable"
                    onClick={() => {
                        action?.startEditable?.(record.id);
                    }}
                >
                    编辑
                </a>,
                <Popconfirm title="确定删除?" key="delete" onConfirm={async () => {
                    const data = await DeleteDevice(record.id);
                    if (data.code === 200) {
                        successMessage("删除成功");
                        if (actionRef.current) {
                            actionRef.current.reload();
                        }
                    }
                }}>
                    <a key="delete">
                        删除
                    </a>
                </Popconfirm>,
            ],
        },
    ]

    const successMessage = (content: string) => {
        messageApi.success(content);
    }

    return (
        userInfo.userId === "" ? <Navigate replace to={"/user/login"} state={{from: "/device"}}/> :
        <PageContainer>
            {contextHolder}
            <Card>
                <ProTable<DeviceItem> columns={columns} actionRef={actionRef}
                                      request={async (params, sort, filter) => {
                                          const searchName = params.name || "";
                                          const searchType = params.type || "";
                                          const data = await SearchDevice(searchName, searchType);
                                          return {
                                              data: data,
                                              success: true
                                          };
                                      }} rowKey="id" search={{labelWidth: "auto",}} pagination={{pageSize: 10,}}
                                      dateFormatter="string" headerTitle="设备列表"
                                      editable={{
                                          type: 'multiple',
                                          actionRender: (row, config, dom) => [dom.save, dom.cancel],
                                          onSave: async (key, row) => {
                                              const data = await ModifyDevice(row.id, row.name, +row.type, row.description);
                                              if (data.code === 200) {
                                                  successMessage("修改成功");
                                                  if (actionRef.current) {
                                                      actionRef.current.reload();
                                                  }
                                              }
                                          },
                                      }}
                                      toolBarRender={() => [<Button key="button" icon={<PlusOutlined/>} onClick={() => {
                                          handleModalVisible(true);
                                      }} type="primary">新建</Button>,]}
                                      options={{fullScreen: false, reload: true, density: false, setting: false}}/>
                <ModalForm
                    title="新建设备"
                    open={createModalVisible}
                    onOpenChange={handleModalVisible}
                    width="700px"
                    layout="horizontal"
                    autoFocusFirstInput
                    modalProps={{destroyOnClose: true,}}
                    onFinish={async (value) => {
                        const data = await CreateDevice(value.name, value.type, value.desc);
                        if (data.code === 200) {
                            successMessage("创建成功");
                            handleModalVisible(false);
                            if (actionRef.current) {
                                actionRef.current.reload();
                            }
                        }
                    }}
                >
                    <ProForm.Group>
                        <ProFormText
                            rules={[
                                {
                                    required: true,
                                    message: "设备名称为必填项",
                                },
                            ]}
                            name="name"
                            label="设备名称"
                        />
                        <ProFormSelect
                            rules={[
                                {
                                    required: true,
                                    message: "设备类型为必填项",
                                },
                            ]}
                            name="type"
                            label="设备类型"
                            valueEnum={{
                                0: {text: "传感器"},
                                1: {text: "智能家居"},
                                2: {text: "执行器"},
                                3: {text: "控制器"},
                                4: {text: "网关"},
                                5: {text: "终端"},
                                6: {text: "嵌入式"},
                                7: {text: "其他"},
                            }}
                            showSearch={true}
                        />
                    </ProForm.Group>
                    <ProFormTextArea name="desc" label="设备描述"/>
                </ModalForm>
            </Card>
        </PageContainer>);
}

export default DevicePage;
