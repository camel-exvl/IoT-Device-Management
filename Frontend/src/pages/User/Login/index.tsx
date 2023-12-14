import Footer from '@/components/Footer';
import {LockOutlined, UserOutlined,} from '@ant-design/icons';
import {LoginForm, ProFormCheckbox, ProFormText,} from '@ant-design/pro-components';
import {useEmotionCss} from '@ant-design/use-emotion-css';
import {FormattedMessage, Helmet, SelectLang, useIntl, useModel} from '@umijs/max';
import {Alert, message} from 'antd';
import React, {useState} from 'react';
import {login} from "@/services/swagger/user";
import {ResponseStructure} from "@/requestErrorConfig";
import {flushSync} from "react-dom";
import {history} from "umi";

const Lang = () => {
  const langClassName = useEmotionCss(({token}) => {
    return {
      width: 42,
      height: 42,
      lineHeight: '42px',
      position: 'fixed',
      right: 16,
      borderRadius: token.borderRadius,
      ':hover': {
        backgroundColor: token.colorBgTextHover,
      },
    };
  });

  return (
    <div className={langClassName} data-lang>
      {SelectLang && <SelectLang/>}
    </div>
  );
};

const LoginMessage: React.FC<{
  content: string;
}> = ({content}) => {
  return (
    <Alert
      style={{
        marginBottom: 24,
      }}
      message={content}
      type="error"
      showIcon
    />
  );
};

const Login: React.FC = () => {
  const [userLoginState, setUserLoginState] = useState<string>('');
  const [errorMsg, setErrorMsg] = useState<string>('');
  const {initialState, setInitialState} = useModel('@@initialState');

  const containerClassName = useEmotionCss(() => {
    return {
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      overflow: 'auto',
      backgroundImage:
        "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
      backgroundSize: '100% 100%',
    };
  });

  const intl = useIntl();

  const fetchUserInfo = async () => {
    const userInfo = await initialState?.fetchUserInfo?.();
    if (userInfo) {
      flushSync(() => {
        setInitialState((s) => ({
          ...s,
          currentUser: userInfo,
        }));
      });
    }
  };

  const handleSubmit = async (values: API.LoginData) => {
    try {
      const msg = (await login({...values}));
      console.log(msg);
      if (msg.code === 200) {
        message.success(intl.formatMessage({id: 'pages.login.success'}));
        await fetchUserInfo();
        const urlParams = new URL(window.location.href).searchParams;
        history.push(urlParams.get('redirect') || '/');
        return;
      }
    } catch (error) {
      const code = (error as ResponseStructure).code || undefined;
      setUserLoginState('error');
      switch (code) {
        case 404:
          setErrorMsg(intl.formatMessage({id: 'pages.login.userNotExist'}));
          break;
        case 401:
          setErrorMsg(intl.formatMessage({id: 'pages.login.userPasswordError'}));
          break;
        case 500:
          setErrorMsg(intl.formatMessage({id: 'pages.login.serverError'}));
          break;
        default:
          setErrorMsg(intl.formatMessage({id: 'pages.login.failure'}));
      }
    }
  }

  return (
    <div className={containerClassName}>
      <Helmet>
        <title>
          {intl.formatMessage({id: 'menu.login',})}&nbsp;- {intl.formatMessage({id: 'app.title',})}
        </title>
      </Helmet>
      <Lang/>
      <div
        style={{
          flex: '1',
          padding: '32px 0',
        }}
      >
        <LoginForm
          contentStyle={{
            minWidth: 280,
            maxWidth: '75vw',
          }}
          containerStyle={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
          }}
          logo={<img alt="logo" src="/logo.svg"/>}
          title={intl.formatMessage({id: 'app.title',})}
          subTitle={intl.formatMessage({id: 'pages.layouts.userLayout.title'})}
          initialValues={{
            autoLogin: true,
          }}
          onFinish={async (values) => {
            await handleSubmit(values as API.LoginData);
          }}
        >

          {userLoginState === 'error' && (
            <LoginMessage
              content={intl.formatMessage({id: errorMsg})}
            />
          )}
          {(
            <>
              <ProFormText
                name="username"
                fieldProps={{
                  size: 'large',
                  prefix: <UserOutlined/>,
                }}
                placeholder={intl.formatMessage({id: 'pages.login.username.placeholder',})}
                rules={[
                  {
                    required: true,
                    message: (
                      <FormattedMessage id="pages.login.username.required"/>
                    ),
                  },
                ]}
              />
              <ProFormText.Password
                name="password"
                fieldProps={{
                  size: 'large',
                  prefix: <LockOutlined/>,
                }}
                placeholder={intl.formatMessage({id: 'pages.login.password.placeholder',})}
                rules={[
                  {
                    required: true,
                    message: (
                      <FormattedMessage id="pages.login.password.required"/>
                    ),
                  },
                ]}
              />
            </>
          )}

          <div
            style={{
              marginBottom: 24,
            }}
          >
            <ProFormCheckbox noStyle name="rememberMe">
              <FormattedMessage id="pages.login.rememberMe"/>
            </ProFormCheckbox>
            {/*<a*/}
            {/*  style={{*/}
            {/*    float: 'right',*/}
            {/*  }}*/}
            {/*>*/}
            {/*  <FormattedMessage id="pages.login.forgotPassword"/>*/}
            {/*</a>*/}
          </div>
        </LoginForm>
      </div>
      <Footer/>
    </div>
  );
};

export default Login;
