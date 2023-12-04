declare namespace API {
  type CreateUserData = {
    username?: string;
    email?: string;
    password?: string;
  };

  type LoginData = {
    username?: string;
    password?: string;
    rememberMe?: boolean;
  };

  type ResponseStructure = {
    success?: boolean;
    errorMessage?: string;
    code?: number;
    data?: Record<string, any>;
  };

  type ResponseStructureObject = {
    success?: boolean;
    errorMessage?: string;
    code?: number;
    data?: Record<string, any>;
  };

  type ResponseStructureUserInfo = {
    success?: boolean;
    errorMessage?: string;
    code?: number;
    data?: UserInfo;
  };

  type UserInfo = {
    username?: string;
    email?: string;
  };
}
