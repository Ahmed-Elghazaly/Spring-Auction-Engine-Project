export interface UserResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;

  enabled: boolean;

  createdAt: string;

  roles: string[];
}

export interface AuthResponse {
  token: string;

  tokenType: string;

  expiresInMs: number;
  username: string;
  roles: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UpdateUserStatusRequest {
  enabled: boolean;
}
