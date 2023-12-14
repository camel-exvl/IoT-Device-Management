import {PageContainer} from '@ant-design/pro-components';
import {useIntl, useModel} from '@umijs/max';
import {Card, theme} from 'antd';
import React from 'react';

const Welcome: React.FC = () => {
  const {token} = theme.useToken();
  const {initialState} = useModel('@@initialState');
  const intl = useIntl();
  return (
    <PageContainer>
      <Card
        style={{
          borderRadius: 8,
        }}
        bodyStyle={{
          backgroundImage:
            initialState?.settings?.navTheme === 'realDark'
              ? 'background-image: linear-gradient(75deg, #1A1B1F 0%, #191C1F 100%)'
              : 'background-image: linear-gradient(75deg, #FBFDFF 0%, #F5F7FF 100%)',
        }}
      >
        <div
          style={{
            fontSize: '20px',
            color: token.colorTextHeading,
          }}
        >
          {intl.formatMessage({id: 'pages.welcome.title'})}
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
          {intl.formatMessage({id: 'pages.welcome.desc'})}
        </p>
      </Card>
    </PageContainer>
  );
};

export default Welcome;
