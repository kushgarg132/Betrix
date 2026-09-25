import { useMutation } from '@apollo/client/react';
import { LOGIN, REGISTER, GUEST_LOGIN } from '../graphql/mutations';

/**
 * Thin wrappers around the login/register/guestLogin mutations. Storing the token and loading the
 * user record is AuthContext's job (see login()/ctxLogin there) — this hook used to duplicate
 * both (its own GET_ME query, its own token write), which meant every mount of Login or Register
 * fired a second, unused /me request. Callers here are expected to hand the returned token to
 * AuthContext's login().
 */
export function useAuth() {
  const [loginMutation] = useMutation(LOGIN);
  const [registerMutation] = useMutation(REGISTER);
  const [guestLoginMutation] = useMutation(GUEST_LOGIN);

  const login = async (username, password) => {
    const { data } = await loginMutation({
      variables: { input: { username, password } },
    });
    return data.login;
  };

  const register = async (name, username, password, email) => {
    const { data } = await registerMutation({
      variables: { input: { name, username, password, email } },
    });
    return data.register;
  };

  const guestLogin = async () => {
    const { data } = await guestLoginMutation();
    return data.guestLogin;
  };

  return {
    login,
    register,
    guestLogin,
  };
}
