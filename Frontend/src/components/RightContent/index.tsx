import {SelectLang as UmiSelectLang, useModel} from '@umijs/max';
import React from 'react';
import {Button} from "antd";

// export type SiderTheme = 'light' | 'dark';

export const SelectLang = () => {
  return (
    <UmiSelectLang
      style={{
        padding: 4,
      }}
    />
  );
};

// export const Question = () => {
//   return (
//     <div
//       style={{
//         display: 'flex',
//         height: 26,
//       }}
//       onClick={() => {
//         window.open('https://pro.ant.design/docs/getting-started');
//       }}
//     >
//       <QuestionCircleOutlined/>
//     </div>
//   );
// };

export const Theme = () => {
  const {initialState, setInitialState} = useModel('@@initialState');
  return (
    <div
      style={{
        display: 'flex',
      }}
      onClick={() => {
        const theme = initialState?.settings?.navTheme;
        const newTheme = theme === 'light' ? 'realDark' : 'light';
        setInitialState({
          ...initialState,
          settings: {
            ...initialState?.settings,
            navTheme: newTheme,
          },
        });
      }}
    >
      <Button type="link" style={{padding: 0}} shape='circle' size='small' title='切换主题'>
        {initialState?.settings?.navTheme === 'light' ? '🌙' : '☀️'}
      </Button>
    </div>
  );
}
