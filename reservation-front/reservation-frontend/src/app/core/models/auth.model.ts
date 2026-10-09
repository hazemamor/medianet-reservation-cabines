export interface LoginRequest {
  email: string;
}

export interface VerifyOtpRequest {
  email: string;
  code: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: number;
  email: string;
  role: 'EMPLOYE' | 'ADMINISTRATEUR';
}
