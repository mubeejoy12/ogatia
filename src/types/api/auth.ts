/**
 * Mirrors of the backend auth DTOs
 * ({@code com.eazicut.api.auth.dto.*} +
 *  {@code com.eazicut.api.users.dto.UserResponse}).
 *
 * <p>Field names match the JSON on the wire verbatim — do not rename
 * without updating the backend at the same time.
 */

export type Role = "CUSTOMER" | "ADMIN";

export type ApiUserResponse = {
  id: string;
  email: string;
  displayName: string | null;
  role: Role;
  createdAt: string; // ISO-8601 Instant
};

export type ApiRegisterRequest = {
  email: string;
  password: string;
  displayName?: string;
};

export type ApiLoginRequest = {
  email: string;
  password: string;
};

/**
 * Backend never puts `refreshToken` on the wire (it's `@JsonIgnore`'d
 * server-side and only carried in the HttpOnly refresh cookie). We
 * omit it here so the frontend can never accidentally reach for it.
 */
export type ApiLoginResponse = {
  accessToken: string;
  expiresInSeconds: number;
  user: ApiUserResponse;
};
